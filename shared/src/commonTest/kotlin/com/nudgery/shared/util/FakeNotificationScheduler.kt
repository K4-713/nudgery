package com.nudgery.shared.util

import com.nudgery.shared.model.Nudge
import com.nudgery.shared.model.Schedule
import com.nudgery.shared.scheduler.NotificationScheduler

class FakeNotificationScheduler : NotificationScheduler {
    val scheduled = mutableListOf<Pair<Nudge, Schedule>>()
    val cancelled = mutableListOf<String>()
    val rescheduled = mutableListOf<Pair<Nudge, Schedule>>()

    override fun schedule(nudge: Nudge, schedule: Schedule) {
        scheduled += nudge to schedule
    }

    override fun cancel(nudgeId: String) {
        cancelled += nudgeId
    }

    override fun reschedule(nudge: Nudge, schedule: Schedule) {
        rescheduled += nudge to schedule
    }

    fun reset() {
        scheduled.clear()
        cancelled.clear()
        rescheduled.clear()
    }
}
