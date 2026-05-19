package com.nudgery.android.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemePreference { SYSTEM, LIGHT, DARK }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nudgery_settings")

private val KEY_THEME = stringPreferencesKey("theme_preference")
private val KEY_BOLD_TEXT = booleanPreferencesKey("bold_text")

class AppSettings(private val context: Context) {

    val themePreference: Flow<ThemePreference> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_THEME]) {
            "LIGHT" -> ThemePreference.LIGHT
            "DARK" -> ThemePreference.DARK
            else -> ThemePreference.SYSTEM
        }
    }

    val boldText: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BOLD_TEXT] ?: false
    }

    suspend fun setThemePreference(pref: ThemePreference) {
        context.dataStore.edit { prefs -> prefs[KEY_THEME] = pref.name }
    }

    suspend fun setBoldText(bold: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_BOLD_TEXT] = bold }
    }
}
