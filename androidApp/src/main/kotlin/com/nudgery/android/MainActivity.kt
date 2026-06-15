// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nudgery.android.ui.nav.ARG_INITIAL_STEP
import com.nudgery.android.ui.nav.ARG_NUDGE_ID
import com.nudgery.android.ui.nav.ARG_SCHEDULED_AT
import com.nudgery.android.ui.nav.NudgeryScreen
import com.nudgery.android.ui.screen.AboutScreen
import com.nudgery.android.ui.screen.AnswerFormScreen
import com.nudgery.android.ui.screen.CreateNudgeScreen
import com.nudgery.android.ui.screen.EditNudgeScreen
import com.nudgery.android.ui.screen.ExactAlarmRationaleEffect
import com.nudgery.android.ui.screen.LicensesScreen
import com.nudgery.android.ui.screen.NudgeDetailScreen
import com.nudgery.android.ui.screen.NudgeListScreen
import com.nudgery.android.ui.screen.ImportCollisionDialog
import com.nudgery.android.ui.screen.SettingsScreen
import androidx.compose.runtime.CompositionLocalProvider
import com.nudgery.android.viewmodel.CollisionResolution
import com.nudgery.android.viewmodel.ImportStatus
import com.nudgery.android.ui.theme.LocalEmojiScale
import com.nudgery.android.ui.theme.NudgeryTheme
import com.nudgery.android.viewmodel.NudgeListViewModel
import com.nudgery.android.viewmodel.SettingsViewModel
import com.nudgery.shared.notification.EXTRA_NUDGE_ID
import com.nudgery.shared.notification.EXTRA_SCHEDULED_AT
import kotlinx.datetime.Instant
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel as koinActivityViewModel

private const val TAG = "MainActivity"
private const val NAV_TRANSITION_DURATION_MS = 490

// Splash hold: the icon lingers at least this long, until the nudge list loads, capped for safety.
private const val MIN_SPLASH_HOLD_MS = 550L
private const val MAX_SPLASH_HOLD_MS = 2000L

class MainActivity : ComponentActivity() {

    // Hoisted so both onCreate (cold-start tap) and onNewIntent (warm tap) can reach it.
    private val nudgeListViewModel: NudgeListViewModel by koinActivityViewModel()
    private val settingsViewModel: SettingsViewModel by koinActivityViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Handle notification tap that cold-started the app.
        handleNudgeIntent(intent)
        handleNudgeFileIntent(intent)
        setContent {
            val settingsState by settingsViewModel.uiState.collectAsState()

            NudgeryTheme(
                themePreference = settingsState.themePreference,
                boldText = settingsState.boldText
            ) {
              CompositionLocalProvider(LocalEmojiScale provides settingsState.emojiScale) {
                NotificationPermissionEffect()
                ExactAlarmRationaleEffect()

                val navController = rememberNavController()
                val importSnackbarHostState = remember { SnackbarHostState() }

                // Activity-level import feedback — handles .nudge file imports and
                // Settings-initiated imports, visible from any screen.
                val importStatus = settingsState.importStatus
                val activityContext = LocalContext.current
                val importSuccessMessage = stringResource(R.string.settings_import_success)

                LaunchedEffect(importStatus) {
                    when (importStatus) {
                        is ImportStatus.BulkSuccess -> {
                            val message = if (importStatus.imported == 1 &&
                                importStatus.skipped == 0 && importStatus.failed == 0) {
                                importSuccessMessage
                            } else buildString {
                                append(activityContext.getString(R.string.settings_import_all_success, importStatus.imported))
                                if (importStatus.skipped > 0) {
                                    append(" ")
                                    append(activityContext.getString(R.string.settings_import_skipped, importStatus.skipped))
                                }
                                if (importStatus.failed > 0) {
                                    append(" ")
                                    append(activityContext.getString(R.string.settings_import_unreadable, importStatus.failed))
                                }
                            }
                            importSnackbarHostState.showSnackbar(message)
                            settingsViewModel.clearImportStatus()
                        }
                        is ImportStatus.Failure -> {
                            importSnackbarHostState.showSnackbar(
                                activityContext.getString(R.string.settings_import_failure, importStatus.message)
                            )
                            settingsViewModel.clearImportStatus()
                        }
                        else -> Unit
                    }
                }

                // Fix navigation — after importing a nudge with validation issues, open the editor.
                val fixNavigation by settingsViewModel.fixNavigation.collectAsState()
                LaunchedEffect(fixNavigation) {
                    val nav = fixNavigation ?: return@LaunchedEffect
                    settingsViewModel.clearFixNavigation()
                    navController.navigate(NudgeryScreen.EditNudge.createRoute(nav.nudgeId, initialStep = nav.editStep))
                }

                // Import collision dialog
                if (importStatus is ImportStatus.Collision) {
                    ImportCollisionDialog(
                        incomingName = importStatus.incomingName,
                        showRepeatForAll = importStatus.hasMore,
                        onResolve = { resolution, repeatForAll ->
                            settingsViewModel.resolveCollision(resolution, repeatForAll)
                        },
                        onDismiss = {
                            settingsViewModel.resolveCollision(CollisionResolution.SKIP, repeatForAll = false)
                        }
                    )
                }

                // Import validation fix dialog
                if (importStatus is ImportStatus.NeedsFix) {
                    AlertDialog(
                        onDismissRequest = { settingsViewModel.cancelInvalidImport() },
                        title = { Text(stringResource(R.string.import_fix_title)) },
                        text = { Text(stringResource(R.string.import_fix_body, importStatus.incomingName)) },
                        confirmButton = {
                            TextButton(onClick = { settingsViewModel.fixInvalidImport() }) {
                                Text(stringResource(R.string.import_fix_action))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { settingsViewModel.cancelInvalidImport() }) {
                                Text(stringResource(R.string.import_fix_cancel))
                            }
                        }
                    )
                }

                Box(Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = NudgeryScreen.NudgeList.route,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    enterTransition = { fadeIn(animationSpec = tween(NAV_TRANSITION_DURATION_MS)) },
                    exitTransition = { fadeOut(animationSpec = tween(NAV_TRANSITION_DURATION_MS)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(NAV_TRANSITION_DURATION_MS)) },
                    popExitTransition = { fadeOut(animationSpec = tween(NAV_TRANSITION_DURATION_MS)) }
                ) {
                    composable(NudgeryScreen.NudgeList.route) {
                        NudgeListScreen(
                            onNudgeClick = { nudgeId ->
                                navController.navigate(NudgeryScreen.NudgeDetail.createRoute(nudgeId))
                            },
                            onCreateClick = {
                                navController.navigate(NudgeryScreen.CreateNudge.route)
                            },
                            onSettingsClick = {
                                navController.navigate(NudgeryScreen.Settings.route)
                            },
                            onNavigateToAnswerForm = { nudgeId, scheduledAt ->
                                navController.navigate(NudgeryScreen.AnswerForm.createRoute(nudgeId, scheduledAt))
                            },
                            viewModel = nudgeListViewModel  // shared instance — also receives notification taps
                        )
                    }

                    composable(NudgeryScreen.CreateNudge.route) {
                        CreateNudgeScreen(
                            onDismiss = { navController.popBackStack() },
                            onSuccess = { navController.popBackStack() }
                        )
                    }

                    composable(NudgeryScreen.NudgeDetail.route) { backStackEntry ->
                        val nudgeId = backStackEntry.arguments?.getString(ARG_NUDGE_ID) ?: return@composable
                        NudgeDetailScreen(
                            nudgeId = nudgeId,
                            onBack = { navController.popBackStack() },
                            onEditClick = {
                                navController.navigate(NudgeryScreen.EditNudge.createRoute(nudgeId))
                            },
                            onEditFollowUpsClick = {
                                navController.navigate(NudgeryScreen.EditNudge.createRoute(nudgeId, initialStep = 1))
                            },
                            onEditScheduleClick = {
                                navController.navigate(NudgeryScreen.EditNudge.createRoute(nudgeId, initialStep = 2))
                            },
                            onAnswerNow = {
                                navController.navigate(NudgeryScreen.AnswerForm.createRoute(nudgeId))
                            }
                        )
                    }

                    composable(
                        route = NudgeryScreen.EditNudge.route,
                        arguments = listOf(
                            navArgument(ARG_NUDGE_ID) { type = NavType.StringType },
                            navArgument(ARG_INITIAL_STEP) { type = NavType.IntType; defaultValue = 0 }
                        )
                    ) { backStackEntry ->
                        val nudgeId = backStackEntry.arguments?.getString(ARG_NUDGE_ID) ?: return@composable
                        val initialStep = backStackEntry.arguments?.getInt(ARG_INITIAL_STEP) ?: 0
                        EditNudgeScreen(
                            nudgeId = nudgeId,
                            initialStep = initialStep,
                            onDismiss = { navController.popBackStack() },
                            onSuccess = { navController.popBackStack() }
                        )
                    }

                    composable(
                        NudgeryScreen.AnswerForm.route,
                        arguments = listOf(
                            navArgument(ARG_NUDGE_ID) { type = NavType.StringType },
                            navArgument(ARG_SCHEDULED_AT) { type = NavType.LongType; defaultValue = -1L }
                        )
                    ) { backStackEntry ->
                        val nudgeId = backStackEntry.arguments?.getString(ARG_NUDGE_ID) ?: return@composable
                        val scheduledAtMs = backStackEntry.arguments?.getLong(ARG_SCHEDULED_AT, -1L) ?: -1L
                        val scheduledAt = if (scheduledAtMs > 0) Instant.fromEpochMilliseconds(scheduledAtMs) else null
                        AnswerFormScreen(
                            nudgeId = nudgeId,
                            scheduledAt = scheduledAt,
                            onDismiss = { navController.popBackStack() }
                        )
                    }

                    composable(NudgeryScreen.Settings.route) {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onAboutClick = { navController.navigate(NudgeryScreen.About.route) },
                            viewModel = settingsViewModel
                        )
                    }

                    composable(NudgeryScreen.About.route) {
                        AboutScreen(
                            onBack = { navController.popBackStack() },
                            onLicensesClick = { navController.navigate(NudgeryScreen.Licenses.route) }
                        )
                    }

                    composable(NudgeryScreen.Licenses.route) {
                        LicensesScreen(onBack = { navController.popBackStack() })
                    }
                }

                SnackbarHost(
                    hostState = importSnackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                )
                } // Box
              }
            }
        }

        // Hold the system splash a beat longer — until the nudge list has loaded (and at least a
        // brief minimum) — so launch goes straight from the icon to the list, with no flash of the
        // empty-state button and no momentary blank. A hard cap guarantees it never sticks.
        val splashStart = SystemClock.uptimeMillis()
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                val elapsed = SystemClock.uptimeMillis() - splashStart
                val ready = elapsed >= MAX_SPLASH_HOLD_MS ||
                    (nudgeListViewModel.uiState.value != null && elapsed >= MIN_SPLASH_HOLD_MS)
                if (ready) content.viewTreeObserver.removeOnPreDrawListener(this)
                return ready
            }
        })
        content.postDelayed({ content.invalidate() }, MIN_SPLASH_HOLD_MS)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNudgeIntent(intent)
        handleNudgeFileIntent(intent)
    }

    private fun handleNudgeIntent(intent: Intent?) {
        val nudgeId = intent?.getStringExtra(EXTRA_NUDGE_ID) ?: return
        val scheduledAtMs = intent.getLongExtra(EXTRA_SCHEDULED_AT, -1L)
        val scheduledAt = if (scheduledAtMs > 0) Instant.fromEpochMilliseconds(scheduledAtMs) else null
        nudgeListViewModel.handleNotificationIntent(nudgeId, scheduledAt)
    }

    /** Handle ACTION_VIEW for a .nudge file: read the content and route to the import flow. */
    private fun handleNudgeFileIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        // Check file extension: only accept .nudge files. The display name may come from
        // the content provider (content:// URI) or the last path segment (file:// URI).
        val displayName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) cursor.getString(nameIndex) else null
            } else null
        } ?: uri.lastPathSegment
        if (displayName == null || !displayName.endsWith(".nudge")) {
            Log.d(TAG, "Ignoring non-.nudge file: $displayName")
            return
        }
        val jsonContent = try {
            contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
        } catch (e: Exception) {
            Log.w(TAG, "Could not read .nudge file", e)
            null
        }
        if (jsonContent != null) {
            Log.i(TAG, "Importing shared nudge from $displayName")
            settingsViewModel.importNudgeFromBackup(jsonContent)
        }
    }
}

@androidx.compose.runtime.Composable
private fun NotificationPermissionEffect() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.i(TAG, "POST_NOTIFICATIONS permission granted=$granted")
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
