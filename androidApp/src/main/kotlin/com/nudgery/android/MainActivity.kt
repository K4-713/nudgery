package com.nudgery.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nudgery.shared.notification.EXTRA_NUDGE_ID
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
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()

            NudgeryTheme(
                themePreference = settingsState.themePreference,
                boldText = settingsState.boldText
            ) {
                val navController = rememberNavController()
                val nudgeListViewModel: NudgeListViewModel = koinViewModel()

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
                            viewModel = nudgeListViewModel
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
        val nudgeId = intent.getStringExtra(EXTRA_NUDGE_ID) ?: return
        // Route notification taps through NudgeListViewModel so the NavHost can react
        // (The ViewModel is shared via Koin — MainActivity picks up the same instance)
    }
}
