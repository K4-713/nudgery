package com.nudgery.android.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nudgery.android.ui.theme.ChartPalettePreference
import com.nudgery.shared.emoji.Gender
import com.nudgery.shared.emoji.SkinTone
import com.nudgery.shared.model.Timeframe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemePreference { SYSTEM, LIGHT, DARK }

interface AppSettings {
    val themePreference: Flow<ThemePreference>
    val boldText: Flow<Boolean>
    val chartPalette: Flow<ChartPalettePreference>
    /** Default skin tone applied to picked emoji that support it (ED-6). */
    val defaultEmojiSkinTone: Flow<SkinTone>
    /** Default gender applied to picked neutral person emoji (ED-7). */
    val defaultEmojiGender: Flow<Gender>
    suspend fun setThemePreference(pref: ThemePreference)
    suspend fun setBoldText(bold: Boolean)
    suspend fun setChartPalette(palette: ChartPalettePreference)
    suspend fun setDefaultEmojiSkinTone(tone: SkinTone)
    suspend fun setDefaultEmojiGender(gender: Gender)
    /** Recently picked emoji, most-recent-first (ED-13 recents tab). */
    val emojiRecents: Flow<List<String>>
    suspend fun addEmojiRecent(emoji: String)
    /** Global emoji size multiplier applied to emoji surfaces (ED-14); 1.0 = default. */
    val emojiScale: Flow<Float>
    suspend fun setEmojiScale(scale: Float)
    fun getDefaultTimeframe(nudgeId: String): Flow<Timeframe>
    suspend fun setDefaultTimeframe(nudgeId: String, timeframe: Timeframe)
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nudgery_settings")

private val KEY_THEME = stringPreferencesKey("theme_preference")
private val KEY_BOLD_TEXT = booleanPreferencesKey("bold_text")
private val KEY_CHART_PALETTE = stringPreferencesKey("chart_palette")
private val KEY_EMOJI_SKIN_TONE = stringPreferencesKey("default_emoji_skin_tone")
private val KEY_EMOJI_GENDER = stringPreferencesKey("default_emoji_gender")
private val KEY_EMOJI_RECENTS = stringPreferencesKey("emoji_recents")
private const val MAX_EMOJI_RECENTS = 32
private val KEY_EMOJI_SCALE = floatPreferencesKey("emoji_scale")

/** Bounds for the global emoji-scale setting (ED-14): from normal size up to 2.5×. */
const val EMOJI_SCALE_MIN = 1f
const val EMOJI_SCALE_MAX = 2.5f

class DataStoreAppSettings(private val context: Context) : AppSettings {

    override val themePreference: Flow<ThemePreference> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_THEME]) {
            "LIGHT" -> ThemePreference.LIGHT
            "DARK" -> ThemePreference.DARK
            else -> ThemePreference.SYSTEM
        }
    }

    override val boldText: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BOLD_TEXT] ?: false
    }

    override val chartPalette: Flow<ChartPalettePreference> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_CHART_PALETTE]) {
            "HORIZON" -> ChartPalettePreference.HORIZON
            "EMBER" -> ChartPalettePreference.EMBER
            else -> ChartPalettePreference.SPECTRUM
        }
    }

    override suspend fun setThemePreference(pref: ThemePreference) {
        context.dataStore.edit { prefs -> prefs[KEY_THEME] = pref.name }
    }

    override suspend fun setBoldText(bold: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_BOLD_TEXT] = bold }
    }

    override suspend fun setChartPalette(palette: ChartPalettePreference) {
        context.dataStore.edit { prefs -> prefs[KEY_CHART_PALETTE] = palette.name }
    }

    override val defaultEmojiSkinTone: Flow<SkinTone> = context.dataStore.data.map { prefs ->
        prefs[KEY_EMOJI_SKIN_TONE]?.let { runCatching { SkinTone.valueOf(it) }.getOrNull() } ?: SkinTone.DEFAULT
    }

    override val defaultEmojiGender: Flow<Gender> = context.dataStore.data.map { prefs ->
        prefs[KEY_EMOJI_GENDER]?.let { runCatching { Gender.valueOf(it) }.getOrNull() } ?: Gender.NEUTRAL
    }

    override suspend fun setDefaultEmojiSkinTone(tone: SkinTone) {
        context.dataStore.edit { prefs -> prefs[KEY_EMOJI_SKIN_TONE] = tone.name }
    }

    override suspend fun setDefaultEmojiGender(gender: Gender) {
        context.dataStore.edit { prefs -> prefs[KEY_EMOJI_GENDER] = gender.name }
    }

    override val emojiRecents: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_EMOJI_RECENTS]?.split('\n')?.filter { it.isNotEmpty() } ?: emptyList()
    }

    override suspend fun addEmojiRecent(emoji: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_EMOJI_RECENTS]?.split('\n')?.filter { it.isNotEmpty() } ?: emptyList()
            val updated = (listOf(emoji) + current).distinct().take(MAX_EMOJI_RECENTS)
            prefs[KEY_EMOJI_RECENTS] = updated.joinToString("\n")
        }
    }

    override val emojiScale: Flow<Float> = context.dataStore.data.map { prefs ->
        (prefs[KEY_EMOJI_SCALE] ?: 1f).coerceIn(EMOJI_SCALE_MIN, EMOJI_SCALE_MAX)
    }

    override suspend fun setEmojiScale(scale: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_EMOJI_SCALE] = scale.coerceIn(EMOJI_SCALE_MIN, EMOJI_SCALE_MAX)
        }
    }

    override fun getDefaultTimeframe(nudgeId: String): Flow<Timeframe> =
        context.dataStore.data.map { prefs ->
            when (prefs[stringPreferencesKey("default_timeframe_$nudgeId")]) {
                "MONTHLY" -> Timeframe.MONTHLY
                "YEARLY" -> Timeframe.YEARLY
                "ALL_TIME" -> Timeframe.ALL_TIME
                else -> Timeframe.WEEKLY
            }
        }

    override suspend fun setDefaultTimeframe(nudgeId: String, timeframe: Timeframe) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("default_timeframe_$nudgeId")] = timeframe.name
        }
    }
}
