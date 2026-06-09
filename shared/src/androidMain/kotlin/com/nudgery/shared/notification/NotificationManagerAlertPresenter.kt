// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.notification

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat

private const val TAG = "AlertPresenter"

/**
 * Dismisses a nudge's posted alert via [NotificationManagerCompat], cancelling the same id the worker
 * posts under ([nudgeNotificationId]) so an answered nudge's notification leaves the shade (ED-18).
 * Cancelling a notification needs no runtime permission, so this is safe regardless of
 * POST_NOTIFICATIONS state.
 */
class NotificationManagerAlertPresenter(private val context: Context) : AlertPresenter {
    override fun dismissAlert(nudgeId: String) {
        NotificationManagerCompat.from(context).cancel(nudgeNotificationId(nudgeId))
        Log.i(TAG, "Dismissed alert for nudge $nudgeId")
    }
}
