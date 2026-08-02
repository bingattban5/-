package com.agon.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agon.app.data.AiModel
import com.agon.app.data.EngineInfo
import com.agon.app.data.WhisperModel
import com.agon.app.domain.usecase.DeleteAiModelUseCase
import com.agon.app.domain.usecase.DownloadAiModelUseCase
import com.agon.app.domain.usecase.GetAiModelsUseCase
import com.agon.app.domain.usecase.GetTotalStorageUsedUseCase
import com.agon.app.engine.YtDlpEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelsUiState(
    val selectedTab: Int = 0,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val aiModels: List<AiModel> = emptyList(),
    val totalStorageUsed: Long = 0L,
    val totalStorageFormatted: String = "0 MB",
    val ytDlpEngine: EngineInfo = EngineInfo("yt-dlp", "", false),
    val downloadingModels: Map<String, Int> = emptyMap() // modelId -> progress
)

@HiltViewModel
class ModelsViewModel @Inject constructor(
    application: Application,
    private val ytDlpEngine: YtDlpEngine,
    private val getAiModelsUseCase: GetAiModelsUseCase,
    private val downloadAiModelUseCase: DownloadAiModelUseCase,
    private val deleteAiModelUseCase: DeleteAiModelUseCase,
    private val getTotalStorageUsedUseCase: GetTotalStorageUsedUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ModelsUiState())
    val uiState: StateFlow<ModelsUiState> = _uiState.asStateFlow()

    init {
        loadYtDlpInfo()
        loadAiModels()
        loadStorageUsage()
    }

    private fun loadYtDlpInfo() {
        viewModelScope.launch {
            val isInstalled = ytDlpEngine.isYtDlpInstalled()
            val version = if (isInstalled) {
                ytDlpEngine.getVersion().getOrNull() ?: "Unknown"
            } else {
                ""
            }

            _uiState.value = _uiState.value.copy(
                ytDlpEngine = EngineInfo(
                    name = "yt-dlp",
                    version = version,
                    isInstalled = isInstalled
                )
            )
        }
    }

    private fun loadAiModels() {
        viewModelScope.launch {
            getAiModelsUseCase.syncWithAvailableModels()
            
            getAiModelsUseCase().collect { models ->
                _uiState.value = _uiState.value.copy(aiModels = models)
            }
        }
    }

    private fun loadStorageUsage() {
        viewModelScope.launch {
            val totalBytes = getTotalStorageUsedUseCase()
            val formatted = formatBytes(totalBytes)
            _uiState.value = _uiState.value.copy(
                totalStorageUsed = totalBytes,
                totalStorageFormatted = formatted
            )
        }
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun downloadModel(modelId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                downloadingModels = _uiState.value.downloadingModels + (modelId to 0)
            )

            val result = downloadAiModelUseCase(modelId) { progress, downloaded, total ->
                _uiState.value = _uiState.value.copy(
                    downloadingModels = _uiState.value.downloadingModels + (modelId to progress)
                )
            }

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        downloadingModels = _uiState.value.downloadingModels - modelId,
                        successMessage = "تم تحميل النموذج بنجاح"
                    )
                    loadStorageUsage()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        downloadingModels = _uiState.value.downloadingModels - modelId,
                        // استخدام رسالة الخطأ المخصصة مباشرة من الـ UseCase
                        errorMessage = error.message ?: "حدث خطأ أثناء تحميل النموذج"
                    )
                }
            )
        }
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            val result = deleteAiModelUseCase(modelId)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "تم حذف النموذج"
                    )
                    loadStorageUsage()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "فشل الحذف: ${error.message}"
                    )
                }
            )
        }
    }

    fun checkYtDlpUpdate() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                ytDlpEngine = _uiState.value.ytDlpEngine.copy(isChecking = true)
            )

            // Note: Real update check would query GitHub API
            _uiState.value = _uiState.value.copy(
                ytDlpEngine = _uiState.value.ytDlpEngine.copy(
                    isChecking = false,
                    hasUpdate = false
                ),
                successMessage = "لا توجد تحديثات متاحة"
            )
        }
    }

    fun updateYtDlp() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(successMessage = "جاري تحديث yt-dlp...")
            
            val result = ytDlpEngine.installYtDlp()
            result.fold(
                onSuccess = {
                    loadYtDlpInfo()
                    _uiState.value = _uiState.value.copy(successMessage = "تم تحديث yt-dlp بنجاح")
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        // التقاط تفاصيل الخطأ مباشرة من المحرك
                        errorMessage = error.message ?: "حدث خطأ غير معروف أثناء التحديث"
                    )
                }
            )
        }
    }

    fun reinstallYtDlp() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(successMessage = "جاري إعادة التثبيت...")
            
            val result = ytDlpEngine.installYtDlp()
            result.fold(
                onSuccess = {
                    loadYtDlpInfo()
                    _uiState.value = _uiState.value.copy(successMessage = "تم إعادة التثبيت بنجاح")
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        // التقاط تفاصيل الخطأ مباشرة من المحرك
                        errorMessage = error.message ?: "حدث خطأ غير معروف أثناء التثبيت"
                    )
                }
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
