package com.nudgery.shared.usecase

import com.nudgery.shared.model.Nudge
import com.nudgery.shared.model.Schedule
import com.nudgery.shared.repository.NotificationFireRepository
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

// Upper bound on loop iterations — covers hourly nudges missed for up to ~3 weeks.
private const val MAX_CATCHUP_STEPS = 500

class CatchUpMissedFiresUseCase(
    private val notificationFireRepository: NotificationFireRepository,
    private val computeNextFireTime: ComputeNextFireTimeUseCase
) {
    sealed class Result {
        /** A notification was missed. Show one for this scheduled time, then reschedule normally. */
        data class FireNow(val missedScheduledAt: Instant) : Result()
        /** Nothing was missed. Schedule the next future alarm as usual. */
        object ScheduleNext : Result()
    }

    suspend fun execute(nudge: Nudge, schedule: Schedule, now: Instant, timeZone: TimeZone): Result {
        val lastKnownFiredAt = notificationFireRepository.getMostRecentByNudgeId(nudge.id)?.firedAt
            ?: nudge.createdAt

        var ref = lastKnownFiredAt
        var mostRecentMissed: Instant? = null

        for (step in 0 until MAX_CATCHUP_STEPS) {
            val next = runCatching {
                computeNextFireTime.execute(schedule, ref, timeZone)
            }.getOrNull() ?: break
            if (next >= now) break
            mostRecentMissed = next
            ref = next
        }

        return if (mostRecentMissed != null) {
            Result.FireNow(mostRecentMissed!!)
        } else {
            Result.ScheduleNext
        }
    }
}
