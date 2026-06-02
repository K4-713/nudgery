package com.nudgery.shared.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class DailyCount(val date: LocalDate, val value: Double)

data class DataPoint(val at: Instant, val value: Double)

data class NamedCount(val label: String, val count: Int)

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
        val granularity: HeatMapGranularity,
        /** When true, the chart sizes cells to fit the entire dataset in view without scrolling. */
        val fillViewport: Boolean = false
    ) : VisualizationData()
    data class LineGraph(
        val points: List<DataPoint>,
        val windowStart: LocalDate,
        val windowEnd: LocalDate
    ) : VisualizationData()
    data class ColumnChart(val entries: List<NamedCount>) : VisualizationData()
    data class BarChart(val entries: List<NamedCount>) : VisualizationData()
    data class PackedBubble(val entries: List<NamedCount>) : VisualizationData()
}
