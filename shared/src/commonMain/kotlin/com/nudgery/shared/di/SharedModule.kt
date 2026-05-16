package com.nudgery.shared.di

import com.nudgery.shared.db.DatabaseDriverFactory
import com.nudgery.shared.db.NudgeryDatabase
import com.nudgery.shared.db.SqlDelightAnswerRepository
import com.nudgery.shared.db.SqlDelightNudgeEditRepository
import com.nudgery.shared.db.SqlDelightNudgeRepository
import com.nudgery.shared.db.SqlDelightQuestionOptionRepository
import com.nudgery.shared.db.SqlDelightQuestionRepository
import com.nudgery.shared.db.SqlDelightScheduleRepository
import com.nudgery.shared.repository.AnswerRepository
import com.nudgery.shared.repository.NudgeEditRepository
import com.nudgery.shared.repository.NudgeRepository
import com.nudgery.shared.repository.QuestionOptionRepository
import com.nudgery.shared.repository.QuestionRepository
import com.nudgery.shared.repository.ScheduleRepository
import org.koin.dsl.module

val sharedModule = module {
    single { get<DatabaseDriverFactory>().createDriver() }
    single { NudgeryDatabase(get()) }

    single<NudgeRepository> { SqlDelightNudgeRepository(get()) }
    single<QuestionRepository> { SqlDelightQuestionRepository(get()) }
    single<QuestionOptionRepository> { SqlDelightQuestionOptionRepository(get()) }
    single<ScheduleRepository> { SqlDelightScheduleRepository(get()) }
    single<AnswerRepository> { SqlDelightAnswerRepository(get()) }
    single<NudgeEditRepository> { SqlDelightNudgeEditRepository(get()) }
}
