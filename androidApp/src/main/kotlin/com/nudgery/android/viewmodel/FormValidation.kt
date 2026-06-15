// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.viewmodel

import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.TriggerOperator

/**
 * Pure validation rules for the create/edit nudge forms (ED-22, ED-23). Kept free of
 * Compose/Android so they can be unit-tested directly and shared by both the create wizard and the
 * edit screens.
 */

/**
 * An option-type question needs at least this many options to be meaningful — you cannot "choose
 * one" (or choose several) from fewer than two. (ED-23.)
 */
const val MIN_OPTIONS_PER_QUESTION = 2

/**
 * A required free-text field is satisfied only by non-whitespace content. Checked **trimmed**, in
 * keeping with ED-16 (text is trimmed at the save boundary), so a whitespace-only entry is blank.
 */
fun isRequiredTextProvided(text: String): Boolean = text.trim().isNotEmpty()

/**
 * Options are valid when there are at least [MIN_OPTIONS_PER_QUESTION] of them and none is blank
 * (trimmed). (ED-23.)
 */
fun areOptionsValid(options: List<String>): Boolean =
    options.size >= MIN_OPTIONS_PER_QUESTION && options.all { isRequiredTextProvided(it) }

/**
 * A single question's own configuration is valid when it has real text and, for option types, a
 * valid set of options, and, for a scale, an ascending range (ED-25).
 */
fun isQuestionConfigValid(question: QuestionFormState): Boolean =
    isRequiredTextProvided(question.text) &&
        (!question.type.isOptionType || areOptionsValid(question.options)) &&
        (question.type != QuestionType.SCALE ||
            (areScaleTextsValid(question.scaleMinText, question.scaleMaxText) &&
                isScaleRangeValid(question.scaleMin, question.scaleMax)))

/** Both scale text fields must parse as whole numbers before the range can be checked. */
fun areScaleTextsValid(minText: String, maxText: String): Boolean =
    minText.toIntOrNull() != null && maxText.toIntOrNull() != null

/** A scale's range is valid only when its minimum is strictly below its maximum (ED-25). Mirrors
 *  the use-case's existing `InvalidScaleRange` backstop. */
fun isScaleRangeValid(scaleMin: Int, scaleMax: Int): Boolean = scaleMin < scaleMax

/**
 * The "Question" inputs (create wizard step 1 and the edit question screen) are valid only when the
 * nudge name carries real content and the main question's own configuration is valid.
 */
fun isQuestionSectionValid(nudgeName: String, question: QuestionFormState): Boolean =
    isRequiredTextProvided(nudgeName) && isQuestionConfigValid(question)

/**
 * A follow-up's trigger condition — "show this follow-up when the answer is …" — must be specified;
 * it cannot be defaulted, since it's the whole meaning of the follow-up (ED-24). What's required
 * depends on the **main** question's type, which is what the user is answering:
 * - Any type: an [ALWAYS][TriggerOperator.ALWAYS] operator is always valid (no value needed).
 * - Yes/No or option main: a specific answer must be chosen (`triggerAnswerValue` set).
 * - Number/Scale main: both a comparison operator and a numeric threshold are required.
 * - Free-form main (text/emoji): only ALWAYS is accepted (no conditional triggers).
 */
fun isFollowUpTriggerValid(mainType: QuestionType, followUp: QuestionFormState): Boolean =
    if (followUp.triggerOperator == TriggerOperator.ALWAYS) true
    else when (mainType) {
        QuestionType.YES_NO,
        QuestionType.OPTION_SINGLE,
        QuestionType.OPTION_MULTI -> followUp.triggerAnswerValue != null
        QuestionType.NUMBER,
        QuestionType.SCALE ->
            followUp.triggerOperator != null && followUp.triggerAnswerValue?.toDoubleOrNull() != null
        QuestionType.TEXT, QuestionType.EMOJI -> false
    }

/** Pristine stub states that ED-21 discards — the legacy default (null trigger) and the current
 *  default (ALWAYS trigger, ED-28). */
private val UNTOUCHED_STUBS = setOf(
    QuestionFormState(),
    QuestionFormState(triggerOperator = TriggerOperator.ALWAYS)
)

/**
 * Follow-ups are valid when every one is either an untouched stub — a pristine [QuestionFormState]
 * which ED-21 discards on navigation/submit so it must not block — or has a valid configuration
 * (real text, valid options if it is an option type) **and** a specified trigger condition for the
 * given [mainType] (ED-24). A follow-up the user configured but left without text, without enough
 * options, or without a trigger blocks submission.
 */
fun areFollowUpsValid(mainType: QuestionType, followUps: List<QuestionFormState>): Boolean =
    followUps.all {
        it in UNTOUCHED_STUBS ||
            (isQuestionConfigValid(it) && isFollowUpTriggerValid(mainType, it))
    }
