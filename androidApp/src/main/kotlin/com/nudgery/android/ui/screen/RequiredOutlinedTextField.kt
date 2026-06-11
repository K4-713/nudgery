// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.screen

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * An [OutlinedTextField] for a required free-text field (ED-22).
 *
 * The inline error appears only once the field has actually held non-whitespace content and is then
 * blank — a never-filled field never scolds the user, so the disabled Next/Save button is the only
 * signal until they have typed something real (and then cleared it). The `hasHadContent` latch flips
 * on any non-blank value, whether typed by the user or loaded into the field (e.g. an existing nudge
 * name on the edit screen, or the default "Nudge #N"), so blanking out pre-filled text shows the
 * error immediately while a fresh empty field stays quiet.
 *
 * This component does not itself gate submission; callers disable the forward action via the
 * `FormValidation` helpers. It only governs when the *visual* error is shown.
 */
@Composable
fun RequiredOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    errorText: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: @Composable (() -> Unit)? = null,
    minLines: Int = 1
) {
    var hasHadContent by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(value) { if (value.trim().isNotEmpty()) hasHadContent = true }
    val isError = hasHadContent && value.trim().isEmpty()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { { Text(it) } },
        placeholder = placeholder,
        isError = isError,
        supportingText = if (isError) {
            { Text(errorText) }
        } else {
            null
        },
        minLines = minLines,
        modifier = modifier
    )
}
