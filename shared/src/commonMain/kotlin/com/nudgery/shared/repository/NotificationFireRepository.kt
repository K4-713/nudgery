// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.repository

import com.nudgery.shared.model.NotificationFire
import kotlinx.coroutines.flow.Flow

interface NotificationFireRepository {
    suspend fun insert(fire: NotificationFire)
    suspend fun getMostRecentByNudgeId(nudgeId: String): NotificationFire?
    fun observeMostRecentByNudgeId(nudgeId: String): Flow<NotificationFire?>
    fun observeAll(): Flow<List<NotificationFire>>
}
