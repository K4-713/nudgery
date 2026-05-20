package com.nudgery.shared.notification

import android.app.PendingIntent
import com.nudgery.shared.R
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nudgery.shared.model.NotificationFire
import com.nudgery.shared.model.Nudge
import com.nudgery.shared.model.Schedule
import com.nudgery.shared.repository.NotificationFireRepository
import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.repository.QuestionRepository
import com.nudgery.shared.repository.ScheduleRepository
import com.nudgery.shared.scheduler.NotificationScheduler
import com.nudgery.shared.util.generateUuid
import kotlinx.datetime.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val TAG = "NudgeNotificationWorker"

class NudgeNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val nudgeRepository: NudgeRepository by inject()
    private val questionRepository: QuestionRepository by inject()
    private val scheduleRepository: ScheduleRepository by inject()
    private val notificationScheduler: NotificationScheduler by inject()
    private val notificationFireRepository: NotificationFireRepository by inject()

    override suspend fun doWork(): Result {
        val nudgeId = inputData.getString(WORKER_KEY_NUDGE_ID)
        if (nudgeId == null) {
            Log.e(TAG, "Worker received no nudge ID in input data")
            return Result.failure()
        }
        val scheduledAtMs = inputData.getLong(WORKER_KEY_SCHEDULED_AT, -1L)

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

        val firedAt = Clock.System.now()
        showNotification(nudgeId, nudge.name, mainQuestion?.text ?: nudge.name, scheduledAtMs)
        notificationFireRepository.insert(NotificationFire(generateUuid(), nudgeId, firedAt))
        Log.i(TAG, "Recorded notification fire for nudge $nudgeId at $firedAt")
        scheduleNextFire(nudge, schedule)

        return Result.success()
    }

    private fun showNotification(nudgeId: String, title: String, text: String, scheduledAtMs: Long) {
        val launchIntent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ?.putExtra(EXTRA_NUDGE_ID, nudgeId)
            ?.putExtra(EXTRA_SCHEDULED_AT, scheduledAtMs)

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            nudgeId.hashCode(),
            launchIntent ?: Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, NUDGE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(nudgeId.hashCode(), notification)
        Log.i(TAG, "Notification shown for nudge: $nudgeId")
    }

    private fun scheduleNextFire(nudge: Nudge, schedule: Schedule?) {
        if (schedule == null) {
            Log.w(TAG, "No schedule found for nudge ${nudge.id} — notification fired but cannot reschedule")
            return
        }
        notificationScheduler.reschedule(nudge, schedule)
    }
}
