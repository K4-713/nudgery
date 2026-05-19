package com.nudgery.shared

import com.nudgery.shared.model.Answer
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.model.Timeframe
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
        // README "Viewing Nudges": "YES_NO | Calendar heat map, column chart"
        val (nudgeId, questionId) = createNudgeAndRecordAnswer(QuestionType.YES_NO, answerValue = "YES")
        val charts = getVisualizationData.execute(nudgeId, questionId, Timeframe.ALL_TIME)

        assertTrue(charts.any { it is VisualizationData.CalendarHeatMap },
            "YES_NO should provide a CalendarHeatMap")
    }

    @Test
    fun TDD_yesNoQuestionProvidesColumnChartData() = runTest {
        // README "Viewing Nudges": "YES_NO | Calendar heat map, column chart"
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

    // --- OPTION_SINGLE ---

    @Test
    fun TDD_optionSingleQuestionProvidesBarChartData() = runTest {
        // README "Viewing Nudges": "OPTION_SINGLE | Bar chart, column chart, tag cloud"
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
        // README "Viewing Nudges": "OPTION_SINGLE | Bar chart, column chart, tag cloud"
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
    fun TDD_optionSingleQuestionProvidesTagCloudData() = runTest {
        // README "Viewing Nudges": "OPTION_SINGLE | Bar chart, column chart, tag cloud"
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
        assertTrue(charts.any { it is VisualizationData.TagCloud },
            "OPTION_SINGLE should provide a TagCloud")
    }

    // --- OPTION_MULTI ---

    @Test
    fun TDD_optionMultiQuestionProvidesBarChartData() = runTest {
        // README "Viewing Nudges": "OPTION_MULTI | Bar chart, tag cloud"
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
    fun TDD_optionMultiQuestionProvidesTagCloudData() = runTest {
        // README "Viewing Nudges": "OPTION_MULTI | Bar chart, tag cloud"
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
        assertTrue(charts.any { it is VisualizationData.TagCloud },
            "OPTION_MULTI should provide a TagCloud")
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
}
