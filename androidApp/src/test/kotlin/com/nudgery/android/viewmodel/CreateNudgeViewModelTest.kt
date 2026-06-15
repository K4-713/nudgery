// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.viewmodel

import com.nudgery.android.util.TestViewModelRepositories
import com.nudgery.shared.model.Nudge
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.usecase.CreateNudgeResult
import kotlinx.datetime.Clock
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
        viewModel = CreateNudgeViewModel(createNudge = repos.createNudgeUseCase(), nudgeRepository = repos.nudgeRepo)
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
    fun TDD_mainQuestionTypeCanBeText() = runTest {
        // README "Setting Up a Nudge": free Text is a valid main question type
        viewModel.setMainQuestion(QuestionFormState(text = "Notes?", type = QuestionType.TEXT))
        viewModel.setSchedule(dailyScheduleForm())

        viewModel.submit()
        advanceUntilIdle()

        assertTrue("Text main question should create successfully",
            viewModel.formState.value.result is CreateNudgeResult.Success)
    }

    @Test
    fun TDD_switchingMainQuestionToTextKeepsFollowUps() = runTest {
        // ED-28: all main question types support follow-ups; switching to Text preserves them.
        viewModel.setMainQuestion(QuestionFormState(text = "How are you?", type = QuestionType.YES_NO))
        viewModel.addFollowUpQuestion(QuestionFormState(text = "Why?", type = QuestionType.TEXT))
        assertEquals(1, viewModel.formState.value.followUpQuestions.size)

        viewModel.setMainQuestion(QuestionFormState(text = "Notes?", type = QuestionType.TEXT))

        assertEquals("Follow-ups should be preserved when main becomes text",
            1, viewModel.formState.value.followUpQuestions.size)
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
        // The follow-up needs a trigger (ED-26): fire it when the main answer is "YES".
        viewModel.addFollowUpQuestion(
            QuestionFormState(text = "What did you do?", type = QuestionType.TEXT, triggerAnswerValue = "YES")
        )
        viewModel.setSchedule(dailyScheduleForm())

        viewModel.submit()
        advanceUntilIdle()

        val nudgeId = (viewModel.formState.value.result as CreateNudgeResult.Success).nudgeId
        val questions = repos.questionRepo.getByNudgeId(nudgeId)
        assertEquals(2, questions.size)
        assertTrue("Follow-up question should be persisted", questions.any { !it.isMainQuestion })
    }

    @Test
    fun TDD_untouchedFollowUpStubIsPrunedOnNavigation() = runTest {
        // ENGINEERING_DECISIONS.md ED-21: adding a follow-up but changing nothing should not leave a
        // phantom follow-up. The wizard commits a pristine stub on "Add follow-up question";
        // navigating away (or submitting) prunes it.
        viewModel.setMainQuestion(QuestionFormState(text = "Did you exercise?", type = QuestionType.YES_NO))
        viewModel.addFollowUpQuestion(QuestionFormState())
        assertEquals(1, viewModel.formState.value.followUpQuestions.size)

        viewModel.pruneUntouchedFollowUps()

        assertTrue("An untouched follow-up stub should be discarded",
            viewModel.formState.value.followUpQuestions.isEmpty())
    }

    @Test
    fun TDD_editedFollowUpSurvivesPruning() = runTest {
        // A follow-up the user actually edited (any change from the default) must be kept.
        viewModel.addFollowUpQuestion(QuestionFormState(text = "Why?", type = QuestionType.TEXT))

        viewModel.pruneUntouchedFollowUps()

        assertEquals("An edited follow-up must not be pruned",
            1, viewModel.formState.value.followUpQuestions.size)
    }

    @Test
    fun TDD_submitDropsUntouchedFollowUpStub() = runTest {
        // Defensive: an untouched stub must never be persisted as a blank follow-up question.
        viewModel.setMainQuestion(QuestionFormState(text = "Did you exercise?", type = QuestionType.YES_NO))
        viewModel.addFollowUpQuestion(QuestionFormState())
        viewModel.setSchedule(dailyScheduleForm())

        viewModel.submit()
        advanceUntilIdle()

        val nudgeId = (viewModel.formState.value.result as CreateNudgeResult.Success).nudgeId
        val questions = repos.questionRepo.getByNudgeId(nudgeId)
        assertEquals("Only the main question should be persisted", 1, questions.size)
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

    @Test
    fun TDD_defaultNudgeName_noExistingNudges_isNudge1() = runTest {
        // New users with no nudges should start at "Nudge #1"
        assertEquals("Nudge #1", viewModel.formState.value.nudgeName)
    }

    @Test
    fun TDD_defaultNudgeName_withExistingDefaultNudges_incrementsHighest() = runTest {
        // If "Nudge #1" and "Nudge #2" exist, next default should be "Nudge #3"
        val now = Clock.System.now()
        repos.nudgeRepo.insert(Nudge(id = "a", name = "Nudge #1", isEnabled = true, createdAt = now, updatedAt = now))
        repos.nudgeRepo.insert(Nudge(id = "b", name = "Nudge #2", isEnabled = true, createdAt = now, updatedAt = now))
        val vm = CreateNudgeViewModel(createNudge = repos.createNudgeUseCase(), nudgeRepository = repos.nudgeRepo)
        assertEquals("Nudge #3", vm.formState.value.nudgeName)
    }

    @Test
    fun TDD_defaultNudgeName_withCustomNamedNudges_countsDriveNumber() = runTest {
        // Custom-named nudges count toward the total, so "Nudge #N" stays ahead of the total nudge count
        val now = Clock.System.now()
        repos.nudgeRepo.insert(Nudge(id = "a", name = "Bird watching", isEnabled = true, createdAt = now, updatedAt = now))
        val vm = CreateNudgeViewModel(createNudge = repos.createNudgeUseCase(), nudgeRepository = repos.nudgeRepo)
        assertEquals("Nudge #2", vm.formState.value.nudgeName)
    }

    @Test
    fun TDD_defaultNudgeName_withGapInSequence_usesHighestPlusOne() = runTest {
        // Pattern number (highest existing +1) wins over total count when it's higher
        val now = Clock.System.now()
        repos.nudgeRepo.insert(Nudge(id = "a", name = "Nudge #1", isEnabled = true, createdAt = now, updatedAt = now))
        repos.nudgeRepo.insert(Nudge(id = "b", name = "Nudge #3", isEnabled = true, createdAt = now, updatedAt = now))
        val vm = CreateNudgeViewModel(createNudge = repos.createNudgeUseCase(), nudgeRepository = repos.nudgeRepo)
        assertEquals("Nudge #4", vm.formState.value.nudgeName)
    }

    @Test
    fun TDD_defaultNudgeName_highPatternNumberOverridesTotalCount() = runTest {
        // If "Nudge #10" exists among only 2 nudges, pattern number wins
        val now = Clock.System.now()
        repos.nudgeRepo.insert(Nudge(id = "a", name = "Bird watching", isEnabled = true, createdAt = now, updatedAt = now))
        repos.nudgeRepo.insert(Nudge(id = "b", name = "Nudge #10", isEnabled = true, createdAt = now, updatedAt = now))
        val vm = CreateNudgeViewModel(createNudge = repos.createNudgeUseCase(), nudgeRepository = repos.nudgeRepo)
        assertEquals("Nudge #11", vm.formState.value.nudgeName)
    }

    private fun dailyScheduleForm() = ScheduleFormState(
        type = ScheduleType.DAILY,
        timeOfDay = LocalTime(12, 0),
        activeDaysOfWeek = DayOfWeek.entries.toSet()
    )
}
