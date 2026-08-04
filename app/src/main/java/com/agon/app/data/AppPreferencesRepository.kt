package com.agon.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// تعريف الـ DataStore على مستوى التطبيق
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class AppPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // مفاتيح الإعدادات - يجب أن تكون مطابقة تماماً لما يستخدم في SettingsViewModel
        val SUBTITLE_FORMAT_KEY = stringPreferencesKey("subtitle_format")
        val DEFAULT_QUALITY_KEY = stringPreferencesKey("default_quality")
        val AUTO_TRANSLATE_KEY = stringPreferencesKey("auto_translate")
    }

    /**
     * Flow لصيغة الترجمة المفضلة
     * يُرجع "srt" كقيمة افتراضية إذا لم يختر المستخدم شيئاً
     */
    val subtitleFormatFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SUBTITLE_FORMAT_KEY] ?: "srt"
    }

    /**
     * Flow للجودة الافتراضية
     */
    val defaultQualityFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_QUALITY_KEY] ?: "best"
    }

    /**
     * تحديث صيغة الترجمة
     */
    suspend fun setSubtitleFormat(format: String) {
        context.dataStore.edit { preferences ->
            preferences[SUBTITLE_FORMAT_KEY] = format
        }
    }

    /**
     * تحديث الجودة الافتراضية
     */
    suspend fun setDefaultQuality(quality: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_QUALITY_KEY] = quality
        }
    }
}