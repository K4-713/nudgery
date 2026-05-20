package com.nudgery.android

import android.app.Application
import android.content.Context
import android.util.Log
import com.nudgery.android.di.appModule
import com.nudgery.shared.di.sharedModule
import com.nudgery.shared.notification.KEY_LAST_TIMEZONE
import com.nudgery.shared.notification.NUDGERY_SYSTEM_PREFS
import com.nudgery.shared.notification.createNudgeNotificationChannel
import kotlinx.datetime.TimeZone
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
        seedLastKnownTimezone()
        Log.i(TAG, "Application initialized")
    }

    // Initializes the stored timezone on first install so the very first timezone-change
    // event has a real "from" value rather than defaulting to the new timezone.
    private fun seedLastKnownTimezone() {
        val prefs = getSharedPreferences(NUDGERY_SYSTEM_PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_LAST_TIMEZONE)) {
            val currentTz = TimeZone.currentSystemDefault().id
            prefs.edit().putString(KEY_LAST_TIMEZONE, currentTz).apply()
            Log.i(TAG, "Seeded last known timezone: $currentTz")
        }
    }
}
