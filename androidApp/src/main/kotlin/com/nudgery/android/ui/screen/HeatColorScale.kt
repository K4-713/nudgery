// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.screen

/**
 * Maps heat map cell values onto the palette gradient (DESIGN.md "Heat Map Value-to-Color Scaling").
 *
 * Scale questions build a [fixed] scale from their defined min/max so a given value keeps the same
 * color in every timeframe and window; Yes/No and number questions build one
 * [fromObservedValues], fitting the gradient to the cells currently in view. Zero renders as the
 * Null-vs-Zero "recorded zero" grey only when zero is the bottom of the range ([zeroIsBaseline]);
 * on ranges that extend below zero it is an ordinary gradient point.
 */
internal data class HeatColorScale(
    val min: Double,
    val max: Double,
    /** True when zero means "recorded, but nothing" and renders the zero grey instead of a palette color. */
    val zeroIsBaseline: Boolean
) {
    /**
     * Gradient position 0..1 for [value], or null when the cell should render the recorded-zero
     * grey instead of a palette color.
     */
    fun fractionFor(value: Double): Float? = when {
        zeroIsBaseline && value <= 0.0 -> null
        max <= min -> 0f  // degenerate range (e.g. every observed cell holds the same value)
        else -> ((value - min) / (max - min)).toFloat().coerceIn(0f, 1f)
    }

    companion object {
        /** A scale question's color scale: anchored to its defined bounds, identical in every view. */
        fun fixed(min: Double, max: Double): HeatColorScale =
            HeatColorScale(min, max, zeroIsBaseline = min == 0.0)

        /**
         * A count-style color scale fitted to the observed cell [values]: zero up to the hottest
         * cell (never less than 1, so faint fractional counts stay faint) when nothing is negative;
         * the full observed range — with zero as an ordinary point — when responses go negative.
         */
        fun fromObservedValues(values: List<Double>): HeatColorScale {
            val observedMin = values.minOrNull() ?: 0.0
            val zeroIsBaseline = observedMin >= 0.0
            val min = if (zeroIsBaseline) 0.0 else observedMin
            val max = values.maxOrNull() ?: 1.0
            return HeatColorScale(
                min = min,
                max = if (zeroIsBaseline) max.coerceAtLeast(1.0) else max,
                zeroIsBaseline = zeroIsBaseline
            )
        }
    }
}
