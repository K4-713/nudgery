package com.nudgery.android.backup

import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.model.TriggerOperator
import com.nudgery.shared.usecase.ImportAnswerRequest
import com.nudgery.shared.usecase.ImportNudgeRequest
import com.nudgery.shared.usecase.ImportQuestionRequest
import com.nudgery.shared.usecase.ScheduleRequest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import org.json.JSONArray
import org.json.JSONObject

class NudgeBackupParser {

    sealed class ParseResult {
        data class Success(val request: ImportNudgeRequest) : ParseResult()
        data class Failure(val message: String) : ParseResult()
    }

    fun parse(json: String): ParseResult {
        return try {
            val root = JSONObject(json)
            val nudgeObj = root.getJSONObject("nudge")
            val name = nudgeObj.getString("name")
            val isEnabled = nudgeObj.optBoolean("isEnabled", true)

            val schedule = parseSchedule(root)
            val questions = parseQuestions(root.getJSONArray("questions"))
            val answers = parseAnswers(root.optJSONArray("answers") ?: JSONArray())

            ParseResult.Success(
                ImportNudgeRequest(
                    name = name,
                    isEnabled = isEnabled,
                    schedule = schedule,
                    questions = questions,
                    answers = answers
                )
            )
        } catch (e: Exception) {
            ParseResult.Failure("Could not read backup file: ${e.message}")
        }
    }

    private fun parseSchedule(root: JSONObject): ScheduleRequest? {
        if (root.isNull("schedule")) return null
        val obj = root.optJSONObject("schedule") ?: return null

        val type = ScheduleType.valueOf(obj.getString("type"))

        val timeParts = obj.getString("timeOfDay").split(":")
        val timeOfDay = LocalTime(timeParts[0].toInt(), timeParts[1].toInt())

        val activeDaysOfWeek = obj.optJSONArray("activeDaysOfWeek")
            ?.let { arr -> (0 until arr.length()).map { DayOfWeek.valueOf(arr.getString(it)) }.toSet() }

        val dayOfMonth = if (obj.isNull("dayOfMonth")) null else obj.optInt("dayOfMonth").takeIf { it != 0 }

        val activeHours = obj.optJSONArray("activeHours")
            ?.let { arr -> (0 until arr.length()).map { arr.getInt(it) }.toSet() }

        return ScheduleRequest(
            type = type,
            timeOfDay = timeOfDay,
            activeDaysOfWeek = activeDaysOfWeek,
            dayOfMonth = dayOfMonth,
            activeHours = activeHours
        )
    }

    private fun parseQuestions(arr: JSONArray): List<ImportQuestionRequest> {
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val type = QuestionType.valueOf(obj.getString("type"))

            val options = obj.optJSONArray("options")
                ?.let { opts -> (0 until opts.length()).map { opts.getString(it) } }
                ?: emptyList()

            val triggerOperator = obj.optString("triggerOperator", "").takeIf { it.isNotEmpty() }
                ?.let { TriggerOperator.valueOf(it) }
            val triggerAnswerValue = obj.optString("triggerAnswerValue", "").ifEmpty { null }

            ImportQuestionRequest(
                orderIndex = obj.getInt("orderIndex"),
                text = obj.getString("text"),
                type = type,
                scaleMin = obj.optInt("scaleMin", 0),
                scaleMax = obj.optInt("scaleMax", 10),
                triggerOperator = triggerOperator,
                triggerAnswerValue = triggerAnswerValue,
                options = options,
                collapsePerDay = obj.optBoolean("collapsePerDay", false)
            )
        }
    }

    private fun parseAnswers(arr: JSONArray): List<ImportAnswerRequest> {
        return (0 until arr.length()).mapNotNull { i ->
            val obj = arr.getJSONObject(i)
            runCatching {
                ImportAnswerRequest(
                    questionOrderIndex = obj.getInt("questionOrderIndex"),
                    value = obj.getString("value"),
                    scheduledAt = Instant.parse(obj.getString("scheduledAt")),
                    answeredAt = Instant.parse(obj.getString("answeredAt"))
                )
            }.getOrNull()
        }
    }
}
