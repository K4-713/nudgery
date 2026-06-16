// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.nudgery.shared.model.Answer
import com.nudgery.shared.repository.AnswerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

class SqlDelightAnswerRepository(private val database: NudgeryDatabase) : AnswerRepository {

    override fun observeByNudgeId(nudgeId: String): Flow<List<Answer>> =
        database.answerQueries.selectByNudgeId(nudgeId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeVisibleByNudgeId(nudgeId: String): Flow<List<Answer>> =
        database.answerQueries.selectVisibleByNudgeId(nudgeId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getAllByNudgeId(nudgeId: String): List<Answer> = withContext(Dispatchers.Default) {
        database.answerQueries.selectByNudgeId(nudgeId).executeAsList().map { it.toDomain() }
    }

    override suspend fun getVisibleByNudgeIdSince(nudgeId: String, since: Instant): List<Answer> =
        withContext(Dispatchers.Default) {
            database.answerQueries
                .selectVisibleByNudgeIdSince(nudgeId = nudgeId, since = since.toString())
                .executeAsList()
                .map { it.toDomain() }
        }

    override suspend fun insert(answer: Answer) = withContext(Dispatchers.Default) {
        database.answerQueries.insert(
            id = answer.id,
            nudgeId = answer.nudgeId,
            questionId = answer.questionId,
            value = answer.value,
            scheduledAt = answer.scheduledAt.toString(),
            answeredAt = answer.answeredAt.toString(),
            isHidden = if (answer.isHidden) 1L else 0L
        )
    }

    override suspend fun setHidden(answerId: String, isHidden: Boolean) = withContext(Dispatchers.Default) {
        database.answerQueries.setHidden(
            isHidden = if (isHidden) 1L else 0L,
            id = answerId
        )
    }

    override suspend fun getMostRecentAnsweredAtByNudgeId(nudgeId: String): Instant? =
        withContext(Dispatchers.Default) {
            database.answerQueries
                .selectMostRecentAnsweredAtByNudgeId(nudgeId)
                .executeAsOneOrNull()
                ?.mostRecentAnsweredAt
                ?.let { Instant.parse(it) }
        }

    override suspend fun countByQuestionId(questionId: String): Int = withContext(Dispatchers.Default) {
        database.answerQueries.countByQuestionId(questionId).executeAsOne().toInt()
    }

    override suspend fun deleteByQuestionId(questionId: String) = withContext(Dispatchers.Default) {
        database.answerQueries.deleteByQuestionId(questionId)
    }

    override fun observeAll(): Flow<List<Answer>> =
        database.answerQueries.observeAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    private fun com.nudgery.shared.db.Answer.toDomain() = Answer(
        id = id,
        nudgeId = nudgeId,
        questionId = questionId,
        value = value_,
        scheduledAt = Instant.parse(scheduledAt),
        answeredAt = Instant.parse(answeredAt),
        isHidden = isHidden != 0L
    )
}
