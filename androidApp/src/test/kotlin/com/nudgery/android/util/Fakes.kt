package com.nudgery.android.util

import com.nudgery.shared.model.Answer
import com.nudgery.shared.model.NotificationFire
import com.nudgery.shared.model.NudgeEdit
import com.nudgery.shared.model.Nudge
import com.nudgery.shared.model.Question
import com.nudgery.shared.model.QuestionOption
import com.nudgery.shared.model.Schedule
import com.nudgery.shared.repository.AnswerRepository
import com.nudgery.shared.repository.NotificationFireRepository
import com.nudgery.shared.repository.NudgeEditRepository
import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.repository.QuestionOptionRepository
import com.nudgery.shared.repository.QuestionRepository
import com.nudgery.shared.repository.ScheduleRepository
import com.nudgery.shared.scheduler.NotificationScheduler
import com.nudgery.shared.usecase.CreateNudgeUseCase
import com.nudgery.shared.usecase.ExportAnswersUseCase
import com.nudgery.shared.usecase.GetVisualizationDataUseCase
import com.nudgery.shared.usecase.RecordAnswerUseCase
import com.nudgery.shared.usecase.SetAnswerHiddenUseCase
import com.nudgery.shared.usecase.UpdateNudgeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant

class FakeNudgeRepository : NudgeRepository {
    private val _nudges = MutableStateFlow<List<Nudge>>(emptyList())

    override fun observeAll(): Flow<List<Nudge>> = _nudges
    override suspend fun getById(id: String): Nudge? = _nudges.value.find { it.id == id }
    override suspend fun insert(nudge: Nudge) { _nudges.update { it + nudge } }
    override suspend fun update(nudge: Nudge) {
        _nudges.update { list -> list.map { if (it.id == nudge.id) nudge else it } }
    }
    override suspend fun setEnabled(nudgeId: String, isEnabled: Boolean) {
        _nudges.update { list ->
            list.map { if (it.id == nudgeId) it.copy(isEnabled = isEnabled) else it }
        }
    }
    override suspend fun delete(nudgeId: String) {
        _nudges.update { list -> list.filter { it.id != nudgeId } }
    }
}

class FakeQuestionRepository : QuestionRepository {
    private val questions = mutableListOf<Question>()

    override suspend fun getByNudgeId(nudgeId: String): List<Question> =
        questions.filter { it.nudgeId == nudgeId }.sortedBy { it.orderIndex }
    override suspend fun insert(question: Question) { questions.add(question) }
    override suspend fun updateText(questionId: String, text: String) {
        val i = questions.indexOfFirst { it.id == questionId }
        if (i >= 0) questions[i] = questions[i].copy(text = text)
    }
    override suspend fun update(question: Question) {
        val i = questions.indexOfFirst { it.id == question.id }
        if (i >= 0) questions[i] = question
    }
    override suspend fun deleteById(questionId: String) {
        questions.removeAll { it.id == questionId }
    }
    override suspend fun deleteByNudgeId(nudgeId: String) {
        questions.removeAll { it.nudgeId == nudgeId }
    }
}

class FakeQuestionOptionRepository : QuestionOptionRepository {
    private val options = mutableListOf<QuestionOption>()

    override suspend fun getByQuestionId(questionId: String): List<QuestionOption> =
        options.filter { it.questionId == questionId }.sortedBy { it.orderIndex }
    override suspend fun insert(option: QuestionOption) { options.add(option) }
    override suspend fun updateText(optionId: String, text: String) {
        val i = options.indexOfFirst { it.id == optionId }
        if (i >= 0) options[i] = options[i].copy(text = text)
    }
    override suspend fun deleteByQuestionId(questionId: String) {
        options.removeAll { it.questionId == questionId }
    }
}

class FakeScheduleRepository : ScheduleRepository {
    private val schedules = mutableListOf<Schedule>()

    override suspend fun getByNudgeId(nudgeId: String): Schedule? =
        schedules.find { it.nudgeId == nudgeId }
    override suspend fun insert(schedule: Schedule) { schedules.add(schedule) }
    override suspend fun update(schedule: Schedule) {
        val i = schedules.indexOfFirst { it.nudgeId == schedule.nudgeId }
        if (i >= 0) schedules[i] = schedule else schedules.add(schedule)
    }
    override suspend fun deleteByNudgeId(nudgeId: String) {
        schedules.removeAll { it.nudgeId == nudgeId }
    }
}

class FakeAnswerRepository : AnswerRepository {
    private val _answers = MutableStateFlow<List<Answer>>(emptyList())

    override fun observeByNudgeId(nudgeId: String): Flow<List<Answer>> =
        _answers.map { it.filter { a -> a.nudgeId == nudgeId } }
    override fun observeVisibleByNudgeId(nudgeId: String): Flow<List<Answer>> =
        _answers.map { it.filter { a -> a.nudgeId == nudgeId && !a.isHidden } }
    override suspend fun getAllByNudgeId(nudgeId: String): List<Answer> =
        _answers.value.filter { it.nudgeId == nudgeId }
    override suspend fun getVisibleByNudgeIdSince(nudgeId: String, since: Instant): List<Answer> =
        _answers.value.filter { it.nudgeId == nudgeId && !it.isHidden && it.scheduledAt >= since }
    override suspend fun insert(answer: Answer) { _answers.update { it + answer } }
    override suspend fun setHidden(answerId: String, isHidden: Boolean) {
        _answers.update { list ->
            list.map { if (it.id == answerId) it.copy(isHidden = isHidden) else it }
        }
    }
    override suspend fun getMostRecentAnsweredAtByNudgeId(nudgeId: String): Instant? =
        _answers.value.filter { it.nudgeId == nudgeId && !it.isHidden }.maxOfOrNull { it.answeredAt }
    override fun observeAll(): Flow<List<Answer>> = _answers
}

class FakeNotificationFireRepository : NotificationFireRepository {
    private val _fires = MutableStateFlow<List<NotificationFire>>(emptyList())

    override suspend fun insert(fire: NotificationFire) { _fires.update { it + fire } }
    override suspend fun getMostRecentByNudgeId(nudgeId: String): NotificationFire? =
        _fires.value.filter { it.nudgeId == nudgeId }.maxByOrNull { it.firedAt }
    override fun observeMostRecentByNudgeId(nudgeId: String): Flow<NotificationFire?> =
        _fires.map { it.filter { f -> f.nudgeId == nudgeId }.maxByOrNull { f -> f.firedAt } }
    override fun observeAll(): Flow<List<NotificationFire>> = _fires
}

class FakeNudgeEditRepository : NudgeEditRepository {
    private val edits = mutableListOf<NudgeEdit>()

    override suspend fun getByNudgeId(nudgeId: String): List<NudgeEdit> =
        edits.filter { it.nudgeId == nudgeId }
    override suspend fun insert(edit: NudgeEdit) { edits.add(edit) }
}

class FakeNotificationScheduler : NotificationScheduler {
    val scheduled = mutableListOf<Pair<Nudge, Schedule>>()
    val cancelled = mutableListOf<String>()
    val rescheduled = mutableListOf<Pair<Nudge, Schedule>>()

    override fun schedule(nudge: Nudge, schedule: Schedule) { scheduled.add(nudge to schedule) }
    override fun cancel(nudgeId: String) { cancelled.add(nudgeId) }
    override fun reschedule(nudge: Nudge, schedule: Schedule) { rescheduled.add(nudge to schedule) }

    fun reset() { scheduled.clear(); cancelled.clear(); rescheduled.clear() }
}

/** Bundles all fakes and use-case factories for ViewModel tests. */
class TestViewModelRepositories {
    val nudgeRepo = FakeNudgeRepository()
    val questionRepo = FakeQuestionRepository()
    val optionRepo = FakeQuestionOptionRepository()
    val scheduleRepo = FakeScheduleRepository()
    val answerRepo = FakeAnswerRepository()
    val nudgeEditRepo = FakeNudgeEditRepository()
    val notificationFireRepo = FakeNotificationFireRepository()
    val scheduler = FakeNotificationScheduler()

    fun createNudgeUseCase() =
        CreateNudgeUseCase(nudgeRepo, questionRepo, optionRepo, scheduleRepo, scheduler)
    fun updateNudgeUseCase() =
        UpdateNudgeUseCase(nudgeRepo, questionRepo, optionRepo, scheduleRepo, nudgeEditRepo, scheduler)
    fun setAnswerHiddenUseCase() = SetAnswerHiddenUseCase(answerRepo)
    fun exportAnswersUseCase() = ExportAnswersUseCase(nudgeRepo, questionRepo, optionRepo, answerRepo)
    fun getVisualizationDataUseCase() = GetVisualizationDataUseCase(answerRepo, questionRepo, optionRepo)
    fun recordAnswerUseCase() = RecordAnswerUseCase(answerRepo)
}
