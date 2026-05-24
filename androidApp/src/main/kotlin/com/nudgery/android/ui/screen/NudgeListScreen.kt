package com.nudgery.android.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nudgery.android.R
import com.nudgery.android.viewmodel.NudgeListViewModel
import com.nudgery.android.viewmodel.NudgeSummary
import kotlinx.datetime.Instant
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NudgeListScreen(
    onNudgeClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateToAnswerForm: (nudgeId: String, scheduledAt: Long?) -> Unit,
    viewModel: NudgeListViewModel = koinViewModel()
) {
    val nudges by viewModel.uiState.collectAsState()
    val pendingAnswer by viewModel.pendingAnswer.collectAsState()
    val exactAlarmGranted = rememberExactAlarmGranted()

    LaunchedEffect(pendingAnswer) {
        pendingAnswer?.let { pending ->
            viewModel.consumePendingAnswerNavigation()
            onNavigateToAnswerForm(pending.nudgeId, pending.scheduledAt?.toEpochMilliseconds())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.nudge_list_title))
                        Text(
                            text = stringResource(R.string.about_tagline),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.nav_settings)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (nudges.isNotEmpty()) {
                FloatingActionButton(onClick = onCreateClick) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.nudge_list_add)
                    )
                }
            }
        }
    ) { innerPadding ->
        if (nudges.isEmpty()) {
            EmptyNudgeList(
                onCreateClick = onCreateClick,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                itemsIndexed(nudges, key = { _, n -> n.nudgeId }) { _, nudge ->
                    NudgeListItem(
                        nudge = nudge,
                        exactAlarmGranted = exactAlarmGranted,
                        onClick = { onNudgeClick(nudge.nudgeId) },
                        onToggleEnabled = { viewModel.toggleEnabled(nudge.nudgeId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyNudgeList(onCreateClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize()
    ) {
        FloatingActionButton(
            onClick = onCreateClick,
            modifier = Modifier.size(120.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = stringResource(R.string.nudge_list_create_first),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun NudgeListItem(
    nudge: NudgeSummary,
    exactAlarmGranted: Boolean,
    onClick: () -> Unit,
    onToggleEnabled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showApproximateDialog by remember { mutableStateOf(false) }

    if (showApproximateDialog) {
        AlertDialog(
            onDismissRequest = { showApproximateDialog = false },
            title = { Text(stringResource(R.string.permission_approximate_time_title)) },
            text = { Text(stringResource(R.string.permission_approximate_time_body)) },
            confirmButton = {
                Button(onClick = { showApproximateDialog = false; openExactAlarmSettings(context) }) {
                    Text(stringResource(R.string.settings_exact_alarm_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showApproximateDialog = false }) {
                    Text(stringResource(R.string.action_dismiss))
                }
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Text(
                            text = nudge.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (nudge.hasMissedNotification) {
                            MissedDot(modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp))
                        }
                    }
                }
                Text(
                    text = nudge.scheduleDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!exactAlarmGranted && nudge.nextFireTimeApproximate != null) {
                    Text(
                        text = stringResource(R.string.nudge_next_fire, nudge.nextFireTimeApproximate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { showApproximateDialog = true }
                    )
                } else {
                    nudge.nextFireTime?.let { next ->
                        Text(
                            text = stringResource(R.string.nudge_next_fire, next),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = nudge.isEnabled,
                onCheckedChange = { onToggleEnabled() }
            )
        }
    }
}
