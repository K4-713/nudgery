// SPDX-License-Identifier: CC0-1.0

package com.nudgery.shared.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class DailyCount(val date: LocalDate, val value: Double)

data class DataPoint(val at: Instant, val value: Double)

data class NamedCount(
    val label: String,
    val count: Int,
    /**
     * This category's stable position within its full set, from 0.0 to 1.0 — an option's order
     * among all the question's options, or 0.0/1.0 for YES/NO. It does not depend on the count,
     * the sorted rank, or the selected window, so the bar and column charts can map it onto the
     * chart palette and keep a category's color fixed as the timeframe moves. 0.0 when there is no
     * inherent ordering (free-text words, which the packed bubble chart colors by magnitude).
     */
    val orderFraction: Float = 0f
)

/**
 * The time unit of each heat-map cell, chosen from the dashboard timeframe (see `computeGranularity`).
 * The weekly view renders its cells as a single centered row — the only non-`fillViewport` layout;
 * every other timeframe fills the viewport as an auto-fit grid (see `CalendarHeatMapChart`).
 *
 * Note: with the fill layout, `SINGLE_DAY`/`DAY` and `WEEK`/`WEEK_GRID` now render identically and
 * differ only in which timeframe selects them — a remaining simplification opportunity.
 */
enum class HeatMapGranularity {
    /** One cell per day; weekly draws these as a single row, short all-time spans as an auto-fit grid. */
    SINGLE_DAY,
    /** One cell per day, auto-fit grid (monthly, and 30–90-day all-time spans). */
    DAY,
    /** One cell per week, auto-fit grid (90–365-day all-time spans). */
    WEEK,
    /** One cell per week, auto-fit grid (yearly). */
    WEEK_GRID,
    /** One cell per calendar month, auto-fit grid (all-time spans over a year). */
    MONTH
}

sealed class VisualizationData {
    data class CalendarHeatMap(
        val dailyCounts: List<DailyCount>,
        val windowStart: LocalDate,
        val windowEnd: LocalDate,
        /**
         * Data-collection start (earliest recorded answer). The week-bucketed views (`WEEK`,
         * `WEEK_GRID`) count their 7-day cells from this date rather than from a calendar Monday, so
         * the first cell is a full week measured from when logging began — not a partial week left
         * over from a mid-week window edge.
         */
        val weekAnchor: LocalDate,
        val granularity: HeatMapGranularity,
        /** When true, the chart sizes cells to fit the entire dataset in view without scrolling. */
        val fillViewport: Boolean = false
    ) : VisualizationData()
    data class LineGraph(
        val points: List<DataPoint>,
        val windowStart: LocalDate,
        val windowEnd: LocalDate,
        /**
         * Number of days the chart shows at once before scrolling, set from the selected timeframe.
         * `Int.MAX_VALUE` means "fit the whole range with no scrolling" (all-time).
         */
        val visibleDays: Int = Int.MAX_VALUE,
        /**
         * Fixed Y-axis bounds so the axis stays stable while scrubbing the window (SCALE uses its
         * defined range; NUMBER/YES use 0 to the rounded global max). Null = auto-fit to visible data.
         */
        val yMin: Double? = null,
        val yMax: Double? = null
    ) : VisualizationData()
    data class ColumnChart(val entries: List<NamedCount>) : VisualizationData()
    data class BarChart(val entries: List<NamedCount>) : VisualizationData()
    data class PackedBubble(val entries: List<NamedCount>) : VisualizationData()
}
