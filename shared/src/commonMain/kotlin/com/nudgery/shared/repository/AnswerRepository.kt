package com.nudgery.shared.repository

import com.nudgery.shared.model.Answer
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface AnswerRepository {
    fun observeByNudgeId(nudgeId: String): Flow<List<Answer>>
    fun observeVisibleByNudgeId(nudgeId: String): Flow<List<Answer>>
    suspend fun getAllByNudgeId(nudgeId: String): List<Answer>
    suspend fun getVisibleByNudgeIdSince(nudgeId: String, since: Instant): List<Answer>
    suspend fun insert(answer: Answer)
    suspend fun setHidden(answerId: String, isHidden: Boolean)
    suspend fun getMostRecentAnsweredAtByNudgeId(nudgeId: String): Instant?
    fun observeAll(): Flow<List<Answer>>
}
