package com.nudgery.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.nudgery.android.settings.ThemePreference

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
        content = content
    )
}
