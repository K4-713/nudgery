// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared

import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.repository.AnswerRepository
import com.nudgery.shared.usecase.CreateNudgeRequest
import com.nudgery.shared.usecase.CreateNudgeResult
import com.nudgery.shared.usecase.CreateNudgeUseCase
import com.nudgery.shared.usecase.GetVisualizationDataUseCase
import com.nudgery.shared.usecase.QuestionRequest
import com.nudgery.shared.usecase.RecordAnswerUseCase
import com.nudgery.shared.usecase.ScheduleRequest
import com.nudgery.shared.usecase.SetAnswerHiddenUseCase
import com.nudgery.shared.model.Timeframe
import com.nudgery.shared.model.VisualizationData
import com.nudgery.shared.util.FakeNotificationScheduler
import com.nudgery.shared.util.TestRepositories
import com.nudgery.shared.util.createTestRepositories
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AnswerRecordingTest {

    private lateinit var repos: TestRepositories
    private lateinit var createNudge: CreateNudgeUseCase
    private lateinit var recordAnswer: RecordAnswerUseCase
    private lateinit var setAnswerHidden: SetAnswerHiddenUseCase
    private lateinit var getVisualizationData: GetVisualizationDataUseCase

    @BeforeTest
    fun setup() {
        repos = createTestRepositories()
        createNudge = CreateNudgeUseCase(
            repos.nudgeRepository, repos.questionRepository, repos.questionOptionRepository,
            repos.scheduleRepository, FakeNotificationScheduler()
        )
        recordAnswer = RecordAnswerUseCase(repos.answerRepository)
        setAnswerHidden = SetAnswerHiddenUseCase(repos.answerRepository)
        getVisualizationData = GetVisualizationDataUseCase(
            repos.answerRepository, repos.questionRepository, repos.questionOptionRepository
        )
    }

    private fun dailySchedule() = ScheduleRequest(
        type = ScheduleType.DAILY,
        timeOfDay = LocalTime(12, 0),
        activeDaysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
    )

    private suspend fun createYesNoNudge(): Pair<String, String> {
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Did you exercise?", QuestionType.YES_NO),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val questionId = repos.questionRepository.getByNudgeId(result.nudgeId)
            .first { it.isMainQuestion }.id
        return result.nudgeId to questionId
    }

    @Test
    fun TDD_textAnswerTrimmedOnRecord() = runTest {
        // ENGINEERING_DECISIONS.md ED-16: a free-text answer is trimmed at the save boundary, so
        // stray surrounding whitespace never reaches storage.
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("How was your day?", QuestionType.TEXT),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val questionId = repos.questionRepository.getByNudgeId(result.nudgeId)
            .first { it.isMainQuestion }.id

        recordAnswer.execute(result.nudgeId, questionId, "  pretty good  ")

        val answers = repos.answerRepository.getAllByNudgeId(result.nudgeId)
        assertEquals(1, answers.size)
        assertEquals("pretty good", answers[0].value)
    }

    @Test
    fun TDD_answerRecordedWithNudgeAndQuestionReference() = runTest {
        // ARCHITECTURE.md Answer: nudgeId FK → Nudge, questionId FK → Question
        val (nudgeId, questionId) = createYesNoNudge()

        val answerId = recordAnswer.execute(nudgeId, questionId, "YES")

        val answers = repos.answerRepository.getAllByNudgeId(nudgeId)
        assertEquals(1, answers.size)
        assertEquals(nudgeId, answers[0].nudgeId)
        assertEquals(questionId, answers[0].questionId)
        assertEquals(answerId, answers[0].id)
    }

    @Test
    fun TDD_answerRecordedWithTimestamp() = runTest {
        // ARCHITECTURE.md Answer.scheduledAt / answeredAt: "Instant"
        val (nudgeId, questionId) = createYesNoNudge()

        recordAnswer.execute(nudgeId, questionId, "YES")

        val answers = repos.answerRepository.getAllByNudgeId(nudgeId)
        assertNotNull(answers[0].scheduledAt)
        assertNotNull(answers[0].answeredAt)
    }

    @Test
    fun TDD_answerIsHiddenDefaultsFalse() = runTest {
        // ARCHITECTURE.md Answer.isHidden: hidden rows excluded from visualizations;
        //   newly recorded answers are visible by default
        val (nudgeId, questionId) = createYesNoNudge()

        recordAnswer.execute(nudgeId, questionId, "YES")

        val answers = repos.answerRepository.getAllByNudgeId(nudgeId)
        assertFalse(answers[0].isHidden)
    }

    @Test
    fun TDD_answerCanBeMarkedHidden() = runTest {
        // README "Editing Nudges": "you can select individual answers and tag them as hidden"
        val (nudgeId, questionId) = createYesNoNudge()
        val answerId = recordAnswer.execute(nudgeId, questionId, "YES")

        setAnswerHidden.execute(answerId, isHidden = true)

        val answers = repos.answerRepository.getAllByNudgeId(nudgeId)
        assertTrue(answers.first { it.id == answerId }.isHidden)
    }

    @Test
    fun TDD_hiddenAnswersExcludedFromVisualizationData() = runTest {
        // README "Editing Nudges": "Hidden rows no longer appear in the data visualization"
        val (nudgeId, questionId) = createYesNoNudge()

        val visibleAnswerId = recordAnswer.execute(nudgeId, questionId, "YES")
        val hiddenAnswerId = recordAnswer.execute(nudgeId, questionId, "YES")
        setAnswerHidden.execute(hiddenAnswerId, isHidden = true)

        val vizData = getVisualizationData.execute(nudgeId, questionId, Timeframe.ALL_TIME)
        val heatMap = vizData.filterIsInstance<VisualizationData.CalendarHeatMap>().first()
        val totalCount = heatMap.dailyCounts.sumOf { it.value }

        // Only the visible answer should appear
        assertEquals(1.0, totalCount)
    }

    @Test
    fun TDD_answerValueDataCannotBeEdited() = runTest {
        // README "Editing Nudges": "While you cannot edit Nudge answer data once entered..."
        // Verified by API design: AnswerRepository exposes no method to update answer values.
        val methods = AnswerRepository::class.members.map { it.name }.toSet()
        val hasValueUpdateMethod = methods.any { name ->
            (name.startsWith("update") || name.startsWith("edit"))
                && !name.contains("Hidden", ignoreCase = true)
                && !name.contains("hidden", ignoreCase = true)
        }
        assertFalse(hasValueUpdateMethod, "AnswerRepository must not expose an answer value update method")
    }

    @Test
    fun TDD_answerNowRecordsAnswerOutsideOfNotification() = runTest {
        // README "Viewing Nudges": "an 'Answer Now' button that you can use if you missed a
        //   Nudge notification, or if you want to add a data point immediately"
        // RecordAnswerUseCase serves both notification-driven and "Answer Now" flows
        val (nudgeId, questionId) = createYesNoNudge()

        val answerId = recordAnswer.execute(nudgeId, questionId, "YES")

        val answers = repos.answerRepository.getAllByNudgeId(nudgeId)
        assertEquals(1, answers.size)
        assertEquals(answerId, answers[0].id)
        assertEquals("YES", answers[0].value)
    }
}
