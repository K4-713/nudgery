package com.nudgery.shared.db

import com.nudgery.shared.model.Question
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.TriggerOperator
import com.nudgery.shared.repository.QuestionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlDelightQuestionRepository(private val database: NudgeryDatabase) : QuestionRepository {

    override suspend fun getByNudgeId(nudgeId: String): List<Question> = withContext(Dispatchers.Default) {
        database.questionQueries.selectByNudgeId(nudgeId).executeAsList().map { it.toDomain() }
    }

    override suspend fun insert(question: Question) = withContext(Dispatchers.Default) {
        database.questionQueries.insert(
            id = question.id,
            nudgeId = question.nudgeId,
            text = question.text,
            type = question.type.name,
            orderIndex = question.orderIndex.toLong(),
            triggerAnswerValue = question.triggerAnswerValue,
            triggerOperator = question.triggerOperator?.name,
            scale_min = question.scaleMin?.toLong(),
            scale_max = question.scaleMax?.toLong()
        )
    }

    override suspend fun updateText(questionId: String, text: String) = withContext(Dispatchers.Default) {
        database.questionQueries.updateText(text = text, id = questionId)
    }

    override suspend fun update(question: Question) = withContext(Dispatchers.Default) {
        database.questionQueries.update(
            id = question.id,
            text = question.text,
            type = question.type.name,
            orderIndex = question.orderIndex.toLong(),
            triggerAnswerValue = question.triggerAnswerValue,
            triggerOperator = question.triggerOperator?.name,
            scale_min = question.scaleMin?.toLong(),
            scale_max = question.scaleMax?.toLong()
        )
    }

    override suspend fun deleteById(questionId: String) = withContext(Dispatchers.Default) {
        database.questionQueries.deleteById(questionId)
    }

    override suspend fun deleteByNudgeId(nudgeId: String) = withContext(Dispatchers.Default) {
        database.questionQueries.deleteByNudgeId(nudgeId)
    }

    private fun com.nudgery.shared.db.Question.toDomain() = Question(
        id = id,
        nudgeId = nudgeId,
        text = text,
        type = QuestionType.valueOf(type),
        orderIndex = orderIndex.toInt(),
        triggerAnswerValue = triggerAnswerValue,
        triggerOperator = triggerOperator?.let { TriggerOperator.valueOf(it) },
        scaleMin = scale_min?.toInt(),
        scaleMax = scale_max?.toInt()
    )
}
