package com.nudgery.android.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import com.nudgery.android.R
import com.nudgery.android.backup.nudgeExportFileBase
import com.nudgery.android.viewmodel.AnswerRow
import com.nudgery.android.viewmodel.FollowUpVisualization
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
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
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.nudgery.android.viewmodel.NudgeDetailViewModel
import com.nudgery.android.ui.theme.ChartPalettePreference
import com.nudgery.android.ui.theme.paletteStops
import com.nudgery.android.viewmodel.SettingsViewModel
import com.nudgery.shared.model.DailyCount
import com.nudgery.shared.model.DataPoint
import com.nudgery.shared.model.HeatMapGranularity
import com.nudgery.shared.model.NamedCount
import com.nudgery.shared.model.Timeframe
import com.nudgery.shared.model.VisualizationData
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until
import android.content.Intent
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.FileProvider
import com.nudgery.shared.model.ExportFormat
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
    viewModel: NudgeDetailViewModel = koinViewModel(parameters = { parametersOf(nudgeId) }),
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val chartPalette = settingsState.chartPalette
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
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val base = nudgeExportFileBase(uiState.nudgeName, today)
        val (fileName, mimeType) = when (uiState.exportFormat) {
            ExportFormat.CSV -> "$base.csv" to "text/csv"
            ExportFormat.TSV -> "$base.tsv" to "text/tab-separated-values"
            ExportFormat.JSON -> "$base.json" to "application/json"
        }
        val file = File(exportDir, fileName)
        file.writeText(content)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
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

            // Follow-up questions row — hidden for free-text main questions, which can't have them.
            if (uiState.mainQuestionType?.allowsFollowUps == true) {
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
                        onExport = { format -> viewModel.exportAnswers(format) },
                        onExpandChart = { showFullScreenChart = true },
                        chartPalette = chartPalette
                    )
                }
            }

            // Follow-up charts (one card per follow-up question with charatable data)
            uiState.followUpVisualizations.forEach { followUp ->
                item(key = followUp.questionId) {
                    FollowUpChartSection(
                        followUp = followUp,
                        selectedTimeframe = uiState.selectedTimeframe,
                        onTimeframeSelect = { viewModel.selectTimeframe(it) },
                        chartPalette = chartPalette
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
            onDismiss = { showFullScreenChart = false },
            chartPalette = chartPalette
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
    onExport: (ExportFormat) -> Unit,
    onExpandChart: () -> Unit,
    chartPalette: ChartPalettePreference
) {
    var showTypePicker by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    val safeIndex = selectedIndex.coerceAtMost(visualizations.lastIndex)

    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.weight(1f)) {
                    NudgeryChart(visualization = visualizations[safeIndex], chartPalette = chartPalette)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showTypePicker = true },
                        enabled = visualizations.size > 1
                    ) {
                        Icon(Icons.Outlined.BarChart, contentDescription = stringResource(R.string.detail_edit_chart_type))
                    }
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Outlined.Download, contentDescription = stringResource(R.string.detail_export))
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.detail_export_csv)) },
                                onClick = { showExportMenu = false; onExport(ExportFormat.CSV) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.detail_export_backup)) },
                                onClick = { showExportMenu = false; onExport(ExportFormat.JSON) }
                            )
                        }
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
    is VisualizationData.PackedBubble -> stringResource(R.string.chart_type_packed_bubble)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenChartDialog(
    visualizations: List<VisualizationData>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    selectedTimeframe: Timeframe,
    onTimeframeSelect: (Timeframe) -> Unit,
    onDismiss: () -> Unit,
    chartPalette: ChartPalettePreference,
    title: String? = null
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
                        title = { Text(title ?: visualizationLabel(visualizations[safeIndex])) },
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
                        chartPalette = chartPalette,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        zoomable = true
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
    chartPalette: ChartPalettePreference,
    modifier: Modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
    // Only the full-screen packed bubble chart enables pinch-to-zoom.
    zoomable: Boolean = false
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
                    windowEnd = visualization.windowEnd,
                    granularity = visualization.granularity,
                    palette = chartPalette,
                    fillViewport = visualization.fillViewport
                )
                is VisualizationData.LineGraph -> LineGraphChart(
                    points = visualization.points,
                    windowStart = visualization.windowStart,
                    windowEnd = visualization.windowEnd,
                    visibleDays = visualization.visibleDays
                )
                is VisualizationData.BarChart -> HorizontalBarChart(visualization.entries)
                is VisualizationData.ColumnChart -> NamedCountChart(visualization.entries)
                is VisualizationData.PackedBubble -> PackedBubbleChart(visualization.entries, chartPalette, zoomable)
            }
        }
    }
}

// The weekly (SINGLE_DAY) heat map lays days out in a short, wide grid: 2 rows keeps the squares
// large and lets ~SINGLE_DAY_GRID_VISIBLE_COLS columns fill the card's 16:9 width (≈8 days/screen),
// scrolling for older days.
private const val SINGLE_DAY_GRID_ROWS = 2
private const val SINGLE_DAY_GRID_VISIBLE_COLS = 4

// The yearly (WEEK_GRID) heat map wraps week cells into this many rows and targets
// WEEK_GRID_VISIBLE_COLS columns per screen (≈ a year: 5 × 11 ≈ 55 weeks), scrolling for older data.
private const val WEEK_GRID_ROWS = 5
private const val WEEK_GRID_VISIBLE_COLS = 11

@Composable
private fun CalendarHeatMapChart(
    counts: List<DailyCount>,
    windowStart: LocalDate,
    windowEnd: LocalDate,
    granularity: HeatMapGranularity,
    palette: ChartPalettePreference,
    fillViewport: Boolean = false
) {
    val textMeasurer = rememberTextMeasurer()
    val emptyCellColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val zeroCellColor = if (isDark) lerp(emptyCellColor, Color.White, 0.1f)
                        else lerp(emptyCellColor, Color.Black, 0.1f)
    val paletteStops = palette.paletteStops
    val density = LocalDensity.current

    val countByDate = remember(counts) { counts.associate { it.date to it.value } }

    val dayGridWeeks = remember(windowStart, windowEnd) {
        val gridStart = windowStart.minus(windowStart.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
        val gridEnd = windowEnd.plus(7 - windowEnd.dayOfWeek.isoDayNumber, DateTimeUnit.DAY)
        buildList {
            var week = gridStart
            while (week <= gridEnd) {
                add(week)
                week = week.plus(7, DateTimeUnit.DAY)
            }
        }
    }

    // One entry per calendar day; only built when granularity == SINGLE_DAY.
    val singleDayCells = remember(counts, windowStart, windowEnd, granularity) {
        if (granularity != HeatMapGranularity.SINGLE_DAY) return@remember emptyList<Pair<LocalDate, Double?>>()
        buildList {
            var date = windowStart
            while (date <= windowEnd) {
                add(date to countByDate[date])
                date = date.plus(1, DateTimeUnit.DAY)
            }
        }
    }

    // Every calendar day in the window; built for the all-time auto-fit grid when its cell unit is days.
    val fillDayCells = remember(counts, windowStart, windowEnd, granularity, fillViewport) {
        if (!fillViewport ||
            (granularity != HeatMapGranularity.SINGLE_DAY && granularity != HeatMapGranularity.DAY)
        ) return@remember emptyList<Pair<LocalDate, Double?>>()
        buildList {
            var date = windowStart
            while (date <= windowEnd) {
                add(date to countByDate[date])
                date = date.plus(1, DateTimeUnit.DAY)
            }
        }
    }

    val weekCells = remember(counts, windowStart, windowEnd) {
        buildWeekCells(counts, windowStart, windowEnd)
    }

    val monthCells = remember(counts, windowStart, windowEnd) {
        buildMonthCells(counts, windowStart, windowEnd)
    }

    val maxValue = remember(counts, granularity, singleDayCells, weekCells, monthCells) {
        when (granularity) {
            HeatMapGranularity.SINGLE_DAY -> singleDayCells.maxOfOrNull { it.second ?: 0.0 }?.coerceAtLeast(1.0) ?: 1.0
            HeatMapGranularity.DAY -> counts.maxOfOrNull { it.value }?.coerceAtLeast(1.0) ?: 1.0
            HeatMapGranularity.WEEK, HeatMapGranularity.WEEK_GRID ->
                weekCells.maxOfOrNull { it.second ?: 0.0 }?.coerceAtLeast(1.0) ?: 1.0
            HeatMapGranularity.MONTH -> monthCells.maxOfOrNull { it.second ?: 0.0 }?.coerceAtLeast(1.0) ?: 1.0
        }
    }

    // The color→value scale is only meaningful once cells hold something other than 0 or 1; a plain
    // yes/no-once-a-day map (only 0s and 1s) needs no legend.
    val showValueScale = remember(counts, granularity, weekCells, monthCells) {
        val values = when (granularity) {
            HeatMapGranularity.SINGLE_DAY, HeatMapGranularity.DAY -> counts.map { it.value }
            HeatMapGranularity.WEEK, HeatMapGranularity.WEEK_GRID -> weekCells.mapNotNull { it.second }
            HeatMapGranularity.MONTH -> monthCells.mapNotNull { it.second }
        }
        values.any { it != 0.0 && it != 1.0 }
    }
    val gradientStops = if (isDark) paletteStops.darkStops else paletteStops.lightStops
    // Subtle ring marking the tapped cell — visible on any palette color without dominating it.
    val outlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)

    // Cell selected by tapping; shows its exact value. Reset when the data or granularity changes.
    var selectedCell by remember(counts, granularity, windowStart, windowEnd) {
        mutableStateOf<SelectedHeatCell?>(null)
    }

    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val availableWidthPx = constraints.maxWidth.toFloat()
            val availableHeightPx = constraints.maxHeight.toFloat()

            val gapPx = with(density) { 2.dp.toPx() }
            val labelAreaPx = with(density) { 16.dp.toPx() }
            val minCellPx = with(density) { 14.dp.toPx() }
            val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)

            if (fillViewport) {
                // All-time: lay every cell out in an auto-fit grid that fills the canvas, no scroll.
                // The adaptive granularity keeps the cell count bounded; the grid picks the rows/cols
                // that maximize square cell size for that count. Column-major, oldest at top-left.
                val unitCells = when (granularity) {
                    HeatMapGranularity.SINGLE_DAY, HeatMapGranularity.DAY -> fillDayCells
                    HeatMapGranularity.WEEK, HeatMapGranularity.WEEK_GRID -> weekCells
                    HeatMapGranularity.MONTH -> monthCells
                }
                val grid = fitHeatGrid(unitCells.size, availableWidthPx, availableHeightPx - labelAreaPx, gapPx)
                val fillTap = Modifier.pointerInput(granularity, grid, unitCells) {
                    detectTapGestures { offset ->
                        selectedCell = hitTestFillGrid(offset, unitCells, grid, gapPx, labelAreaPx)
                    }
                }
                Canvas(modifier = Modifier.fillMaxSize().then(fillTap)) {
                    if (unitCells.isEmpty() || grid.cellPx <= 0f) return@Canvas
                    val selectedDate = selectedCell?.date
                    val cellPx = grid.cellPx
                    val byYear = granularity == HeatMapGranularity.MONTH
                    var currentLabelKey = Int.MIN_VALUE
                    var lastLabelEnd = Float.NEGATIVE_INFINITY
                    unitCells.forEachIndexed { idx, (date, value) ->
                        val colIdx = idx / grid.rows
                        val rowIdx = idx % grid.rows
                        val x = colIdx * (cellPx + gapPx)
                        val y = labelAreaPx + rowIdx * (cellPx + gapPx)
                        // Sparse labels at column tops: month for day/week units, year for months.
                        if (rowIdx == 0) {
                            val key = if (byYear) date.year else date.monthNumber
                            if (colIdx == 0 || key != currentLabelKey) {
                                currentLabelKey = key
                                val label = if (byYear) date.year.toString()
                                    else date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                                val measured = textMeasurer.measure(label, labelStyle)
                                if (x >= lastLabelEnd + 4.dp.toPx()) {
                                    drawText(measured, topLeft = Offset(x, 0f))
                                    lastLabelEnd = x + measured.size.width
                                }
                            }
                        }
                        drawRoundRect(
                            color = when {
                                value == null -> emptyCellColor
                                value <= 0.0 -> zeroCellColor
                                else -> paletteStops.colorAt((value / maxValue).toFloat().coerceIn(0f, 1f), isDark)
                            },
                            topLeft = Offset(x, y),
                            size = Size(cellPx, cellPx),
                            cornerRadius = CornerRadius(cellPx * 0.15f)
                        )
                        if (date == selectedDate) {
                            drawRoundRect(
                                color = outlineColor,
                                topLeft = Offset(x, y),
                                size = Size(cellPx, cellPx),
                                cornerRadius = CornerRadius(cellPx * 0.15f),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }
                }
            } else {

            val numberOfCells = when (granularity) {
                // Number of columns in the grid (each column holds SINGLE_DAY_GRID_ROWS days).
                HeatMapGranularity.SINGLE_DAY ->
                    (singleDayCells.size + SINGLE_DAY_GRID_ROWS - 1) / SINGLE_DAY_GRID_ROWS
                HeatMapGranularity.DAY -> dayGridWeeks.size
                HeatMapGranularity.WEEK -> weekCells.size
                // Number of columns in the year grid (each column holds WEEK_GRID_ROWS weeks).
                HeatMapGranularity.WEEK_GRID ->
                    (weekCells.size + WEEK_GRID_ROWS - 1) / WEEK_GRID_ROWS
                HeatMapGranularity.MONTH -> monthCells.size
            }
            val numberOfRows = if (granularity == HeatMapGranularity.DAY) 7 else 1

            val maxCellByHeight = (availableHeightPx - labelAreaPx - gapPx * (numberOfRows - 1)) / numberOfRows
            val naturalCellByWidth = if (numberOfCells > 1)
                (availableWidthPx - gapPx * (numberOfCells - 1)) / numberOfCells
            else availableWidthPx

            val maxCellByHeightForSingleDayGrid =
                (availableHeightPx - labelAreaPx - gapPx * (SINGLE_DAY_GRID_ROWS - 1)) / SINGLE_DAY_GRID_ROWS
            val maxCellByHeightForWeekGrid =
                (availableHeightPx - labelAreaPx - gapPx * (WEEK_GRID_ROWS - 1)) / WEEK_GRID_ROWS

            val cellPx = when (granularity) {
                HeatMapGranularity.SINGLE_DAY ->
                    // Weekly: target SINGLE_DAY_GRID_VISIBLE_COLS columns per screen, then scroll.
                    minOf(maxCellByHeightForSingleDayGrid,
                        maxOf((availableWidthPx - gapPx * (SINGLE_DAY_GRID_VISIBLE_COLS - 1)) / SINGLE_DAY_GRID_VISIBLE_COLS, minCellPx))
                HeatMapGranularity.WEEK_GRID ->
                    // Yearly: target WEEK_GRID_VISIBLE_COLS columns (~a year) per screen, then scroll
                    // for older weeks. Height (WEEK_GRID_ROWS rows) still caps the cell size.
                    minOf(maxCellByHeightForWeekGrid,
                        maxOf((availableWidthPx - gapPx * (WEEK_GRID_VISIBLE_COLS - 1)) / WEEK_GRID_VISIBLE_COLS, minCellPx))
                else ->
                    minOf(maxCellByHeight, maxOf(naturalCellByWidth, minCellPx))
            }
            val contentWidthPx = if (numberOfCells > 0)
                numberOfCells * (cellPx + gapPx) - gapPx else 0f
            val needsScroll = !fillViewport && contentWidthPx > availableWidthPx

            LaunchedEffect(contentWidthPx) {
                if (needsScroll) scrollState.scrollTo(scrollState.maxValue)
            }

            val tapModifier = Modifier.pointerInput(granularity, cellPx, gapPx, labelAreaPx, counts) {
                detectTapGestures { offset ->
                    selectedCell = hitTestHeatCell(
                        offset, granularity, cellPx, gapPx, labelAreaPx,
                        singleDayCells, dayGridWeeks, weekCells, monthCells, countByDate
                    )
                }
            }

            Canvas(
                modifier = (if (needsScroll)
                    Modifier
                        .horizontalScroll(scrollState)
                        .width(with(density) { contentWidthPx.toDp() })
                        .height(with(density) { availableHeightPx.toDp() })
                else
                    Modifier.fillMaxSize()
                ).then(tapModifier)
            ) {
                val selectedDate = selectedCell?.date
                when (granularity) {
                    HeatMapGranularity.SINGLE_DAY -> {
                        if (singleDayCells.isEmpty()) return@Canvas
                        // Column-major layout: each column holds SINGLE_DAY_GRID_ROWS days
                        // stacked top-to-bottom, columns progress left (oldest) to right (newest).
                        var currentMonth = -1
                        var lastLabelEnd = Float.NEGATIVE_INFINITY
                        singleDayCells.forEachIndexed { dayIdx, (date, value) ->
                            val colIdx = dayIdx / SINGLE_DAY_GRID_ROWS
                            val rowIdx = dayIdx % SINGLE_DAY_GRID_ROWS
                            val x = colIdx * (cellPx + gapPx)
                            val y = labelAreaPx + rowIdx * (cellPx + gapPx)
                            if (rowIdx == 0) {
                                val month = date.monthNumber
                                if (colIdx == 0 || month != currentMonth) {
                                    currentMonth = month
                                    val label = date.month.name.take(3)
                                        .lowercase().replaceFirstChar { it.uppercase() }
                                    val measured = textMeasurer.measure(label, labelStyle)
                                    if (x >= lastLabelEnd + 4.dp.toPx()) {
                                        drawText(measured, topLeft = Offset(x, 0f))
                                        lastLabelEnd = x + measured.size.width
                                    }
                                }
                            }
                            drawRoundRect(
                                color = when {
                                    value == null -> emptyCellColor
                                    value <= 0.0 -> zeroCellColor
                                    else -> paletteStops.colorAt(
                                        (value / maxValue).toFloat().coerceIn(0f, 1f), isDark
                                    )
                                },
                                topLeft = Offset(x, y),
                                size = Size(cellPx, cellPx),
                                cornerRadius = CornerRadius(cellPx * 0.15f)
                            )
                            if (date == selectedDate) {
                                drawRoundRect(
                                    color = outlineColor,
                                    topLeft = Offset(x, y),
                                    size = Size(cellPx, cellPx),
                                    cornerRadius = CornerRadius(cellPx * 0.15f),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                        }
                    }
                    HeatMapGranularity.DAY -> {
                        if (dayGridWeeks.isEmpty()) return@Canvas
                        var currentMonth = -1
                        var lastLabelEnd = Float.NEGATIVE_INFINITY
                        dayGridWeeks.forEachIndexed { weekIdx, weekStart ->
                            val weekX = weekIdx * (cellPx + gapPx)
                            val weekMonth = weekStart.monthNumber
                            if (weekIdx == 0 || weekMonth != currentMonth) {
                                currentMonth = weekMonth
                                val label = weekStart.month.name.take(3)
                                    .lowercase().replaceFirstChar { it.uppercase() }
                                val measured = textMeasurer.measure(label, labelStyle)
                                if (weekX >= lastLabelEnd + 4.dp.toPx()) {
                                    drawText(measured, topLeft = Offset(weekX, 0f))
                                    lastLabelEnd = weekX + measured.size.width
                                }
                            }
                            for (dayIdx in 0..6) {
                                val date = weekStart.plus(dayIdx, DateTimeUnit.DAY)
                                val rawCount = countByDate[date]
                                val cellTopLeft = Offset(weekX, labelAreaPx + dayIdx * (cellPx + gapPx))
                                drawRoundRect(
                                    color = when {
                                        rawCount == null -> emptyCellColor
                                        rawCount <= 0.0 -> zeroCellColor
                                        else -> paletteStops.colorAt(
                                            (rawCount / maxValue).toFloat().coerceIn(0f, 1f), isDark
                                        )
                                    },
                                    topLeft = cellTopLeft,
                                    size = Size(cellPx, cellPx),
                                    cornerRadius = CornerRadius(cellPx * 0.15f)
                                )
                                if (date == selectedDate) {
                                    drawRoundRect(
                                        color = outlineColor,
                                        topLeft = cellTopLeft,
                                        size = Size(cellPx, cellPx),
                                        cornerRadius = CornerRadius(cellPx * 0.15f),
                                        style = Stroke(width = 1.5.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                    HeatMapGranularity.WEEK -> {
                        if (weekCells.isEmpty()) return@Canvas
                        var currentMonth = -1
                        var lastLabelEnd = Float.NEGATIVE_INFINITY
                        weekCells.forEachIndexed { idx, (weekStart, value) ->
                            val x = idx * (cellPx + gapPx)
                            val weekMonth = weekStart.monthNumber
                            if (idx == 0 || weekMonth != currentMonth) {
                                currentMonth = weekMonth
                                val label = weekStart.month.name.take(3)
                                    .lowercase().replaceFirstChar { it.uppercase() }
                                val measured = textMeasurer.measure(label, labelStyle)
                                if (x >= lastLabelEnd + 4.dp.toPx()) {
                                    drawText(measured, topLeft = Offset(x, 0f))
                                    lastLabelEnd = x + measured.size.width
                                }
                            }
                            drawRoundRect(
                                color = when {
                                    value == null -> emptyCellColor
                                    value <= 0.0 -> zeroCellColor
                                    else -> paletteStops.colorAt(
                                        (value / maxValue).toFloat().coerceIn(0f, 1f), isDark
                                    )
                                },
                                topLeft = Offset(x, labelAreaPx),
                                size = Size(cellPx, cellPx),
                                cornerRadius = CornerRadius(cellPx * 0.15f)
                            )
                            if (weekStart == selectedDate) {
                                drawRoundRect(
                                    color = outlineColor,
                                    topLeft = Offset(x, labelAreaPx),
                                    size = Size(cellPx, cellPx),
                                    cornerRadius = CornerRadius(cellPx * 0.15f),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                        }
                    }
                    HeatMapGranularity.WEEK_GRID -> {
                        if (weekCells.isEmpty()) return@Canvas
                        // Column-major: each column holds WEEK_GRID_ROWS weeks (oldest at top-left),
                        // columns advance chronologically left to right.
                        var currentMonth = -1
                        var lastLabelEnd = Float.NEGATIVE_INFINITY
                        weekCells.forEachIndexed { idx, (weekStart, value) ->
                            val colIdx = idx / WEEK_GRID_ROWS
                            val rowIdx = idx % WEEK_GRID_ROWS
                            val x = colIdx * (cellPx + gapPx)
                            val y = labelAreaPx + rowIdx * (cellPx + gapPx)
                            // Month labels along the top, keyed to the first week of each column.
                            if (rowIdx == 0) {
                                val weekMonth = weekStart.monthNumber
                                if (colIdx == 0 || weekMonth != currentMonth) {
                                    currentMonth = weekMonth
                                    val label = weekStart.month.name.take(3)
                                        .lowercase().replaceFirstChar { it.uppercase() }
                                    val measured = textMeasurer.measure(label, labelStyle)
                                    if (x >= lastLabelEnd + 4.dp.toPx()) {
                                        drawText(measured, topLeft = Offset(x, 0f))
                                        lastLabelEnd = x + measured.size.width
                                    }
                                }
                            }
                            drawRoundRect(
                                color = when {
                                    value == null -> emptyCellColor
                                    value <= 0.0 -> zeroCellColor
                                    else -> paletteStops.colorAt(
                                        (value / maxValue).toFloat().coerceIn(0f, 1f), isDark
                                    )
                                },
                                topLeft = Offset(x, y),
                                size = Size(cellPx, cellPx),
                                cornerRadius = CornerRadius(cellPx * 0.15f)
                            )
                            if (weekStart == selectedDate) {
                                drawRoundRect(
                                    color = outlineColor,
                                    topLeft = Offset(x, y),
                                    size = Size(cellPx, cellPx),
                                    cornerRadius = CornerRadius(cellPx * 0.15f),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                        }
                    }
                    HeatMapGranularity.MONTH -> {
                        if (monthCells.isEmpty()) return@Canvas
                        var currentYear = -1
                        var lastLabelEnd = Float.NEGATIVE_INFINITY
                        monthCells.forEachIndexed { idx, (monthStart, value) ->
                            val x = idx * (cellPx + gapPx)
                            val year = monthStart.year
                            if (idx == 0 || year != currentYear) {
                                currentYear = year
                                val measured = textMeasurer.measure(year.toString(), labelStyle)
                                if (x >= lastLabelEnd + 4.dp.toPx()) {
                                    drawText(measured, topLeft = Offset(x, 0f))
                                    lastLabelEnd = x + measured.size.width
                                }
                            }
                            drawRoundRect(
                                color = when {
                                    value == null -> emptyCellColor
                                    value <= 0.0 -> zeroCellColor
                                    else -> paletteStops.colorAt(
                                        (value / maxValue).toFloat().coerceIn(0f, 1f), isDark
                                    )
                                },
                                topLeft = Offset(x, labelAreaPx),
                                size = Size(cellPx, cellPx),
                                cornerRadius = CornerRadius(cellPx * 0.15f)
                            )
                            if (monthStart == selectedDate) {
                                drawRoundRect(
                                    color = outlineColor,
                                    topLeft = Offset(x, labelAreaPx),
                                    size = Size(cellPx, cellPx),
                                    cornerRadius = CornerRadius(cellPx * 0.15f),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }
            }
        }

        val noDataText = stringResource(R.string.heatmap_no_data)
        Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
            // Exact value of the tapped cell.
            selectedCell?.let { sel ->
                Text(
                    text = "${heatCellDateLabel(sel.date, granularity)} · " +
                        (sel.value?.let { formatHeatValue(it) } ?: noDataText),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Color→value scale: cold end is 0, hot end is the maximum cell value.
                if (showValueScale) {
                    Text("0", style = MaterialTheme.typography.labelSmall, color = labelColor)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(64.dp)
                            .background(Brush.horizontalGradient(gradientStops), RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = formatHeatValue(maxValue),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(
                        when (granularity) {
                            HeatMapGranularity.SINGLE_DAY, HeatMapGranularity.DAY -> R.string.heatmap_legend_day
                            HeatMapGranularity.WEEK, HeatMapGranularity.WEEK_GRID -> R.string.heatmap_legend_week
                            HeatMapGranularity.MONTH -> R.string.heatmap_legend_month
                        }
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor
                )
            }
        }
    }
}

/** A heat map cell selected by tapping; [value] is null when no data was recorded for that period. */
private data class SelectedHeatCell(val date: LocalDate, val value: Double?)

/** Dimensions of an auto-fit heat map grid: square cells of [cellPx] in [columns] × [rows]. */
internal data class HeatGrid(val columns: Int, val rows: Int, val cellPx: Float)

/**
 * Chooses the grid that fits [cellCount] square cells into [availWidth] × [availHeight] with the
 * largest possible cell size — used by the all-time view to fill the canvas without scrolling.
 * Column-major: column index = cellIndex / rows.
 */
internal fun fitHeatGrid(cellCount: Int, availWidth: Float, availHeight: Float, gap: Float): HeatGrid {
    if (cellCount <= 0 || availWidth <= 0f || availHeight <= 0f) return HeatGrid(0, 0, 0f)
    var best = HeatGrid(1, cellCount, 0f)
    var bestCell = -1f
    for (columns in 1..cellCount) {
        val rows = (cellCount + columns - 1) / columns
        val cellByWidth = (availWidth - gap * (columns - 1)) / columns
        val cellByHeight = (availHeight - gap * (rows - 1)) / rows
        val cell = minOf(cellByWidth, cellByHeight)
        if (cell > bestCell) {
            bestCell = cell
            best = HeatGrid(columns, rows, cell)
        }
    }
    return best
}

/** Maps a tap to a cell in an auto-fit (column-major) all-time grid. */
private fun hitTestFillGrid(
    offset: Offset,
    cells: List<Pair<LocalDate, Double?>>,
    grid: HeatGrid,
    gap: Float,
    labelArea: Float
): SelectedHeatCell? {
    if (grid.rows <= 0) return null
    val step = grid.cellPx + gap
    if (step <= 0f || offset.x < 0f) return null
    val column = (offset.x / step).toInt()
    val yInGrid = offset.y - labelArea
    if (yInGrid < 0f) return null
    val row = (yInGrid / step).toInt()
    if (row !in 0 until grid.rows) return null
    val index = column * grid.rows + row
    return cells.getOrNull(index)?.let { SelectedHeatCell(it.first, it.second) }
}

/** Maps a tap [offset] (in the chart's content coordinate space) to the cell it landed on. */
private fun hitTestHeatCell(
    offset: Offset,
    granularity: HeatMapGranularity,
    cellPx: Float,
    gapPx: Float,
    labelAreaPx: Float,
    singleDayCells: List<Pair<LocalDate, Double?>>,
    dayGridWeeks: List<LocalDate>,
    weekCells: List<Pair<LocalDate, Double?>>,
    monthCells: List<Pair<LocalDate, Double?>>,
    countByDate: Map<LocalDate, Double>
): SelectedHeatCell? {
    val step = cellPx + gapPx
    if (step <= 0f || offset.x < 0f) return null
    val column = (offset.x / step).toInt()
    val yInGrid = offset.y - labelAreaPx
    if (yInGrid < 0f) return null
    val row = (yInGrid / step).toInt()

    return when (granularity) {
        HeatMapGranularity.SINGLE_DAY -> {
            if (row !in 0 until SINGLE_DAY_GRID_ROWS) return null
            val index = column * SINGLE_DAY_GRID_ROWS + row
            singleDayCells.getOrNull(index)?.let { SelectedHeatCell(it.first, it.second) }
        }
        HeatMapGranularity.DAY -> {
            if (row !in 0..6) return null
            val weekStart = dayGridWeeks.getOrNull(column) ?: return null
            val date = weekStart.plus(row, DateTimeUnit.DAY)
            SelectedHeatCell(date, countByDate[date])
        }
        HeatMapGranularity.WEEK -> {
            if (row != 0) return null
            weekCells.getOrNull(column)?.let { SelectedHeatCell(it.first, it.second) }
        }
        HeatMapGranularity.WEEK_GRID -> {
            if (row !in 0 until WEEK_GRID_ROWS) return null
            val index = column * WEEK_GRID_ROWS + row
            weekCells.getOrNull(index)?.let { SelectedHeatCell(it.first, it.second) }
        }
        HeatMapGranularity.MONTH -> {
            if (row != 0) return null
            monthCells.getOrNull(column)?.let { SelectedHeatCell(it.first, it.second) }
        }
    }
}

/** Human-readable label for a selected cell's period, matching its granularity. */
private fun heatCellDateLabel(date: LocalDate, granularity: HeatMapGranularity): String {
    val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return when (granularity) {
        HeatMapGranularity.SINGLE_DAY, HeatMapGranularity.DAY -> "$month ${date.dayOfMonth}"
        HeatMapGranularity.WEEK, HeatMapGranularity.WEEK_GRID -> "Week of $month ${date.dayOfMonth}"
        HeatMapGranularity.MONTH -> "$month ${date.year}"
    }
}

/** Formats a heat map value as a whole number when integral, otherwise to one decimal place. */
private fun formatHeatValue(value: Double): String {
    val rounded = (value * 10).roundToInt() / 10.0
    return if (rounded == floor(rounded)) rounded.toInt().toString() else rounded.toString()
}

@Composable
private fun LineGraphChart(
    points: List<DataPoint>,
    windowStart: LocalDate,
    windowEnd: LocalDate,
    visibleDays: Int = Int.MAX_VALUE
) {
    if (points.isEmpty()) {
        Text(stringResource(R.string.detail_no_answers), style = MaterialTheme.typography.bodySmall)
        return
    }
    val tz = TimeZone.currentSystemDefault()
    val windowDays = remember(windowStart, windowEnd) {
        (windowStart.until(windowEnd, DateTimeUnit.DAY) + 1).toInt().coerceAtLeast(1)
    }
    // Show a sliding window of `visibleDays` anchored on the freshest data, scrolling for older.
    // When the whole range already fits the window (or all-time), fit it all with no scrolling.
    val fitAll = visibleDays >= windowDays
    val zoomState = rememberVicoZoomState(
        zoomEnabled = false,
        initialZoom = if (fitAll) Zoom.Content else Zoom.x(visibleDays.toDouble())
    )
    val scrollState = rememberVicoScrollState(
        scrollEnabled = !fitAll,
        initialScroll = Scroll.Absolute.End
    )
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
        scrollState = scrollState,
        zoomState = zoomState,
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

/** Maximum number of bubbles drawn; keeps the busiest packed bubble charts legible. Entries are pre-sorted by count. */
private const val MAX_BUBBLES = 25

/** Pinch-zoom bounds for the full-screen packed bubble chart. */
private const val MIN_BUBBLE_ZOOM = 1f
private const val MAX_BUBBLE_ZOOM = 6f

@Composable
private fun PackedBubbleChart(
    entries: List<NamedCount>,
    palette: ChartPalettePreference,
    zoomable: Boolean = false
) {
    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.detail_no_answers), style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    val textMeasurer = rememberTextMeasurer()
    val maxCount = entries.maxOf { it.count }.toFloat()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val stops = palette.paletteStops

    val shown = remember(entries) { entries.take(MAX_BUBBLES) }

    // Pack once per data set. Radius scales with sqrt(count) so a bubble's *area* encodes
    // frequency. Positions are packed tangentially around a center for an organic cluster.
    val packed = remember(shown) {
        val circles = shown.map { PackedCircle(it, sqrt(it.count.toDouble())) }
        packSiblings(circles)
        circles
    }

    val bounds = remember(packed) {
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        packed.forEach { c ->
            minX = min(minX, c.x - c.r)
            maxX = max(maxX, c.x + c.r)
            minY = min(minY, c.y - c.r)
            maxY = max(maxY, c.y + c.r)
        }
        PackBounds(minX, minY, maxX, maxY)
    }

    val drawBubbles: DrawScope.() -> Unit = {
        // Scale the packed cluster to fill the available space (works for both the small
        // card thumbnail and the full-screen view), keeping its aspect ratio and centering it.
        val contentW = (bounds.maxX - bounds.minX).coerceAtLeast(1e-6)
        val contentH = (bounds.maxY - bounds.minY).coerceAtLeast(1e-6)
        val scale = (minOf(size.width / contentW, size.height / contentH) * 0.98).toFloat()
        val offsetX = (size.width - (contentW * scale).toFloat()) / 2f
        val offsetY = (size.height - (contentH * scale).toFloat()) / 2f

        packed.forEach { c ->
            val cx = ((c.x - bounds.minX) * scale).toFloat() + offsetX
            val cy = ((c.y - bounds.minY) * scale).toFloat() + offsetY
            val r = (c.r * scale).toFloat()

            val intensity = (c.entry.count / maxCount).coerceIn(0f, 1f)
            val bubbleColor = stops.colorAt(intensity, isDark)
            drawCircle(color = bubbleColor, radius = r, center = Offset(cx, cy))

            // Only label bubbles big enough to read; smaller ones stay as plain dots (thumbnail).
            if (r < 18f) return@forEach
            val textColor = if (bubbleColor.luminance() > 0.4f) Color.Black else Color.White

            val wordPx = (r * 0.5f).coerceIn(9f, 26f)
            val wordLayout = textMeasurer.measure(
                text = c.entry.label,
                style = TextStyle(color = textColor, fontSize = wordPx.toSp(), fontWeight = FontWeight.Bold),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                constraints = Constraints(maxWidth = (r * 1.7f).toInt().coerceAtLeast(1))
            )
            // Word centered on the bubble's center.
            drawText(
                wordLayout,
                topLeft = Offset(cx - wordLayout.size.width / 2f, cy - wordLayout.size.height / 2f)
            )

            // Count tucked just beneath the word, only when there is room.
            if (r >= 26f) {
                val countLayout = textMeasurer.measure(
                    text = c.entry.count.toString(),
                    style = TextStyle(color = textColor.copy(alpha = 0.8f), fontSize = (wordPx * 0.7f).toSp()),
                    maxLines = 1
                )
                drawText(
                    countLayout,
                    topLeft = Offset(
                        cx - countLayout.size.width / 2f,
                        cy + wordLayout.size.height / 2f + 1f
                    )
                )
            }
        }
    }

    if (!zoomable) {
        Canvas(modifier = Modifier.fillMaxSize(), onDraw = drawBubbles)
        return
    }

    // Full-screen: pinch to zoom and drag to pan. Double-tap resets. Panning is clamped so the
    // cluster can't be dragged entirely out of view, and zoom-out is clamped back to fit.
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(MIN_BUBBLE_ZOOM, MAX_BUBBLE_ZOOM)
                    val maxX = (viewportSize.width * (scale - 1f)) / 2f
                    val maxY = (viewportSize.height * (scale - 1f)) / 2f
                    offset = Offset(
                        (offset.x + pan.x).coerceIn(-maxX, maxX),
                        (offset.y + pan.y).coerceIn(-maxY, maxY)
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { scale = 1f; offset = Offset.Zero })
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            onDraw = drawBubbles
        )
    }
}

/** A circle in the packing layout. Position is filled in by [packSiblings]; [r] encodes frequency. */
private class PackedCircle(val entry: NamedCount, val r: Double) {
    var x: Double = 0.0
    var y: Double = 0.0
}

private class PackBounds(val minX: Double, val minY: Double, val maxX: Double, val maxY: Double)

/** Front-chain node wrapping a [PackedCircle] in a circular doubly-linked list. */
private class PackNode(val circle: PackedCircle) {
    var prev: PackNode? = null
    var next: PackNode? = null
}

/**
 * Positions [circles] tangentially with no overlaps, nestled around a shared center, by porting
 * d3-hierarchy's front-chain `packSiblings` algorithm. Mutates each circle's x/y in place.
 */
private fun packSiblings(circles: List<PackedCircle>) {
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

    // Initialize the front chain with the first three circles.
    var nodeA = PackNode(a)
    var nodeB = PackNode(b)
    val nodeC = PackNode(c)
    nodeA.next = nodeC; nodeC.prev = nodeA
    nodeB.next = nodeA; nodeA.prev = nodeB
    nodeC.next = nodeB; nodeB.prev = nodeC

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FollowUpChartSection(
    followUp: FollowUpVisualization,
    selectedTimeframe: Timeframe,
    onTimeframeSelect: (Timeframe) -> Unit,
    chartPalette: ChartPalettePreference
) {
    var selectedChartIndex by rememberSaveable(followUp.questionId) { mutableStateOf(0) }
    var showTypePicker by remember { mutableStateOf(false) }
    var showFullScreen by remember { mutableStateOf(false) }
    val safeIndex = selectedChartIndex.coerceAtMost(followUp.visualizations.lastIndex)

    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = followUp.questionText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.weight(1f)) {
                    NudgeryChart(
                        visualization = followUp.visualizations[safeIndex],
                        chartPalette = chartPalette
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (followUp.visualizations.size > 1) {
                        IconButton(onClick = { showTypePicker = true }) {
                            Icon(Icons.Outlined.BarChart, contentDescription = stringResource(R.string.detail_edit_chart_type))
                        }
                    }
                    IconButton(onClick = { showFullScreen = true }) {
                        Icon(Icons.Outlined.ZoomIn, contentDescription = stringResource(R.string.detail_expand_chart))
                    }
                }
            }
        }
    }

    if (showTypePicker) {
        ChartTypePickerSheet(
            visualizations = followUp.visualizations,
            safeIndex = safeIndex,
            onSelectIndex = { selectedChartIndex = it },
            onDismiss = { showTypePicker = false }
        )
    }

    if (showFullScreen) {
        FullScreenChartDialog(
            visualizations = followUp.visualizations,
            selectedIndex = selectedChartIndex,
            onSelectIndex = { selectedChartIndex = it },
            selectedTimeframe = selectedTimeframe,
            onTimeframeSelect = onTimeframeSelect,
            onDismiss = { showFullScreen = false },
            chartPalette = chartPalette,
            title = followUp.questionText
        )
    }
}

private data class AnswerSession(
    val sessionKey: String,
    val mainAnswer: AnswerRow?,
    val followUps: List<AnswerRow>
)

@Composable
private fun AnswerTableSection(
    answers: List<AnswerRow>,
    onSetHidden: (String, Boolean) -> Unit
) {
    var tableExpanded by rememberSaveable { mutableStateOf(false) }
    // Tracks which session keys have their follow-up section expanded; reset on recomposition is fine
    var expandedSessionKeys by remember { mutableStateOf(emptySet<String>()) }
    var confirmHideId by remember { mutableStateOf<String?>(null) }

    val sessions = remember(answers) {
        answers
            .groupBy { it.scheduledAt }
            .entries
            .sortedByDescending { (scheduledAt, _) -> scheduledAt }
            .map { (scheduledAt, sessionAnswers) ->
                val sorted = sessionAnswers.sortedBy { it.questionOrderIndex }
                AnswerSession(
                    sessionKey = scheduledAt.toString(),
                    mainAnswer = sorted.firstOrNull { it.questionOrderIndex == 0 },
                    followUps = sorted.filter { it.questionOrderIndex > 0 }
                )
            }
    }

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
                    .clickable { tableExpanded = !tableExpanded }
                    .padding(16.dp)
            ) {
                // A "response" is one main-question answer plus any follow-up answers it triggered,
                // counted as a single unit.
                val responseCount = answers.count { it.questionOrderIndex == 0 }
                Text(
                    text = if (responseCount == 0)
                        stringResource(R.string.detail_no_responses)
                    else
                        stringResource(R.string.detail_responses_header, responseCount),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (tableExpanded) "▲" else "▼",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = tableExpanded) {
                Column {
                    HorizontalDivider()
                    sessions.forEachIndexed { index, session ->
                        session.mainAnswer?.let { main ->
                            AnswerTableRow(
                                answer = main,
                                onHide = { confirmHideId = main.answerId },
                                onUnhide = { onSetHidden(main.answerId, false) }
                            )
                        }

                        if (session.followUps.isNotEmpty()) {
                            val sessionExpanded = session.sessionKey in expandedSessionKeys
                            FollowUpToggleRow(
                                count = session.followUps.size,
                                expanded = sessionExpanded,
                                onClick = {
                                    expandedSessionKeys = if (sessionExpanded)
                                        expandedSessionKeys - session.sessionKey
                                    else
                                        expandedSessionKeys + session.sessionKey
                                }
                            )
                            AnimatedVisibility(visible = sessionExpanded) {
                                Column {
                                    session.followUps.forEach { followUp ->
                                        FollowUpAnswerRows(
                                            answer = followUp,
                                            onHide = { confirmHideId = followUp.answerId },
                                            onUnhide = { onSetHidden(followUp.answerId, false) }
                                        )
                                    }
                                }
                            }
                        }

                        if (index < sessions.lastIndex) HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowUpToggleRow(count: Int, expanded: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 32.dp, end = 16.dp, top = 6.dp, bottom = 6.dp)
    ) {
        Text(
            text = pluralStringResource(R.plurals.detail_session_followup_count, count, count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (expanded) "▼" else "▶",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FollowUpAnswerRows(
    answer: AnswerRow,
    onHide: () -> Unit,
    onUnhide: () -> Unit
) {
    Column(modifier = Modifier.padding(start = 24.dp)) {
        Text(
            text = stringResource(R.string.detail_followup_question_label, answer.questionText),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp)
        )
        AnswerTableRow(answer = answer, onHide = onHide, onUnhide = onUnhide, showDate = false)
    }
}

private fun buildWeekCells(
    counts: List<DailyCount>,
    windowStart: LocalDate,
    windowEnd: LocalDate
): List<Pair<LocalDate, Double?>> {
    val weeklyMap = mutableMapOf<LocalDate, Double>()
    counts.forEach { dc ->
        val weekMonday = dc.date.minus(dc.date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
        weeklyMap[weekMonday] = (weeklyMap[weekMonday] ?: 0.0) + dc.value
    }
    val gridStart = windowStart.minus(windowStart.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    val result = mutableListOf<Pair<LocalDate, Double?>>()
    var week = gridStart
    while (week <= windowEnd) {
        result.add(week to weeklyMap[week])  // null = no data recorded this week
        week = week.plus(7, DateTimeUnit.DAY)
    }
    return result
}

private fun buildMonthCells(
    counts: List<DailyCount>,
    windowStart: LocalDate,
    windowEnd: LocalDate
): List<Pair<LocalDate, Double?>> {
    val monthlyMap = mutableMapOf<LocalDate, Double>()
    counts.forEach { dc ->
        val monthStart = LocalDate(dc.date.year, dc.date.month, 1)
        monthlyMap[monthStart] = (monthlyMap[monthStart] ?: 0.0) + dc.value
    }
    val gridStart = LocalDate(windowStart.year, windowStart.month, 1)
    val result = mutableListOf<Pair<LocalDate, Double?>>()
    var month = gridStart
    while (month <= windowEnd) {
        result.add(month to monthlyMap[month])  // null = no data recorded this month
        month = month.plus(1, DateTimeUnit.MONTH)
    }
    return result
}

@Composable
private fun AnswerTableRow(
    answer: AnswerRow,
    onHide: () -> Unit,
    onUnhide: () -> Unit,
    showDate: Boolean = true
) {
    val tz = TimeZone.currentSystemDefault()
    val dateTime = answer.answeredAt.toLocalDateTime(tz)
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
            if (showDate) {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
