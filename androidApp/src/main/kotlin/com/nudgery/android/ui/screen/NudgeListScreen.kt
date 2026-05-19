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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
    onClick: () -> Unit,
    onToggleEnabled: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    Text(
                        text = nudge.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // TODO: missed nudge indicator — requires ComputePreviousFireTimeUseCase
                    // When implemented: show a small golden-yellow exclamation badge rotated 32°
                }
                Text(
                    text = nudge.scheduleDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = nudge.isEnabled,
                onCheckedChange = { onToggleEnabled() }
            )
        }
    }
}
