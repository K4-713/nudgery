package com.nudgery.shared.model

import kotlinx.datetime.Instant

data class TimezoneChangeEvent(
    val id: String,
    val changedAt: Instant,
    val fromTimezone: String,
    val toTimezone: String
)
