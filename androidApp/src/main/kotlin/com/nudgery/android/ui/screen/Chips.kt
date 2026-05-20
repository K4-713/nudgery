package com.nudgery.android.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nudgery.android.R

/** Small amber dot used to signal a missed (unanswered) notification. */
@Composable
internal fun MissedDot(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.nudge_missed_indicator)
    Box(
        modifier = modifier
            .size(8.dp)
            .background(Color(0xFFFFC107), CircleShape)
            .semantics { contentDescription = description }
    )
}

/**
 * Toggle chip used throughout the app for mutually-exclusive and multi-select choices
 * (schedule type, day of week, question type, timeframe, etc.).
 *
 * Selected state: background drops to the darkest surface (colorScheme.background),
 * text is full onBackground, and a primary-colored border marks the selection clearly.
 *
 * Unselected state: background rises to colorScheme.surface (noticeably lighter in dark
 * mode, slightly more saturated in light mode), and text is rendered at 38% alpha so
 * it reads as greyed out against the lighter background.
 */
@Composable
internal fun NudgeryToggleChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            selectedContainerColor = MaterialTheme.colorScheme.background,
            selectedLabelColor = MaterialTheme.colorScheme.onBackground,
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        ),
    )
}
