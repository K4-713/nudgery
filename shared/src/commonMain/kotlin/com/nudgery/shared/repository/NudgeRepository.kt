package com.nudgery.shared.repository

import com.nudgery.shared.model.Nudge
import kotlinx.coroutines.flow.Flow

interface NudgeRepository {
    fun observeAll(): Flow<List<Nudge>>
    suspend fun getById(id: String): Nudge?
    suspend fun insert(nudge: Nudge)
    suspend fun update(nudge: Nudge)
    suspend fun setEnabled(nudgeId: String, isEnabled: Boolean)
    suspend fun delete(nudgeId: String)
}
