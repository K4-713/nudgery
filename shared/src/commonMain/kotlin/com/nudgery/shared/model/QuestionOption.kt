// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.model

data class QuestionOption(
    val id: String,
    val questionId: String,
    val text: String,
    val orderIndex: Int
)
