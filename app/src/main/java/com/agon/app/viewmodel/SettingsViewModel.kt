package com.agon.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agon.app.data.AppPreferences
import com.agon.app.engine.YtDlpEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val showClearCacheDialog: Boolean = false,
    val cacheSize: String = "45.2 MB",
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val savedCookieFiles: List<String> = emptyList(), // قائمة بأسماء النطاقات المحفوظة (مثل pornhub.com)
    val isCookieLoading: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val preferences: AppPreferences,
    private val ytDlpEngine: YtDlpEngine // تم الحقن للوصول لمحرك yt-dlp
) : AndroidViewModel(application) {

    val isDarkTheme = preferences.isDarkThemeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val savePath = preferences.savePathFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "Downloads/SubVIDD"
    )

    val autoTranslate = preferences.autoTranslateFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    val defaultQuality = preferences.defaultQualityFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "best"
    )

    val cacheEnabled = preferences.cacheEnabledFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    val subtitleFormat = preferences.subtitleFormatFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "srt"
    )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSavedCookies()
    }

    private fun loadSavedCookies() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCookieLoading = true)
            val result = ytDlpEngine.getSavedCookieFiles()
            // استخراج أسماء النطاقات فقط من القائمة (مثال: "pornhub.com" من "pornhub.com.txt")
            val domains = result.map { it.first }
            _uiState.value = _uiState.value.copy(
                savedCookieFiles = domains,
                isCookieLoading = false
            )
        }
    }

    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            preferences.setDarkTheme(isDark)
        }
    }

    fun setSavePath(path: String) {
        viewModelScope.launch {
            preferences.setSavePath(path)
        }
    }

    fun setAutoTranslate(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setAutoTranslate(enabled)
        }
    }

    fun setDefaultQuality(quality: String) {
        viewModelScope.launch {
            preferences.setDefaultQuality(quality)
        }
    }

    fun setCacheEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setCacheEnabled(enabled)
        }
    }

    fun setSubtitleFormat(format: String) {
        viewModelScope.launch {
            preferences.setSubtitleFormat(format)
        }
    }

    // ==========================================
    // دوال إدارة ملفات الـ Cookies
    // ==========================================

    fun saveCookieFile(domainName: String, fileContent: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCookieLoading = true)
            val result = ytDlpEngine.saveCookieFile(domainName, fileContent)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "تم حفظ ملف الكوكيز لـ $domainName بنجاح",
                        isCookieLoading = false
                    )
                    loadSavedCookies() // تحديث القائمة
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "فشل الحفظ: ${error.message}",
                        isCookieLoading = false
                    )
                }
            )
        }
    }

    fun deleteCookieFile(domainName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCookieLoading = true)
            val result = ytDlpEngine.deleteCookieFile(domainName)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "تم حذف ملف الكوكيز لـ $domainName",
                        isCookieLoading = false
                    )
                    loadSavedCookies() // تحديث القائمة
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "فشل الحذف: ${error.message}",
                        isCookieLoading = false
                    )
                }
            )
        }
    }
    // ==========================================

    fun showClearCacheDialog() {
        _uiState.value = _uiState.value.copy(showClearCacheDialog = true)
    }

    fun dismissClearCacheDialog() {
        _uiState.value = _uiState.value.copy(showClearCacheDialog = false)
    }

    fun clearCache() {
        _uiState.value = _uiState.value.copy(
            showClearCacheDialog = false,
            cacheSize = "0 MB",
            successMessage = "تم مسح ذاكرة التخزين المؤقت"
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }
}
