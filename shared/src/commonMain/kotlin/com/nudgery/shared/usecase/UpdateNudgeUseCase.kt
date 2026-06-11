// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.usecase

import com.nudgery.shared.model.NudgeEdit
import com.nudgery.shared.model.Nudge
import com.nudgery.shared.model.Question
import com.nudgery.shared.model.QuestionOption
import com.nudgery.shared.model.QuestionType
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
    suspend fun execute(rawRequest: UpdateNudgeRequest): UpdateNudgeResult {
        // Trim every user-typed field up front (ED-16). Normalizing before the change-detection
        // comparisons below means a trailing space alone never reads as an edit (which would write a
        // spurious edit-history entry and bump updatedAt).
        val request = rawRequest.normalized()
        val nudge = nudgeRepository.getById(request.nudgeId)
            ?: return UpdateNudgeResult.NudgeNotFound
        val questions = questionRepository.getByNudgeId(request.nudgeId)
        val mainQuestion = questions.firstOrNull { it.isMainQuestion }
        val existingSchedule = scheduleRepository.getByNudgeId(request.nudgeId)
        val now = Clock.System.now()

        // ED-26: backstop the follow-up replacements (each a full request) — their own config and
        // their trigger against the main question's type — before any change is written. The main
        // question's options are edited as deltas (not a full request), and that path is guarded by
        // the edit form (ED-23); there is no non-form route into update.
        mainQuestion?.let { main ->
            request.followUpReplacements?.forEach { replacement ->
                (replacement.request.configProblem() ?: triggerProblem(main.type, replacement.request))?.let {
                    return UpdateNudgeResult.InvalidQuestion(it)
                }
            }
        }

        val hasTextChanges = (request.mainQuestionText != null && request.mainQuestionText != mainQuestion?.text)
            || request.optionUpdates.isNotEmpty()
            || request.removedOptionIds.isNotEmpty()

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

        // "One Yes Per Day" (ED-17) is a display-only flag, never a split-worthy content change.
        // Reuse the full question update with the current (post-edit) text so a same-edit text change
        // isn't clobbered.
        if (request.mainQuestionCollapsePerDay != null && mainQuestion != null &&
            request.mainQuestionCollapsePerDay != mainQuestion.collapsePerDay
        ) {
            questionRepository.update(
                mainQuestion.copy(
                    text = request.mainQuestionText ?: mainQuestion.text,
                    collapsePerDay = request.mainQuestionCollapsePerDay
                )
            )
            updatedNudge = updatedNudge.copy(updatedAt = now)
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

        if (request.removedOptionIds.isNotEmpty() && mainQuestion != null) {
            request.removedOptionIds.forEach { questionOptionRepository.deleteById(it) }
            updatedNudge = updatedNudge.copy(updatedAt = now)
        }

        if (request.optionReorder != null && mainQuestion != null) {
            request.optionReorder.forEachIndexed { newIndex, optionId ->
                questionOptionRepository.updateOrderIndex(optionId, newIndex)
            }
            updatedNudge = updatedNudge.copy(updatedAt = now)
        }

        if (request.newOptions.isNotEmpty() && mainQuestion != null) {
            val existingOptions = questionOptionRepository.getByQuestionId(mainQuestion.id)
            val nextIndex = if (existingOptions.isEmpty()) 0 else existingOptions.maxOf { it.orderIndex } + 1
            request.newOptions.forEachIndexed { idx, text ->
                questionOptionRepository.insert(
                    QuestionOption(id = generateUuid(), questionId = mainQuestion.id, text = text, orderIndex = nextIndex + idx)
                )
            }
            updatedNudge = updatedNudge.copy(updatedAt = now)
        }

        if (request.followUpReplacements != null) {
            applyFollowUpReplacements(nudge = updatedNudge, replacements = request.followUpReplacements)
            updatedNudge = updatedNudge.copy(updatedAt = now)
        }

        if (updatedNudge != nudge) {
            nudgeRepository.update(updatedNudge)
        }

        return UpdateNudgeResult.Success(request.nudgeId)
    }

    private suspend fun applyFollowUpReplacements(nudge: Nudge, replacements: List<FollowUpReplacement>) {
        val allQuestions = questionRepository.getByNudgeId(nudge.id)
        val existingFollowUps = allQuestions.filter { !it.isMainQuestion }
        val existingFollowUpMap = existingFollowUps.associateBy { it.id }

        val mainQuestion = allQuestions.firstOrNull { it.isMainQuestion }
        val mainOptionIds = if (mainQuestion?.type?.isOptionType == true) {
            questionOptionRepository.getByQuestionId(mainQuestion.id)
                .sortedBy { it.orderIndex }
                .map { it.id }
        } else {
            emptyList()
        }

        // Delete follow-ups that were removed from the list
        val keptIds = replacements.mapNotNull { it.questionId }.toSet()
        existingFollowUps
            .filter { it.id !in keptIds }
            .forEach { removed ->
                questionOptionRepository.deleteByQuestionId(removed.id)
                questionRepository.deleteById(removed.id)
            }

        // Apply each replacement in order; orderIndex = position + 1 (main question is 0)
        replacements.forEachIndexed { index, replacement ->
            val orderIndex = index + 1
            val req = replacement.request
            val resolvedTriggerValue = resolveFollowUpTriggerValue(req.triggerAnswerValue, mainOptionIds)

            if (replacement.questionId != null) {
                val existing = existingFollowUpMap[replacement.questionId] ?: return@forEachIndexed
                questionRepository.update(
                    existing.copy(
                        text = req.text,
                        type = req.type,
                        orderIndex = orderIndex,
                        triggerAnswerValue = resolvedTriggerValue,
                        triggerOperator = req.triggerOperator,
                        scaleMin = if (req.type == QuestionType.SCALE) req.scaleMin else null,
                        scaleMax = if (req.type == QuestionType.SCALE) req.scaleMax else null,
                        collapsePerDay = req.type == QuestionType.YES_NO && req.collapsePerDay
                    )
                )
                val existingOptionTexts = questionOptionRepository.getByQuestionId(existing.id)
                    .sortedBy { it.orderIndex }
                    .map { it.text }
                val newOptionTexts = if (req.type.isOptionType) req.options else emptyList()
                if (newOptionTexts != existingOptionTexts) {
                    questionOptionRepository.deleteByQuestionId(existing.id)
                    newOptionTexts.forEachIndexed { optIdx, text ->
                        questionOptionRepository.insert(
                            QuestionOption(id = generateUuid(), questionId = existing.id, text = text, orderIndex = optIdx)
                        )
                    }
                }
            } else {
                val newQuestionId = generateUuid()
                questionRepository.insert(
                    Question(
                        id = newQuestionId,
                        nudgeId = nudge.id,
                        text = req.text,
                        type = req.type,
                        orderIndex = orderIndex,
                        triggerAnswerValue = resolvedTriggerValue,
                        triggerOperator = req.triggerOperator,
                        scaleMin = if (req.type == QuestionType.SCALE) req.scaleMin else null,
                        scaleMax = if (req.type == QuestionType.SCALE) req.scaleMax else null,
                        collapsePerDay = req.type == QuestionType.YES_NO && req.collapsePerDay
                    )
                )
                if (req.type.isOptionType) {
                    req.options.forEachIndexed { optIdx, text ->
                        questionOptionRepository.insert(
                            QuestionOption(id = generateUuid(), questionId = newQuestionId, text = text, orderIndex = optIdx)
                        )
                    }
                }
            }
        }
    }

    private fun resolveFollowUpTriggerValue(triggerValue: String?, mainOptionIds: List<String>): String? {
        if (triggerValue == null || mainOptionIds.isEmpty()) return triggerValue
        val index = triggerValue.toIntOrNull() ?: return triggerValue
        return mainOptionIds.getOrElse(index) { triggerValue }
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
                    text = request.mainQuestionText ?: oldMainQuestion.text,
                    collapsePerDay = request.mainQuestionCollapsePerDay ?: oldMainQuestion.collapsePerDay
                )
            )

            val optionUpdateMap = request.optionUpdates.associateBy { it.optionId }
            val existingOptions = questionOptionRepository.getByQuestionId(oldMainQuestion.id)
                .sortedBy { it.orderIndex }
                .filter { it.id !in request.removedOptionIds }
            val orderedOptions = request.optionReorder
                ?.mapNotNull { id -> existingOptions.find { it.id == id } }
                ?: existingOptions
            orderedOptions.forEachIndexed { newIndex, option ->
                questionOptionRepository.insert(
                    option.copy(
                        id = generateUuid(),
                        questionId = newMainQuestionId,
                        orderIndex = newIndex,
                        text = optionUpdateMap[option.id]?.newText ?: option.text
                    )
                )
            }
            request.newOptions.forEachIndexed { idx, text ->
                questionOptionRepository.insert(
                    QuestionOption(id = generateUuid(), questionId = newMainQuestionId, text = text, orderIndex = orderedOptions.size + idx)
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
