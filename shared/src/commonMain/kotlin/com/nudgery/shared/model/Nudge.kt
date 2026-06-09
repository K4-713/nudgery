// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.model

import kotlinx.datetime.Instant

data class Nudge(
    val id: String,
    val name: String,
    val isEnabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    // ED-19: user-defined list position. Assigned by the data layer (inserts append), so callers
    // that build a Nudge for create/update can leave it at the default — the insert computes the
    // appended value and the update query never writes it.
    val sortOrder: Int = 0
)
