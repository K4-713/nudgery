package com.nudgery.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.repository.QuestionOptionRepository
import com.nudgery.shared.repository.QuestionRepository
import com.nudgery.shared.repository.ScheduleRepository
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
    val optionId: String,
    val text: String,
    val originalText: String
) {
    val isChanged: Boolean get() = text != originalText
}

data class EditNudgeFormState(
    val isLoading: Boolean = true,
    val name: String = "",
    val originalName: String = "",
    val isEnabled: Boolean = false,
    val mainQuestionText: String = "",
    val originalMainQuestionText: String = "",
    val options: List<OptionEditState> = emptyList(),
    val schedule: ScheduleFormState = ScheduleFormState(),
    val showSplitDialog: Boolean = false,
    val isSubmitting: Boolean = false,
    val result: UpdateNudgeResult? = null
) {
    val questionOrOptionChanged: Boolean
        get() = mainQuestionText != originalMainQuestionText || options.any { it.isChanged }
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

    fun setMainQuestionText(text: String) {
        _formState.update { it.copy(mainQuestionText = text) }
    }

    fun updateOption(optionId: String, newText: String) {
        _formState.update { state ->
            state.copy(options = state.options.map { opt ->
                if (opt.optionId == optionId) opt.copy(text = newText) else opt
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
                        .map { UpdateOptionRequest(it.optionId, it.text) },
                    schedule = state.schedule.toRequest(),
                    splitEdit = splitEdit
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

        val options = if (mainQuestion?.type?.isOptionType == true) {
            questionOptionRepository.getByQuestionId(mainQuestion.id).map { opt ->
                OptionEditState(optionId = opt.id, text = opt.text, originalText = opt.text)
            }
        } else {
            emptyList()
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
                options = options,
                schedule = scheduleForm
            )
        }
    }
}
