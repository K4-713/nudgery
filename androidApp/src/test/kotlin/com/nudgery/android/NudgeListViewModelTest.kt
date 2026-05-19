package com.nudgery.android

import com.nudgery.android.util.TestViewModelRepositories
import com.nudgery.android.viewmodel.NudgeListViewModel
import com.nudgery.shared.model.Nudge
import com.nudgery.shared.model.Schedule
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.usecase.ComputeNextFireTimeUseCase
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
class NudgeListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repos: TestViewModelRepositories
    private lateinit var viewModel: NudgeListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repos = TestViewModelRepositories()
        viewModel = NudgeListViewModel(
            nudgeRepository = repos.nudgeRepo,
            scheduleRepository = repos.scheduleRepo,
            computeNextFireTime = ComputeNextFireTimeUseCase(),
            updateNudge = repos.updateNudgeUseCase()
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun TDD_nudgeListLoadsAllSavedNudges() = runTest {
        // README "Setting Up a Nudge": "It will appear on the main screen in the list
        //   with the rest of your Nudges"
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        repos.nudgeRepo.insert(makeNudge("1", "Mood check"))
        repos.nudgeRepo.insert(makeNudge("2", "Step count"))
        repos.nudgeRepo.insert(makeNudge("3", "Water intake"))
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.size)
        val names = viewModel.uiState.value.map { it.name }
        assertTrue(names.containsAll(listOf("Mood check", "Step count", "Water intake")))
    }

    @Test
    fun TDD_nudgeListEntryShowsNextNudgeDateTime() = runTest {
        // README "Setting Up a Nudge": "indicating...next nudge date and time"
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        // Schedule must be present before the nudge is inserted so toSummary() finds it
        // when observeAll() emits the updated list.
        repos.scheduleRepo.insert(makeDailySchedule("1"))
        repos.nudgeRepo.insert(makeNudge("1", "Exercise"))
        advanceUntilIdle()

        val summary = viewModel.uiState.value.first()
        assertNotNull("Next fire time should be computed from the schedule", summary.nextFireTime)
    }

    @Test
    fun TDD_nudgeListEntryShowsEnabledStatus() = runTest {
        // README "Setting Up a Nudge": "indicating...whether or not it is enabled"
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        repos.nudgeRepo.insert(makeNudge("1", "Active nudge", isEnabled = true))
        repos.nudgeRepo.insert(makeNudge("2", "Paused nudge", isEnabled = false))
        advanceUntilIdle()

        val active = viewModel.uiState.value.first { it.nudgeId == "1" }
        val paused = viewModel.uiState.value.first { it.nudgeId == "2" }
        assertTrue("Enabled nudge should report isEnabled=true", active.isEnabled)
        assertFalse("Disabled nudge should report isEnabled=false", paused.isEnabled)
    }

    @Test
    fun TDD_togglingNudgeEnabledStatusUpdatesListEntry() = runTest {
        // README "Viewing Nudges": editable details include enabled toggle — must reflect immediately
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        repos.nudgeRepo.insert(makeNudge("1", "Mood check", isEnabled = true))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.first().isEnabled)

        viewModel.toggleEnabled("1")
        advanceUntilIdle()

        assertFalse("Toggling an enabled nudge should disable it", viewModel.uiState.value.first().isEnabled)
    }

    @Test
    fun TDD_notificationTapNavigatesToQuestionForm() = runTest {
        // README "Setting Up a Nudge": "When the notification pops up, clicking on it will
        //   take you directly to the question form"
        assertNull(viewModel.pendingAnswer.value)

        viewModel.handleNotificationIntent("nudge-abc", scheduledAt = null)

        assertEquals("nudge-abc", viewModel.pendingAnswer.value?.nudgeId)
    }

    private fun makeNudge(id: String, name: String, isEnabled: Boolean = true) = Nudge(
        id = id, name = name, isEnabled = isEnabled,
        createdAt = Clock.System.now(), updatedAt = Clock.System.now()
    )

    private fun makeDailySchedule(nudgeId: String) = Schedule(
        id = "sched-$nudgeId", nudgeId = nudgeId,
        type = ScheduleType.DAILY,
        timeOfDay = LocalTime(12, 0),
        activeDaysOfWeek = DayOfWeek.entries.toSet(),
        dayOfMonth = null,
        activeHours = null
    )
}
