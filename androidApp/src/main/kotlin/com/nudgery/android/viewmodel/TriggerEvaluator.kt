// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.viewmodel

import com.nudgery.shared.model.TriggerOperator

internal fun evaluateTrigger(triggerValue: String, operator: TriggerOperator?, answer: String): Boolean {
    val effectiveOperator = operator ?: TriggerOperator.EQ
    val answerNum by lazy { answer.toDoubleOrNull() }
    val triggerNum by lazy { triggerValue.toDoubleOrNull() }
    return when (effectiveOperator) {
        TriggerOperator.ALWAYS -> true
        TriggerOperator.EQ -> answer == triggerValue
        TriggerOperator.CONTAINS -> answer.split(",").contains(triggerValue)
        TriggerOperator.GT -> answerNum != null && triggerNum != null && answerNum!! > triggerNum!!
        TriggerOperator.GTE -> answerNum != null && triggerNum != null && answerNum!! >= triggerNum!!
        TriggerOperator.LT -> answerNum != null && triggerNum != null && answerNum!! < triggerNum!!
        TriggerOperator.LTE -> answerNum != null && triggerNum != null && answerNum!! <= triggerNum!!
    }
}
