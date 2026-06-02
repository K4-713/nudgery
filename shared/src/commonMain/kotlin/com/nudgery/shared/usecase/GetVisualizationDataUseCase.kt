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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
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
        val answers = answerRepository.getVisibleByNudgeIdSince(nudgeId, Instant.DISTANT_PAST)
            .filter { it.questionId == questionId }

        val windowStart = answers.minOfOrNull { it.scheduledAt.toLocalDateTime(timeZone).date } ?: today
        val windowEnd = today

        val granularity = computeGranularity(timeframe, windowStart, windowEnd)
        val fillViewport = timeframe == Timeframe.ALL_TIME
        // Days the line graph shows at once before scrolling; ALL_TIME fits everything (no scroll).
        val lineVisibleDays = when (timeframe) {
            Timeframe.WEEKLY -> 7
            Timeframe.MONTHLY -> 31
            Timeframe.YEARLY -> 365
            Timeframe.ALL_TIME -> Int.MAX_VALUE
        }

        return when (question.type) {
            QuestionType.YES_NO -> buildYesNoCharts(answers, timeZone, windowStart, windowEnd, granularity, fillViewport, lineVisibleDays)
            QuestionType.SCALE, QuestionType.NUMBER -> buildNumberCharts(answers, timeZone, windowStart, windowEnd, granularity, fillViewport, lineVisibleDays)
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
        granularity: HeatMapGranularity,
        fillViewport: Boolean,
        lineVisibleDays: Int
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
            VisualizationData.CalendarHeatMap(dailyCounts, windowStart, windowEnd, granularity, fillViewport),
            VisualizationData.LineGraph(dailyYesPoints, windowStart, windowEnd, lineVisibleDays),
            VisualizationData.ColumnChart(listOf(NamedCount("YES", totalYes), NamedCount("NO", totalNo)))
        )
    }

    private fun buildNumberCharts(
        answers: List<Answer>,
        timeZone: TimeZone,
        windowStart: LocalDate,
        windowEnd: LocalDate,
        granularity: HeatMapGranularity,
        fillViewport: Boolean,
        lineVisibleDays: Int
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
            VisualizationData.LineGraph(points, windowStart, windowEnd, lineVisibleDays),
            VisualizationData.CalendarHeatMap(dailyCounts, windowStart, windowEnd, granularity, fillViewport)
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

    /**
     * Returns each individual emoji in [token] as its own word, so distinct emoji written without a
     * space (e.g. 🐶🐱) become separate words. A single emoji's modifiers, variation selectors,
     * ZWJ-joined parts, and regional-indicator (flag) pairs stay attached to that one emoji.
     */
    private fun extractEmojiWords(token: String): List<String> {
        val words = mutableListOf<String>()
        val current = StringBuilder()
        var afterZwj = false       // previous code point was a zero-width joiner
        var regionalRun = 0        // count of consecutive regional indicators in `current`

        fun flush() {
            if (current.isNotEmpty()) words.add(current.toString())
            current.clear()
            afterZwj = false
            regionalRun = 0
        }

        var index = 0
        while (index < token.length) {
            val high = token[index]
            val pairedLow = if (high.isHighSurrogate() && index + 1 < token.length) token[index + 1] else null
            val (codePoint, charCount) = if (pairedLow != null && pairedLow.isLowSurrogate()) {
                combineSurrogates(high, pairedLow) to 2
            } else {
                high.code to 1
            }

            when {
                isEmojiJoiner(codePoint) -> {
                    if (current.isNotEmpty()) {
                        current.append(token, index, index + charCount)
                        afterZwj = codePoint == 0x200D
                        regionalRun = 0
                    }
                }
                isEmojiStart(codePoint) -> {
                    val regional = codePoint in 0x1F1E6..0x1F1FF
                    val continuesCurrent = current.isEmpty() ||
                        afterZwj ||                          // ZWJ sequence continues this emoji
                        (regional && regionalRun == 1)       // second half of a flag
                    if (!continuesCurrent) flush()
                    current.append(token, index, index + charCount)
                    afterZwj = false
                    regionalRun = if (regional) regionalRun + 1 else 0
                }
                else -> flush()
            }
            index += charCount
        }
        flush()
        return words
    }

    private fun combineSurrogates(high: Char, low: Char): Int =
        0x10000 + ((high.code - 0xD800) shl 10) + (low.code - 0xDC00)

    /** A code point that begins (or is) an emoji. */
    private fun isEmojiStart(codePoint: Int): Boolean = when (codePoint) {
        in 0x1F000..0x1FAFF -> true   // emoticons, pictographs, transport, supplemental, flags
        in 0x2600..0x27BF -> true     // miscellaneous symbols and dingbats
        in 0x2B00..0x2BFF -> true     // miscellaneous symbols and arrows
        in 0x2300..0x23FF -> true     // miscellaneous technical (⌚ ⏰ ▶ …)
        in 0x25A0..0x25FF -> true     // geometric shapes
        in 0x2190..0x21FF -> true     // arrows
        0x2122, 0x2139, 0x203C, 0x2049, 0x24C2, 0x3030, 0x303D, 0x3297, 0x3299 -> true
        else -> false
    }

    /** Code points that extend an emoji already in progress (joiner, variation, skin tone, keycap). */
    private fun isEmojiJoiner(codePoint: Int): Boolean =
        codePoint == 0x200D || codePoint == 0x20E3 ||
            codePoint in 0xFE00..0xFE0F || codePoint in 0x1F3FB..0x1F3FF

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
            add(VisualizationData.PackedBubble(optionCounts))
        }
    }
}
