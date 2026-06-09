// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared

import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.model.TriggerOperator
import com.nudgery.shared.usecase.CreateNudgeRequest
import com.nudgery.shared.usecase.CreateNudgeResult
import com.nudgery.shared.usecase.CreateNudgeUseCase
import com.nudgery.shared.usecase.FollowUpReplacement
import com.nudgery.shared.usecase.QuestionRequest
import com.nudgery.shared.usecase.ScheduleRequest
import com.nudgery.shared.usecase.UpdateNudgeRequest
import com.nudgery.shared.usecase.UpdateNudgeResult
import com.nudgery.shared.usecase.UpdateNudgeUseCase
import com.nudgery.shared.util.FakeNotificationScheduler
import com.nudgery.shared.util.TestRepositories
import com.nudgery.shared.util.createTestRepositories
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UpdateNudgeFollowUpTest {

    private lateinit var repos: TestRepositories
    private lateinit var createNudge: CreateNudgeUseCase
    private lateinit var updateNudge: UpdateNudgeUseCase
    private val scheduler = FakeNotificationScheduler()

    @BeforeTest
    fun setup() {
        repos = createTestRepositories()
        createNudge = CreateNudgeUseCase(
            repos.nudgeRepository,
            repos.questionRepository,
            repos.questionOptionRepository,
            repos.scheduleRepository,
            scheduler
        )
        updateNudge = UpdateNudgeUseCase(
            repos.nudgeRepository,
            repos.questionRepository,
            repos.questionOptionRepository,
            repos.scheduleRepository,
            repos.nudgeEditRepository,
            scheduler
        )
    }

    private fun dailySchedule() = ScheduleRequest(
        type = ScheduleType.DAILY,
        timeOfDay = LocalTime(12, 0),
        activeDaysOfWeek = DayOfWeek.entries.toSet()
    )

    private suspend fun createSimpleNudge(withFollowUp: Boolean = false): String {
        val followUps = if (withFollowUp) listOf(
            QuestionRequest(
                text = "How bad was it?",
                type = QuestionType.NUMBER,
                triggerAnswerValue = "YES",
                triggerOperator = TriggerOperator.EQ
            )
        ) else emptyList()
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Did you have a headache?", QuestionType.YES_NO),
                followUpQuestions = followUps,
                schedule = dailySchedule()
            )
        )
        return (result as CreateNudgeResult.Success).nudgeId
    }

    @Test
    fun TDD_addFollowUpToExistingNudge() = runTest {
        // README: follow-up questions can be added after nudge creation
        val nudgeId = createSimpleNudge(withFollowUp = false)

        val result = updateNudge.execute(
            UpdateNudgeRequest(
                nudgeId = nudgeId,
                followUpReplacements = listOf(
                    FollowUpReplacement(
                        questionId = null,
                        request = QuestionRequest(
                            text = "Describe the pain",
                            type = QuestionType.TEXT,
                            triggerAnswerValue = "YES",
                            triggerOperator = TriggerOperator.EQ
                        )
                    )
                )
            )
        )

        assertIs<UpdateNudgeResult.Success>(result)
        val questions = repos.questionRepository.getByNudgeId(nudgeId)
        val followUps = questions.filter { !it.isMainQuestion }
        assertEquals(1, followUps.size)
        assertEquals("Describe the pain", followUps.first().text)
        assertEquals(QuestionType.TEXT, followUps.first().type)
        assertEquals("YES", followUps.first().triggerAnswerValue)
    }

    @Test
    fun TDD_removeExistingFollowUpFromNudge() = runTest {
        // Passing an empty replacement list removes all follow-ups
        val nudgeId = createSimpleNudge(withFollowUp = true)

        val before = repos.questionRepository.getByNudgeId(nudgeId).filter { !it.isMainQuestion }
        assertEquals(1, before.size, "Precondition: nudge should have one follow-up")

        val result = updateNudge.execute(
            UpdateNudgeRequest(
                nudgeId = nudgeId,
                followUpReplacements = emptyList()
            )
        )

        assertIs<UpdateNudgeResult.Success>(result)
        val followUps = repos.questionRepository.getByNudgeId(nudgeId).filter { !it.isMainQuestion }
        assertTrue(followUps.isEmpty(), "Follow-up should have been removed")
    }

    @Test
    fun TDD_updateExistingFollowUpText() = runTest {
        // An existing follow-up (identified by questionId) is updated in-place
        val nudgeId = createSimpleNudge(withFollowUp = true)

        val existingFollowUp = repos.questionRepository.getByNudgeId(nudgeId).first { !it.isMainQuestion }

        val result = updateNudge.execute(
            UpdateNudgeRequest(
                nudgeId = nudgeId,
                followUpReplacements = listOf(
                    FollowUpReplacement(
                        questionId = existingFollowUp.id,
                        request = QuestionRequest(
                            text = "Rate the pain from 1 to 10",
                            type = QuestionType.NUMBER,
                            triggerAnswerValue = "YES",
                            triggerOperator = TriggerOperator.EQ
                        )
                    )
                )
            )
        )

        assertIs<UpdateNudgeResult.Success>(result)
        val updated = repos.questionRepository.getByNudgeId(nudgeId).first { !it.isMainQuestion }
        assertEquals("Rate the pain from 1 to 10", updated.text)
        assertEquals(existingFollowUp.id, updated.id, "Question ID should be preserved on in-place update")
    }

    @Test
    fun TDD_noChangeToFollowUpsWhenReplacementsIsNull() = runTest {
        // null followUpReplacements means "don't touch follow-ups at all"
        val nudgeId = createSimpleNudge(withFollowUp = true)

        val result = updateNudge.execute(
            UpdateNudgeRequest(
                nudgeId = nudgeId,
                followUpReplacements = null  // no follow-up change
            )
        )

        assertIs<UpdateNudgeResult.Success>(result)
        val followUps = repos.questionRepository.getByNudgeId(nudgeId).filter { !it.isMainQuestion }
        assertEquals(1, followUps.size, "Follow-ups should be unchanged when replacements is null")
    }
}
