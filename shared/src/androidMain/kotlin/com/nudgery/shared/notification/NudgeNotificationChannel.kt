// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

fun createNudgeNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        NUDGE_CHANNEL_ID,
        "Nudge Prompts",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "Periodic nudges for your tracked questions"
    }
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)
}
