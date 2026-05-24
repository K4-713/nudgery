package com.nudgery.shared.usecase

import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.TriggerOperator
import kotlinx.datetime.Instant

data class ImportNudgeRequest(
    val name: String,
    val isEnabled: Boolean,
    val schedule: ScheduleRequest?,
    val questions: List<ImportQuestionRequest>,
    val answers: List<ImportAnswerRequest>
)

data class ImportQuestionRequest(
    val orderIndex: Int,
    val text: String,
    val type: QuestionType,
    val scaleMin: Int = 0,
    val scaleMax: Int = 10,
    val triggerOperator: TriggerOperator? = null,
    val triggerAnswerValue: String? = null,
    val options: List<String> = emptyList()
)

data class ImportAnswerRequest(
    val questionOrderIndex: Int,
    val value: String,
    val scheduledAt: Instant,
    val answeredAt: Instant
)
