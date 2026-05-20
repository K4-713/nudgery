package com.nudgery.android.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nudgery.shared.notification.KEY_CATCH_UP_MISSED
import com.nudgery.shared.notification.RescheduleAllNudgesWorker

private const val TAG = "BootReceiver"

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Device rebooted — checking for missed fires and rescheduling all active nudges")
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<RescheduleAllNudgesWorker>()
                    .setInputData(workDataOf(KEY_CATCH_UP_MISSED to true))
                    .build()
            )
        }
    }
}
