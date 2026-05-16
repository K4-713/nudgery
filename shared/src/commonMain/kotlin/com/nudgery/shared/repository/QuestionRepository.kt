package com.nudgery.shared.repository

import com.nudgery.shared.model.Question

interface QuestionRepository {
    suspend fun getByNudgeId(nudgeId: String): List<Question>
    suspend fun insert(question: Question)
    suspend fun updateText(questionId: String, text: String)
    suspend fun deleteByNudgeId(nudgeId: String)
}
