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
    fun TDD_mainQuestionCanBeTextType() = runTest {
        // README "Setting Up a Nudge": free Text is now a valid main question type
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Write your thoughts", QuestionType.TEXT),
                schedule = dailySchedule()
            )
        )
        assertIs<CreateNudgeResult.Success>(result)
    }

    @Test
    fun TDD_textMainQuestionCannotHaveFollowUps() = runTest {
        // README "Setting Up a Nudge": a Text main question has no answer conditions to branch on,
        //   so it cannot have follow-up questions
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Write your thoughts", QuestionType.TEXT),
                followUpQuestions = listOf(
                    QuestionRequest("A follow-up", QuestionType.YES_NO)
                ),
                schedule = dailySchedule()
            )
        )
        assertIs<CreateNudgeResult.Failure.FreeformMainCannotHaveFollowUps>(result)
    }

    @Test
    fun TDD_emojiMainQuestionCannotHaveFollowUps() = runTest {
        // README "Setting Up a Nudge": "anything that isn't free text or emoji" can have follow-ups —
        //   an EMOJI main is free-form (ED-1), so like TEXT it cannot.
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("How do you feel?", QuestionType.EMOJI),
                followUpQuestions = listOf(
                    QuestionRequest("A follow-up", QuestionType.YES_NO)
                ),
                schedule = dailySchedule()
            )
        )
        assertIs<CreateNudgeResult.Failure.FreeformMainCannotHaveFollowUps>(result)
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
    fun TDD_scaleQuestion_storesAndRetrievesMinAndMax() = runTest {
        // SCALE questions have a user-defined integer range; both bounds must round-trip through the DB
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Rate your focus today?", QuestionType.SCALE, scaleMin = 1, scaleMax = 7),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val questions = repos.questionRepository.getByNudgeId(result.nudgeId)
        val main = questions.first { it.isMainQuestion }
        assertEquals(QuestionType.SCALE, main.type)
        assertEquals(1, main.scaleMin)
        assertEquals(7, main.scaleMax)
    }

    @Test
    fun TDD_scaleQuestion_invalidRangeIsRejected() = runTest {
        // scaleMin must be strictly less than scaleMax; equal values have no valid positions
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Rate your focus?", QuestionType.SCALE, scaleMin = 5, scaleMax = 5),
                schedule = dailySchedule()
            )
        )
        assertIs<CreateNudgeResult.Failure.InvalidScaleRange>(result)
    }

    @Test
    fun TDD_scaleQuestion_minGreaterThanMaxIsRejected() = runTest {
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Rate your focus?", QuestionType.SCALE, scaleMin = 10, scaleMax = 1),
                schedule = dailySchedule()
            )
        )
        assertIs<CreateNudgeResult.Failure.InvalidScaleRange>(result)
    }

    @Test
    fun TDD_scaleQuestion_defaultsAreZeroToTen() = runTest {
        // Existing SCALE questions migrated from the old NUMBER type default to 0–10
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Rate your day?", QuestionType.SCALE),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val main = repos.questionRepository.getByNudgeId(result.nudgeId).first { it.isMainQuestion }
        assertEquals(0, main.scaleMin)
        assertEquals(10, main.scaleMax)
    }

    @Test
    fun TDD_scaleQuestion_isValidForMainQuestion() = runTest {
        // SCALE is not TEXT, so it should be valid as a main question
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("How tired are you?", QuestionType.SCALE, scaleMin = 0, scaleMax = 5),
                schedule = dailySchedule()
            )
        )
        assertIs<CreateNudgeResult.Success>(result)
    }

    @Test
    fun TDD_numberQuestion_isValidAsFollowUp() = runTest {
        // NUMBER (free-form decimal) is a valid follow-up type
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Did you exercise?", QuestionType.YES_NO),
                followUpQuestions = listOf(
                    QuestionRequest(
                        text = "How many km did you run?",
                        type = QuestionType.NUMBER,
                        triggerAnswerValue = "YES",
                        triggerOperator = TriggerOperator.EQ
                    )
                ),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val followUp = repos.questionRepository.getByNudgeId(result.nudgeId).first { !it.isMainQuestion }
        assertEquals(QuestionType.NUMBER, followUp.type)
    }

    @Test
    fun TDD_numberQuestion_hasNoScaleBounds() = runTest {
        // NUMBER (free-form decimal) does not carry scaleMin/scaleMax — those are SCALE-only
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("How many km did you run?", QuestionType.NUMBER),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val main = repos.questionRepository.getByNudgeId(result.nudgeId).first { it.isMainQuestion }
        assertEquals(null, main.scaleMin)
        assertEquals(null, main.scaleMax)
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
