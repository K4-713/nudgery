package com.nudgery.shared.usecase

import com.nudgery.shared.model.NudgeEdit
import com.nudgery.shared.model.Nudge
import com.nudgery.shared.model.Question
import com.nudgery.shared.model.QuestionOption
import com.nudgery.shared.model.Schedule
import com.nudgery.shared.repository.NudgeEditRepository
import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.repository.QuestionOptionRepository
import com.nudgery.shared.repository.QuestionRepository
import com.nudgery.shared.repository.ScheduleRepository
import com.nudgery.shared.scheduler.NotificationScheduler
import com.nudgery.shared.util.generateUuid
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class UpdateNudgeUseCase(
    private val nudgeRepository: NudgeRepository,
    private val questionRepository: QuestionRepository,
    private val questionOptionRepository: QuestionOptionRepository,
    private val scheduleRepository: ScheduleRepository,
    private val nudgeEditRepository: NudgeEditRepository,
    private val notificationScheduler: NotificationScheduler
) {
    suspend fun execute(request: UpdateNudgeRequest): UpdateNudgeResult {
        val nudge = nudgeRepository.getById(request.nudgeId)
            ?: return UpdateNudgeResult.NudgeNotFound
        val questions = questionRepository.getByNudgeId(request.nudgeId)
        val mainQuestion = questions.firstOrNull { it.isMainQuestion }
        val existingSchedule = scheduleRepository.getByNudgeId(request.nudgeId)
        val now = Clock.System.now()

        val hasTextChanges = (request.mainQuestionText != null && request.mainQuestionText != mainQuestion?.text)
            || request.optionUpdates.isNotEmpty()

        if (hasTextChanges && request.splitEdit) {
            val newNudgeId = createSplitNudge(nudge, questions, request, existingSchedule, now)
            nudgeRepository.setEnabled(request.nudgeId, false)
            notificationScheduler.cancel(request.nudgeId)
            return UpdateNudgeResult.Success(newNudgeId)
        }

        var updatedNudge = nudge

        if (request.name != null && request.name != nudge.name) {
            updatedNudge = updatedNudge.copy(name = request.name, updatedAt = now)
        }

        if (request.isEnabled != null && request.isEnabled != nudge.isEnabled) {
            nudgeRepository.setEnabled(request.nudgeId, request.isEnabled)
            updatedNudge = updatedNudge.copy(isEnabled = request.isEnabled, updatedAt = now)
            if (!request.isEnabled) {
                notificationScheduler.cancel(request.nudgeId)
            } else if (existingSchedule != null) {
                notificationScheduler.reschedule(updatedNudge, existingSchedule)
            }
        }

        if (hasTextChanges && mainQuestion != null) {
            if (request.mainQuestionText != null && request.mainQuestionText != mainQuestion.text) {
                questionRepository.updateText(mainQuestion.id, request.mainQuestionText)
                nudgeEditRepository.insert(
                    NudgeEdit(
                        id = generateUuid(),
                        nudgeId = request.nudgeId,
                        editedAt = now,
                        fieldChanged = "question.text",
                        previousValue = mainQuestion.text
                    )
                )
                updatedNudge = updatedNudge.copy(updatedAt = now)
            }

            if (request.optionUpdates.isNotEmpty()) {
                val existingOptions = questionOptionRepository.getByQuestionId(mainQuestion.id)
                    .associateBy { it.id }
                for (optionUpdate in request.optionUpdates) {
                    val existingOption = existingOptions[optionUpdate.optionId] ?: continue
                    if (optionUpdate.newText == existingOption.text) continue
                    questionOptionRepository.updateText(optionUpdate.optionId, optionUpdate.newText)
                    nudgeEditRepository.insert(
                        NudgeEdit(
                            id = generateUuid(),
                            nudgeId = request.nudgeId,
                            editedAt = now,
                            fieldChanged = "option.text[${optionUpdate.optionId}]",
                            previousValue = existingOption.text
                        )
                    )
                    updatedNudge = updatedNudge.copy(updatedAt = now)
                }
            }
        }

        if (request.schedule != null && existingSchedule != null) {
            val updatedSchedule = existingSchedule.copy(
                type = request.schedule.type,
                timeOfDay = request.schedule.timeOfDay,
                activeDaysOfWeek = request.schedule.activeDaysOfWeek,
                dayOfMonth = request.schedule.dayOfMonth,
                activeHours = request.schedule.activeHours
            )
            scheduleRepository.update(updatedSchedule)
            if (updatedNudge.isEnabled) {
                notificationScheduler.reschedule(updatedNudge, updatedSchedule)
            }
            updatedNudge = updatedNudge.copy(updatedAt = now)
        }

        if (updatedNudge != nudge) {
            nudgeRepository.update(updatedNudge)
        }

        return UpdateNudgeResult.Success(request.nudgeId)
    }

    private suspend fun createSplitNudge(
        oldNudge: Nudge,
        oldQuestions: List<Question>,
        request: UpdateNudgeRequest,
        oldSchedule: Schedule?,
        now: Instant
    ): String {
        val newNudgeId = generateUuid()
        val newNudge = oldNudge.copy(
            id = newNudgeId,
            name = request.name ?: oldNudge.name,
            isEnabled = true,
            createdAt = now,
            updatedAt = now
        )
        nudgeRepository.insert(newNudge)

        val oldMainQuestion = oldQuestions.firstOrNull { it.isMainQuestion }
        if (oldMainQuestion != null) {
            val newMainQuestionId = generateUuid()
            questionRepository.insert(
                oldMainQuestion.copy(
                    id = newMainQuestionId,
                    nudgeId = newNudgeId,
                    text = request.mainQuestionText ?: oldMainQuestion.text
                )
            )

            val optionUpdateMap = request.optionUpdates.associateBy { it.optionId }
            questionOptionRepository.getByQuestionId(oldMainQuestion.id).forEach { option ->
                questionOptionRepository.insert(
                    option.copy(
                        id = generateUuid(),
                        questionId = newMainQuestionId,
                        text = optionUpdateMap[option.id]?.newText ?: option.text
                    )
                )
            }

            oldQuestions.filter { !it.isMainQuestion }.forEach { question ->
                val newQuestionId = generateUuid()
                questionRepository.insert(question.copy(id = newQuestionId, nudgeId = newNudgeId))
                if (question.type.isOptionType) {
                    questionOptionRepository.getByQuestionId(question.id).forEach { option ->
                        questionOptionRepository.insert(
                            option.copy(id = generateUuid(), questionId = newQuestionId)
                        )
                    }
                }
            }
        }

        val scheduleRequest = request.schedule ?: oldSchedule?.let {
            ScheduleRequest(
                type = it.type,
                timeOfDay = it.timeOfDay,
                activeDaysOfWeek = it.activeDaysOfWeek,
                dayOfMonth = it.dayOfMonth,
                activeHours = it.activeHours
            )
        }

        if (scheduleRequest != null) {
            val newSchedule = Schedule(
                id = generateUuid(),
                nudgeId = newNudgeId,
                type = scheduleRequest.type,
                timeOfDay = scheduleRequest.timeOfDay,
                activeDaysOfWeek = scheduleRequest.activeDaysOfWeek,
                dayOfMonth = scheduleRequest.dayOfMonth,
                activeHours = scheduleRequest.activeHours
            )
            scheduleRepository.insert(newSchedule)
            notificationScheduler.schedule(newNudge, newSchedule)
        }

        return newNudgeId
    }
}
