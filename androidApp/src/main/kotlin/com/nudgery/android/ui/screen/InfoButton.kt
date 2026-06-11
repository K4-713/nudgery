// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nudgery.android.R

/**
 * A small "ⓘ" affordance that opens a modal help dialog explaining a nearby control.
 *
 * Reusable contextual help: drop one next to any label whose explanation would otherwise clutter
 * the form, and the detail moves off-screen until asked for. Tapping opens an [AlertDialog] showing
 * [title] and [body] — deliberately the app's standard dialog pattern rather than a tooltip, for
 * predictable behavior and robust accessibility on every screen size. Manages its own open state
 * (preserved across configuration changes).
 *
 * @param title heading shown in the help dialog (typically the control's own label).
 * @param body the explanatory text.
 * @param contentDescription accessibility label for the icon itself; defaults to "More about
 *   [title]".
 */
@Composable
fun InfoButton(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(R.string.info_button_desc, title)
) {
    var showHelp by rememberSaveable { mutableStateOf(false) }

    IconButton(onClick = { showHelp = true }, modifier = modifier) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text(title) },
            text = { Text(body) },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text(stringResource(R.string.action_dismiss))
                }
            }
        )
    }
}
