// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared

import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.model.TriggerOperator
import com.nudgery.shared.usecase.CreateNudgeRequest
import com.nudgery.shared.usecase.CreateNudgeResult
import com.nudgery.shared.usecase.CreateNudgeUseCase
import com.nudgery.shared.usecase.QuestionRequest
import com.nudgery.shared.usecase.ScheduleRequest
import com.nudgery.shared.util.FakeNotificationScheduler
import com.nudgery.shared.util.TestRepositories
import com.nudgery.shared.util.createTestRepositories
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class FollowUpTriggerTest {

    private lateinit var repos: TestRepositories
    private lateinit var createNudge: CreateNudgeUseCase

    @BeforeTest
    fun setup() {
        repos = createTestRepositories()
        createNudge = CreateNudgeUseCase(
            repos.nudgeRepository,
            repos.questionRepository,
            repos.questionOptionRepository,
            repos.scheduleRepository,
            FakeNotificationScheduler()
        )
    }

    private fun dailySchedule() = ScheduleRequest(
        type = ScheduleType.DAILY,
        timeOfDay = LocalTime(12, 0),
        activeDaysOfWeek = DayOfWeek.entries.toSet()
    )

    @Test
    fun TDD_optionSingleTriggerIndexResolvedToOptionId() = runTest {
        // The wizard stores the option index ("0", "1") as the trigger value during creation.
        // CreateNudgeUseCase must resolve it to the actual option ID before persisting.
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest(
                    text = "How do you feel?",
                    type = QuestionType.OPTION_SINGLE,
                    options = listOf("Great", "OK", "Bad")
                ),
                followUpQuestions = listOf(
                    QuestionRequest(
                        text = "What went wrong?",
                        type = QuestionType.TEXT,
                        triggerAnswerValue = "2",  // index of "Bad"
                        triggerOperator = TriggerOperator.EQ
                    )
                ),
                schedule = dailySchedule()
            )
        )
        assertIs<CreateNudgeResult.Success>(result)

        val questions = repos.questionRepository.getByNudgeId(result.nudgeId)
        val mainQuestion = questions.first { it.isMainQuestion }
        val followUp = questions.first { !it.isMainQuestion }

        val options = repos.questionOptionRepository.getByQuestionId(mainQuestion.id)
            .sortedBy { it.orderIndex }

        val badOptionId = options[2].id
        assertEquals(
            badOptionId,
            followUp.triggerAnswerValue,
            "Trigger value should be resolved from index '2' to the option ID of 'Bad'"
        )
        assertEquals(TriggerOperator.EQ, followUp.triggerOperator)
    }

    @Test
    fun TDD_optionMultiTriggerIndexResolvedToOptionIdWithContains() = runTest {
        // OPTION_MULTI triggers use CONTAINS so the follow-up fires when the option
        // appears anywhere in the comma-separated multi-select answer.
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest(
                    text = "Which symptoms do you have?",
                    type = QuestionType.OPTION_MULTI,
                    options = listOf("Fatigue", "Headache", "Nausea")
                ),
                followUpQuestions = listOf(
                    QuestionRequest(
                        text = "How severe is the nausea?",
                        type = QuestionType.NUMBER,
                        triggerAnswerValue = "2",  // index of "Nausea"
                        triggerOperator = TriggerOperator.CONTAINS
                    )
                ),
                schedule = dailySchedule()
            )
        )
        assertIs<CreateNudgeResult.Success>(result)

        val questions = repos.questionRepository.getByNudgeId(result.nudgeId)
        val mainQuestion = questions.first { it.isMainQuestion }
        val followUp = questions.first { !it.isMainQuestion }

        val options = repos.questionOptionRepository.getByQuestionId(mainQuestion.id)
            .sortedBy { it.orderIndex }

        val nauseaOptionId = options[2].id
        assertEquals(
            nauseaOptionId,
            followUp.triggerAnswerValue,
            "Trigger value should be resolved to the option ID of 'Nausea'"
        )
        assertEquals(TriggerOperator.CONTAINS, followUp.triggerOperator)
    }

    @Test
    fun TDD_yesNoTriggerStoredDirectlyWithoutIndexResolution() = runTest {
        // YES/NO trigger values ("YES", "NO") are not indices and must be stored as-is.
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Did you exercise?", QuestionType.YES_NO),
                followUpQuestions = listOf(
                    QuestionRequest(
                        text = "What did you do?",
                        type = QuestionType.TEXT,
                        triggerAnswerValue = "YES",
                        triggerOperator = TriggerOperator.EQ
                    )
                ),
                schedule = dailySchedule()
            )
        )
        assertIs<CreateNudgeResult.Success>(result)

        val followUp = repos.questionRepository.getByNudgeId(result.nudgeId)
            .first { !it.isMainQuestion }

        assertEquals("YES", followUp.triggerAnswerValue)
        assertEquals(TriggerOperator.EQ, followUp.triggerOperator)
    }

    @Test
    fun TDD_numberTriggerStoredDirectlyWithOperator() = runTest {
        // Numeric trigger values are stored as-is with their operator.
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Rate your pain (0–10)", QuestionType.NUMBER),
                followUpQuestions = listOf(
                    QuestionRequest(
                        text = "Describe the pain",
                        type = QuestionType.TEXT,
                        triggerAnswerValue = "6",
                        triggerOperator = TriggerOperator.GTE
                    )
                ),
                schedule = dailySchedule()
            )
        )
        assertIs<CreateNudgeResult.Success>(result)

        val followUp = repos.questionRepository.getByNudgeId(result.nudgeId)
            .first { !it.isMainQuestion }

        assertEquals("6", followUp.triggerAnswerValue)
        assertEquals(TriggerOperator.GTE, followUp.triggerOperator)
    }

    @Test
    fun TDD_outOfRangeOptionIndexFallsBackToRawValue() = runTest {
        // If the trigger index is out of bounds, the raw string is preserved rather than crashing.
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest(
                    text = "Pick one",
                    type = QuestionType.OPTION_SINGLE,
                    options = listOf("A", "B")
                ),
                followUpQuestions = listOf(
                    QuestionRequest(
                        text = "Follow up",
                        type = QuestionType.TEXT,
                        triggerAnswerValue = "99",
                        triggerOperator = TriggerOperator.EQ
                    )
                ),
                schedule = dailySchedule()
            )
        )
        assertIs<CreateNudgeResult.Success>(result)

        val followUp = repos.questionRepository.getByNudgeId(result.nudgeId)
            .first { !it.isMainQuestion }

        assertNotNull(followUp.triggerAnswerValue, "Trigger value should be preserved when index is out of range")
    }

    // README: "Follow-up questions can be any of the main question types, plus a freeform Text type."

    @Test
    fun TDD_followUpTextTypeIsPersistedCorrectly() = runTest {
        // A follow-up with type TEXT must be stored with type=TEXT, not the default YES_NO.
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Did you exercise?", QuestionType.YES_NO),
                followUpQuestions = listOf(
                    QuestionRequest(
                        text = "Describe your workout",
                        type = QuestionType.TEXT,
                        triggerAnswerValue = "YES",
                        triggerOperator = TriggerOperator.EQ
                    )
                ),
                schedule = dailySchedule()
            )
        )
        assertIs<CreateNudgeResult.Success>(result)

        val followUp = repos.questionRepository.getByNudgeId(result.nudgeId)
            .first { !it.isMainQuestion }

        assertEquals(QuestionType.TEXT, followUp.type)
    }

    @Test
    fun TDD_followUpOptionSingleTypeWithOptionsIsPersistedCorrectly() = runTest {
        // A follow-up can itself be a choice question with its own option list.
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Did you exercise?", QuestionType.YES_NO),
                followUpQuestions = listOf(
                    QuestionRequest(
                        text = "What type of exercise?",
                        type = QuestionType.OPTION_SINGLE,
                        options = listOf("Cardio", "Strength", "Flexibility"),
                        triggerAnswerValue = "YES",
                        triggerOperator = TriggerOperator.EQ
                    )
                ),
                schedule = dailySchedule()
            )
        )
        assertIs<CreateNudgeResult.Success>(result)

        val questions = repos.questionRepository.getByNudgeId(result.nudgeId)
        val followUp = questions.first { !it.isMainQuestion }

        assertEquals(QuestionType.OPTION_SINGLE, followUp.type)

        val options = repos.questionOptionRepository.getByQuestionId(followUp.id)
            .sortedBy { it.orderIndex }
        assertEquals(listOf("Cardio", "Strength", "Flexibility"), options.map { it.text })
    }
}
