// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for [HeatColorScale], the heat map's value→gradient mapping
 * (DESIGN.md "Heat Map Value-to-Color Scaling").
 */
class HeatColorScaleTest {

    @Test
    fun TDD_fixedScaleSpanningZero_treatsZeroAsAnOrdinaryValue() {
        // DESIGN.md "Heat Map Value-to-Color Scaling": on a scale that extends below zero, zero is
        // an ordinary mid-gradient value with a normal palette color — never the recorded-zero grey.
        val scale = HeatColorScale.fixed(-10.0, 10.0)

        assertEquals(0.5f, scale.fractionFor(0.0)!!, 1e-6f)
        assertEquals(0.0f, scale.fractionFor(-10.0)!!, 1e-6f)
        assertEquals(1.0f, scale.fractionFor(10.0)!!, 1e-6f)
        assertEquals(0.3f, scale.fractionFor(-4.0)!!, 1e-6f)
    }

    @Test
    fun TDD_fixedScaleStartingAtZero_keepsTheRecordedZeroGrey() {
        // DESIGN.md "Heat Map Value-to-Color Scaling": "On a scale defined from 0 upward, a zero
        // answer means 'bottom of scale' and keeps the Null-vs-Zero grey treatment."
        val scale = HeatColorScale.fixed(0.0, 10.0)

        assertNull(scale.fractionFor(0.0))
        assertEquals(0.5f, scale.fractionFor(5.0)!!, 1e-6f)
        assertEquals(1.0f, scale.fractionFor(10.0)!!, 1e-6f)
    }

    @Test
    fun TDD_fixedScaleAnchorsAtItsDefinedBounds_notTheHottestObservedCell() {
        // DESIGN.md "Heat Map Value-to-Color Scaling": "A given answer value is always the same
        // color regardless of timeframe, granularity, or where the window is scrubbed." A 7 on a
        // 0–10 scale sits at 0.7 even when it is the highest value on screen.
        val scale = HeatColorScale.fixed(0.0, 10.0)

        assertEquals(0.7f, scale.fractionFor(7.0)!!, 1e-6f)
        // A 1–10 scale anchors at its bottom, not at zero.
        assertEquals(0.0f, HeatColorScale.fixed(1.0, 10.0).fractionFor(1.0)!!, 1e-6f)
    }

    @Test
    fun TDD_fixedScaleClampsOutOfRangeValues() {
        val scale = HeatColorScale.fixed(-10.0, 10.0)

        assertEquals(1.0f, scale.fractionFor(35.0)!!, 1e-6f)
        assertEquals(0.0f, scale.fractionFor(-20.0)!!, 1e-6f)
    }

    @Test
    fun TDD_observedNonNegativeValues_anchorAtZero_withZeroGrey() {
        // DESIGN.md "Heat Map Value-to-Color Scaling": count-style gradients span "from zero up to
        // the hottest cell currently in view (zero keeps the recorded-zero grey)".
        val scale = HeatColorScale.fromObservedValues(listOf(0.0, 2.0, 4.0))

        assertNull(scale.fractionFor(0.0))
        assertEquals(0.5f, scale.fractionFor(2.0)!!, 1e-6f)
        assertEquals(1.0f, scale.fractionFor(4.0)!!, 1e-6f)
    }

    @Test
    fun TDD_observedNegativeValues_fitTheFullRange_withoutZeroGrey() {
        // DESIGN.md "Heat Map Value-to-Color Scaling": "If a number question's responses go
        // negative, the gradient extends down to the lowest visible cell instead of zero, and the
        // zero-grey treatment no longer applies."
        val scale = HeatColorScale.fromObservedValues(listOf(-4.0, 0.0, 4.0))

        assertEquals(0.0f, scale.fractionFor(-4.0)!!, 1e-6f)
        assertEquals(0.5f, scale.fractionFor(0.0)!!, 1e-6f)
        assertEquals(1.0f, scale.fractionFor(4.0)!!, 1e-6f)
    }

    @Test
    fun TDD_observedFractionalMaxKeepsTheLegacyMinimumAnchorOfOne() {
        // A lone sub-1 value (e.g. a 0.4 logged on a number question) still normalizes against a
        // top of 1, preserving the pre-existing behavior for faint counts.
        val scale = HeatColorScale.fromObservedValues(listOf(0.4))

        assertEquals(0.4f, scale.fractionFor(0.4)!!, 1e-6f)
    }

    @Test
    fun TDD_degenerateInputsDoNotDivideByZero() {
        // Empty cells and all-equal negative values must yield finite, clamped fractions.
        val empty = HeatColorScale.fromObservedValues(emptyList())
        assertEquals(1.0f, empty.fractionFor(3.0)!!, 1e-6f)

        val allEqualNegative = HeatColorScale.fromObservedValues(listOf(-3.0, -3.0))
        val fraction = allEqualNegative.fractionFor(-3.0)
        assertEquals(0.0f, fraction!!, 1e-6f)
    }
}
