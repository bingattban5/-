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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class DownloadsUiState(
    val selectedFilter: DownloadFilter = DownloadFilter.ALL,
    val showDeleteConfirm: String? = null,
    val showSrtPreview: DownloadItem? = null,
    // ➕ متغيرات جديدة لدعم التحديد المتعدد
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet()
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

    // قراءة التنزيلات بشكل حي من قاعدة البيانات
    val downloads = getDownloadsUseCase().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    // قائمة مفلترة حية: تُعاد حسابها تلقائياً عند تغيير الفلتر أو تحديث قاعدة البيانات
    val filteredDownloads: StateFlow<List<DownloadItem>> = combine(
        getDownloadsUseCase(),
        _uiState.map { it.selectedFilter }
    ) { all, filter ->
        applyFilter(all, filter)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun setFilter(filter: DownloadFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        // عند تغيير الفلتر، نلغي التحديد لتجنب تحديد عناصر غير ظاهرة
        if (_uiState.value.isSelectionMode) {
            _uiState.value = _uiState.value.copy(selectedIds = emptySet())
        }
    }

    private fun applyFilter(list: List<DownloadItem>, filter: DownloadFilter): List<DownloadItem> {
        return when (filter) {
            DownloadFilter.ALL -> list
            DownloadFilter.ACTIVE -> list.filter {
                it.status in listOf(
                    DownloadStatus.DOWNLOADING,
                    DownloadStatus.PAUSED,
                    DownloadStatus.QUEUED,
                    DownloadStatus.ANALYZING,
                    DownloadStatus.EXTRACTING_SUBS,
                    DownloadStatus.TRANSLATING
                )
            }
            DownloadFilter.COMPLETED -> list.filter { it.status == DownloadStatus.COMPLETED }
            // شرط مقوّى: يشمل أي عنصر فاشل أو ملغي أو يحمل رسالة خطأ عالقة
            DownloadFilter.FAILED -> list.filter {
                it.status == DownloadStatus.FAILED ||
                it.status == DownloadStatus.CANCELLED ||
                (it.errorMessage.isNotEmpty() && it.status != DownloadStatus.COMPLETED)
            }
        }
    }

    fun filterDownloads(downloads: List<DownloadItem>): List<DownloadItem> {
        return applyFilter(downloads, _uiState.value.selectedFilter)
    }

    // ==========================================
    // ➕ دوال التحكم في التحديد المتعدد
    // ==========================================

    fun toggleSelectionMode() {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = !_uiState.value.isSelectionMode,
            selectedIds = emptySet()
        )
    }

    fun toggleItemSelection(id: String) {
        val current = _uiState.value.selectedIds
        val newSet = if (current.contains(id)) current - id else current + id
        _uiState.value = _uiState.value.copy(selectedIds = newSet)
    }

    fun selectAllVisible(visibleIds: List<String>) {
        _uiState.value = _uiState.value.copy(selectedIds = visibleIds.toSet())
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = false,
            selectedIds = emptySet()
        )
    }

    fun deleteSelectedItems() {
        viewModelScope.launch {
            val idsToDelete = _uiState.value.selectedIds.toList()
            idsToDelete.forEach { id ->
                val download = downloadRepository.getDownloadById(id)
                if (download != null && download.workManagerId.isNotEmpty()) {
                    try {
                        WorkManager.getInstance(getApplication())
                            .cancelWorkById(UUID.fromString(download.workManagerId))
                    } catch (_: Exception) {
                        // تجاهل الأخطاء في حال كان العمل قد انتهى بالفعل
                    }
                }
                deleteDownloadUseCase(id)
            }
            clearSelection() // الخروج من وضع التحديد بعد الحذف
        }
    }

    // ==========================================
    // دوال الإدارة الأصلية (محدثة قليلاً لمنع التعارض)
    // ==========================================

    fun requestDelete(id: String) {
        // منع فتح نافذة الحذف الفردية أثناء وضع التحديد المتعدد
        if (_uiState.value.isSelectionMode) return
        _uiState.value = _uiState.value.copy(showDeleteConfirm = id)
    }

    fun dismissDelete() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = null)
    }

    fun confirmDelete() {
        val id = _uiState.value.showDeleteConfirm ?: return
        viewModelScope.launch {
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