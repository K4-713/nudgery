package com.nudgery.shared.model

import kotlinx.datetime.Instant

data class NudgeEdit(
    val id: String,
    val nudgeId: String,
    val editedAt: Instant,
    val fieldChanged: String,
    val previousValue: String
)
