package com.nudgery.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nudgery.android.settings.AppSettings
import com.nudgery.android.settings.ThemePreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val boldText: Boolean = false
)

class SettingsViewModel(private val appSettings: AppSettings) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        appSettings.themePreference,
        appSettings.boldText
    ) { theme, bold -> SettingsUiState(theme, bold) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(pref: ThemePreference) {
        viewModelScope.launch { appSettings.setThemePreference(pref) }
    }

    fun setBoldText(bold: Boolean) {
        viewModelScope.launch { appSettings.setBoldText(bold) }
    }
}
