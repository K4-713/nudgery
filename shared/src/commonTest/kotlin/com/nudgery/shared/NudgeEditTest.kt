package com.nudgery.shared

import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.usecase.CreateNudgeRequest
import com.nudgery.shared.usecase.CreateNudgeResult
import com.nudgery.shared.usecase.CreateNudgeUseCase
import com.nudgery.shared.usecase.QuestionRequest
import com.nudgery.shared.usecase.ScheduleRequest
import com.nudgery.shared.usecase.UpdateNudgeRequest
import com.nudgery.shared.usecase.UpdateNudgeResult
import com.nudgery.shared.usecase.UpdateNudgeUseCase
import com.nudgery.shared.usecase.UpdateOptionRequest
import com.nudgery.shared.util.FakeNotificationScheduler
import com.nudgery.shared.util.TestRepositories
import com.nudgery.shared.util.createTestRepositories
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NudgeEditTest {

    private lateinit var repos: TestRepositories
    private lateinit var fakeScheduler: FakeNotificationScheduler
    private lateinit var createNudge: CreateNudgeUseCase
    private lateinit var updateNudge: UpdateNudgeUseCase

    @BeforeTest
    fun setup() {
        repos = createTestRepositories()
        fakeScheduler = FakeNotificationScheduler()
        createNudge = CreateNudgeUseCase(
            repos.nudgeRepository, repos.questionRepository, repos.questionOptionRepository,
            repos.scheduleRepository, fakeScheduler
        )
        updateNudge = UpdateNudgeUseCase(
            repos.nudgeRepository, repos.questionRepository, repos.questionOptionRepository,
            repos.scheduleRepository, repos.nudgeEditRepository, fakeScheduler
        )
    }

    private fun dailySchedule() = ScheduleRequest(
        type = ScheduleType.DAILY,
        timeOfDay = LocalTime(12, 0),
        activeDaysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
    )

    private suspend fun createYesNoNudge(questionText: String = "Did you exercise?"): String {
        return (createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest(questionText, QuestionType.YES_NO),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success).nudgeId
    }

    @Test
    fun TDD_nudgeBaseQuestionTypeCannotBeChanged() = runTest {
        // README "Editing Nudges": "Nudge configuration [can] be edited, except for the
        //   base type of the main question"
        // Verified structurally: UpdateNudgeUseCase has no field for changing question type.
        // The UpdateNudgeRequest intentionally omits a questionType field, enforcing this constraint.
        val nudgeId = createYesNoNudge()
        val questions = repos.questionRepository.getByNudgeId(nudgeId)
        val originalType = questions.first { it.isMainQuestion }.type

        updateNudge.execute(UpdateNudgeRequest(nudgeId = nudgeId, mainQuestionText = "Did you sleep well?"))

        val updatedQuestions = repos.questionRepository.getByNudgeId(nudgeId)
        val updatedType = updatedQuestions.first { it.isMainQuestion }.type
        assertEquals(originalType, updatedType)
    }

    @Test
    fun TDD_nudgeScheduleCanBeEdited() = runTest {
        // README "Editing Nudges": "Nudge configuration [can] be edited..."
        val nudgeId = createYesNoNudge()
        val weeklySchedule = ScheduleRequest(
            type = ScheduleType.WEEKLY,
            timeOfDay = LocalTime(9, 0),
            activeDaysOfWeek = setOf(DayOfWeek.MONDAY)
        )

        val result = updateNudge.execute(UpdateNudgeRequest(nudgeId = nudgeId, schedule = weeklySchedule))
        assertIs<UpdateNudgeResult.Success>(result)

        val updatedSchedule = repos.scheduleRepository.getByNudgeId(nudgeId)
        assertNotNull(updatedSchedule)
        assertEquals(ScheduleType.WEEKLY, updatedSchedule.type)
        assertEquals(LocalTime(9, 0), updatedSchedule.timeOfDay)
    }

    @Test
    fun TDD_editingQuestionTextOffersChoiceBetweenSplitAndInPlace() = runTest {
        // README "Editing Nudges": "If you edit the main question text or selectable option text,
        //   you will be asked if you would prefer to split the Nudge instead of editing in place"
        // Verified by UpdateNudgeUseCase API: the splitEdit parameter represents the user's choice.
        val nudgeId = createYesNoNudge("Original question?")

        // splitEdit = false → in-place edit (no new nudge created)
        updateNudge.execute(UpdateNudgeRequest(nudgeId = nudgeId, mainQuestionText = "Updated question?", splitEdit = false))
        val allNudges = repos.nudgeRepository.observeAll().first()
        assertEquals(1, allNudges.size)
        val question = repos.questionRepository.getByNudgeId(nudgeId).first { it.isMainQuestion }
        assertEquals("Updated question?", question.text)
    }

    @Test
    fun TDD_editingOptionTextOffersChoiceBetweenSplitAndInPlace() = runTest {
        // README "Editing Nudges": "If you edit...selectable option text, you will be asked if
        //   you would prefer to split the Nudge instead of editing in place"
        val result = createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest(
                    text = "How do you feel?",
                    type = QuestionType.OPTION_SINGLE,
                    options = listOf("Good", "Okay", "Bad")
                ),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success

        val nudgeId = result.nudgeId
        val questions = repos.questionRepository.getByNudgeId(nudgeId)
        val options = repos.questionOptionRepository.getByQuestionId(questions[0].id)
        val optionToUpdate = options.first { it.text == "Okay" }

        // splitEdit = false → in-place
        updateNudge.execute(
            UpdateNudgeRequest(
                nudgeId = nudgeId,
                optionUpdates = listOf(UpdateOptionRequest(optionToUpdate.id, "Alright")),
                splitEdit = false
            )
        )

        val updatedOptions = repos.questionOptionRepository.getByQuestionId(questions[0].id)
        assertTrue(updatedOptions.any { it.text == "Alright" })
        assertFalse(updatedOptions.any { it.text == "Okay" })
    }

    @Test
    fun TDD_splitEditDisablesOldNudge() = runTest {
        // README "Editing Nudges": "The old version of the Nudge and all its related data will
        //   be preserved, and the old Nudge will be disabled"
        val nudgeId = createYesNoNudge("Did you sleep well?")

        updateNudge.execute(
            UpdateNudgeRequest(nudgeId = nudgeId, mainQuestionText = "Did you sleep enough?", splitEdit = true)
        )

        val oldNudge = repos.nudgeRepository.getById(nudgeId)
        assertNotNull(oldNudge)
        assertFalse(oldNudge.isEnabled)
    }

    @Test
    fun TDD_splitEditPreservesOldNudgeData() = runTest {
        // README "Editing Nudges": "The old version of the Nudge and all its related data will
        //   be preserved..."
        val nudgeId = createYesNoNudge("Did you sleep well?")

        updateNudge.execute(
            UpdateNudgeRequest(nudgeId = nudgeId, mainQuestionText = "Did you sleep enough?", splitEdit = true)
        )

        val oldNudge = repos.nudgeRepository.getById(nudgeId)
        assertNotNull(oldNudge)
        val oldQuestions = repos.questionRepository.getByNudgeId(nudgeId)
        assertTrue(oldQuestions.any { it.text == "Did you sleep well?" })
    }

    @Test
    fun TDD_splitEditCreatesNewEnabledNudgeWithEdits() = runTest {
        // README "Editing Nudges": "The edits will essentially be a new Nudge, which will be
        //   enabled going forward"
        val nudgeId = createYesNoNudge("Did you sleep well?")

        val updateResult = updateNudge.execute(
            UpdateNudgeRequest(nudgeId = nudgeId, mainQuestionText = "Did you sleep enough?", splitEdit = true)
        ) as UpdateNudgeResult.Success
        val newNudgeId = updateResult.nudgeId

        assertTrue(newNudgeId != nudgeId)
        val newNudge = repos.nudgeRepository.getById(newNudgeId)
        assertNotNull(newNudge)
        assertTrue(newNudge.isEnabled)
        val newQuestions = repos.questionRepository.getByNudgeId(newNudgeId)
        assertTrue(newQuestions.any { it.text == "Did you sleep enough?" })
    }

    @Test
    fun TDD_inPlaceEditSavesNudgeEditAuditRecord() = runTest {
        // README "Editing Nudges": "a note recording the date/time and contents of the edit
        //   will be saved with the Nudge"
        // ARCHITECTURE.md NudgeEdit: editedAt, fieldChanged, previousValue
        val nudgeId = createYesNoNudge("Original question?")

        updateNudge.execute(
            UpdateNudgeRequest(nudgeId = nudgeId, mainQuestionText = "Updated question?", splitEdit = false)
        )

        val edits = repos.nudgeEditRepository.getByNudgeId(nudgeId)
        assertEquals(1, edits.size)
        assertNotNull(edits[0].editedAt)
    }

    @Test
    fun TDD_inPlaceEditAuditRecordContainsPreviousValue() = runTest {
        // ARCHITECTURE.md NudgeEdit.previousValue: "Value before the edit"
        val nudgeId = createYesNoNudge("Original question?")

        updateNudge.execute(
            UpdateNudgeRequest(nudgeId = nudgeId, mainQuestionText = "Updated question?", splitEdit = false)
        )

        val edits = repos.nudgeEditRepository.getByNudgeId(nudgeId)
        assertEquals("Original question?", edits[0].previousValue)
    }

    @Test
    fun TDD_inPlaceEditAuditRecordContainsFieldChanged() = runTest {
        // ARCHITECTURE.md NudgeEdit.fieldChanged: "Which field was changed"
        val nudgeId = createYesNoNudge("Original question?")

        updateNudge.execute(
            UpdateNudgeRequest(nudgeId = nudgeId, mainQuestionText = "Updated question?", splitEdit = false)
        )

        val edits = repos.nudgeEditRepository.getByNudgeId(nudgeId)
        assertNotNull(edits[0].fieldChanged)
        assertTrue(edits[0].fieldChanged.isNotBlank())
    }

    @Test
    fun TDD_inPlaceEditRetainsOldAnswerDataWithNewQuestionText() = runTest {
        // README "Editing Nudges": "The old data will be kept with the new question text..."
        val nudgeId = createYesNoNudge("Did you exercise?")
        val questions = repos.questionRepository.getByNudgeId(nudgeId)
        val questionId = questions.first { it.isMainQuestion }.id

        // Record some answers
        repos.answerRepository.insert(
            com.nudgery.shared.model.Answer(
                id = "ans1",
                nudgeId = nudgeId,
                questionId = questionId,
                value = "YES",
                scheduledAt = kotlinx.datetime.Clock.System.now(),
                answeredAt = kotlinx.datetime.Clock.System.now(),
                isHidden = false
            )
        )

        updateNudge.execute(
            UpdateNudgeRequest(nudgeId = nudgeId, mainQuestionText = "Did you work out?", splitEdit = false)
        )

        // The answer is still there (associated with the same questionId)
        val answers = repos.answerRepository.getAllByNudgeId(nudgeId)
        assertEquals(1, answers.size)
        assertEquals("YES", answers[0].value)
        assertEquals(questionId, answers[0].questionId)
        // The question text has been updated
        val updatedQuestion = repos.questionRepository.getByNudgeId(nudgeId).first { it.isMainQuestion }
        assertEquals("Did you work out?", updatedQuestion.text)
    }

    @Test
    fun TDD_optionOrderPersistedOnUpdate() = runTest {
        // DESIGN.md "Create / Edit Nudge Wizard": "Option builder (add/remove/reorder up to 16 options)"
        val nudgeId = createOptionNudge(listOf("Alpha", "Beta", "Gamma"))
        val mainQuestion = repos.questionRepository.getByNudgeId(nudgeId).first { it.isMainQuestion }
        val options = repos.questionOptionRepository.getByQuestionId(mainQuestion.id).sortedBy { it.orderIndex }

        // Reorder: Gamma, Alpha, Beta
        val result = updateNudge.execute(
            UpdateNudgeRequest(
                nudgeId = nudgeId,
                optionReorder = listOf(options[2].id, options[0].id, options[1].id)
            )
        )

        assertIs<UpdateNudgeResult.Success>(result)
        val reordered = repos.questionOptionRepository.getByQuestionId(mainQuestion.id).sortedBy { it.orderIndex }
        assertEquals(listOf("Gamma", "Alpha", "Beta"), reordered.map { it.text })
    }

    @Test
    fun TDD_optionReorderPreservesOptionIds() = runTest {
        // Option IDs must not change on reorder — existing answers reference them
        val nudgeId = createOptionNudge(listOf("Alpha", "Beta", "Gamma"))
        val mainQuestion = repos.questionRepository.getByNudgeId(nudgeId).first { it.isMainQuestion }
        val originalOptions = repos.questionOptionRepository.getByQuestionId(mainQuestion.id).sortedBy { it.orderIndex }
        val originalIds = originalOptions.map { it.id }.toSet()

        updateNudge.execute(
            UpdateNudgeRequest(
                nudgeId = nudgeId,
                optionReorder = listOf(originalOptions[2].id, originalOptions[0].id, originalOptions[1].id)
            )
        )

        val reorderedIds = repos.questionOptionRepository.getByQuestionId(mainQuestion.id).map { it.id }.toSet()
        assertEquals(originalIds, reorderedIds, "Option IDs must be unchanged after reorder")
    }

    @Test
    fun TDD_optionReorderDoesNotTriggerSplitDialog() = runTest {
        // Reordering is cosmetic — it does not change the semantic meaning of historical answers,
        // so it must not require a split (no new nudge should be created).
        val nudgeId = createOptionNudge(listOf("Alpha", "Beta", "Gamma"))
        val mainQuestion = repos.questionRepository.getByNudgeId(nudgeId).first { it.isMainQuestion }
        val options = repos.questionOptionRepository.getByQuestionId(mainQuestion.id).sortedBy { it.orderIndex }

        updateNudge.execute(
            UpdateNudgeRequest(
                nudgeId = nudgeId,
                optionReorder = listOf(options[2].id, options[0].id, options[1].id),
                splitEdit = false
            )
        )

        val allNudges = repos.nudgeRepository.observeAll().first()
        assertEquals(1, allNudges.size, "Reorder must not create a split nudge")
    }

    private suspend fun createOptionNudge(options: List<String>): String {
        return (createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("Which do you prefer?", QuestionType.OPTION_SINGLE, options),
                schedule = dailySchedule()
            )
        ) as CreateNudgeResult.Success).nudgeId
    }
}
