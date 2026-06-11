// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.viewmodel

import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.TriggerOperator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ENGINEERING_DECISIONS.md ED-22: the nudge name and question text are required, validated trimmed;
 * a follow-up the user edited but left text-less blocks submission, while an untouched stub does not.
 */
class FormValidationTest {

    @Test
    fun TDD_blankOrWhitespaceTextIsNotProvided() {
        assertFalse("empty is blank", isRequiredTextProvided(""))
        assertFalse("whitespace-only is blank (trimmed)", isRequiredTextProvided("   "))
        assertFalse("tabs/newlines are blank (trimmed)", isRequiredTextProvided("\t\n "))
        assertTrue("real content is provided", isRequiredTextProvided("Did you sleep?"))
        assertTrue("content with surrounding space still counts", isRequiredTextProvided("  hi  "))
    }

    @Test
    fun TDD_questionSectionRequiresBothNameAndText() {
        assertTrue(yesNoSection(nudgeName = "Sleep", questionText = "Did you sleep?"))
        assertFalse("blank name invalidates", yesNoSection(nudgeName = "  ", questionText = "Did you sleep?"))
        assertFalse("blank question invalidates", yesNoSection(nudgeName = "Sleep", questionText = ""))
    }

    @Test
    fun TDD_optionsRequireTwoNonBlankEntries() {
        // ED-23: at least MIN_OPTIONS_PER_QUESTION (2) options, none blank (trimmed).
        assertTrue(areOptionsValid(listOf("Bus", "Bike")))
        assertFalse("fewer than two is invalid", areOptionsValid(listOf("Bus")))
        assertFalse("empty is invalid", areOptionsValid(emptyList()))
        assertFalse("a blank option is invalid", areOptionsValid(listOf("Bus", "")))
        assertFalse("a whitespace-only option is invalid", areOptionsValid(listOf("Bus", "   ")))
    }

    @Test
    fun TDD_optionQuestionSectionRequiresValidOptions() {
        // An option-type main question blocks the section until its options are valid.
        assertFalse(
            "one option blocks",
            isQuestionSectionValid("Commute", QuestionFormState(text = "How?", type = QuestionType.OPTION_SINGLE, options = listOf("Bus")))
        )
        assertTrue(
            "two non-blank options pass",
            isQuestionSectionValid("Commute", QuestionFormState(text = "How?", type = QuestionType.OPTION_SINGLE, options = listOf("Bus", "Bike")))
        )
    }

    @Test
    fun TDD_scaleRangeMustAscend() {
        // ED-25: a scale needs min < max.
        assertTrue(isScaleRangeValid(0, 10))
        assertFalse("equal endpoints", isScaleRangeValid(5, 5))
        assertFalse("inverted", isScaleRangeValid(10, 5))
    }

    @Test
    fun TDD_scaleQuestionSectionRequiresAscendingRange() {
        val scale = QuestionFormState(text = "How tired?", type = QuestionType.SCALE)
        assertTrue("default 0..10 is valid", isQuestionSectionValid("Tired", scale))
        assertFalse(
            "inverted range blocks",
            isQuestionSectionValid("Tired", scale.copy(scaleMin = 10, scaleMax = 1))
        )
    }

    @Test
    fun TDD_optionFollowUpWithoutEnoughOptionsBlocks() {
        // An edited option follow-up with too few options blocks; with two it passes. (Both carry a
        // valid Yes/No trigger so options are the only difference.)
        val tooFew = QuestionFormState(text = "Which?", type = QuestionType.OPTION_SINGLE, options = listOf("A"), triggerAnswerValue = "YES")
        val enough = QuestionFormState(text = "Which?", type = QuestionType.OPTION_SINGLE, options = listOf("A", "B"), triggerAnswerValue = "YES")
        assertFalse(areFollowUpsValid(QuestionType.YES_NO, listOf(tooFew)))
        assertTrue(areFollowUpsValid(QuestionType.YES_NO, listOf(enough)))
    }

    /** A non-option question section (no options needed), for the name/text assertions. */
    private fun yesNoSection(nudgeName: String, questionText: String): Boolean =
        isQuestionSectionValid(nudgeName, QuestionFormState(text = questionText, type = QuestionType.YES_NO))

    @Test
    fun TDD_untouchedFollowUpStubDoesNotBlock() {
        // An untouched stub (ED-21 discards it) must not disable the forward action.
        assertTrue(areFollowUpsValid(QuestionType.YES_NO, listOf(QuestionFormState())))
    }

    @Test
    fun TDD_editedFollowUpWithoutTextBlocks() {
        // Configured (trigger set) but text left blank: this must block submission.
        val configuredButTextless = QuestionFormState(triggerAnswerValue = "YES")
        assertFalse(areFollowUpsValid(QuestionType.YES_NO, listOf(configuredButTextless)))
    }

    @Test
    fun TDD_followUpWithoutTriggerBlocks() {
        // ED-24: a follow-up with text but no trigger condition can't fire — it must block.
        val noTrigger = QuestionFormState(text = "Why?", type = QuestionType.TEXT)
        assertFalse("Yes/No main: needs an answer selected", areFollowUpsValid(QuestionType.YES_NO, listOf(noTrigger)))
        assertTrue(
            "with a Yes/No answer chosen it passes",
            areFollowUpsValid(QuestionType.YES_NO, listOf(noTrigger.copy(triggerAnswerValue = "YES")))
        )
    }

    @Test
    fun TDD_numberFollowUpTriggerNeedsOperatorAndValue() {
        // ED-24: a Number/Scale main needs both a comparison operator and a numeric threshold.
        val base = QuestionFormState(text = "Why?", type = QuestionType.TEXT)
        assertFalse("operator only", areFollowUpsValid(QuestionType.NUMBER, listOf(base.copy(triggerOperator = TriggerOperator.GTE))))
        assertFalse("value only", areFollowUpsValid(QuestionType.NUMBER, listOf(base.copy(triggerAnswerValue = "7"))))
        assertFalse("non-numeric value", areFollowUpsValid(QuestionType.NUMBER, listOf(base.copy(triggerOperator = TriggerOperator.GTE, triggerAnswerValue = "-"))))
        assertTrue("operator + numeric value", areFollowUpsValid(QuestionType.NUMBER, listOf(base.copy(triggerOperator = TriggerOperator.GTE, triggerAnswerValue = "7"))))
    }
}
