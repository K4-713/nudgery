// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// Identifier for each available chart color palette.
enum class ChartPalettePreference { SPECTRUM, HORIZON, EMBER }

// Color stops for a palette in both dark-mode and light-mode contexts.
// Stops are evenly spaced from intensity 0.0 (cold / low data) to 1.0 (hot / max data).
// The empty-cell color (no data) is always the theme surfaceVariant — handled separately.
data class PaletteStops(
    val darkStops: List<Color>,
    val lightStops: List<Color>
) {
    fun colorAt(intensity: Float, isDark: Boolean): Color {
        val stops = if (isDark) darkStops else lightStops
        if (stops.size == 1) return stops[0]
        val pos = intensity.coerceIn(0f, 1f) * (stops.size - 1)
        val lo = pos.toInt().coerceIn(0, stops.size - 2)
        return lerp(stops[lo], stops[lo + 1], pos - lo)
    }
}

val ChartPalettePreference.paletteStops: PaletteStops
    get() = when (this) {
        // Full ROYGBV gradient. Brand yellow and both brand primaries appear as natural anchors.
        // Dark blue (#1840E0) is chosen over a teal-adjacent blue to maintain sufficient
        // perceptual distance from the teal stop under deuteranopia/protanopia simulation.
        ChartPalettePreference.SPECTRUM -> PaletteStops(
            darkStops = listOf(
                Color(0xFF6040A0),  // violet
                Color(0xFF1840E0),  // blue
                Color(0xFF2898A8),  // teal (brand family)
                Color(0xFF48B050),  // green
                Color(0xFFFFCC55),  // yellow (brand)
                Color(0xFFFF8030),  // orange
                Color(0xFFE03060)   // red
            ),
            lightStops = listOf(
                Color(0xFF5B3A8A),  // violet (brand)
                Color(0xFF2050C0),  // blue
                Color(0xFF1C7069),  // teal (brand)
                Color(0xFF358A30),  // green
                Color(0xFFA87800),  // dark amber (yellow fails WCAG on light bg)
                Color(0xFFD86020),  // brighter orange — increased luminance vs amber to preserve
                                    // adjacency under deuteranopia simulation (#C05010 was too close)
                Color(0xFFB01830)   // dark red
            )
        )

        // Blue-to-orange scale. Safe for deuteranopia and protanopia (red-green colour blindness)
        // because it avoids green entirely and spans the blue-to-orange axis, which is the most
        // robustly preserved hue contrast under those simulations. Also visually appealing as a
        // "twilight" or "sunset" scale for viewers without colour blindness.
        ChartPalettePreference.HORIZON -> PaletteStops(
            darkStops = listOf(
                Color(0xFF1A3878),  // navy
                Color(0xFF2860C8),  // blue
                Color(0xFF4898C8),  // teal-blue
                Color(0xFF80C0D8),  // light teal (brighter = higher intensity on dark bg)
                Color(0xFFF0A830),  // amber
                Color(0xFFE07020),  // orange
                Color(0xFFB04010)   // dark orange
            ),
            lightStops = listOf(
                Color(0xFF0A2878),  // deep navy
                Color(0xFF1058B8),  // blue
                Color(0xFF2080C8),  // medium blue
                Color(0xFF5098C0),  // lighter blue
                Color(0xFFA87000),  // dark amber
                Color(0xFFE08020),  // bright orange — increased luminance to maintain separation
                                    // from amber under deuteranopia (#C06010 was too close)
                Color(0xFF903010)   // dark orange-brown
            )
        )

        // Purple-to-red scale. Safe for tritanopia (blue-yellow colour blindness) because
        // it avoids the blue-yellow axis entirely, progressing through magenta and red instead.
        // Reads as a warm "ember" or "fire" scale for viewers without colour blindness.
        // Dark mode ends at warm peach (brightest cell = highest intensity).
        // Light mode ends at very dark red (darkest cell = highest intensity on light bg).
        ChartPalettePreference.EMBER -> PaletteStops(
            darkStops = listOf(
                Color(0xFF3A1050),  // deep plum
                Color(0xFF7030A0),  // purple
                Color(0xFFB83080),  // hot pink
                Color(0xFFD84040),  // red
                Color(0xFFEE7050),  // coral
                Color(0xFFF8A860),  // peach-orange
                Color(0xFFFAD090)   // warm peach (brightest = maximum intensity)
            ),
            lightStops = listOf(
                Color(0xFF6838A8),  // medium purple — R channel rises monotonically toward hot end,
                Color(0xFF881868),  // dark magenta    providing clear tritanopia separation (R is the
                Color(0xFFB01850),  // crimson          only reliably distinct channel under tritanopia)
                Color(0xFFC02030),  // dark red
                Color(0xFFB81010),  // deep red
                Color(0xFF980C0C),  // very deep red
                Color(0xFF700808)   // near-black red (darkest = maximum intensity on light bg)
            )
        )
    }
