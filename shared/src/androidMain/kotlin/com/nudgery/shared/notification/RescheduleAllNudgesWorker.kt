package com.nudgery.shared.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.repository.ScheduleRepository
import com.nudgery.shared.scheduler.NotificationScheduler
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val TAG = "RescheduleAllNudgesWorker"

class RescheduleAllNudgesWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val nudgeRepository: NudgeRepository by inject()
    private val scheduleRepository: ScheduleRepository by inject()
    private val notificationScheduler: NotificationScheduler by inject()

    override suspend fun doWork(): Result {
        Log.i(TAG, "Rescheduling all enabled nudges")
        val enabledNudges = nudgeRepository.observeAll().first().filter { it.isEnabled }
        var rescheduled = 0
        enabledNudges.forEach { nudge ->
            val schedule = scheduleRepository.getByNudgeId(nudge.id)
            if (schedule != null) {
                notificationScheduler.schedule(nudge, schedule)
                rescheduled++
            } else {
                Log.w(TAG, "Enabled nudge ${nudge.id} has no schedule — skipping")
            }
        }
        Log.i(TAG, "Rescheduled $rescheduled of ${enabledNudges.size} enabled nudges")
        return Result.success()
    }
}
