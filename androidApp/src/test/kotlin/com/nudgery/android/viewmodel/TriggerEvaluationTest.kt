package com.nudgery.android.viewmodel

import com.nudgery.shared.model.TriggerOperator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerEvaluationTest {

    // YES/NO triggers (EQ with no explicit operator)

    @Test
    fun TDD_yesNoTrigger_matchesYes() {
        assertTrue(evaluateTrigger("YES", null, "YES"))
    }

    @Test
    fun TDD_yesNoTrigger_doesNotMatchNo() {
        assertFalse(evaluateTrigger("YES", null, "NO"))
    }

    @Test
    fun TDD_yesNoTrigger_noOperatorDefaultsToEq() {
        assertTrue(evaluateTrigger("NO", null, "NO"))
        assertFalse(evaluateTrigger("NO", null, "YES"))
    }

    // Numeric triggers (GT, GTE, LT, LTE, EQ)

    @Test
    fun TDD_numericTrigger_gt_firesWhenAnswerIsGreater() {
        assertTrue(evaluateTrigger("5", TriggerOperator.GT, "7"))
    }

    @Test
    fun TDD_numericTrigger_gt_doesNotFireWhenAnswerIsEqual() {
        assertFalse(evaluateTrigger("5", TriggerOperator.GT, "5"))
    }

    @Test
    fun TDD_numericTrigger_gte_firesWhenAnswerIsEqual() {
        assertTrue(evaluateTrigger("5", TriggerOperator.GTE, "5"))
    }

    @Test
    fun TDD_numericTrigger_gte_firesWhenAnswerIsGreater() {
        assertTrue(evaluateTrigger("5", TriggerOperator.GTE, "8"))
    }

    @Test
    fun TDD_numericTrigger_lt_firesWhenAnswerIsLess() {
        assertTrue(evaluateTrigger("5", TriggerOperator.LT, "3"))
    }

    @Test
    fun TDD_numericTrigger_lte_firesWhenAnswerIsEqual() {
        assertTrue(evaluateTrigger("5", TriggerOperator.LTE, "5"))
    }

    @Test
    fun TDD_numericTrigger_eq_firesOnExactMatch() {
        assertTrue(evaluateTrigger("7", TriggerOperator.EQ, "7"))
        assertFalse(evaluateTrigger("7", TriggerOperator.EQ, "8"))
    }

    @Test
    fun TDD_numericTrigger_nonNumericAnswerDoesNotFire() {
        assertFalse(evaluateTrigger("5", TriggerOperator.GT, "notanumber"))
    }

    // OPTION_MULTI triggers (CONTAINS)

    @Test
    fun TDD_containsTrigger_firesWhenOptionIsPresentInMultiAnswer() {
        // Multi-select answer is comma-separated option IDs
        assertTrue(evaluateTrigger("opt-b", TriggerOperator.CONTAINS, "opt-a,opt-b,opt-c"))
    }

    @Test
    fun TDD_containsTrigger_doesNotFireWhenOptionIsAbsent() {
        assertFalse(evaluateTrigger("opt-d", TriggerOperator.CONTAINS, "opt-a,opt-b,opt-c"))
    }

    @Test
    fun TDD_containsTrigger_firesOnSingleOptionAnswer() {
        assertTrue(evaluateTrigger("opt-a", TriggerOperator.CONTAINS, "opt-a"))
    }

    @Test
    fun TDD_containsTrigger_doesNotMatchPartialId() {
        // "opt-a" should not match "opt-ab"
        assertFalse(evaluateTrigger("opt-a", TriggerOperator.CONTAINS, "opt-ab"))
    }

    // OPTION_SINGLE triggers (EQ with option ID)

    @Test
    fun TDD_optionSingleTrigger_firesOnMatchingOptionId() {
        assertTrue(evaluateTrigger("uuid-123", TriggerOperator.EQ, "uuid-123"))
    }

    @Test
    fun TDD_optionSingleTrigger_doesNotFireOnDifferentOptionId() {
        assertFalse(evaluateTrigger("uuid-123", TriggerOperator.EQ, "uuid-456"))
    }
}
