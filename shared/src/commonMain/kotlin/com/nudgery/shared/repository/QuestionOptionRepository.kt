// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.repository

import com.nudgery.shared.model.QuestionOption

interface QuestionOptionRepository {
    suspend fun getByQuestionId(questionId: String): List<QuestionOption>
    suspend fun insert(option: QuestionOption)
    suspend fun updateText(optionId: String, text: String)
    suspend fun updateOrderIndex(optionId: String, orderIndex: Int)
    suspend fun deleteById(optionId: String)
    suspend fun deleteByQuestionId(questionId: String)
}
