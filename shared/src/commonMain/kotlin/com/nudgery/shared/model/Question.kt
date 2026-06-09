// SPDX-License-Identifier: CC0-1.0

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
    val scaleMax: Int? = null,
    /**
     * YES/NO only (ED-17): when true, charts collapse this question's answers to a single Yes/No per
     * calendar day (any "YES" that day → Yes) instead of summing every answer. Display-only; raw
     * answers are unaffected. Default false (sum). Ignored for non-YES/NO types.
     */
    val collapsePerDay: Boolean = false
) {
    val isMainQuestion: Boolean get() = orderIndex == 0
}
