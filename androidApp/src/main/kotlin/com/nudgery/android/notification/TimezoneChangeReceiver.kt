package com.nudgery.android.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nudgery.shared.notification.RescheduleAllNudgesWorker

private const val TAG = "TimezoneChangeReceiver"

class TimezoneChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            Log.i(TAG, "Timezone changed — rescheduling all active nudges")
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<RescheduleAllNudgesWorker>().build()
            )
        }
    }
}
