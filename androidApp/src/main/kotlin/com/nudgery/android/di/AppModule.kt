package com.nudgery.android.di

import com.nudgery.shared.db.DatabaseDriverFactory
import com.nudgery.shared.scheduler.NotificationScheduler
import com.nudgery.shared.scheduler.WorkManagerNotificationScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { DatabaseDriverFactory(androidContext()) }
    single<NotificationScheduler> { WorkManagerNotificationScheduler(androidContext()) }
}
