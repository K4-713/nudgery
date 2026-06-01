package com.nudgery.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.repository.QuestionOptionRepository
import com.nudgery.shared.repository.QuestionRepository
import com.nudgery.shared.repository.ScheduleRepository
import com.nudgery.shared.usecase.FollowUpReplacement
import com.nudgery.shared.usecase.UpdateNudgeRequest
import com.nudgery.shared.usecase.UpdateNudgeResult
import com.nudgery.shared.usecase.UpdateNudgeUseCase
import com.nudgery.shared.usecase.UpdateOptionRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "EditNudgeViewModel"

data class OptionEditState(
    val optionId: String?,  // null = new option not yet in the DB
    val text: String,
    val originalText: String = ""
) {
    val isNew: Boolean get() = optionId == null
    val isChanged: Boolean get() = !isNew && text != originalText
}

data class EditableFollowUp(
    val questionId: String?,  // null for newly-added follow-ups not yet in DB
    val formState: QuestionFormState
)

data class EditNudgeFormState(
    val isLoading: Boolean = true,
    val name: String = "",
    val originalName: String = "",
    val isEnabled: Boolean = false,
    val mainQuestionText: String = "",
    val originalMainQuestionText: String = "",
    val mainQuestionType: QuestionType = QuestionType.YES_NO,
    val mainQuestionScaleMin: Int = 0,
    val mainQuestionScaleMax: Int = 10,
    val options: List<OptionEditState> = emptyList(),
    val originalOptionOrder: List<String> = emptyList(),
    val removedOptionIds: Set<String> = emptySet(),
    val followUps: List<EditableFollowUp> = emptyList(),
    val schedule: ScheduleFormState = ScheduleFormState(),
    val showSplitDialog: Boolean = false,
    val isSubmitting: Boolean = false,
    val result: UpdateNudgeResult? = null
) {
    val hasOrderChanged: Boolean
        get() = originalOptionOrder.isNotEmpty() && options.mapNotNull { it.optionId } != originalOptionOrder
    val questionOrOptionChanged: Boolean
        get() = mainQuestionText != originalMainQuestionText ||
                options.any { it.isChanged } ||
                removedOptionIds.isNotEmpty()
}

class EditNudgeViewModel(
    private val nudgeId: String,
    private val nudgeRepository: NudgeRepository,
    private val questionRepository: QuestionRepository,
    private val questionOptionRepository: QuestionOptionRepository,
    private val scheduleRepository: ScheduleRepository,
    private val updateNudge: UpdateNudgeUseCase
) : ViewModel() {

    private val _formState = MutableStateFlow(EditNudgeFormState())
    val formState: StateFlow<EditNudgeFormState> = _formState.asStateFlow()

    init {
        viewModelScope.launch { loadExistingData() }
    }

    fun setName(name: String) { _formState.update { it.copy(name = name) } }
    fun setEnabled(isEnabled: Boolean) { _formState.update { it.copy(isEnabled = isEnabled) } }
    fun setSchedule(schedule: ScheduleFormState) { _formState.update { it.copy(schedule = schedule) } }

    fun addFollowUp() {
        _formState.update { state ->
            state.copy(followUps = state.followUps + EditableFollowUp(questionId = null, formState = QuestionFormState()))
        }
    }

    fun updateFollowUp(index: Int, formState: QuestionFormState) {
        _formState.update { state ->
            val updated = state.followUps.toMutableList()
            if (index in updated.indices) updated[index] = updated[index].copy(formState = formState)
            state.copy(followUps = updated)
        }
    }

    fun removeFollowUp(index: Int) {
        _formState.update { state ->
            state.copy(followUps = state.followUps.toMutableList().also { it.removeAt(index) })
        }
    }

    fun setMainQuestionText(text: String) {
        _formState.update { it.copy(mainQuestionText = text) }
    }

    fun addOption() {
        _formState.update { state ->
            state.copy(options = state.options + OptionEditState(optionId = null, text = ""))
        }
    }

    fun removeOption(index: Int) {
        _formState.update { state ->
            val target = state.options.getOrNull(index) ?: return@update state
            val updatedRemoved = if (target.optionId != null)
                state.removedOptionIds + target.optionId
            else
                state.removedOptionIds
            state.copy(
                options = state.options.toMutableList().also { it.removeAt(index) },
                removedOptionIds = updatedRemoved
            )
        }
    }

    fun reorderOption(fromIndex: Int, toIndex: Int) {
        _formState.update { state ->
            if (fromIndex !in state.options.indices || toIndex !in state.options.indices) return@update state
            val reordered = state.options.toMutableList()
            reordered.add(toIndex, reordered.removeAt(fromIndex))
            state.copy(options = reordered)
        }
    }

    fun updateOptionAt(index: Int, newText: String) {
        _formState.update { state ->
            if (index !in state.options.indices) return@update state
            state.copy(options = state.options.toMutableList().also { list ->
                list[index] = list[index].copy(text = newText)
            })
        }
    }

    /** Initial submit. Shows split dialog when question/option text was changed. */
    fun submit() {
        val state = _formState.value
        if (state.isSubmitting) return
        if (state.questionOrOptionChanged) {
            _formState.update { it.copy(showSplitDialog = true) }
            return
        }
        performSubmit(splitEdit = false)
    }

    /** Called when the user selects "Split" in the split dialog. */
    fun submitWithSplit() {
        _formState.update { it.copy(showSplitDialog = false) }
        performSubmit(splitEdit = true)
    }

    /** Called when the user selects "Edit in place" in the split dialog. */
    fun submitInPlace() {
        _formState.update { it.copy(showSplitDialog = false) }
        performSubmit(splitEdit = false)
    }

    fun dismissSplitDialog() {
        _formState.update { it.copy(showSplitDialog = false) }
    }

    private fun performSubmit(splitEdit: Boolean) {
        val state = _formState.value
        _formState.update { it.copy(isSubmitting = true, result = null) }
        viewModelScope.launch {
            val result = updateNudge.execute(
                UpdateNudgeRequest(
                    nudgeId = nudgeId,
                    name = state.name.takeIf { it != state.originalName },
                    isEnabled = state.isEnabled,
                    mainQuestionText = state.mainQuestionText.takeIf { it != state.originalMainQuestionText },
                    optionUpdates = state.options
                        .filter { it.isChanged }
                        .map { UpdateOptionRequest(it.optionId!!, it.text) },
                    optionReorder = if (state.hasOrderChanged) state.options.mapNotNull { it.optionId } else null,
                    newOptions = state.options.filter { it.isNew }.map { it.text }.filter { it.isNotBlank() },
                    removedOptionIds = state.removedOptionIds,
                    schedule = state.schedule.toRequest(),
                    splitEdit = splitEdit,
                    followUpReplacements = state.followUps.map { ef ->
                        FollowUpReplacement(questionId = ef.questionId, request = ef.formState.toRequest())
                    }
                )
            )
            _formState.update { it.copy(isSubmitting = false, result = result) }
            Log.i(TAG, "UpdateNudge result: $result (split=$splitEdit)")
        }
    }

    private suspend fun loadExistingData() {
        val nudge = nudgeRepository.getById(nudgeId)
        if (nudge == null) {
            Log.w(TAG, "Nudge $nudgeId not found for editing")
            _formState.update { it.copy(isLoading = false) }
            return
        }

        val questions = questionRepository.getByNudgeId(nudgeId)
        val mainQuestion = questions.firstOrNull { it.isMainQuestion }

        val mainOptions = if (mainQuestion?.type?.isOptionType == true) {
            questionOptionRepository.getByQuestionId(mainQuestion.id).sortedBy { it.orderIndex }
        } else {
            emptyList()
        }
        val mainOptionIds = mainOptions.map { it.id }
        val editableOptions = mainOptions.map { opt ->
            OptionEditState(optionId = opt.id, text = opt.text, originalText = opt.text)
        }

        val followUps = questions.filter { !it.isMainQuestion }.map { q ->
            val followUpOptions = if (q.type.isOptionType) {
                questionOptionRepository.getByQuestionId(q.id)
                    .sortedBy { it.orderIndex }
                    .map { it.text }
            } else {
                emptyList()
            }
            // Convert stored option UUID back to index for the trigger UI
            val uiTriggerValue = if (mainQuestion?.type?.isOptionType == true && q.triggerAnswerValue != null) {
                val idx = mainOptionIds.indexOf(q.triggerAnswerValue)
                if (idx >= 0) "$idx" else q.triggerAnswerValue
            } else {
                q.triggerAnswerValue
            }
            EditableFollowUp(
                questionId = q.id,
                formState = QuestionFormState(
                    text = q.text,
                    type = q.type,
                    options = followUpOptions,
                    triggerAnswerValue = uiTriggerValue,
                    triggerOperator = q.triggerOperator,
                    scaleMin = q.scaleMin ?: 0,
                    scaleMax = q.scaleMax ?: 10
                )
            )
        }

        val schedule = scheduleRepository.getByNudgeId(nudgeId)
        val scheduleForm = schedule?.let { ScheduleFormState.fromSchedule(it) } ?: ScheduleFormState()

        _formState.update {
            it.copy(
                isLoading = false,
                name = nudge.name,
                originalName = nudge.name,
                isEnabled = nudge.isEnabled,
                mainQuestionText = mainQuestion?.text ?: "",
                originalMainQuestionText = mainQuestion?.text ?: "",
                mainQuestionType = mainQuestion?.type ?: QuestionType.YES_NO,
                mainQuestionScaleMin = mainQuestion?.scaleMin ?: 0,
                mainQuestionScaleMax = mainQuestion?.scaleMax ?: 10,
                options = editableOptions,
                originalOptionOrder = editableOptions.mapNotNull { it.optionId },
                followUps = followUps,
                schedule = scheduleForm
            )
        }
    }
}
