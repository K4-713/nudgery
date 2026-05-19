package com.nudgery.shared.model

import kotlinx.datetime.Instant

data class Answer(
    val id: String,
    val nudgeId: String,
    val questionId: String,
    val value: String,
    val scheduledAt: Instant,
    val answeredAt: Instant,
    val isHidden: Boolean
)
