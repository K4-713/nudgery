// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.nudgery.shared.db.NudgeryDatabase
import com.nudgery.shared.db.SqlDelightAnswerRepository
import com.nudgery.shared.db.SqlDelightNotificationFireRepository
import com.nudgery.shared.db.SqlDelightNudgeEditRepository
import com.nudgery.shared.db.SqlDelightNudgeRepository
import com.nudgery.shared.db.SqlDelightQuestionOptionRepository
import com.nudgery.shared.db.SqlDelightQuestionRepository
import com.nudgery.shared.db.SqlDelightScheduleRepository
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.notification.NudgeAlarmReceiver
import com.nudgery.shared.notification.NudgeNotificationWorker
import com.nudgery.shared.repository.AnswerRepository
import com.nudgery.shared.repository.NotificationFireRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

// Mirror of the internal constant — kept here to avoid coupling test code to private symbols.
private const val WORKER_INPUT_KEY_NUDGE_ID = "nudge_id"

@RunWith(AndroidJUnit4::class)
class NudgeNotificationWorkerTest {

    private lateinit var context: Context
    private lateinit var nudgeRepo: SqlDelightNudgeRepository
    private lateinit var questionRepo: SqlDelightQuestionRepository
    private lateinit var optionRepo: SqlDelightQuestionOptionRepository
    private lateinit var scheduleRepo: SqlDelightScheduleRepository
    private lateinit var alarmManager: AlarmManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        alarmManager = context.getSystemService(AlarmManager::class.java)

        val driver = AndroidSqliteDriver(NudgeryDatabase.Schema, context, null)
        val database = NudgeryDatabase(driver)
        nudgeRepo = SqlDelightNudgeRepository(database)
        questionRepo = SqlDelightQuestionRepository(database)
        optionRepo = SqlDelightQuestionOptionRepository(database)
        scheduleRepo = SqlDelightScheduleRepository(database)
        val answerRepo = SqlDelightAnswerRepository(database)
        val notificationFireRepo = SqlDelightNotificationFireRepository(database)
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
                single<NotificationFireRepository> { notificationFireRepo }
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
    fun TDD_workerSkipsRescheduleForDisabledNudge() {
        // README: "Enabled Nudges will send you notifications" — disabled nudges must not reschedule
        val nudgeId = createNudge(isEnabled = false)
        cancelAlarm(nudgeId) // ensure no alarm exists from creation

        val worker = TestListenableWorkerBuilder<NudgeNotificationWorker>(context)
            .setInputData(workDataOf(WORKER_INPUT_KEY_NUDGE_ID to nudgeId))
            .build()

        val result = worker.startWork().get()

        assertEquals(ListenableWorker.Result.success(), result)
        assertNull(
            "Disabled nudge must not cause the worker to schedule a follow-up alarm",
            findAlarmPendingIntent(nudgeId)
        )
    }

    @Test
    fun TDD_workerReschedulesNextAlarmForEnabledNudge() {
        // README: notifications recur on schedule — worker must reschedule itself after each fire
        val nudgeId = createNudge(isEnabled = true)
        cancelAlarm(nudgeId) // remove the alarm set during creation so we test only the worker's reschedule

        val worker = TestListenableWorkerBuilder<NudgeNotificationWorker>(context)
            .setInputData(workDataOf(WORKER_INPUT_KEY_NUDGE_ID to nudgeId))
            .build()

        val result = worker.startWork().get()

        assertEquals(ListenableWorker.Result.success(), result)
        assertNotNull(
            "Worker must schedule the next alarm after firing",
            findAlarmPendingIntent(nudgeId)
        )

        cancelAlarm(nudgeId) // clean up
    }

    private fun findAlarmPendingIntent(nudgeId: String): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            nudgeId.hashCode(),
            Intent(context, NudgeAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

    private fun cancelAlarm(nudgeId: String) {
        findAlarmPendingIntent(nudgeId)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
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
