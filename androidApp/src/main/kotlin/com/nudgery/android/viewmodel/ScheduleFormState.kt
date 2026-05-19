package com.nudgery.android.viewmodel

import com.nudgery.shared.model.Schedule
import com.nudgery.shared.model.ScheduleType
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
    val timeOfDay: LocalTime = LocalTime(12, 0),
    val activeDaysOfWeek: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val dayOfMonth: Int = 1,
    val activeHours: Set<Int> = (8..20).toSet()
) {
    fun toRequest() = ScheduleRequest(
        type = type,
        timeOfDay = timeOfDay,
        activeDaysOfWeek = when (type) {
            ScheduleType.DAILY, ScheduleType.HOURLY -> activeDaysOfWeek
            else -> null
        },
        dayOfMonth = if (type == ScheduleType.MONTHLY) dayOfMonth else null,
        activeHours = if (type == ScheduleType.HOURLY) activeHours else null
    )

    fun toDescription(): String = when (type) {
        ScheduleType.DAILY -> {
            val daysLabel = when (activeDaysOfWeek) {
                ALL_DAYS -> "Every Day"
                WEEKDAYS -> "Weekdays"
                WEEKENDS -> "Weekends"
                else -> activeDaysOfWeek.sortedBy { it.ordinal }.joinToString(", ") { it.toAbbreviation() }
            }
            "Daily at ${timeOfDay.toDisplayString()}, $daysLabel"
        }
        ScheduleType.WEEKLY -> {
            val day = activeDaysOfWeek.firstOrNull()
                ?.name?.lowercase()?.replaceFirstChar { c -> c.uppercase() } ?: ""
            "Weekly on $day at ${timeOfDay.toDisplayString()}"
        }
        ScheduleType.MONTHLY -> "Monthly on day $dayOfMonth at ${timeOfDay.toDisplayString()}"
        ScheduleType.HOURLY -> {
            val hours = activeHours.sorted().joinToString(", ") { h ->
                if (h == 0) "12 AM" else if (h < 12) "$h AM" else if (h == 12) "12 PM" else "${h - 12} PM"
            }
            "Hourly: $hours"
        }
    }

    companion object {
        fun fromSchedule(schedule: Schedule) = ScheduleFormState(
            type = schedule.type,
            timeOfDay = schedule.timeOfDay,
            activeDaysOfWeek = schedule.activeDaysOfWeek ?: DayOfWeek.entries.toSet(),
            dayOfMonth = schedule.dayOfMonth ?: 1,
            activeHours = schedule.activeHours ?: (8..20).toSet()
        )
    }
}
