package com.nudgery.shared.scheduler

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nudgery.shared.model.Nudge
import com.nudgery.shared.model.Schedule
import com.nudgery.shared.notification.NudgeNotificationWorker
import com.nudgery.shared.notification.WORKER_KEY_NUDGE_ID
import com.nudgery.shared.notification.WORKER_KEY_SCHEDULED_AT
import com.nudgery.shared.notification.nudgeWorkName
import com.nudgery.shared.usecase.ComputeNextFireTimeUseCase
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import java.util.concurrent.TimeUnit

private const val TAG = "WorkManagerScheduler"

class WorkManagerNotificationScheduler(private val context: Context) : NotificationScheduler {

    private val computeNextFireTime = ComputeNextFireTimeUseCase()

    override fun schedule(nudge: Nudge, schedule: Schedule) {
        enqueueWork(nudge.id, schedule)
    }

    override fun cancel(nudgeId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(nudgeWorkName(nudgeId))
        Log.i(TAG, "Cancelled scheduled notifications for nudge $nudgeId")
    }

    // Uses ExistingWorkPolicy.REPLACE so this atomically cancels any existing request and enqueues a new one.
    override fun reschedule(nudge: Nudge, schedule: Schedule) {
        enqueueWork(nudge.id, schedule)
    }

    private fun enqueueWork(nudgeId: String, schedule: Schedule) {
        val now = Clock.System.now()
        val nextFireTime = computeNextFireTime.execute(schedule, now, TimeZone.currentSystemDefault())
        val delayMillis = (nextFireTime - now).inWholeMilliseconds

        if (delayMillis <= 0) {
            Log.w(TAG, "Computed fire time is not in the future for nudge $nudgeId — skipping enqueue")
            return
        }

        WorkManager.getInstance(context).enqueueUniqueWork(
            nudgeWorkName(nudgeId),
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<NudgeNotificationWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(
                    WORKER_KEY_NUDGE_ID to nudgeId,
                    WORKER_KEY_SCHEDULED_AT to nextFireTime.toEpochMilliseconds()
                ))
                .build()
        )
        Log.i(TAG, "Notification enqueued for nudge $nudgeId, fires in ${delayMillis / 1000}s")
    }
}
