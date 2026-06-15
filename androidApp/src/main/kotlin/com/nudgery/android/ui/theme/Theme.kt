// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.nudgery.android.settings.ThemePreference
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme

private val darkColorScheme = darkColorScheme(
    primary = ColorDarkViolet,
    onPrimary = ColorDarkOnViolet,
    primaryContainer = ColorDarkSurface,
    onPrimaryContainer = ColorDarkOnSurface,
    secondary = ColorDarkTeal,
    onSecondary = ColorDarkOnTeal,
    secondaryContainer = ColorDarkSurface,
    onSecondaryContainer = ColorDarkOnSurface,
    tertiary = ColorYellow,
    onTertiary = ColorOnYellow,
    background = ColorDarkBackground,
    onBackground = ColorDarkOnBackground,
    surface = ColorDarkSurface,
    onSurface = ColorDarkOnSurface,
    surfaceVariant = ColorDarkSurface,
    onSurfaceVariant = ColorDarkOnSurface,
    outline = ColorDarkOutline,
)

private val lightColorScheme = lightColorScheme(
    primary = ColorLightViolet,
    onPrimary = ColorLightOnViolet,
    primaryContainer = ColorLightSurface,
    onPrimaryContainer = ColorLightOnSurface,
    secondary = ColorLightTeal,
    onSecondary = ColorLightOnTeal,
    secondaryContainer = ColorLightSurface,
    onSecondaryContainer = ColorLightOnSurface,
    tertiary = ColorYellow,
    onTertiary = ColorOnYellow,
    background = ColorLightBackground,
    onBackground = ColorLightOnBackground,
    surface = ColorLightSurface,
    onSurface = ColorLightOnSurface,
    surfaceVariant = ColorLightSurface,
    onSurfaceVariant = ColorLightOnSurface,
    outline = ColorLightOutline,
)

private val nudgeryShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(50.dp),
)

@Composable
fun NudgeryTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    boldText: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme else lightColorScheme,
        typography = nudgeryTypography(boldText),
        shapes = nudgeryShapes,
    ) {
        ProvideVicoTheme(rememberM3VicoTheme(), content)
    }
}

/** How far the raised fill is blended from surface toward onSurface — subtle by design. */
private const val RAISED_SURFACE_FRACTION = 0.07f

/**
 * A subtly "raised" fill derived from the live theme: [ColorScheme.surface][androidx.compose.material3.ColorScheme.surface]
 * blended a little toward [onSurface][androidx.compose.material3.ColorScheme.onSurface], which makes
 * it a touch lighter in dark mode and a touch darker in light mode. Derived from the active scheme
 * (so it follows the in-app light/dark override) and stays on-palette rather than relying on the
 * uncustomized Material surface-container tones.
 */
@Composable
fun raisedSurfaceColor(fraction: Float = RAISED_SURFACE_FRACTION): Color =
    lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface, fraction)
