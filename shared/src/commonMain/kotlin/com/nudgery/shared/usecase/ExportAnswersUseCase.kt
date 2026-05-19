package com.nudgery.shared.usecase

import com.nudgery.shared.model.ExportFormat
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.repository.AnswerRepository
import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.repository.QuestionOptionRepository
import com.nudgery.shared.repository.QuestionRepository

private val EXPORT_HEADERS = listOf("nudge_name", "question_text", "answer_value", "option_text", "scheduled_at", "answered_at")

class ExportAnswersUseCase(
    private val nudgeRepository: NudgeRepository,
    private val questionRepository: QuestionRepository,
    private val questionOptionRepository: QuestionOptionRepository,
    private val answerRepository: AnswerRepository
) {
    suspend fun execute(nudgeId: String, format: ExportFormat): String {
        val nudge = nudgeRepository.getById(nudgeId) ?: return ""
        val questions = questionRepository.getByNudgeId(nudgeId).associateBy { it.id }

        val optionsByQuestionId = questions.values
            .filter { it.type.isOptionType }
            .associate { q -> q.id to questionOptionRepository.getByQuestionId(q.id).associateBy { it.id } }

        val answers = answerRepository.getAllByNudgeId(nudgeId)

        val delimiter = when (format) {
            ExportFormat.CSV -> ","
            ExportFormat.TSV -> "\t"
        }

        val sb = StringBuilder()
        sb.appendLine(EXPORT_HEADERS.joinToString(delimiter) { escape(it, format) })

        answers.forEach { answer ->
            val question = questions[answer.questionId]
            val questionText = question?.text ?: ""

            val optionText = if (question?.type?.isOptionType == true) {
                val optionsById = optionsByQuestionId[answer.questionId] ?: emptyMap()
                answer.value.split(",")
                    .map { it.trim() }
                    .mapNotNull { id -> optionsById[id]?.text }
                    .joinToString("; ")
            } else {
                ""
            }

            val row = listOf(nudge.name, questionText, answer.value, optionText, answer.scheduledAt.toString(), answer.answeredAt.toString())
            sb.appendLine(row.joinToString(delimiter) { escape(it, format) })
        }

        return sb.toString()
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
