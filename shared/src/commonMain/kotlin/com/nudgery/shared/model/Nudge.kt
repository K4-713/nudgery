// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.model

import kotlinx.datetime.Instant

data class Nudge(
    val id: String,
    val name: String,
    val isEnabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
