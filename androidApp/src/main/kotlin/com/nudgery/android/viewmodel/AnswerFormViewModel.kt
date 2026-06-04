package com.nudgery.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nudgery.android.settings.AppSettings
import com.nudgery.shared.emoji.Gender
import com.nudgery.shared.emoji.SkinTone
import com.nudgery.shared.model.Question
import com.nudgery.shared.model.QuestionOption
import com.nudgery.shared.repository.QuestionOptionRepository
import com.nudgery.shared.repository.QuestionRepository
import com.nudgery.shared.usecase.RecordAnswerUseCase
import com.nudgery.shared.util.dropLastEmoji
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

private const val TAG = "AnswerFormViewModel"

// Koin viewModelOf resolves by type; null has no type info, so Instant? can't be injected directly.
data class ScheduledAt(val instant: Instant?)

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
    val isDismissed: Boolean = false,
    // Emoji-answer support (ED-13): defaults applied on pick, plus the recents tab.
    val emojiSkinTone: SkinTone = SkinTone.DEFAULT,
    val emojiGender: Gender = Gender.NEUTRAL,
    val emojiRecents: List<String> = emptyList()
) {
    val currentQuestion: AnswerFormQuestion? get() = questions.getOrNull(currentStepIndex)
    val totalSteps: Int get() = questions.size
}

class AnswerFormViewModel(
    private val nudgeId: String,
    private val scheduledAt: ScheduledAt,
    private val questionRepository: QuestionRepository,
    private val questionOptionRepository: QuestionOptionRepository,
    private val recordAnswer: RecordAnswerUseCase,
    private val appSettings: AppSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnswerFormUiState())
    val uiState: StateFlow<AnswerFormUiState> = _uiState.asStateFlow()

    private var allQuestions: List<Question> = emptyList()
    private var optionsByQuestion: Map<String, List<QuestionOption>> = emptyMap()

    // Answers are buffered until the session completes. If the form is dismissed mid-session,
    // the buffer is discarded and nothing is written to the database.
    private data class BufferedAnswer(val questionId: String, val value: String, val scheduledAt: Instant)
    private val pendingAnswers = mutableListOf<BufferedAnswer>()

    init {
        viewModelScope.launch { loadQuestions() }
        viewModelScope.launch { appSettings.defaultEmojiSkinTone.collect { t -> _uiState.update { it.copy(emojiSkinTone = t) } } }
        viewModelScope.launch { appSettings.defaultEmojiGender.collect { g -> _uiState.update { it.copy(emojiGender = g) } } }
        viewModelScope.launch { appSettings.emojiRecents.collect { r -> _uiState.update { it.copy(emojiRecents = r) } } }
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

    /** Appends a picked emoji to the answer string and records it as recent (ED-13). */
    fun appendEmoji(emoji: String) {
        _uiState.update { it.copy(currentAnswer = it.currentAnswer + emoji) }
        viewModelScope.launch { appSettings.addEmojiRecent(emoji) }
    }

    /** Removes the last emoji from the answer string (the picker's backspace). */
    fun backspaceEmoji() {
        _uiState.update { it.copy(currentAnswer = dropLastEmoji(it.currentAnswer)) }
    }

    fun saveAnswer() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val currentQuestion = state.currentQuestion ?: return
        val answer = state.currentAnswer
        if (answer.isBlank()) return

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            val effectiveScheduledAt = scheduledAt.instant ?: Clock.System.now()
            pendingAnswers.add(BufferedAnswer(currentQuestion.question.id, answer, effectiveScheduledAt))
            Log.d(TAG, "Buffered answer for question ${currentQuestion.question.id}: $answer")

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
                commitPendingAnswers()
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
        pendingAnswers.clear()
        _uiState.update { it.copy(isDismissed = true) }
    }

    private suspend fun commitPendingAnswers() {
        for (pending in pendingAnswers) {
            recordAnswer.execute(nudgeId, pending.questionId, pending.value, pending.scheduledAt)
            Log.i(TAG, "Committed answer for question ${pending.questionId}: ${pending.value}")
        }
        pendingAnswers.clear()
    }

    private fun isTriggerSatisfied(question: Question, mainAnswer: String): Boolean {
        val triggerValue = question.triggerAnswerValue ?: return true
        return evaluateTrigger(triggerValue, question.triggerOperator, mainAnswer)
    }
}
