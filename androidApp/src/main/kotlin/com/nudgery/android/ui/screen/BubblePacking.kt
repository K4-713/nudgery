package com.nudgery.android.ui.screen

import com.nudgery.shared.model.NamedCount
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Tangential circle packing for the packed-bubble chart, ported from d3-hierarchy's front-chain
 * `packSiblings`. Kept in its own file (rather than buried in the Compose screen) so the geometry
 * is unit-testable: the chart's defining invariant is that no two bubbles overlap.
 */

/** A circle in the packing layout. Position is filled in by [packSiblings]; [r] encodes frequency. */
internal class PackedCircle(val entry: NamedCount, val r: Double) {
    var x: Double = 0.0
    var y: Double = 0.0
}

internal class PackBounds(val minX: Double, val minY: Double, val maxX: Double, val maxY: Double)

/** Front-chain node wrapping a [PackedCircle] in a circular doubly-linked list. */
private class PackNode(val circle: PackedCircle) {
    var prev: PackNode? = null
    var next: PackNode? = null
}

/**
 * Positions [circles] tangentially with no overlaps, nestled around a shared center, by porting
 * d3-hierarchy's front-chain `packSiblings` algorithm. Mutates each circle's x/y in place.
 */
internal fun packSiblings(circles: List<PackedCircle>) {
    val n = circles.size
    if (n == 0) return

    val a = circles[0]
    a.x = 0.0
    a.y = 0.0
    if (n == 1) return

    val b = circles[1]
    a.x = -b.r
    b.x = a.r
    b.y = 0.0
    if (n == 2) return

    val c = circles[2]
    placeTangent(b, a, c)

    // Initialize the front chain with the first three circles. The forward (next) cycle must run
    // a -> b -> c to match the rotational direction in which placeTangent positions new circles
    // and in which the collision walk below scans; reversing it lets the walk miss intersections,
    // so a circle can be placed on top of an already-placed one (a bubble hidden behind another).
    var nodeA = PackNode(a)
    var nodeB = PackNode(b)
    val nodeC = PackNode(c)
    nodeA.next = nodeB; nodeA.prev = nodeC
    nodeB.next = nodeC; nodeB.prev = nodeA
    nodeC.next = nodeA; nodeC.prev = nodeB

    var i = 3
    outer@ while (i < n) {
        val circle = circles[i]
        placeTangent(nodeA.circle, nodeB.circle, circle)
        val newNode = PackNode(circle)

        // Walk outward from both ends of the chain, looking for the nearest collision.
        var j = nodeB.next!!
        var k = nodeA.prev!!
        var sj = nodeB.circle.r
        var sk = nodeA.circle.r
        while (true) {
            if (sj <= sk) {
                if (intersects(j.circle, circle)) {
                    nodeB = j
                    nodeA.next = nodeB; nodeB.prev = nodeA
                    continue@outer // retry placing the same circle without advancing i
                }
                sj += j.circle.r
                j = j.next!!
            } else {
                if (intersects(k.circle, circle)) {
                    nodeA = k
                    nodeA.next = nodeB; nodeB.prev = nodeA
                    continue@outer
                }
                sk += k.circle.r
                k = k.prev!!
            }
            if (j === k.next) break
        }

        // No collision: splice the new circle into the chain between nodeA and nodeB.
        val oldB = nodeB
        newNode.prev = nodeA; newNode.next = oldB
        nodeA.next = newNode; oldB.prev = newNode
        nodeB = newNode

        // Recompute the pair on the chain whose weighted midpoint is closest to the center,
        // so the next circle grows from there.
        var bestScore = score(nodeA)
        var scan = newNode.next!!
        while (scan !== newNode) {
            val s = score(scan)
            if (s < bestScore) {
                nodeA = scan
                bestScore = s
            }
            scan = scan.next!!
        }
        nodeB = nodeA.next!!
        i++
    }
}

/** Positions [target] tangent to the already-placed circles [c1] and [c2]. */
private fun placeTangent(c1: PackedCircle, c2: PackedCircle, target: PackedCircle) {
    val dx = c1.x - c2.x
    val dy = c1.y - c2.y
    val d2 = dx * dx + dy * dy
    if (d2 != 0.0) {
        val a2 = (c2.r + target.r) * (c2.r + target.r)
        val b2 = (c1.r + target.r) * (c1.r + target.r)
        if (a2 > b2) {
            val x = (d2 + b2 - a2) / (2 * d2)
            val y = sqrt(max(0.0, b2 / d2 - x * x))
            target.x = c1.x - x * dx - y * dy
            target.y = c1.y - x * dy + y * dx
        } else {
            val x = (d2 + a2 - b2) / (2 * d2)
            val y = sqrt(max(0.0, a2 / d2 - x * x))
            target.x = c2.x + x * dx - y * dy
            target.y = c2.y + x * dy + y * dx
        }
    } else {
        target.x = c2.x + target.r
        target.y = c2.y
    }
}

private fun intersects(a: PackedCircle, b: PackedCircle): Boolean {
    val dr = a.r + b.r - 1e-6
    val dx = b.x - a.x
    val dy = b.y - a.y
    return dr > 0.0 && dr * dr > dx * dx + dy * dy
}

/** Squared distance from the center to the radius-weighted midpoint of a node and its successor. */
private fun score(node: PackNode): Double {
    val a = node.circle
    val b = node.next!!.circle
    val ab = a.r + b.r
    val dx = (a.x * b.r + b.x * a.r) / ab
    val dy = (a.y * b.r + b.y * a.r) / ab
    return dx * dx + dy * dy
}
