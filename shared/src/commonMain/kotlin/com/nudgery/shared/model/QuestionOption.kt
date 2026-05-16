package com.nudgery.shared.model

data class QuestionOption(
    val id: String,
    val questionId: String,
    val text: String,
    val orderIndex: Int
)
