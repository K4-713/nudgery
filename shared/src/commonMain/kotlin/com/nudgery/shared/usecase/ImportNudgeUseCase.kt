package com.nudgery.shared.usecase

import com.nudgery.shared.model.Answer
import com.nudgery.shared.model.Nudge
import com.nudgery.shared.model.Question
import com.nudgery.shared.model.QuestionOption
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.Schedule
import com.nudgery.shared.repository.AnswerRepository
import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.repository.QuestionOptionRepository
import com.nudgery.shared.repository.QuestionRepository
import com.nudgery.shared.repository.ScheduleRepository
import com.nudgery.shared.scheduler.NotificationScheduler
import com.nudgery.shared.util.generateUuid
import kotlinx.datetime.Clock

class ImportNudgeUseCase(
    private val nudgeRepository: NudgeRepository,
    private val questionRepository: QuestionRepository,
    private val questionOptionRepository: QuestionOptionRepository,
    private val scheduleRepository: ScheduleRepository,
    private val answerRepository: AnswerRepository,
    private val notificationScheduler: NotificationScheduler
) {
    suspend fun execute(rawRequest: ImportNudgeRequest): String {
        // Import is a save boundary too: trim every restored text field up front (ED-16).
        val request = rawRequest.normalized()
        val now = Clock.System.now()
        val nudgeId = generateUuid()

        val nudge = Nudge(
            id = nudgeId,
            name = request.name,
            isEnabled = request.isEnabled,
            createdAt = now,
            updatedAt = now
        )
        nudgeRepository.insert(nudge)

        // Create questions in order, collecting: orderIndex→questionId and questionId→(optionText→optionId)
        val orderIndexToQuestionId = mutableMapOf<Int, String>()
        val questionIdToOptionTextMap = mutableMapOf<String, Map<String, String>>()

        request.questions.sortedBy { it.orderIndex }.forEach { questionReq ->
            val questionId = persistQuestion(nudgeId, questionReq, questionIdToOptionTextMap)
            orderIndexToQuestionId[questionReq.orderIndex] = questionId
        }

        // Create schedule and schedule notifications if enabled
        val schedule = request.schedule?.let { schedReq ->
            val s = Schedule(
                id = generateUuid(),
                nudgeId = nudgeId,
                type = schedReq.type,
                timeOfDay = schedReq.timeOfDay,
                activeDaysOfWeek = schedReq.activeDaysOfWeek,
                dayOfMonth = schedReq.dayOfMonth,
                activeHours = schedReq.activeHours
            )
            scheduleRepository.insert(s)
            s
        }

        if (request.isEnabled && schedule != null) {
            notificationScheduler.schedule(nudge, schedule)
        }

        // Create answers, resolving option texts back to new option UUIDs
        request.answers.forEach { answerReq ->
            val questionId = orderIndexToQuestionId[answerReq.questionOrderIndex] ?: return@forEach
            val questionReq = request.questions.find { it.orderIndex == answerReq.questionOrderIndex }
            val resolvedValue = resolveAnswerValue(
                rawValue = answerReq.value,
                questionType = questionReq?.type,
                optionTextMap = questionIdToOptionTextMap[questionId]
            )
            answerRepository.insert(
                Answer(
                    id = generateUuid(),
                    nudgeId = nudgeId,
                    questionId = questionId,
                    value = resolvedValue,
                    scheduledAt = answerReq.scheduledAt,
                    answeredAt = answerReq.answeredAt,
                    isHidden = false
                )
            )
        }

        return nudgeId
    }

    private suspend fun persistQuestion(
        nudgeId: String,
        questionReq: ImportQuestionRequest,
        questionIdToOptionTextMap: MutableMap<String, Map<String, String>>
    ): String {
        val questionId = generateUuid()
        val isMainQuestion = questionReq.orderIndex == 0

        // For option-type main questions, follow-up trigger values are stored as option texts in
        // the backup; resolve them to the new option UUIDs using the main question's option map.
        val triggerValue = when {
            isMainQuestion -> null
            questionReq.triggerAnswerValue == null -> null
            else -> {
                val mainQuestionId = questionIdToOptionTextMap.keys.firstOrNull()
                val mainOptionMap = mainQuestionId?.let { questionIdToOptionTextMap[it] }
                mainOptionMap?.get(questionReq.triggerAnswerValue) ?: questionReq.triggerAnswerValue
            }
        }

        questionRepository.insert(
            Question(
                id = questionId,
                nudgeId = nudgeId,
                text = questionReq.text,
                type = questionReq.type,
                orderIndex = questionReq.orderIndex,
                triggerAnswerValue = triggerValue,
                triggerOperator = if (isMainQuestion) null else questionReq.triggerOperator,
                scaleMin = if (questionReq.type == QuestionType.SCALE) questionReq.scaleMin else null,
                scaleMax = if (questionReq.type == QuestionType.SCALE) questionReq.scaleMax else null,
                collapsePerDay = questionReq.type == QuestionType.YES_NO && questionReq.collapsePerDay
            )
        )

        if (questionReq.type.isOptionType) {
            val textToId = mutableMapOf<String, String>()
            questionReq.options.forEachIndexed { index, optionText ->
                val optionId = generateUuid()
                textToId[optionText] = optionId
                questionOptionRepository.insert(
                    QuestionOption(
                        id = optionId,
                        questionId = questionId,
                        text = optionText,
                        orderIndex = index
                    )
                )
            }
            questionIdToOptionTextMap[questionId] = textToId
        }

        return questionId
    }

    private fun resolveAnswerValue(
        rawValue: String,
        questionType: QuestionType?,
        optionTextMap: Map<String, String>?
    ): String {
        if (questionType?.isOptionType != true || optionTextMap == null) return rawValue
        return rawValue.split(",")
            .map { it.trim() }
            .mapNotNull { optionTextMap[it] }
            .joinToString(",")
            .ifEmpty { rawValue }
    }
}
