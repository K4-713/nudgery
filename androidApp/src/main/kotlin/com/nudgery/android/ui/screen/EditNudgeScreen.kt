// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nudgery.android.R
import com.nudgery.android.ui.theme.GhostText
import com.nudgery.android.viewmodel.EditNudgeViewModel
import com.nudgery.android.viewmodel.QuestionFormState
import com.nudgery.android.viewmodel.ScheduleFormState
import com.nudgery.android.viewmodel.areFollowUpsValid
import com.nudgery.android.viewmodel.isQuestionSectionValid
import com.nudgery.shared.model.QuestionType
import com.nudgery.shared.usecase.UpdateNudgeResult
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNudgeScreen(
    nudgeId: String,
    initialStep: Int = 0,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: EditNudgeViewModel = koinViewModel(parameters = { parametersOf(nudgeId) })
) {
    val formState by viewModel.formState.collectAsState()

    LaunchedEffect(formState.result) {
        if (formState.result is UpdateNudgeResult.Success) onSuccess()
    }

    if (formState.showSplitDialog) {
        SplitDialog(
            onSplit = { viewModel.submitWithSplit() },
            onInPlace = { viewModel.submitInPlace() },
            onCancel = { viewModel.dismissSplitDialog() }
        )
    }

    val titleRes = when (initialStep) {
        0 -> R.string.edit_question_title
        1 -> R.string.edit_followups_title
        else -> R.string.edit_schedule_title
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(titleRes)) })
        }
    ) { innerPadding ->
        if (formState.isLoading) {
            Column(modifier = Modifier.padding(innerPadding).padding(24.dp)) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                when (initialStep) {
                    0 -> EditQuestionStep(
                        name = formState.name,
                        onNameChange = { viewModel.setName(it) },
                        questionText = formState.mainQuestionText,
                        onQuestionTextChange = { viewModel.setMainQuestionText(it) },
                        options = formState.options.map { it.text },
                        onOptionChange = { index, text -> viewModel.updateOptionAt(index, text) },
                        onOptionReorder = { from, to -> viewModel.reorderOption(from, to) },
                        onOptionRemove = { index -> viewModel.removeOption(index) },
                        onOptionAdd = { viewModel.addOption() },
                        canAddOption = formState.options.size < 16,
                        isYesNo = formState.mainQuestionType == QuestionType.YES_NO,
                        collapsePerDay = formState.mainQuestionCollapsePerDay,
                        onCollapsePerDayChange = { viewModel.setMainQuestionCollapsePerDay(it) }
                    )
                    1 -> FollowUpStep(
                        mainQuestion = QuestionFormState(
                            text = formState.mainQuestionText,
                            type = formState.mainQuestionType,
                            options = formState.options.map { it.text },
                            scaleMin = formState.mainQuestionScaleMin,
                            scaleMax = formState.mainQuestionScaleMax
                        ),
                        followUps = formState.followUps.map { it.formState },
                        onAdd = { viewModel.addFollowUp() },
                        onUpdate = { index, state -> viewModel.updateFollowUp(index, state) },
                        onRemove = { index -> viewModel.removeFollowUp(index) }
                    )
                    else -> ScheduleStep(
                        schedule = formState.schedule,
                        isEnabled = formState.isEnabled,
                        onScheduleChange = { viewModel.setSchedule(it) },
                        onEnabledChange = { viewModel.setEnabled(it) }
                    )
                }
            }

            // ED-22: Save is disabled while the visible section's required fields are blank; Cancel
            // stays available. Schedule has no required text, so it never blocks.
            val canSave = when (initialStep) {
                0 -> isQuestionSectionValid(formState.name, formState.mainQuestionText)
                1 -> areFollowUpsValid(formState.followUps.map { it.formState })
                else -> true
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = { viewModel.submit() },
                    enabled = canSave && !formState.isSubmitting,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@Composable
private fun EditQuestionStep(
    name: String,
    onNameChange: (String) -> Unit,
    questionText: String,
    onQuestionTextChange: (String) -> Unit,
    options: List<String>,
    onOptionChange: (Int, String) -> Unit,
    onOptionReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onOptionRemove: (Int) -> Unit,
    onOptionAdd: () -> Unit,
    canAddOption: Boolean,
    isYesNo: Boolean,
    collapsePerDay: Boolean,
    onCollapsePerDayChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.step_question_title), style = MaterialTheme.typography.titleMedium)

        RequiredOutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = stringResource(R.string.field_nudge_name),
            errorText = stringResource(R.string.error_nudge_name_required),
            modifier = Modifier.fillMaxWidth()
        )

        RequiredOutlinedTextField(
            value = questionText,
            onValueChange = onQuestionTextChange,
            label = stringResource(R.string.field_question_text),
            errorText = stringResource(R.string.error_question_text_required),
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        if (options.isNotEmpty()) {
            Text(
                text = stringResource(R.string.field_options),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            options.forEachIndexed { index, option ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = option,
                        onValueChange = { onOptionChange(index, it) },
                        placeholder = { GhostText(stringResource(R.string.option_hint)) },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        enabled = index > 0,
                        onClick = { onOptionReorder(index, index - 1) }
                    ) {
                        Icon(Icons.Outlined.ArrowUpward, contentDescription = stringResource(R.string.option_move_up))
                    }
                    IconButton(
                        enabled = index < options.lastIndex,
                        onClick = { onOptionReorder(index, index + 1) }
                    ) {
                        Icon(Icons.Outlined.ArrowDownward, contentDescription = stringResource(R.string.option_move_down))
                    }
                    IconButton(onClick = { onOptionRemove(index) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.option_remove))
                    }
                }
            }
        }

        if (options.isNotEmpty() && canAddOption) {
            TextButton(onClick = onOptionAdd) {
                Text(stringResource(R.string.option_add))
            }
        }

        if (isYesNo) {
            OneYesPerDayToggle(checked = collapsePerDay, onCheckedChange = onCollapsePerDayChange)
        }
    }
}

@Composable
private fun SplitDialog(
    onSplit: () -> Unit,
    onInPlace: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.split_dialog_title)) },
        text = { Text(stringResource(R.string.split_dialog_body)) },
        confirmButton = {
            Button(onClick = onSplit) {
                Text(stringResource(R.string.split_dialog_split))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.split_dialog_cancel))
                }
                TextButton(onClick = onInPlace) {
                    Text(stringResource(R.string.split_dialog_in_place))
                }
            }
        }
    )
}
