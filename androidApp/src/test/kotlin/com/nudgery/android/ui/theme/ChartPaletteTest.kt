package com.nudgery.android.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow
import kotlin.math.sqrt

// Standard simplified colorblind simulation matrices (Viénot 1999 approximation).
// These operate on linearised sRGB — strictly correct simulation requires gamma expansion first,
// but for palette-design validation the sRGB approximation is sufficient to flag adjacency issues.
//
// Documents: Viénot F, Brettel H, Mollon JD (1999) "Digital video colourmaps for checking the
// legibility of displays by dichromats." Color Research & Application 24(4):243-252.

class ChartPaletteTest {

    private fun simulateProtanopia(c: Color) = Color(
        red   = (0.567f * c.red + 0.433f * c.green).coerceIn(0f, 1f),
        green = (0.558f * c.red + 0.442f * c.green).coerceIn(0f, 1f),
        blue  = (0.242f * c.green + 0.758f * c.blue).coerceIn(0f, 1f)
    )

    private fun simulateDeuteranopia(c: Color) = Color(
        red   = (0.625f * c.red + 0.375f * c.green).coerceIn(0f, 1f),
        green = (0.700f * c.red + 0.300f * c.green).coerceIn(0f, 1f),
        blue  = (0.300f * c.green + 0.700f * c.blue).coerceIn(0f, 1f)
    )

    private fun simulateTritanopia(c: Color) = Color(
        red   = (0.950f * c.red + 0.050f * c.green).coerceIn(0f, 1f),
        green = (0.433f * c.green + 0.567f * c.blue).coerceIn(0f, 1f),
        blue  = (0.475f * c.green + 0.525f * c.blue).coerceIn(0f, 1f)
    )

    // Euclidean distance in sRGB space — practical for palette adjacency checks on sequential scales
    private fun colorDistance(a: Color, b: Color): Float {
        val dr = a.red - b.red
        val dg = a.green - b.green
        val db = a.blue - b.blue
        return sqrt((dr * dr + dg * dg + db * db).toDouble()).toFloat()
    }

    private fun assertMinimumSeparation(
        stops: List<Color>,
        simulate: (Color) -> Color,
        minDistance: Float,
        context: String
    ) {
        val simulated = stops.map(simulate)
        for (i in 0 until simulated.size - 1) {
            val dist = colorDistance(simulated[i], simulated[i + 1])
            assertTrue(
                "$context: stops $i and ${i + 1} are too similar after simulation " +
                "(distance=$dist, minimum=$minDistance). " +
                "Original colours: ${stops[i]} → ${stops[i + 1]}",
                dist >= minDistance
            )
        }
    }

    // ── Spectrum (default ROYGBV) ────────────────────────────────────────────────────────────────

    @Test
    fun TDD_spectrumDarkPaletteHasMinimumSeparationUnderDeuteranopia() {
        // Documents: Spectrum dark stops remain distinguishable under deuteranopia simulation.
        // The blue stop (#1840E0) is deliberately bluer than a standard ROYGBV blue to maintain
        // adequate distance from the adjacent teal stop under this simulation.
        assertMinimumSeparation(
            ChartPalettePreference.SPECTRUM.paletteStops.darkStops,
            ::simulateDeuteranopia,
            minDistance = 0.06f,
            "Spectrum dark / deuteranopia"
        )
    }

    @Test
    fun TDD_spectrumDarkPaletteHasMinimumSeparationUnderProtanopia() {
        assertMinimumSeparation(
            ChartPalettePreference.SPECTRUM.paletteStops.darkStops,
            ::simulateProtanopia,
            minDistance = 0.06f,
            "Spectrum dark / protanopia"
        )
    }

    @Test
    fun TDD_spectrumLightPaletteHasMinimumSeparationUnderDeuteranopia() {
        assertMinimumSeparation(
            ChartPalettePreference.SPECTRUM.paletteStops.lightStops,
            ::simulateDeuteranopia,
            minDistance = 0.06f,
            "Spectrum light / deuteranopia"
        )
    }

    // ── Horizon (deuteranopia / protanopia safe) ─────────────────────────────────────────────────

    @Test
    fun TDD_horizonDarkPaletteHasStrongSeparationUnderDeuteranopia() {
        // Documents: Horizon palette is specifically designed for deuteranopia safety.
        // Higher threshold than Spectrum reflects the intentional design goal.
        assertMinimumSeparation(
            ChartPalettePreference.HORIZON.paletteStops.darkStops,
            ::simulateDeuteranopia,
            minDistance = 0.10f,
            "Horizon dark / deuteranopia"
        )
    }

    @Test
    fun TDD_horizonDarkPaletteHasStrongSeparationUnderProtanopia() {
        assertMinimumSeparation(
            ChartPalettePreference.HORIZON.paletteStops.darkStops,
            ::simulateProtanopia,
            minDistance = 0.10f,
            "Horizon dark / protanopia"
        )
    }

    @Test
    fun TDD_horizonLightPaletteHasStrongSeparationUnderDeuteranopia() {
        assertMinimumSeparation(
            ChartPalettePreference.HORIZON.paletteStops.lightStops,
            ::simulateDeuteranopia,
            minDistance = 0.10f,
            "Horizon light / deuteranopia"
        )
    }

    // ── Ember (tritanopia safe) ──────────────────────────────────────────────────────────────────

    @Test
    fun TDD_emberDarkPaletteHasStrongSeparationUnderTritanopia() {
        // Documents: Ember palette is specifically designed for tritanopia safety.
        assertMinimumSeparation(
            ChartPalettePreference.EMBER.paletteStops.darkStops,
            ::simulateTritanopia,
            minDistance = 0.10f,
            "Ember dark / tritanopia"
        )
    }

    @Test
    fun TDD_emberLightPaletteHasStrongSeparationUnderTritanopia() {
        assertMinimumSeparation(
            ChartPalettePreference.EMBER.paletteStops.lightStops,
            ::simulateTritanopia,
            minDistance = 0.08f,
            "Ember light / tritanopia"
        )
    }

    // ── Interpolation correctness ────────────────────────────────────────────────────────────────

    @Test
    fun TDD_colorInterpolationStaysWithinBounds() {
        // Documents: colorAt() never produces channel values outside [0, 1]
        ChartPalettePreference.entries.forEach { pref ->
            listOf(0f, 0.01f, 0.25f, 0.5f, 0.75f, 0.99f, 1f).forEach { intensity ->
                listOf(true, false).forEach { isDark ->
                    val c = pref.paletteStops.colorAt(intensity, isDark)
                    assertTrue("$pref isDark=$isDark intensity=$intensity: red ${c.red} out of range", c.red in 0f..1f)
                    assertTrue("$pref isDark=$isDark intensity=$intensity: green ${c.green} out of range", c.green in 0f..1f)
                    assertTrue("$pref isDark=$isDark intensity=$intensity: blue ${c.blue} out of range", c.blue in 0f..1f)
                }
            }
        }
    }

    @Test
    fun TDD_allPalettesHaveAtLeastTwoStops() {
        ChartPalettePreference.entries.forEach { pref ->
            val stops = pref.paletteStops
            assertTrue("${pref}.darkStops must have ≥ 2 stops", stops.darkStops.size >= 2)
            assertTrue("${pref}.lightStops must have ≥ 2 stops", stops.lightStops.size >= 2)
        }
    }

    @Test
    fun TDD_coldEndAndHotEndAreDistinct() {
        // Documents: the minimum-intensity and maximum-intensity colours are meaningfully different
        val minSeparation = 0.30f
        ChartPalettePreference.entries.forEach { pref ->
            val dark = pref.paletteStops.darkStops
            assertTrue(
                "$pref dark: cold end and hot end are too similar",
                colorDistance(dark.first(), dark.last()) >= minSeparation
            )
            val light = pref.paletteStops.lightStops
            assertTrue(
                "$pref light: cold end and hot end are too similar",
                colorDistance(light.first(), light.last()) >= minSeparation
            )
        }
    }
}
