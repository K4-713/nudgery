package com.nudgery.shared.util

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.nudgery.shared.db.NudgeryDatabase
import com.nudgery.shared.db.SqlDelightAnswerRepository
import com.nudgery.shared.db.SqlDelightNotificationFireRepository
import com.nudgery.shared.db.SqlDelightNudgeEditRepository
import com.nudgery.shared.db.SqlDelightTimezoneChangeEventRepository
import com.nudgery.shared.db.SqlDelightNudgeRepository
import com.nudgery.shared.db.SqlDelightQuestionOptionRepository
import com.nudgery.shared.db.SqlDelightQuestionRepository
import com.nudgery.shared.db.SqlDelightScheduleRepository
import com.nudgery.shared.repository.AnswerRepository
import com.nudgery.shared.repository.NotificationFireRepository
import com.nudgery.shared.repository.NudgeEditRepository
import com.nudgery.shared.repository.TimezoneChangeEventRepository
import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.repository.QuestionOptionRepository
import com.nudgery.shared.repository.QuestionRepository
import com.nudgery.shared.repository.ScheduleRepository

data class TestRepositories(
    val nudgeRepository: NudgeRepository,
    val questionRepository: QuestionRepository,
    val questionOptionRepository: QuestionOptionRepository,
    val scheduleRepository: ScheduleRepository,
    val answerRepository: AnswerRepository,
    val nudgeEditRepository: NudgeEditRepository,
    val notificationFireRepository: NotificationFireRepository,
    val timezoneChangeEventRepository: TimezoneChangeEventRepository
)

fun createTestRepositories(): TestRepositories {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    NudgeryDatabase.Schema.create(driver)
    val database = NudgeryDatabase(driver)
    return TestRepositories(
        nudgeRepository = SqlDelightNudgeRepository(database),
        questionRepository = SqlDelightQuestionRepository(database),
        questionOptionRepository = SqlDelightQuestionOptionRepository(database),
        scheduleRepository = SqlDelightScheduleRepository(database),
        answerRepository = SqlDelightAnswerRepository(database),
        nudgeEditRepository = SqlDelightNudgeEditRepository(database),
        notificationFireRepository = SqlDelightNotificationFireRepository(database),
        timezoneChangeEventRepository = SqlDelightTimezoneChangeEventRepository(database)
    )
}
