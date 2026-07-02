// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.usecase

import com.nudgery.shared.model.Answer
import com.nudgery.shared.model.DailyCount
import com.nudgery.shared.model.DataPoint
import com.nudgery.shared.model.HeatMapBucketAggregation
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
            QuestionType.YES_NO -> buildYesNoCharts(answers, timeZone, windowStart, windowEnd, earliest, granularity, fillViewport, lineVisibleDays, lineYRange, question.collapsePerDay)
            QuestionType.SCALE, QuestionType.NUMBER -> buildNumberCharts(question, answers, timeZone, windowStart, windowEnd, earliest, granularity, fillViewport, lineVisibleDays, lineYRange)
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
        lineYRange: Pair<Double, Double>?,
        collapsePerDay: Boolean
    ): List<VisualizationData> {
        // ED-17: when "One Yes Per Day" is on, each calendar day contributes a single bit — 1 if any
        // "YES" that day, else 0 — instead of summing every Yes. Larger heat-map buckets then sum
        // these day-bits ("Yes days"). Off (default), every Yes is counted.
        val answersByDate = answers.groupBy { it.scheduledAt.toLocalDateTime(timeZone).date }
        val dailyCounts = answersByDate
            .map { (date, dayAnswers) ->
                val yesCount = dayAnswers.count { it.value.uppercase() == "YES" }
                val value = if (collapsePerDay) (if (yesCount > 0) 1.0 else 0.0) else yesCount.toDouble()
                DailyCount(date, value)
            }
            .sortedBy { it.date }

        val dailyYesPoints = dailyCounts.map { DataPoint(it.date.atStartOfDayIn(timeZone), it.value) }

        // Collapsed: the summary counts Yes days vs No days (a day with answers but no Yes is one No
        // day). Otherwise it counts raw Yes vs No answers.
        val totalYes: Int
        val totalNo: Int
        if (collapsePerDay) {
            totalYes = answersByDate.values.count { day -> day.any { it.value.uppercase() == "YES" } }
            totalNo = answersByDate.size - totalYes
        } else {
            totalYes = answers.count { it.value.uppercase() == "YES" }
            totalNo = answers.count { it.value.uppercase() == "NO" }
        }

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
        question: Question,
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

        // DESIGN.md "Heat Map Value-to-Color Scaling": a scale answer is a level, not an event
        // count, so its heat map colors are anchored to the question's defined bounds in every view
        // and its week/month buckets average their logged days. NUMBER answers are quantities: the
        // color scale fits the observed cells and buckets keep summing.
        val isScale = question.type == QuestionType.SCALE
        return listOf(
            VisualizationData.LineGraph(points, windowStart, windowEnd, lineVisibleDays, lineYRange?.first, lineYRange?.second),
            VisualizationData.CalendarHeatMap(
                dailyCounts, windowStart, windowEnd, dataStart, granularity, fillViewport,
                colorScaleMin = if (isScale) (question.scaleMin ?: 0).toDouble() else null,
                colorScaleMax = if (isScale) (question.scaleMax ?: 10).toDouble() else null,
                bucketAggregation = if (isScale) HeatMapBucketAggregation.AVERAGE
                                    else HeatMapBucketAggregation.SUM
            )
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
            // With "One Yes Per Day" (ED-17) each day's line value is 0 or 1, so the axis tops out at
            // 1; otherwise it tops out at the busiest day's raw Yes count.
            val maxDailyYes = if (question.collapsePerDay) {
                if (allAnswers.any { it.value.uppercase() == "YES" }) 1 else 0
            } else {
                allAnswers
                    .groupBy { it.scheduledAt.toLocalDateTime(timeZone).date }
                    .maxOfOrNull { (_, day) -> day.count { it.value.uppercase() == "YES" } } ?: 0
            }
            0.0 to niceCeil(maxDailyYes.toDouble())
        }
        else -> null
    }

    /**
     * Rounds [value] up to a clean axis bound: the smallest "nice" number ([NICE_AXIS_FRACTIONS] ×
     * a power of ten) that is ≥ [value]. The fine ladder keeps the bound close to the data — modest
     * headroom, at most ~25% above the max — rather than overshooting toward 2× as a coarse
     * 1/2/5/10 ladder would (e.g. a max of 11 yields 12, not 20). A value ≤ 0 yields 1.
     */
    private fun niceCeil(value: Double): Double {
        if (value <= 0.0) return 1.0
        val magnitude = 10.0.pow(floor(log10(value)))
        val fraction = value / magnitude
        // Tolerate floating-point noise so e.g. 12/10 == 1.2000…2 still matches the 1.2 step.
        val niceFraction = NICE_AXIS_FRACTIONS.first { it >= fraction - 1e-9 }
        return niceFraction * magnitude
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

/**
 * The "nice" mantissas a line-graph Y axis may top out at (× a power of ten), ascending. Finer than
 * the classic 1/2/5/10 so the locked axis sits just above the data max instead of wasting up to half
 * the height. See `niceCeil`.
 */
private val NICE_AXIS_FRACTIONS = listOf(1.0, 1.2, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0)

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
