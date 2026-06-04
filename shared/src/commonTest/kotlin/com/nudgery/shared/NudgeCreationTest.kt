package com.nudgery.shared

import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.usecase.CreateNudgeRequest
import com.nudgery.shared.usecase.CreateNudgeResult
import com.nudgery.shared.usecase.CreateNudgeUseCase
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
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NudgeCreationTest {

    private lateinit var repos: TestRepositories
    private lateinit var fakeScheduler: FakeNotificationScheduler
    private lateinit var createNudge: CreateNudgeUseCase

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
    }

    private fun dailySchedule() = ScheduleRequest(
        type = ScheduleType.DAILY,
        timeOfDay = LocalTime(12, 0),
        activeDaysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
    )

    @Test
    fun TDD_nudgeNameTrimmedOnCreate() = runTest {
        // ENGINEERING_DECISIONS.md ED-16: user-typed text is trimmed at the save boundary, so a
        // trailing space (e.g. from keyboard autocomplete) can't reach storage and break, say, the
        // emoji-only detection that drives emoji scaling.
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Did you sleep well?", QuestionType.YES_NO),
                schedule = dailySchedule(),
                name = "  🐶  "
            )
        )
        assertIs<CreateNudgeResult.Success>(result)
        val nudge = repos.nudgeRepository.getById(result.nudgeId)
        assertNotNull(nudge)
        assertEquals("🐶", nudge.name)
    }

    @Test
    fun TDD_questionAndOptionTextTrimmedOnCreate() = runTest {
        // ED-16: question text and option text are trimmed at save time, too.
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest(
                    "  How do you feel?  ",
                    QuestionType.OPTION_SINGLE,
                    options = listOf("  Good  ", "  Bad  ")
                ),
                schedule = dailySchedule()
            )
        )
        assertIs<CreateNudgeResult.Success>(result)
        val question = repos.questionRepository.getByNudgeId(result.nudgeId).first { it.isMainQuestion }
        assertEquals("How do you feel?", question.text)
        val options = repos.questionOptionRepository.getByQuestionId(question.id).sortedBy { it.orderIndex }
        assertEquals(listOf("Good", "Bad"), options.map { it.text })
    }

    @Test
    fun TDD_blankNameFallsBackToQuestionText() = runTest {
        // ED-16: a name that is only whitespace collapses to "absent", so the nudge falls back to
        // its (trimmed) question text rather than being saved as a blank name.
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("  Did you stretch?  ", QuestionType.YES_NO),
                schedule = dailySchedule(),
                name = "   "
            )
        )
        assertIs<CreateNudgeResult.Success>(result)
        val nudge = repos.nudgeRepository.getById(result.nudgeId)
        assertNotNull(nudge)
        assertEquals("Did you stretch?", nudge.name)
    }

    @Test
    fun TDD_createNudgeWithYesNoQuestion() = runTest {
        // README "Setting Up a Nudge": "choose what kind of answer you want with the main question (Yes or No...)"
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Did you sleep well?", QuestionType.YES_NO),
                schedule = dailySchedule()
            )
        )
        assertIs<CreateNudgeResult.Success>(result)
        val questions = repos.questionRepository.getByNudgeId(result.nudgeId)
        assertEquals(1, questions.size)
        assertEquals(QuestionType.YES_NO, questions[0].type)
    }

    @Test
    fun TDD_createNudgeWithNumberQuestion() = runTest {
        // README "Setting Up a Nudge": "...Number..."
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("How many hours did you sleep?", QuestionType.NUMBER),
                schedule = dailySchedule()
            )
        )
        assertIs<CreateNudgeResult.Success>(result)
        val questions = repos.questionRepository.getByNudgeId(result.nudgeId)
        assertEquals(QuestionType.NUMBER, questions[0].type)
    }

    @Test
    fun TDD_createNudgeWithOptionSingleQuestion() = runTest {
        // README "Setting Up a Nudge": "...Option (Single)..."
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest(
                    text = "How do you feel?",
                    type = QuestionType.OPTION_SINGLE,
                    options = listOf("Great", "Good", "Bad")
                ),
                schedule = dailySchedule()
            )
        )
        assertIs<CreateNudgeResult.Success>(result)
        val questions = repos.questionRepository.getByNudgeId(result.nudgeId)
        assertEquals(QuestionType.OPTION_SINGLE, questions[0].type)
        val options = repos.questionOptionRepository.getByQuestionId(questions[0].id)
        assertEquals(3, options.size)
    }

    @Test
    fun TDD_createNudgeWithOptionMultiQuestion() = runTest {
        // README "Setting Up a Nudge": "...Option (Multi)"
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest(
                    text = "Which symptoms do you have?",
                    type = QuestionType.OPTION_MULTI,
                    options = listOf("Headache", "Fatigue", "Nausea")
                ),
                schedule = dailySchedule()
            )
        )
        assertIs<CreateNudgeResult.Success>(result)
        val questions = repos.questionRepository.getByNudgeId(result.nudgeId)
        assertEquals(QuestionType.OPTION_MULTI, questions[0].type)
        val options = repos.questionOptionRepository.getByQuestionId(questions[0].id)
        assertEquals(3, options.size)
    }

    @Test
    fun TDD_optionQuestionAllowsUpToSixteenOptions() = runTest {
        // README "Setting Up a Nudge": "you will be prompted here to add up to 16 selectable answers"
        val options = (1..16).map { "Option $it" }
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Pick one", QuestionType.OPTION_SINGLE, options),
                schedule = dailySchedule()
            )
        )
        assertIs<CreateNudgeResult.Success>(result)
        val questions = repos.questionRepository.getByNudgeId(result.nudgeId)
        val savedOptions = repos.questionOptionRepository.getByQuestionId(questions[0].id)
        assertEquals(16, savedOptions.size)
    }

    @Test
    fun TDD_optionQuestionRejectsMoreThanSixteenOptions() = runTest {
        // README "Setting Up a Nudge": "...up to 16 selectable answers"
        val options = (1..17).map { "Option $it" }
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Pick one", QuestionType.OPTION_SINGLE, options),
                schedule = dailySchedule()
            )
        )
        assertIs<CreateNudgeResult.Failure.TooManyOptions>(result)
    }

    @Test
    fun TDD_nudgeNameDefaultsToMainQuestionText() = runTest {
        // ARCHITECTURE.md Nudge.name: "Derived from main question text by default"
        val questionText = "Did you exercise today?"
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest(questionText, QuestionType.YES_NO),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val nudge = repos.nudgeRepository.getById(result.nudgeId)
        assertEquals(questionText, nudge!!.name)
    }

    @Test
    fun TDD_newNudgeIsEnabledByDefault() = runTest {
        // README "Viewing Nudges": "whether or not it is enabled" — new nudges should fire from the start
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Question?", QuestionType.YES_NO),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val nudge = repos.nudgeRepository.getById(result.nudgeId)
        assertTrue(nudge!!.isEnabled)
    }

    @Test
    fun TDD_savedNudgeAppearsInNudgeList() = runTest {
        // README "Setting Up a Nudge": "It will appear on the main screen in the list with the rest of your Nudges"
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Question?", QuestionType.YES_NO),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val nudges = repos.nudgeRepository.observeAll().first()
        assertTrue(nudges.any { it.id == result.nudgeId })
    }

    @Test
    fun TDD_nudgeListEntryShowsNameScheduleNextDateAndEnabledStatus() = runTest {
        // README "Setting Up a Nudge": "indicating the Nudge's name, schedule, next nudge date and time,
        //   and whether or not it is enabled"
        val nudgeName = "My Sleep Tracker"
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Question?", QuestionType.YES_NO),
                schedule = dailySchedule(),
                name = nudgeName,
                isEnabled = true
            )
        ) as CreateNudgeResult.Success

        val nudge = repos.nudgeRepository.getById(result.nudgeId)
        val schedule = repos.scheduleRepository.getByNudgeId(result.nudgeId)

        assertNotNull(nudge)
        assertNotNull(schedule)
        assertEquals(nudgeName, nudge.name)
        assertTrue(nudge.isEnabled)
        assertEquals(ScheduleType.DAILY, schedule.type)
    }
}
