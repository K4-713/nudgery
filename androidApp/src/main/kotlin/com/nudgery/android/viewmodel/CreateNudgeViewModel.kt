// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nudgery.shared.model.Nudge
import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.usecase.CreateNudgeRequest
import com.nudgery.shared.usecase.CreateNudgeResult
import com.nudgery.shared.usecase.CreateNudgeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "CreateNudgeViewModel"
private const val DEFAULT_NAME_PREFIX = "Nudge #"

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
    private val createNudge: CreateNudgeUseCase,
    private val nudgeRepository: NudgeRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(CreateNudgeFormState())
    val formState: StateFlow<CreateNudgeFormState> = _formState.asStateFlow()

    init {
        viewModelScope.launch {
            val existingNudges = nudgeRepository.observeAll().first()
            _formState.update { it.copy(nudgeName = nextDefaultName(existingNudges)) }
        }
    }

    fun setNudgeName(name: String) { _formState.update { it.copy(nudgeName = name) } }
    fun setEnabled(isEnabled: Boolean) { _formState.update { it.copy(isEnabled = isEnabled) } }
    fun setMainQuestion(question: QuestionFormState) {
        _formState.update {
            // A free-text main question can't have follow-ups; drop any that were added.
            val followUps = if (question.type.allowsFollowUps) it.followUpQuestions else emptyList()
            it.copy(mainQuestion = question, followUpQuestions = followUps)
        }
    }
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

    /**
     * Drop any follow-up the user added but never edited — one still equal to a pristine default
     * stub. Tapping "Add follow-up question" commits a blank stub to the form immediately, so a user
     * who adds one and then changes nothing (then taps Back or Next) would otherwise be left with a
     * phantom follow-up that re-opens the editor instead of the step's empty state, and that would
     * be submitted as a blank follow-up. Called when leaving the follow-up step, and defensively
     * before submit.
     */
    fun pruneUntouchedFollowUps() {
        _formState.update { state ->
            state.copy(followUpQuestions = state.followUpQuestions.filterNot { it == QuestionFormState() })
        }
    }

    fun submit() {
        pruneUntouchedFollowUps()
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

internal fun nextDefaultName(existingNudges: List<Nudge>): String {
    val candidateA = existingNudges.size + 1
    val highestPatternNumber = existingNudges
        .mapNotNull { nudge ->
            nudge.name.removePrefix(DEFAULT_NAME_PREFIX).toIntOrNull()
                ?.takeIf { nudge.name == "$DEFAULT_NAME_PREFIX$it" }
        }
        .maxOrNull() ?: 0
    val candidateB = highestPatternNumber + 1
    return "$DEFAULT_NAME_PREFIX${maxOf(candidateA, candidateB)}"
}
