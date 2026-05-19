package com.nudgery.shared.usecase

import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.model.TriggerOperator
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

data class QuestionRequest(
    val text: String,
    val type: QuestionType,
    val options: List<String> = emptyList(),
    val triggerAnswerValue: String? = null,
    val triggerOperator: TriggerOperator? = null
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
        data object MainQuestionCannotBeText : Failure()
        data class TooManyOptions(val questionText: String) : Failure()
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
    val optionUpdates: List<UpdateOptionRequest> = emptyList(),
    val schedule: ScheduleRequest? = null,
    val splitEdit: Boolean = false,
    val followUpReplacements: List<FollowUpReplacement>? = null
)

sealed class UpdateNudgeResult {
    data class Success(val nudgeId: String) : UpdateNudgeResult()
    data object NudgeNotFound : UpdateNudgeResult()
}
