package com.nudgery.shared.usecase

import com.nudgery.shared.repository.AnswerRepository

class SetAnswerHiddenUseCase(private val answerRepository: AnswerRepository) {

    suspend fun execute(answerId: String, isHidden: Boolean) {
        answerRepository.setHidden(answerId, isHidden)
    }
}
