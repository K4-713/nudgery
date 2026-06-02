package com.nudgery.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nudgery.android.settings.AppSettings
import com.nudgery.shared.model.ExportFormat
import com.nudgery.shared.model.Nudge
import com.nudgery.shared.model.Question
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.Schedule
import com.nudgery.shared.model.Timeframe
import com.nudgery.shared.model.VisualizationData
import com.nudgery.shared.repository.AnswerRepository
import com.nudgery.shared.repository.NotificationFireRepository
import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.repository.QuestionOptionRepository
import com.nudgery.shared.repository.QuestionRepository
import com.nudgery.shared.repository.ScheduleRepository
import com.nudgery.shared.usecase.ComputeNextFireTimeUseCase
import com.nudgery.shared.usecase.DeleteNudgeUseCase
import com.nudgery.shared.usecase.ExportAnswersUseCase
import com.nudgery.shared.usecase.GetVisualizationDataUseCase
import com.nudgery.shared.usecase.SetAnswerHiddenUseCase
import com.nudgery.shared.usecase.UpdateNudgeRequest
import com.nudgery.shared.usecase.UpdateNudgeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

private const val TAG = "NudgeDetailViewModel"

data class FollowUpVisualization(
    val questionId: String,
    val questionText: String,
    val visualizations: List<VisualizationData>
)

data class AnswerRow(
    val answerId: String,
    val questionText: String,
    val questionOrderIndex: Int,
    val displayValue: String,
    val scheduledAt: Instant,
    val answeredAt: Instant,
    val isHidden: Boolean
)

data class NudgeDetailUiState(
    val isLoading: Boolean = true,
    val nudgeName: String = "",
    val mainQuestionText: String = "",
    val mainQuestionType: QuestionType? = null,
    val isEnabled: Boolean = false,
    val schedule: Schedule? = null,
    val nextFireTime: Instant? = null,
    val mainQuestionId: String? = null,
    val followUpCount: Int = 0,
    val answers: List<AnswerRow> = emptyList(),
    val hasMissedNotification: Boolean = false,
    val visualizations: List<VisualizationData> = emptyList(),
    val followUpVisualizations: List<FollowUpVisualization> = emptyList(),
    val selectedTimeframe: Timeframe = Timeframe.WEEKLY,
    val exportContent: String? = null,
    val exportFormat: ExportFormat = ExportFormat.CSV,
    val isExporting: Boolean = false,
    val isDeleted: Boolean = false
)

class NudgeDetailViewModel(
    private val nudgeId: String,
    private val nudgeRepository: NudgeRepository,
    private val questionRepository: QuestionRepository,
    private val questionOptionRepository: QuestionOptionRepository,
    private val scheduleRepository: ScheduleRepository,
    private val answerRepository: AnswerRepository,
    private val notificationFireRepository: NotificationFireRepository,
    private val computeNextFireTime: ComputeNextFireTimeUseCase,
    private val getVisualizationData: GetVisualizationDataUseCase,
    private val setAnswerHidden: SetAnswerHiddenUseCase,
    private val exportAnswers: ExportAnswersUseCase,
    private val updateNudge: UpdateNudgeUseCase,
    private val deleteNudge: DeleteNudgeUseCase,
    private val appSettings: AppSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(NudgeDetailUiState())
    val uiState: StateFlow<NudgeDetailUiState> = _uiState.asStateFlow()

    // Maps used to join answers with display text; updated in loadNudgeData()
    private val _questionMap = MutableStateFlow<Map<String, Question>>(emptyMap())
    private val _optionTextMap = MutableStateFlow<Map<String, String>>(emptyMap())

    private var followUpQuestions: List<Question> = emptyList()

    init {
        viewModelScope.launch {
            val storedTimeframe = appSettings.getDefaultTimeframe(nudgeId).first()
            _uiState.update { it.copy(selectedTimeframe = storedTimeframe) }

            nudgeRepository.observeById(nudgeId).collect { nudge ->
                if (nudge == null) {
                    Log.w(TAG, "Nudge $nudgeId not found")
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    loadNudgeData(nudge)
                }
            }
        }

        viewModelScope.launch {
            combine(
                answerRepository.observeByNudgeId(nudgeId),
                _questionMap,
                _optionTextMap,
                notificationFireRepository.observeMostRecentByNudgeId(nudgeId)
            ) { answers, questions, optionTexts, recentFire ->
                val rows = answers.map { answer ->
                    val question = questions[answer.questionId]
                    AnswerRow(
                        answerId = answer.id,
                        questionText = question?.text ?: "",
                        questionOrderIndex = question?.orderIndex ?: 0,
                        displayValue = formatDisplayValue(answer.value, question?.type, optionTexts),
                        scheduledAt = answer.scheduledAt,
                        answeredAt = answer.answeredAt,
                        isHidden = answer.isHidden
                    )
                }
                val mostRecentAnsweredAt = answers.filter { !it.isHidden }.maxOfOrNull { it.answeredAt }
                val hasMissed = recentFire != null &&
                    (mostRecentAnsweredAt == null || mostRecentAnsweredAt < recentFire.firedAt)
                Pair(rows, hasMissed)
            }.collect { (rows, hasMissed) ->
                _uiState.update { it.copy(answers = rows, hasMissedNotification = hasMissed) }
                loadVisualizations()
            }
        }
    }

    fun selectTimeframe(timeframe: Timeframe) {
        _uiState.update { it.copy(selectedTimeframe = timeframe) }
        viewModelScope.launch { appSettings.setDefaultTimeframe(nudgeId, timeframe) }
        loadVisualizations()
    }

    fun setAnswerHidden(answerId: String, isHidden: Boolean) {
        viewModelScope.launch {
            setAnswerHidden.execute(answerId, isHidden)
            Log.i(TAG, "Answer $answerId hidden=$isHidden")
        }
    }

    fun exportAnswers(format: ExportFormat) {
        _uiState.update { it.copy(isExporting = true, exportFormat = format) }
        viewModelScope.launch {
            val content = exportAnswers.execute(nudgeId, format)
            _uiState.update { it.copy(exportContent = content, isExporting = false) }
            Log.i(TAG, "Exported as $format")
        }
    }

    fun updateEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            updateNudge.execute(UpdateNudgeRequest(nudgeId = nudgeId, isEnabled = isEnabled))
            _uiState.update { it.copy(isEnabled = isEnabled) }
            Log.i(TAG, "Nudge $nudgeId enabled=$isEnabled")
        }
    }

    fun deleteNudge() {
        viewModelScope.launch {
            deleteNudge.execute(nudgeId)
            _uiState.update { it.copy(isDeleted = true) }
            Log.i(TAG, "Deleted nudge $nudgeId")
        }
    }

    fun clearExportContent() {
        _uiState.update { it.copy(exportContent = null) }
    }

    private suspend fun loadNudgeData(nudge: Nudge) {
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
        followUpQuestions = questions.filter { !it.isMainQuestion }

        _uiState.update {
            it.copy(
                isLoading = false,
                nudgeName = nudge.name,
                mainQuestionText = mainQuestion?.text ?: "",
                mainQuestionType = mainQuestion?.type,
                isEnabled = nudge.isEnabled,
                schedule = schedule,
                nextFireTime = nextFire,
                mainQuestionId = mainQuestion?.id,
                followUpCount = followUpQuestions.size
            )
        }

        loadVisualizations()
    }

    private fun loadVisualizations() {
        val questionId = _uiState.value.mainQuestionId ?: return
        val timeframe = _uiState.value.selectedTimeframe
        viewModelScope.launch {
            val now = Clock.System.now()
            val tz = TimeZone.currentSystemDefault()
            val visualizations = getVisualizationData.execute(
                nudgeId = nudgeId,
                questionId = questionId,
                timeframe = timeframe,
                now = now,
                timeZone = tz
            )
            val followUpVizs = followUpQuestions.mapNotNull { question ->
                val vizs = getVisualizationData.execute(
                    nudgeId = nudgeId,
                    questionId = question.id,
                    timeframe = timeframe,
                    now = now,
                    timeZone = tz
                )
                if (vizs.isEmpty()) null
                else FollowUpVisualization(
                    questionId = question.id,
                    questionText = question.text,
                    visualizations = vizs
                )
            }
            _uiState.update { it.copy(visualizations = visualizations, followUpVisualizations = followUpVizs) }
            Log.i(TAG, "Loaded ${visualizations.size} main + ${followUpVizs.size} follow-up visualizations for $timeframe")
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
