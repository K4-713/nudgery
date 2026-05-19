package com.nudgery.shared.notification

internal const val NUDGE_CHANNEL_ID = "nudge_prompts"
internal const val WORKER_KEY_NUDGE_ID = "nudge_id"
internal const val WORKER_KEY_SCHEDULED_AT = "scheduled_at_ms"

// Public — read by androidApp when handling the notification tap intent.
const val EXTRA_NUDGE_ID = "com.nudgery.android.EXTRA_NUDGE_ID"
const val EXTRA_SCHEDULED_AT = "com.nudgery.android.EXTRA_SCHEDULED_AT"
