package com.nudgery.android.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nudgery.android.R
import com.nudgery.android.viewmodel.EditNudgeViewModel
import com.nudgery.android.viewmodel.QuestionFormState
import com.nudgery.android.viewmodel.ScheduleFormState
import com.nudgery.shared.usecase.UpdateNudgeResult
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val EDIT_WIZARD_STEPS = 3

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
    var currentStep by remember { mutableIntStateOf(initialStep) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.wizard_step_of, currentStep + 1, EDIT_WIZARD_STEPS))
                }
            )
        }
    ) { innerPadding ->
        if (formState.isLoading) {
            Column(modifier = Modifier.padding(innerPadding).padding(24.dp)) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(modifier = Modifier.padding(innerPadding)) {
            LinearProgressIndicator(
                progress = { (currentStep + 1).toFloat() / EDIT_WIZARD_STEPS },
                modifier = Modifier.fillMaxWidth()
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                when (currentStep) {
                    0 -> EditQuestionStep(
                        name = formState.name,
                        onNameChange = { viewModel.setName(it) },
                        questionText = formState.mainQuestionText,
                        onQuestionTextChange = { viewModel.setMainQuestionText(it) },
                        options = formState.options.map { it.text },
                        onOptionChange = { index, text ->
                            val optionId = formState.options.getOrNull(index)?.optionId ?: return@EditQuestionStep
                            viewModel.updateOption(optionId, text)
                        }
                    )
                    1 -> FollowUpStep(
                        mainQuestion = QuestionFormState(
                            text = formState.mainQuestionText,
                            type = formState.mainQuestionType,
                            options = formState.options.map { it.text }
                        ),
                        followUps = formState.followUps.map { it.formState },
                        onAdd = { viewModel.addFollowUp() },
                        onUpdate = { index, state -> viewModel.updateFollowUp(index, state) },
                        onRemove = { index -> viewModel.removeFollowUp(index) }
                    )
                    2 -> ScheduleStep(
                        schedule = formState.schedule,
                        isEnabled = formState.isEnabled,
                        onScheduleChange = { viewModel.setSchedule(it) },
                        onEnabledChange = { viewModel.setEnabled(it) }
                    )
                }
            }

            WizardNavBar(
                currentStep = currentStep,
                totalSteps = EDIT_WIZARD_STEPS,
                isSubmitting = formState.isSubmitting,
                onCancel = onDismiss,
                onBack = { currentStep-- },
                onNext = { currentStep++ },
                onSave = { viewModel.submit() }
            )
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
    onOptionChange: (Int, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.step_question_title), style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.field_nudge_name)) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = questionText,
            onValueChange = onQuestionTextChange,
            label = { Text(stringResource(R.string.field_question_text)) },
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
                OutlinedTextField(
                    value = option,
                    onValueChange = { onOptionChange(index, it) },
                    placeholder = { Text(stringResource(R.string.option_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
