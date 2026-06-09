// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.di

import com.nudgery.shared.db.DatabaseDriverFactory
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
import com.nudgery.shared.usecase.CatchUpMissedFiresUseCase
import com.nudgery.shared.usecase.DeleteNudgeUseCase
import com.nudgery.shared.usecase.ComputeNextFireTimeUseCase
import com.nudgery.shared.usecase.CreateNudgeUseCase
import com.nudgery.shared.usecase.ExportAnswersUseCase
import com.nudgery.shared.usecase.GetVisualizationDataUseCase
import com.nudgery.shared.usecase.ImportNudgeUseCase
import com.nudgery.shared.usecase.RecordAnswerUseCase
import com.nudgery.shared.usecase.SetAnswerHiddenUseCase
import com.nudgery.shared.usecase.UpdateNudgeUseCase
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
    single<NotificationFireRepository> { SqlDelightNotificationFireRepository(get()) }
    single<TimezoneChangeEventRepository> { SqlDelightTimezoneChangeEventRepository(get()) }

    factory { CreateNudgeUseCase(get(), get(), get(), get(), get()) }
    factory { DeleteNudgeUseCase(get(), get()) }
    factory { UpdateNudgeUseCase(get(), get(), get(), get(), get(), get()) }
    factory { RecordAnswerUseCase(get(), get()) }
    factory { SetAnswerHiddenUseCase(get()) }
    factory { ComputeNextFireTimeUseCase() }
    factory { CatchUpMissedFiresUseCase(get(), get()) }
    factory { ExportAnswersUseCase(get(), get(), get(), get(), get()) }
    factory { ImportNudgeUseCase(get(), get(), get(), get(), get(), get()) }
    factory { GetVisualizationDataUseCase(get(), get(), get()) }
}
