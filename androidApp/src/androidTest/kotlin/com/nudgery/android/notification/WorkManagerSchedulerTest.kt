// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nudgery.shared.model.Nudge
import com.nudgery.shared.model.Schedule
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.notification.NudgeAlarmReceiver
import com.nudgery.shared.scheduler.WorkManagerNotificationScheduler
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkManagerSchedulerTest {

    private lateinit var context: Context
    private lateinit var scheduler: WorkManagerNotificationScheduler

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        scheduler = WorkManagerNotificationScheduler(context)
    }

    @Test
    fun TDD_scheduleSetsPendingIntent() {
        // README: "Enabled Nudges will send you notifications when it's time to answer your questions."
        val nudgeId = "nudge-sched-1"
        scheduler.cancel(nudgeId) // clean up any prior state

        scheduler.schedule(makeTestNudge(nudgeId), makeAllDayHourlySchedule(nudgeId))

        assertNotNull(
            "schedule() should register a pending alarm for the nudge",
            findPendingIntent(nudgeId)
        )

        scheduler.cancel(nudgeId) // clean up
    }

    @Test
    fun TDD_cancelRemovesPendingIntent() {
        // README: "whether or not it is enabled" — disabling a nudge must stop pending notifications
        val nudgeId = "nudge-cancel-1"
        scheduler.schedule(makeTestNudge(nudgeId), makeAllDayHourlySchedule(nudgeId))
        scheduler.cancel(nudgeId)

        assertNull(
            "cancel() should remove the pending alarm for the nudge",
            findPendingIntent(nudgeId)
        )
    }

    @Test
    fun TDD_rescheduleReplacesExistingAlarm() {
        // README: editing a nudge's schedule must update the notification without duplicating it.
        // AlarmManager deduplicates by request code (nudgeId.hashCode()) + FLAG_UPDATE_CURRENT,
        // so two consecutive schedule() calls still result in exactly one live alarm.
        val nudgeId = "nudge-resched-1"
        scheduler.cancel(nudgeId) // clean up any prior state

        val schedule = makeAllDayHourlySchedule(nudgeId)
        val nudge = makeTestNudge(nudgeId)
        scheduler.schedule(nudge, schedule)
        scheduler.reschedule(nudge, schedule)

        assertNotNull(
            "reschedule() should leave exactly one pending alarm for the nudge",
            findPendingIntent(nudgeId)
        )

        scheduler.cancel(nudgeId) // clean up
    }

    private fun findPendingIntent(nudgeId: String): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            nudgeId.hashCode(),
            Intent(context, NudgeAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

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
