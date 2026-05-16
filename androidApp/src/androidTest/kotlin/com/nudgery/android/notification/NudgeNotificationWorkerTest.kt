package com.nudgery.android.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.nudgery.shared.db.NudgeryDatabase
import com.nudgery.shared.db.SqlDelightAnswerRepository
import com.nudgery.shared.db.SqlDelightNudgeEditRepository
import com.nudgery.shared.db.SqlDelightNudgeRepository
import com.nudgery.shared.db.SqlDelightQuestionOptionRepository
import com.nudgery.shared.db.SqlDelightQuestionRepository
import com.nudgery.shared.db.SqlDelightScheduleRepository
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.notification.NudgeNotificationWorker
import com.nudgery.shared.repository.AnswerRepository
import com.nudgery.shared.repository.NudgeEditRepository
import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.repository.QuestionOptionRepository
import com.nudgery.shared.repository.QuestionRepository
import com.nudgery.shared.repository.ScheduleRepository
import com.nudgery.shared.scheduler.NotificationScheduler
import com.nudgery.shared.scheduler.WorkManagerNotificationScheduler
import com.nudgery.shared.usecase.ComputeNextFireTimeUseCase
import com.nudgery.shared.usecase.CreateNudgeRequest
import com.nudgery.shared.usecase.CreateNudgeResult
import com.nudgery.shared.usecase.CreateNudgeUseCase
import com.nudgery.shared.usecase.QuestionRequest
import com.nudgery.shared.usecase.ScheduleRequest
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

// These mirror internal constants in shared/androidMain — kept as test-local literals to avoid
// coupling test code to implementation-private symbols.
private const val WORKER_INPUT_KEY_NUDGE_ID = "nudge_id"
private fun workerUniqueName(nudgeId: String) = "nudge_notification_$nudgeId"

@RunWith(AndroidJUnit4::class)
class NudgeNotificationWorkerTest {

    private lateinit var context: Context
    private lateinit var nudgeRepo: SqlDelightNudgeRepository
    private lateinit var questionRepo: SqlDelightQuestionRepository
    private lateinit var optionRepo: SqlDelightQuestionOptionRepository
    private lateinit var scheduleRepo: SqlDelightScheduleRepository
    private lateinit var workManager: WorkManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        workManager = WorkManager.getInstance(context)

        val driver = AndroidSqliteDriver(NudgeryDatabase.Schema, context, null)
        val database = NudgeryDatabase(driver)
        nudgeRepo = SqlDelightNudgeRepository(database)
        questionRepo = SqlDelightQuestionRepository(database)
        optionRepo = SqlDelightQuestionOptionRepository(database)
        scheduleRepo = SqlDelightScheduleRepository(database)
        val answerRepo = SqlDelightAnswerRepository(database)
        val nudgeEditRepo = SqlDelightNudgeEditRepository(database)

        stopKoin()
        startKoin {
            androidContext(context)
            modules(module {
                single<NudgeRepository> { nudgeRepo }
                single<QuestionRepository> { questionRepo }
                single<QuestionOptionRepository> { optionRepo }
                single<ScheduleRepository> { scheduleRepo }
                single<AnswerRepository> { answerRepo }
                single<NudgeEditRepository> { nudgeEditRepo }
                single<NotificationScheduler> { WorkManagerNotificationScheduler(context) }
                factory { ComputeNextFireTimeUseCase() }
            })
        }
    }

    @After
    fun teardown() {
        stopKoin()
    }

    @Test
    fun TDD_workerReturnsFailureWhenNudgeIdMissing() {
        // Defensive: worker must report failure when its input data is missing the nudge ID
        val worker = TestListenableWorkerBuilder<NudgeNotificationWorker>(context).build()

        val result = worker.startWork().get()

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun TDD_workerReturnsSuccessWhenNudgeNotFound() {
        // Graceful degradation: a deleted nudge should not cause the worker to crash or retry
        val worker = TestListenableWorkerBuilder<NudgeNotificationWorker>(context)
            .setInputData(workDataOf(WORKER_INPUT_KEY_NUDGE_ID to "nonexistent-nudge"))
            .build()

        val result = worker.startWork().get()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun TDD_workerSkipsNotificationForDisabledNudge() {
        // README: "Enabled Nudges will send you notifications" — disabled nudges must not fire
        val nudgeId = createNudge(isEnabled = false)
        val worker = TestListenableWorkerBuilder<NudgeNotificationWorker>(context)
            .setInputData(workDataOf(WORKER_INPUT_KEY_NUDGE_ID to nudgeId))
            .build()

        val result = worker.startWork().get()

        assertEquals(ListenableWorker.Result.success(), result)
        val followUp = workManager.getWorkInfosForUniqueWork(workerUniqueName(nudgeId)).get()
        assertTrue("Disabled nudge must not cause the worker to enqueue a follow-up notification",
            followUp.none { it.state == WorkInfo.State.ENQUEUED })
    }

    @Test
    fun TDD_workerReschedulesNextNotificationForEnabledNudge() {
        // README: notifications recur on schedule — worker must re-enqueue itself after each fire
        val nudgeId = createNudge(isEnabled = true)
        // Cancel the initial scheduled work so the assertion tests only the worker's own reschedule
        workManager.cancelUniqueWork(workerUniqueName(nudgeId)).result.get()
        workManager.pruneWork().result.get()

        val worker = TestListenableWorkerBuilder<NudgeNotificationWorker>(context)
            .setInputData(workDataOf(WORKER_INPUT_KEY_NUDGE_ID to nudgeId))
            .build()

        val result = worker.startWork().get()

        assertEquals(ListenableWorker.Result.success(), result)
        val followUp = workManager.getWorkInfosForUniqueWork(workerUniqueName(nudgeId)).get()
        assertTrue("Worker must enqueue the next notification after firing",
            followUp.any { it.state == WorkInfo.State.ENQUEUED })
    }

    private fun createNudge(isEnabled: Boolean): String = runBlocking {
        val scheduler = WorkManagerNotificationScheduler(context)
        val createNudge = CreateNudgeUseCase(nudgeRepo, questionRepo, optionRepo, scheduleRepo, scheduler)
        (createNudge.execute(
            CreateNudgeRequest(
                mainQuestion = QuestionRequest("How are you today?", QuestionType.YES_NO),
                schedule = ScheduleRequest(
                    type = ScheduleType.HOURLY,
                    timeOfDay = LocalTime(0, 0),
                    activeHours = (0..23).toSet()
                ),
                isEnabled = isEnabled
            )
        ) as CreateNudgeResult.Success).nudgeId
    }
}
