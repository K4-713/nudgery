// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.screen

import com.nudgery.shared.model.NamedCount
import kotlin.math.sqrt

/**
 * Scrub-transition geometry for the packed bubble chart (DESIGN.md "Packed bubble scrub
 * transitions"): when the shared window slides, the chart morphs between successive packings by
 * matching bubbles across windows by label. Pure pack-space math, kept out of the Compose screen
 * so the interpolation rules are unit-testable.
 */

/** One bubble's drawable state in pack space. [intensity] is its palette position (count ÷ window max). */
internal data class BubbleState(
    val label: String,
    val count: Int,
    val intensity: Float,
    val x: Float,
    val y: Float,
    val r: Float
)

/** Packs [entries] (see [packSiblings]) and captures the result as immutable bubble states. */
internal fun packedBubbleLayout(entries: List<NamedCount>): List<BubbleState> {
    if (entries.isEmpty()) return emptyList()
    val maxCount = entries.maxOf { it.count }.toFloat().coerceAtLeast(1f)
    val circles = entries.map { PackedCircle(it, sqrt(it.count.toDouble())) }
    packSiblings(circles)
    return circles.map {
        BubbleState(
            label = it.entry.label,
            count = it.entry.count,
            intensity = (it.entry.count / maxCount).coerceIn(0f, 1f),
            x = it.x.toFloat(),
            y = it.y.toFloat(),
            r = it.r.toFloat()
        )
    }
}

/**
 * The bubbles to draw at transition progress [t] (0 = the [from] layout, 1 = the [to] layout),
 * in draw order:
 * - departing bubbles (in [from] only) come first — beneath the survivors — deflating in place;
 * - bubbles in both layouts glide and resize, their intensity following; the count caption reads
 *   the destination count immediately (tweening numerals would imply data that never existed);
 * - new bubbles (in [to] only) inflate in place from nothing at their packed position.
 */
internal fun interpolateBubbles(
    from: List<BubbleState>,
    to: List<BubbleState>,
    t: Float
): List<BubbleState> {
    if (t >= 1f) return to
    val fromByLabel = from.associateBy { it.label }
    val toLabels = to.mapTo(HashSet()) { it.label }
    val bubbles = ArrayList<BubbleState>(from.size + to.size)
    from.forEach { old ->
        if (old.label !in toLabels) bubbles.add(old.copy(r = lerp(old.r, 0f, t)))
    }
    to.forEach { target ->
        val old = fromByLabel[target.label]
        bubbles.add(
            if (old == null) target.copy(r = lerp(0f, target.r, t))
            else target.copy(
                intensity = lerp(old.intensity, target.intensity, t),
                x = lerp(old.x, target.x, t),
                y = lerp(old.y, target.y, t),
                r = lerp(old.r, target.r, t)
            )
        )
    }
    return bubbles
}

private fun lerp(start: Float, stop: Float, t: Float): Float = start + (stop - start) * t
