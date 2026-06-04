package com.nudgery.android.ui.screen

import com.nudgery.shared.model.DailyCount
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [buildWeekCells]: the week-bucketed heat maps (yearly / all-time WEEK) count weeks from
 * the data-collection start, so the first cell is a full week rather than a partial one clipped by a
 * mid-week window edge or a calendar-Monday boundary.
 */
class WeekCellsTest {

    private fun dailyOnes(start: LocalDate, days: Int): List<DailyCount> =
        (0 until days).map { DailyCount(start.plus(it, DateTimeUnit.DAY), 1.0) }

    @Test
    fun TDD_firstWeekIsAFullWeekFromDataStart_notCalendarMonday() {
        // Weeks are 7-day periods counted from the data-collection start, so even though tracking
        // began on a Wednesday the first cell sums a full 7 days (no low partial week).
        val anchor = LocalDate(2026, 1, 7) // a Wednesday
        val cells = buildWeekCells(
            counts = dailyOnes(anchor, 14),
            windowStart = anchor,
            windowEnd = anchor.plus(13, DateTimeUnit.DAY),
            weekAnchor = anchor
        )

        assertEquals(2, cells.size)
        assertEquals(anchor, cells[0].first)                       // starts on the data-start day, not a Monday
        assertEquals(7.0, cells[0].second)                         // a full week, not a low partial
        assertEquals(anchor.plus(7, DateTimeUnit.DAY), cells[1].first)
        assertEquals(7.0, cells[1].second)
    }

    @Test
    fun TDD_windowClippedLeadingPartialWeekIsDropped() {
        // The yearly rolling window can begin mid-period when data predates it; the clipped leading
        // days are dropped so the first visible cell is still a whole week, never a low partial.
        val anchor = LocalDate(2025, 1, 1)
        val windowStart = LocalDate(2025, 1, 3) // two days into the Jan-1 period
        val windowEnd = LocalDate(2025, 1, 21)
        val cells = buildWeekCells(dailyOnes(windowStart, 19), windowStart, windowEnd, weekAnchor = anchor)

        assertEquals(LocalDate(2025, 1, 8), cells.first().first)   // first whole period on/after window start
        assertEquals(7.0, cells.first().second)                    // full week; clipped Jan 3–7 dropped
    }

    @Test
    fun TDD_weeksWithoutDataAreNull() {
        val anchor = LocalDate(2026, 3, 2)
        val cells = buildWeekCells(
            counts = dailyOnes(anchor, 7), // only the first week has data
            windowStart = anchor,
            windowEnd = anchor.plus(13, DateTimeUnit.DAY),
            weekAnchor = anchor
        )

        assertEquals(2, cells.size)
        assertEquals(7.0, cells[0].second)
        assertEquals(null, cells[1].second) // no data the second week
    }
}
