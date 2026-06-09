// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

private const val TAG = "NudgeAlarmReceiver"

class NudgeAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val nudgeId = intent.getStringExtra(EXTRA_NUDGE_ID) ?: run {
            Log.e(TAG, "Alarm fired with no nudge ID in intent")
            return
        }
        val scheduledAtMs = intent.getLongExtra(EXTRA_SCHEDULED_AT, -1L)
        Log.i(TAG, "Alarm received for nudge $nudgeId scheduled at $scheduledAtMs")

        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<NudgeNotificationWorker>()
                .setInputData(workDataOf(
                    WORKER_KEY_NUDGE_ID to nudgeId,
                    WORKER_KEY_SCHEDULED_AT to scheduledAtMs
                ))
                .build()
        )
    }
}
