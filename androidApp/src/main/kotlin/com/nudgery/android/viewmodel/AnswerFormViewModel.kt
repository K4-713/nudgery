package com.nudgery.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nudgery.shared.model.Question
import com.nudgery.shared.model.QuestionOption
import com.nudgery.shared.model.TriggerOperator
import com.nudgery.shared.repository.QuestionOptionRepository
import com.nudgery.shared.repository.QuestionRepository
import com.nudgery.shared.usecase.RecordAnswerUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

private const val TAG = "AnswerFormViewModel"

data class AnswerFormQuestion(
    val question: Question,
    val options: List<QuestionOption>
)

data class AnswerFormUiState(
    val isLoading: Boolean = true,
    val currentStepIndex: Int = 0,
    val questions: List<AnswerFormQuestion> = emptyList(),
    val currentAnswer: String = "",
    val isSubmitting: Boolean = false,
    val isDismissed: Boolean = false
) {
    val currentQuestion: AnswerFormQuestion? get() = questions.getOrNull(currentStepIndex)
    val totalSteps: Int get() = questions.size
}

class AnswerFormViewModel(
    private val nudgeId: String,
    private val scheduledAt: Instant?,
    private val questionRepository: QuestionRepository,
    private val questionOptionRepository: QuestionOptionRepository,
    private val recordAnswer: RecordAnswerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnswerFormUiState())
    val uiState: StateFlow<AnswerFormUiState> = _uiState.asStateFlow()

    private var allQuestions: List<Question> = emptyList()
    private var optionsByQuestion: Map<String, List<QuestionOption>> = emptyMap()

    init {
        viewModelScope.launch { loadQuestions() }
    }

    private suspend fun loadQuestions() {
        val questions = questionRepository.getByNudgeId(nudgeId).sortedBy { it.orderIndex }
        val mainQuestion = questions.firstOrNull { it.isMainQuestion }
        if (mainQuestion == null) {
            Log.w(TAG, "No main question found for nudge $nudgeId")
            _uiState.update { it.copy(isLoading = false, isDismissed = true) }
            return
        }

        val optionsMap = questions.associate { q ->
            q.id to questionOptionRepository.getByQuestionId(q.id)
        }

        allQuestions = questions
        optionsByQuestion = optionsMap

        _uiState.update { state ->
            state.copy(
                isLoading = false,
                questions = listOf(AnswerFormQuestion(mainQuestion, optionsMap[mainQuestion.id] ?: emptyList()))
            )
        }
    }

    fun setCurrentAnswer(answer: String) {
        _uiState.update { it.copy(currentAnswer = answer) }
    }

    fun saveAnswer() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val currentQuestion = state.currentQuestion ?: return
        val answer = state.currentAnswer
        if (answer.isBlank()) return

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            val effectiveScheduledAt = scheduledAt ?: Clock.System.now()
            recordAnswer.execute(nudgeId, currentQuestion.question.id, answer, effectiveScheduledAt)
            Log.i(TAG, "Recorded answer for question ${currentQuestion.question.id}: $answer")

            val updatedQuestions = if (currentQuestion.question.isMainQuestion) {
                val triggeredFollowUps = allQuestions
                    .filter { !it.isMainQuestion }
                    .filter { isTriggerSatisfied(it, answer) }
                    .map { AnswerFormQuestion(it, optionsByQuestion[it.id] ?: emptyList()) }
                state.questions + triggeredFollowUps
            } else {
                state.questions
            }

            val nextStep = state.currentStepIndex + 1
            if (nextStep >= updatedQuestions.size) {
                _uiState.update { it.copy(isSubmitting = false, questions = updatedQuestions, isDismissed = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        currentStepIndex = nextStep,
                        currentAnswer = "",
                        questions = updatedQuestions
                    )
                }
            }
        }
    }

    fun dismiss() {
        _uiState.update { it.copy(isDismissed = true) }
    }

    private fun isTriggerSatisfied(question: Question, mainAnswer: String): Boolean {
        val triggerValue = question.triggerAnswerValue ?: return true
        val operator = question.triggerOperator ?: return mainAnswer == triggerValue

        val answerNum = mainAnswer.toDoubleOrNull()
        val triggerNum = triggerValue.toDoubleOrNull()

        return when (operator) {
            TriggerOperator.EQ -> mainAnswer == triggerValue
            TriggerOperator.GT -> answerNum != null && triggerNum != null && answerNum > triggerNum
            TriggerOperator.GTE -> answerNum != null && triggerNum != null && answerNum >= triggerNum
            TriggerOperator.LT -> answerNum != null && triggerNum != null && answerNum < triggerNum
            TriggerOperator.LTE -> answerNum != null && triggerNum != null && answerNum <= triggerNum
        }
    }
}
