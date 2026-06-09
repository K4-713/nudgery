// SPDX-License-Identifier: CC0-1.0

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
import com.nudgery.shared.usecase.QuestionVisualizationSource
import com.nudgery.shared.usecase.analysisWindow
import com.nudgery.shared.usecase.SetAnswerHiddenUseCase
import com.nudgery.shared.usecase.UpdateNudgeRequest
import com.nudgery.shared.usecase.UpdateNudgeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until

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
    // The shared data window the whole dashboard is locked to.
    val windowStart: LocalDate? = null,
    val windowEnd: LocalDate? = null,
    // Full data extent (earliest answer .. today), for the navigation scrubber.
    val dataStart: LocalDate? = null,
    val dataEnd: LocalDate? = null,
    val windowOffsetDays: Int = 0,
    val windowLabel: String = "",
    val canShiftOlder: Boolean = false,
    val canShiftNewer: Boolean = false,
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

    // Earliest recorded answer date; bounds how far back the shared window can be shifted.
    private var earliestAnswerDate: LocalDate? = null

    // In-flight visualization render. A continuous drag shifts the window many times in quick
    // succession; cancelling the prior render conflates those to the latest window so we do one
    // aggregation per resting position instead of a backlog of stale ones.
    private var visualizationsJob: Job? = null

    // Per-question chart sources (full answer history + options), loaded from the database only when
    // the underlying answers change. Scrubbing re-aggregates these in memory — see renderVisualizations.
    private var visualizationSources: Map<String, QuestionVisualizationSource> = emptyMap()

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
                earliestAnswerDate = rows.minOfOrNull { it.scheduledAt }
                    ?.toLocalDateTime(TimeZone.currentSystemDefault())?.date
                applyWindow(_uiState.value.windowOffsetDays)
                reloadVisualizationSources()
            }
        }
    }

    fun selectTimeframe(timeframe: Timeframe) {
        _uiState.update { it.copy(selectedTimeframe = timeframe) }
        viewModelScope.launch { appSettings.setDefaultTimeframe(nudgeId, timeframe) }
        applyWindow(0) // resize the shared window; reset to the most recent period
        renderVisualizations()
    }

    /** Slides the shared window; positive [deltaDays] moves it further back in time (older). */
    fun shiftWindowDays(deltaDays: Int) {
        val current = _uiState.value.windowOffsetDays
        val next = (current + deltaDays).coerceIn(0, maxWindowOffsetDays())
        if (next == current) return
        applyWindow(next)
        renderVisualizations()
    }

    private fun today(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    private fun maxWindowOffsetDays(): Int {
        val earliest = earliestAnswerDate ?: return 0
        return earliest.until(today(), DateTimeUnit.DAY).toInt().coerceAtLeast(0)
    }

    /** Recomputes the shared window for the given offset and publishes it (label, bounds, flags). */
    private fun applyWindow(offsetDays: Int) {
        val timeframe = _uiState.value.selectedTimeframe
        val today = today()
        val earliest = earliestAnswerDate ?: today
        val maxOffset = maxWindowOffsetDays()
        val offset = if (timeframe == Timeframe.ALL_TIME) 0 else offsetDays.coerceIn(0, maxOffset)
        val (start, end) = analysisWindow(timeframe, offset, today, earliest)
        _uiState.update {
            it.copy(
                windowOffsetDays = offset,
                windowStart = start,
                windowEnd = end,
                dataStart = earliestAnswerDate,
                dataEnd = today,
                windowLabel = windowLabel(timeframe, start, end),
                canShiftOlder = timeframe != Timeframe.ALL_TIME && offset < maxOffset,
                canShiftNewer = timeframe != Timeframe.ALL_TIME && offset > 0
            )
        }
    }

    private fun windowLabel(timeframe: Timeframe, start: LocalDate, end: LocalDate): String {
        if (timeframe == Timeframe.ALL_TIME) return "All time"
        return "${formatWindowDate(start)} – ${formatWindowDate(end)}"
    }

    private fun formatWindowDate(date: LocalDate): String {
        val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        return if (date.year != today().year) "$month ${date.dayOfMonth}, ${date.year}"
        else "$month ${date.dayOfMonth}"
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

        reloadVisualizationSources()
    }

    /**
     * Reads each charted question's full answer history (and options) from the database and caches
     * it, then renders. This is the only visualization path that touches storage, so it runs only
     * when the underlying answers change — not while the user scrubs the window.
     */
    private suspend fun reloadVisualizationSources() {
        val questionIds = buildList {
            _uiState.value.mainQuestionId?.let { add(it) }
            addAll(followUpQuestions.map { it.id })
        }
        visualizationSources = questionIds
            .mapNotNull { id -> getVisualizationData.loadSource(nudgeId, id)?.let { id to it } }
            .toMap()
        Log.i(TAG, "Loaded ${visualizationSources.size} visualization sources for $nudgeId")
        renderVisualizations()
    }

    /**
     * Rebuilds the charts for the current window from the cached [visualizationSources]. Pure
     * in-memory aggregation (no database), so it is cheap to call on every scrub step; it cancels
     * any prior render so a fast drag collapses to its resting window instead of rendering a backlog
     * of intermediate windows.
     */
    private fun renderVisualizations() {
        val mainQuestionId = _uiState.value.mainQuestionId ?: return
        val timeframe = _uiState.value.selectedTimeframe
        val offsetDays = _uiState.value.windowOffsetDays
        val now = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        val sources = visualizationSources
        visualizationsJob?.cancel()
        visualizationsJob = viewModelScope.launch {
            val visualizations = sources[mainQuestionId]
                ?.let { getVisualizationData.build(it, timeframe, offsetDays, now, tz) }
                ?: emptyList()
            val followUpVizs = followUpQuestions.mapNotNull { question ->
                val source = sources[question.id] ?: return@mapNotNull null
                val vizs = getVisualizationData.build(source, timeframe, offsetDays, now, tz)
                if (vizs.isEmpty()) null
                else FollowUpVisualization(
                    questionId = question.id,
                    questionText = question.text,
                    visualizations = vizs
                )
            }
            _uiState.update { it.copy(visualizations = visualizations, followUpVisualizations = followUpVizs) }
            Log.i(TAG, "Rendered ${visualizations.size} main + ${followUpVizs.size} follow-up visualizations for $timeframe")
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
