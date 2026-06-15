// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.viewmodel

import com.nudgery.android.util.TestViewModelRepositories
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.model.TriggerOperator
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

    @Test
    fun TDD_editFormLoadsExistingFollowUps() = runTest {
        // README: follow-up questions are loaded into the edit wizard from the DB
        val nudgeId = createNudge("Did you exercise?", withFollowUp = true)
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()

        val followUps = viewModel.formState.value.followUps
        assertTrue("Should load one follow-up", followUps.size == 1)
        assertEquals("Describe your workout", followUps.first().formState.text)
        assertNotNull("Loaded follow-up should have a DB question ID", followUps.first().questionId)
    }

    @Test
    fun TDD_addFollowUpAndSave() = runTest {
        // README: follow-up questions can be added after nudge creation from the detail screen
        val nudgeId = createNudge("Did you meditate?")
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()

        viewModel.addFollowUp()
        viewModel.updateFollowUp(0, viewModel.formState.value.followUps[0].formState.copy(
            text = "For how long?",
            type = com.nudgery.shared.model.QuestionType.NUMBER,
            triggerAnswerValue = "YES",
            triggerOperator = com.nudgery.shared.model.TriggerOperator.EQ
        ))
        viewModel.submit()
        advanceUntilIdle()

        assertTrue("Add follow-up should succeed", viewModel.formState.value.result is UpdateNudgeResult.Success)
        val followUps = repos.questionRepo.getByNudgeId(nudgeId).filter { !it.isMainQuestion }
        assertTrue("Should have one new follow-up", followUps.size == 1)
        assertEquals("For how long?", followUps.first().text)
    }

    @Test
    fun TDD_removeFollowUpAndSave() = runTest {
        // Removing a follow-up via the wizard and saving deletes it from the DB
        val nudgeId = createNudge("Did you exercise?", withFollowUp = true)
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()

        assertTrue("Precondition: one follow-up loaded", viewModel.formState.value.followUps.size == 1)
        viewModel.removeFollowUp(0)
        viewModel.submit()
        advanceUntilIdle()

        assertTrue("Remove follow-up should succeed", viewModel.formState.value.result is UpdateNudgeResult.Success)
        val followUps = repos.questionRepo.getByNudgeId(nudgeId).filter { !it.isMainQuestion }
        assertTrue("Follow-up should have been deleted", followUps.isEmpty())
    }

    @Test
    fun TDD_untouchedAddedFollowUpNotSavedOnEdit() = runTest {
        // ED-21: a follow-up added in the edit screen but never edited is discarded on save,
        // matching the create wizard — not persisted as a blank follow-up.
        val nudgeId = createNudge("Did you exercise?")
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()

        viewModel.addFollowUp()
        assertEquals("Precondition: a pristine stub was added", 1, viewModel.formState.value.followUps.size)
        viewModel.submit()
        advanceUntilIdle()

        assertTrue("Save should succeed", viewModel.formState.value.result is UpdateNudgeResult.Success)
        val followUps = repos.questionRepo.getByNudgeId(nudgeId).filter { !it.isMainQuestion }
        assertTrue("An untouched added stub must not be persisted", followUps.isEmpty())
    }

    @Test
    fun TDD_existingFollowUpSurvivesPruneOnEdit() = runTest {
        // ED-21: the prune only drops new pristine stubs (questionId == null); an existing stored
        // follow-up is preserved through a save even when otherwise unchanged.
        val nudgeId = createNudge("Did you exercise?", withFollowUp = true)
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()
        assertEquals("Precondition: one existing follow-up loaded", 1, viewModel.formState.value.followUps.size)

        viewModel.submit()
        advanceUntilIdle()

        val followUps = repos.questionRepo.getByNudgeId(nudgeId).filter { !it.isMainQuestion }
        assertEquals("Existing follow-up must survive the prune", 1, followUps.size)
    }

    @Test
    fun TDD_followUpWithoutTriggerIsRejectedOnSave() = runTest {
        // ED-26 backstop: editing in a follow-up with text but no trigger is refused at the use-case
        // (the form prevents this; this verifies the safety net), and nothing is persisted.
        val nudgeId = createNudge("Did you exercise?")
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()

        viewModel.addFollowUp()
        viewModel.updateFollowUp(0, viewModel.formState.value.followUps[0].formState.copy(
            text = "What did you do?",
            type = com.nudgery.shared.model.QuestionType.TEXT,
            triggerOperator = null,  // cleared ALWAYS but no conditional trigger set — invalid
            triggerAnswerValue = null
        ))
        viewModel.submit()
        advanceUntilIdle()

        assertTrue(
            "Save must be refused with InvalidQuestion",
            viewModel.formState.value.result is UpdateNudgeResult.InvalidQuestion
        )
        val followUps = repos.questionRepo.getByNudgeId(nudgeId).filter { !it.isMainQuestion }
        assertTrue("Nothing should be persisted", followUps.isEmpty())
    }

    @Test
    fun TDD_addOptionAppearsInFormState() = runTest {
        // DESIGN.md "Create / Edit Nudge Wizard": "Option builder (add/remove/reorder up to 16 options)"
        val nudgeId = createNudge("Feeling?", withOptions = listOf("Good", "Bad"))
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()
        assertEquals(2, viewModel.formState.value.options.size)

        viewModel.addOption()

        assertEquals(3, viewModel.formState.value.options.size)
        assertTrue("New option should have no DB id", viewModel.formState.value.options.last().isNew)
    }

    @Test
    fun TDD_removeExistingOptionTracksRemovedId() = runTest {
        // DESIGN.md "Create / Edit Nudge Wizard": "Option builder (add/remove/reorder up to 16 options)"
        val nudgeId = createNudge("Feeling?", withOptions = listOf("Good", "Bad"))
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()
        val removedId = viewModel.formState.value.options[0].optionId!!

        viewModel.removeOption(0)

        assertEquals(1, viewModel.formState.value.options.size)
        assertTrue(removedId in viewModel.formState.value.removedOptionIds)
    }

    @Test
    fun TDD_removingOptionTriggersSplitDialog() = runTest {
        // Removing an option changes what existing answers mean — split dialog must be shown
        val nudgeId = createNudge("Feeling?", withOptions = listOf("Good", "Bad"))
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()

        viewModel.removeOption(0)
        viewModel.submit()
        advanceUntilIdle()

        assertTrue("Removing an option should trigger the split dialog",
            viewModel.formState.value.showSplitDialog)
    }

    @Test
    fun TDD_addingOptionDoesNotTriggerSplitDialog() = runTest {
        // Adding a new option doesn't change the meaning of existing answers — no split needed
        val nudgeId = createNudge("Feeling?", withOptions = listOf("Good", "Bad"))
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()

        viewModel.addOption()
        viewModel.updateOptionAt(2, "Okay")
        viewModel.submit()
        advanceUntilIdle()

        assertFalse("Adding an option must not trigger split dialog",
            viewModel.formState.value.showSplitDialog)
        assertTrue("Save should succeed", viewModel.formState.value.result is UpdateNudgeResult.Success)
    }

    private fun buildViewModel(nudgeId: String) = EditNudgeViewModel(
        nudgeId = nudgeId,
        nudgeRepository = repos.nudgeRepo,
        questionRepository = repos.questionRepo,
        questionOptionRepository = repos.optionRepo,
        scheduleRepository = repos.scheduleRepo,
        updateNudge = repos.updateNudgeUseCase()
    )

    private suspend fun createNudge(
        questionText: String,
        withFollowUp: Boolean = false,
        withOptions: List<String> = emptyList()
    ): String {
        val useCase = CreateNudgeUseCase(
            repos.nudgeRepo, repos.questionRepo, repos.optionRepo, repos.scheduleRepo, repos.scheduler
        )
        val followUps = if (withFollowUp) listOf(
            QuestionRequest(
                text = "Describe your workout",
                type = QuestionType.TEXT,
                triggerAnswerValue = "YES",
                triggerOperator = com.nudgery.shared.model.TriggerOperator.EQ
            )
        ) else emptyList()
        val mainType = if (withOptions.isNotEmpty()) QuestionType.OPTION_SINGLE else QuestionType.YES_NO
        return (useCase.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest(questionText, mainType, withOptions),
                followUpQuestions = followUps,
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
