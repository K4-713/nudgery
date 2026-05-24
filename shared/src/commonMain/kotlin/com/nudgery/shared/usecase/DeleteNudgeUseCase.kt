package com.nudgery.shared.usecase

import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.scheduler.NotificationScheduler

class DeleteNudgeUseCase(
    private val nudgeRepository: NudgeRepository,
    private val notificationScheduler: NotificationScheduler
) {
    suspend fun execute(nudgeId: String) {
        notificationScheduler.cancel(nudgeId)
        nudgeRepository.delete(nudgeId)
    }
}
