// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.screen

import com.nudgery.shared.model.NamedCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the packed bubble chart's scrub-transition interpolation
 * (DESIGN.md "Packed bubble scrub transitions").
 */
class BubbleTransitionTest {

    private fun bubble(
        label: String,
        x: Float = 0f,
        y: Float = 0f,
        r: Float = 10f,
        count: Int = 5,
        intensity: Float = 0.5f
    ) = BubbleState(label = label, count = count, intensity = intensity, x = x, y = y, r = r)

    @Test
    fun TDD_persistingBubblesGlideAndResizeBetweenLayouts() {
        // DESIGN.md "Packed bubble scrub transitions": "A bubble present in both windows glides to
        // its new packed position while its radius and its magnitude color interpolate."
        val from = listOf(bubble("coffee", x = 0f, y = 0f, r = 10f, intensity = 1.0f))
        val to = listOf(bubble("coffee", x = 20f, y = -10f, r = 20f, intensity = 0.5f))

        val mid = interpolateBubbles(from, to, t = 0.5f).single()

        assertEquals(10f, mid.x, 1e-4f)
        assertEquals(-5f, mid.y, 1e-4f)
        assertEquals(15f, mid.r, 1e-4f)
        assertEquals(0.75f, mid.intensity, 1e-4f)
    }

    @Test
    fun TDD_persistingBubbleCaptionReadsTheDestinationCountImmediately() {
        // DESIGN.md: "Its count caption reads the destination count immediately — numerals ticking
        // through intermediate values would imply data that never existed."
        val from = listOf(bubble("coffee", count = 4))
        val to = listOf(bubble("coffee", count = 9))

        assertEquals(9, interpolateBubbles(from, to, t = 0.25f).single().count)
    }

    @Test
    fun TDD_newBubblesInflateInPlaceAtTheirPackedPosition() {
        // DESIGN.md: "A bubble new to the window inflates in place from nothing at its packed
        // position."
        val to = listOf(bubble("tea", x = 30f, y = 40f, r = 12f))

        val start = interpolateBubbles(emptyList(), to, t = 0f).single()
        assertEquals(0f, start.r, 1e-4f)
        assertEquals(30f, start.x, 1e-4f)
        assertEquals(40f, start.y, 1e-4f)

        val mid = interpolateBubbles(emptyList(), to, t = 0.5f).single()
        assertEquals(6f, mid.r, 1e-4f)
        assertEquals(30f, mid.x, 1e-4f)
    }

    @Test
    fun TDD_departingBubblesDeflateInPlaceBeneathTheSurvivors() {
        // DESIGN.md: "a departing bubble deflates in place, drawn beneath the surviving bubbles" —
        // list order is draw order, so departing bubbles must come first.
        val from = listOf(
            bubble("stays", x = 0f, r = 10f),
            bubble("goes", x = 50f, y = 60f, r = 8f)
        )
        val to = listOf(bubble("stays", x = 0f, r = 10f))

        val mid = interpolateBubbles(from, to, t = 0.5f)
        assertEquals(2, mid.size)
        assertEquals("goes", mid[0].label)      // drawn first = beneath
        assertEquals(4f, mid[0].r, 1e-4f)       // half deflated
        assertEquals(50f, mid[0].x, 1e-4f)      // in place
        assertEquals(60f, mid[0].y, 1e-4f)
        assertEquals("stays", mid[1].label)
    }

    @Test
    fun TDD_completedTransitionIsExactlyTheTargetLayout() {
        // At t = 1 the chart must render the new window's packing exactly: departed bubbles are
        // gone and survivors sit at their final geometry.
        val from = listOf(bubble("stays", x = 0f, r = 10f), bubble("goes", x = 50f, r = 8f))
        val to = listOf(bubble("stays", x = 20f, r = 15f), bubble("tea", x = -20f, r = 5f))

        assertEquals(to, interpolateBubbles(from, to, t = 1f))
    }

    @Test
    fun TDD_packedBubbleLayoutCarriesCountsIntoPackSpaceWithoutOverlap() {
        // The layout step feeds the transition: every entry becomes one bubble whose intensity is
        // its count relative to the window max, packed with no overlaps (the chart's invariant,
        // see BubblePackingTest).
        val layout = packedBubbleLayout(
            listOf(NamedCount("big", 9), NamedCount("small", 1))
        )

        assertEquals(2, layout.size)
        assertEquals(1.0f, layout.first { it.label == "big" }.intensity, 1e-4f)
        val a = layout[0]; val b = layout[1]
        val centerDistance = kotlin.math.hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble())
        assertTrue(
            "bubbles must not overlap",
            centerDistance >= (a.r + b.r) - 1e-3
        )
    }
}
