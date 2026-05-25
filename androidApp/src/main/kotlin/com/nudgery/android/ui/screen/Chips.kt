package com.nudgery.android.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nudgery.android.R

/** Alert icon used to signal a missed (unanswered) notification. */
@Composable
internal fun MissedDot(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_missed_alert),
        contentDescription = stringResource(R.string.nudge_missed_indicator),
        tint = Color.Unspecified,
        modifier = modifier.size(12.dp)
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
