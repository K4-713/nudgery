package com.nudgery.shared.usecase

import com.nudgery.shared.model.Answer
import com.nudgery.shared.repository.AnswerRepository
import com.nudgery.shared.util.generateUuid
import kotlinx.datetime.Clock

class RecordAnswerUseCase(private val answerRepository: AnswerRepository) {

    suspend fun execute(nudgeId: String, questionId: String, value: String): String {
        val answerId = generateUuid()
        answerRepository.insert(
            Answer(
                id = answerId,
                nudgeId = nudgeId,
                questionId = questionId,
                value = value,
                recordedAt = Clock.System.now(),
                isHidden = false
            )
        )
        return answerId
    }
}
