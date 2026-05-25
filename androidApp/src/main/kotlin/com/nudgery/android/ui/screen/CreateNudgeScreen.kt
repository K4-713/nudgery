package com.nudgery.android.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nudgery.android.R
import com.nudgery.android.viewmodel.CreateNudgeViewModel
import com.nudgery.android.viewmodel.QuestionFormState
import com.nudgery.android.viewmodel.ScheduleFormState
import com.nudgery.shared.usecase.CreateNudgeResult
import org.koin.androidx.compose.koinViewModel

private const val WIZARD_TOTAL_STEPS = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNudgeScreen(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: CreateNudgeViewModel = koinViewModel()
) {
    val formState by viewModel.formState.collectAsState()
    var currentStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(formState.result) {
        if (formState.result is CreateNudgeResult.Success) onSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.wizard_step_of, currentStep + 1, WIZARD_TOTAL_STEPS))
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            LinearProgressIndicator(
                progress = { (currentStep + 1).toFloat() / WIZARD_TOTAL_STEPS },
                modifier = Modifier.fillMaxWidth()
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                when (currentStep) {
                    0 -> QuestionStep(
                        nudgeName = formState.nudgeName,
                        onNameChange = { viewModel.setNudgeName(it) },
                        question = formState.mainQuestion,
                        onQuestionChange = { viewModel.setMainQuestion(it) },
                        existingFollowUps = formState.followUpQuestions
                    )
                    1 -> FollowUpStep(
                        mainQuestion = formState.mainQuestion,
                        followUps = formState.followUpQuestions,
                        onAdd = { viewModel.addFollowUpQuestion(QuestionFormState()) },
                        onUpdate = { index, q -> viewModel.updateFollowUpQuestion(index, q) },
                        onRemove = { viewModel.removeFollowUpQuestion(it) }
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
                totalSteps = WIZARD_TOTAL_STEPS,
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
internal fun WizardNavBar(
    currentStep: Int,
    totalSteps: Int,
    isSubmitting: Boolean,
    onCancel: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (currentStep > 0) {
            OutlinedButton(onClick = onBack) {
                Text(stringResource(R.string.wizard_back))
            }
        } else {
            OutlinedButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }
        }

        if (currentStep < totalSteps - 1) {
            Button(onClick = onNext) {
                Text(stringResource(R.string.wizard_next))
            }
        } else {
            Button(onClick = onSave, enabled = !isSubmitting) {
                Text(stringResource(R.string.wizard_save))
            }
        }
    }
}
