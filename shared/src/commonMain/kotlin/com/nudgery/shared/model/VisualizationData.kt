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

enum class HeatMapGranularity {
    /** One cell per day, rendered as a single horizontal strip (8 days visible per screen). */
    SINGLE_DAY,
    /** One cell per day, rendered as a 7-row Mon–Sun grid (GitHub contribution graph style). */
    DAY,
    /** One cell per week, rendered as a single horizontal strip that scrolls. */
    WEEK,
    /** One cell per week, rendered as a multi-row grid showing roughly a year per screen, scrolling for older weeks. */
    WEEK_GRID,
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
         * the first cell is a full week measured from when tracking began — not a partial week left
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
