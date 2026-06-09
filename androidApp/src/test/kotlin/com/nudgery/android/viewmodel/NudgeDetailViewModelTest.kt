// SPDX-License-Identifier: CC0-1.0

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
import com.nudgery.shared.model.HeatMapGranularity
import com.nudgery.shared.model.VisualizationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days
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

    @Test
    fun TDD_selectTimeframe_locksAllChartsToTheSharedWindow() = runTest {
        // The dashboard is locked to one window: WEEKLY (last 7 days) excludes a 60-day-old answer,
        // while YEARLY (last 365 days) includes it. Switching timeframe resizes the shared window.
        val nudgeId = createNudge("Did you see any cool birds today?")
        val questions = repos.questionRepo.getByNudgeId(nudgeId)
        val questionId = questions.first().id
        val now = Clock.System.now()

        repos.answerRepo.insert(makeAnswer(nudgeId, questionId, "YES", id = "recent"))
        repos.answerRepo.insert(makeAnswer(nudgeId, questionId, "YES", id = "old",
            scheduledAt = now - 60.days))

        val viewModel = buildViewModel(nudgeId)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val weeklyHeatMap = viewModel.uiState.value.visualizations
            .filterIsInstance<VisualizationData.CalendarHeatMap>().first()
        assertEquals("WEEKLY window (last 7 days) should include only the recent answer",
            1.0, weeklyHeatMap.dailyCounts.sumOf { it.value }, 0.0)
        assertEquals("WEEKLY should use single-day strip granularity",
            HeatMapGranularity.SINGLE_DAY, weeklyHeatMap.granularity)

        viewModel.selectTimeframe(Timeframe.YEARLY)
        advanceUntilIdle()

        val yearlyHeatMap = viewModel.uiState.value.visualizations
            .filterIsInstance<VisualizationData.CalendarHeatMap>().first()
        assertEquals("YEARLY window (last 365 days) should include both answers",
            2.0, yearlyHeatMap.dailyCounts.sumOf { it.value }, 0.0)
        assertEquals("YEARLY should use the week-grid granularity",
            HeatMapGranularity.WEEK_GRID, yearlyHeatMap.granularity)
    }

    @Test
    fun TDD_deleteNudge_setsIsDeleted() = runTest {
        // Deleting a nudge must signal the UI to navigate away
        val nudgeId = createNudge("Did you see any cool birds today?")
        val viewModel = buildViewModel(nudgeId)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.deleteNudge()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDeleted)
    }

    @Test
    fun TDD_deleteNudge_cancelsNotifications() = runTest {
        // Deleting a nudge must cancel its scheduled notifications
        val nudgeId = createNudge("Did you see any cool birds today?")
        repos.scheduler.reset()
        val viewModel = buildViewModel(nudgeId)

        viewModel.deleteNudge()
        advanceUntilIdle()

        assertTrue(repos.scheduler.cancelled.contains(nudgeId))
    }

    @Test
    fun TDD_defaultTimeframe_loadsPersistedValueOnOpen() = runTest {
        // Detail screen: timeframe picker should be pre-populated with the user's last choice
        val nudgeId = createNudge("Did you exercise?")
        repos.appSettings.setDefaultTimeframe(nudgeId, Timeframe.YEARLY)

        val viewModel = buildViewModel(nudgeId)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(Timeframe.YEARLY, viewModel.uiState.value.selectedTimeframe)
    }

    @Test
    fun TDD_defaultTimeframe_persistsWhenTimeframeSelected() = runTest {
        // Selecting a timeframe must save it so it survives an app restart
        val nudgeId = createNudge("Did you exercise?")
        val viewModel = buildViewModel(nudgeId)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.selectTimeframe(Timeframe.MONTHLY)
        advanceUntilIdle()

        val stored = repos.appSettings.getDefaultTimeframe(nudgeId).first()
        assertEquals(Timeframe.MONTHLY, stored)
    }

    @Test
    fun TDD_defaultTimeframe_newViewModelUsesPersistedValue() = runTest {
        // A freshly constructed ViewModel for the same nudge should open with the previously saved timeframe
        val nudgeId = createNudge("Did you exercise?")
        buildViewModel(nudgeId).also { vm ->
            backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }
            advanceUntilIdle()
            vm.selectTimeframe(Timeframe.ALL_TIME)
            advanceUntilIdle()
        }

        val secondViewModel = buildViewModel(nudgeId)
        backgroundScope.launch(testDispatcher) { secondViewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(Timeframe.ALL_TIME, secondViewModel.uiState.value.selectedTimeframe)
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
        updateNudge = repos.updateNudgeUseCase(),
        deleteNudge = repos.deleteNudgeUseCase(),
        appSettings = repos.appSettings
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
        id: String = "ans-${System.nanoTime()}",
        scheduledAt: kotlinx.datetime.Instant = Clock.System.now()
    ) = Answer(
        id = id, nudgeId = nudgeId, questionId = questionId,
        value = value, scheduledAt = scheduledAt, answeredAt = Clock.System.now(), isHidden = false
    )
}
