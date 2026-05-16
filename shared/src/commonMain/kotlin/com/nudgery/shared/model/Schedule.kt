package com.nudgery.shared.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

data class Schedule(
    val id: String,
    val nudgeId: String,
    val type: ScheduleType,
    val timeOfDay: LocalTime,
    val activeDaysOfWeek: Set<DayOfWeek>?,
    val dayOfMonth: Int?,
    val activeHours: Set<Int>?
)
