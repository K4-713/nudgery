// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.notification

internal const val NUDGE_CHANNEL_ID = "nudge_prompts"
internal const val WORKER_KEY_NUDGE_ID = "nudge_id"
internal const val WORKER_KEY_SCHEDULED_AT = "scheduled_at_ms"

/**
 * The system notification id for a nudge's alert. There is one slot per nudge (a new fire reuses it),
 * so this is also the id to cancel when dismissing the alert on answer (ED-18). Kept in one place so
 * the worker that posts and the presenter that dismisses can never disagree.
 */
internal fun nudgeNotificationId(nudgeId: String): Int = nudgeId.hashCode()

// Public — read by androidApp receivers and work-data builders.
const val EXTRA_NUDGE_ID = "com.nudgery.android.EXTRA_NUDGE_ID"
const val EXTRA_SCHEDULED_AT = "com.nudgery.android.EXTRA_SCHEDULED_AT"
const val KEY_CATCH_UP_MISSED = "catch_up_missed"
const val KEY_TIMEZONE_FROM = "timezone_from"
const val KEY_TIMEZONE_TO = "timezone_to"

// SharedPreferences file and key used to track the last known timezone across process restarts.
const val NUDGERY_SYSTEM_PREFS = "nudgery_system"
const val KEY_LAST_TIMEZONE = "last_known_timezone"
