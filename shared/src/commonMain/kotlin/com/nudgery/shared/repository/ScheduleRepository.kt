// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.repository

import com.nudgery.shared.model.Schedule

interface ScheduleRepository {
    suspend fun getByNudgeId(nudgeId: String): Schedule?
    suspend fun insert(schedule: Schedule)
    suspend fun update(schedule: Schedule)
    suspend fun deleteByNudgeId(nudgeId: String)
}
