// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.usecase

import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.model.TriggerOperator
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

private const val DEFAULT_SCALE_MIN = 0
private const val DEFAULT_SCALE_MAX = 10

data class QuestionRequest(
    val text: String,
    val type: QuestionType,
    val options: List<String> = emptyList(),
    val triggerAnswerValue: String? = null,
    val triggerOperator: TriggerOperator? = null,
    val scaleMin: Int = DEFAULT_SCALE_MIN,
    val scaleMax: Int = DEFAULT_SCALE_MAX,
    /** YES/NO "One Yes Per Day" flag (ED-17); ignored for other types. Default off (sum). */
    val collapsePerDay: Boolean = false
)

data class ScheduleRequest(
    val type: ScheduleType,
    val timeOfDay: LocalTime,
    val activeDaysOfWeek: Set<DayOfWeek>? = null,
    val dayOfMonth: Int? = null,
    val activeHours: Set<Int>? = null
)

data class CreateNudgeRequest(
    val mainQuestion: QuestionRequest,
    val followUpQuestions: List<QuestionRequest> = emptyList(),
    val schedule: ScheduleRequest,
    val name: String? = null,
    val isEnabled: Boolean = true
)

sealed class CreateNudgeResult {
    data class Success(val nudgeId: String) : CreateNudgeResult()
    sealed class Failure : CreateNudgeResult() {
        /** A free-form (TEXT or EMOJI) main question may not have follow-ups (no trigger conditions exist). */
        data object FreeformMainCannotHaveFollowUps : Failure()
        data class TooManyOptions(val questionText: String) : Failure()
        data object InvalidScaleRange : Failure()
    }
}

data class UpdateOptionRequest(val optionId: String, val newText: String)

data class FollowUpReplacement(
    val questionId: String?,  // null = new follow-up to insert; non-null = update existing
    val request: QuestionRequest
)

data class UpdateNudgeRequest(
    val nudgeId: String,
    val name: String? = null,
    val isEnabled: Boolean? = null,
    val mainQuestionText: String? = null,
    /** New "One Yes Per Day" value for a YES/NO main question (ED-17); null = no change. */
    val mainQuestionCollapsePerDay: Boolean? = null,
    val optionUpdates: List<UpdateOptionRequest> = emptyList(),
    val optionReorder: List<String>? = null,  // existing option IDs in desired order; null = no change
    val newOptions: List<String> = emptyList(),
    val removedOptionIds: Set<String> = emptySet(),
    val schedule: ScheduleRequest? = null,
    val splitEdit: Boolean = false,
    val followUpReplacements: List<FollowUpReplacement>? = null
)

sealed class UpdateNudgeResult {
    data class Success(val nudgeId: String) : UpdateNudgeResult()
    data object NudgeNotFound : UpdateNudgeResult()
}

// --- Storage normalization (ENGINEERING_DECISIONS.md ED-16) ---
// Every user-typed text field is trimmed at the save boundary so untrimmed text can never reach
// storage. Soft keyboards routinely append a trailing space after autocomplete or a tapped
// suggestion; left in place it would, for an emoji-only string, defeat the emoji-only detection that
// drives emoji scaling (ED-14), and elsewhere produce stray-whitespace data we'd have to special-case
// at display time. Each use case normalizes its request once, up front, so the same comparisons it
// makes against already-stored (already-trimmed) text see the trimmed form too.

/** Trims a required user-typed text field to its stored form. */
internal fun String.normalizedForStorage(): String = trim()

/** Trims an optional user-typed field; a value that is blank once trimmed becomes `null` (absent). */
internal fun String?.normalizedOptionalForStorage(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

internal fun QuestionRequest.normalized(): QuestionRequest = copy(
    text = text.normalizedForStorage(),
    options = options.map { it.normalizedForStorage() },
    triggerAnswerValue = triggerAnswerValue.normalizedOptionalForStorage()
)

internal fun CreateNudgeRequest.normalized(): CreateNudgeRequest = copy(
    name = name.normalizedOptionalForStorage(),
    mainQuestion = mainQuestion.normalized(),
    followUpQuestions = followUpQuestions.map { it.normalized() }
)

internal fun UpdateNudgeRequest.normalized(): UpdateNudgeRequest = copy(
    name = name.normalizedOptionalForStorage(),
    mainQuestionText = mainQuestionText.normalizedOptionalForStorage(),
    optionUpdates = optionUpdates.map { it.copy(newText = it.newText.normalizedForStorage()) },
    newOptions = newOptions.map { it.normalizedForStorage() },
    followUpReplacements = followUpReplacements?.map { it.copy(request = it.request.normalized()) }
)
