package com.nudgery.shared

import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.model.TriggerOperator
import com.nudgery.shared.usecase.CreateNudgeRequest
import com.nudgery.shared.usecase.CreateNudgeResult
import com.nudgery.shared.usecase.CreateNudgeUseCase
import com.nudgery.shared.usecase.QuestionRequest
import com.nudgery.shared.usecase.ScheduleRequest
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QuestionSetupTest {

    private lateinit var repos: TestRepositories
    private lateinit var createNudge: CreateNudgeUseCase

    @BeforeTest
    fun setup() {
        repos = createTestRepositories()
        createNudge = CreateNudgeUseCase(
            repos.nudgeRepository,
            repos.questionRepository,
            repos.questionOptionRepository,
            repos.scheduleRepository,
            FakeNotificationScheduler()
        )
    }

    private fun dailySchedule() = ScheduleRequest(
        type = ScheduleType.DAILY,
        timeOfDay = LocalTime(12, 0),
        activeDaysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
    )

    @Test
    fun TDD_mainQuestionCannotBeTextType() = runTest {
        // README "Setting Up a Nudge": "Follow-up questions can be any of the main question types,
        //   plus a freeform Text type" — TEXT is only valid as a follow-up
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Write your thoughts", QuestionType.TEXT),
                schedule = dailySchedule()
            )
        )
        assertIs<CreateNudgeResult.Failure.MainQuestionCannotBeText>(result)
    }

    @Test
    fun TDD_followUpQuestionCanBeYesNo() = runTest {
        // README "Setting Up a Nudge": "Follow-up questions can be any of the main question types..."
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Did you exercise?", QuestionType.YES_NO),
                followUpQuestions = listOf(
                    QuestionRequest(
                        text = "Did you enjoy it?",
                        type = QuestionType.YES_NO,
                        triggerAnswerValue = "YES",
                        triggerOperator = TriggerOperator.EQ
                    )
                ),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val questions = repos.questionRepository.getByNudgeId(result.nudgeId)
        assertEquals(2, questions.size)
        val followUp = questions.first { !it.isMainQuestion }
        assertEquals(QuestionType.YES_NO, followUp.type)
    }

    @Test
    fun TDD_followUpQuestionCanBeNumber() = runTest {
        // README "Setting Up a Nudge": "Follow-up questions can be any of the main question types..."
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Had a headache?", QuestionType.YES_NO),
                followUpQuestions = listOf(
                    QuestionRequest(
                        text = "Rate the pain 1-10",
                        type = QuestionType.NUMBER,
                        triggerAnswerValue = "YES",
                        triggerOperator = TriggerOperator.EQ
                    )
                ),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val questions = repos.questionRepository.getByNudgeId(result.nudgeId)
        val followUp = questions.first { !it.isMainQuestion }
        assertEquals(QuestionType.NUMBER, followUp.type)
    }

    @Test
    fun TDD_followUpQuestionCanBeOptionSingle() = runTest {
        // README "Setting Up a Nudge": "Follow-up questions can be any of the main question types..."
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Did you dream?", QuestionType.YES_NO),
                followUpQuestions = listOf(
                    QuestionRequest(
                        text = "What kind of dream?",
                        type = QuestionType.OPTION_SINGLE,
                        options = listOf("Good", "Neutral", "Nightmare"),
                        triggerAnswerValue = "YES",
                        triggerOperator = TriggerOperator.EQ
                    )
                ),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val questions = repos.questionRepository.getByNudgeId(result.nudgeId)
        val followUp = questions.first { !it.isMainQuestion }
        assertEquals(QuestionType.OPTION_SINGLE, followUp.type)
    }

    @Test
    fun TDD_followUpQuestionCanBeOptionMulti() = runTest {
        // README "Setting Up a Nudge": "Follow-up questions can be any of the main question types..."
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Did you feel unwell?", QuestionType.YES_NO),
                followUpQuestions = listOf(
                    QuestionRequest(
                        text = "Which symptoms?",
                        type = QuestionType.OPTION_MULTI,
                        options = listOf("Headache", "Fatigue", "Nausea"),
                        triggerAnswerValue = "YES",
                        triggerOperator = TriggerOperator.EQ
                    )
                ),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val questions = repos.questionRepository.getByNudgeId(result.nudgeId)
        val followUp = questions.first { !it.isMainQuestion }
        assertEquals(QuestionType.OPTION_MULTI, followUp.type)
    }

    @Test
    fun TDD_followUpQuestionCanBeText() = runTest {
        // README "Setting Up a Nudge": "...plus a freeform Text type"
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Boss annoyance score?", QuestionType.NUMBER),
                followUpQuestions = listOf(
                    QuestionRequest(
                        text = "Any notes?",
                        type = QuestionType.TEXT,
                        triggerAnswerValue = "7",
                        triggerOperator = TriggerOperator.GTE
                    )
                ),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val questions = repos.questionRepository.getByNudgeId(result.nudgeId)
        val followUp = questions.first { !it.isMainQuestion }
        assertEquals(QuestionType.TEXT, followUp.type)
    }

    @Test
    fun TDD_followUpTriggeredByExactAnswerValue() = runTest {
        // README "Setting Up a Nudge": "you will be able to add follow-up questions for specific answers"
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Did you exercise?", QuestionType.YES_NO),
                followUpQuestions = listOf(
                    QuestionRequest(
                        text = "For how long?",
                        type = QuestionType.NUMBER,
                        triggerAnswerValue = "YES",
                        triggerOperator = TriggerOperator.EQ
                    )
                ),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val questions = repos.questionRepository.getByNudgeId(result.nudgeId)
        val followUp = questions.first { !it.isMainQuestion }
        assertEquals("YES", followUp.triggerAnswerValue)
        assertEquals(TriggerOperator.EQ, followUp.triggerOperator)
    }

    @Test
    fun TDD_followUpTriggeredByAnswerRange() = runTest {
        // README "Setting Up a Nudge": "Follow-up questions for specific answers or ranges of answers"
        // ARCHITECTURE.md Question.triggerOperator: "EQ, GTE, LTE, etc. Allows range-based follow-up triggers"
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Boss annoyance score (1-10)?", QuestionType.NUMBER),
                followUpQuestions = listOf(
                    QuestionRequest(
                        text = "Brief notes?",
                        type = QuestionType.TEXT,
                        triggerAnswerValue = "7",
                        triggerOperator = TriggerOperator.GTE
                    )
                ),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val questions = repos.questionRepository.getByNudgeId(result.nudgeId)
        val followUp = questions.first { !it.isMainQuestion }
        assertEquals("7", followUp.triggerAnswerValue)
        assertEquals(TriggerOperator.GTE, followUp.triggerOperator)
    }

    @Test
    fun TDD_mainQuestionHasOrderIndexZero() = runTest {
        // ARCHITECTURE.md Question.orderIndex: "0 = main question"
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Main question?", QuestionType.YES_NO),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val questions = repos.questionRepository.getByNudgeId(result.nudgeId)
        val mainQuestion = questions.first { it.isMainQuestion }
        assertEquals(0, mainQuestion.orderIndex)
    }

    @Test
    fun TDD_followUpQuestionsHaveOrderIndexGreaterThanZero() = runTest {
        // ARCHITECTURE.md Question.orderIndex: "subsequent questions are follow-ups"
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("How was your day?", QuestionType.NUMBER),
                followUpQuestions = listOf(
                    QuestionRequest(
                        text = "First follow-up",
                        type = QuestionType.TEXT,
                        triggerAnswerValue = "1",
                        triggerOperator = TriggerOperator.GTE
                    ),
                    QuestionRequest(
                        text = "Second follow-up",
                        type = QuestionType.TEXT,
                        triggerAnswerValue = "1",
                        triggerOperator = TriggerOperator.GTE
                    )
                ),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val questions = repos.questionRepository.getByNudgeId(result.nudgeId)
        val followUps = questions.filter { !it.isMainQuestion }
        assertEquals(2, followUps.size)
        assertTrue(followUps.all { it.orderIndex > 0 })
        assertEquals(setOf(1, 2), followUps.map { it.orderIndex }.toSet())
    }
}
