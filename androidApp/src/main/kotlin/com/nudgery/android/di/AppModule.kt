package com.nudgery.android.di

import com.nudgery.android.backup.NudgeBackupParser
import com.nudgery.android.settings.AppSettings
import com.nudgery.android.settings.DataStoreAppSettings
import com.nudgery.android.viewmodel.AnswerFormViewModel
import com.nudgery.android.viewmodel.CreateNudgeViewModel
import com.nudgery.android.viewmodel.EditNudgeViewModel
import com.nudgery.android.viewmodel.NudgeDetailViewModel
import com.nudgery.android.viewmodel.NudgeListViewModel
import com.nudgery.android.viewmodel.SettingsViewModel
import com.nudgery.shared.db.DatabaseDriverFactory
import com.nudgery.shared.scheduler.NotificationScheduler
import com.nudgery.shared.scheduler.WorkManagerNotificationScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { DatabaseDriverFactory(androidContext()) }
    single<NotificationScheduler> { WorkManagerNotificationScheduler(androidContext()) }
    single<AppSettings> { DataStoreAppSettings(androidContext()) }
    single { NudgeBackupParser() }

    viewModelOf(::NudgeListViewModel)
    viewModelOf(::CreateNudgeViewModel)
    viewModelOf(::NudgeDetailViewModel)
    viewModelOf(::EditNudgeViewModel)
    viewModelOf(::AnswerFormViewModel)
    viewModelOf(::SettingsViewModel)
}
