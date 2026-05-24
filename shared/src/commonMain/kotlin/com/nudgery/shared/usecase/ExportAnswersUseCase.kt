package com.nudgery.shared.usecase

import com.nudgery.shared.model.ExportFormat
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.repository.AnswerRepository
import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.repository.QuestionOptionRepository
import com.nudgery.shared.repository.QuestionRepository
import com.nudgery.shared.repository.ScheduleRepository

private val EXPORT_HEADERS = listOf("nudge_name", "question_text", "answer_value", "option_text", "scheduled_at", "answered_at")

class ExportAnswersUseCase(
    private val nudgeRepository: NudgeRepository,
    private val questionRepository: QuestionRepository,
    private val questionOptionRepository: QuestionOptionRepository,
    private val answerRepository: AnswerRepository,
    private val scheduleRepository: ScheduleRepository
) {
    suspend fun execute(nudgeId: String, format: ExportFormat): String {
        val nudge = nudgeRepository.getById(nudgeId) ?: return ""
        val questions = questionRepository.getByNudgeId(nudgeId).sortedBy { it.orderIndex }

        val optionsByQuestionId = questions
            .filter { it.type.isOptionType }
            .associate { q -> q.id to questionOptionRepository.getByQuestionId(q.id).sortedBy { it.orderIndex } }

        val answers = answerRepository.getAllByNudgeId(nudgeId)

        return when (format) {
            ExportFormat.JSON -> {
                val schedule = scheduleRepository.getByNudgeId(nudgeId)
                buildJsonExport(nudge, schedule, questions, optionsByQuestionId, answers)
            }
            else -> {
                val optionsById = optionsByQuestionId.mapValues { (_, opts) -> opts.associateBy { it.id } }
                val questionsById = questions.associateBy { it.id }
                buildDelimitedExport(nudge.name, questionsById, optionsById, answers, format)
            }
        }
    }

    private fun buildDelimitedExport(
        nudgeName: String,
        questionsById: Map<String, com.nudgery.shared.model.Question>,
        optionsById: Map<String, Map<String, com.nudgery.shared.model.QuestionOption>>,
        answers: List<com.nudgery.shared.model.Answer>,
        format: ExportFormat
    ): String {
        val delimiter = if (format == ExportFormat.CSV) "," else "\t"
        val sb = StringBuilder()
        sb.appendLine(EXPORT_HEADERS.joinToString(delimiter) { escape(it, format) })

        answers.forEach { answer ->
            val question = questionsById[answer.questionId]
            val questionText = question?.text ?: ""

            val optionText = if (question?.type?.isOptionType == true) {
                val opts = optionsById[answer.questionId] ?: emptyMap()
                answer.value.split(",")
                    .map { it.trim() }
                    .mapNotNull { id -> opts[id]?.text }
                    .joinToString("; ")
            } else {
                ""
            }

            val row = listOf(nudgeName, questionText, answer.value, optionText, answer.scheduledAt.toString(), answer.answeredAt.toString())
            sb.appendLine(row.joinToString(delimiter) { escape(it, format) })
        }

        return sb.toString()
    }

    private fun buildJsonExport(
        nudge: com.nudgery.shared.model.Nudge,
        schedule: com.nudgery.shared.model.Schedule?,
        questions: List<com.nudgery.shared.model.Question>,
        optionsByQuestionId: Map<String, List<com.nudgery.shared.model.QuestionOption>>,
        answers: List<com.nudgery.shared.model.Answer>
    ): String {
        val sb = StringBuilder()
        sb.append("{\n")

        // nudge
        sb.append("  \"nudge\": {\n")
        sb.append("    \"name\": ${jsonString(nudge.name)},\n")
        sb.append("    \"isEnabled\": ${nudge.isEnabled}\n")
        sb.append("  },\n")

        // schedule
        if (schedule != null) {
            sb.append("  \"schedule\": {\n")
            sb.append("    \"type\": \"${schedule.type.name}\",\n")
            sb.append("    \"timeOfDay\": \"${schedule.timeOfDay.hour.toString().padStart(2, '0')}:${schedule.timeOfDay.minute.toString().padStart(2, '0')}\",\n")
            val days = schedule.activeDaysOfWeek
                ?.sortedBy { it.ordinal }
                ?.joinToString(", ") { "\"${it.name}\"" } ?: ""
            sb.append("    \"activeDaysOfWeek\": [$days],\n")
            sb.append("    \"dayOfMonth\": ${schedule.dayOfMonth ?: "null"},\n")
            val hours = schedule.activeHours
                ?.sorted()
                ?.joinToString(", ") ?: ""
            sb.append("    \"activeHours\": ${if (schedule.activeHours != null) "[$hours]" else "null"}\n")
            sb.append("  },\n")
        } else {
            sb.append("  \"schedule\": null,\n")
        }

        // Build lookup maps first so they are available when serializing both questions and answers
        val questionIdToOrderIndex = questions.associate { it.id to it.orderIndex }
        val optionIdToText = optionsByQuestionId.values.flatten().associate { it.id to it.text }

        // questions
        sb.append("  \"questions\": [\n")
        questions.forEachIndexed { index, question ->
            sb.append("    {\n")
            sb.append("      \"orderIndex\": ${question.orderIndex},\n")
            sb.append("      \"text\": ${jsonString(question.text)},\n")
            sb.append("      \"type\": \"${question.type.name}\"")
            if (question.type == QuestionType.SCALE) {
                sb.append(",\n      \"scaleMin\": ${question.scaleMin ?: 0}")
                sb.append(",\n      \"scaleMax\": ${question.scaleMax ?: 10}")
            }
            if (question.triggerOperator != null) {
                // Resolve option UUID trigger values to option text for round-trip import
                val triggerText = optionIdToText[question.triggerAnswerValue] ?: question.triggerAnswerValue ?: ""
                sb.append(",\n      \"triggerOperator\": \"${question.triggerOperator.name}\"")
                sb.append(",\n      \"triggerAnswerValue\": ${jsonString(triggerText)}")
            }
            val options = optionsByQuestionId[question.id] ?: emptyList()
            if (options.isNotEmpty()) {
                val optArray = options.joinToString(", ") { jsonString(it.text) }
                sb.append(",\n      \"options\": [$optArray]")
            }
            sb.append("\n    }")
            if (index < questions.lastIndex) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ],\n")

        // answers — reference questions by orderIndex, resolve option UUIDs to text
        sb.append("  \"answers\": [\n")
        answers.forEachIndexed { index, answer ->
            val orderIndex = questionIdToOrderIndex[answer.questionId]
            val question = questions.find { it.id == answer.questionId }
            val resolvedValue = if (question?.type?.isOptionType == true) {
                answer.value.split(",")
                    .map { it.trim() }
                    .mapNotNull { optionIdToText[it] }
                    .joinToString(", ")
            } else {
                answer.value
            }
            sb.append("    {\n")
            sb.append("      \"questionOrderIndex\": ${orderIndex ?: 0},\n")
            sb.append("      \"value\": ${jsonString(resolvedValue)},\n")
            sb.append("      \"scheduledAt\": ${jsonString(answer.scheduledAt.toString())},\n")
            sb.append("      \"answeredAt\": ${jsonString(answer.answeredAt.toString())}\n")
            sb.append("    }")
            if (index < answers.lastIndex) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n")

        sb.append("}")
        return sb.toString()
    }

    private fun jsonString(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    private fun escape(value: String, format: ExportFormat): String {
        if (format == ExportFormat.TSV) return value
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
