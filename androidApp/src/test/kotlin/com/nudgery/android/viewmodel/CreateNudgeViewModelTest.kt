package com.nudgery.android.viewmodel

import com.nudgery.android.util.TestViewModelRepositories
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.usecase.CreateNudgeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateNudgeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repos: TestViewModelRepositories
    private lateinit var viewModel: CreateNudgeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repos = TestViewModelRepositories()
        viewModel = CreateNudgeViewModel(createNudge = repos.createNudgeUseCase())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun TDD_submitCreatesNudgeWithMainQuestion() = runTest {
        // README "Setting Up a Nudge": "This will bring up a screen that lets you write the
        //   main question"
        viewModel.setMainQuestion(QuestionFormState(text = "Did you exercise?", type = QuestionType.YES_NO))
        viewModel.setSchedule(dailyScheduleForm())

        viewModel.submit()
        advanceUntilIdle()

        val result = viewModel.formState.value.result
        assertTrue("Submit should succeed", result is CreateNudgeResult.Success)
    }

    @Test
    fun TDD_mainQuestionTypeCannotBeText() = runTest {
        // README: "choose what kind of answer you want with the main question
        //   (Yes or No, Number, Option (Single), or Option (Multi))" — TEXT is follow-up only
        viewModel.setMainQuestion(QuestionFormState(text = "Notes?", type = QuestionType.TEXT))
        viewModel.setSchedule(dailyScheduleForm())

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(CreateNudgeResult.Failure.MainQuestionCannotBeText, viewModel.formState.value.result)
    }

    @Test
    fun TDD_submitWithOptionsStoresOptions() = runTest {
        // README: "For the Option types, you will be prompted here to add up to 16 selectable answers."
        viewModel.setMainQuestion(
            QuestionFormState(
                text = "How was your commute?",
                type = QuestionType.OPTION_SINGLE,
                options = listOf("Great", "Fine", "Awful")
            )
        )
        viewModel.setSchedule(dailyScheduleForm())

        viewModel.submit()
        advanceUntilIdle()

        val nudgeId = (viewModel.formState.value.result as CreateNudgeResult.Success).nudgeId
        val questions = repos.questionRepo.getByNudgeId(nudgeId)
        val options = repos.optionRepo.getByQuestionId(questions.first().id)
        assertEquals(3, options.size)
        assertEquals(listOf("Great", "Fine", "Awful"), options.map { it.text })
    }

    @Test
    fun TDD_addFollowUpQuestionIncludesItInForm() = runTest {
        // README: "you will be able to add follow-up questions for specific answers"
        viewModel.addFollowUpQuestion(
            QuestionFormState(
                text = "Any notes?",
                type = QuestionType.TEXT,
                triggerAnswerValue = "NO"
            )
        )

        assertEquals(1, viewModel.formState.value.followUpQuestions.size)
        assertEquals("Any notes?", viewModel.formState.value.followUpQuestions.first().text)
    }

    @Test
    fun TDD_submitWithFollowUpPersistsFollowUpQuestion() = runTest {
        // README: "you will be able to add follow-up questions for specific answers"
        viewModel.setMainQuestion(QuestionFormState(text = "Did you exercise?", type = QuestionType.YES_NO))
        viewModel.addFollowUpQuestion(QuestionFormState(text = "What did you do?", type = QuestionType.TEXT))
        viewModel.setSchedule(dailyScheduleForm())

        viewModel.submit()
        advanceUntilIdle()

        val nudgeId = (viewModel.formState.value.result as CreateNudgeResult.Success).nudgeId
        val questions = repos.questionRepo.getByNudgeId(nudgeId)
        assertEquals(2, questions.size)
        assertTrue("Follow-up question should be persisted", questions.any { !it.isMainQuestion })
    }

    @Test
    fun TDD_disabledNudgeDoesNotScheduleNotifications() = runTest {
        // README: "Enabled Nudges will send you notifications" — disabled on creation must not schedule
        viewModel.setEnabled(false)
        viewModel.setMainQuestion(QuestionFormState(text = "Mood?", type = QuestionType.YES_NO))
        viewModel.setSchedule(dailyScheduleForm())

        viewModel.submit()
        advanceUntilIdle()

        assertTrue("Disabled nudge must not enqueue notifications", repos.scheduler.scheduled.isEmpty())
    }

    private fun dailyScheduleForm() = ScheduleFormState(
        type = ScheduleType.DAILY,
        timeOfDay = LocalTime(12, 0),
        activeDaysOfWeek = DayOfWeek.entries.toSet()
    )
}
