package com.nudgery.shared.db

import com.nudgery.shared.model.QuestionOption
import com.nudgery.shared.repository.QuestionOptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlDelightQuestionOptionRepository(private val database: NudgeryDatabase) : QuestionOptionRepository {

    override suspend fun getByQuestionId(questionId: String): List<QuestionOption> = withContext(Dispatchers.Default) {
        database.questionOptionQueries.selectByQuestionId(questionId).executeAsList().map { it.toDomain() }
    }

    override suspend fun insert(option: QuestionOption) = withContext(Dispatchers.Default) {
        database.questionOptionQueries.insert(
            id = option.id,
            questionId = option.questionId,
            text = option.text,
            orderIndex = option.orderIndex.toLong()
        )
    }

    override suspend fun updateText(optionId: String, text: String) = withContext(Dispatchers.Default) {
        database.questionOptionQueries.updateText(text = text, id = optionId)
    }

    override suspend fun updateOrderIndex(optionId: String, orderIndex: Int) = withContext(Dispatchers.Default) {
        database.questionOptionQueries.updateOrderIndex(orderIndex = orderIndex.toLong(), id = optionId)
    }

    override suspend fun deleteByQuestionId(questionId: String) = withContext(Dispatchers.Default) {
        database.questionOptionQueries.deleteByQuestionId(questionId)
    }

    private fun com.nudgery.shared.db.QuestionOption.toDomain() = QuestionOption(
        id = id,
        questionId = questionId,
        text = text,
        orderIndex = orderIndex.toInt()
    )
}
