package com.nudgery.android.viewmodel

import com.nudgery.android.util.TestViewModelRepositories
import com.nudgery.shared.model.Answer
import com.nudgery.shared.model.ExportFormat
import com.nudgery.shared.model.NotificationFire
import com.nudgery.shared.model.Nudge
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.model.Timeframe
import com.nudgery.shared.usecase.ComputeNextFireTimeUseCase
import com.nudgery.shared.usecase.CreateNudgeRequest
import com.nudgery.shared.usecase.CreateNudgeResult
import com.nudgery.shared.usecase.CreateNudgeUseCase
import com.nudgery.shared.usecase.QuestionRequest
import com.nudgery.shared.usecase.ScheduleRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NudgeDetailViewModelTest {

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
    fun TDD_detailLoadsNudgeName() = runTest {
        // README "Viewing Nudges": "The Nudge's details will open"
        val nudgeId = createNudge("How are you?")
        val viewModel = buildViewModel(nudgeId)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertFalse("Detail screen should finish loading", viewModel.uiState.value.isLoading)
        assertEquals("How are you?", viewModel.uiState.value.nudgeName)
    }

    @Test
    fun TDD_answersAppearInDetailState() = runTest {
        // README "Viewing Nudges": "a raw table of the answer data"
        val nudgeId = createNudge("Did you exercise?")
        val questions = repos.questionRepo.getByNudgeId(nudgeId)
        val viewModel = buildViewModel(nudgeId)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        repos.answerRepo.insert(makeAnswer(nudgeId, questions.first().id, "YES"))
        repos.answerRepo.insert(makeAnswer(nudgeId, questions.first().id, "NO"))
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.answers.size)
    }

    @Test
    fun TDD_timeframeChangeReloadsVisualizations() = runTest {
        // README "Viewing Nudges": "The timeframe can be switched between weekly, monthly, yearly, and all-time."
        val nudgeId = createNudge("Mood (1-10)?", type = QuestionType.NUMBER)
        val viewModel = buildViewModel(nudgeId)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(Timeframe.WEEKLY, viewModel.uiState.value.selectedTimeframe)

        viewModel.selectTimeframe(Timeframe.MONTHLY)
        advanceUntilIdle()

        assertEquals(Timeframe.MONTHLY, viewModel.uiState.value.selectedTimeframe)
    }

    @Test
    fun TDD_hidingAnswerUpdatesItsVisibility() = runTest {
        // README "Viewing Nudges": "you can select individual answers and tag them as hidden"
        val nudgeId = createNudge("Did you sleep well?")
        val questions = repos.questionRepo.getByNudgeId(nudgeId)
        val viewModel = buildViewModel(nudgeId)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        val answerId = "ans-1"
        repos.answerRepo.insert(makeAnswer(nudgeId, questions.first().id, "YES", id = answerId))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.answers.first().isHidden)

        viewModel.setAnswerHidden(answerId, true)
        advanceUntilIdle()

        assertTrue("Hidden answer should be reflected in detail state",
            viewModel.uiState.value.answers.first().isHidden)
    }

    @Test
    fun TDD_exportProducesNonEmptyContent() = runTest {
        // README "Viewing Nudges": "a raw table of the answer data which can be exported to a CSV/TSV file"
        val nudgeId = createNudge("Did you exercise?")
        val questions = repos.questionRepo.getByNudgeId(nudgeId)
        repos.answerRepo.insert(makeAnswer(nudgeId, questions.first().id, "YES"))
        val viewModel = buildViewModel(nudgeId)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.exportContent)

        viewModel.exportAnswers(ExportFormat.CSV)
        advanceUntilIdle()

        assertNotNull("Export should produce content", viewModel.uiState.value.exportContent)
        assertTrue(viewModel.uiState.value.exportContent!!.contains("nudge_name"))
    }

    @Test
    fun TDD_detailMissedIndicatorSetWhenFireHasNoAnswer() = runTest {
        // README "Viewing Nudges": missed indicator on the Answer Now button
        val nudgeId = createNudge("Did you exercise?")
        repos.notificationFireRepo.insert(NotificationFire("fire-1", nudgeId, Clock.System.now()))
        val viewModel = buildViewModel(nudgeId)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(
            "hasMissedNotification should be true when a fire has no subsequent answer",
            viewModel.uiState.value.hasMissedNotification
        )
    }

    @Test
    fun TDD_detailMissedIndicatorClearedAfterAnswer() = runTest {
        val nudgeId = createNudge("Did you exercise?")
        val questions = repos.questionRepo.getByNudgeId(nudgeId)
        val fireTime = Clock.System.now()
        repos.notificationFireRepo.insert(NotificationFire("fire-1", nudgeId, fireTime))
        val viewModel = buildViewModel(nudgeId)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasMissedNotification)

        repos.answerRepo.insert(makeAnswer(nudgeId, questions.first().id, "YES"))
        advanceUntilIdle()

        assertFalse(
            "hasMissedNotification should clear once an answer is recorded after the fire",
            viewModel.uiState.value.hasMissedNotification
        )
    }

    private fun buildViewModel(nudgeId: String) = NudgeDetailViewModel(
        nudgeId = nudgeId,
        nudgeRepository = repos.nudgeRepo,
        questionRepository = repos.questionRepo,
        questionOptionRepository = repos.optionRepo,
        scheduleRepository = repos.scheduleRepo,
        answerRepository = repos.answerRepo,
        notificationFireRepository = repos.notificationFireRepo,
        computeNextFireTime = ComputeNextFireTimeUseCase(),
        getVisualizationData = repos.getVisualizationDataUseCase(),
        setAnswerHidden = repos.setAnswerHiddenUseCase(),
        exportAnswers = repos.exportAnswersUseCase(),
        updateNudge = repos.updateNudgeUseCase()
    )

    private suspend fun createNudge(
        questionText: String,
        type: QuestionType = QuestionType.YES_NO
    ): String {
        val useCase = CreateNudgeUseCase(
            repos.nudgeRepo, repos.questionRepo, repos.optionRepo, repos.scheduleRepo, repos.scheduler
        )
        return (useCase.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest(questionText, type),
                schedule = ScheduleRequest(
                    type = ScheduleType.DAILY,
                    timeOfDay = LocalTime(12, 0),
                    activeDaysOfWeek = DayOfWeek.entries.toSet()
                ),
                isEnabled = true
            )
        ) as CreateNudgeResult.Success).nudgeId
    }

    private fun makeAnswer(
        nudgeId: String,
        questionId: String,
        value: String,
        id: String = "ans-${System.nanoTime()}"
    ) = Answer(
        id = id, nudgeId = nudgeId, questionId = questionId,
        value = value, scheduledAt = Clock.System.now(), answeredAt = Clock.System.now(), isHidden = false
    )
}
