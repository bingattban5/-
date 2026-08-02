package com.agon.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppPreferences(private val context: Context) {

    private val isDarkThemeKey = booleanPreferencesKey("is_dark_theme")
    private val savePathKey = stringPreferencesKey("save_path")
    private val autoTranslateKey = booleanPreferencesKey("auto_translate")
    private val defaultQualityKey = stringPreferencesKey("default_quality")
    private val cacheEnabledKey = booleanPreferencesKey("cache_enabled")
    private val subtitleFormatKey = stringPreferencesKey("subtitle_format")

    val isDarkThemeFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[isDarkThemeKey] ?: false
    }

    val savePathFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[savePathKey] ?: "Downloads/SubVIDD"
    }

    val autoTranslateFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[autoTranslateKey] ?: true
    }

    val defaultQualityFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[defaultQualityKey] ?: "best"
    }

    val cacheEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[cacheEnabledKey] ?: true
    }

    val subtitleFormatFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[subtitleFormatKey] ?: "srt"
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[isDarkThemeKey] = isDark
        }
    }

    suspend fun setSavePath(path: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[savePathKey] = path
        }
    }

    suspend fun setAutoTranslate(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[autoTranslateKey] = enabled
        }
    }

    suspend fun setDefaultQuality(quality: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[defaultQualityKey] = quality
        }
    }

    suspend fun setCacheEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[cacheEnabledKey] = enabled
        }
    }

    suspend fun setSubtitleFormat(format: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[subtitleFormatKey] = format
        }
    }
}
