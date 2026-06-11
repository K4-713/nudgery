// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Reduced-emphasis color factor for placeholder ("ghost") text, so an example or hint reads as
 * clearly less intense than text the user has actually typed.
 *
 * The app theme deliberately pins `onSurfaceVariant` to the full-strength `onSurface` color for
 * high contrast (see Theme.kt). That also cancels Material 3's built-in placeholder de-emphasis,
 * which is keyed off `onSurfaceVariant` — so placeholders must dim themselves explicitly. Only the
 * placeholder is dimmed; real secondary content stays at the theme's intended full contrast.
 */
private const val GHOST_TEXT_ALPHA = 0.6f

/**
 * A text-field placeholder rendered at reduced emphasis, to distinguish example/hint text from real
 * input. Inherits the field's placeholder text style (size, weight); only the color is dimmed.
 */
@Composable
fun GhostText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = GHOST_TEXT_ALPHA)
    )
}
