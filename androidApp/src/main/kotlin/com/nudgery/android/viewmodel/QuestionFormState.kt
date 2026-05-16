package com.nudgery.android.viewmodel

import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.TriggerOperator
import com.nudgery.shared.usecase.QuestionRequest

data class QuestionFormState(
    val text: String = "",
    val type: QuestionType = QuestionType.YES_NO,
    val options: List<String> = emptyList(),
    val triggerAnswerValue: String? = null,
    val triggerOperator: TriggerOperator? = null
) {
    fun toRequest() = QuestionRequest(
        text = text,
        type = type,
        options = options,
        triggerAnswerValue = triggerAnswerValue,
        triggerOperator = triggerOperator
    )
}
