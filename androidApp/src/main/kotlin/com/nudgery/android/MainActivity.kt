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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nudgery.android.ui.nav.ARG_NUDGE_ID
import com.nudgery.android.ui.nav.NudgeryScreen
import com.nudgery.android.ui.screen.AboutScreen
import com.nudgery.android.ui.screen.AnswerFormScreen
import com.nudgery.android.ui.screen.CreateNudgeScreen
import com.nudgery.android.ui.screen.EditNudgeScreen
import com.nudgery.android.ui.screen.NudgeDetailScreen
import com.nudgery.android.ui.screen.NudgeListScreen
import com.nudgery.android.ui.screen.SettingsScreen
import com.nudgery.android.ui.theme.NudgeryTheme
import com.nudgery.android.viewmodel.NudgeListViewModel
import com.nudgery.android.viewmodel.SettingsViewModel
import com.nudgery.shared.notification.EXTRA_NUDGE_ID
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel as koinActivityViewModel

private const val TAG = "MainActivity"

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
                NotificationPermissionEffect()

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = NudgeryScreen.NudgeList.route
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
                            onNavigateToAnswerForm = { nudgeId ->
                                navController.navigate(NudgeryScreen.AnswerForm.createRoute(nudgeId))
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
                            onAnswerNow = {
                                navController.navigate(NudgeryScreen.AnswerForm.createRoute(nudgeId))
                            }
                        )
                    }

                    composable(NudgeryScreen.EditNudge.route) { backStackEntry ->
                        val nudgeId = backStackEntry.arguments?.getString(ARG_NUDGE_ID) ?: return@composable
                        EditNudgeScreen(
                            nudgeId = nudgeId,
                            onDismiss = { navController.popBackStack() },
                            onSuccess = { navController.popBackStack() }
                        )
                    }

                    composable(NudgeryScreen.AnswerForm.route) { backStackEntry ->
                        val nudgeId = backStackEntry.arguments?.getString(ARG_NUDGE_ID) ?: return@composable
                        AnswerFormScreen(
                            nudgeId = nudgeId,
                            scheduledAt = null,
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
                        AboutScreen(onBack = { navController.popBackStack() })
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
        Log.i(TAG, "Notification tap received for nudge $nudgeId")
        nudgeListViewModel.handleNotificationIntent(nudgeId)
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
