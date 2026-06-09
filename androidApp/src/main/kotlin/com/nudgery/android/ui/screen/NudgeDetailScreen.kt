// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.core.cartesian.axis.Axis
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.Defaults
import com.patrykandpatrick.vico.core.common.component.LineComponent
import com.patrykandpatrick.vico.core.common.data.ExtraStore
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
import com.nudgery.android.ui.theme.LocalEmojiScale
import com.nudgery.android.ui.theme.emojiScaledAppBarHeight
import com.nudgery.android.ui.theme.emojiScaledStyle
import com.nudgery.shared.util.isSingleEmoji
import com.nudgery.shared.usecase.windowStepDays
import com.nudgery.shared.model.VisualizationData
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.daysUntil
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until
import android.content.Intent
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
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

    // One shared window for the whole dashboard; every chart reads it and any drag/scrubber moves it.
    val windowNav = ChartWindowNav(
        windowStart = uiState.windowStart,
        windowEnd = uiState.windowEnd,
        dataStart = uiState.dataStart,
        dataEnd = uiState.dataEnd,
        canShiftOlder = uiState.canShiftOlder,
        canShiftNewer = uiState.canShiftNewer,
        windowStepDays = windowStepDays(uiState.selectedTimeframe),
        onShiftDays = { viewModel.shiftWindowDays(it) }
    )

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

    val titleStyle = MaterialTheme.typography.titleLarge
    // Grow the header just enough for a tall emoji-only title at high emoji scale, so it isn't
    // clipped by the bar's fixed height (ENGINEERING_DECISIONS.md ED-15).
    val barHeight = with(LocalDensity.current) {
        emojiScaledAppBarHeight(
            text = uiState.nudgeName,
            scale = LocalEmojiScale.current,
            baseTitleSize = titleStyle.fontSize,
            defaultHeight = TopAppBarDefaults.TopAppBarExpandedHeight
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = barHeight,
                title = {
                    Text(
                        text = uiState.nudgeName,
                        style = emojiScaledStyle(uiState.nudgeName, titleStyle),
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
            // Header block: question text, schedule, and follow-up rows. Grouped into a single item
            // with tight internal spacing so these single-line rows don't inherit the 16dp inter-card
            // spacing of the LazyColumn — that gap, on top of the rows' 48dp icon-button height, left
            // a lot of dead vertical space between them.
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Main question text — extra top padding so it isn't crowded against the title bar.
                    if (uiState.mainQuestionText.isNotEmpty()) {
                        Text(
                            text = uiState.mainQuestionText,
                            style = emojiScaledStyle(uiState.mainQuestionText, MaterialTheme.typography.titleMedium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    // Schedule row
                    uiState.schedule?.let { schedule ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = com.nudgery.android.viewmodel.ScheduleFormState.fromSchedule(schedule).toDescription(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            RowEditButton(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = stringResource(R.string.detail_edit_schedule),
                                onClick = onEditScheduleClick
                            )
                        }
                    }

                    // Follow-up questions row — hidden for free-text main questions, which can't have them.
                    if (uiState.mainQuestionType?.allowsFollowUps == true) {
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
                            RowEditButton(
                                imageVector = Icons.Outlined.QuestionAnswer,
                                contentDescription = stringResource(R.string.detail_edit_followups),
                                onClick = onEditFollowUpsClick
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
                        chartPalette = chartPalette,
                        windowLabel = uiState.windowLabel,
                        nav = windowNav
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
                        chartPalette = chartPalette,
                        nav = windowNav
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
        // A Dialog's own window does not reliably report the system-bar insets to Compose, so capture
        // them here in the activity composition (where edge-to-edge makes them correct) and hand them
        // down for the dialog to apply explicitly. This call site sits outside the parent Scaffold's
        // content, so the insets haven't been consumed yet.
        val systemBarInsets = WindowInsets.systemBars.asPaddingValues()
        FullScreenChartDialog(
            visualizations = uiState.visualizations,
            selectedIndex = selectedChartIndex,
            onSelectIndex = { selectedChartIndex = it },
            selectedTimeframe = uiState.selectedTimeframe,
            onTimeframeSelect = { viewModel.selectTimeframe(it) },
            onDismiss = { showFullScreenChart = false },
            chartPalette = chartPalette,
            windowLabel = uiState.windowLabel,
            systemBarInsets = systemBarInsets,
            nav = windowNav,
            // The full-screen title is the question being charted, not the chart style (the chart
            // style is already chosen via the chart-type picker). Matches the follow-up charts, which
            // already title by their question text.
            title = uiState.mainQuestionText
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
    chartPalette: ChartPalettePreference,
    windowLabel: String,
    nav: ChartWindowNav
) {
    var showTypePicker by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    val safeIndex = selectedIndex.coerceAtMost(visualizations.lastIndex)

    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.weight(1f)) {
                    NudgeryChart(visualization = visualizations[safeIndex], chartPalette = chartPalette, nav = nav)
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

            if (windowLabel.isNotEmpty()) {
                Text(
                    text = windowLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

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

/** Padding around the 24dp icon in [RowEditButton], giving a ~40dp touch target (24 + 2×8). */
private val ROW_EDIT_BUTTON_PADDING = 8.dp

/**
 * A compact edit affordance for the detail-screen header rows. A default [IconButton] forces a 48dp
 * row, which left the single-line schedule and follow-up rows floating in dead vertical space; this
 * keeps the row short (a ~40dp touch target) so those lines sit closer together. The icon's
 * [contentDescription] is the accessible label, and the press exposes a Button role.
 */
@Composable
private fun RowEditButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(ROW_EDIT_BUTTON_PADDING),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = imageVector, contentDescription = contentDescription)
    }
}

/**
 * The title for a full-screen chart: the text of the question being charted ([questionText]) rather
 * than the chart-style name — the chart style is already chosen in the chart-type picker. Falls back
 * to [chartStyleLabel] only when the question has no text (e.g. an emoji-only question), so the bar
 * is never blank. Both the main chart and follow-up charts title by their own question text.
 */
internal fun fullScreenChartTitle(questionText: String?, chartStyleLabel: String): String =
    questionText?.takeIf { it.isNotBlank() } ?: chartStyleLabel

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
    systemBarInsets: PaddingValues = PaddingValues(),
    nav: ChartWindowNav = ChartWindowNav.None,
    windowLabel: String = "",
    title: String? = null
) {
    var showTypePicker by remember { mutableStateOf(false) }
    val safeIndex = selectedIndex.coerceAtMost(visualizations.lastIndex)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // A Dialog owns a separate window that does not reliably report the system-bar insets to
        // Compose, so its own WindowInsets read as zero. We instead apply [systemBarInsets] —
        // captured by the caller in the activity composition — explicitly (see the padded Box below),
        // and zero out the Scaffold's/TopAppBar's own (unreliable) inset handling so it can't double
        // up. Opting the window out of decor-fit makes it draw edge-to-edge so that explicit padding,
        // rather than the platform decor, is the single source of truth for placement.
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                // Insets are applied by the outer padding (systemBarInsets), so the Scaffold and its
                // bars must add none of their own — otherwise the bottom chips would be double-inset
                // or (when the dialog reports zero) fall under the navigation bar.
                modifier = Modifier.padding(systemBarInsets),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    TopAppBar(
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        title = { Text(fullScreenChartTitle(title, visualizationLabel(visualizations[safeIndex]))) },
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
                        zoomable = true,
                        nav = nav
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (windowLabel.isNotEmpty()) {
                        Text(
                            text = windowLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    // The navigation-bar inset is already applied by the outer Scaffold padding
                    // (systemBarInsets), so the selector only adds its own breathing room.
                    TimeframeSelector(
                        selectedTimeframe = selectedTimeframe,
                        onTimeframeSelect = onTimeframeSelect,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
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

/**
 * Shared-window navigation handed to charts so the whole dashboard moves together: dragging a
 * time-based chart, or the scrubber under a categorical chart, shifts the same window.
 */
private data class ChartWindowNav(
    val windowStart: LocalDate?,
    val windowEnd: LocalDate?,
    val dataStart: LocalDate?,
    val dataEnd: LocalDate?,
    val canShiftOlder: Boolean,
    val canShiftNewer: Boolean,
    // Days the window slides per one-cell step, matching the heat map's cell size (a week on the
    // yearly view, a day otherwise) so dragging moves the grid a whole square at a time.
    val windowStepDays: Int,
    val onShiftDays: (Int) -> Unit
) {
    /** There is somewhere to scroll to (not all-time, and history exists beyond the window). */
    val navigable: Boolean get() = canShiftOlder || canShiftNewer

    val windowDays: Int
        get() = if (windowStart != null && windowEnd != null)
            (windowStart.until(windowEnd, DateTimeUnit.DAY) + 1).toInt().coerceAtLeast(1)
        else 1

    companion object {
        val None = ChartWindowNav(null, null, null, null, false, false, 1, {})
    }
}

@Composable
private fun NudgeryChart(
    visualization: VisualizationData,
    chartPalette: ChartPalettePreference,
    modifier: Modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
    // Only the full-screen packed bubble chart enables pinch-to-zoom.
    zoomable: Boolean = false,
    nav: ChartWindowNav = ChartWindowNav.None
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Key on the chart *type*, not the data object. A window shift reloads the data into a new
        // VisualizationData instance every drag step; keying on the whole object tore down and
        // rebuilt the chart each step, which replayed the line's grow-from-zero entrance animation
        // and killed any in-flight drag gesture (forcing a re-tap per day). Keying on the type keeps
        // the same composable across reloads — Vico animates the data diff, gestures survive a
        // continuous multi-day drag — and only rebuilds when the user pages to a different chart.
        key(visualization::class) {
            when (visualization) {
                // Time-based charts: drag the chart itself to slide the shared window.
                is VisualizationData.CalendarHeatMap -> Box(Modifier.fillMaxSize().timeWindowDrag(nav)) {
                    CalendarHeatMapChart(
                        counts = visualization.dailyCounts,
                        windowStart = visualization.windowStart,
                        windowEnd = visualization.windowEnd,
                        weekAnchor = visualization.weekAnchor,
                        granularity = visualization.granularity,
                        palette = chartPalette,
                        fillViewport = visualization.fillViewport
                    )
                }
                is VisualizationData.LineGraph -> Box(Modifier.fillMaxSize().timeWindowDrag(nav)) {
                    LineGraphChart(
                        points = visualization.points,
                        windowStart = visualization.windowStart,
                        windowEnd = visualization.windowEnd,
                        visibleDays = visualization.visibleDays,
                        yMin = visualization.yMin,
                        yMax = visualization.yMax
                    )
                }
                // Categorical charts: no time axis, so a scrubber underneath moves the window.
                is VisualizationData.BarChart -> CategoricalChart(nav) { HorizontalBarChart(visualization.entries, chartPalette) }
                is VisualizationData.ColumnChart -> CategoricalChart(nav) { NamedCountChart(visualization.entries, chartPalette) }
                is VisualizationData.PackedBubble -> CategoricalChart(nav) { PackedBubbleChart(visualization.entries, chartPalette, zoomable) }
            }
        }
    }
}

// One full-width swipe across a time-based chart slides the shared window by this many times the
// selected timeframe's span — e.g. 2 weeks per swipe on the weekly view, 2 months on monthly,
// 2 years on yearly. Raise this to scroll through history faster, lower it for finer control.
private const val FULL_SWIPE_TIMEFRAME_MULTIPLIER = 2

/** Horizontal drag that slides the shared window; dragging right reveals older data. */
private fun Modifier.timeWindowDrag(nav: ChartWindowNav): Modifier {
    if (!nav.navigable) return this
    val windowDays = nav.windowDays
    val stepDays = nav.windowStepDays
    return pointerInput(windowDays, stepDays) {
        var accumulated = 0f
        detectHorizontalDragGestures(
            onDragEnd = { accumulated = 0f },
            onDragCancel = { accumulated = 0f }
        ) { _, dragAmount ->
            accumulated += dragAmount
            val daysPerPx = (windowDays * FULL_SWIPE_TIMEFRAME_MULTIPLIER).toFloat() /
                size.width.coerceAtLeast(1)
            // Slide in whole cells (a day, or a week on the yearly view) so the grid moves one
            // square at a time and week buckets stay intact.
            val steps = (accumulated * daysPerPx / stepDays).toInt()
            if (steps != 0) {
                val shiftDays = steps * stepDays
                nav.onShiftDays(shiftDays) // drag right (+dx) → older
                accumulated -= shiftDays / daysPerPx
            }
        }
    }
}

/** A categorical chart with a draggable time strip beneath it (only when there's history to scrub). */
@Composable
private fun CategoricalChart(nav: ChartWindowNav, chart: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) { chart() }
        if (nav.navigable) {
            TimeWindowScrubber(nav, Modifier.fillMaxWidth().padding(top = 6.dp))
        }
    }
}

/** A slim track showing the full history with the current window highlighted; drag to move it. */
@Composable
private fun TimeWindowScrubber(nav: ChartWindowNav, modifier: Modifier = Modifier) {
    val windowStart = nav.windowStart ?: return
    val dataStart = nav.dataStart ?: return
    val dataEnd = nav.dataEnd ?: return

    val totalDays = (dataStart.until(dataEnd, DateTimeUnit.DAY) + 1).toInt().coerceAtLeast(1)
    val windowDays = nav.windowDays
    val startOffsetDays = dataStart.until(windowStart, DateTimeUnit.DAY).toInt()

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val knobColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .height(10.dp)
            .pointerInput(totalDays, nav.windowStepDays) {
                val stepDays = nav.windowStepDays
                var accumulated = 0f
                detectHorizontalDragGestures(
                    onDragEnd = { accumulated = 0f },
                    onDragCancel = { accumulated = 0f }
                ) { _, dragAmount ->
                    accumulated += dragAmount
                    val daysPerPx = totalDays.toFloat() / size.width.coerceAtLeast(1)
                    // Slide in whole cells so the shared window stays week-aligned on the yearly
                    // view (keeping any heat map's week buckets intact).
                    val steps = (accumulated * daysPerPx / stepDays).toInt()
                    if (steps != 0) {
                        val shiftDays = steps * stepDays
                        nav.onShiftDays(-shiftDays) // drag the knob right (+dx) → newer
                        accumulated -= shiftDays / daysPerPx
                    }
                }
            }
    ) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(color = trackColor, size = size, cornerRadius = radius)
        val knobWidth = (windowDays.toFloat() / totalDays * size.width).coerceIn(6f, size.width)
        val knobX = (startOffsetDays.toFloat() / totalDays * size.width)
            .coerceIn(0f, size.width - knobWidth)
        drawRoundRect(
            color = knobColor,
            topLeft = Offset(knobX, 0f),
            size = Size(knobWidth, size.height),
            cornerRadius = radius
        )
    }
}

// The weekly (SINGLE_DAY) heat map shows its 7 days as a single row of large squares: with so few
// cells, one row reads at a glance, where a multi-row grid was confusing. All seven fill the card
// width (SINGLE_DAY_GRID_VISIBLE_COLS targets that many columns on screen) and the row is centered
// vertically in the card.
private const val SINGLE_DAY_GRID_ROWS = 1
private const val SINGLE_DAY_GRID_VISIBLE_COLS = 7

// The yearly view shows about this many month labels across the grid width — spaced ~1/N of the
// canvas apart so the labels stay sparse (three floating markers) rather than one per month.
private const val WEEK_GRID_TARGET_MONTH_LABELS = 3

@Composable
private fun CalendarHeatMapChart(
    counts: List<DailyCount>,
    windowStart: LocalDate,
    windowEnd: LocalDate,
    weekAnchor: LocalDate,
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

    val weekCells = remember(counts, windowStart, windowEnd, weekAnchor) {
        buildWeekCells(counts, windowStart, windowEnd, weekAnchor)
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
                // Monthly, yearly, and all-time: lay every cell out in an auto-fit grid that fills
                // the canvas, no scroll. The granularity keeps the cell count bounded; the grid picks
                // the rows/cols that maximize square cell size for that count. Column-major, oldest
                // at top-left.
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
                    // Yearly spaces its month labels ~1/N of the width apart so only ~N float across
                    // the grid; other granularities keep tight, near-every-period labels.
                    val labelMinGapPx = if (granularity == HeatMapGranularity.WEEK_GRID)
                        availableWidthPx / WEEK_GRID_TARGET_MONTH_LABELS
                    else 4.dp.toPx()
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
                                if (x >= lastLabelEnd + labelMinGapPx) {
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

            // The non-fill path renders the weekly view only (SINGLE_DAY): a single row of day
            // cells, one column per day. Every other timeframe fills the viewport (handled above).
            val numberOfCells = singleDayCells.size
            // Fit all the week's days across the width (capped by the card height, floored at minCellPx).
            val cellPx = minOf(
                availableHeightPx - labelAreaPx,
                maxOf(
                    (availableWidthPx - gapPx * (SINGLE_DAY_GRID_VISIBLE_COLS - 1)) / SINGLE_DAY_GRID_VISIBLE_COLS,
                    minCellPx
                )
            )
            // Center the single short row vertically; it would otherwise float at the top of the card.
            val singleDayTopPx = labelAreaPx + maxOf(0f, (availableHeightPx - labelAreaPx - cellPx) / 2f)

            val contentWidthPx = if (numberOfCells > 0)
                numberOfCells * (cellPx + gapPx) - gapPx else 0f
            val needsScroll = contentWidthPx > availableWidthPx

            LaunchedEffect(contentWidthPx) {
                if (needsScroll) scrollState.scrollTo(scrollState.maxValue)
            }

            val tapModifier = Modifier.pointerInput(cellPx, gapPx, singleDayTopPx, counts) {
                detectTapGestures { offset ->
                    selectedCell = hitTestWeeklyCell(offset, cellPx, gapPx, singleDayTopPx, singleDayCells)
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
                        // A single centered row (SINGLE_DAY_GRID_ROWS == 1): one cell per column,
                        // oldest (left) to newest (right).
                        singleDayCells.forEachIndexed { dayIdx, (date, value) ->
                            val colIdx = dayIdx / SINGLE_DAY_GRID_ROWS
                            val rowIdx = dayIdx % SINGLE_DAY_GRID_ROWS
                            val x = colIdx * (cellPx + gapPx)
                            val y = singleDayTopPx + rowIdx * (cellPx + gapPx)
                            // Replace the month label with a single "Monday" marker on the week's one
                            // Monday square, to orient the row; it sits just above that square. The
                            // weekly view has no competing labels, so the full word fits comfortably.
                            if (date.dayOfWeek == DayOfWeek.MONDAY) {
                                val label = date.dayOfWeek.name
                                    .lowercase().replaceFirstChar { it.uppercase() }
                                val measured = textMeasurer.measure(label, labelStyle)
                                drawText(measured, topLeft = Offset(x, maxOf(0f, y - measured.size.height.toFloat())))
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
                    // Only SINGLE_DAY (weekly) reaches the non-fill path; every other granularity
                    // fills the viewport and is drawn above.
                    else -> Unit
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
/**
 * Maps a tap to a day cell in the weekly single-row heat map (the only non-fill layout). Every other
 * timeframe fills the viewport and is hit-tested by [hitTestFillGrid].
 */
private fun hitTestWeeklyCell(
    offset: Offset,
    cellPx: Float,
    gapPx: Float,
    rowTopPx: Float,
    cells: List<Pair<LocalDate, Double?>>
): SelectedHeatCell? {
    val step = cellPx + gapPx
    if (step <= 0f || offset.x < 0f || offset.y < rowTopPx) return null
    if (((offset.y - rowTopPx) / step).toInt() != 0) return null  // tap is below the single row
    val column = (offset.x / step).toInt()
    return cells.getOrNull(column)?.let { SelectedHeatCell(it.first, it.second) }
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
    visibleDays: Int = Int.MAX_VALUE,
    yMin: Double? = null,
    yMax: Double? = null
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
                    maxX = (windowDays - 1).toDouble(),
                    // Fixed Y bounds (ED: stable axis while scrubbing); null lets Vico auto-fit.
                    minY = yMin,
                    maxY = yMax
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
        // Every window shift swaps the whole dataset, so Vico's default difference animation would
        // interpolate between two unrelated windows — the line appears to "dance" while scrubbing and
        // only settles once dragging stops. Snap to each window's real data instead.
        animationSpec = null,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun HorizontalBarChart(entries: List<NamedCount>, palette: ChartPalettePreference) {
    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.detail_no_answers), style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    val maxCount = remember(entries) { entries.maxOf { it.count }.coerceAtLeast(1) }
    // Each bar's color is fixed to its category (orderFraction), so a category keeps its hue as the
    // timeframe moves and the bars re-sort by count.
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val stops = palette.paletteStops
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
                                .background(stops.colorAt(entry.orderFraction, isDark), MaterialTheme.shapes.extraSmall)
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

// Column chart x-axis labels are drawn smaller than Vico's 12sp default and tilted at a shallow
// angle. Vico's aligned item placer thins labels (e.g. shows only every other one) whenever the
// widest label is wider than the per-column spacing; shrinking and angling each label cuts its
// horizontal footprint enough to keep them all visible, in both the inline and full-screen views.
private val COLUMN_AXIS_LABEL_SIZE = 9.sp
private const val COLUMN_AXIS_LABEL_ROTATION_DEGREES = 45f

@Composable
private fun NamedCountChart(entries: List<NamedCount>, palette: ChartPalettePreference) {
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
    // One column color per category, fixed to its orderFraction so a category keeps its hue as the
    // timeframe moves. Vico picks the column for each bar by its x value (the category's index).
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val columnProvider = remember(entries, isDark, palette) {
        val stops = palette.paletteStops
        val columns = entries.map { entry ->
            LineComponent(fill(stops.colorAt(entry.orderFraction, isDark)), Defaults.COLUMN_WIDTH)
        }
        object : ColumnCartesianLayer.ColumnProvider {
            override fun getColumn(
                entry: ColumnCartesianLayerModel.Entry,
                seriesIndex: Int,
                extraStore: ExtraStore
            ): LineComponent = columns[entry.x.toInt().coerceIn(0, columns.lastIndex)]

            override fun getWidestSeriesColumn(seriesIndex: Int, extraStore: ExtraStore): LineComponent =
                columns.first()
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(columnProvider = columnProvider),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                label = rememberAxisLabelComponent(textSize = COLUMN_AXIS_LABEL_SIZE),
                labelRotationDegrees = COLUMN_AXIS_LABEL_ROTATION_DEGREES,
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
        // See LineGraphChart: the dataset is fully replaced on every window shift, so the default
        // difference animation would tween between unrelated windows. Snap to each window's data.
        animationSpec = null,
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
    val emojiScale = LocalEmojiScale.current // captured for the draw lambda (ED-14, full-screen floor)
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

            // A lone emoji floats in a sea of bubble; with no neighbors it has room to be twice the
            // size of regular word labels (and to use more of the bubble's width before wrapping).
            val emojiOnly = isSingleEmoji(c.entry.label)
            // The emoji scale (ED-14) is a *floor*: enforced only in the full-screen (zoomable) chart,
            // where lone emoji are at least the chosen size and may go larger; the thumbnail stays small.
            val emojiFloor = if (zoomable) emojiScale else 1f
            val wordPx = if (emojiOnly) (r * 1.0f).coerceIn(18f * emojiFloor, 52f * emojiFloor)
                         else (r * 0.5f).coerceIn(9f, 26f)
            val labelMaxWidth = (r * if (emojiOnly) 2f else 1.7f).toInt().coerceAtLeast(1)
            val wordLayout = if (emojiOnly) {
                // An emoji is atomic — never ellipsize it to "…" (a wide multi-person glyph would
                // otherwise vanish into an ellipsis). Measure it unconstrained, then shrink the glyph
                // to fit the bubble if it's wider than the bubble.
                val natural = textMeasurer.measure(
                    text = c.entry.label,
                    style = TextStyle(color = textColor, fontSize = wordPx.toSp(), fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                if (natural.size.width > labelMaxWidth && natural.size.width > 0) {
                    val fitted = wordPx * labelMaxWidth / natural.size.width
                    textMeasurer.measure(
                        text = c.entry.label,
                        style = TextStyle(color = textColor, fontSize = fitted.toSp(), fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                } else natural
            } else {
                textMeasurer.measure(
                    text = c.entry.label,
                    style = TextStyle(color = textColor, fontSize = wordPx.toSp(), fontWeight = FontWeight.Bold),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    constraints = Constraints(maxWidth = labelMaxWidth)
                )
            }
            // The count is shown only in the full-screen view: the thumbnail conveys frequency by
            // bubble size, where a tiny digit would be unreadable and would spill past the small
            // bubble's edge. When shown, the label and count are centered together as one block so
            // neither clips the bubble.
            val countLayout = if (zoomable && r >= 26f) {
                textMeasurer.measure(
                    text = c.entry.count.toString(),
                    style = TextStyle(color = textColor.copy(alpha = 0.8f), fontSize = (wordPx * 0.7f).toSp()),
                    maxLines = 1
                )
            } else null

            val countGap = if (countLayout != null) 2f else 0f
            val groupHeight = wordLayout.size.height + countGap + (countLayout?.size?.height ?: 0)
            val wordTop = cy - groupHeight / 2f
            drawText(wordLayout, topLeft = Offset(cx - wordLayout.size.width / 2f, wordTop))
            if (countLayout != null) {
                drawText(
                    countLayout,
                    topLeft = Offset(
                        cx - countLayout.size.width / 2f,
                        wordTop + wordLayout.size.height + countGap
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FollowUpChartSection(
    followUp: FollowUpVisualization,
    selectedTimeframe: Timeframe,
    onTimeframeSelect: (Timeframe) -> Unit,
    chartPalette: ChartPalettePreference,
    nav: ChartWindowNav = ChartWindowNav.None
) {
    var selectedChartIndex by rememberSaveable(followUp.questionId) { mutableStateOf(0) }
    var showTypePicker by remember { mutableStateOf(false) }
    var showFullScreen by remember { mutableStateOf(false) }
    val safeIndex = selectedChartIndex.coerceAtMost(followUp.visualizations.lastIndex)

    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = followUp.questionText,
                style = emojiScaledStyle(followUp.questionText, MaterialTheme.typography.labelMedium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.weight(1f)) {
                    NudgeryChart(
                        visualization = followUp.visualizations[safeIndex],
                        chartPalette = chartPalette,
                        nav = nav
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
            nav = nav,
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

/**
 * Buckets daily [counts] into one cell per 7-day week for the WEEK / WEEK_GRID heat maps. Weeks are
 * counted from [weekAnchor] (the data-collection start) rather than calendar Mondays, so the first
 * cell is a full week measured from when logging began instead of a partial week clipped by a
 * mid-week window edge. The grid starts at the first whole period that begins inside the window;
 * when data predates the window (e.g. the yearly view's year-ago edge) the ≤6 leading clipped days
 * are dropped rather than shown as a misleadingly low partial cell. A `null` value = no data that
 * week.
 */
internal fun buildWeekCells(
    counts: List<DailyCount>,
    windowStart: LocalDate,
    windowEnd: LocalDate,
    weekAnchor: LocalDate
): List<Pair<LocalDate, Double?>> {
    // Start of the 7-day period (anchored on weekAnchor) that contains [date].
    fun weekStartOf(date: LocalDate): LocalDate {
        val periods = weekAnchor.daysUntil(date).floorDiv(7)
        return weekAnchor.plus(periods * 7, DateTimeUnit.DAY)
    }

    val weeklyMap = mutableMapOf<LocalDate, Double>()
    counts.forEach { dc ->
        val weekStart = weekStartOf(dc.date)
        weeklyMap[weekStart] = (weeklyMap[weekStart] ?: 0.0) + dc.value
    }

    // First whole period that begins on or after the window start (so the leading cell is never a
    // window-clipped partial week).
    var gridStart = weekStartOf(windowStart)
    if (gridStart < windowStart) gridStart = gridStart.plus(7, DateTimeUnit.DAY)

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
                style = emojiScaledStyle(answer.displayValue, MaterialTheme.typography.bodyMedium),
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
