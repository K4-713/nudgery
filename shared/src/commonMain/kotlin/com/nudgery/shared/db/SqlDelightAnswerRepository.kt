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
            recordedAt = answer.recordedAt.toString(),
            isHidden = if (answer.isHidden) 1L else 0L
        )
    }

    override suspend fun setHidden(answerId: String, isHidden: Boolean) = withContext(Dispatchers.Default) {
        database.answerQueries.setHidden(
            isHidden = if (isHidden) 1L else 0L,
            id = answerId
        )
    }

    private fun com.nudgery.shared.db.Answer.toDomain() = Answer(
        id = id,
        nudgeId = nudgeId,
        questionId = questionId,
        value = value_,
        recordedAt = Instant.parse(recordedAt),
        isHidden = isHidden != 0L
    )
}
