// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.screen

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nudgery.android.R
import com.nudgery.android.ui.theme.emojiScaledStyle
import com.nudgery.android.viewmodel.NudgeListViewModel
import com.nudgery.android.viewmodel.NudgeSummary
import kotlinx.datetime.Instant
import org.koin.androidx.compose.koinViewModel

/** Bottom clearance reserved below the nudge list so the last card scrolls clear of the floating
 *  "New Nudge" button rather than being trapped under it: the 56dp FAB plus its 16dp margin top and
 *  bottom. */
private val NUDGE_LIST_FAB_CLEARANCE = 88.dp

/** Lift treatment for a picked-up nudge during drag-to-reorder (ED-19): a slight scale-up and a
 *  raised shadow so the card reads as floating above the list. */
private const val LIFTED_NUDGE_SCALE = 1.03f
private val LIFTED_NUDGE_ELEVATION = 12.dp

/** A jaunty clockwise tilt on the lifted card, pivoting 2/5 of the way along its width from the left
 *  (vertically centered). Tweak the angle/pivot to taste. */
private const val LIFTED_NUDGE_TILT_DEGREES = 11f
private const val LIFTED_NUDGE_TILT_PIVOT_X = 0.4f
private const val LIFTED_NUDGE_TILT_PIVOT_Y = 0.5f

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
            if (nudges?.isNotEmpty() == true) {
                FloatingActionButton(onClick = onCreateClick) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.nudge_list_add)
                    )
                }
            }
        }
    ) { innerPadding ->
        val currentNudges = nudges
        when {
            // Still loading: keep the screen empty so the empty-state button never flashes on launch.
            currentNudges == null -> Unit
            currentNudges.isEmpty() -> EmptyNudgeList(
                onCreateClick = onCreateClick,
                modifier = Modifier.padding(innerPadding)
            )
            else -> {
                val lazyListState = rememberLazyListState()
                val haptics = LocalHapticFeedback.current
                // Local working copy the drag reorders optimistically (ED-19, Phase 2); resynced from
                // the source list whenever we're not mid-drag, and persisted on drop.
                var localOrder by remember { mutableStateOf(currentNudges) }
                val dragDropState = rememberNudgeDragDropState(lazyListState) { from, to ->
                    localOrder = localOrder.moveItem(from, to)
                    // A light tick each time the order actually changes under the finger.
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                LaunchedEffect(currentNudges) {
                    if (dragDropState.draggingItemIndex == null) localOrder = currentNudges
                }

                LazyColumn(
                    state = lazyListState,
                    // Extra bottom padding so the last card can scroll clear of the floating "New Nudge"
                    // button instead of being trapped under it (the FAB's height is not folded into the
                    // Scaffold's innerPadding, unlike a bottomBar). Horizontal inset stays on the modifier.
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding() + NUDGE_LIST_FAB_CLEARANCE
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    itemsIndexed(localOrder, key = { _, n -> n.nudgeId }) { index, nudge ->
                        val isDragging = index == dragDropState.draggingItemIndex
                        // The lift (scale, tilt, shadow, accent wash) eases in/out on a spring rather
                        // than snapping, so picking a nudge up feels smooth. Position still follows the
                        // finger immediately — only the styling is animated.
                        val liftProgress by animateFloatAsState(
                            targetValue = if (isDragging) 1f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "nudgeLiftProgress"
                        )
                        val cardShape = MaterialTheme.shapes.medium
                        // The lifted card floats under the finger (translate/scale/tilt) while an
                        // accent-tinted outline stays behind it, marking the slot where it would land.
                        // The drag detector lives on the card itself (a stable node keyed by nudgeId)
                        // so picking up never depends on a pointer hit-test, and the active gesture
                        // survives the card's lift restyle.
                        Box(
                            modifier = Modifier
                                .then(if (isDragging) Modifier else Modifier.animateItem())
                                .zIndex(if (isDragging) 1f else 0f)
                                .fillMaxWidth()
                        ) {
                            if (liftProgress > 0f) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = liftProgress),
                                            shape = cardShape
                                        )
                                )
                            }
                            NudgeListItem(
                                nudge = nudge,
                                exactAlarmGranted = exactAlarmGranted,
                                onClick = { onNudgeClick(nudge.nudgeId) },
                                onToggleEnabled = { viewModel.toggleEnabled(nudge.nudgeId) },
                                liftProgress = liftProgress,
                                modifier = Modifier
                                    .graphicsLayer {
                                        translationY = if (isDragging) dragDropState.draggingItemOffset else 0f
                                        val scale = 1f + (LIFTED_NUDGE_SCALE - 1f) * liftProgress
                                        scaleX = scale
                                        scaleY = scale
                                        rotationZ = LIFTED_NUDGE_TILT_DEGREES * liftProgress
                                        transformOrigin = TransformOrigin(
                                            LIFTED_NUDGE_TILT_PIVOT_X,
                                            LIFTED_NUDGE_TILT_PIVOT_Y
                                        )
                                        shadowElevation = LIFTED_NUDGE_ELEVATION.toPx() * liftProgress
                                        shape = cardShape
                                        clip = false
                                    }
                                    .pointerInput(nudge.nudgeId) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                dragDropState.onDragStart(nudge.nudgeId)
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                if (dragDropState.onDrag(dragAmount)) {
                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                            },
                                            onDragEnd = {
                                                val newOrder = localOrder.map { it.nudgeId }
                                                dragDropState.onDragInterrupted()
                                                // Only persist when the order actually changed.
                                                if (newOrder != currentNudges.map { it.nudgeId }) {
                                                    viewModel.reorder(newOrder)
                                                }
                                            },
                                            onDragCancel = { dragDropState.onDragInterrupted() }
                                        )
                                    }
                            )
                        }
                    }
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
    modifier: Modifier = Modifier,
    liftProgress: Float = 0f
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

    // ED-19 lift: a faint accent wash that eases in with [liftProgress] while the card is picked up
    // for reordering. The raised shadow is applied by the caller's graphicsLayer so it animates too.
    val containerColor = lerp(
        CardDefaults.cardColors().containerColor,
        MaterialTheme.colorScheme.primaryContainer,
        liftProgress
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor)
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
                            style = emojiScaledStyle(nudge.name, MaterialTheme.typography.titleSmall),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (nudge.hasMissedNotification) {
                            MissedDot(modifier = Modifier.align(Alignment.TopEnd).offset(x = 23.dp, y = (-13).dp))
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
