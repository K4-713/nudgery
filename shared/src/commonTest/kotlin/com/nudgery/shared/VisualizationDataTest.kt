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
import com.nudgery.shared.util.FakeNotificationScheduler
import com.nudgery.shared.util.TestRepositories
import com.nudgery.shared.util.createTestRepositories
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
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
}
