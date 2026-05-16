package com.nudgery.shared

import com.nudgery.shared.model.Answer
import com.nudgery.shared.model.ExportFormat
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.usecase.CreateNudgeRequest
import com.nudgery.shared.usecase.CreateNudgeResult
import com.nudgery.shared.usecase.CreateNudgeUseCase
import com.nudgery.shared.usecase.ExportAnswersUseCase
import com.nudgery.shared.usecase.QuestionRequest
import com.nudgery.shared.usecase.ScheduleRequest
import com.nudgery.shared.util.FakeNotificationScheduler
import com.nudgery.shared.util.TestRepositories
import com.nudgery.shared.util.createTestRepositories
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DataExportTest {

    private lateinit var repos: TestRepositories
    private lateinit var createNudge: CreateNudgeUseCase
    private lateinit var exportAnswers: ExportAnswersUseCase

    @BeforeTest
    fun setup() {
        repos = createTestRepositories()
        createNudge = CreateNudgeUseCase(
            repos.nudgeRepository, repos.questionRepository, repos.questionOptionRepository,
            repos.scheduleRepository, FakeNotificationScheduler()
        )
        exportAnswers = ExportAnswersUseCase(
            repos.nudgeRepository, repos.questionRepository,
            repos.questionOptionRepository, repos.answerRepository
        )
    }

    private fun dailySchedule() = ScheduleRequest(
        type = ScheduleType.DAILY,
        timeOfDay = LocalTime(12, 0),
        activeDaysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
    )

    private suspend fun createNudgeWithAnswer(
        questionText: String = "Did you sleep well?",
        questionType: QuestionType = QuestionType.YES_NO,
        options: List<String> = emptyList(),
        answerValue: String = "YES",
        nudgeName: String = "Sleep Tracker"
    ): Triple<String, String, String> {
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest(questionText, questionType, options),
                schedule = dailySchedule(),
                name = nudgeName
            )
        ) as CreateNudgeResult.Success

        val nudgeId = result.nudgeId
        val questionId = repos.questionRepository.getByNudgeId(nudgeId).first { it.isMainQuestion }.id
        val answerId = "answer-${nudgeId}"

        repos.answerRepository.insert(
            Answer(
                id = answerId,
                nudgeId = nudgeId,
                questionId = questionId,
                value = answerValue,
                recordedAt = Clock.System.now(),
                isHidden = false
            )
        )

        return Triple(nudgeId, questionId, answerId)
    }

    @Test
    fun TDD_exportProducesCsvFile() = runTest {
        // README "Viewing Nudges": "a raw table of the answer data which can be exported
        //   to a CSV/TSV file"
        val (nudgeId, _, _) = createNudgeWithAnswer()
        val csv = exportAnswers.execute(nudgeId, ExportFormat.CSV)

        assertTrue(csv.isNotBlank())
        assertTrue(csv.contains(","), "CSV output should contain commas as delimiters")
    }

    @Test
    fun TDD_exportProducesTsvFile() = runTest {
        // README "Viewing Nudges": "...to a CSV/TSV file"
        val (nudgeId, _, _) = createNudgeWithAnswer()
        val tsv = exportAnswers.execute(nudgeId, ExportFormat.TSV)

        assertTrue(tsv.isNotBlank())
        assertTrue(tsv.contains("\t"), "TSV output should contain tabs as delimiters")
    }

    @Test
    fun TDD_exportRowContainsAnswerValue() = runTest {
        // ARCHITECTURE.md "Export produces one row per Answer, joined with its Question,
        //   Nudge, and any relevant QuestionOption text"
        val (nudgeId, _, _) = createNudgeWithAnswer(answerValue = "YES")
        val csv = exportAnswers.execute(nudgeId, ExportFormat.CSV)

        val dataRows = csv.lines().filter { it.isNotBlank() }.drop(1) // skip header
        assertEquals(1, dataRows.size)
        assertTrue(dataRows[0].contains("YES"))
    }

    @Test
    fun TDD_exportRowContainsQuestionText() = runTest {
        // ARCHITECTURE.md "...joined with its Question..."
        val (nudgeId, _, _) = createNudgeWithAnswer(questionText = "Did you drink water?")
        val csv = exportAnswers.execute(nudgeId, ExportFormat.CSV)

        assertTrue(csv.contains("Did you drink water?"))
    }

    @Test
    fun TDD_exportRowContainsNudgeName() = runTest {
        // ARCHITECTURE.md "...joined with its...Nudge..."
        val (nudgeId, _, _) = createNudgeWithAnswer(nudgeName = "Hydration Tracker")
        val csv = exportAnswers.execute(nudgeId, ExportFormat.CSV)

        assertTrue(csv.contains("Hydration Tracker"))
    }

    @Test
    fun TDD_exportRowContainsOptionTextForOptionTypeAnswers() = runTest {
        // ARCHITECTURE.md "...and any relevant QuestionOption text"
        val options = listOf("Very Good", "Good", "Poor")
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Sleep quality?", QuestionType.OPTION_SINGLE, options),
                schedule = dailySchedule(),
                name = "Sleep Quality"
            )
        ) as CreateNudgeResult.Success

        val nudgeId = result.nudgeId
        val questionId = repos.questionRepository.getByNudgeId(nudgeId).first { it.isMainQuestion }.id
        val optionId = repos.questionOptionRepository.getByQuestionId(questionId)
            .first { it.text == "Good" }.id

        repos.answerRepository.insert(
            Answer(
                id = "answer-opt",
                nudgeId = nudgeId,
                questionId = questionId,
                value = optionId,
                recordedAt = Clock.System.now(),
                isHidden = false
            )
        )

        val csv = exportAnswers.execute(nudgeId, ExportFormat.CSV)
        assertTrue(csv.contains("Good"), "Export should include the resolved option text")
    }

    @Test
    fun TDD_exportRowContainsAnswerTimestamp() = runTest {
        // ARCHITECTURE.md Answer.recordedAt — timestamp should be included in the export record
        val (nudgeId, _, _) = createNudgeWithAnswer()
        val csv = exportAnswers.execute(nudgeId, ExportFormat.CSV)

        val dataRows = csv.lines().filter { it.isNotBlank() }.drop(1)
        // The timestamp is an ISO 8601 Instant string — verify it looks like one
        assertTrue(dataRows[0].contains("T"), "Export row should contain an ISO timestamp")
        assertTrue(dataRows[0].contains("Z") || dataRows[0].contains("+"), "Timestamp should include timezone")
    }
}
