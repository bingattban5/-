package com.agon.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.agon.app.data.DownloadItem
import com.agon.app.data.DownloadStatus
import com.agon.app.domain.usecase.DeleteDownloadUseCase
import com.agon.app.domain.usecase.GetDownloadsUseCase
import com.agon.app.data.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class DownloadsUiState(
    val selectedFilter: DownloadFilter = DownloadFilter.ALL,
    val showDeleteConfirm: String? = null,
    val showSrtPreview: DownloadItem? = null
)

enum class DownloadFilter(val label: String) {
    ALL("الكل"),
    ACTIVE("نشط"),
    COMPLETED("مكتمل"),
    FAILED("فشل")
}

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    application: Application,
    private val getDownloadsUseCase: GetDownloadsUseCase,
    private val deleteDownloadUseCase: DeleteDownloadUseCase,
    private val downloadRepository: DownloadRepository
) : AndroidViewModel(application) {

    // قراءة التنزيلات بشكل حي من قاعدة البيانات، مما يضمن وصول أخطاء الـ Worker فوراً للواجهة
    val downloads = getDownloadsUseCase().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    fun setFilter(filter: DownloadFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun filterDownloads(downloads: List<DownloadItem>): List<DownloadItem> {
        return when (_uiState.value.selectedFilter) {
            DownloadFilter.ALL -> downloads
            DownloadFilter.ACTIVE -> downloads.filter {
                it.status in listOf(
                    DownloadStatus.DOWNLOADING,
                    DownloadStatus.PAUSED,
                    DownloadStatus.QUEUED,
                    DownloadStatus.ANALYZING,
                    DownloadStatus.EXTRACTING_SUBS,
                    DownloadStatus.TRANSLATING
                )
            }
            DownloadFilter.COMPLETED -> downloads.filter { it.status == DownloadStatus.COMPLETED }
            DownloadFilter.FAILED -> downloads.filter { it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELLED }
        }
    }

    fun requestDelete(id: String) {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = id)
    }

    fun dismissDelete() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = null)
    }

    fun confirmDelete() {
        val id = _uiState.value.showDeleteConfirm ?: return
        viewModelScope.launch {
            // Cancel WorkManager task if active
            val download = downloadRepository.getDownloadById(id)
            if (download != null && download.workManagerId.isNotEmpty()) {
                try {
                    WorkManager.getInstance(getApplication())
                        .cancelWorkById(UUID.fromString(download.workManagerId))
                } catch (_: Exception) {}
            }
            deleteDownloadUseCase(id)
            _uiState.value = _uiState.value.copy(showDeleteConfirm = null)
        }
    }

    fun cancelDownload(id: String) {
        viewModelScope.launch {
            val download = downloadRepository.getDownloadById(id) ?: return@launch
            if (download.workManagerId.isNotEmpty()) {
                try {
                    WorkManager.getInstance(getApplication())
                        .cancelWorkById(UUID.fromString(download.workManagerId))
                } catch (_: Exception) {}
            }
            // تمرير رسالة واضحة للواجهة توضح سبب الفشل (الإلغاء)
            downloadRepository.updateDownload(download.copy(
                status = DownloadStatus.CANCELLED,
                errorMessage = "تم الإلغاء بواسطة المستخدم",
                isPaused = false
            ))
        }
    }

    fun retryDownload(id: String) {
        viewModelScope.launch {
            val download = downloadRepository.getDownloadById(id) ?: return@launch
            // تصفير الأخطاء والتقدم للبدء من جديد
            downloadRepository.updateDownload(download.copy(
                status = DownloadStatus.QUEUED,
                progress = 0,
                errorMessage = "",
                isPaused = false
            ))
        }
    }

    fun showSrtPreview(item: DownloadItem) {
        _uiState.value = _uiState.value.copy(showSrtPreview = item)
    }

    fun dismissSrtPreview() {
        _uiState.value = _uiState.value.copy(showSrtPreview = null)
    }

    fun clearCompleted() {
        viewModelScope.launch {
            downloadRepository.clearCompletedDownloads()
        }
    }
}
