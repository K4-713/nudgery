// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.screen

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nudgery.android.R

private const val PREFS_NUDGERY_SYSTEM = "nudgery_system"
private const val KEY_EXACT_ALARM_RATIONALE_SHOWN = "exact_alarm_rationale_shown"

// Returns true if the app currently holds exact alarm scheduling permission, or if the device
// is below API 31 where no permission is needed. Re-evaluates on every ON_RESUME so the
// indicator clears immediately when the user returns from system settings.
@Composable
internal fun rememberExactAlarmGranted(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    var granted by remember { mutableStateOf(alarmManager.canScheduleExactAlarms()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = alarmManager.canScheduleExactAlarms()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return granted
}

// Navigates the user to the system Alarm & Reminder settings page for this app, where they
// can grant SCHEDULE_EXACT_ALARM. No-op below API 31.
internal fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData(Uri.parse("package:${context.packageName}"))
        )
    }
}

// Shows a one-time rationale dialog on first launch when the exact alarm permission is not
// held on API 31+. Once the dialog is shown (regardless of user action), it is never shown
// again — the approximate-time indicators on the nudge list serve as the ongoing reminder.
@Composable
internal fun ExactAlarmRationaleEffect() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val context = LocalContext.current
    val exactAlarmGranted = rememberExactAlarmGranted()
    val prefs = remember { context.getSharedPreferences(PREFS_NUDGERY_SYSTEM, Context.MODE_PRIVATE) }
    var showDialog by remember {
        mutableStateOf(!prefs.getBoolean(KEY_EXACT_ALARM_RATIONALE_SHOWN, false) && !exactAlarmGranted)
    }

    if (showDialog) {
        val dismiss = {
            showDialog = false
            prefs.edit().putBoolean(KEY_EXACT_ALARM_RATIONALE_SHOWN, true).apply()
        }
        AlertDialog(
            onDismissRequest = { dismiss() },
            title = { Text(stringResource(R.string.permission_exact_alarm_rationale_title)) },
            text = { Text(stringResource(R.string.permission_exact_alarm_rationale_body)) },
            confirmButton = {
                Button(onClick = { dismiss(); openExactAlarmSettings(context) }) {
                    Text(stringResource(R.string.settings_exact_alarm_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { dismiss() }) {
                    Text(stringResource(R.string.permission_not_now))
                }
            }
        )
    }
}
