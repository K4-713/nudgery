// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.viewmodel

/**
 * Pure validation rules for the create/edit nudge forms (ED-22). Kept free of Compose/Android so
 * they can be unit-tested directly and shared by both the create wizard and the edit screens.
 */

/**
 * A required free-text field is satisfied only by non-whitespace content. Checked **trimmed**, in
 * keeping with ED-16 (text is trimmed at the save boundary), so a whitespace-only entry is blank.
 */
fun isRequiredTextProvided(text: String): Boolean = text.trim().isNotEmpty()

/**
 * The "Question" inputs (create wizard step 1 and the edit question screen) are valid only when both
 * the nudge name and the main question text carry real content.
 */
fun isQuestionSectionValid(nudgeName: String, questionText: String): Boolean =
    isRequiredTextProvided(nudgeName) && isRequiredTextProvided(questionText)

/**
 * Follow-ups are valid when every one is either an untouched stub — equal to a pristine
 * [QuestionFormState], which ED-21 discards on navigation/submit so it must not block — or carries
 * real question text. A follow-up the user configured (e.g. set a trigger) but left without text
 * blocks submission.
 */
fun areFollowUpsValid(followUps: List<QuestionFormState>): Boolean =
    followUps.all { it == QuestionFormState() || isRequiredTextProvided(it.text) }
