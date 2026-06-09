// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.repository

import com.nudgery.shared.model.TimezoneChangeEvent

interface TimezoneChangeEventRepository {
    suspend fun insert(event: TimezoneChangeEvent)
    suspend fun getAll(): List<TimezoneChangeEvent>
}
