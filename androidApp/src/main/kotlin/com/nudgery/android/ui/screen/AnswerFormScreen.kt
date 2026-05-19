package com.nudgery.android.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nudgery.android.R
import com.nudgery.android.viewmodel.AnswerFormUiState
import com.nudgery.android.viewmodel.AnswerFormViewModel
import com.nudgery.shared.model.QuestionType
import kotlinx.datetime.Instant
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val NUMBER_SLIDER_MIN = 0f
private const val NUMBER_SLIDER_MAX = 10f
private const val NUMBER_SLIDER_STEPS = 9

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerFormScreen(
    nudgeId: String,
    scheduledAt: Instant?,
    onDismiss: () -> Unit,
    viewModel: AnswerFormViewModel = koinViewModel(parameters = { parametersOf(nudgeId, scheduledAt) })
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isDismissed) {
        if (uiState.isDismissed) onDismiss()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (!uiState.isLoading && uiState.totalSteps > 1) {
                        Text(stringResource(R.string.answer_step_indicator, uiState.currentStepIndex + 1, uiState.totalSteps))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.dismiss() }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.nav_close)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                AnimatedContent(
                    targetState = uiState.currentStepIndex,
                    transitionSpec = {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    },
                    label = "answer_step"
                ) { stepIndex ->
                    AnswerStep(
                        uiState = uiState,
                        stepIndex = stepIndex,
                        onAnswerChange = { viewModel.setCurrentAnswer(it) },
                        onSave = { viewModel.saveAnswer() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnswerStep(
    uiState: AnswerFormUiState,
    stepIndex: Int,
    onAnswerChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val question = uiState.questions.getOrNull(stepIndex) ?: return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = question.question.text,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        when (question.question.type) {
            QuestionType.YES_NO -> YesNoInput(
                currentAnswer = uiState.currentAnswer,
                onAnswerChange = onAnswerChange
            )
            QuestionType.NUMBER -> NumberInput(
                currentAnswer = uiState.currentAnswer,
                onAnswerChange = onAnswerChange
            )
            QuestionType.OPTION_SINGLE -> OptionSingleInput(
                options = question.options,
                currentAnswer = uiState.currentAnswer,
                onAnswerChange = onAnswerChange
            )
            QuestionType.OPTION_MULTI -> OptionMultiInput(
                options = question.options,
                currentAnswer = uiState.currentAnswer,
                onAnswerChange = onAnswerChange
            )
            QuestionType.TEXT -> TextInput(
                currentAnswer = uiState.currentAnswer,
                onAnswerChange = onAnswerChange
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onSave,
            enabled = uiState.currentAnswer.isNotBlank() && !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.answer_save))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun YesNoInput(currentAnswer: String, onAnswerChange: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = { onAnswerChange("YES") },
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(R.string.answer_yes),
                style = MaterialTheme.typography.titleMedium,
                color = if (currentAnswer == "YES") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
        OutlinedButton(
            onClick = { onAnswerChange("NO") },
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(R.string.answer_no),
                style = MaterialTheme.typography.titleMedium,
                color = if (currentAnswer == "NO") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun NumberInput(currentAnswer: String, onAnswerChange: (String) -> Unit) {
    val value = currentAnswer.toFloatOrNull() ?: NUMBER_SLIDER_MIN

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toInt().toString(),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Slider(
            value = value,
            onValueChange = { onAnswerChange(it.toInt().toString()) },
            valueRange = NUMBER_SLIDER_MIN..NUMBER_SLIDER_MAX,
            steps = NUMBER_SLIDER_STEPS,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(NUMBER_SLIDER_MIN.toInt().toString(), style = MaterialTheme.typography.bodySmall)
            Text(NUMBER_SLIDER_MAX.toInt().toString(), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun OptionSingleInput(
    options: List<com.nudgery.shared.model.QuestionOption>,
    currentAnswer: String,
    onAnswerChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            OutlinedButton(
                onClick = { onAnswerChange(option.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = option.text,
                    color = if (currentAnswer == option.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun OptionMultiInput(
    options: List<com.nudgery.shared.model.QuestionOption>,
    currentAnswer: String,
    onAnswerChange: (String) -> Unit
) {
    val selectedIds = currentAnswer.split(",").filter { it.isNotBlank() }.toMutableSet()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = option.id in selectedIds,
                    onCheckedChange = { checked ->
                        if (checked) selectedIds.add(option.id) else selectedIds.remove(option.id)
                        onAnswerChange(selectedIds.joinToString(","))
                    }
                )
                Text(
                    text = option.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TextInput(currentAnswer: String, onAnswerChange: (String) -> Unit) {
    OutlinedTextField(
        value = currentAnswer,
        onValueChange = onAnswerChange,
        placeholder = { Text(stringResource(R.string.answer_text_hint)) },
        minLines = 3,
        modifier = Modifier.fillMaxWidth()
    )
}
