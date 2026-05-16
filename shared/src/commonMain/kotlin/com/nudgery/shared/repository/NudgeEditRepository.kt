package com.nudgery.shared.repository

import com.nudgery.shared.model.NudgeEdit

interface NudgeEditRepository {
    suspend fun getByNudgeId(nudgeId: String): List<NudgeEdit>
    suspend fun insert(edit: NudgeEdit)
}
