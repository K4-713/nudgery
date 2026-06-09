// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil

class FitHeatGridTest {

    private fun assertFits(grid: HeatGrid, count: Int, width: Float, height: Float, gap: Float) {
        assertTrue("must have enough cells for $count", grid.columns * grid.rows >= count)
        assertEquals("rows = ceil(count/cols)",
            ceil(count.toDouble() / grid.columns).toInt(), grid.rows)
        val usedW = grid.columns * (grid.cellPx + gap) - gap
        val usedH = grid.rows * (grid.cellPx + gap) - gap
        assertTrue("content width $usedW must fit $width", usedW <= width + 0.01f)
        assertTrue("content height $usedH must fit $height", usedH <= height + 0.01f)
    }

    @Test
    fun TDD_fitHeatGrid_fillsWithoutOverflow() {
        // All-time view: the chosen grid must fit every cell within the canvas, no scrolling
        val grid = fitHeatGrid(cellCount = 52, availWidth = 1000f, availHeight = 560f, gap = 4f)
        assertFits(grid, 52, 1000f, 560f, 4f)
    }

    @Test
    fun TDD_fitHeatGrid_maximizesCellSizeVsSingleRow() {
        // A multi-row grid should yield larger cells than cramming everything into one row
        val grid = fitHeatGrid(cellCount = 52, availWidth = 1000f, availHeight = 560f, gap = 4f)
        val singleRowCell = (1000f - 4f * 51) / 52
        assertTrue("grid cell ${grid.cellPx} should beat single-row $singleRowCell",
            grid.cellPx > singleRowCell)
        assertTrue("should use more than one row to fill height", grid.rows > 1)
    }

    @Test
    fun TDD_fitHeatGrid_singleCell() {
        val grid = fitHeatGrid(cellCount = 1, availWidth = 300f, availHeight = 200f, gap = 4f)
        assertEquals(1, grid.columns)
        assertEquals(1, grid.rows)
        assertFits(grid, 1, 300f, 200f, 4f)
    }

    @Test
    fun TDD_fitHeatGrid_emptyIsZero() {
        val grid = fitHeatGrid(cellCount = 0, availWidth = 300f, availHeight = 200f, gap = 4f)
        assertEquals(HeatGrid(0, 0, 0f), grid)
    }
}
