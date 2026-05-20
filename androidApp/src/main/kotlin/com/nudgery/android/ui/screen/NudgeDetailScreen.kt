package com.nudgery.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.nudgery.android.viewmodel.NudgeDetailViewModel
import com.nudgery.shared.model.DailyCount
import com.nudgery.shared.model.DataPoint
import com.nudgery.shared.model.NamedCount
import com.nudgery.shared.model.Timeframe
import com.nudgery.shared.model.VisualizationData
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

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
                Button(
                    onClick = onAnswerNow,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.detail_answer_now))
                }
            }

            // Chart section
            if (uiState.visualizations.isNotEmpty()) {
                item {
                    ChartSection(
                        visualizations = uiState.visualizations,
                        selectedTimeframe = uiState.selectedTimeframe,
                        onTimeframeSelect = { viewModel.selectTimeframe(it) },
                        onExport = { viewModel.exportAnswers(com.nudgery.shared.model.ExportFormat.CSV) }
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

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChartSection(
    visualizations: List<VisualizationData>,
    selectedTimeframe: Timeframe,
    onTimeframeSelect: (Timeframe) -> Unit,
    onExport: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.weight(1f)) {
                    NudgeryChart(visualization = visualizations.first())
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { /* chart type selector — TODO */ }) {
                        Icon(Icons.Outlined.BarChart, contentDescription = stringResource(R.string.detail_edit_chart_type))
                    }
                    IconButton(onClick = onExport) {
                        Icon(Icons.Outlined.Download, contentDescription = stringResource(R.string.detail_export))
                    }
                    IconButton(onClick = { /* full screen chart — TODO */ }) {
                        Icon(Icons.Outlined.ZoomIn, contentDescription = stringResource(R.string.detail_expand_chart))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    }
}

@Composable
private fun NudgeryChart(visualization: VisualizationData, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        when (visualization) {
            is VisualizationData.CalendarHeatMap -> CalendarHeatMapChart(visualization.dailyCounts)
            is VisualizationData.LineGraph -> LineGraphChart(visualization.points)
            is VisualizationData.BarChart -> NamedCountChart(visualization.entries)
            is VisualizationData.ColumnChart -> NamedCountChart(visualization.entries)
            is VisualizationData.TagCloud -> TagCloudChart(visualization.entries)
        }
    }
}

@Composable
private fun CalendarHeatMapChart(counts: List<DailyCount>) {
    // Simple placeholder: show count summary until a full calendar grid is implemented
    val total = counts.sumOf { it.value }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${counts.size} days", style = MaterialTheme.typography.bodySmall)
        Text("%.1f avg".format(if (counts.isEmpty()) 0.0 else total / counts.size),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun LineGraphChart(points: List<DataPoint>) {
    if (points.isEmpty()) {
        Text(stringResource(R.string.detail_no_answers), style = MaterialTheme.typography.bodySmall)
        return
    }
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points) {
        modelProducer.runTransaction {
            lineSeries { series(points.map { it.value }) }
        }
    }
    val tz = TimeZone.currentSystemDefault()
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = object : CartesianValueFormatter {
                    override fun format(
                        context: CartesianMeasuringContext,
                        value: Double,
                        verticalAxisPosition: Axis.Position.Vertical?
                    ): CharSequence {
                        val idx = value.toInt().coerceIn(0, points.lastIndex)
                        val dt = points[idx].at.toLocalDateTime(tz)
                        return "${dt.monthNumber}/${dt.dayOfMonth}"
                    }
                }
            ),
        ),
        modelProducer = modelProducer,
        modifier = Modifier.fillMaxSize(),
    )
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
