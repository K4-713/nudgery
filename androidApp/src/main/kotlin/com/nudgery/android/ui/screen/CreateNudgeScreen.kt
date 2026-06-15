// SPDX-License-Identifier: CC0-1.0

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
import com.nudgery.android.viewmodel.areFollowUpsValid
import com.nudgery.android.viewmodel.isQuestionSectionValid
import com.nudgery.shared.model.TriggerOperator
import com.nudgery.shared.usecase.CreateNudgeResult
import org.koin.androidx.compose.koinViewModel

/** Wizard steps: Question, Follow-ups, Schedule — always three steps for all question types. */
private enum class WizardStep { QUESTION, FOLLOW_UPS, SCHEDULE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNudgeScreen(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: CreateNudgeViewModel = koinViewModel()
) {
    val formState by viewModel.formState.collectAsState()
    var currentStep by remember { mutableIntStateOf(0) }

    val steps = listOf(WizardStep.QUESTION, WizardStep.FOLLOW_UPS, WizardStep.SCHEDULE)
    val totalSteps = steps.size
    val safeStep = currentStep.coerceIn(0, totalSteps - 1)

    LaunchedEffect(formState.result) {
        if (formState.result is CreateNudgeResult.Success) onSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.wizard_step_of, safeStep + 1, totalSteps))
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            LinearProgressIndicator(
                progress = { (safeStep + 1).toFloat() / totalSteps },
                modifier = Modifier.fillMaxWidth()
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                when (steps[safeStep]) {
                    WizardStep.QUESTION -> QuestionStep(
                        nudgeName = formState.nudgeName,
                        onNameChange = { viewModel.setNudgeName(it) },
                        question = formState.mainQuestion,
                        onQuestionChange = { viewModel.setMainQuestion(it) },
                        existingFollowUps = formState.followUpQuestions
                    )
                    WizardStep.FOLLOW_UPS -> FollowUpStep(
                        mainQuestion = formState.mainQuestion,
                        followUps = formState.followUpQuestions,
                        onAdd = { viewModel.addFollowUpQuestion(QuestionFormState(triggerOperator = TriggerOperator.ALWAYS)) },
                        onUpdate = { index, q -> viewModel.updateFollowUpQuestion(index, q) },
                        onRemove = { viewModel.removeFollowUpQuestion(it) },
                        showIntro = true
                    )
                    WizardStep.SCHEDULE -> ScheduleStep(
                        schedule = formState.schedule,
                        isEnabled = formState.isEnabled,
                        onScheduleChange = { viewModel.setSchedule(it) },
                        onEnabledChange = { viewModel.setEnabled(it) }
                    )
                }
            }

            // ED-22: the forward action is disabled while the current step's required fields are
            // blank; Back/Cancel stay available. An untouched follow-up stub doesn't block (ED-21
            // discards it on navigation).
            val canContinue = when (steps[safeStep]) {
                WizardStep.QUESTION -> isQuestionSectionValid(formState.nudgeName, formState.mainQuestion)
                WizardStep.FOLLOW_UPS -> areFollowUpsValid(formState.mainQuestion.type, formState.followUpQuestions)
                WizardStep.SCHEDULE -> true
            }

            WizardNavBar(
                currentStep = safeStep,
                totalSteps = totalSteps,
                isSubmitting = formState.isSubmitting,
                canContinue = canContinue,
                onCancel = onDismiss,
                // Prune on navigation so an untouched follow-up stub added on the follow-up step
                // doesn't linger when the user moves on without editing it (idempotent elsewhere).
                onBack = { viewModel.pruneUntouchedFollowUps(); currentStep = safeStep - 1 },
                onNext = { viewModel.pruneUntouchedFollowUps(); currentStep = safeStep + 1 },
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
    canContinue: Boolean,
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
            Button(onClick = onNext, enabled = canContinue) {
                Text(stringResource(R.string.wizard_next))
            }
        } else {
            Button(onClick = onSave, enabled = canContinue && !isSubmitting) {
                Text(stringResource(R.string.wizard_save))
            }
        }
    }
}
