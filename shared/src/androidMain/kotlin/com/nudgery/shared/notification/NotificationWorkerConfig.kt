package com.nudgery.shared.notification

internal const val NUDGE_CHANNEL_ID = "nudge_prompts"
internal const val WORKER_KEY_NUDGE_ID = "nudge_id"

internal fun nudgeWorkName(nudgeId: String) = "nudge_notification_$nudgeId"
