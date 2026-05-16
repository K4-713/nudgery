package com.nudgery.shared.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.repository.QuestionRepository
import com.nudgery.shared.repository.ScheduleRepository
import com.nudgery.shared.model.Schedule
import com.nudgery.shared.usecase.ComputeNextFireTimeUseCase
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

private const val TAG = "NudgeNotificationWorker"

class NudgeNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val nudgeRepository: NudgeRepository by inject()
    private val questionRepository: QuestionRepository by inject()
    private val scheduleRepository: ScheduleRepository by inject()
    private val computeNextFireTime = ComputeNextFireTimeUseCase()

    override suspend fun doWork(): Result {
        val nudgeId = inputData.getString(WORKER_KEY_NUDGE_ID)
        if (nudgeId == null) {
            Log.e(TAG, "Worker received no nudge ID in input data")
            return Result.failure()
        }

        val nudge = nudgeRepository.getById(nudgeId)
        if (nudge == null) {
            Log.w(TAG, "Nudge $nudgeId not found — skipping notification")
            return Result.success()
        }
        if (!nudge.isEnabled) {
            Log.i(TAG, "Nudge $nudgeId is disabled — skipping notification")
            return Result.success()
        }

        val questions = questionRepository.getByNudgeId(nudgeId)
        val mainQuestion = questions.firstOrNull { it.isMainQuestion }
        val schedule = scheduleRepository.getByNudgeId(nudgeId)

        showNotification(nudgeId, nudge.name, mainQuestion?.text ?: nudge.name)

        if (schedule != null) {
            scheduleNextFire(nudgeId, schedule)
        } else {
            Log.w(TAG, "No schedule found for nudge $nudgeId — notification fired but cannot reschedule")
        }

        return Result.success()
    }

    private fun showNotification(nudgeId: String, title: String, text: String) {
        val launchIntent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            nudgeId.hashCode(),
            launchIntent ?: Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, NUDGE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(nudgeId.hashCode(), notification)
        Log.i(TAG, "Notification shown for nudge: $nudgeId")
    }

    private fun scheduleNextFire(nudgeId: String, schedule: Schedule) {
        val now = Clock.System.now()
        val nextFireTime = computeNextFireTime.execute(schedule, now, TimeZone.currentSystemDefault())
        val delayMillis = (nextFireTime - now).inWholeMilliseconds

        if (delayMillis <= 0) {
            Log.w(TAG, "Computed next fire time is not in the future for nudge $nudgeId — skipping reschedule")
            return
        }

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            nudgeWorkName(nudgeId),
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<NudgeNotificationWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(WORKER_KEY_NUDGE_ID to nudgeId))
                .build()
        )
        Log.i(TAG, "Next notification for nudge $nudgeId scheduled in ${delayMillis / 1000}s")
    }
}
