// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.scheduler

import com.nudgery.shared.model.Nudge
import com.nudgery.shared.model.Schedule

interface NotificationScheduler {
    fun schedule(nudge: Nudge, schedule: Schedule)
    fun cancel(nudgeId: String)
    fun reschedule(nudge: Nudge, schedule: Schedule)
}
