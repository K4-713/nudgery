// SPDX-License-Identifier: CC0-1.0

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
    /** Raw text from the scale min/max fields; kept in sync with [scaleMin]/[scaleMax] when the
     *  text is a valid integer, but allowed to diverge for intermediate states like "-" or "3.5". */
    val scaleMinText: String = "0",
    val scaleMaxText: String = "10",
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
