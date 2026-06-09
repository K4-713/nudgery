// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.viewmodel

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

internal fun LocalTime.toDisplayString(): String {
    val h = if (hour % 12 == 0) 12 else hour % 12
    val period = if (hour < 12) "AM" else "PM"
    return if (minute == 0) "$h $period" else "$h:%02d $period".format(minute)
}

internal fun Instant.toLocalDisplayString(
    timeZone: TimeZone,
    now: Instant = Clock.System.now(),
    approximate: Boolean = false
): String {
    val local = toLocalDateTime(timeZone)
    val today = now.toLocalDateTime(timeZone).date
    val tomorrow = today.plus(1, DateTimeUnit.DAY)
    val timeStr = local.time.toDisplayString()
    val separator = if (approximate) ", around " else " at "

    return when (local.date) {
        today -> "Today${separator}$timeStr"
        tomorrow -> "Tomorrow${separator}$timeStr"
        else -> {
            val monthName = local.month.name.lowercase().replaceFirstChar { it.uppercase() }
            "$monthName ${local.dayOfMonth}${separator}$timeStr"
        }
    }
}
