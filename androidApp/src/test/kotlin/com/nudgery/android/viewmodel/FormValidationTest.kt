// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.viewmodel

import com.nudgery.shared.model.QuestionType
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
        assertTrue(isQuestionSectionValid(nudgeName = "Sleep", questionText = "Did you sleep?"))
        assertFalse("blank name invalidates", isQuestionSectionValid(nudgeName = "  ", questionText = "Did you sleep?"))
        assertFalse("blank question invalidates", isQuestionSectionValid(nudgeName = "Sleep", questionText = ""))
    }

    @Test
    fun TDD_untouchedFollowUpStubDoesNotBlock() {
        // An untouched stub (ED-21 discards it) must not disable the forward action.
        assertTrue(areFollowUpsValid(listOf(QuestionFormState())))
    }

    @Test
    fun TDD_editedFollowUpWithoutTextBlocks() {
        // Configured (trigger set) but text left blank: this must block submission.
        val configuredButTextless = QuestionFormState(triggerAnswerValue = "YES")
        assertFalse(areFollowUpsValid(listOf(configuredButTextless)))
    }

    @Test
    fun TDD_followUpWithTextIsValid() {
        assertTrue(areFollowUpsValid(listOf(QuestionFormState(text = "Why?", type = QuestionType.TEXT))))
    }
}
