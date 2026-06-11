// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.viewmodel

import com.nudgery.shared.model.Schedule
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.model.buildHourlyWindow
import com.nudgery.shared.model.orderedHourlyWindow
import com.nudgery.shared.usecase.ScheduleRequest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

private val WEEKDAYS = setOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
)
private val WEEKENDS = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
private val ALL_DAYS = DayOfWeek.entries.toSet()

fun DayOfWeek.toAbbreviation(): String = when (this) {
    DayOfWeek.MONDAY -> "M"
    DayOfWeek.TUESDAY -> "Tu"
    DayOfWeek.WEDNESDAY -> "W"
    DayOfWeek.THURSDAY -> "Th"
    DayOfWeek.FRIDAY -> "F"
    DayOfWeek.SATURDAY -> "Sa"
    DayOfWeek.SUNDAY -> "Su"
    else -> name.take(2)
}

data class ScheduleFormState(
    val type: ScheduleType = ScheduleType.DAILY,
    // For HOURLY this is the first nudge of the day; its minute is shared by every hourly nudge.
    val timeOfDay: LocalTime = LocalTime(12, 0),
    val activeDaysOfWeek: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val dayOfMonth: Int = 1,
    // HOURLY only: the hour of the last nudge of the day. With timeOfDay's hour as the start, this
    // defines the (possibly midnight-wrapping) window. The last nudge fires at timeOfDay's minute.
    val hourlyEndHour: Int = 20
) {
    fun toRequest() = ScheduleRequest(
        type = type,
        timeOfDay = timeOfDay,
        activeDaysOfWeek = when (type) {
            ScheduleType.DAILY, ScheduleType.HOURLY -> activeDaysOfWeek
            else -> null
        },
        dayOfMonth = if (type == ScheduleType.MONTHLY) dayOfMonth else null,
        activeHours = if (type == ScheduleType.HOURLY)
            buildHourlyWindow(timeOfDay.hour, hourlyEndHour).toSet() else null
    )

    /**
     * Label for the day-of-week set used by DAILY and HOURLY descriptions. Deselecting every day is
     * an allowed (if unusual) state — the nudge simply never fires — so it gets an explicit label
     * rather than rendering as an empty string after a trailing comma.
     */
    private fun activeDaysLabel(): String = when {
        activeDaysOfWeek.isEmpty() -> "no days enabled"
        activeDaysOfWeek == ALL_DAYS -> "Every Day"
        activeDaysOfWeek == WEEKDAYS -> "Weekdays"
        activeDaysOfWeek == WEEKENDS -> "Weekends"
        else -> activeDaysOfWeek.sortedBy { it.ordinal }.joinToString(", ") { it.toAbbreviation() }
    }

    fun toDescription(): String = when (type) {
        ScheduleType.DAILY -> "Daily at ${timeOfDay.toDisplayString()}, ${activeDaysLabel()}"
        ScheduleType.WEEKLY -> {
            val day = activeDaysOfWeek.firstOrNull()
                ?.name?.lowercase()?.replaceFirstChar { c -> c.uppercase() } ?: ""
            "Weekly on $day at ${timeOfDay.toDisplayString()}"
        }
        ScheduleType.MONTHLY -> "Monthly on day $dayOfMonth at ${timeOfDay.toDisplayString()}"
        ScheduleType.HOURLY -> {
            val lastTime = LocalTime(hourlyEndHour, timeOfDay.minute)
            "Hourly, ${timeOfDay.toDisplayString()}–${lastTime.toDisplayString()}, ${activeDaysLabel()}"
        }
    }

    companion object {
        fun fromSchedule(schedule: Schedule): ScheduleFormState {
            val window = orderedHourlyWindow(schedule.activeHours ?: emptySet())
            // Normalize the first nudge time to the window's actual start hour (keeping the stored
            // minute) so the form's start matches the stored window even for legacy schedules.
            val timeOfDay = if (schedule.type == ScheduleType.HOURLY && window.isNotEmpty()) {
                LocalTime(window.first(), schedule.timeOfDay.minute)
            } else {
                schedule.timeOfDay
            }
            return ScheduleFormState(
                type = schedule.type,
                timeOfDay = timeOfDay,
                activeDaysOfWeek = schedule.activeDaysOfWeek ?: DayOfWeek.entries.toSet(),
                dayOfMonth = schedule.dayOfMonth ?: 1,
                hourlyEndHour = window.lastOrNull() ?: 20
            )
        }
    }
}
