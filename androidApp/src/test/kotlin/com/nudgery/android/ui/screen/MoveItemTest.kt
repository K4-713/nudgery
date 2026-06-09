// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [moveItem], the pure in-memory reorder the drag applies optimistically before the new
 * order is persisted (ENGINEERING_DECISIONS.md ED-19).
 */
class MoveItemTest {

    private val base = listOf("a", "b", "c", "d")

    @Test
    fun TDD_moveItemDown_shiftsInterveningItemsUp() {
        assertEquals(listOf("b", "c", "a", "d"), base.moveItem(from = 0, to = 2))
    }

    @Test
    fun TDD_moveItemUp_shiftsInterveningItemsDown() {
        assertEquals(listOf("a", "d", "b", "c"), base.moveItem(from = 3, to = 1))
    }

    @Test
    fun TDD_moveItemToEnds_works() {
        assertEquals(listOf("b", "c", "d", "a"), base.moveItem(from = 0, to = 3))
        assertEquals(listOf("d", "a", "b", "c"), base.moveItem(from = 3, to = 0))
    }

    @Test
    fun TDD_moveItemSameIndex_isNoOp() {
        assertEquals(base, base.moveItem(from = 2, to = 2))
    }

    @Test
    fun TDD_moveItemOutOfRange_returnsListUnchanged() {
        assertEquals(base, base.moveItem(from = 0, to = 9))
        assertEquals(base, base.moveItem(from = -1, to = 1))
    }
}
