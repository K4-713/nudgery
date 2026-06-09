// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared

import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.usecase.CreateNudgeRequest
import com.nudgery.shared.usecase.CreateNudgeResult
import com.nudgery.shared.usecase.CreateNudgeUseCase
import com.nudgery.shared.usecase.DeleteNudgeUseCase
import com.nudgery.shared.usecase.QuestionRequest
import com.nudgery.shared.usecase.ScheduleRequest
import com.nudgery.shared.util.FakeNotificationScheduler
import com.nudgery.shared.util.TestRepositories
import com.nudgery.shared.util.createTestRepositories
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NudgeDeletionTest {

    private lateinit var repos: TestRepositories
    private lateinit var fakeScheduler: FakeNotificationScheduler
    private lateinit var createNudge: CreateNudgeUseCase
    private lateinit var deleteNudge: DeleteNudgeUseCase

    @BeforeTest
    fun setup() {
        repos = createTestRepositories()
        fakeScheduler = FakeNotificationScheduler()
        createNudge = CreateNudgeUseCase(
            repos.nudgeRepository,
            repos.questionRepository,
            repos.questionOptionRepository,
            repos.scheduleRepository,
            fakeScheduler
        )
        deleteNudge = DeleteNudgeUseCase(repos.nudgeRepository, fakeScheduler)
    }

    private suspend fun createTestNudge(): String {
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest(text = "Did you see any cool birds today?", type = QuestionType.YES_NO),
                schedule = ScheduleRequest(
                    type = ScheduleType.DAILY,
                    timeOfDay = LocalTime(9, 0),
                    activeDaysOfWeek = DayOfWeek.entries.toSet()
                )
            )
        )
        return (result as CreateNudgeResult.Success).nudgeId
    }

    @Test
    fun TDD_deleteNudge_removesNudgeFromRepository() = runTest {
        // Deleting a nudge must remove it from storage
        val nudgeId = createTestNudge()
        deleteNudge.execute(nudgeId)
        assertNull(repos.nudgeRepository.getById(nudgeId))
    }

    @Test
    fun TDD_deleteNudge_nudgeNoLongerAppearsInList() = runTest {
        // Deleted nudge must not appear in the nudge list
        val nudgeId = createTestNudge()
        deleteNudge.execute(nudgeId)
        val remaining = repos.nudgeRepository.observeAll().first()
        assertTrue(remaining.none { it.id == nudgeId })
    }

    @Test
    fun TDD_deleteNudge_cancelsNotificationSchedule() = runTest {
        // Deleting a nudge must cancel its pending notifications
        val nudgeId = createTestNudge()
        fakeScheduler.reset()
        deleteNudge.execute(nudgeId)
        assertTrue(fakeScheduler.cancelled.contains(nudgeId))
    }

    @Test
    fun TDD_deleteNudge_cascadesQuestions() = runTest {
        // Questions belonging to the deleted nudge must also be removed
        val nudgeId = createTestNudge()
        deleteNudge.execute(nudgeId)
        assertTrue(repos.questionRepository.getByNudgeId(nudgeId).isEmpty())
    }

    @Test
    fun TDD_deleteNudge_doesNotAffectOtherNudges() = runTest {
        // Deleting one nudge must leave other nudges intact
        val nudgeId = createTestNudge()
        val otherNudgeId = createTestNudge()
        deleteNudge.execute(nudgeId)
        val remaining = repos.nudgeRepository.observeAll().first()
        assertTrue(remaining.any { it.id == otherNudgeId })
    }
}
