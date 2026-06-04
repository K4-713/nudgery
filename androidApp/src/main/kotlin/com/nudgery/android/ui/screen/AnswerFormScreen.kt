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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.draw.alpha
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
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.sp
import com.nudgery.android.ui.theme.LocalEmojiScale
import com.nudgery.android.ui.theme.emojiScaledStyle
import com.nudgery.android.viewmodel.AnswerFormViewModel
import com.nudgery.shared.emoji.Gender
import com.nudgery.shared.emoji.SkinTone
import com.nudgery.android.viewmodel.ScheduledAt
import com.nudgery.shared.model.QuestionType
import kotlinx.datetime.Instant
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val UNSELECTED_ALPHA = 0.4f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerFormScreen(
    nudgeId: String,
    scheduledAt: Instant?,
    onDismiss: () -> Unit,
    viewModel: AnswerFormViewModel = koinViewModel(parameters = { parametersOf(nudgeId, ScheduledAt(scheduledAt)) })
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
                        onEmojiAppend = { viewModel.appendEmoji(it) },
                        onEmojiBackspace = { viewModel.backspaceEmoji() },
                        onSave = { viewModel.saveAnswer() },
                        onCancel = { viewModel.dismiss() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .imePadding()
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
    onEmojiAppend: (String) -> Unit,
    onEmojiBackspace: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
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
            style = emojiScaledStyle(question.question.text, MaterialTheme.typography.headlineSmall),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        when (question.question.type) {
            QuestionType.YES_NO -> YesNoInput(
                currentAnswer = uiState.currentAnswer,
                onAnswerChange = onAnswerChange
            )
            QuestionType.SCALE -> ScaleInput(
                currentAnswer = uiState.currentAnswer,
                scaleMin = question.question.scaleMin ?: 0,
                scaleMax = question.question.scaleMax ?: 10,
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
            QuestionType.EMOJI -> EmojiInput(
                currentAnswer = uiState.currentAnswer,
                skinTone = uiState.emojiSkinTone,
                gender = uiState.emojiGender,
                recents = uiState.emojiRecents,
                onAppend = onEmojiAppend,
                onBackspace = onEmojiBackspace,
                modifier = Modifier.weight(1f)
            )
        }

        // The emoji picker fills the remaining space itself; other (small) answer inputs need this
        // weighted spacer to push the buttons to the bottom. Two weighted children would split the
        // space and starve the picker, so it is omitted for EMOJI.
        if (question.question.type != QuestionType.EMOJI) {
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
            Button(
                onClick = onSave,
                enabled = uiState.currentAnswer.isNotBlank() && !uiState.isSubmitting,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.answer_save),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SelectableOptionButton(
    text: String,
    selected: Boolean,
    anySelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.alpha(if (anySelected) UNSELECTED_ALPHA else 1f)
        ) {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun YesNoInput(currentAnswer: String, onAnswerChange: (String) -> Unit) {
    val anySelected = currentAnswer.isNotBlank()
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        SelectableOptionButton(
            text = stringResource(R.string.answer_yes),
            selected = currentAnswer == "YES",
            anySelected = anySelected,
            onClick = { onAnswerChange("YES") },
            modifier = Modifier.weight(1f)
        )
        SelectableOptionButton(
            text = stringResource(R.string.answer_no),
            selected = currentAnswer == "NO",
            anySelected = anySelected,
            onClick = { onAnswerChange("NO") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ScaleInput(
    currentAnswer: String,
    scaleMin: Int,
    scaleMax: Int,
    onAnswerChange: (String) -> Unit
) {
    val steps = (scaleMax - scaleMin - 1).coerceAtLeast(0)
    val value = currentAnswer.toFloatOrNull()?.coerceIn(scaleMin.toFloat(), scaleMax.toFloat())
        ?: scaleMin.toFloat()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toInt().toString(),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Slider(
            value = value,
            onValueChange = { onAnswerChange(it.toInt().toString()) },
            valueRange = scaleMin.toFloat()..scaleMax.toFloat(),
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(scaleMin.toString(), style = MaterialTheme.typography.bodySmall)
            Text(scaleMax.toString(), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun NumberInput(currentAnswer: String, onAnswerChange: (String) -> Unit) {
    OutlinedTextField(
        value = currentAnswer,
        onValueChange = { raw ->
            val filtered = raw.filter { it.isDigit() || it == '.' || it == '-' }
            onAnswerChange(filtered)
        },
        label = { Text(stringResource(R.string.answer_type_number)) },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun OptionSingleInput(
    options: List<com.nudgery.shared.model.QuestionOption>,
    currentAnswer: String,
    onAnswerChange: (String) -> Unit
) {
    val anySelected = currentAnswer.isNotBlank()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            SelectableOptionButton(
                text = option.text,
                selected = currentAnswer == option.id,
                anySelected = anySelected,
                onClick = { onAnswerChange(option.id) },
                modifier = Modifier.fillMaxWidth()
            )
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
    val anySelected = selectedIds.isNotEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            val isSelected = option.id in selectedIds
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (anySelected && !isSelected) UNSELECTED_ALPHA else 1f)
            ) {
                Checkbox(
                    checked = isSelected,
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

/**
 * Emoji answer input (ED-13): a read-only chosen-emoji display with a backspace, above the inline
 * always-open emoji picker. The answer is one or more emoji; the system keyboard is used only by the
 * picker's search field.
 */
@Composable
private fun EmojiInput(
    currentAnswer: String,
    skinTone: SkinTone,
    gender: Gender,
    recents: List<String>,
    onAppend: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Text(
                text = currentAnswer.ifEmpty { stringResource(R.string.emoji_answer_hint) },
                // The chosen emoji honor the global emoji scale (ED-14); the empty hint stays normal.
                fontSize = if (currentAnswer.isEmpty()) 16.sp else (28 * LocalEmojiScale.current).sp,
                modifier = Modifier.weight(1f)
            )
            if (currentAnswer.isNotEmpty()) {
                TextButton(onClick = onBackspace) { Text("⌫", fontSize = 22.sp) }
            }
        }
        EmojiPicker(
            recents = recents,
            defaultSkinTone = skinTone,
            defaultGender = gender,
            onPick = onAppend,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}
