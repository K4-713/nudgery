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
    val options: List<String> = emptyList(),
    /** YES/NO "One Yes Per Day" flag (ED-17); absent in older backups ⇒ false. */
    val collapsePerDay: Boolean = false
)

data class ImportAnswerRequest(
    val questionOrderIndex: Int,
    val value: String,
    val scheduledAt: Instant,
    val answeredAt: Instant
)

// Storage normalization (ENGINEERING_DECISIONS.md ED-16): import is a save boundary too, so a
// restored backup is trimmed on the way in. Trimming uniformly keeps option-text-keyed trigger and
// answer resolution internally consistent (all keys and values are trimmed together).
internal fun ImportQuestionRequest.normalized(): ImportQuestionRequest = copy(
    text = text.normalizedForStorage(),
    options = options.map { it.normalizedForStorage() },
    triggerAnswerValue = triggerAnswerValue.normalizedOptionalForStorage()
)

internal fun ImportNudgeRequest.normalized(): ImportNudgeRequest = copy(
    name = name.normalizedForStorage(),
    questions = questions.map { it.normalized() },
    answers = answers.map { it.copy(value = it.value.normalizedForStorage()) }
)
