package com.nudgery.android.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Device rebooted — rescheduling all active nudges")
            // TODO: reschedule all enabled nudges via NotificationScheduler
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
