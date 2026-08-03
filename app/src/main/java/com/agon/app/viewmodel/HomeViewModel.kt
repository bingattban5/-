package com.agon.app.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.agon.app.data.DownloadItem
import com.agon.app.data.DownloadMode
import com.agon.app.data.DownloadRepository
import com.agon.app.data.DownloadStatus
import com.agon.app.data.SubtitleMethod
import com.agon.app.data.VideoInfo
import com.agon.app.data.VideoQuality
import com.agon.app.domain.usecase.AddDownloadUseCase
import com.agon.app.domain.usecase.AnalyzeUrlUseCase
import com.agon.app.domain.usecase.DetermineSubtitleMethodUseCase
import com.agon.app.engine.YtDlpEngine
import com.agon.app.worker.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class HomeUiState(
    val url: String = "",
    val isAnalyzing: Boolean = false,
    val videoInfo: VideoInfo? = null,
    val selectedQuality: VideoQuality? = null,
    val selectedMode: DownloadMode = DownloadMode.VIDEO_AND_SUBTITLE,
    val subtitleMethod: SubtitleMethod = SubtitleMethod.NONE,
    val errorMessage: String? = null,
    val isResultScreenVisible: Boolean = false,
    val showExitDialog: Boolean = false,
    val subtitleSearchStep: Int = 0,
    val analysisStep: String = "",
    val cpuArch: String = "ARM 64-bit / Universal",
    val srtContent: String = "",
    val showSrtPreview: Boolean = false,
    val successMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val analyzeUrlUseCase: AnalyzeUrlUseCase,
    private val determineSubtitleMethodUseCase: DetermineSubtitleMethodUseCase,
    private val addDownloadUseCase: AddDownloadUseCase,
    private val downloadRepository: DownloadRepository,
    private val ytDlpEngine: YtDlpEngine,
    private val processMediaUseCase: com.agon.app.domain.usecase.ProcessMediaUseCase,
    private val transcribeAudioUseCase: com.agon.app.domain.usecase.TranscribeAudioUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            cpuArch = "ARM 64-bit / Universal"
        )
    }

    fun onUrlChange(url: String) {
        _uiState.value = _uiState.value.copy(url = url, errorMessage = null, successMessage = null)
    }

    fun selectMode(mode: DownloadMode) {
        _uiState.value = _uiState.value.copy(selectedMode = mode)
    }

    fun analyzeUrl() {
        val url = _uiState.value.url.trim()
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "الرجاء إدخال رابط الفيديو")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAnalyzing = true,
                errorMessage = null,
                analysisStep = "جاري تحليل الرابط...",
                videoInfo = null,
                isResultScreenVisible = false,
                subtitleSearchStep = 0
            )

            val result = analyzeUrlUseCase(url)

            result.onSuccess { info ->
                val method = determineSubtitleMethodUseCase(info.availableSubtitles)

                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    videoInfo = info,
                    subtitleMethod = method,
                    isResultScreenVisible = true,
                    selectedQuality = info.qualities.firstOrNull(),
                    successMessage = "تم تحليل الرابط بنجاح"
                )
            }

            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    errorMessage = error.message ?: "حدث خطأ أثناء التحليل",
                    analysisStep = ""
                )
            }
        }
    }

    fun selectQuality(quality: VideoQuality) {
        _uiState.value = _uiState.value.copy(selectedQuality = quality)
    }

    fun onBackPressed() {
        if (_uiState.value.isResultScreenVisible) {
            _uiState.value = _uiState.value.copy(showExitDialog = true)
        }
    }

    fun confirmExit() {
        _uiState.value = _uiState.value.copy(
            showExitDialog = false,
            isResultScreenVisible = false,
            videoInfo = null,
            subtitleSearchStep = 0
        )
    }

    fun dismissExitDialog() {
        _uiState.value = _uiState.value.copy(showExitDialog = false)
    }

    fun performSubtitleSearch() {
        _uiState.value = _uiState.value.copy(subtitleSearchStep = 1)
    }

    fun startSpecificDownload(mode: DownloadMode, method: SubtitleMethod) {
        val state = _uiState.value
        val videoInfo = state.videoInfo ?: return
        val quality = state.selectedQuality ?: return

        viewModelScope.launch {
            val downloadId = UUID.randomUUID().toString()
            val downloadDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "SubVIDD"
            )
            downloadDir.mkdirs()

            val videoFileName = "${videoInfo.title.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF]"), "_")}.${quality.format}"
            val outputPath = File(downloadDir, videoFileName).absolutePath

            val subtitleLang = if (method == SubtitleMethod.TRANSLATED_FROM_OTHER) {
                videoInfo.availableSubtitles.firstOrNull { it.languageCode != "ar" }?.languageCode ?: "en"
            } else {
                ""
            }

            val downloadItem = DownloadItem(
                id = downloadId,
                url = state.url,
                title = videoInfo.title,
                thumbnailUrl = videoInfo.thumbnailUrl,
                selectedQuality = quality.label,
                downloadMode = mode,
                subtitleMethod = method,
                status = DownloadStatus.QUEUED,
                totalSize = quality.fileSize,
                videoFilePath = if (mode != DownloadMode.SUBTITLE_ONLY) outputPath else "",
                srtFilePath = if (mode != DownloadMode.VIDEO_ONLY) outputPath.replaceAfterLast('.', "srt") else "",
                timestamp = System.currentTimeMillis()
            )

            addDownloadUseCase(downloadItem)

            val workData = Data.Builder()
                .putString(DownloadWorker.KEY_URL, state.url)
                .putString(DownloadWorker.KEY_FORMAT_ID, quality.id)
                .putString(DownloadWorker.KEY_OUTPUT_PATH, outputPath)
                .putString(DownloadWorker.KEY_DOWNLOAD_ID, downloadId)
                .putInt(DownloadWorker.KEY_NOTIFICATION_ID, downloadId.hashCode())
                .putString(DownloadWorker.KEY_DOWNLOAD_MODE, mode.name)
                .putString(DownloadWorker.KEY_SUBTITLE_METHOD, method.name)
                .putString(DownloadWorker.KEY_SUBTITLE_LANG, subtitleLang)
                .build()

            val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(workData)
                .build()

            WorkManager.getInstance(getApplication()).enqueue(downloadWorkRequest)
            downloadRepository.updateDownload(downloadItem.copy(workManagerId = downloadWorkRequest.id.toString()))

            val message = when (mode) {
                DownloadMode.VIDEO_ONLY -> "بدأ تحميل الفيديو وتمت إضافته للتنزيلات"
                DownloadMode.SUBTITLE_ONLY -> when (method) {
                    SubtitleMethod.WHISPER_GENERATED -> "جاري إنشاء الترجمة بالذكاء الاصطناعي وإضافتها للتنزيلات"
                    SubtitleMethod.TRANSLATED_FROM_OTHER -> "بدأت عملية ترجمة الترجمة وإضافتها للتنزيلات"
                    else -> "بدأ تحميل الترجمة المباشرة"
                }
                else -> "بدأ التحميل..."
            }

            _uiState.value = _uiState.value.copy(successMessage = message)
        }
    }

    fun toggleSrtPreview() {
        _uiState.value = _uiState.value.copy(showSrtPreview = !_uiState.value.showSrtPreview)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun resetState() {
        _uiState.value = HomeUiState(cpuArch = _uiState.value.cpuArch)
    }
}
