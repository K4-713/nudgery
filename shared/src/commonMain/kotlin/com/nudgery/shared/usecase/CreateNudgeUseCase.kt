package com.nudgery.shared.usecase

import com.nudgery.shared.model.Nudge
import com.nudgery.shared.model.Question
import com.nudgery.shared.model.QuestionOption
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.Schedule
import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.repository.QuestionOptionRepository
import com.nudgery.shared.repository.QuestionRepository
import com.nudgery.shared.repository.ScheduleRepository
import com.nudgery.shared.scheduler.NotificationScheduler
import com.nudgery.shared.util.generateUuid
import kotlinx.datetime.Clock

private const val MAX_OPTIONS_PER_QUESTION = 16

class CreateNudgeUseCase(
    private val nudgeRepository: NudgeRepository,
    private val questionRepository: QuestionRepository,
    private val questionOptionRepository: QuestionOptionRepository,
    private val scheduleRepository: ScheduleRepository,
    private val notificationScheduler: NotificationScheduler
) {
    suspend fun execute(request: CreateNudgeRequest): CreateNudgeResult {
        if (request.mainQuestion.type == QuestionType.TEXT) {
            return CreateNudgeResult.Failure.MainQuestionCannotBeText
        }

        for (question in listOf(request.mainQuestion) + request.followUpQuestions) {
            if (question.type.isOptionType && question.options.size > MAX_OPTIONS_PER_QUESTION) {
                return CreateNudgeResult.Failure.TooManyOptions(question.text)
            }
        }

        val now = Clock.System.now()
        val nudgeId = generateUuid()
        val nudge = Nudge(
            id = nudgeId,
            name = request.name ?: request.mainQuestion.text,
            isEnabled = request.isEnabled,
            createdAt = now,
            updatedAt = now
        )
        nudgeRepository.insert(nudge)

        persistQuestion(nudgeId = nudgeId, questionRequest = request.mainQuestion, orderIndex = 0)

        request.followUpQuestions.forEachIndexed { index, questionReq ->
            persistQuestion(nudgeId = nudgeId, questionRequest = questionReq, orderIndex = index + 1)
        }

        val schedule = Schedule(
            id = generateUuid(),
            nudgeId = nudgeId,
            type = request.schedule.type,
            timeOfDay = request.schedule.timeOfDay,
            activeDaysOfWeek = request.schedule.activeDaysOfWeek,
            dayOfMonth = request.schedule.dayOfMonth,
            activeHours = request.schedule.activeHours
        )
        scheduleRepository.insert(schedule)

        if (request.isEnabled) {
            notificationScheduler.schedule(nudge, schedule)
        }

        return CreateNudgeResult.Success(nudgeId)
    }

    private suspend fun persistQuestion(
        nudgeId: String,
        questionRequest: QuestionRequest,
        orderIndex: Int
    ): String {
        val questionId = generateUuid()
        questionRepository.insert(
            Question(
                id = questionId,
                nudgeId = nudgeId,
                text = questionRequest.text,
                type = questionRequest.type,
                orderIndex = orderIndex,
                triggerAnswerValue = if (orderIndex == 0) null else questionRequest.triggerAnswerValue,
                triggerOperator = if (orderIndex == 0) null else questionRequest.triggerOperator
            )
        )
        if (questionRequest.type.isOptionType) {
            questionRequest.options.forEachIndexed { index, optionText ->
                questionOptionRepository.insert(
                    QuestionOption(
                        id = generateUuid(),
                        questionId = questionId,
                        text = optionText,
                        orderIndex = index
                    )
                )
            }
        }
        return questionId
    }
}
