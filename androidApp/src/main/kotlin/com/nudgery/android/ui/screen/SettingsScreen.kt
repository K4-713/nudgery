package com.nudgery.android.ui.screen

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nudgery.android.R
import com.nudgery.android.settings.ThemePreference
import com.nudgery.android.ui.theme.ChartPalettePreference
import com.nudgery.android.ui.theme.paletteStops
import com.nudgery.android.viewmodel.ImportStatus
import com.nudgery.android.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAboutClick: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val importSuccessMessage = stringResource(R.string.settings_import_success)
    val importStatus = uiState.importStatus
    val importFailureMessage = if (importStatus is ImportStatus.Failure)
        stringResource(R.string.settings_import_failure, importStatus.message)
    else null

    LaunchedEffect(importStatus) {
        when (importStatus) {
            is ImportStatus.Success -> {
                snackbarHostState.showSnackbar(importSuccessMessage)
                viewModel.clearImportStatus()
            }
            is ImportStatus.Failure -> {
                if (importFailureMessage != null) {
                    snackbarHostState.showSnackbar(importFailureMessage)
                }
                viewModel.clearImportStatus()
            }
            else -> Unit
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            if (content != null) {
                viewModel.importNudgeFromBackup(content)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val systemIsDark = isSystemInDarkTheme()
        val isDark = when (uiState.themePreference) {
            ThemePreference.DARK -> true
            ThemePreference.LIGHT -> false
            ThemePreference.SYSTEM -> systemIsDark
        }

        Column(modifier = Modifier.padding(innerPadding)) {
            SettingsSectionLabel(stringResource(R.string.settings_theme))

            Column(modifier = Modifier.selectableGroup()) {
                ThemeOption(
                    label = stringResource(R.string.settings_theme_system),
                    selected = uiState.themePreference == ThemePreference.SYSTEM,
                    onSelect = { viewModel.setTheme(ThemePreference.SYSTEM) }
                )
                ThemeOption(
                    label = stringResource(R.string.settings_theme_light),
                    selected = uiState.themePreference == ThemePreference.LIGHT,
                    onSelect = { viewModel.setTheme(ThemePreference.LIGHT) }
                )
                ThemeOption(
                    label = stringResource(R.string.settings_theme_dark),
                    selected = uiState.themePreference == ThemePreference.DARK,
                    onSelect = { viewModel.setTheme(ThemePreference.DARK) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionLabel(stringResource(R.string.settings_charts))

            Column(modifier = Modifier.selectableGroup()) {
                PaletteOption(
                    label = stringResource(R.string.settings_palette_spectrum),
                    description = stringResource(R.string.settings_palette_spectrum_description),
                    palette = ChartPalettePreference.SPECTRUM,
                    selected = uiState.chartPalette == ChartPalettePreference.SPECTRUM,
                    isDark = isDark,
                    onSelect = { viewModel.setChartPalette(ChartPalettePreference.SPECTRUM) }
                )
                PaletteOption(
                    label = stringResource(R.string.settings_palette_horizon),
                    description = stringResource(R.string.settings_palette_horizon_description),
                    palette = ChartPalettePreference.HORIZON,
                    selected = uiState.chartPalette == ChartPalettePreference.HORIZON,
                    isDark = isDark,
                    onSelect = { viewModel.setChartPalette(ChartPalettePreference.HORIZON) }
                )
                PaletteOption(
                    label = stringResource(R.string.settings_palette_ember),
                    description = stringResource(R.string.settings_palette_ember_description),
                    palette = ChartPalettePreference.EMBER,
                    selected = uiState.chartPalette == ChartPalettePreference.EMBER,
                    isDark = isDark,
                    onSelect = { viewModel.setChartPalette(ChartPalettePreference.EMBER) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_bold_text),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.settings_bold_text_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.boldText,
                    onCheckedChange = { viewModel.setBoldText(it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ExactAlarmDiagnosticRow()

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionLabel(stringResource(R.string.settings_import))

            val isImporting = uiState.importStatus is ImportStatus.InProgress
            TextButton(
                onClick = { filePicker.launch("application/json") },
                enabled = !isImporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = if (isImporting)
                        stringResource(R.string.settings_import_in_progress)
                    else
                        stringResource(R.string.settings_import_button),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            TextButton(
                onClick = onAboutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_about),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ExactAlarmDiagnosticRow() {
    val context = LocalContext.current
    val granted = rememberExactAlarmGranted()

    SettingsSectionLabel(stringResource(R.string.settings_diagnostics))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_exact_alarm),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = if (granted)
                    stringResource(R.string.settings_exact_alarm_granted)
                else
                    stringResource(R.string.settings_exact_alarm_not_granted),
                style = MaterialTheme.typography.bodySmall,
                color = if (granted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }
        if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            TextButton(onClick = { openExactAlarmSettings(context) }) {
                Text(stringResource(R.string.settings_exact_alarm_open_settings))
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun PaletteOption(
    label: String,
    description: String,
    palette: ChartPalettePreference,
    selected: Boolean,
    isDark: Boolean,
    onSelect: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        val stops = palette.paletteStops.run { if (isDark) darkStops else lightStops }
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(12.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(Brush.horizontalGradient(stops))
        )
    }
}
