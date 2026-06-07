package com.nudgery.android.ui.screen

import com.nudgery.shared.model.NamedCount
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression coverage for the packed-bubble chart's circle packing
 * (ARCHITECTURE.md: "Custom Canvas with d3-style front-chain circle packing").
 *
 * The chart's defining invariant is that bubbles never overlap — a bubble drawn on top of, or
 * hidden behind, another is the visible failure mode. A reversed front-chain initialization (the
 * bug this test guards against) let the collision walk miss intersections and stack circles.
 */
class BubblePackingTest {

    private fun circlesFor(counts: List<Int>): List<PackedCircle> =
        counts.map { count -> PackedCircle(NamedCount(label = "n$count", count = count), sqrt(count.toDouble())) }

    /** Number of pairs that overlap by more than a tiny epsilon (floating-point slack). */
    private fun overlappingPairs(circles: List<PackedCircle>): Int {
        var overlaps = 0
        for (i in circles.indices) {
            for (j in i + 1 until circles.size) {
                val a = circles[i]
                val b = circles[j]
                val gap = hypot(a.x - b.x, a.y - b.y) - (a.r + b.r)
                if (gap < -1e-3) overlaps++
            }
        }
        return overlaps
    }

    // TDD_ ARCHITECTURE.md ("d3-style front-chain circle packing"): no two packed bubbles overlap.
    @Test
    fun TDD_packedBubblesNeverOverlap_handPickedSets() {
        val sets = listOf(
            listOf(1),
            listOf(5, 5),
            listOf(9, 4, 1),
            listOf(10, 8, 6, 4, 2, 1),
            listOf(50, 1, 1, 1, 1, 1, 1, 1), // one giant bubble surrounded by tiny ones
            listOf(3, 3, 3, 3, 3, 3, 3, 3, 3, 3), // all equal — the case from the reported screenshot
            (1..40).toList()
        )
        for (counts in sets) {
            val circles = circlesFor(counts)
            packSiblings(circles)
            assertTrue(
                "Bubbles overlapped for counts=$counts",
                overlappingPairs(circles) == 0
            )
        }
    }

    // TDD_ ARCHITECTURE.md: the non-overlap invariant must hold for arbitrary frequency data, not
    // just curated sets — fuzz a wide range of sizes/magnitudes.
    @Test
    fun TDD_packedBubblesNeverOverlap_fuzz() {
        val random = Random(seed = 20260606)
        repeat(2_000) {
            val n = random.nextInt(1, 30)
            val counts = List(n) { random.nextInt(1, 100) }
            val circles = circlesFor(counts)
            packSiblings(circles)
            assertTrue(
                "Bubbles overlapped for counts=$counts",
                overlappingPairs(circles) == 0
            )
        }
    }
}
