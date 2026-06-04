package com.nudgery.shared

import com.nudgery.shared.model.ExportFormat
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.model.TriggerOperator
import com.nudgery.shared.usecase.CreateNudgeRequest
import com.nudgery.shared.usecase.CreateNudgeResult
import com.nudgery.shared.usecase.CreateNudgeUseCase
import com.nudgery.shared.usecase.ExportAnswersUseCase
import com.nudgery.shared.usecase.ImportAnswerRequest
import com.nudgery.shared.usecase.ImportNudgeRequest
import com.nudgery.shared.usecase.ImportNudgeUseCase
import com.nudgery.shared.usecase.ImportQuestionRequest
import com.nudgery.shared.usecase.QuestionRequest
import com.nudgery.shared.usecase.ScheduleRequest
import com.nudgery.shared.util.FakeNotificationScheduler
import com.nudgery.shared.util.TestRepositories
import com.nudgery.shared.util.createTestRepositories
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NudgeImportTest {

    private lateinit var repos: TestRepositories
    private lateinit var importNudge: ImportNudgeUseCase
    private lateinit var createNudge: CreateNudgeUseCase
    private lateinit var exportAnswers: ExportAnswersUseCase

    @BeforeTest
    fun setup() {
        repos = createTestRepositories()
        val scheduler = FakeNotificationScheduler()
        importNudge = ImportNudgeUseCase(
            repos.nudgeRepository, repos.questionRepository, repos.questionOptionRepository,
            repos.scheduleRepository, repos.answerRepository, scheduler
        )
        createNudge = CreateNudgeUseCase(
            repos.nudgeRepository, repos.questionRepository, repos.questionOptionRepository,
            repos.scheduleRepository, scheduler
        )
        exportAnswers = ExportAnswersUseCase(
            repos.nudgeRepository, repos.questionRepository, repos.questionOptionRepository,
            repos.answerRepository, repos.scheduleRepository
        )
    }

    private fun dailyScheduleRequest() = ScheduleRequest(
        type = ScheduleType.DAILY,
        timeOfDay = LocalTime(9, 0),
        activeDaysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
    )

    @Test
    fun TDD_importTrimsTextFields() = runTest {
        // ENGINEERING_DECISIONS.md ED-16: import is a save boundary too — restored name, question
        // text, options, and answer values are trimmed on the way in.
        val request = ImportNudgeRequest(
            name = "  Imported 🐶  ",
            isEnabled = true,
            schedule = dailyScheduleRequest(),
            questions = listOf(
                ImportQuestionRequest(
                    orderIndex = 0,
                    text = "  How do you feel?  ",
                    type = QuestionType.OPTION_SINGLE,
                    options = listOf("  Good  ", "  Bad  ")
                )
            ),
            answers = listOf(
                ImportAnswerRequest(
                    questionOrderIndex = 0,
                    value = "Good",
                    scheduledAt = Clock.System.now(),
                    answeredAt = Clock.System.now()
                )
            )
        )

        val nudgeId = importNudge.execute(request)

        val nudge = repos.nudgeRepository.getById(nudgeId)
        assertNotNull(nudge)
        assertEquals("Imported 🐶", nudge.name)
        val question = repos.questionRepository.getByNudgeId(nudgeId).first { it.isMainQuestion }
        assertEquals("How do you feel?", question.text)
        val options = repos.questionOptionRepository.getByQuestionId(question.id).sortedBy { it.orderIndex }
        assertEquals(listOf("Good", "Bad"), options.map { it.text })
    }

    @Test
    fun TDD_import_createsNudgeWithCorrectName() = runTest {
        // README: import recreates the nudge from a JSON backup
        val request = ImportNudgeRequest(
            name = "My Imported Nudge",
            isEnabled = true,
            schedule = dailyScheduleRequest(),
            questions = listOf(
                ImportQuestionRequest(orderIndex = 0, text = "How are you?", type = QuestionType.YES_NO)
            ),
            answers = emptyList()
        )

        val nudgeId = importNudge.execute(request)

        val nudge = repos.nudgeRepository.getById(nudgeId)
        assertNotNull(nudge)
        assertEquals("My Imported Nudge", nudge.name)
    }

    @Test
    fun TDD_import_preservesEnabledState() = runTest {
        // Disabled nudges should stay disabled after import
        val request = ImportNudgeRequest(
            name = "Paused Nudge",
            isEnabled = false,
            schedule = dailyScheduleRequest(),
            questions = listOf(
                ImportQuestionRequest(orderIndex = 0, text = "Check?", type = QuestionType.YES_NO)
            ),
            answers = emptyList()
        )

        val nudgeId = importNudge.execute(request)

        val nudge = repos.nudgeRepository.getById(nudgeId)
        assertNotNull(nudge)
        assertEquals(false, nudge.isEnabled)
    }

    @Test
    fun TDD_import_createsMainQuestionWithCorrectText() = runTest {
        // Question text must be preserved exactly
        val request = ImportNudgeRequest(
            name = "Exercise",
            isEnabled = true,
            schedule = dailyScheduleRequest(),
            questions = listOf(
                ImportQuestionRequest(orderIndex = 0, text = "Did you exercise today?", type = QuestionType.YES_NO)
            ),
            answers = emptyList()
        )

        val nudgeId = importNudge.execute(request)

        val questions = repos.questionRepository.getByNudgeId(nudgeId)
        assertEquals(1, questions.size)
        assertEquals("Did you exercise today?", questions[0].text)
        assertTrue(questions[0].isMainQuestion)
    }

    @Test
    fun TDD_import_createsScheduleWithCorrectType() = runTest {
        // Schedule type must be preserved so notifications fire on the right cadence
        val request = ImportNudgeRequest(
            name = "Weekly Check",
            isEnabled = true,
            schedule = ScheduleRequest(
                type = ScheduleType.WEEKLY,
                timeOfDay = LocalTime(18, 0),
                activeDaysOfWeek = setOf(DayOfWeek.SUNDAY)
            ),
            questions = listOf(
                ImportQuestionRequest(orderIndex = 0, text = "How was your week?", type = QuestionType.YES_NO)
            ),
            answers = emptyList()
        )

        val nudgeId = importNudge.execute(request)

        val schedule = repos.scheduleRepository.getByNudgeId(nudgeId)
        assertNotNull(schedule)
        assertEquals(ScheduleType.WEEKLY, schedule.type)
        assertEquals(LocalTime(18, 0), schedule.timeOfDay)
    }

    @Test
    fun TDD_import_withNoSchedule_doesNotCreateSchedule() = runTest {
        // A backup with null schedule should still import the nudge without a schedule
        val request = ImportNudgeRequest(
            name = "Unscheduled",
            isEnabled = false,
            schedule = null,
            questions = listOf(
                ImportQuestionRequest(orderIndex = 0, text = "Anything?", type = QuestionType.YES_NO)
            ),
            answers = emptyList()
        )

        val nudgeId = importNudge.execute(request)

        val schedule = repos.scheduleRepository.getByNudgeId(nudgeId)
        assertNull(schedule)
    }

    @Test
    fun TDD_import_preservesScaleBoundsOnScaleQuestion() = runTest {
        // SCALE questions carry user-defined min/max; these must survive round-trip
        val request = ImportNudgeRequest(
            name = "Energy",
            isEnabled = true,
            schedule = dailyScheduleRequest(),
            questions = listOf(
                ImportQuestionRequest(
                    orderIndex = 0, text = "Rate your energy", type = QuestionType.SCALE,
                    scaleMin = 1, scaleMax = 7
                )
            ),
            answers = emptyList()
        )

        val nudgeId = importNudge.execute(request)

        val question = repos.questionRepository.getByNudgeId(nudgeId).first { it.isMainQuestion }
        assertEquals(1, question.scaleMin)
        assertEquals(7, question.scaleMax)
    }

    @Test
    fun TDD_import_createsOptionTexts() = runTest {
        // Option texts must be recreated with new IDs so answers can be recorded normally
        val request = ImportNudgeRequest(
            name = "Mood",
            isEnabled = true,
            schedule = dailyScheduleRequest(),
            questions = listOf(
                ImportQuestionRequest(
                    orderIndex = 0, text = "How are you feeling?",
                    type = QuestionType.OPTION_SINGLE,
                    options = listOf("Great", "Fine", "Tired")
                )
            ),
            answers = emptyList()
        )

        val nudgeId = importNudge.execute(request)

        val question = repos.questionRepository.getByNudgeId(nudgeId).first { it.isMainQuestion }
        val options = repos.questionOptionRepository.getByQuestionId(question.id)
        val texts = options.map { it.text }
        assertEquals(3, options.size)
        assertTrue(texts.contains("Great"))
        assertTrue(texts.contains("Fine"))
        assertTrue(texts.contains("Tired"))
    }

    @Test
    fun TDD_import_preservesAnswers() = runTest {
        // Historical answers must be imported so charts populate immediately
        val now = Clock.System.now()
        val request = ImportNudgeRequest(
            name = "Sleep",
            isEnabled = true,
            schedule = dailyScheduleRequest(),
            questions = listOf(
                ImportQuestionRequest(orderIndex = 0, text = "Did you sleep well?", type = QuestionType.YES_NO)
            ),
            answers = listOf(
                ImportAnswerRequest(
                    questionOrderIndex = 0,
                    value = "YES",
                    scheduledAt = Instant.fromEpochSeconds(1_700_000_000),
                    answeredAt = Instant.fromEpochSeconds(1_700_000_060)
                ),
                ImportAnswerRequest(
                    questionOrderIndex = 0,
                    value = "NO",
                    scheduledAt = Instant.fromEpochSeconds(1_700_086_400),
                    answeredAt = Instant.fromEpochSeconds(1_700_086_460)
                )
            )
        )

        val nudgeId = importNudge.execute(request)

        val answers = repos.answerRepository.getAllByNudgeId(nudgeId)
        assertEquals(2, answers.size)
        val values = answers.map { it.value }.toSet()
        assertTrue(values.contains("YES"))
        assertTrue(values.contains("NO"))
    }

    @Test
    fun TDD_import_resolvesOptionTextToNewOptionId() = runTest {
        // Option answers in the backup store option text; import must resolve to the new option UUID
        val request = ImportNudgeRequest(
            name = "Mood",
            isEnabled = true,
            schedule = dailyScheduleRequest(),
            questions = listOf(
                ImportQuestionRequest(
                    orderIndex = 0, text = "How are you?",
                    type = QuestionType.OPTION_SINGLE,
                    options = listOf("Happy", "Neutral", "Sad")
                )
            ),
            answers = listOf(
                ImportAnswerRequest(
                    questionOrderIndex = 0,
                    value = "Happy",
                    scheduledAt = Instant.fromEpochSeconds(1_700_000_000),
                    answeredAt = Instant.fromEpochSeconds(1_700_000_060)
                )
            )
        )

        val nudgeId = importNudge.execute(request)

        val question = repos.questionRepository.getByNudgeId(nudgeId).first { it.isMainQuestion }
        val happyOption = repos.questionOptionRepository.getByQuestionId(question.id).first { it.text == "Happy" }
        val answers = repos.answerRepository.getAllByNudgeId(nudgeId)

        assertEquals(1, answers.size)
        assertEquals(happyOption.id, answers[0].value, "Imported option answer should use the new option UUID")
    }

    @Test
    fun TDD_import_preservesFollowUpWithTrigger() = runTest {
        // Follow-up questions with YES/NO triggers must be reconstructed with the correct trigger condition
        val request = ImportNudgeRequest(
            name = "Exercise",
            isEnabled = true,
            schedule = dailyScheduleRequest(),
            questions = listOf(
                ImportQuestionRequest(orderIndex = 0, text = "Did you exercise?", type = QuestionType.YES_NO),
                ImportQuestionRequest(
                    orderIndex = 1, text = "For how long?", type = QuestionType.SCALE,
                    scaleMin = 0, scaleMax = 120,
                    triggerOperator = TriggerOperator.EQ,
                    triggerAnswerValue = "YES"
                )
            ),
            answers = emptyList()
        )

        val nudgeId = importNudge.execute(request)

        val questions = repos.questionRepository.getByNudgeId(nudgeId).sortedBy { it.orderIndex }
        assertEquals(2, questions.size)
        val followUp = questions[1]
        assertEquals("For how long?", followUp.text)
        assertEquals(TriggerOperator.EQ, followUp.triggerOperator)
        assertEquals("YES", followUp.triggerAnswerValue)
    }

    @Test
    fun TDD_import_followUpWithOptionTrigger_resolvesToNewOptionId() = runTest {
        // When the main question is option-type, follow-up trigger values are option texts in the
        // backup; they must be resolved to new option UUIDs on import
        val request = ImportNudgeRequest(
            name = "Mood Detail",
            isEnabled = true,
            schedule = dailyScheduleRequest(),
            questions = listOf(
                ImportQuestionRequest(
                    orderIndex = 0, text = "How are you?",
                    type = QuestionType.OPTION_SINGLE,
                    options = listOf("Great", "Fine", "Bad")
                ),
                ImportQuestionRequest(
                    orderIndex = 1, text = "What's wrong?",
                    type = QuestionType.TEXT,
                    triggerOperator = TriggerOperator.EQ,
                    triggerAnswerValue = "Bad"
                )
            ),
            answers = emptyList()
        )

        val nudgeId = importNudge.execute(request)

        val questions = repos.questionRepository.getByNudgeId(nudgeId).sortedBy { it.orderIndex }
        val mainQuestion = questions[0]
        val followUp = questions[1]

        val badOption = repos.questionOptionRepository.getByQuestionId(mainQuestion.id)
            .first { it.text == "Bad" }
        assertEquals(badOption.id, followUp.triggerAnswerValue,
            "Follow-up trigger should reference new option UUID, not text")
    }

    @Test
    fun TDD_import_answersLinkedToCorrectQuestion() = runTest {
        // Multi-question nudge: each answer's questionId must map to the right question
        val request = ImportNudgeRequest(
            name = "Exercise",
            isEnabled = true,
            schedule = dailyScheduleRequest(),
            questions = listOf(
                ImportQuestionRequest(orderIndex = 0, text = "Did you exercise?", type = QuestionType.YES_NO),
                ImportQuestionRequest(
                    orderIndex = 1, text = "For how long?", type = QuestionType.NUMBER,
                    triggerOperator = TriggerOperator.EQ, triggerAnswerValue = "YES"
                )
            ),
            answers = listOf(
                ImportAnswerRequest(
                    questionOrderIndex = 0, value = "YES",
                    scheduledAt = Instant.fromEpochSeconds(1_700_000_000),
                    answeredAt = Instant.fromEpochSeconds(1_700_000_060)
                ),
                ImportAnswerRequest(
                    questionOrderIndex = 1, value = "30",
                    scheduledAt = Instant.fromEpochSeconds(1_700_000_000),
                    answeredAt = Instant.fromEpochSeconds(1_700_000_090)
                )
            )
        )

        val nudgeId = importNudge.execute(request)

        val questions = repos.questionRepository.getByNudgeId(nudgeId).sortedBy { it.orderIndex }
        val answers = repos.answerRepository.getAllByNudgeId(nudgeId)
        assertEquals(2, answers.size)

        val mainAnswer = answers.first { it.value == "YES" }
        val followUpAnswer = answers.first { it.value == "30" }
        assertEquals(questions[0].id, mainAnswer.questionId, "YES answer should link to main question")
        assertEquals(questions[1].id, followUpAnswer.questionId, "30 answer should link to follow-up question")
    }

    @Test
    fun TDD_import_schedulesNotificationsWhenEnabled() = runTest {
        // An enabled nudge with a schedule should have its notification scheduled on import
        val scheduler = FakeNotificationScheduler()
        val importWithScheduler = ImportNudgeUseCase(
            repos.nudgeRepository, repos.questionRepository, repos.questionOptionRepository,
            repos.scheduleRepository, repos.answerRepository, scheduler
        )

        val request = ImportNudgeRequest(
            name = "Enabled Nudge",
            isEnabled = true,
            schedule = dailyScheduleRequest(),
            questions = listOf(
                ImportQuestionRequest(orderIndex = 0, text = "All good?", type = QuestionType.YES_NO)
            ),
            answers = emptyList()
        )

        importWithScheduler.execute(request)

        assertEquals(1, scheduler.scheduled.size, "Notification should be scheduled for enabled nudge on import")
    }

    @Test
    fun TDD_import_doesNotScheduleNotificationsWhenDisabled() = runTest {
        // A disabled nudge should not trigger notification scheduling
        val scheduler = FakeNotificationScheduler()
        val importWithScheduler = ImportNudgeUseCase(
            repos.nudgeRepository, repos.questionRepository, repos.questionOptionRepository,
            repos.scheduleRepository, repos.answerRepository, scheduler
        )

        val request = ImportNudgeRequest(
            name = "Disabled Nudge",
            isEnabled = false,
            schedule = dailyScheduleRequest(),
            questions = listOf(
                ImportQuestionRequest(orderIndex = 0, text = "All good?", type = QuestionType.YES_NO)
            ),
            answers = emptyList()
        )

        importWithScheduler.execute(request)

        assertEquals(0, scheduler.scheduled.size, "Notification should not be scheduled for disabled nudge")
    }
}
