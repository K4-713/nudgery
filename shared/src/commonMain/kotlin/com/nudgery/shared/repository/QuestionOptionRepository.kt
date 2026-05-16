package com.nudgery.shared.repository

import com.nudgery.shared.model.QuestionOption

interface QuestionOptionRepository {
    suspend fun getByQuestionId(questionId: String): List<QuestionOption>
    suspend fun insert(option: QuestionOption)
    suspend fun updateText(optionId: String, text: String)
    suspend fun deleteByQuestionId(questionId: String)
}
