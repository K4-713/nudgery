package com.nudgery.shared.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class DailyCount(val date: LocalDate, val value: Double)

data class DataPoint(val at: Instant, val value: Double)

data class NamedCount(val label: String, val count: Int)

enum class HeatMapGranularity { DAY, WEEK, MONTH }

sealed class VisualizationData {
    data class CalendarHeatMap(
        val dailyCounts: List<DailyCount>,
        val windowStart: LocalDate,
        val windowEnd: LocalDate,
        val granularity: HeatMapGranularity
    ) : VisualizationData()
    data class LineGraph(
        val points: List<DataPoint>,
        val windowStart: LocalDate,
        val windowEnd: LocalDate
    ) : VisualizationData()
    data class ColumnChart(val entries: List<NamedCount>) : VisualizationData()
    data class BarChart(val entries: List<NamedCount>) : VisualizationData()
    data class TagCloud(val entries: List<NamedCount>) : VisualizationData()
}
