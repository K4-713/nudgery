package com.nudgery.android.ui.screen

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nudgery.android.R
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.nudgery.android.viewmodel.QuestionFormState
import com.nudgery.android.viewmodel.ScheduleFormState
import com.nudgery.android.viewmodel.toAbbreviation
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.model.ScheduleType
import com.nudgery.shared.model.TriggerOperator
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

// ---- Step 1: Question ----

@Composable
fun QuestionStep(
    nudgeName: String,
    onNameChange: (String) -> Unit,
    question: QuestionFormState,
    onQuestionChange: (QuestionFormState) -> Unit,
    existingFollowUps: List<QuestionFormState> = emptyList()
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.step_question_title), style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = nudgeName,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.field_nudge_name)) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = question.text,
            onValueChange = { onQuestionChange(question.copy(text = it)) },
            label = { Text(stringResource(R.string.field_question_text)) },
            placeholder = {
                val hints = stringArrayResource(R.array.question_text_hints)
                val hint = remember(hints) { hints.random() }
                Text(hint)
            },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        AnswerTypeSelector(
            selected = question.type,
            onSelect = { newType ->
                val clearedFollowUps = existingFollowUps.isNotEmpty() || question.options.isNotEmpty()
                val isDestructive = clearedFollowUps &&
                    newType != question.type &&
                    !(question.type.isOptionType && newType.isOptionType)
                if (isDestructive) {
                    // Caller handles the confirmation dialog; just propagate type change
                }
                onQuestionChange(question.copy(type = newType, options = if (newType.isOptionType) question.options else emptyList()))
            }
        )

        if (question.type == QuestionType.SCALE) {
            ScaleRangeEditor(
                scaleMin = question.scaleMin,
                scaleMax = question.scaleMax,
                onScaleMinChange = { onQuestionChange(question.copy(scaleMin = it)) },
                onScaleMaxChange = { onQuestionChange(question.copy(scaleMax = it)) }
            )
        }

        if (question.type.isOptionType) {
            OptionListEditor(
                options = question.options,
                onOptionsChange = { onQuestionChange(question.copy(options = it)) }
            )
        }
    }
}

@Composable
private fun ScaleRangeEditor(
    scaleMin: Int,
    scaleMax: Int,
    onScaleMinChange: (Int) -> Unit,
    onScaleMaxChange: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = scaleMin.toString(),
            onValueChange = { raw ->
                raw.toIntOrNull()?.let { onScaleMinChange(it) }
            },
            label = { Text(stringResource(R.string.field_scale_min)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = scaleMax.toString(),
            onValueChange = { raw ->
                raw.toIntOrNull()?.let { onScaleMaxChange(it) }
            },
            label = { Text(stringResource(R.string.field_scale_max)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnswerTypeSelector(
    selected: QuestionType,
    onSelect: (QuestionType) -> Unit,
    includeText: Boolean = false
) {
    Column {
        Text(
            text = stringResource(R.string.field_answer_type),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            buildList {
                add(QuestionType.YES_NO to R.string.answer_type_yes_no)
                add(QuestionType.SCALE to R.string.answer_type_scale)
                add(QuestionType.NUMBER to R.string.answer_type_number)
                add(QuestionType.OPTION_SINGLE to R.string.answer_type_option_single)
                add(QuestionType.OPTION_MULTI to R.string.answer_type_option_multi)
                if (includeText) add(QuestionType.TEXT to R.string.answer_type_text)
            }.forEach { (type, labelRes) ->
                NudgeryToggleChip(
                    selected = selected == type,
                    onClick = { onSelect(type) },
                    label = { Text(stringResource(labelRes)) }
                )
            }
        }
    }
}

@Composable
private fun OptionListEditor(
    options: List<String>,
    onOptionsChange: (List<String>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.field_options),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        options.forEachIndexed { index, option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = option,
                    onValueChange = { updated ->
                        onOptionsChange(options.toMutableList().also { it[index] = updated })
                    },
                    placeholder = { Text(stringResource(R.string.option_hint)) },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    enabled = index > 0,
                    onClick = {
                        val reordered = options.toMutableList()
                        reordered.add(index - 1, reordered.removeAt(index))
                        onOptionsChange(reordered)
                    }
                ) {
                    Icon(Icons.Outlined.ArrowUpward, contentDescription = stringResource(R.string.option_move_up))
                }
                IconButton(
                    enabled = index < options.lastIndex,
                    onClick = {
                        val reordered = options.toMutableList()
                        reordered.add(index + 1, reordered.removeAt(index))
                        onOptionsChange(reordered)
                    }
                ) {
                    Icon(Icons.Outlined.ArrowDownward, contentDescription = stringResource(R.string.option_move_down))
                }
                IconButton(
                    onClick = { onOptionsChange(options.toMutableList().also { it.removeAt(index) }) }
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.option_remove))
                }
            }
        }
        if (options.size < 16) {
            TextButton(onClick = { onOptionsChange(options + "") }) {
                Text(stringResource(R.string.option_add))
            }
        }
    }
}

// ---- Step 2: Follow-ups ----

@Composable
fun FollowUpStep(
    mainQuestion: QuestionFormState,
    followUps: List<QuestionFormState>,
    onAdd: () -> Unit,
    onUpdate: (Int, QuestionFormState) -> Unit,
    onRemove: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.step_followups_title), style = MaterialTheme.typography.titleMedium)

        followUps.forEachIndexed { index, followUp ->
            HorizontalDivider()
            FollowUpEditor(
                index = index,
                mainQuestion = mainQuestion,
                followUp = followUp,
                onUpdate = { onUpdate(index, it) },
                onRemove = { onRemove(index) }
            )
        }

        TextButton(onClick = onAdd) {
            Text(stringResource(R.string.followup_add))
        }
    }
}

@Composable
private fun FollowUpEditor(
    index: Int,
    mainQuestion: QuestionFormState,
    followUp: QuestionFormState,
    onUpdate: (QuestionFormState) -> Unit,
    onRemove: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Follow-up ${index + 1}",
                style = MaterialTheme.typography.labelLarge
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.followup_remove))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.followup_trigger_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when (mainQuestion.type) {
                QuestionType.YES_NO -> YesNoTrigger(followUp, onUpdate)
                QuestionType.SCALE, QuestionType.NUMBER -> NumberTrigger(followUp, onUpdate)
                QuestionType.OPTION_SINGLE -> OptionTrigger(mainQuestion.options, followUp, onUpdate, containsOnSelect = false)
                QuestionType.OPTION_MULTI -> OptionTrigger(mainQuestion.options, followUp, onUpdate, containsOnSelect = true)
                else -> {}
            }
        }

        OutlinedTextField(
            value = followUp.text,
            onValueChange = { onUpdate(followUp.copy(text = it)) },
            label = { Text(stringResource(R.string.field_question_text)) },
            modifier = Modifier.fillMaxWidth()
        )

        AnswerTypeSelector(
            selected = followUp.type,
            onSelect = { newType ->
                onUpdate(followUp.copy(
                    type = newType,
                    options = if (newType.isOptionType) followUp.options else emptyList()
                ))
            },
            includeText = true
        )

        if (followUp.type == QuestionType.SCALE) {
            ScaleRangeEditor(
                scaleMin = followUp.scaleMin,
                scaleMax = followUp.scaleMax,
                onScaleMinChange = { onUpdate(followUp.copy(scaleMin = it)) },
                onScaleMaxChange = { onUpdate(followUp.copy(scaleMax = it)) }
            )
        }

        if (followUp.type.isOptionType) {
            OptionListEditor(
                options = followUp.options,
                onOptionsChange = { onUpdate(followUp.copy(options = it)) }
            )
        }
    }
}

@Composable
private fun YesNoTrigger(followUp: QuestionFormState, onUpdate: (QuestionFormState) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "YES" to R.string.answer_yes,
            "NO" to R.string.answer_no
        ).forEach { (value, labelRes) ->
            NudgeryToggleChip(
                selected = followUp.triggerAnswerValue == value,
                onClick = { onUpdate(followUp.copy(triggerAnswerValue = value, triggerOperator = TriggerOperator.EQ)) },
                label = { Text(stringResource(labelRes)) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NumberTrigger(followUp: QuestionFormState, onUpdate: (QuestionFormState) -> Unit) {
    val operators = listOf(
        TriggerOperator.EQ to R.string.trigger_op_eq,
        TriggerOperator.GT to R.string.trigger_op_gt,
        TriggerOperator.GTE to R.string.trigger_op_gte,
        TriggerOperator.LT to R.string.trigger_op_lt,
        TriggerOperator.LTE to R.string.trigger_op_lte
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            operators.forEach { (op, labelRes) ->
                NudgeryToggleChip(
                    selected = followUp.triggerOperator == op,
                    onClick = { onUpdate(followUp.copy(triggerOperator = op)) },
                    label = { Text(stringResource(labelRes)) }
                )
            }
        }
        OutlinedTextField(
            value = followUp.triggerAnswerValue ?: "",
            onValueChange = { raw ->
                val filtered = raw.filter { it.isDigit() || it == '.' || it == '-' }
                onUpdate(followUp.copy(triggerAnswerValue = filtered.takeIf { it.isNotBlank() }))
            },
            label = { Text(stringResource(R.string.trigger_number_value)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionTrigger(
    options: List<String>,
    followUp: QuestionFormState,
    onUpdate: (QuestionFormState) -> Unit,
    containsOnSelect: Boolean
) {
    if (options.isEmpty()) {
        Text(
            text = stringResource(R.string.trigger_no_options),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val selectedIndex = followUp.triggerAnswerValue?.toIntOrNull()
    val operator = if (containsOnSelect) TriggerOperator.CONTAINS else TriggerOperator.EQ
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { index, optionText ->
            NudgeryToggleChip(
                selected = selectedIndex == index,
                onClick = { onUpdate(followUp.copy(triggerAnswerValue = "$index", triggerOperator = operator)) },
                label = { Text(optionText) }
            )
        }
    }
}

// ---- Step 3: Schedule ----

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduleStep(
    schedule: ScheduleFormState,
    isEnabled: Boolean,
    onScheduleChange: (ScheduleFormState) -> Unit,
    onEnabledChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.step_schedule_title), style = MaterialTheme.typography.titleMedium)

        // Schedule type chips
        Column {
            Text(
                text = stringResource(R.string.field_answer_type),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ScheduleType.DAILY to R.string.schedule_type_daily,
                    ScheduleType.WEEKLY to R.string.schedule_type_weekly,
                    ScheduleType.MONTHLY to R.string.schedule_type_monthly,
                    ScheduleType.HOURLY to R.string.schedule_type_hourly,
                ).forEach { (type, labelRes) ->
                    NudgeryToggleChip(
                        selected = schedule.type == type,
                        onClick = { onScheduleChange(schedule.copy(type = type)) },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }
        }

        // Time of day
        var showTimePicker by remember { mutableStateOf(false) }
        val timePickerState = rememberTimePickerState(
            initialHour = schedule.timeOfDay.hour,
            initialMinute = schedule.timeOfDay.minute,
            is24Hour = false
        )

        if (showTimePicker) {
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        onScheduleChange(schedule.copy(
                            timeOfDay = LocalTime(timePickerState.hour, timePickerState.minute)
                        ))
                        showTimePicker = false
                    }) { Text(stringResource(R.string.action_confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
                text = { TimePicker(state = timePickerState) }
            )
        }

        val timeFieldInteraction = remember { MutableInteractionSource() }
        val timeFieldPressed by timeFieldInteraction.collectIsPressedAsState()
        LaunchedEffect(timeFieldPressed) {
            if (timeFieldPressed) showTimePicker = true
        }

        OutlinedTextField(
            value = schedule.timeOfDay.let {
                val h = if (it.hour % 12 == 0) 12 else it.hour % 12
                val period = if (it.hour < 12) "AM" else "PM"
                if (it.minute == 0) "$h $period" else "$h:${it.minute.toString().padStart(2, '0')} $period"
            },
            onValueChange = {},
            label = { Text(stringResource(R.string.schedule_time_of_day)) },
            trailingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
            readOnly = true,
            interactionSource = timeFieldInteraction,
            modifier = Modifier.fillMaxWidth()
        )

        // Active days (DAILY + HOURLY)
        if (schedule.type == ScheduleType.DAILY || schedule.type == ScheduleType.HOURLY) {
            Column {
                Text(
                    text = stringResource(R.string.schedule_active_days),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        NudgeryToggleChip(
                            selected = day in (schedule.activeDaysOfWeek ?: emptySet()),
                            onClick = {
                                val current = schedule.activeDaysOfWeek ?: emptySet()
                                val updated = if (day in current) current - day else current + day
                                onScheduleChange(schedule.copy(activeDaysOfWeek = updated))
                            },
                            label = { Text(day.toAbbreviation()) }
                        )
                    }
                }
            }
        }

        // Day of month (MONTHLY)
        if (schedule.type == ScheduleType.MONTHLY) {
            OutlinedTextField(
                value = schedule.dayOfMonth.toString(),
                onValueChange = { v -> v.toIntOrNull()?.let { d -> if (d in 1..31) onScheduleChange(schedule.copy(dayOfMonth = d)) } },
                label = { Text(stringResource(R.string.schedule_day_of_month)) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Enabled toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.nudge_enabled), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = isEnabled, onCheckedChange = onEnabledChange)
        }
    }
}
