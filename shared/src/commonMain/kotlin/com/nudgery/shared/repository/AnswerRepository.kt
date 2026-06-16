// SPDX-License-Identifier: CC0-1.0

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
    /** How many answers (visible or hidden) are recorded against a single question. */
    suspend fun countByQuestionId(questionId: String): Int
    /** Deletes every answer recorded against a single question. Used when a question is removed,
     *  since the Answer→Question foreign key has no ON DELETE CASCADE (ED-29). */
    suspend fun deleteByQuestionId(questionId: String)
    fun observeAll(): Flow<List<Answer>>
}
