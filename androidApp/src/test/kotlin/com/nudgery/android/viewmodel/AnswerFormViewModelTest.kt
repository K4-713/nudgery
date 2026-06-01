package com.nudgery.android.viewmodel

import com.nudgery.android.util.TestViewModelRepositories
import com.nudgery.android.viewmodel.ScheduledAt
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.model.TriggerOperator
import com.nudgery.shared.usecase.CreateNudgeRequest
import com.nudgery.shared.usecase.CreateNudgeResult
import com.nudgery.shared.usecase.QuestionRequest
import com.nudgery.shared.usecase.ScheduleRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class AnswerFormViewModelTest {

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

    private fun dailySchedule() = ScheduleRequest(
        type = ScheduleType.DAILY,
        timeOfDay = LocalTime(12, 0),
        activeDaysOfWeek = setOf(DayOfWeek.MONDAY)
    )

    private suspend fun createYesNoNudge(withFollowUp: Boolean = false): String {
        val followUps = if (withFollowUp) {
            listOf(
                QuestionRequest(
                    text = "Tell me more",
                    type = QuestionType.YES_NO,
                    triggerAnswerValue = "YES",
                    triggerOperator = TriggerOperator.EQ
                )
            )
        } else emptyList()

        val result = repos.createNudgeUseCase().execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Did you exercise?", QuestionType.YES_NO),
                followUpQuestions = followUps,
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        return result.nudgeId
    }

    private fun buildViewModel(nudgeId: String, scheduledAt: Instant? = null) =
        AnswerFormViewModel(
            nudgeId = nudgeId,
            scheduledAt = ScheduledAt(scheduledAt),
            questionRepository = repos.questionRepo,
            questionOptionRepository = repos.optionRepo,
            recordAnswer = repos.recordAnswerUseCase()
        )

    @Test
    fun TDD_scheduledAtFromNotificationIsPreservedInAnswer() = runTest {
        // ARCHITECTURE.md: "The notification's launch Intent carries EXTRA_SCHEDULED_AT (epoch
        //   milliseconds)... ensuring answers record the nudge's fire time rather than the
        //   wall-clock time of the tap"
        val nudgeId = createYesNoNudge()
        val notificationFiredAt = Clock.System.now() - 10.seconds
        val viewModel = buildViewModel(nudgeId, scheduledAt = notificationFiredAt)
        advanceUntilIdle()

        viewModel.setCurrentAnswer("YES")
        viewModel.saveAnswer()
        advanceUntilIdle()

        val answers = repos.answerRepo.getAllByNudgeId(nudgeId)
        assertEquals(1, answers.size)
        assertEquals(notificationFiredAt, answers[0].scheduledAt)
    }

    @Test
    fun TDD_answerNowUsesCurrentTimeWhenScheduledAtIsNull() = runTest {
        // README "Viewing Nudges": "an 'Answer Now' button that you can use if you missed a
        //   Nudge notification" — no scheduledAt is provided; defaults to now
        val nudgeId = createYesNoNudge()
        val before = Clock.System.now()
        val viewModel = buildViewModel(nudgeId, scheduledAt = null)
        advanceUntilIdle()

        viewModel.setCurrentAnswer("YES")
        viewModel.saveAnswer()
        advanceUntilIdle()

        val after = Clock.System.now()
        val answers = repos.answerRepo.getAllByNudgeId(nudgeId)
        assertEquals(1, answers.size)
        assertTrue(
            "scheduledAt should be approximately now",
            answers[0].scheduledAt >= before && answers[0].scheduledAt <= after
        )
    }

    @Test
    fun TDD_followUpAppearsAfterMainQuestionIsAnswered() = runTest {
        // DESIGN.md: "Follow-up questions appear as subsequent pages after the previous answer
        //   is submitted"
        val nudgeId = createYesNoNudge(withFollowUp = true)
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()

        assertEquals("Form should start with 1 step", 1, viewModel.uiState.value.totalSteps)

        viewModel.setCurrentAnswer("YES")
        viewModel.saveAnswer()
        advanceUntilIdle()

        assertEquals("Follow-up should appear after main is answered", 2, viewModel.uiState.value.totalSteps)
        assertEquals("Should advance to follow-up step", 1, viewModel.uiState.value.currentStepIndex)
    }

    @Test
    fun TDD_followUpDoesNotAppearWhenTriggerIsNotMet() = runTest {
        // README "Setting Up a Nudge": follow-up questions triggered by specific answers
        //   only — no trigger match means the follow-up is skipped
        val nudgeId = createYesNoNudge(withFollowUp = true)
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()

        viewModel.setCurrentAnswer("NO")
        viewModel.saveAnswer()
        advanceUntilIdle()

        assertTrue("Form should be done without a follow-up", viewModel.uiState.value.isDismissed)
        assertEquals("Only the main answer should be recorded", 1, repos.answerRepo.getAllByNudgeId(nudgeId).size)
    }

    @Test
    fun TDD_formDismissedAfterLastAnswerSubmitted() = runTest {
        // DESIGN.md: form closes after the final step is saved
        val nudgeId = createYesNoNudge()
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()

        viewModel.setCurrentAnswer("YES")
        viewModel.saveAnswer()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDismissed)
    }

    @Test
    fun TDD_blankAnswerIsNotSaved() = runTest {
        // DESIGN.md: every answer type has an explicit Save Answer button; blank is not submittable
        val nudgeId = createYesNoNudge()
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()

        viewModel.setCurrentAnswer("")
        viewModel.saveAnswer()
        advanceUntilIdle()

        assertTrue("Blank answer should not be recorded", repos.answerRepo.getAllByNudgeId(nudgeId).isEmpty())
        assertFalse("Form should not dismiss on blank answer", viewModel.uiState.value.isDismissed)
    }

    @Test
    fun TDD_dismissingMidSessionWritesNoAnswers() = runTest {
        // DESIGN.md: "If the workflow is abandoned before completion (via the close button),
        //   the entire session is discarded — no partial records are written"
        val nudgeId = createYesNoNudge(withFollowUp = true)
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()

        // Answer main question — follow-up is now shown
        viewModel.setCurrentAnswer("YES")
        viewModel.saveAnswer()
        advanceUntilIdle()

        // User closes without answering the follow-up
        viewModel.dismiss()
        advanceUntilIdle()

        val answers = repos.answerRepo.getAllByNudgeId(nudgeId)
        assertTrue(
            "Abandoning mid-session should discard all answers, but ${answers.size} were found",
            answers.isEmpty()
        )
    }

    @Test
    fun TDD_completedSessionWithFollowUpWritesBothAnswers() = runTest {
        // DESIGN.md: both main and triggered follow-up answers are recorded when the session
        //   is completed
        val nudgeId = createYesNoNudge(withFollowUp = true)
        val viewModel = buildViewModel(nudgeId)
        advanceUntilIdle()

        viewModel.setCurrentAnswer("YES")
        viewModel.saveAnswer()
        advanceUntilIdle()

        viewModel.setCurrentAnswer("NO")
        viewModel.saveAnswer()
        advanceUntilIdle()

        val answers = repos.answerRepo.getAllByNudgeId(nudgeId)
        assertEquals("Both main and follow-up answers should be recorded", 2, answers.size)
        assertTrue(viewModel.uiState.value.isDismissed)
    }
}
