package com.nudgery.android

import android.app.Application
import android.util.Log
import com.nudgery.android.di.appModule
import com.nudgery.shared.di.sharedModule
import com.nudgery.shared.notification.createNudgeNotificationChannel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

private const val TAG = "NudgeryApplication"

class NudgeryApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNudgeNotificationChannel(this)
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@NudgeryApplication)
            modules(appModule, sharedModule)
        }
        Log.i(TAG, "Application initialized")
    }
}
