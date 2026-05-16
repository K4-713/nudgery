package com.nudgery.android.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.nudgery.shared.model.Nudge
import com.nudgery.shared.model.Schedule
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.scheduler.WorkManagerNotificationScheduler
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class WorkManagerSchedulerTest {

    private lateinit var context: Context
    private lateinit var scheduler: WorkManagerNotificationScheduler
    private lateinit var workManager: WorkManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        workManager = WorkManager.getInstance(context)
        scheduler = WorkManagerNotificationScheduler(context)
    }

    @Test
    fun TDD_scheduleEnqueuesWorkWithCorrectWorkName() {
        // README: "Enabled Nudges will send you notifications when it's time to answer your questions."
        val nudge = makeTestNudge("nudge-sched-1")
        scheduler.schedule(nudge, makeAllDayHourlySchedule("nudge-sched-1"))

        val infos = workManager.getWorkInfosForUniqueWork("nudge_notification_nudge-sched-1").get()
        assertTrue("schedule() should enqueue a work request for the nudge",
            infos.any { it.state == WorkInfo.State.ENQUEUED })
    }

    @Test
    fun TDD_cancelRemovesScheduledWork() {
        // README: "whether or not it is enabled" — disabling a nudge must stop pending notifications
        val nudge = makeTestNudge("nudge-cancel-1")
        scheduler.schedule(nudge, makeAllDayHourlySchedule("nudge-cancel-1"))
        scheduler.cancel("nudge-cancel-1")

        val infos = workManager.getWorkInfosForUniqueWork("nudge_notification_nudge-cancel-1").get()
        assertTrue("cancel() should leave no enqueued work for the nudge",
            infos.none { it.state == WorkInfo.State.ENQUEUED })
    }

    @Test
    fun TDD_rescheduleReplacesExistingWorkWithSingleEntry() {
        // README: editing a nudge's schedule must update the notification without duplicating it
        val nudge = makeTestNudge("nudge-resched-1")
        val schedule = makeAllDayHourlySchedule("nudge-resched-1")
        scheduler.schedule(nudge, schedule)
        scheduler.reschedule(nudge, schedule)

        val infos = workManager.getWorkInfosForUniqueWork("nudge_notification_nudge-resched-1").get()
        assertEquals("reschedule() should leave exactly one enqueued work item",
            1, infos.count { it.state == WorkInfo.State.ENQUEUED })
    }

    private fun makeTestNudge(id: String) = Nudge(
        id = id,
        name = "Test Nudge",
        isEnabled = true,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )

    private fun makeAllDayHourlySchedule(nudgeId: String) = Schedule(
        id = "sched-$nudgeId",
        nudgeId = nudgeId,
        type = ScheduleType.HOURLY,
        timeOfDay = LocalTime(0, 0),
        activeDaysOfWeek = null,
        dayOfMonth = null,
        activeHours = (0..23).toSet()
    )
}
