package com.nudgery.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nudgery.android.backup.NudgeBackupParser
import com.nudgery.android.settings.AppSettings
import com.nudgery.android.settings.ThemePreference
import com.nudgery.android.ui.theme.ChartPalettePreference
import com.nudgery.shared.usecase.ImportNudgeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "SettingsViewModel"

sealed class ImportStatus {
    data object Idle : ImportStatus()
    data object InProgress : ImportStatus()
    data class Success(val nudgeId: String) : ImportStatus()
    data class Failure(val message: String) : ImportStatus()
}

data class SettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val boldText: Boolean = false,
    val chartPalette: ChartPalettePreference = ChartPalettePreference.SPECTRUM,
    val importStatus: ImportStatus = ImportStatus.Idle
)

class SettingsViewModel(
    private val appSettings: AppSettings,
    private val importNudge: ImportNudgeUseCase,
    private val backupParser: NudgeBackupParser
) : ViewModel() {

    private val _importStatus = MutableStateFlow<ImportStatus>(ImportStatus.Idle)

    val uiState: StateFlow<SettingsUiState> = combine(
        appSettings.themePreference,
        appSettings.boldText,
        appSettings.chartPalette,
        _importStatus
    ) { theme, bold, palette, importStatus ->
        SettingsUiState(theme, bold, palette, importStatus)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(pref: ThemePreference) {
        viewModelScope.launch { appSettings.setThemePreference(pref) }
    }

    fun setBoldText(bold: Boolean) {
        viewModelScope.launch { appSettings.setBoldText(bold) }
    }

    fun setChartPalette(palette: ChartPalettePreference) {
        viewModelScope.launch { appSettings.setChartPalette(palette) }
    }

    fun importNudgeFromBackup(jsonContent: String) {
        _importStatus.update { ImportStatus.InProgress }
        viewModelScope.launch {
            when (val parsed = backupParser.parse(jsonContent)) {
                is NudgeBackupParser.ParseResult.Failure -> {
                    Log.w(TAG, "Failed to parse nudge backup: ${parsed.message}")
                    _importStatus.update { ImportStatus.Failure(parsed.message) }
                }
                is NudgeBackupParser.ParseResult.Success -> {
                    val nudgeId = importNudge.execute(parsed.request)
                    Log.i(TAG, "Imported nudge from backup: $nudgeId")
                    _importStatus.update { ImportStatus.Success(nudgeId) }
                }
            }
        }
    }

    fun clearImportStatus() {
        _importStatus.update { ImportStatus.Idle }
    }
}
