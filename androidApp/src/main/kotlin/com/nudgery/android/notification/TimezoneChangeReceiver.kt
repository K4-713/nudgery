// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nudgery.shared.notification.KEY_CATCH_UP_MISSED
import com.nudgery.shared.notification.KEY_LAST_TIMEZONE
import com.nudgery.shared.notification.KEY_TIMEZONE_FROM
import com.nudgery.shared.notification.KEY_TIMEZONE_TO
import com.nudgery.shared.notification.NUDGERY_SYSTEM_PREFS
import com.nudgery.shared.notification.RescheduleAllNudgesWorker
import kotlinx.datetime.TimeZone

private const val TAG = "TimezoneChangeReceiver"

class TimezoneChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_TIMEZONE_CHANGED) return

        // EXTRA_TIMEZONE is available on API 31+; fall back to the system default (which
        // has already been updated by the time the broadcast is delivered).
        val newTzId = intent.getStringExtra(Intent.EXTRA_TIMEZONE)
            ?: TimeZone.currentSystemDefault().id

        val prefs = context.getSharedPreferences(NUDGERY_SYSTEM_PREFS, Context.MODE_PRIVATE)
        val oldTzId = prefs.getString(KEY_LAST_TIMEZONE, null) ?: newTzId
        prefs.edit().putString(KEY_LAST_TIMEZONE, newTzId).apply()

        Log.i(TAG, "Timezone changed: $oldTzId → $newTzId — rescheduling with catch-up")

        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<RescheduleAllNudgesWorker>()
                .setInputData(workDataOf(
                    KEY_CATCH_UP_MISSED to true,
                    KEY_TIMEZONE_FROM to oldTzId,
                    KEY_TIMEZONE_TO to newTzId
                ))
                .build()
        )
    }
}
