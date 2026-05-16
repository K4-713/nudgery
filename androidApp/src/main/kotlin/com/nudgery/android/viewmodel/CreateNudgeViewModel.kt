package com.nudgery.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nudgery.shared.usecase.CreateNudgeRequest
import com.nudgery.shared.usecase.CreateNudgeResult
import com.nudgery.shared.usecase.CreateNudgeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "CreateNudgeViewModel"

data class CreateNudgeFormState(
    val nudgeName: String = "",
    val isEnabled: Boolean = true,
    val mainQuestion: QuestionFormState = QuestionFormState(),
    val followUpQuestions: List<QuestionFormState> = emptyList(),
    val schedule: ScheduleFormState = ScheduleFormState(),
    val isSubmitting: Boolean = false,
    val result: CreateNudgeResult? = null
)

class CreateNudgeViewModel(
    private val createNudge: CreateNudgeUseCase
) : ViewModel() {

    private val _formState = MutableStateFlow(CreateNudgeFormState())
    val formState: StateFlow<CreateNudgeFormState> = _formState.asStateFlow()

    fun setNudgeName(name: String) { _formState.update { it.copy(nudgeName = name) } }
    fun setEnabled(isEnabled: Boolean) { _formState.update { it.copy(isEnabled = isEnabled) } }
    fun setMainQuestion(question: QuestionFormState) { _formState.update { it.copy(mainQuestion = question) } }
    fun setSchedule(schedule: ScheduleFormState) { _formState.update { it.copy(schedule = schedule) } }

    fun addFollowUpQuestion(question: QuestionFormState) {
        _formState.update { it.copy(followUpQuestions = it.followUpQuestions + question) }
    }

    fun updateFollowUpQuestion(index: Int, question: QuestionFormState) {
        _formState.update {
            it.copy(followUpQuestions = it.followUpQuestions.toMutableList().also { list -> list[index] = question })
        }
    }

    fun removeFollowUpQuestion(index: Int) {
        _formState.update {
            it.copy(followUpQuestions = it.followUpQuestions.toMutableList().also { list -> list.removeAt(index) })
        }
    }

    fun submit() {
        val state = _formState.value
        if (state.isSubmitting) return
        _formState.update { it.copy(isSubmitting = true, result = null) }
        viewModelScope.launch {
            val result = createNudge.execute(
                CreateNudgeRequest(
                    mainQuestion = state.mainQuestion.toRequest(),
                    followUpQuestions = state.followUpQuestions.map { it.toRequest() },
                    schedule = state.schedule.toRequest(),
                    name = state.nudgeName.takeIf { it.isNotBlank() },
                    isEnabled = state.isEnabled
                )
            )
            _formState.update { it.copy(isSubmitting = false, result = result) }
            Log.i(TAG, "CreateNudge result: $result")
        }
    }
}
