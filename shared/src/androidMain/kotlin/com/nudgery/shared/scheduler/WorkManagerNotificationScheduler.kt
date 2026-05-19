package com.nudgery.shared.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.nudgery.shared.model.Nudge
import com.nudgery.shared.model.Schedule
import com.nudgery.shared.notification.EXTRA_NUDGE_ID
import com.nudgery.shared.notification.EXTRA_SCHEDULED_AT
import com.nudgery.shared.notification.NudgeAlarmReceiver
import com.nudgery.shared.usecase.ComputeNextFireTimeUseCase
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone

private const val TAG = "WorkManagerScheduler"

class WorkManagerNotificationScheduler(private val context: Context) : NotificationScheduler {

    private val computeNextFireTime = ComputeNextFireTimeUseCase()

    override fun schedule(nudge: Nudge, schedule: Schedule) {
        enqueueAlarm(nudge.id, schedule)
    }

    override fun cancel(nudgeId: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = buildAlarmPendingIntent(nudgeId, scheduledAtMs = 0L, PendingIntent.FLAG_NO_CREATE)
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
        Log.i(TAG, "Cancelled scheduled alarm for nudge $nudgeId")
    }

    override fun reschedule(nudge: Nudge, schedule: Schedule) {
        enqueueAlarm(nudge.id, schedule)
    }

    private fun enqueueAlarm(nudgeId: String, schedule: Schedule) {
        val now = Clock.System.now()
        val nextFireTime = computeNextFireTime.execute(schedule, now, TimeZone.currentSystemDefault())
        val nextFireMs = nextFireTime.toEpochMilliseconds()

        if (nextFireMs <= now.toEpochMilliseconds()) {
            Log.w(TAG, "Computed fire time is not in the future for nudge $nudgeId — skipping")
            return
        }

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = buildAlarmPendingIntent(nudgeId, nextFireMs, PendingIntent.FLAG_UPDATE_CURRENT)
            ?: run {
                Log.e(TAG, "Failed to create PendingIntent for nudge $nudgeId — skipping alarm")
                return
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextFireMs, pendingIntent)
            Log.w(TAG, "Exact alarm permission not granted for nudge $nudgeId — using inexact alarm")
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextFireMs, pendingIntent)
            Log.i(TAG, "Exact alarm set for nudge $nudgeId at $nextFireTime")
        }
    }

    private fun buildAlarmPendingIntent(nudgeId: String, scheduledAtMs: Long, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            nudgeId.hashCode(),
            Intent(context, NudgeAlarmReceiver::class.java)
                .putExtra(EXTRA_NUDGE_ID, nudgeId)
                .putExtra(EXTRA_SCHEDULED_AT, scheduledAtMs),
            flags or PendingIntent.FLAG_IMMUTABLE
        )
}
