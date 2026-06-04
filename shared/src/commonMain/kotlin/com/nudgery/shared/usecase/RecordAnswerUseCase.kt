package com.nudgery.shared.usecase

import com.nudgery.shared.model.Answer
import com.nudgery.shared.repository.AnswerRepository
import com.nudgery.shared.util.generateUuid
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class RecordAnswerUseCase(private val answerRepository: AnswerRepository) {

    suspend fun execute(
        nudgeId: String,
        questionId: String,
        value: String,
        scheduledAt: Instant = Clock.System.now()
    ): String {
        val answerId = generateUuid()
        // Trim the answer text at the save boundary (ED-16); a no-op for structured/emoji values.
        val storedValue = value.normalizedForStorage()
        answerRepository.insert(
            Answer(
                id = answerId,
                nudgeId = nudgeId,
                questionId = questionId,
                value = storedValue,
                scheduledAt = scheduledAt,
                answeredAt = Clock.System.now(),
                isHidden = false
            )
        )
        return answerId
    }
}
