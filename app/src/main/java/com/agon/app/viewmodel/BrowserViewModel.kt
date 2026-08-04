package com.agon.app.viewmodel

import android.app.Application
import android.os.Environment
import android.util.Patterns
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
import com.agon.app.data.VideoQuality
import com.agon.app.data.browser.TabManager
import com.agon.app.domain.usecase.AddDownloadUseCase
import com.agon.app.domain.usecase.AnalyzeUrlUseCase
import com.agon.app.domain.usecase.DetermineSubtitleMethodUseCase
import com.agon.app.ui.screens.browser.state.BrowserState
import com.agon.app.ui.screens.browser.state.TabInfo
import com.agon.app.worker.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    application: Application,
    private val tabManager: TabManager,
    private val analyzeUrlUseCase: AnalyzeUrlUseCase,
    private val determineSubtitleMethodUseCase: DetermineSubtitleMethodUseCase,
    private val addDownloadUseCase: AddDownloadUseCase,
    private val downloadRepository: DownloadRepository
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(BrowserState())
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    private val _pendingNavigation = MutableStateFlow<String?>(null)
    val pendingNavigation: StateFlow<String?> = _pendingNavigation.asStateFlow()

    init {
        // ربط حالة التبويبات بالحالة الشاملة
        viewModelScope.launch {
            combine(tabManager.tabs, tabManager.activeTabIndex) { tabs, activeIndex ->
                Pair(tabs, activeIndex)
            }.collect { (tabs, activeIndex) ->
                _state.value = _state.value.copy(
                    tabs = tabs,
                    activeTabIndex = activeIndex
                )
            }
        }
    }

    // ========================================
    // إدارة التبويبات
    // ========================================

    fun createNewTab() {
        tabManager.createNewTab()
    }

    fun closeTab(tabId: String) {
        tabManager.closeTab(tabId)
        hideVideoSheet()
    }

    fun activateTab(index: Int) {
        tabManager.activateTab(index)
        hideVideoSheet()
    }

    fun closeOtherTabs() {
        tabManager.closeOtherTabs()
    }

    fun toggleTabsList() {
        _state.value = _state.value.copy(showTabsList = !_state.value.showTabsList)
    }

    fun dismissTabsList() {
        _state.value = _state.value.copy(showTabsList = false)
    }

    // ========================================
    // تحديث حالة التبويب النشط
    // ========================================

    fun onPageStarted(url: String) {
        tabManager.getActiveTab()?.let { tab ->
            tabManager.updateTab(tab.id) {
                it.copy(isLoading = true, url = url, progress = 0)
            }
        }
    }

    fun onPageProgressChanged(progress: Int) {
        tabManager.getActiveTab()?.let { tab ->
            tabManager.updateTab(tab.id) {
                it.copy(progress = progress)
            }
        }
    }

    fun onPageFinished(url: String, title: String, favicon: String?) {
        tabManager.getActiveTab()?.let { tab ->
            tabManager.updateTab(tab.id) {
                it.copy(
                    isLoading = false,
                    url = url,
                    title = title.ifBlank { extractDomain(url) },
                    favicon = favicon,
                    progress = 100
                )
            }
        }

        // محاولة اكتشاف فيديو في الصفحة
        detectVideoInPage(url)
    }

    fun updateNavigationState(canGoBack: Boolean, canGoForward: Boolean) {
        tabManager.getActiveTab()?.let { tab ->
            tabManager.updateTab(tab.id) {
                it.copy(canGoBack = canGoBack, canGoForward = canGoForward)
            }
        }
    }

    // ========================================
    // اكتشاف الفيديو
    // ========================================

    private fun detectVideoInPage(url: String) {
        // لا نحلل إذا كان هناك فيديو مكتشف بالفعل لنفس الرابط
        if (_state.value.detectedVideoUrl == url) return

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isAnalyzing = true,
                analysisStep = "جاري فحص الصفحة للفيديو..."
            )

            val result = analyzeUrlUseCase(url)

            result.onSuccess { videoInfo ->
                val method = determineSubtitleMethodUseCase(videoInfo.availableSubtitles)

                _state.value = _state.value.copy(
                    isAnalyzing = false,
                    detectedVideoUrl = url,
                    videoInfo = videoInfo,
                    subtitleMethod = method,
                    selectedQuality = videoInfo.qualities.firstOrNull(),
                    showVideoSheet = true
                )
            }

            result.onFailure {
                // لا يوجد فيديو، نعيد الحالة فقط
                _state.value = _state.value.copy(
                    isAnalyzing = false,
                    analysisStep = "",
                    detectedVideoUrl = null
                )
            }
        }
    }

    // ========================================
    // إدارة صفحة الفيديو
    // ========================================

    fun showVideoSheet() {
        if (_state.value.videoInfo != null) {
            _state.value = _state.value.copy(showVideoSheet = true)
        }
    }

    fun hideVideoSheet() {
        _state.value = _state.value.copy(showVideoSheet = false)
    }

    fun selectQuality(quality: VideoQuality) {
        _state.value = _state.value.copy(selectedQuality = quality)
    }

    fun performSubtitleSearch() {
        _state.value = _state.value.copy(subtitleSearchStep = 1)
    }

    // ========================================
    // التنزيل
    // ========================================

    fun startSpecificDownload(mode: DownloadMode, method: SubtitleMethod) {
        viewModelScope.launch {
            try {
                val currentState = _state.value
                val videoInfo = currentState.videoInfo ?: throw IllegalStateException("معلومات الفيديو غير متوفرة")
                val quality = currentState.selectedQuality ?: throw IllegalStateException("لم يتم اختيار الجودة")
                val url = currentState.detectedVideoUrl ?: throw IllegalStateException("الرابط غير متوفر")

                val downloadId = UUID.randomUUID().toString()
                val downloadDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "SubVIDD"
                )
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }

                val cleanTitle = videoInfo.title
                    .replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF\\s]"), "_")
                    .replace(Regex("\\s+"), "_")
                    .take(50)

                val format = quality.format.ifEmpty { "mp4" }
                val videoFileName = "${cleanTitle}.${format}"
                val outputPath = File(downloadDir, videoFileName).absolutePath

                val subtitleLang = if (method == SubtitleMethod.TRANSLATED_FROM_OTHER) {
                    videoInfo.availableSubtitles.firstOrNull { it.languageCode != "ar" }?.languageCode ?: "en"
                } else {
                    ""
                }

                val srtPath = if (mode != DownloadMode.VIDEO_ONLY) {
                    if (outputPath.contains(".")) outputPath.replaceAfterLast('.', "srt") else "$outputPath.srt"
                } else ""

                val downloadItem = DownloadItem(
                    id = downloadId,
                    url = url,
                    title = videoInfo.title,
                    thumbnailUrl = videoInfo.thumbnailUrl,
                    selectedQuality = quality.label,
                    downloadMode = mode,
                    subtitleMethod = method,
                    status = DownloadStatus.QUEUED,
                    totalSize = quality.fileSize,
                    videoFilePath = if (mode != DownloadMode.SUBTITLE_ONLY) outputPath else "",
                    srtFilePath = srtPath,
                    timestamp = System.currentTimeMillis()
                )

                addDownloadUseCase(downloadItem)

                val workData = Data.Builder()
                    .putString(DownloadWorker.KEY_URL, url)
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
                    DownloadMode.VIDEO_ONLY -> "بدأ تحميل الفيديو"
                    DownloadMode.SUBTITLE_ONLY -> when (method) {
                        SubtitleMethod.WHISPER_GENERATED -> "جاري إنشاء الترجمة بالذكاء الاصطناعي"
                        SubtitleMethod.TRANSLATED_FROM_OTHER -> "بدأت عملية الترجمة"
                        else -> "بدأ تحميل الترجمة"
                    }
                    else -> "بدأ التحميل"
                }

                _state.value = _state.value.copy(
                    successMessage = message,
                    showVideoSheet = false
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    errorMessage = "فشل بدء التحميل: ${e.localizedMessage ?: e.message ?: "خطأ غير معروف"}"
                )
            }
        }
    }

    // ========================================
    // البحث في شريط العنوان
    // ========================================

    fun navigateToInput(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return

        val url = if (isUrl(trimmed)) {
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                "https://$trimmed"
            }
        } else {
            "https://www.google.com/search?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
        }

        _pendingNavigation.value = url
    }

    fun clearPendingNavigation() {
        _pendingNavigation.value = null
    }

    private fun isUrl(text: String): Boolean {
        return Patterns.WEB_URL.matcher(text).matches() ||
                text.contains(".com") ||
                text.contains(".org") ||
                text.contains(".net") ||
                text.contains(".io") ||
                text.contains("youtube.com") ||
                text.contains("youtu.be")
    }

    // ========================================
    // الضغط المطول على الروابط
    // ========================================

    fun onLinkLongPressed(link: String) {
        _state.value = _state.value.copy(
            longPressedLink = link,
            showLinkMenu = true
        )
    }

    fun dismissLinkMenu() {
        _state.value = _state.value.copy(
            showLinkMenu = false,
            longPressedLink = null
        )
    }

    fun openLinkInBackground(link: String) {
        val newTab = TabInfo.createNew().copy(
            url = link,
            title = extractDomain(link),
            isLoading = true
        )
        _state.value = _state.value.copy(tabs = _state.value.tabs + newTab)
        dismissLinkMenu()
    }

    // ========================================
    // القائمة الرئيسية
    // ========================================

    fun toggleMainMenu() {
        _state.value = _state.value.copy(showMainMenu = !_state.value.showMainMenu)
    }

    fun dismissMainMenu() {
        _state.value = _state.value.copy(showMainMenu = false)
    }

    // ========================================
    // الرسائل
    // ========================================

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun clearSuccess() {
        _state.value = _state.value.copy(successMessage = null)
    }

    // ========================================
    // مساعدات
    // ========================================

    private fun extractDomain(url: String): String {
        return try {
            URL(url).host.removePrefix("www.")
        } catch (e: Exception) {
            url.take(30)
        }
    }
}
