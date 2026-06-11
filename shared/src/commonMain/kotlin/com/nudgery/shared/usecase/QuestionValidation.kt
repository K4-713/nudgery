// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.usecase

import com.nudgery.shared.model.QuestionType

/** Bounds on the number of options an option-type question may have (ED-26). */
const val MIN_OPTIONS_PER_QUESTION = 2
const val MAX_OPTIONS_PER_QUESTION = 16

/**
 * A data-boundary validation problem for a nudge's questions (ED-26). This is the use-case-side
 * counterpart of the UI's `FormValidation`: the create/edit forms already prevent these (ED-22..25),
 * but the use cases validate too so a non-form path — notably backup import — can't write a
 * malformed nudge to storage. Each problem carries the offending question's text for messaging/logs.
 */
sealed class QuestionValidationProblem {
    abstract val questionText: String

    /** An option-type question has more than [MAX_OPTIONS_PER_QUESTION] options. */
    data class TooManyOptions(override val questionText: String) : QuestionValidationProblem()

    /** An option-type question has fewer than [MIN_OPTIONS_PER_QUESTION] options. */
    data class NotEnoughOptions(override val questionText: String) : QuestionValidationProblem()

    /** An option-type question has a blank (whitespace-only) option. */
    data class BlankOption(override val questionText: String) : QuestionValidationProblem()

    /** A scale question's range is not ascending (`scaleMin >= scaleMax`). */
    data class InvalidScaleRange(override val questionText: String) : QuestionValidationProblem()

    /** A follow-up has no valid trigger condition for the main question's type. */
    data class MissingFollowUpTrigger(override val questionText: String) : QuestionValidationProblem()
}

/** Validates a single question's own configuration (option count/blankness, scale range). */
fun QuestionRequest.configProblem(): QuestionValidationProblem? = when {
    type.isOptionType && options.size > MAX_OPTIONS_PER_QUESTION ->
        QuestionValidationProblem.TooManyOptions(text)
    type.isOptionType && options.size < MIN_OPTIONS_PER_QUESTION ->
        QuestionValidationProblem.NotEnoughOptions(text)
    type.isOptionType && options.any { it.isBlank() } ->
        QuestionValidationProblem.BlankOption(text)
    type == QuestionType.SCALE && scaleMin >= scaleMax ->
        QuestionValidationProblem.InvalidScaleRange(text)
    else -> null
}

/**
 * Validates a follow-up's trigger condition against the main question's type (what the user
 * answers). Mirrors the UI's `isFollowUpTriggerValid`: Yes/No and option mains need a chosen answer;
 * Number/Scale mains need both an operator and a numeric threshold; free-form mains can't have
 * follow-ups, so nothing is required.
 */
fun triggerProblem(mainType: QuestionType, followUp: QuestionRequest): QuestionValidationProblem? {
    val hasTrigger = when (mainType) {
        QuestionType.YES_NO,
        QuestionType.OPTION_SINGLE,
        QuestionType.OPTION_MULTI -> followUp.triggerAnswerValue != null
        QuestionType.NUMBER,
        QuestionType.SCALE ->
            followUp.triggerOperator != null && followUp.triggerAnswerValue?.toDoubleOrNull() != null
        QuestionType.TEXT, QuestionType.EMOJI -> true
    }
    return if (hasTrigger) null else QuestionValidationProblem.MissingFollowUpTrigger(followUp.text)
}

/**
 * Validates a full main + follow-ups set, returning the first problem found (or null if all valid):
 * the main question's own configuration, then each follow-up's configuration and its trigger
 * against the main type.
 */
fun validateNudgeQuestions(
    main: QuestionRequest,
    followUps: List<QuestionRequest>
): QuestionValidationProblem? {
    main.configProblem()?.let { return it }
    for (followUp in followUps) {
        followUp.configProblem()?.let { return it }
        triggerProblem(main.type, followUp)?.let { return it }
    }
    return null
}

/** Adapts an imported question to the common [QuestionRequest] shape for validation. */
private fun ImportQuestionRequest.toQuestionRequest(): QuestionRequest = QuestionRequest(
    text = text,
    type = type,
    options = options,
    triggerAnswerValue = triggerAnswerValue,
    triggerOperator = triggerOperator,
    scaleMin = scaleMin,
    scaleMax = scaleMax,
    collapsePerDay = collapsePerDay
)

/**
 * Validates an import request's questions (the main question at orderIndex 0 plus each follow-up),
 * returning the first problem or null. Unlike create/update, the import flow treats this as
 * **advisory**: it may import anyway and route the user to fix the problem (ED-26).
 */
fun ImportNudgeRequest.questionProblem(): QuestionValidationProblem? {
    val sorted = questions.sortedBy { it.orderIndex }
    val main = sorted.firstOrNull() ?: return null
    return validateNudgeQuestions(main.toQuestionRequest(), sorted.drop(1).map { it.toQuestionRequest() })
}

/**
 * The problem (if any) with just the **main** imported question's own configuration — used to route
 * a fix to the right editor step (the question step vs. the follow-ups step).
 */
fun ImportNudgeRequest.mainConfigProblem(): QuestionValidationProblem? =
    questions.sortedBy { it.orderIndex }.firstOrNull()?.toQuestionRequest()?.configProblem()
