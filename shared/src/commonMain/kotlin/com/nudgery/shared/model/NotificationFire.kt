package com.nudgery.shared.model

import kotlinx.datetime.Instant

data class NotificationFire(
    val id: String,
    val nudgeId: String,
    val firedAt: Instant
)
