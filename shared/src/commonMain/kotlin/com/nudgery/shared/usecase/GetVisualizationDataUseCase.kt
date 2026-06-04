package com.nudgery.shared.usecase

import com.nudgery.shared.model.Answer
import com.nudgery.shared.model.DailyCount
import com.nudgery.shared.model.DataPoint
import com.nudgery.shared.model.HeatMapGranularity
import com.nudgery.shared.model.NamedCount
import com.nudgery.shared.model.Question
import com.nudgery.shared.model.QuestionOption
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.Timeframe
import com.nudgery.shared.model.VisualizationData
import com.nudgery.shared.repository.AnswerRepository
import com.nudgery.shared.repository.QuestionOptionRepository
import com.nudgery.shared.repository.QuestionRepository
import com.nudgery.shared.util.STOP_WORDS
import com.nudgery.shared.util.extractEmojiWords
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until

/**
 * A preloaded, in-memory snapshot of one question's full answer history plus its options — enough
 * to build that question's charts for *any* time window without touching the database. The
 * nudge-detail dashboard loads this once (whenever the underlying answers change) and then
 * re-aggregates it as the user scrubs the shared window, so scrubbing never re-queries SQLite.
 */
data class QuestionVisualizationSource(
    val question: Question,
    /** The question's full visible answer history, unfiltered by window. */
    val answers: List<Answer>,
    /** Option id -> option, for option-type questions; empty for other types. */
    val optionsById: Map<String, QuestionOption>
)

class GetVisualizationDataUseCase(
    private val answerRepository: AnswerRepository,
    private val questionRepository: QuestionRepository,
    private val questionOptionRepository: QuestionOptionRepository
) {
    /**
     * Loads everything needed to chart [questionId] for any window. This is the only step that
     * touches the database, so callers that re-render many windows (e.g. a drag-scrub) should call
     * it once and reuse the result with [build].
     */
    suspend fun loadSource(nudgeId: String, questionId: String): QuestionVisualizationSource? {
        val question = questionRepository.getByNudgeId(nudgeId).firstOrNull { it.id == questionId }
            ?: return null
        val answers = answerRepository.getVisibleByNudgeIdSince(nudgeId, Instant.DISTANT_PAST)
            .filter { it.questionId == questionId }
        val optionsById = when (question.type) {
            QuestionType.OPTION_SINGLE, QuestionType.OPTION_MULTI ->
                questionOptionRepository.getByQuestionId(questionId).associateBy { it.id }
            else -> emptyMap()
        }
        return QuestionVisualizationSource(question, answers, optionsById)
    }

    /**
     * Loads the question's data from the database and builds its charts for the given window.
     * Equivalent to [loadSource] followed by [build]; retained for callers that render a single
     * window and don't benefit from caching the source.
     */
    suspend fun execute(
        nudgeId: String,
        questionId: String,
        timeframe: Timeframe,
        periodOffsetDays: Int = 0,
        now: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): List<VisualizationData> {
        val source = loadSource(nudgeId, questionId) ?: return emptyList()
        return build(source, timeframe, periodOffsetDays, now, timeZone)
    }

    /**
     * Builds a question's charts for one window from an already-loaded [source]. Pure and
     * database-free: it only filters and aggregates the in-memory snapshot, so it is safe to call
     * repeatedly (once per scrub step) without incurring storage I/O.
     */
    fun build(
        source: QuestionVisualizationSource,
        timeframe: Timeframe,
        periodOffsetDays: Int = 0,
        now: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): List<VisualizationData> {
        val question = source.question
        val today = now.toLocalDateTime(timeZone).date
        val allAnswers = source.answers
        val earliest = allAnswers.minOfOrNull { it.scheduledAt.toLocalDateTime(timeZone).date } ?: today

        // The whole dashboard is locked to one shared window; charts only see answers inside it.
        val (windowStart, windowEnd) = analysisWindow(timeframe, periodOffsetDays, today, earliest)
        val answers = allAnswers.filter {
            val date = it.scheduledAt.toLocalDateTime(timeZone).date
            date >= windowStart && date <= windowEnd
        }

        val granularity = computeGranularity(timeframe, windowStart, windowEnd)
        // The window already equals the timeframe's span (a month, a year, or all of history), so
        // there is no older data hiding within the chart — earlier periods are reached by shifting
        // the window. Fitting the whole window into the canvas (no internal scroll) lets the month
        // or year fill the chart. Weekly is the exception: its short day strip is meant to scroll.
        val fillViewport = timeframe != Timeframe.WEEKLY
        // The line graph fits exactly the window (no internal scroll); the dashboard moves the window.
        val lineVisibleDays = (windowStart.until(windowEnd, DateTimeUnit.DAY) + 1).toInt().coerceAtLeast(1)
        // A fixed Y range, computed over ALL answers, keeps the line graph's axis stable while the
        // window is scrubbed (so absolute maxima stay at a constant height).
        val lineYRange = lineGraphYRange(question, allAnswers, timeZone)

        return when (question.type) {
            QuestionType.YES_NO -> buildYesNoCharts(answers, timeZone, windowStart, windowEnd, earliest, granularity, fillViewport, lineVisibleDays, lineYRange)
            QuestionType.SCALE, QuestionType.NUMBER -> buildNumberCharts(answers, timeZone, windowStart, windowEnd, earliest, granularity, fillViewport, lineVisibleDays, lineYRange)
            QuestionType.OPTION_SINGLE -> buildOptionCharts(answers, source.optionsById, includeColumnChart = true)
            QuestionType.OPTION_MULTI -> buildOptionCharts(answers, source.optionsById, includeColumnChart = false)
            QuestionType.TEXT, QuestionType.EMOJI -> buildTextCharts(answers)
        }
    }

    private fun computeGranularity(
        timeframe: Timeframe,
        windowStart: LocalDate,
        windowEnd: LocalDate
    ): HeatMapGranularity = when (timeframe) {
        Timeframe.WEEKLY -> HeatMapGranularity.SINGLE_DAY
        Timeframe.MONTHLY -> HeatMapGranularity.DAY
        Timeframe.YEARLY -> HeatMapGranularity.WEEK_GRID
        Timeframe.ALL_TIME -> {
            val days = windowStart.until(windowEnd, DateTimeUnit.DAY)
            when {
                days <= 30 -> HeatMapGranularity.SINGLE_DAY
                days < 90 -> HeatMapGranularity.DAY
                days < 365 -> HeatMapGranularity.WEEK
                else -> HeatMapGranularity.MONTH
            }
        }
    }

    private fun buildYesNoCharts(
        answers: List<Answer>,
        timeZone: TimeZone,
        windowStart: LocalDate,
        windowEnd: LocalDate,
        dataStart: LocalDate,
        granularity: HeatMapGranularity,
        fillViewport: Boolean,
        lineVisibleDays: Int,
        lineYRange: Pair<Double, Double>?
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
            VisualizationData.CalendarHeatMap(dailyCounts, windowStart, windowEnd, dataStart, granularity, fillViewport),
            VisualizationData.LineGraph(dailyYesPoints, windowStart, windowEnd, lineVisibleDays, lineYRange?.first, lineYRange?.second),
            // YES/NO sit at opposite ends of the palette so they stay clearly distinct.
            VisualizationData.ColumnChart(listOf(
                NamedCount("YES", totalYes, orderFraction = 0f),
                NamedCount("NO", totalNo, orderFraction = 1f)
            ))
        )
    }

    private fun buildNumberCharts(
        answers: List<Answer>,
        timeZone: TimeZone,
        windowStart: LocalDate,
        windowEnd: LocalDate,
        dataStart: LocalDate,
        granularity: HeatMapGranularity,
        fillViewport: Boolean,
        lineVisibleDays: Int,
        lineYRange: Pair<Double, Double>?
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
            VisualizationData.LineGraph(points, windowStart, windowEnd, lineVisibleDays, lineYRange?.first, lineYRange?.second),
            VisualizationData.CalendarHeatMap(dailyCounts, windowStart, windowEnd, dataStart, granularity, fillViewport)
        )
    }

    /**
     * The fixed Y range for the line graph, computed over **all** answers so the axis is identical no
     * matter which window is shown. SCALE uses its defined bounds; NUMBER and YES/NO anchor at 0 and
     * top out at the rounded global maximum (max value, or max daily YES count). Null = auto-fit.
     */
    private fun lineGraphYRange(
        question: Question,
        allAnswers: List<Answer>,
        timeZone: TimeZone
    ): Pair<Double, Double>? = when (question.type) {
        QuestionType.SCALE -> (question.scaleMin?.toDouble() ?: 0.0) to (question.scaleMax?.toDouble() ?: 10.0)
        QuestionType.NUMBER -> {
            val max = allAnswers.mapNotNull { it.value.toDoubleOrNull() }.maxOrNull() ?: 0.0
            0.0 to niceCeil(max)
        }
        QuestionType.YES_NO -> {
            val maxDailyYes = allAnswers
                .groupBy { it.scheduledAt.toLocalDateTime(timeZone).date }
                .maxOfOrNull { (_, day) -> day.count { it.value.uppercase() == "YES" } } ?: 0
            0.0 to niceCeil(maxDailyYes.toDouble())
        }
        else -> null
    }

    /** Rounds [value] up to a clean axis bound (1, 2, or 5 × a power of ten); a value ≤ 0 yields 1. */
    private fun niceCeil(value: Double): Double {
        if (value <= 0.0) return 1.0
        val magnitude = 10.0.pow(floor(log10(value)))
        val fraction = value / magnitude
        val nice = when {
            fraction <= 1.0 -> 1.0
            fraction <= 2.0 -> 2.0
            fraction <= 5.0 -> 5.0
            else -> 10.0
        }
        return nice * magnitude
    }

    private fun buildTextCharts(answers: List<Answer>): List<VisualizationData> {
        val wordCounts = answers
            .flatMap { tokenizeText(it.value) }
            .groupingBy { it }
            .eachCount()
            .map { (word, count) -> NamedCount(word, count) }
            .sortedByDescending { it.count }
        return if (wordCounts.isEmpty()) emptyList()
        else listOf(VisualizationData.PackedBubble(wordCounts))
    }

    /**
     * Splits free text into words for the packed bubble chart. Each whitespace-separated token can
     * contribute both a letter "word" (lowercased, edge punctuation stripped, ≥ 3 chars, not a stop
     * word) and one or more emoji words. Emoji are kept whole and are exempt from the length and
     * stop-word filters, so an answer that is only an emoji still charts.
     */
    private fun tokenizeText(text: String): List<String> =
        text.split(Regex("\\s+")).flatMap { token ->
            buildList {
                val letters = token.lowercase().trim { !it.isLetter() }
                if (letters.length >= 3 && letters !in STOP_WORDS) add(letters)
                addAll(extractEmojiWords(token))
            }
        }

    private fun buildOptionCharts(
        answers: List<Answer>,
        optionsById: Map<String, QuestionOption>,
        includeColumnChart: Boolean
    ): List<VisualizationData> {
        // Spread the options evenly across the palette by their defined order, so each option keeps
        // a fixed color no matter its count, rank, or whether it appears in the current window.
        val optionUniverseSize = optionsById.size

        val optionCounts = answers
            .flatMap { answer -> answer.value.split(",").map { it.trim() } }
            .filter { it.isNotBlank() }
            .groupBy { id -> optionsById[id]?.text ?: id }
            .map { (label, ids) ->
                val orderIndex = ids.firstNotNullOfOrNull { optionsById[it]?.orderIndex }
                val orderFraction = if (orderIndex != null && optionUniverseSize > 1)
                    orderIndex.toFloat() / (optionUniverseSize - 1) else 0f
                NamedCount(label, ids.size, orderFraction)
            }
            .sortedByDescending { it.count }

        return buildList {
            add(VisualizationData.BarChart(optionCounts))
            if (includeColumnChart) add(VisualizationData.ColumnChart(optionCounts))
            add(VisualizationData.PackedBubble(optionCounts))
        }
    }
}

/** Window sizes (in days) for the dashboard's time-based timeframes. */
private const val WEEKLY_WINDOW_DAYS = 7
private const val MONTHLY_WINDOW_DAYS = 30
private const val YEARLY_WINDOW_DAYS = 365

/**
 * The `[start, end]` date window the whole nudge-detail dashboard is focused on: the [timeframe]'s
 * span, shifted [offsetDays] days back from [today] (0 = most recent). `ALL_TIME` spans from the
 * [earliest] recorded answer through [today] and ignores the offset.
 */
fun analysisWindow(
    timeframe: Timeframe,
    offsetDays: Int,
    today: LocalDate,
    earliest: LocalDate,
): Pair<LocalDate, LocalDate> {
    if (timeframe == Timeframe.ALL_TIME) return earliest to today
    val sizeDays = when (timeframe) {
        Timeframe.WEEKLY -> WEEKLY_WINDOW_DAYS
        Timeframe.MONTHLY -> MONTHLY_WINDOW_DAYS
        Timeframe.YEARLY -> YEARLY_WINDOW_DAYS
        Timeframe.ALL_TIME -> WEEKLY_WINDOW_DAYS // unreachable; handled above
    }
    val end = today.minus(offsetDays.coerceAtLeast(0), DateTimeUnit.DAY)
    val start = end.minus(sizeDays - 1, DateTimeUnit.DAY)
    return start to end
}

/** A week, in days — the granularity of the yearly heat map's cells. */
const val DAYS_PER_WEEK = 7

/**
 * How many days the shared window slides per one-cell step on each timeframe, matching the heat
 * map's cell granularity: the yearly heat map's cells are whole weeks, so its window must step a
 * week at a time (otherwise a day-sized shift re-buckets partial weeks and the week-cell count
 * oscillates, reshuffling the grid). Weekly and monthly have day cells and step a day at a time.
 * `ALL_TIME` does not scroll, so its step is unused.
 */
fun windowStepDays(timeframe: Timeframe): Int =
    if (timeframe == Timeframe.YEARLY) DAYS_PER_WEEK else 1
