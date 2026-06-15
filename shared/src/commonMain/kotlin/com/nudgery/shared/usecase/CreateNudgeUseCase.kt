// SPDX-License-Identifier: CC0-1.0

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

class CreateNudgeUseCase(
    private val nudgeRepository: NudgeRepository,
    private val questionRepository: QuestionRepository,
    private val questionOptionRepository: QuestionOptionRepository,
    private val scheduleRepository: ScheduleRepository,
    private val notificationScheduler: NotificationScheduler
) {
    suspend fun execute(rawRequest: CreateNudgeRequest): CreateNudgeResult {
        // Trim every user-typed field up front so untrimmed text can't reach storage (ED-16).
        val request = rawRequest.normalized()
        // ED-26: validate options, scale ranges, and follow-up triggers at the save boundary so bad
        // data can't reach storage even though the form already prevents it (defense in depth).
        validateNudgeQuestions(request.mainQuestion, request.followUpQuestions)?.let {
            return it.toCreateFailure()
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

        val mainOptionIds = persistQuestion(nudgeId = nudgeId, questionRequest = request.mainQuestion, orderIndex = 0)

        request.followUpQuestions.forEachIndexed { index, questionReq ->
            persistQuestion(nudgeId = nudgeId, questionRequest = questionReq, orderIndex = index + 1, mainOptionIds = mainOptionIds)
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
        orderIndex: Int,
        mainOptionIds: List<String> = emptyList()
    ): List<String> {
        val questionId = generateUuid()
        val triggerValue = when {
            orderIndex == 0 -> null
            mainOptionIds.isNotEmpty() -> {
                // Wizard stores option index as trigger; resolve to the actual persisted option ID.
                val index = questionRequest.triggerAnswerValue?.toIntOrNull()
                index?.let { mainOptionIds.getOrNull(it) } ?: questionRequest.triggerAnswerValue
            }
            else -> questionRequest.triggerAnswerValue
        }
        questionRepository.insert(
            Question(
                id = questionId,
                nudgeId = nudgeId,
                text = questionRequest.text,
                type = questionRequest.type,
                orderIndex = orderIndex,
                triggerAnswerValue = triggerValue,
                triggerOperator = if (orderIndex == 0) null else questionRequest.triggerOperator,
                scaleMin = if (questionRequest.type == QuestionType.SCALE) questionRequest.scaleMin else null,
                scaleMax = if (questionRequest.type == QuestionType.SCALE) questionRequest.scaleMax else null,
                collapsePerDay = questionRequest.type == QuestionType.YES_NO && questionRequest.collapsePerDay
            )
        )
        val optionIds = mutableListOf<String>()
        if (questionRequest.type.isOptionType) {
            questionRequest.options.forEachIndexed { index, optionText ->
                val optionId = generateUuid()
                optionIds += optionId
                questionOptionRepository.insert(
                    QuestionOption(
                        id = optionId,
                        questionId = questionId,
                        text = optionText,
                        orderIndex = index
                    )
                )
            }
        }
        return optionIds
    }
}

private fun QuestionValidationProblem.toCreateFailure(): CreateNudgeResult.Failure = when (this) {
    is QuestionValidationProblem.TooManyOptions -> CreateNudgeResult.Failure.TooManyOptions(questionText)
    is QuestionValidationProblem.NotEnoughOptions -> CreateNudgeResult.Failure.NotEnoughOptions(questionText)
    is QuestionValidationProblem.BlankOption -> CreateNudgeResult.Failure.BlankOption(questionText)
    // The existing InvalidScaleRange failure is a singleton without text; the offending text is in logs.
    is QuestionValidationProblem.InvalidScaleRange -> CreateNudgeResult.Failure.InvalidScaleRange
    is QuestionValidationProblem.MissingFollowUpTrigger -> CreateNudgeResult.Failure.MissingFollowUpTrigger(questionText)
}
