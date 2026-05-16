package com.nudgery.android.viewmodel

import com.nudgery.android.util.TestViewModelRepositories
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.usecase.CreateNudgeRequest
import com.nudgery.shared.usecase.CreateNudgeResult
import com.nudgery.shared.usecase.CreateNudgeUseCase
import com.nudgery.shared.usecase.QuestionRequest
import com.nudgery.shared.usecase.ScheduleRequest
import com.nudgery.shared.usecase.UpdateNudgeResult
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
class EditNudgeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repos: TestViewModelRepositories

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repos = TestViewModelRepositories()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun TDD_editFormLoadsExistingNudgeData() = runTest {
        // README "Editing Nudges": "Nudge configuration be[sic] edited" — form must pre-populate
        val nudgeId = createNudge("Did you exercise?")
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()

        assertFalse(viewModel.formState.value.isLoading)
        assertEquals("Did you exercise?", viewModel.formState.value.mainQuestionText)
        assertEquals("Did you exercise?", viewModel.formState.value.originalMainQuestionText)
    }

    @Test
    fun TDD_editNudgeNameAndSubmit() = runTest {
        // README "Editing Nudges": "Nudge configuration be[sic] edited"
        val nudgeId = createNudge("Mood check?")
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()

        viewModel.setName("Daily mood tracker")
        viewModel.submit()  // no question text change → no dialog, submits directly
        advanceUntilIdle()

        val result = viewModel.formState.value.result
        assertTrue("Name edit should succeed", result is UpdateNudgeResult.Success)
        assertEquals("Daily mood tracker", repos.nudgeRepo.getById(nudgeId)?.name)
    }

    @Test
    fun TDD_editingQuestionTextSetsShowSplitDialogTrue() = runTest {
        // README "Editing Nudges": "you will be asked if you would prefer to split the Nudge
        //   instead of editing in place"
        val nudgeId = createNudge("Did you sleep well?")
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()

        viewModel.setMainQuestionText("Did you sleep 8 hours?")
        viewModel.submit()
        advanceUntilIdle()

        assertTrue("Editing question text should prompt split dialog",
            viewModel.formState.value.showSplitDialog)
    }

    @Test
    fun TDD_submitWithSplitEditCreatesNewNudge() = runTest {
        // README "Editing Nudges": "If you are changing the question enough... choose to split.
        //   The old version of the Nudge... will be preserved, and the old Nudge will be disabled."
        val nudgeId = createNudge("Did you eat veggies?")
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()

        viewModel.setMainQuestionText("Did you eat a salad?")
        viewModel.submit()      // shows split dialog
        viewModel.submitWithSplit()  // user chose "split"
        advanceUntilIdle()

        val result = viewModel.formState.value.result
        assertTrue("Split edit should succeed", result is UpdateNudgeResult.Success)
        val newNudgeId = (result as UpdateNudgeResult.Success).nudgeId
        assertFalse("New nudge should have a different ID", newNudgeId == nudgeId)
        assertFalse("Original nudge should be disabled after split",
            repos.nudgeRepo.getById(nudgeId)?.isEnabled ?: true)
    }

    @Test
    fun TDD_submitWithoutSplitEditUpdatesQuestionInPlace() = runTest {
        // README "Editing Nudges": "If the change to the question is not significant... choose not to split."
        val nudgeId = createNudge("Excercise today?") // deliberate typo
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()

        viewModel.setMainQuestionText("Exercise today?")
        viewModel.submit()       // shows split dialog
        viewModel.submitInPlace()  // user chose "edit in place"
        advanceUntilIdle()

        val result = viewModel.formState.value.result
        assertTrue("In-place edit should succeed", result is UpdateNudgeResult.Success)
        assertNotNull(repos.nudgeRepo.getById(nudgeId))
        val updatedText = repos.questionRepo.getByNudgeId(nudgeId).first { it.isMainQuestion }.text
        assertEquals("Exercise today?", updatedText)
    }

    @Test
    fun TDD_dismissSplitDialogHidesDialog() = runTest {
        // Defensive: dismissing split dialog should close it without submitting
        val nudgeId = createNudge("Are you happy?")
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()

        viewModel.setMainQuestionText("Are you happy today?")
        viewModel.submit()  // triggers dialog
        advanceUntilIdle()
        assertTrue(viewModel.formState.value.showSplitDialog)

        viewModel.dismissSplitDialog()

        assertFalse(viewModel.formState.value.showSplitDialog)
        // Result should still be null — no submit happened
        assertEquals(null, viewModel.formState.value.result)
    }

    private fun buildViewModel(nudgeId: String) = EditNudgeViewModel(
        nudgeId = nudgeId,
        nudgeRepository = repos.nudgeRepo,
        questionRepository = repos.questionRepo,
        questionOptionRepository = repos.optionRepo,
        scheduleRepository = repos.scheduleRepo,
        updateNudge = repos.updateNudgeUseCase()
    )

    private suspend fun createNudge(questionText: String): String {
        val useCase = CreateNudgeUseCase(
            repos.nudgeRepo, repos.questionRepo, repos.optionRepo, repos.scheduleRepo, repos.scheduler
        )
        return (useCase.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest(questionText, QuestionType.YES_NO),
                schedule = ScheduleRequest(
                    type = ScheduleType.DAILY,
                    timeOfDay = LocalTime(12, 0),
                    activeDaysOfWeek = DayOfWeek.entries.toSet()
                ),
                isEnabled = true
            )
        ) as CreateNudgeResult.Success).nudgeId
    }
}
