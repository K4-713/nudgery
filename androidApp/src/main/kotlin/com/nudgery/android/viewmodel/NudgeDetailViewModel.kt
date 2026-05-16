package com.nudgery.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nudgery.shared.model.ExportFormat
import com.nudgery.shared.model.Question
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.Schedule
import com.nudgery.shared.model.Timeframe
import com.nudgery.shared.model.VisualizationData
import com.nudgery.shared.repository.AnswerRepository
import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.repository.QuestionOptionRepository
import com.nudgery.shared.repository.QuestionRepository
import com.nudgery.shared.repository.ScheduleRepository
import com.nudgery.shared.usecase.ComputeNextFireTimeUseCase
import com.nudgery.shared.usecase.ExportAnswersUseCase
import com.nudgery.shared.usecase.GetVisualizationDataUseCase
import com.nudgery.shared.usecase.SetAnswerHiddenUseCase
import com.nudgery.shared.usecase.UpdateNudgeRequest
import com.nudgery.shared.usecase.UpdateNudgeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

private const val TAG = "NudgeDetailViewModel"

data class AnswerRow(
    val answerId: String,
    val questionText: String,
    val displayValue: String,
    val recordedAt: Instant,
    val isHidden: Boolean
)

data class NudgeDetailUiState(
    val isLoading: Boolean = true,
    val nudgeName: String = "",
    val isEnabled: Boolean = false,
    val schedule: Schedule? = null,
    val nextFireTime: Instant? = null,
    val mainQuestionId: String? = null,
    val answers: List<AnswerRow> = emptyList(),
    val visualizations: List<VisualizationData> = emptyList(),
    val selectedTimeframe: Timeframe = Timeframe.WEEKLY,
    val exportContent: String? = null,
    val isExporting: Boolean = false
)

class NudgeDetailViewModel(
    private val nudgeId: String,
    private val nudgeRepository: NudgeRepository,
    private val questionRepository: QuestionRepository,
    private val questionOptionRepository: QuestionOptionRepository,
    private val scheduleRepository: ScheduleRepository,
    private val answerRepository: AnswerRepository,
    private val computeNextFireTime: ComputeNextFireTimeUseCase,
    private val getVisualizationData: GetVisualizationDataUseCase,
    private val setAnswerHidden: SetAnswerHiddenUseCase,
    private val exportAnswers: ExportAnswersUseCase,
    private val updateNudge: UpdateNudgeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NudgeDetailUiState())
    val uiState: StateFlow<NudgeDetailUiState> = _uiState.asStateFlow()

    // Maps used to join answers with display text; updated in loadStaticData()
    private val _questionMap = MutableStateFlow<Map<String, Question>>(emptyMap())
    private val _optionTextMap = MutableStateFlow<Map<String, String>>(emptyMap())

    init {
        viewModelScope.launch { loadStaticData() }

        viewModelScope.launch {
            combine(
                answerRepository.observeByNudgeId(nudgeId),
                _questionMap,
                _optionTextMap
            ) { answers, questions, optionTexts ->
                answers.map { answer ->
                    val question = questions[answer.questionId]
                    AnswerRow(
                        answerId = answer.id,
                        questionText = question?.text ?: "",
                        displayValue = formatDisplayValue(answer.value, question?.type, optionTexts),
                        recordedAt = answer.recordedAt,
                        isHidden = answer.isHidden
                    )
                }
            }.collect { rows ->
                _uiState.update { it.copy(answers = rows) }
            }
        }
    }

    fun selectTimeframe(timeframe: Timeframe) {
        _uiState.update { it.copy(selectedTimeframe = timeframe) }
        loadVisualizations()
    }

    fun setAnswerHidden(answerId: String, isHidden: Boolean) {
        viewModelScope.launch {
            setAnswerHidden.execute(answerId, isHidden)
            Log.i(TAG, "Answer $answerId hidden=$isHidden")
        }
    }

    fun exportAnswers(format: ExportFormat) {
        _uiState.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            val content = exportAnswers.execute(nudgeId, format)
            _uiState.update { it.copy(exportContent = content, isExporting = false) }
            Log.i(TAG, "Exported ${content.lines().size} lines as $format")
        }
    }

    fun updateEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            updateNudge.execute(UpdateNudgeRequest(nudgeId = nudgeId, isEnabled = isEnabled))
            _uiState.update { it.copy(isEnabled = isEnabled) }
            Log.i(TAG, "Nudge $nudgeId enabled=$isEnabled")
        }
    }

    fun clearExportContent() {
        _uiState.update { it.copy(exportContent = null) }
    }

    private suspend fun loadStaticData() {
        val nudge = nudgeRepository.getById(nudgeId)
        if (nudge == null) {
            Log.w(TAG, "Nudge $nudgeId not found")
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        val questions = questionRepository.getByNudgeId(nudgeId)
        val mainQuestion = questions.firstOrNull { it.isMainQuestion }

        val optionTextMap = mutableMapOf<String, String>()
        questions.filter { it.type.isOptionType }.forEach { q ->
            questionOptionRepository.getByQuestionId(q.id).forEach { opt ->
                optionTextMap[opt.id] = opt.text
            }
        }

        val schedule = scheduleRepository.getByNudgeId(nudgeId)
        val nextFire = schedule?.let {
            runCatching {
                computeNextFireTime.execute(it, Clock.System.now(), TimeZone.currentSystemDefault())
            }.getOrNull()
        }

        _questionMap.value = questions.associateBy { it.id }
        _optionTextMap.value = optionTextMap

        _uiState.update {
            it.copy(
                isLoading = false,
                nudgeName = nudge.name,
                isEnabled = nudge.isEnabled,
                schedule = schedule,
                nextFireTime = nextFire,
                mainQuestionId = mainQuestion?.id
            )
        }

        loadVisualizations()
    }

    private fun loadVisualizations() {
        val questionId = _uiState.value.mainQuestionId ?: return
        val timeframe = _uiState.value.selectedTimeframe
        viewModelScope.launch {
            val visualizations = getVisualizationData.execute(
                nudgeId = nudgeId,
                questionId = questionId,
                timeframe = timeframe,
                now = Clock.System.now(),
                timeZone = TimeZone.currentSystemDefault()
            )
            _uiState.update { it.copy(visualizations = visualizations) }
            Log.i(TAG, "Loaded ${visualizations.size} visualizations for $timeframe")
        }
    }

    private fun formatDisplayValue(
        value: String,
        type: QuestionType?,
        optionTexts: Map<String, String>
    ): String = when (type) {
        QuestionType.OPTION_SINGLE, QuestionType.OPTION_MULTI ->
            value.split(",")
                .mapNotNull { optionTexts[it.trim()] }
                .joinToString(", ")
                .ifEmpty { value }
        else -> value
    }
}
