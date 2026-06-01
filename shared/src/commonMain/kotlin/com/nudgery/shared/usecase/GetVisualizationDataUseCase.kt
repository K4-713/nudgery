package com.nudgery.shared.usecase

import com.nudgery.shared.model.Answer
import com.nudgery.shared.model.DailyCount
import com.nudgery.shared.model.DataPoint
import com.nudgery.shared.model.HeatMapGranularity
import com.nudgery.shared.model.NamedCount
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.Timeframe
import com.nudgery.shared.model.VisualizationData
import com.nudgery.shared.repository.AnswerRepository
import com.nudgery.shared.repository.QuestionOptionRepository
import com.nudgery.shared.repository.QuestionRepository
import com.nudgery.shared.util.STOP_WORDS
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until

class GetVisualizationDataUseCase(
    private val answerRepository: AnswerRepository,
    private val questionRepository: QuestionRepository,
    private val questionOptionRepository: QuestionOptionRepository
) {
    suspend fun execute(
        nudgeId: String,
        questionId: String,
        timeframe: Timeframe,
        now: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): List<VisualizationData> {
        val question = questionRepository.getByNudgeId(nudgeId).firstOrNull { it.id == questionId }
            ?: return emptyList()

        val today = now.toLocalDateTime(timeZone).date
        val (windowStart, since) = computeWindow(timeframe, today, timeZone)
        val answers = answerRepository.getVisibleByNudgeIdSince(nudgeId, since)
            .filter { it.questionId == questionId }

        val effectiveWindowStart = if (timeframe == Timeframe.ALL_TIME) {
            answers.minOfOrNull { it.scheduledAt.toLocalDateTime(timeZone).date } ?: today
        } else {
            windowStart
        }

        val granularity = computeGranularity(timeframe, effectiveWindowStart, today)

        return when (question.type) {
            QuestionType.YES_NO -> buildYesNoCharts(answers, timeZone, effectiveWindowStart, today, granularity)
            QuestionType.SCALE, QuestionType.NUMBER -> buildNumberCharts(answers, timeZone, effectiveWindowStart, today, granularity)
            QuestionType.OPTION_SINGLE -> buildOptionCharts(answers, questionId, includeColumnChart = true)
            QuestionType.OPTION_MULTI -> buildOptionCharts(answers, questionId, includeColumnChart = false)
            QuestionType.TEXT -> buildTextCharts(answers)
        }
    }

    private fun computeGranularity(
        timeframe: Timeframe,
        windowStart: LocalDate,
        windowEnd: LocalDate
    ): HeatMapGranularity = when (timeframe) {
        Timeframe.WEEKLY, Timeframe.MONTHLY -> HeatMapGranularity.DAY
        Timeframe.YEARLY -> HeatMapGranularity.WEEK
        Timeframe.ALL_TIME -> {
            val days = windowStart.until(windowEnd, DateTimeUnit.DAY)
            when {
                days < 90 -> HeatMapGranularity.DAY
                days < 730 -> HeatMapGranularity.WEEK
                else -> HeatMapGranularity.MONTH
            }
        }
    }

    private fun computeWindow(
        timeframe: Timeframe,
        today: LocalDate,
        timeZone: TimeZone
    ): Pair<LocalDate, Instant> = when (timeframe) {
        Timeframe.WEEKLY -> {
            val start = today.minus(7, DateTimeUnit.DAY)
            start to start.atStartOfDayIn(timeZone)
        }
        Timeframe.MONTHLY -> {
            val start = LocalDate(today.year, today.month, 1)
            start to start.atStartOfDayIn(timeZone)
        }
        Timeframe.YEARLY -> {
            val start = LocalDate(today.year, Month.JANUARY, 1)
            start to start.atStartOfDayIn(timeZone)
        }
        Timeframe.ALL_TIME -> today to Instant.DISTANT_PAST
    }

    private fun buildYesNoCharts(
        answers: List<Answer>,
        timeZone: TimeZone,
        windowStart: LocalDate,
        windowEnd: LocalDate,
        granularity: HeatMapGranularity
    ): List<VisualizationData> {
        val dailyCounts = answers
            .groupBy { it.scheduledAt.toLocalDateTime(timeZone).date }
            .map { (date, dayAnswers) ->
                DailyCount(date, dayAnswers.count { it.value.uppercase() == "YES" }.toDouble())
            }
            .sortedBy { it.date }

        val dailyYesPoints = dailyCounts.map { DataPoint(it.date.atStartOfDayIn(timeZone), it.value) }

        val totalYes = answers.count { it.value.uppercase() == "YES" }
        val totalNo = answers.count { it.value.uppercase() == "NO" }

        return listOf(
            VisualizationData.CalendarHeatMap(dailyCounts, windowStart, windowEnd, granularity),
            VisualizationData.LineGraph(dailyYesPoints, windowStart, windowEnd),
            VisualizationData.ColumnChart(listOf(NamedCount("YES", totalYes), NamedCount("NO", totalNo)))
        )
    }

    private fun buildNumberCharts(
        answers: List<Answer>,
        timeZone: TimeZone,
        windowStart: LocalDate,
        windowEnd: LocalDate,
        granularity: HeatMapGranularity
    ): List<VisualizationData> {
        val points = answers
            .mapNotNull { answer ->
                answer.value.toDoubleOrNull()?.let { DataPoint(answer.scheduledAt, it) }
            }
            .sortedBy { it.at }

        val dailyCounts = answers
            .groupBy { it.scheduledAt.toLocalDateTime(timeZone).date }
            .map { (date, dayAnswers) ->
                val avg = dayAnswers.mapNotNull { it.value.toDoubleOrNull() }.average()
                DailyCount(date, if (avg.isNaN()) 0.0 else avg)
            }
            .sortedBy { it.date }

        return listOf(
            VisualizationData.LineGraph(points, windowStart, windowEnd),
            VisualizationData.CalendarHeatMap(dailyCounts, windowStart, windowEnd, granularity)
        )
    }

    private fun buildTextCharts(answers: List<Answer>): List<VisualizationData> {
        val wordCounts = answers
            .flatMap { tokenizeText(it.value) }
            .groupingBy { it }
            .eachCount()
            .map { (word, count) -> NamedCount(word, count) }
            .sortedByDescending { it.count }
        return if (wordCounts.isEmpty()) emptyList()
        else listOf(VisualizationData.TagCloud(wordCounts))
    }

    private fun tokenizeText(text: String): List<String> =
        text.split(Regex("\\s+"))
            .map { token -> token.lowercase().trim { !it.isLetter() } }
            .filter { word -> word.length >= 3 && word !in STOP_WORDS }

    private suspend fun buildOptionCharts(
        answers: List<Answer>,
        questionId: String,
        includeColumnChart: Boolean
    ): List<VisualizationData> {
        val optionsById = questionOptionRepository.getByQuestionId(questionId).associateBy { it.id }

        val optionCounts = answers
            .flatMap { answer -> answer.value.split(",").map { it.trim() } }
            .filter { it.isNotBlank() }
            .groupBy { id -> optionsById[id]?.text ?: id }
            .map { (label, occurrences) -> NamedCount(label, occurrences.size) }
            .sortedByDescending { it.count }

        return buildList {
            add(VisualizationData.BarChart(optionCounts))
            if (includeColumnChart) add(VisualizationData.ColumnChart(optionCounts))
            add(VisualizationData.TagCloud(optionCounts))
        }
    }
}
