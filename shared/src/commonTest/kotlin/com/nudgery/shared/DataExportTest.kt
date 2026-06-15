// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared

import com.nudgery.shared.model.Answer
import com.nudgery.shared.model.ExportFormat
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.model.TriggerOperator
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
            repos.questionOptionRepository, repos.answerRepository, repos.scheduleRepository
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
                scheduledAt = Clock.System.now(),
                answeredAt = Clock.System.now(),
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
                scheduledAt = Clock.System.now(),
                answeredAt = Clock.System.now(),
                isHidden = false
            )
        )

        val csv = exportAnswers.execute(nudgeId, ExportFormat.CSV)
        assertTrue(csv.contains("Good"), "Export should include the resolved option text")
    }

    // ── JSON full-backup export ──────────────────────────────────────────────────────────────────

    @Test
    fun TDD_jsonExport_includesNudgeName() = runTest {
        // JSON backup must contain the nudge name so it can be reconstructed on import
        val (nudgeId, _, _) = createNudgeWithAnswer(nudgeName = "Mood Tracker")
        val json = exportAnswers.execute(nudgeId, ExportFormat.JSON)

        assertTrue(json.contains("Mood Tracker"), "JSON backup should include the nudge name")
    }

    @Test
    fun TDD_jsonExport_includesOneYesPerDayOnlyWhenOn() = runTest {
        // ED-17: the One Yes Per Day flag round-trips through JSON backups, emitted only when on.
        val on = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Headache?", QuestionType.YES_NO, collapsePerDay = true),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val onJson = exportAnswers.execute(on.nudgeId, ExportFormat.JSON)
        assertTrue(onJson.contains("\"collapsePerDay\": true"), "backup records the flag when on")

        val off = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Headache?", QuestionType.YES_NO),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success
        val offJson = exportAnswers.execute(off.nudgeId, ExportFormat.JSON)
        assertFalse(offJson.contains("collapsePerDay"), "flag omitted when off")
    }

    @Test
    fun TDD_jsonExport_includesScheduleType() = runTest {
        // Schedule type is needed to reconstruct the notification cadence
        val (nudgeId, _, _) = createNudgeWithAnswer()
        val json = exportAnswers.execute(nudgeId, ExportFormat.JSON)

        assertTrue(json.contains("DAILY"), "JSON backup should include the schedule type")
    }

    @Test
    fun TDD_jsonExport_includesScheduleTimeOfDay() = runTest {
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("How are you?", QuestionType.YES_NO),
                schedule = ScheduleRequest(
                    type = ScheduleType.DAILY,
                    timeOfDay = LocalTime(14, 30),
                    activeDaysOfWeek = setOf(DayOfWeek.MONDAY)
                ),
                name = "Afternoon Check"
            )
        ) as CreateNudgeResult.Success
        val json = exportAnswers.execute(result.nudgeId, ExportFormat.JSON)

        assertTrue(json.contains("14:30"), "JSON backup should include the schedule time of day")
    }

    @Test
    fun TDD_jsonExport_includesScheduleActiveDays() = runTest {
        val (nudgeId, _, _) = createNudgeWithAnswer()
        val json = exportAnswers.execute(nudgeId, ExportFormat.JSON)

        assertTrue(json.contains("MONDAY"), "JSON backup should include active days of week")
        assertTrue(json.contains("WEDNESDAY"), "JSON backup should include all active days")
    }

    @Test
    fun TDD_jsonExport_includesQuestionText() = runTest {
        val (nudgeId, _, _) = createNudgeWithAnswer(questionText = "Did you meditate?")
        val json = exportAnswers.execute(nudgeId, ExportFormat.JSON)

        assertTrue(json.contains("Did you meditate?"), "JSON backup should include question text")
    }

    @Test
    fun TDD_jsonExport_includesQuestionType() = runTest {
        val (nudgeId, _, _) = createNudgeWithAnswer(questionType = QuestionType.YES_NO)
        val json = exportAnswers.execute(nudgeId, ExportFormat.JSON)

        assertTrue(json.contains("YES_NO"), "JSON backup should include question type")
    }

    @Test
    fun TDD_jsonExport_includesScaleBoundsForScaleQuestions() = runTest {
        // SCALE questions carry a user-defined range; both bounds must appear in the backup
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Rate your energy?", QuestionType.SCALE, scaleMin = 1, scaleMax = 5),
                schedule = dailySchedule(),
                name = "Energy"
            )
        ) as CreateNudgeResult.Success
        val json = exportAnswers.execute(result.nudgeId, ExportFormat.JSON)

        assertTrue(json.contains("\"scaleMin\""), "JSON backup should include scaleMin field")
        assertTrue(json.contains("\"scaleMax\""), "JSON backup should include scaleMax field")
        assertTrue(json.contains("1"), "JSON backup should include scaleMin value")
        assertTrue(json.contains("5"), "JSON backup should include scaleMax value")
    }

    @Test
    fun TDD_jsonExport_includesOptionTextsForOptionQuestions() = runTest {
        // Option texts (not IDs) must be present so options can be recreated on import
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest(
                    "How did you sleep?", QuestionType.OPTION_SINGLE,
                    options = listOf("Great", "Okay", "Poorly")
                ),
                schedule = dailySchedule(),
                name = "Sleep"
            )
        ) as CreateNudgeResult.Success
        val json = exportAnswers.execute(result.nudgeId, ExportFormat.JSON)

        assertTrue(json.contains("Great"), "JSON backup should include option texts")
        assertTrue(json.contains("Okay"), "JSON backup should include all options")
        assertTrue(json.contains("Poorly"), "JSON backup should include all options")
    }

    @Test
    fun TDD_jsonExport_includesFollowUpTriggerCondition() = runTest {
        // Follow-up trigger operators and values must be present for conditional logic to reconstruct
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Did you exercise?", QuestionType.YES_NO),
                followUpQuestions = listOf(
                    QuestionRequest(
                        text = "For how long?",
                        type = QuestionType.SCALE,
                        scaleMin = 0, scaleMax = 120,
                        triggerAnswerValue = "YES",
                        triggerOperator = TriggerOperator.EQ
                    )
                ),
                schedule = dailySchedule(),
                name = "Exercise"
            )
        ) as CreateNudgeResult.Success
        val json = exportAnswers.execute(result.nudgeId, ExportFormat.JSON)

        assertTrue(json.contains("triggerOperator"), "JSON backup should include trigger operator field")
        assertTrue(json.contains("EQ"), "JSON backup should include trigger operator value")
        assertTrue(json.contains("triggerAnswerValue"), "JSON backup should include trigger value field")
        assertTrue(json.contains("YES"), "JSON backup should include trigger answer value")
    }

    @Test
    fun TDD_jsonExport_answersReferencedByQuestionOrderIndex() = runTest {
        // Answers must reference questions by orderIndex (stable across re-import), not by UUID
        val (nudgeId, _, _) = createNudgeWithAnswer()
        val json = exportAnswers.execute(nudgeId, ExportFormat.JSON)

        assertTrue(json.contains("questionOrderIndex"), "JSON answers should reference question by orderIndex")
    }

    @Test
    fun TDD_jsonExport_answersIncludeResolvedOptionText() = runTest {
        // Option answers store UUIDs internally; the backup must store the human-readable text
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest(
                    "Mood?", QuestionType.OPTION_SINGLE, options = listOf("Happy", "Neutral", "Sad")
                ),
                schedule = dailySchedule(),
                name = "Mood"
            )
        ) as CreateNudgeResult.Success
        val nudgeId = result.nudgeId
        val questionId = repos.questionRepository.getByNudgeId(nudgeId).first { it.isMainQuestion }.id
        val optionId = repos.questionOptionRepository.getByQuestionId(questionId).first { it.text == "Happy" }.id

        repos.answerRepository.insert(
            Answer(
                id = "ans-opt", nudgeId = nudgeId, questionId = questionId,
                value = optionId, scheduledAt = Clock.System.now(),
                answeredAt = Clock.System.now(), isHidden = false
            )
        )
        val json = exportAnswers.execute(nudgeId, ExportFormat.JSON)

        assertTrue(json.contains("Happy"), "JSON export should resolve option UUID to option text in answers")
        assertFalse(json.contains(optionId), "JSON export should not expose internal option UUIDs in answers")
    }

    @Test
    fun TDD_jsonExport_answersIncludeTimestamps() = runTest {
        val (nudgeId, _, _) = createNudgeWithAnswer()
        val json = exportAnswers.execute(nudgeId, ExportFormat.JSON)

        assertTrue(json.contains("scheduledAt"), "JSON export should include scheduledAt in answers")
        assertTrue(json.contains("answeredAt"), "JSON export should include answeredAt in answers")
    }

    @Test
    fun TDD_exportRowContainsAnswerTimestamp() = runTest {
        // ARCHITECTURE.md Answer.scheduledAt / answeredAt — both timestamps included in export record
        val (nudgeId, _, _) = createNudgeWithAnswer()
        val csv = exportAnswers.execute(nudgeId, ExportFormat.CSV)

        val dataRows = csv.lines().filter { it.isNotBlank() }.drop(1)
        // The timestamp is an ISO 8601 Instant string — verify it looks like one
        assertTrue(dataRows[0].contains("T"), "Export row should contain an ISO timestamp")
        assertTrue(dataRows[0].contains("Z") || dataRows[0].contains("+"), "Timestamp should include timezone")
    }

    @Test
    fun TDD_setupOnlyExportContainsQuestionsButNoAnswers() = runTest {
        // ENGINEERING_DECISIONS.md ED-29: setup-only export for sharing includes questions and
        // schedule but no answer data.
        val (nudgeId, _, _) = createNudgeWithAnswer()
        val json = exportAnswers.executeSetupOnly(nudgeId)

        assertTrue(json.contains("Did you sleep well?"), "Should contain question text")
        assertTrue(json.contains("Sleep Tracker"), "Should contain nudge name")
        assertTrue(json.contains("DAILY"), "Should contain schedule")
        assertTrue(json.contains("\"answers\""), "Should contain answers key")
        // The answers array should be empty (no user data shared)
        assertFalse(json.contains("\"value\""), "Answers array should be empty")
    }
}
