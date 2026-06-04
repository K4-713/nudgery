package com.nudgery.shared

import com.nudgery.shared.model.Answer
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.model.Timeframe
import com.nudgery.shared.model.TriggerOperator
import com.nudgery.shared.model.VisualizationData
import com.nudgery.shared.usecase.CreateNudgeRequest
import com.nudgery.shared.usecase.CreateNudgeResult
import com.nudgery.shared.usecase.CreateNudgeUseCase
import com.nudgery.shared.usecase.GetVisualizationDataUseCase
import com.nudgery.shared.usecase.QuestionRequest
import com.nudgery.shared.usecase.ScheduleRequest
import com.nudgery.shared.usecase.SetAnswerHiddenUseCase
import com.nudgery.shared.usecase.analysisWindow
import com.nudgery.shared.util.isSingleEmoji
import com.nudgery.shared.usecase.windowStepDays
import com.nudgery.shared.util.FakeNotificationScheduler
import com.nudgery.shared.util.TestRepositories
import com.nudgery.shared.util.createTestRepositories
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.until
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VisualizationDataTest {

    private lateinit var repos: TestRepositories
    private lateinit var createNudge: CreateNudgeUseCase
    private lateinit var getVisualizationData: GetVisualizationDataUseCase
    private lateinit var setAnswerHidden: SetAnswerHiddenUseCase

    @BeforeTest
    fun setup() {
        repos = createTestRepositories()
        createNudge = CreateNudgeUseCase(
            repos.nudgeRepository, repos.questionRepository, repos.questionOptionRepository,
            repos.scheduleRepository, FakeNotificationScheduler()
        )
        getVisualizationData = GetVisualizationDataUseCase(
            repos.answerRepository, repos.questionRepository, repos.questionOptionRepository
        )
        setAnswerHidden = SetAnswerHiddenUseCase(repos.answerRepository)
    }

    private fun dailySchedule() = ScheduleRequest(
        type = ScheduleType.DAILY,
        timeOfDay = LocalTime(12, 0),
        activeDaysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
    )

    private suspend fun createNudgeAndRecordAnswer(
        questionType: QuestionType,
        options: List<String> = emptyList(),
        answerValue: String
    ): Pair<String, String> {
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Test question", questionType, options),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val nudgeId = result.nudgeId
        val questionId = repos.questionRepository.getByNudgeId(nudgeId).first { it.isMainQuestion }.id

        repos.answerRepository.insert(
            Answer(
                id = "ans-${nudgeId}",
                nudgeId = nudgeId,
                questionId = questionId,
                value = answerValue,
                scheduledAt = Clock.System.now(),
                answeredAt = Clock.System.now(),
                isHidden = false
            )
        )
        return nudgeId to questionId
    }

    // --- YES_NO ---

    @Test
    fun TDD_yesNoQuestionProvidesCalendarHeatMapData() = runTest {
        // README "Viewing Nudges": "YES_NO | Calendar heat map, line graph (daily yes count), column chart"
        val (nudgeId, questionId) = createNudgeAndRecordAnswer(QuestionType.YES_NO, answerValue = "YES")
        val charts = getVisualizationData.execute(nudgeId, questionId, Timeframe.ALL_TIME)

        assertTrue(charts.any { it is VisualizationData.CalendarHeatMap },
            "YES_NO should provide a CalendarHeatMap")
    }

    @Test
    fun TDD_yesNoQuestionProvidesLineGraphData() = runTest {
        // README "Viewing Nudges": "YES_NO | Calendar heat map, line graph (daily yes count), column chart"
        val (nudgeId, questionId) = createNudgeAndRecordAnswer(QuestionType.YES_NO, answerValue = "YES")
        val charts = getVisualizationData.execute(nudgeId, questionId, Timeframe.ALL_TIME)

        assertTrue(charts.any { it is VisualizationData.LineGraph },
            "YES_NO should provide a LineGraph")
        val lineGraph = charts.filterIsInstance<VisualizationData.LineGraph>().first()
        assertEquals(1.0, lineGraph.points.sumOf { it.value }, "Line graph point value should be the daily yes count")
    }

    @Test
    fun TDD_yesNoQuestionProvidesColumnChartData() = runTest {
        // README "Viewing Nudges": "YES_NO | Calendar heat map, line graph (daily yes count), column chart"
        val (nudgeId, questionId) = createNudgeAndRecordAnswer(QuestionType.YES_NO, answerValue = "YES")
        val charts = getVisualizationData.execute(nudgeId, questionId, Timeframe.ALL_TIME)

        assertTrue(charts.any { it is VisualizationData.ColumnChart },
            "YES_NO should provide a ColumnChart")
    }

    // --- NUMBER ---

    @Test
    fun TDD_numberQuestionProvidesLineGraphData() = runTest {
        // README "Viewing Nudges": "NUMBER | Line graph, calendar heat map"
        val (nudgeId, questionId) = createNudgeAndRecordAnswer(QuestionType.NUMBER, answerValue = "7")
        val charts = getVisualizationData.execute(nudgeId, questionId, Timeframe.ALL_TIME)

        assertTrue(charts.any { it is VisualizationData.LineGraph },
            "NUMBER should provide a LineGraph")
    }

    @Test
    fun TDD_lineGraphFitsTheSharedWindow() = runTest {
        // The dashboard is locked to one window; the line graph fits exactly that window's days.
        // For a single answer recorded today, ALL_TIME spans just that one day.
        val (nudgeId, questionId) = createNudgeAndRecordAnswer(QuestionType.NUMBER, answerValue = "7")

        suspend fun visibleDays(tf: Timeframe) = getVisualizationData.execute(nudgeId, questionId, tf)
            .filterIsInstance<VisualizationData.LineGraph>().first().visibleDays

        assertEquals(7, visibleDays(Timeframe.WEEKLY))
        assertEquals(30, visibleDays(Timeframe.MONTHLY))
        assertEquals(365, visibleDays(Timeframe.YEARLY))
        assertEquals(1, visibleDays(Timeframe.ALL_TIME),
            "ALL_TIME spans earliest..today; one same-day answer is a single day")
    }

    @Test
    fun TDD_numberQuestionProvidesCalendarHeatMapData() = runTest {
        // README "Viewing Nudges": "NUMBER | Line graph, calendar heat map"
        val (nudgeId, questionId) = createNudgeAndRecordAnswer(QuestionType.NUMBER, answerValue = "7")
        val charts = getVisualizationData.execute(nudgeId, questionId, Timeframe.ALL_TIME)

        assertTrue(charts.any { it is VisualizationData.CalendarHeatMap },
            "NUMBER should provide a CalendarHeatMap")
    }

    // --- SCALE ---

    @Test
    fun TDD_scaleQuestionProvidesLineGraphData() = runTest {
        // README "Viewing Nudges": "SCALE | Line graph, calendar heat map (daily average)"
        val (nudgeId, questionId) = createNudgeAndRecordAnswer(QuestionType.SCALE, answerValue = "7")
        val charts = getVisualizationData.execute(nudgeId, questionId, Timeframe.ALL_TIME)

        assertTrue(charts.any { it is VisualizationData.LineGraph },
            "SCALE should provide a LineGraph")
    }

    @Test
    fun TDD_scaleQuestionProvidesCalendarHeatMapData() = runTest {
        // README "Viewing Nudges": "SCALE | Line graph, calendar heat map (daily average)"
        val (nudgeId, questionId) = createNudgeAndRecordAnswer(QuestionType.SCALE, answerValue = "7")
        val charts = getVisualizationData.execute(nudgeId, questionId, Timeframe.ALL_TIME)

        assertTrue(charts.any { it is VisualizationData.CalendarHeatMap },
            "SCALE should provide a CalendarHeatMap")
    }

    @Test
    fun TDD_optionChartsCarryStablePerCategoryPaletteFraction() = runTest {
        // Bar and column charts color each category by a fixed palette position (orderFraction) so a
        // category keeps its color as the timeframe moves and the bars re-sort by count. The
        // fraction follows the option's defined order, spread evenly 0..1, independent of count.
        val options = listOf("Good", "Okay", "Bad")  // orderIndex 0, 1, 2
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("How do you feel?", QuestionType.OPTION_SINGLE, options),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val nudgeId = result.nudgeId
        val questionId = repos.questionRepository.getByNudgeId(nudgeId).first { it.isMainQuestion }.id
        val optionIds = repos.questionOptionRepository.getByQuestionId(questionId).sortedBy { it.orderIndex }

        // "Bad" (last option) answered most often → it sorts first by count, but must still carry the
        // palette fraction for its order position, not for its rank.
        val now = Clock.System.now()
        listOf(optionIds[0].id, optionIds[2].id, optionIds[2].id).forEachIndexed { i, optId ->
            repos.answerRepository.insert(Answer(id = "ans-opt-$i", nudgeId = nudgeId, questionId = questionId,
                value = optId, scheduledAt = now, answeredAt = now, isHidden = false))
        }

        val bar = getVisualizationData.execute(nudgeId, questionId, Timeframe.ALL_TIME)
            .filterIsInstance<VisualizationData.BarChart>().first()
        // Bars are sorted by count (Bad first), but each carries its order-based fraction.
        assertEquals(1.0f, bar.entries.first { it.label == "Bad" }.orderFraction, "last of 3 options → 1.0")
        assertEquals(0.0f, bar.entries.first { it.label == "Good" }.orderFraction, "first of 3 options → 0.0")
        assertEquals("Bad", bar.entries.first().label, "highest count sorts first")
    }

    @Test
    fun TDD_yesNoColumnChartPutsYesAndNoAtOppositePaletteEnds() = runTest {
        // YES/NO are colored at opposite ends of the palette so the two columns stay clearly distinct.
        val (nudgeId, questionId) = createNudgeAndRecordAnswer(QuestionType.YES_NO, answerValue = "YES")
        val column = getVisualizationData.execute(nudgeId, questionId, Timeframe.ALL_TIME)
            .filterIsInstance<VisualizationData.ColumnChart>().first()
        assertEquals(0.0f, column.entries.first { it.label == "YES" }.orderFraction)
        assertEquals(1.0f, column.entries.first { it.label == "NO" }.orderFraction)
    }

    // --- OPTION_SINGLE ---

    @Test
    fun TDD_optionSingleQuestionProvidesBarChartData() = runTest {
        // README "Viewing Nudges": "OPTION_SINGLE | Bar chart, column chart, packed bubble chart"
        val options = listOf("Good", "Okay", "Bad")
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("How do you feel?", QuestionType.OPTION_SINGLE, options),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val nudgeId = result.nudgeId
        val questionId = repos.questionRepository.getByNudgeId(nudgeId).first { it.isMainQuestion }.id
        val optionId = repos.questionOptionRepository.getByQuestionId(questionId).first().id

        repos.answerRepository.insert(
            Answer(id = "ans", nudgeId = nudgeId, questionId = questionId,
                value = optionId, scheduledAt = Clock.System.now(), answeredAt = Clock.System.now(), isHidden = false)
        )

        val charts = getVisualizationData.execute(nudgeId, questionId, Timeframe.ALL_TIME)
        assertTrue(charts.any { it is VisualizationData.BarChart },
            "OPTION_SINGLE should provide a BarChart")
    }

    @Test
    fun TDD_optionSingleQuestionProvidesColumnChartData() = runTest {
        // README "Viewing Nudges": "OPTION_SINGLE | Bar chart, column chart, packed bubble chart"
        val options = listOf("Good", "Okay", "Bad")
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("How do you feel?", QuestionType.OPTION_SINGLE, options),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val nudgeId = result.nudgeId
        val questionId = repos.questionRepository.getByNudgeId(nudgeId).first { it.isMainQuestion }.id
        val optionId = repos.questionOptionRepository.getByQuestionId(questionId).first().id

        repos.answerRepository.insert(
            Answer(id = "ans2", nudgeId = nudgeId, questionId = questionId,
                value = optionId, scheduledAt = Clock.System.now(), answeredAt = Clock.System.now(), isHidden = false)
        )

        val charts = getVisualizationData.execute(nudgeId, questionId, Timeframe.ALL_TIME)
        assertTrue(charts.any { it is VisualizationData.ColumnChart },
            "OPTION_SINGLE should provide a ColumnChart")
    }

    @Test
    fun TDD_optionSingleQuestionProvidesPackedBubbleData() = runTest {
        // README "Viewing Nudges": "OPTION_SINGLE | Bar chart, column chart, packed bubble chart"
        val options = listOf("Good", "Okay", "Bad")
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("How do you feel?", QuestionType.OPTION_SINGLE, options),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val nudgeId = result.nudgeId
        val questionId = repos.questionRepository.getByNudgeId(nudgeId).first { it.isMainQuestion }.id
        val optionId = repos.questionOptionRepository.getByQuestionId(questionId).first().id

        repos.answerRepository.insert(
            Answer(id = "ans3", nudgeId = nudgeId, questionId = questionId,
                value = optionId, scheduledAt = Clock.System.now(), answeredAt = Clock.System.now(), isHidden = false)
        )

        val charts = getVisualizationData.execute(nudgeId, questionId, Timeframe.ALL_TIME)
        assertTrue(charts.any { it is VisualizationData.PackedBubble },
            "OPTION_SINGLE should provide a PackedBubble")
    }

    // --- OPTION_MULTI ---

    @Test
    fun TDD_optionMultiQuestionProvidesBarChartData() = runTest {
        // README "Viewing Nudges": "OPTION_MULTI | Bar chart, packed bubble chart"
        val options = listOf("Headache", "Fatigue", "Nausea")
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Which symptoms?", QuestionType.OPTION_MULTI, options),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val nudgeId = result.nudgeId
        val questionId = repos.questionRepository.getByNudgeId(nudgeId).first { it.isMainQuestion }.id
        val optionId = repos.questionOptionRepository.getByQuestionId(questionId).first().id

        repos.answerRepository.insert(
            Answer(id = "ans4", nudgeId = nudgeId, questionId = questionId,
                value = optionId, scheduledAt = Clock.System.now(), answeredAt = Clock.System.now(), isHidden = false)
        )

        val charts = getVisualizationData.execute(nudgeId, questionId, Timeframe.ALL_TIME)
        assertTrue(charts.any { it is VisualizationData.BarChart },
            "OPTION_MULTI should provide a BarChart")
    }

    @Test
    fun TDD_optionMultiQuestionProvidesPackedBubbleData() = runTest {
        // README "Viewing Nudges": "OPTION_MULTI | Bar chart, packed bubble chart"
        val options = listOf("Headache", "Fatigue", "Nausea")
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Which symptoms?", QuestionType.OPTION_MULTI, options),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val nudgeId = result.nudgeId
        val questionId = repos.questionRepository.getByNudgeId(nudgeId).first { it.isMainQuestion }.id
        val optionId = repos.questionOptionRepository.getByQuestionId(questionId).first().id

        repos.answerRepository.insert(
            Answer(id = "ans5", nudgeId = nudgeId, questionId = questionId,
                value = optionId, scheduledAt = Clock.System.now(), answeredAt = Clock.System.now(), isHidden = false)
        )

        val charts = getVisualizationData.execute(nudgeId, questionId, Timeframe.ALL_TIME)
        assertTrue(charts.any { it is VisualizationData.PackedBubble },
            "OPTION_MULTI should provide a PackedBubble")
    }

    // --- Timeframes ---

    @Test
    fun TDD_periodOffsetShiftsTheSharedWindow() = runTest {
        // The dashboard window can be shifted back in time: offset 0 = most recent, larger = older.
        val (nudgeId, questionId) = createNudgeAndRecordAnswer(QuestionType.YES_NO, answerValue = "YES")
        val now = Clock.System.now()
        repos.answerRepository.insert(
            Answer(id = "ten-days-ago", nudgeId = nudgeId, questionId = questionId,
                value = "YES", scheduledAt = now - 10.days, answeredAt = now - 10.days, isHidden = false)
        )

        suspend fun weeklySum(offsetDays: Int) = getVisualizationData
            .execute(nudgeId, questionId, Timeframe.WEEKLY, periodOffsetDays = offsetDays, now = now)
            .filterIsInstance<VisualizationData.CalendarHeatMap>().first()
            .dailyCounts.sumOf { it.value }

        // Most-recent week sees only today's answer; not the 10-day-old one.
        assertEquals(1.0, weeklySum(0), "offset 0 = this week (today only)")
        // A window ending 10 days ago (spanning days 16..10 back) catches the older answer.
        assertEquals(1.0, weeklySum(10), "offset 10 = the week around 10 days ago")
        // A window in between catches neither.
        assertEquals(0.0, weeklySum(3), "offset 3 = a week with no answers")
    }

    @Test
    fun TDD_visualizationDataFilterableByWeeklyTimeframe() = runTest {
        // README "Viewing Nudges": "The timeframe can be switched between weekly, monthly,
        //   yearly, and all-time"
        val (nudgeId, questionId) = createNudgeAndRecordAnswer(QuestionType.YES_NO, answerValue = "YES")

        // Recent answer → should appear in WEEKLY timeframe
        val weeklyCharts = getVisualizationData.execute(nudgeId, questionId, Timeframe.WEEKLY)
        assertTrue(weeklyCharts.isNotEmpty())
        val heatMap = weeklyCharts.filterIsInstance<VisualizationData.CalendarHeatMap>().first()
        assertEquals(1.0, heatMap.dailyCounts.sumOf { it.value })
    }

    @Test
    fun TDD_visualizationDataFilterableByMonthlyTimeframe() = runTest {
        // README "Viewing Nudges": "...weekly, monthly, yearly, and all-time"
        val (nudgeId, questionId) = createNudgeAndRecordAnswer(QuestionType.YES_NO, answerValue = "YES")

        val monthlyCharts = getVisualizationData.execute(nudgeId, questionId, Timeframe.MONTHLY)
        assertTrue(monthlyCharts.isNotEmpty())
        val heatMap = monthlyCharts.filterIsInstance<VisualizationData.CalendarHeatMap>().first()
        assertEquals(1.0, heatMap.dailyCounts.sumOf { it.value })
    }

    @Test
    fun TDD_visualizationDataFilterableByYearlyTimeframe() = runTest {
        // README "Viewing Nudges": "...weekly, monthly, yearly, and all-time"
        val (nudgeId, questionId) = createNudgeAndRecordAnswer(QuestionType.YES_NO, answerValue = "YES")

        val yearlyCharts = getVisualizationData.execute(nudgeId, questionId, Timeframe.YEARLY)
        assertTrue(yearlyCharts.isNotEmpty())
        val heatMap = yearlyCharts.filterIsInstance<VisualizationData.CalendarHeatMap>().first()
        assertEquals(1.0, heatMap.dailyCounts.sumOf { it.value })
    }

    @Test
    fun TDD_visualizationDataFilterableByAllTimeTimeframe() = runTest {
        // README "Viewing Nudges": "...weekly, monthly, yearly, and all-time"
        val (nudgeId, questionId) = createNudgeAndRecordAnswer(QuestionType.YES_NO, answerValue = "YES")

        val allTimeCharts = getVisualizationData.execute(nudgeId, questionId, Timeframe.ALL_TIME)
        assertTrue(allTimeCharts.isNotEmpty())
        val heatMap = allTimeCharts.filterIsInstance<VisualizationData.CalendarHeatMap>().first()
        assertEquals(1.0, heatMap.dailyCounts.sumOf { it.value })
    }

    @Test
    fun TDD_yearlyHeatMapWindowStepsByWholeWeeks() {
        // DESIGN.md "Charts and Visualizations" — the yearly heat map's cells are weeks, so its
        // window steps a week at a time; weekly and monthly step a day.
        assertEquals(7, windowStepDays(Timeframe.YEARLY), "Yearly steps by a whole week")
        assertEquals(1, windowStepDays(Timeframe.WEEKLY), "Weekly steps by a day")
        assertEquals(1, windowStepDays(Timeframe.MONTHLY), "Monthly steps by a day")

        // Stepping the yearly window by one week shifts both ends by exactly 7 days and keeps the
        // start's day-of-week, so the Monday-aligned week grid holds a constant cell count instead
        // of oscillating between 52/53 weeks (which would reshuffle the grid).
        val today = LocalDate(2026, 6, 2)
        val earliest = LocalDate(2020, 1, 1)
        val (start0, end0) = analysisWindow(Timeframe.YEARLY, 0, today, earliest)
        val step = windowStepDays(Timeframe.YEARLY)
        val (start1, end1) = analysisWindow(Timeframe.YEARLY, step, today, earliest)

        assertEquals(step, start1.until(start0, DateTimeUnit.DAY).toInt(), "Start slides one week")
        assertEquals(step, end1.until(end0, DateTimeUnit.DAY).toInt(), "End slides one week")
        assertEquals(start0.dayOfWeek, start1.dayOfWeek, "Day-of-week preserved → constant cell count")
    }

    @Test
    fun TDD_monthlyYearlyAndAllTimeHeatMapsFillTheCanvasWithoutScrolling() = runTest {
        // DESIGN.md "Charts and Visualizations" — Heat map fill vs. scroll: "monthly, yearly, and
        //   all-time heat maps size their cells to fill the canvas with no internal scroll ...
        //   Weekly is the exception: it shows a short, large-celled strip of days that scrolls."
        val (nudgeId, questionId) = createNudgeAndRecordAnswer(QuestionType.YES_NO, answerValue = "YES")

        suspend fun heatMap(timeframe: Timeframe) = getVisualizationData
            .execute(nudgeId, questionId, timeframe)
            .filterIsInstance<VisualizationData.CalendarHeatMap>().first()

        assertTrue(heatMap(Timeframe.MONTHLY).fillViewport, "Monthly heat map should fill the canvas")
        assertTrue(heatMap(Timeframe.YEARLY).fillViewport, "Yearly heat map should fill the canvas")
        assertTrue(heatMap(Timeframe.ALL_TIME).fillViewport, "All-time heat map should fill the canvas")
        assertTrue(!heatMap(Timeframe.WEEKLY).fillViewport, "Weekly heat map should scroll, not fill")
    }

    // --- TEXT (packed bubble) ---

    @Test
    fun TDD_textFollowUpQuestionProvidesPackedBubble() = runTest {
        // README: Follow-up questions can be TEXT type; packed bubble chart is the available visualization
        val (nudgeId, followUpId) = createNudgeWithTextFollowUp()
        repos.answerRepository.insert(
            Answer(id = "ans-text-1", nudgeId = nudgeId, questionId = followUpId,
                value = "Flying over colorful mountains", scheduledAt = Clock.System.now(),
                answeredAt = Clock.System.now(), isHidden = false)
        )

        val charts = getVisualizationData.execute(nudgeId, followUpId, Timeframe.ALL_TIME)

        assertTrue(charts.any { it is VisualizationData.PackedBubble },
            "TEXT question should produce a PackedBubble")
    }

    @Test
    fun TDD_emojiQuestionProvidesPackedBubble() = runTest {
        // README "Viewing Nudges": "EMOJI | Packed bubble chart". EMOJI charts as TEXT (ED-1).
        val (nudgeId, questionId) = createNudgeAndRecordAnswer(QuestionType.EMOJI, answerValue = "😀🐱")
        val charts = getVisualizationData.execute(nudgeId, questionId, Timeframe.ALL_TIME)

        assertTrue(charts.any { it is VisualizationData.PackedBubble },
            "EMOJI should produce a PackedBubble")
    }

    @Test
    fun TDD_textPackedBubbleExcludesStopWords() = runTest {
        // Stop words (function words like "the", "was", "and") should be filtered from the packed bubble chart
        val (nudgeId, followUpId) = createNudgeWithTextFollowUp()
        repos.answerRepository.insert(
            Answer(id = "ans-text-stop", nudgeId = nudgeId, questionId = followUpId,
                value = "the dragon was flying and breathing fire", scheduledAt = Clock.System.now(),
                answeredAt = Clock.System.now(), isHidden = false)
        )

        val charts = getVisualizationData.execute(nudgeId, followUpId, Timeframe.ALL_TIME)
        val bubbles = charts.filterIsInstance<VisualizationData.PackedBubble>().first()
        val words = bubbles.entries.map { it.label }

        assertTrue("the" !in words, "Stop word 'the' should be excluded")
        assertTrue("was" !in words, "Stop word 'was' should be excluded")
        assertTrue("and" !in words, "Stop word 'and' should be excluded")
        assertTrue("dragon" in words, "Content word 'dragon' should be included")
        assertTrue("fire" in words, "Content word 'fire' should be included")
    }

    @Test
    fun TDD_textPackedBubbleExcludesShortWords() = runTest {
        // Words under 3 characters are filtered to remove noise like "ok", "hi", "ha"
        val (nudgeId, followUpId) = createNudgeWithTextFollowUp()
        repos.answerRepository.insert(
            Answer(id = "ans-text-short", nudgeId = nudgeId, questionId = followUpId,
                value = "ok so a dragon appeared", scheduledAt = Clock.System.now(),
                answeredAt = Clock.System.now(), isHidden = false)
        )

        val charts = getVisualizationData.execute(nudgeId, followUpId, Timeframe.ALL_TIME)
        val bubbles = charts.filterIsInstance<VisualizationData.PackedBubble>().first()
        val words = bubbles.entries.map { it.label }

        assertTrue("ok" !in words, "Short word 'ok' (2 chars) should be excluded")
        assertTrue("a" !in words, "Short word 'a' (1 char) should be excluded")
        assertTrue("so" !in words, "Short word 'so' (2 chars) should be excluded")
        assertTrue("dragon" in words, "Longer word 'dragon' should be included")
    }

    @Test
    fun TDD_textPackedBubbleCountsWordFrequencyAcrossAnswers() = runTest {
        // The same word appearing in multiple answers should be summed across all of them
        val (nudgeId, followUpId) = createNudgeWithTextFollowUp()
        val now = Clock.System.now()
        repos.answerRepository.insert(Answer(id = "ans-freq-1", nudgeId = nudgeId, questionId = followUpId,
            value = "chasing dragon through forest", scheduledAt = now, answeredAt = now, isHidden = false))
        repos.answerRepository.insert(Answer(id = "ans-freq-2", nudgeId = nudgeId, questionId = followUpId,
            value = "the dragon returned breathing fire", scheduledAt = now, answeredAt = now, isHidden = false))
        repos.answerRepository.insert(Answer(id = "ans-freq-3", nudgeId = nudgeId, questionId = followUpId,
            value = "forest was peaceful without dragon", scheduledAt = now, answeredAt = now, isHidden = false))

        val charts = getVisualizationData.execute(nudgeId, followUpId, Timeframe.ALL_TIME)
        val bubbles = charts.filterIsInstance<VisualizationData.PackedBubble>().first()
        val dragonEntry = bubbles.entries.find { it.label == "dragon" }

        assertNotNull(dragonEntry, "'dragon' should appear in the packed bubble chart")
        assertEquals(3, dragonEntry!!.count, "'dragon' appears once per answer, total should be 3")
    }

    @Test
    fun TDD_textPackedBubbleStripsLeadingAndTrailingPunctuation() = runTest {
        // Punctuation attached to words should not prevent frequency matching ("running," == "running")
        val (nudgeId, followUpId) = createNudgeWithTextFollowUp()
        val now = Clock.System.now()
        repos.answerRepository.insert(Answer(id = "ans-punct-1", nudgeId = nudgeId, questionId = followUpId,
            value = "running, through the forest.", scheduledAt = now, answeredAt = now, isHidden = false))
        repos.answerRepository.insert(Answer(id = "ans-punct-2", nudgeId = nudgeId, questionId = followUpId,
            value = "running through darkness", scheduledAt = now, answeredAt = now, isHidden = false))

        val charts = getVisualizationData.execute(nudgeId, followUpId, Timeframe.ALL_TIME)
        val bubbles = charts.filterIsInstance<VisualizationData.PackedBubble>().first()
        val runningEntry = bubbles.entries.find { it.label == "running" }

        assertNotNull(runningEntry, "'running' should appear in the packed bubble chart")
        assertEquals(2, runningEntry!!.count, "'running,' and 'running' should both count as 'running'")
    }

    @Test
    fun TDD_textPackedBubbleTreatsEmojiAsWords() = runTest {
        // Emoji-only answers must still chart: each emoji counts as a whole word
        val (nudgeId, followUpId) = createNudgeWithTextFollowUp()
        val now = Clock.System.now()
        listOf("🐶", "🐶", "🐶", "🐱", "🐱").forEachIndexed { i, value ->
            repos.answerRepository.insert(Answer(id = "ans-emoji-$i", nudgeId = nudgeId, questionId = followUpId,
                value = value, scheduledAt = now, answeredAt = now, isHidden = false))
        }

        val charts = getVisualizationData.execute(nudgeId, followUpId, Timeframe.ALL_TIME)
        assertTrue(charts.isNotEmpty(), "Emoji-only answers should still produce a chart")
        val bubbles = charts.filterIsInstance<VisualizationData.PackedBubble>().first()

        val dog = bubbles.entries.find { it.label == "🐶" }
        val cat = bubbles.entries.find { it.label == "🐱" }
        assertNotNull(dog, "🐶 should be counted as a word")
        assertNotNull(cat, "🐱 should be counted as a word")
        assertEquals(3, dog!!.count, "🐶 appears in 3 answers")
        assertEquals(2, cat!!.count, "🐱 appears in 2 answers")
    }

    @Test
    fun TDD_textPackedBubbleSplitsAdjacentEmojiIntoSeparateWords() = runTest {
        // Distinct emoji written without a space are counted as separate words
        val (nudgeId, followUpId) = createNudgeWithTextFollowUp()
        val now = Clock.System.now()
        repos.answerRepository.insert(Answer(id = "ans-adj", nudgeId = nudgeId, questionId = followUpId,
            value = "🐶🐱🐶", scheduledAt = now, answeredAt = now, isHidden = false))

        val charts = getVisualizationData.execute(nudgeId, followUpId, Timeframe.ALL_TIME)
        val bubbles = charts.filterIsInstance<VisualizationData.PackedBubble>().first()

        assertEquals(2, bubbles.entries.find { it.label == "🐶" }?.count,
            "🐶 appears twice in the same answer and should be counted per-emoji")
        assertEquals(1, bubbles.entries.find { it.label == "🐱" }?.count,
            "🐱 should be its own word")
    }

    @Test
    fun TDD_textPackedBubbleKeepsBothEmojiAndText() = runTest {
        // A token mixing a word and an emoji contributes both
        val (nudgeId, followUpId) = createNudgeWithTextFollowUp()
        val now = Clock.System.now()
        repos.answerRepository.insert(Answer(id = "ans-mix", nudgeId = nudgeId, questionId = followUpId,
            value = "great dream ✨", scheduledAt = now, answeredAt = now, isHidden = false))

        val charts = getVisualizationData.execute(nudgeId, followUpId, Timeframe.ALL_TIME)
        val words = charts.filterIsInstance<VisualizationData.PackedBubble>().first().entries.map { it.label }

        assertTrue(words.contains("dream"), "text word kept")
        assertTrue(words.contains("✨"), "emoji kept as its own word")
    }

    @Test
    fun TDD_isSingleEmojiIdentifiesLoneEmojiForLargerBubbleLabels() {
        // The packed bubble chart draws a bubble's label twice as large when the entry is a single
        // emoji; isSingleEmoji decides that, matching the tokenizer's notion of one emoji.
        assertTrue(isSingleEmoji("🐶"), "one emoji")
        assertTrue(isSingleEmoji(" 🐱 "), "surrounding whitespace ignored")
        assertTrue(isSingleEmoji("👍🏽"), "emoji with a skin-tone modifier is still one emoji")
        assertTrue(isSingleEmoji("🇺🇸"), "a flag (regional-indicator pair) is one emoji")
        assertTrue(isSingleEmoji("👨‍👩‍👧"), "a ZWJ sequence is one emoji")

        assertTrue(!isSingleEmoji("🐶🐱"), "two emoji is not a single emoji")
        assertTrue(!isSingleEmoji("dream"), "a word is not an emoji")
        assertTrue(!isSingleEmoji("🐶 dog"), "emoji plus text is not a single emoji")
        assertTrue(!isSingleEmoji(""), "empty string is not an emoji")
    }

    @Test
    fun TDD_hiddenAnswersExcludedFromVisualizationAggregates() = runTest {
        // README "Editing Nudges": "Hidden rows no longer appear in the data visualization"
        val (nudgeId, questionId) = createNudgeAndRecordAnswer(QuestionType.YES_NO, answerValue = "YES")

        // Hide the answer
        val answers = repos.answerRepository.getAllByNudgeId(nudgeId)
        setAnswerHidden.execute(answers[0].id, isHidden = true)

        val charts = getVisualizationData.execute(nudgeId, questionId, Timeframe.ALL_TIME)
        val heatMap = charts.filterIsInstance<VisualizationData.CalendarHeatMap>().first()
        assertEquals(0.0, heatMap.dailyCounts.sumOf { it.value },
            "Hidden answer should not appear in visualization data")
    }

    private suspend fun createNudgeWithTextFollowUp(): Pair<String, String> {
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Did you have a dream?", QuestionType.YES_NO),
                followUpQuestions = listOf(
                    QuestionRequest(
                        text = "Describe the dream",
                        type = QuestionType.TEXT,
                        triggerAnswerValue = "YES",
                        triggerOperator = TriggerOperator.EQ
                    )
                ),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val followUpId = repos.questionRepository.getByNudgeId(result.nudgeId)
            .first { !it.isMainQuestion }.id
        return result.nudgeId to followUpId
    }

    // --- Preloaded source / in-memory rendering (scroll responsiveness refactor) ---

    /** Creates a NUMBER nudge and records one answer per (daysAgo, value) entry. */
    private suspend fun numberNudgeWithAnswers(answers: List<Pair<Int, String>>): Pair<String, String> {
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("How many?", QuestionType.NUMBER),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val nudgeId = result.nudgeId
        val questionId = repos.questionRepository.getByNudgeId(nudgeId).first { it.isMainQuestion }.id
        val now = Clock.System.now()
        answers.forEachIndexed { index, (daysAgo, value) ->
            repos.answerRepository.insert(
                Answer(
                    id = "ans-$index",
                    nudgeId = nudgeId,
                    questionId = questionId,
                    value = value,
                    scheduledAt = now - daysAgo.days,
                    answeredAt = now - daysAgo.days,
                    isHidden = false
                )
            )
        }
        return nudgeId to questionId
    }

    @Test
    fun TDD_numberLineGraphYAxisIsZeroToGlobalMaxRegardlessOfWindow() = runTest {
        // Scrubbing must not rescale the Y axis: NUMBER is fixed to 0..(rounded global max) over all
        // data, so a window of small values uses the same axis as the window holding the peak.
        val (nudgeId, questionId) = numberNudgeWithAnswers(listOf(2 to "5", 40 to "50"))
        val source = getVisualizationData.loadSource(nudgeId, questionId)
        assertNotNull(source)
        val now = Clock.System.now()

        fun lineGraph(offsetDays: Int) = getVisualizationData
            .build(source, Timeframe.WEEKLY, periodOffsetDays = offsetDays, now = now)
            .filterIsInstance<VisualizationData.LineGraph>().first()

        val currentWeek = lineGraph(0) // only sees the value-5 point
        assertEquals(0.0, currentWeek.yMin, "NUMBER axis anchors at zero")
        assertEquals(50.0, currentWeek.yMax, "axis tops out at the rounded global max, not the window's max")

        val weekWithPeak = lineGraph(7)
        assertEquals(currentWeek.yMin, weekWithPeak.yMin, "axis is identical across windows")
        assertEquals(currentWeek.yMax, weekWithPeak.yMax, "axis is identical across windows")
    }

    @Test
    fun TDD_numberLineGraphYAxisHugsTheMax_withoutWastefulHeadroom() = runTest {
        // The locked axis tops out just above the data max via a fine "nice" ladder, not ~2× it: a
        // max of 11 yields 12, where a coarse 1/2/5/10 ladder would have jumped to 20.
        val (nudgeId, questionId) = numberNudgeWithAnswers(listOf(0 to "11"))
        val source = getVisualizationData.loadSource(nudgeId, questionId)
        assertNotNull(source)

        val line = getVisualizationData.build(source, Timeframe.WEEKLY, now = Clock.System.now())
            .filterIsInstance<VisualizationData.LineGraph>().first()
        assertEquals(0.0, line.yMin, "NUMBER axis anchors at zero")
        assertEquals(12.0, line.yMax, "axis hugs the max (11 → 12), not ~2× it")
    }

    @Test
    fun buildFromLoadedSourceMatchesExecuteAcrossTimeframes() = runTest {
        // Refactor safety: rendering from a cached, preloaded source must produce exactly what the
        // database-reading execute() path produces, for every timeframe.
        val (nudgeId, questionId) = numberNudgeWithAnswers(
            listOf(0 to "5", 3 to "8", 10 to "2", 40 to "9")
        )
        val source = getVisualizationData.loadSource(nudgeId, questionId)
        assertNotNull(source)

        val now = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        for (timeframe in Timeframe.values()) {
            assertEquals(
                getVisualizationData.execute(nudgeId, questionId, timeframe, now = now, timeZone = tz),
                getVisualizationData.build(source, timeframe, now = now, timeZone = tz),
                "build() from a cached source should match execute() for $timeframe"
            )
        }
    }

    @Test
    fun buildReadsTheSnapshotNotTheDatabase() = runTest {
        // The performance fix hinges on this: once a source is loaded, building charts must not touch
        // the database, so scrubbing incurs no storage reads. An answer inserted after loadSource must
        // therefore not appear in charts built from the earlier snapshot, while execute() (which
        // re-reads the database) does see it.
        val (nudgeId, questionId) = numberNudgeWithAnswers(listOf(0 to "5"))
        val source = getVisualizationData.loadSource(nudgeId, questionId)
        assertNotNull(source)

        val now = Clock.System.now()
        repos.answerRepository.insert(
            Answer(
                id = "ans-inserted-after-load",
                nudgeId = nudgeId,
                questionId = questionId,
                value = "9",
                scheduledAt = now,
                answeredAt = now,
                isHidden = false
            )
        )

        val fromSnapshot = getVisualizationData.build(source, Timeframe.ALL_TIME, now = now)
            .filterIsInstance<VisualizationData.LineGraph>().first()
        val fromDatabase = getVisualizationData.execute(nudgeId, questionId, Timeframe.ALL_TIME, now = now)
            .filterIsInstance<VisualizationData.LineGraph>().first()

        assertEquals(1, fromSnapshot.points.size, "snapshot build must ignore answers inserted after load")
        assertEquals(2, fromDatabase.points.size, "execute re-reads the database and sees the new answer")
    }

    @Test
    fun buildReaggregatesWindowsFromOneSource() = runTest {
        // Scrubbing shifts the window against one cached source. Different offsets must yield different
        // window contents without reloading, proving the window math runs on the in-memory snapshot.
        val (nudgeId, questionId) = numberNudgeWithAnswers(
            listOf(2 to "5", 9 to "8")  // one answer this week, one the week before
        )
        val source = getVisualizationData.loadSource(nudgeId, questionId)
        assertNotNull(source)
        val now = Clock.System.now()

        fun weeklyPoints(offsetDays: Int) = getVisualizationData
            .build(source, Timeframe.WEEKLY, periodOffsetDays = offsetDays, now = now)
            .filterIsInstance<VisualizationData.LineGraph>().first().points

        assertEquals(1, weeklyPoints(0).size, "current week sees only the recent answer")
        assertEquals(5.0, weeklyPoints(0).first().value)
        assertEquals(1, weeklyPoints(7).size, "shifting a week back sees only the older answer")
        assertEquals(8.0, weeklyPoints(7).first().value)
    }
}
