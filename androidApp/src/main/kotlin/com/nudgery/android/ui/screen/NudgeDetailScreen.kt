package com.nudgery.android.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nudgery.android.R
import com.nudgery.android.viewmodel.AnswerRow
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.core.cartesian.axis.Axis
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.nudgery.android.viewmodel.NudgeDetailViewModel
import com.nudgery.shared.model.DailyCount
import com.nudgery.shared.model.DataPoint
import com.nudgery.shared.model.NamedCount
import com.nudgery.shared.model.Timeframe
import com.nudgery.shared.model.VisualizationData
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NudgeDetailScreen(
    nudgeId: String,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    onEditScheduleClick: () -> Unit,
    onEditFollowUpsClick: () -> Unit,
    onAnswerNow: () -> Unit,
    viewModel: NudgeDetailViewModel = koinViewModel(parameters = { parametersOf(nudgeId) })
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedChartIndex by rememberSaveable { mutableStateOf(0) }
    var showFullScreenChart by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onBack()
    }

    LaunchedEffect(uiState.exportContent) {
        val content = uiState.exportContent ?: return@LaunchedEffect
        val exportDir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val file = File(exportDir, "nudgery-export.csv")
        file.writeText(content)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
        viewModel.clearExportContent()
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.detail_delete_confirm_title)) },
            text = { Text(stringResource(R.string.detail_delete_confirm_body)) },
            confirmButton = {
                Button(onClick = { viewModel.deleteNudge() }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.nudgeName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.detail_edit_nudge))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            // Main question text
            if (uiState.mainQuestionText.isNotEmpty()) {
                item {
                    Text(
                        text = uiState.mainQuestionText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Schedule row
            uiState.schedule?.let { schedule ->
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = com.nudgery.android.viewmodel.ScheduleFormState.fromSchedule(schedule).toDescription(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onEditScheduleClick) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = stringResource(R.string.detail_edit_schedule)
                            )
                        }
                    }
                }
            }

            // Follow-up questions row
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (uiState.followUpCount > 0)
                            stringResource(R.string.detail_followup_count, uiState.followUpCount)
                        else
                            stringResource(R.string.detail_followup_questions),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onEditFollowUpsClick) {
                        Icon(
                            imageVector = Icons.Outlined.QuestionAnswer,
                            contentDescription = stringResource(R.string.detail_edit_followups)
                        )
                    }
                }
            }

            // Answer Now button
            item {
                Box {
                    Button(
                        onClick = onAnswerNow,
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.detail_answer_now))
                    }
                    if (uiState.hasMissedNotification) {
                        MissedDot(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                        )
                    }
                }
            }

            // Chart section
            if (uiState.visualizations.isNotEmpty()) {
                item {
                    ChartSection(
                        visualizations = uiState.visualizations,
                        selectedIndex = selectedChartIndex,
                        onSelectIndex = { selectedChartIndex = it },
                        selectedTimeframe = uiState.selectedTimeframe,
                        onTimeframeSelect = { viewModel.selectTimeframe(it) },
                        onExport = { viewModel.exportAnswers(com.nudgery.shared.model.ExportFormat.CSV) },
                        onExpandChart = { showFullScreenChart = true }
                    )
                }
            }

            // Data table
            item {
                AnswerTableSection(
                    answers = uiState.answers,
                    onSetHidden = { answerId, hidden -> viewModel.setAnswerHidden(answerId, hidden) }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.detail_delete_nudge))
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showFullScreenChart && uiState.visualizations.isNotEmpty()) {
        FullScreenChartDialog(
            visualizations = uiState.visualizations,
            selectedIndex = selectedChartIndex,
            onSelectIndex = { selectedChartIndex = it },
            selectedTimeframe = uiState.selectedTimeframe,
            onTimeframeSelect = { viewModel.selectTimeframe(it) },
            onDismiss = { showFullScreenChart = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ChartSection(
    visualizations: List<VisualizationData>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    selectedTimeframe: Timeframe,
    onTimeframeSelect: (Timeframe) -> Unit,
    onExport: () -> Unit,
    onExpandChart: () -> Unit
) {
    var showTypePicker by remember { mutableStateOf(false) }

    val safeIndex = selectedIndex.coerceAtMost(visualizations.lastIndex)

    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.weight(1f)) {
                    NudgeryChart(visualization = visualizations[safeIndex])
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showTypePicker = true },
                        enabled = visualizations.size > 1
                    ) {
                        Icon(Icons.Outlined.BarChart, contentDescription = stringResource(R.string.detail_edit_chart_type))
                    }
                    IconButton(onClick = onExport) {
                        Icon(Icons.Outlined.Download, contentDescription = stringResource(R.string.detail_export))
                    }
                    IconButton(onClick = onExpandChart) {
                        Icon(Icons.Outlined.ZoomIn, contentDescription = stringResource(R.string.detail_expand_chart))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TimeframeSelector(
                selectedTimeframe = selectedTimeframe,
                onTimeframeSelect = onTimeframeSelect
            )
        }
    }

    if (showTypePicker) {
        ChartTypePickerSheet(
            visualizations = visualizations,
            safeIndex = safeIndex,
            onSelectIndex = onSelectIndex,
            onDismiss = { showTypePicker = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeframeSelector(
    selectedTimeframe: Timeframe,
    onTimeframeSelect: (Timeframe) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        Timeframe.entries.forEach { tf ->
            NudgeryToggleChip(
                selected = selectedTimeframe == tf,
                onClick = { onTimeframeSelect(tf) },
                label = {
                    Text(
                        when (tf) {
                            Timeframe.WEEKLY -> stringResource(R.string.timeframe_weekly)
                            Timeframe.MONTHLY -> stringResource(R.string.timeframe_monthly)
                            Timeframe.YEARLY -> stringResource(R.string.timeframe_yearly)
                            Timeframe.ALL_TIME -> stringResource(R.string.timeframe_all_time)
                        }
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ChartTypePickerSheet(
    visualizations: List<VisualizationData>,
    safeIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.detail_chart_type_picker_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                visualizations.forEachIndexed { index, visualization ->
                    NudgeryToggleChip(
                        selected = index == safeIndex,
                        onClick = {
                            onSelectIndex(index)
                            onDismiss()
                        },
                        label = { Text(visualizationLabel(visualization)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun visualizationLabel(visualization: VisualizationData): String = when (visualization) {
    is VisualizationData.CalendarHeatMap -> stringResource(R.string.chart_type_heat_map)
    is VisualizationData.LineGraph -> stringResource(R.string.chart_type_line_graph)
    is VisualizationData.BarChart -> stringResource(R.string.chart_type_bar_chart)
    is VisualizationData.ColumnChart -> stringResource(R.string.chart_type_column_chart)
    is VisualizationData.TagCloud -> stringResource(R.string.chart_type_tag_cloud)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenChartDialog(
    visualizations: List<VisualizationData>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    selectedTimeframe: Timeframe,
    onTimeframeSelect: (Timeframe) -> Unit,
    onDismiss: () -> Unit
) {
    var showTypePicker by remember { mutableStateOf(false) }
    val safeIndex = selectedIndex.coerceAtMost(visualizations.lastIndex)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(visualizationLabel(visualizations[safeIndex])) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                            }
                        },
                        actions = {
                            if (visualizations.size > 1) {
                                IconButton(onClick = { showTypePicker = true }) {
                                    Icon(Icons.Outlined.BarChart, contentDescription = stringResource(R.string.detail_edit_chart_type))
                                }
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    NudgeryChart(
                        visualization = visualizations[safeIndex],
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TimeframeSelector(
                        selectedTimeframe = selectedTimeframe,
                        onTimeframeSelect = onTimeframeSelect,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp)
                    )
                }
            }
        }
    }

    if (showTypePicker) {
        ChartTypePickerSheet(
            visualizations = visualizations,
            safeIndex = safeIndex,
            onSelectIndex = onSelectIndex,
            onDismiss = { showTypePicker = false }
        )
    }
}

@Composable
private fun NudgeryChart(
    visualization: VisualizationData,
    modifier: Modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        key(visualization) {
            when (visualization) {
                is VisualizationData.CalendarHeatMap -> CalendarHeatMapChart(
                    counts = visualization.dailyCounts,
                    windowStart = visualization.windowStart,
                    windowEnd = visualization.windowEnd
                )
                is VisualizationData.LineGraph -> LineGraphChart(
                    points = visualization.points,
                    windowStart = visualization.windowStart,
                    windowEnd = visualization.windowEnd
                )
                is VisualizationData.BarChart -> HorizontalBarChart(visualization.entries)
                is VisualizationData.ColumnChart -> NamedCountChart(visualization.entries)
                is VisualizationData.TagCloud -> TagCloudChart(visualization.entries)
            }
        }
    }
}

@Composable
private fun CalendarHeatMapChart(
    counts: List<DailyCount>,
    windowStart: LocalDate,
    windowEnd: LocalDate
) {
    val maxValue = remember(counts) { counts.maxOfOrNull { it.value }?.coerceAtLeast(1.0) ?: 1.0 }
    val countByDate = remember(counts) { counts.associate { it.date to it.value } }
    // List of Monday dates, one per week column in the grid, spanning the full window
    val gridWeeks = remember(windowStart, windowEnd) {
        val gridStart = windowStart.minus(windowStart.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
        val gridEnd = windowEnd.plus(7 - windowEnd.dayOfWeek.isoDayNumber, DateTimeUnit.DAY)
        val weeks = mutableListOf<LocalDate>()
        var week = gridStart
        while (week <= gridEnd) {
            weeks.add(week)
            week = week.plus(7, DateTimeUnit.DAY)
        }
        weeks.toList()
    }

    val emptyCellColor = MaterialTheme.colorScheme.surfaceVariant
    val filledCellColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val gapPx = 2.dp.toPx()
        val totalWeeks = gridWeeks.size
        if (totalWeeks == 0) return@Canvas

        // Scale cell size to fit all weeks horizontally and all 7 days vertically
        val cellByHeight = (size.height - gapPx * 6) / 7f
        val cellByWidth = if (totalWeeks > 1)
            (size.width - gapPx * (totalWeeks - 1)) / totalWeeks.toFloat()
        else
            size.width
        val cellSize = minOf(cellByHeight, cellByWidth)

        gridWeeks.forEachIndexed { weekIdx, weekStart ->
            for (dayIdx in 0..6) {
                val date = weekStart.plus(dayIdx, DateTimeUnit.DAY)
                val intensity = ((countByDate[date] ?: 0.0) / maxValue).toFloat().coerceIn(0f, 1f)

                drawRoundRect(
                    color = lerp(emptyCellColor, filledCellColor, intensity),
                    topLeft = Offset(weekIdx * (cellSize + gapPx), dayIdx * (cellSize + gapPx)),
                    size = Size(cellSize, cellSize),
                    cornerRadius = CornerRadius(cellSize * 0.15f)
                )
            }
        }
    }
}

@Composable
private fun LineGraphChart(
    points: List<DataPoint>,
    windowStart: LocalDate,
    windowEnd: LocalDate
) {
    if (points.isEmpty()) {
        Text(stringResource(R.string.detail_no_answers), style = MaterialTheme.typography.bodySmall)
        return
    }
    val tz = TimeZone.currentSystemDefault()
    val windowDays = remember(windowStart, windowEnd) {
        (windowStart.until(windowEnd, DateTimeUnit.DAY) + 1).toInt().coerceAtLeast(1)
    }
    val xOffsets = remember(points, windowStart) {
        points.map { pt ->
            windowStart.until(pt.at.toLocalDateTime(tz).date, DateTimeUnit.DAY).toDouble()
        }
    }
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points, windowStart) {
        modelProducer.runTransaction {
            lineSeries { series(xOffsets, points.map { it.value }) }
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                rangeProvider = CartesianLayerRangeProvider.fixed(
                    minX = 0.0,
                    maxX = (windowDays - 1).toDouble()
                )
            ),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = object : CartesianValueFormatter {
                    override fun format(
                        context: CartesianMeasuringContext,
                        value: Double,
                        verticalAxisPosition: Axis.Position.Vertical?
                    ): CharSequence {
                        val date = windowStart.plus(
                            value.toInt().coerceIn(0, windowDays - 1),
                            DateTimeUnit.DAY
                        )
                        return "${date.monthNumber}/${date.dayOfMonth}"
                    }
                }
            ),
        ),
        modelProducer = modelProducer,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun HorizontalBarChart(entries: List<NamedCount>) {
    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.detail_no_answers), style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    val maxCount = remember(entries) { entries.maxOf { it.count }.coerceAtLeast(1) }
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        entries.forEach { entry ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(80.dp)
                )
                Spacer(Modifier.width(4.dp))
                // Bar track with filled bar overlaid
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(0.65f)
                        .background(trackColor, MaterialTheme.shapes.extraSmall)
                ) {
                    if (entry.count > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(entry.count.toFloat() / maxCount)
                                .fillMaxHeight()
                                .background(barColor, MaterialTheme.shapes.extraSmall)
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = entry.count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor
                )
            }
        }
    }
}

@Composable
private fun NamedCountChart(entries: List<NamedCount>) {
    if (entries.isEmpty()) {
        Text(stringResource(R.string.detail_no_answers), style = MaterialTheme.typography.bodySmall)
        return
    }
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(entries) {
        modelProducer.runTransaction {
            columnSeries { series(entries.map { it.count }) }
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = object : CartesianValueFormatter {
                    override fun format(
                        context: CartesianMeasuringContext,
                        value: Double,
                        verticalAxisPosition: Axis.Position.Vertical?
                    ): CharSequence {
                        return entries.getOrNull(value.toInt())?.label ?: ""
                    }
                }
            ),
        ),
        modelProducer = modelProducer,
        modifier = Modifier.fillMaxSize(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagCloudChart(entries: List<NamedCount>) {
    val max = entries.maxOfOrNull { it.count }?.toFloat() ?: 1f
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEach { entry ->
            val scale = 0.7f + (entry.count / max) * 0.6f
            Text(
                text = entry.label,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize * scale,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AnswerTableSection(
    answers: List<AnswerRow>,
    onSetHidden: (String, Boolean) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var confirmHideId by remember { mutableStateOf<String?>(null) }

    confirmHideId?.let { answerId ->
        AlertDialog(
            onDismissRequest = { confirmHideId = null },
            title = { Text(stringResource(R.string.detail_hide_confirm_title)) },
            text = { Text(stringResource(R.string.detail_hide_confirm_body)) },
            confirmButton = {
                Button(onClick = {
                    onSetHidden(answerId, true)
                    confirmHideId = null
                }) {
                    Text(stringResource(R.string.detail_hide_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmHideId = null }) {
                    Text(stringResource(R.string.detail_cancel))
                }
            }
        )
    }

    Card {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp)
            ) {
                Text(
                    text = if (answers.isEmpty())
                        stringResource(R.string.detail_no_answers)
                    else
                        stringResource(R.string.detail_answers_header, answers.size),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                HorizontalDivider()
                answers.forEach { answer ->
                    AnswerTableRow(
                        answer = answer,
                        onHide = { confirmHideId = answer.answerId },
                        onUnhide = { onSetHidden(answer.answerId, false) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AnswerTableRow(
    answer: AnswerRow,
    onHide: () -> Unit,
    onUnhide: () -> Unit
) {
    val tz = TimeZone.currentSystemDefault()
    val dateTime = answer.scheduledAt.toLocalDateTime(tz)
    val dateLabel = "${dateTime.date} ${dateTime.hour}:${dateTime.minute.toString().padStart(2, '0')}"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = answer.displayValue,
                style = MaterialTheme.typography.bodyMedium,
                color = if (answer.isHidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(
            onClick = if (answer.isHidden) onUnhide else onHide
        ) {
            Text(
                if (answer.isHidden)
                    stringResource(R.string.detail_show_answer)
                else
                    stringResource(R.string.detail_hide_answer)
            )
        }
    }
}
