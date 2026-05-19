package com.nudgery.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nudgery.shared.model.Nudge
import com.nudgery.shared.model.Schedule
import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.repository.ScheduleRepository
import com.nudgery.shared.usecase.ComputeNextFireTimeUseCase
import com.nudgery.shared.usecase.UpdateNudgeRequest
import com.nudgery.shared.usecase.UpdateNudgeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

private const val TAG = "NudgeListViewModel"

data class PendingAnswerNavigation(val nudgeId: String, val scheduledAt: Instant?)

data class NudgeSummary(
    val nudgeId: String,
    val name: String,
    val scheduleDescription: String,
    val nextFireTime: Instant?,
    val isEnabled: Boolean
)

class NudgeListViewModel(
    private val nudgeRepository: NudgeRepository,
    private val scheduleRepository: ScheduleRepository,
    private val computeNextFireTime: ComputeNextFireTimeUseCase,
    private val updateNudge: UpdateNudgeUseCase
) : ViewModel() {

    val uiState: StateFlow<List<NudgeSummary>> = nudgeRepository.observeAll()
        .map { nudges -> nudges.map { it.toSummary() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _pendingAnswer = MutableStateFlow<PendingAnswerNavigation?>(null)
    val pendingAnswer: StateFlow<PendingAnswerNavigation?> = _pendingAnswer.asStateFlow()

    fun toggleEnabled(nudgeId: String) {
        viewModelScope.launch {
            val current = uiState.value.find { it.nudgeId == nudgeId } ?: return@launch
            updateNudge.execute(UpdateNudgeRequest(nudgeId = nudgeId, isEnabled = !current.isEnabled))
            Log.i(TAG, "Toggled nudge $nudgeId enabled → ${!current.isEnabled}")
        }
    }

    fun handleNotificationIntent(nudgeId: String, scheduledAt: Instant?) {
        Log.i(TAG, "Notification tap received for nudge $nudgeId scheduled at $scheduledAt")
        _pendingAnswer.value = PendingAnswerNavigation(nudgeId, scheduledAt)
    }

    fun consumePendingAnswerNavigation() {
        _pendingAnswer.value = null
    }

    private suspend fun Nudge.toSummary(): NudgeSummary {
        val schedule = scheduleRepository.getByNudgeId(id)
        val nextFire = schedule?.computeNextFire()
        return NudgeSummary(
            nudgeId = id,
            name = name,
            scheduleDescription = schedule?.let { ScheduleFormState.fromSchedule(it).toDescription() } ?: "",
            nextFireTime = nextFire,
            isEnabled = isEnabled
        )
    }

    private fun Schedule.computeNextFire(): Instant? = runCatching {
        computeNextFireTime.execute(this, Clock.System.now(), TimeZone.currentSystemDefault())
    }.getOrNull()
}
