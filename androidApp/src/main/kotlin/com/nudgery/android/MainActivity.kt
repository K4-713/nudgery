package com.nudgery.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.nudgery.android.ui.screen.SettingsScreen
import androidx.compose.runtime.CompositionLocalProvider
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

class MainActivity : ComponentActivity() {

    // Hoisted so both onCreate (cold-start tap) and onNewIntent (warm tap) can reach it.
    private val nudgeListViewModel: NudgeListViewModel by koinActivityViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Handle notification tap that cold-started the app.
        handleNudgeIntent(intent)
        setContent {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()

            NudgeryTheme(
                themePreference = settingsState.themePreference,
                boldText = settingsState.boldText
            ) {
              CompositionLocalProvider(LocalEmojiScale provides settingsState.emojiScale) {
                NotificationPermissionEffect()
                ExactAlarmRationaleEffect()

                val navController = rememberNavController()

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
                            onAboutClick = { navController.navigate(NudgeryScreen.About.route) }
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
              }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNudgeIntent(intent)
    }

    private fun handleNudgeIntent(intent: Intent?) {
        val nudgeId = intent?.getStringExtra(EXTRA_NUDGE_ID) ?: return
        val scheduledAtMs = intent.getLongExtra(EXTRA_SCHEDULED_AT, -1L)
        val scheduledAt = if (scheduledAtMs > 0) Instant.fromEpochMilliseconds(scheduledAtMs) else null
        nudgeListViewModel.handleNotificationIntent(nudgeId, scheduledAt)
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
