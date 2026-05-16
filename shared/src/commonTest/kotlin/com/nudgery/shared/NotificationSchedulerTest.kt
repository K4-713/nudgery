package com.nudgery.shared

import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.usecase.CreateNudgeRequest
import com.nudgery.shared.usecase.CreateNudgeResult
import com.nudgery.shared.usecase.CreateNudgeUseCase
import com.nudgery.shared.usecase.QuestionRequest
import com.nudgery.shared.usecase.ScheduleRequest
import com.nudgery.shared.usecase.UpdateNudgeRequest
import com.nudgery.shared.usecase.UpdateNudgeUseCase
import com.nudgery.shared.util.FakeNotificationScheduler
import com.nudgery.shared.util.TestRepositories
import com.nudgery.shared.util.createTestRepositories
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationSchedulerTest {

    private lateinit var repos: TestRepositories
    private lateinit var fakeScheduler: FakeNotificationScheduler
    private lateinit var createNudge: CreateNudgeUseCase
    private lateinit var updateNudge: UpdateNudgeUseCase

    @BeforeTest
    fun setup() {
        repos = createTestRepositories()
        fakeScheduler = FakeNotificationScheduler()
        createNudge = CreateNudgeUseCase(
            repos.nudgeRepository, repos.questionRepository, repos.questionOptionRepository,
            repos.scheduleRepository, fakeScheduler
        )
        updateNudge = UpdateNudgeUseCase(
            repos.nudgeRepository, repos.questionRepository, repos.questionOptionRepository,
            repos.scheduleRepository, repos.nudgeEditRepository, fakeScheduler
        )
    }

    private fun dailySchedule() = ScheduleRequest(
        type = ScheduleType.DAILY,
        timeOfDay = LocalTime(12, 0),
        activeDaysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
    )

    private suspend fun createEnabledNudge(): String {
        return (createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Did you exercise?", QuestionType.YES_NO),
                schedule = dailySchedule(),
                isEnabled = true
            )
        ) as CreateNudgeResult.Success).nudgeId
    }

    @Test
    fun TDD_enabledNudgeSchedulesNotificationOnSave() = runTest {
        // README "Setting Up a Nudge": "Enabled Nudges will send you notifications when it's
        //   time to answer your questions"
        val nudgeId = createEnabledNudge()

        assertEquals(1, fakeScheduler.scheduled.size)
        assertEquals(nudgeId, fakeScheduler.scheduled[0].first.id)
    }

    @Test
    fun TDD_disabledNudgeDoesNotScheduleNotifications() = runTest {
        // README "Setting Up a Nudge": "Enabled Nudges will send you notifications..."
        //   — the inverse: a disabled nudge must not fire
        createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Question?", QuestionType.YES_NO),
                schedule = dailySchedule(),
                isEnabled = false
            )
        )

        assertTrue(fakeScheduler.scheduled.isEmpty())
    }

    @Test
    fun TDD_disablingANudgeCancelsItsScheduledNotifications() = runTest {
        // README "Viewing Nudges": "whether or not it is enabled"
        //   — toggling enabled off must cancel any pending notifications
        val nudgeId = createEnabledNudge()
        fakeScheduler.reset()

        updateNudge.execute(UpdateNudgeRequest(nudgeId = nudgeId, isEnabled = false))

        assertEquals(1, fakeScheduler.cancelled.size)
        assertEquals(nudgeId, fakeScheduler.cancelled[0])
        assertTrue(fakeScheduler.scheduled.isEmpty())
    }

    @Test
    fun TDD_enablingANudgeReschedulesItsNotifications() = runTest {
        // README "Viewing Nudges": "Enabled Nudges will send you notifications..."
        //   — toggling enabled on must re-register the notification schedule
        val nudgeId = (createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Question?", QuestionType.YES_NO),
                schedule = dailySchedule(),
                isEnabled = false
            )
        ) as CreateNudgeResult.Success).nudgeId
        fakeScheduler.reset()

        updateNudge.execute(UpdateNudgeRequest(nudgeId = nudgeId, isEnabled = true))

        assertEquals(1, fakeScheduler.rescheduled.size)
        assertEquals(nudgeId, fakeScheduler.rescheduled[0].first.id)
        assertTrue(fakeScheduler.cancelled.isEmpty())
    }

    @Test
    fun TDD_editingScheduleReschedulesNotifications() = runTest {
        // README "Editing Nudges": "Nudge configuration [can] be edited" — changing the
        //   schedule must update the pending notification trigger
        val nudgeId = createEnabledNudge()
        fakeScheduler.reset()

        val newSchedule = ScheduleRequest(
            type = ScheduleType.WEEKLY,
            timeOfDay = LocalTime(9, 0),
            activeDaysOfWeek = setOf(DayOfWeek.MONDAY)
        )
        updateNudge.execute(UpdateNudgeRequest(nudgeId = nudgeId, schedule = newSchedule))

        assertEquals(1, fakeScheduler.rescheduled.size)
        assertEquals(nudgeId, fakeScheduler.rescheduled[0].first.id)
        assertEquals(ScheduleType.WEEKLY, fakeScheduler.rescheduled[0].second.type)
    }
}
