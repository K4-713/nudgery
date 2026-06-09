// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nudgery.shared.model.TimezoneChangeEvent
import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.repository.ScheduleRepository
import com.nudgery.shared.repository.TimezoneChangeEventRepository
import com.nudgery.shared.scheduler.NotificationScheduler
import com.nudgery.shared.usecase.CatchUpMissedFiresUseCase
import com.nudgery.shared.util.generateUuid
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
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
    private val catchUpMissedFires: CatchUpMissedFiresUseCase by inject()
    private val timezoneChangeEventRepository: TimezoneChangeEventRepository by inject()

    override suspend fun doWork(): Result {
        val catchUp = inputData.getBoolean(KEY_CATCH_UP_MISSED, false)
        val tzFrom = inputData.getString(KEY_TIMEZONE_FROM)
        val tzTo = inputData.getString(KEY_TIMEZONE_TO)

        recordTimezoneChangeIfPresent(tzFrom, tzTo)

        Log.i(TAG, "Rescheduling all enabled nudges (catchUp=$catchUp)")
        val enabledNudges = nudgeRepository.observeAll().first().filter { it.isEnabled }
        val now = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        var rescheduled = 0
        var caughtUp = 0

        enabledNudges.forEach { nudge ->
            val schedule = scheduleRepository.getByNudgeId(nudge.id)
            if (schedule == null) {
                Log.w(TAG, "Enabled nudge ${nudge.id} has no schedule — skipping")
                return@forEach
            }

            if (catchUp) {
                val catchUpResult = catchUpMissedFires.execute(nudge, schedule, now, tz)
                if (catchUpResult is CatchUpMissedFiresUseCase.Result.FireNow) {
                    // Enqueue an immediate notification for the missed fire. That worker will
                    // show the notification and reschedule the next alarm, so we must not
                    // also call schedule() here for this nudge.
                    WorkManager.getInstance(applicationContext).enqueue(
                        OneTimeWorkRequestBuilder<NudgeNotificationWorker>()
                            .setInputData(workDataOf(
                                WORKER_KEY_NUDGE_ID to nudge.id,
                                WORKER_KEY_SCHEDULED_AT to catchUpResult.missedScheduledAt.toEpochMilliseconds()
                            ))
                            .build()
                    )
                    Log.i(TAG, "Catch-up notification enqueued for nudge ${nudge.id} " +
                            "(missed ${catchUpResult.missedScheduledAt})")
                    caughtUp++
                    return@forEach
                }
            }

            notificationScheduler.schedule(nudge, schedule)
            rescheduled++
        }

        Log.i(TAG, "Rescheduled $rescheduled nudge(s), sent catch-up for $caughtUp nudge(s) " +
                "of ${enabledNudges.size} enabled")
        return Result.success()
    }

    private suspend fun recordTimezoneChangeIfPresent(tzFrom: String?, tzTo: String?) {
        if (tzFrom == null || tzTo == null || tzFrom == tzTo) return
        val event = TimezoneChangeEvent(
            id = generateUuid(),
            changedAt = Clock.System.now(),
            fromTimezone = tzFrom,
            toTimezone = tzTo
        )
        timezoneChangeEventRepository.insert(event)
        Log.i(TAG, "Timezone change recorded: $tzFrom → $tzTo at ${event.changedAt}")
    }
}
