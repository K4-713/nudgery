package com.nudgery.shared.model

data class Question(
    val id: String,
    val nudgeId: String,
    val text: String,
    val type: QuestionType,
    val orderIndex: Int,
    val triggerAnswerValue: String?,
    val triggerOperator: TriggerOperator?,
    val scaleMin: Int? = null,
    val scaleMax: Int? = null
) {
    val isMainQuestion: Boolean get() = orderIndex == 0
}
