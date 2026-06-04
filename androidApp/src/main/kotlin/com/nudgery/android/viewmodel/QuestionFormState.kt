package com.nudgery.android.viewmodel

import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.TriggerOperator
import com.nudgery.shared.usecase.QuestionRequest

data class QuestionFormState(
    val text: String = "",
    val type: QuestionType = QuestionType.YES_NO,
    val options: List<String> = emptyList(),
    val triggerAnswerValue: String? = null,
    val triggerOperator: TriggerOperator? = null,
    val scaleMin: Int = 0,
    val scaleMax: Int = 10,
    /** YES/NO "One Yes Per Day" toggle (ED-17); only meaningful when [type] is YES_NO. */
    val collapsePerDay: Boolean = false
) {
    fun toRequest() = QuestionRequest(
        text = text,
        type = type,
        options = options,
        triggerAnswerValue = triggerAnswerValue,
        triggerOperator = triggerOperator,
        scaleMin = scaleMin,
        scaleMax = scaleMax,
        collapsePerDay = collapsePerDay
    )
}
