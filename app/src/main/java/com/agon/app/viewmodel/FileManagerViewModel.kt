package com.agon.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agon.app.engine.DuplicateGroup
import com.agon.app.engine.FileManager
import com.agon.app.engine.MediaFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FileManagerUiState(
    val files: List<MediaFile> = emptyList(),
    val filteredFiles: List<MediaFile> = emptyList(),
    val selectedFilter: FileFilter = FileFilter.ALL,
    val isLoading: Boolean = false,
    val totalSize: String = "0 MB",
    val tempSize: String = "0 MB",
    val duplicates: List<DuplicateGroup> = emptyList(),
    val showDuplicateDialog: Boolean = false,
    val showRenameDialog: MediaFile? = null,
    val showDeleteDialog: MediaFile? = null,
    val showMoveDialog: MediaFile? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = ""
)

enum class FileFilter(val label: String) {
    ALL("الكل"),
    VIDEO("فيديو"),
    SUBTITLE("ترجمة"),
    AUDIO("صوت")
}

@HiltViewModel
class FileManagerViewModel @Inject constructor(
    application: Application,
    private val fileManager: FileManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(FileManagerUiState())
    val uiState: StateFlow<FileManagerUiState> = _uiState.asStateFlow()

    init {
        loadFiles()
    }

    fun loadFiles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val files = fileManager.listFiles()
            val totalSize = fileManager.getTotalSize()
            val tempSize = fileManager.getTempSize()

            _uiState.value = _uiState.value.copy(
                files = files,
                filteredFiles = applyFilter(files, _uiState.value.selectedFilter, _uiState.value.searchQuery),
                totalSize = formatBytes(totalSize),
                tempSize = formatBytes(tempSize),
                isLoading = false
            )
        }
    }

    fun setFilter(filter: FileFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        applyFilterAndSearch()
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilterAndSearch()
    }

    private fun applyFilterAndSearch() {
        val state = _uiState.value
        _uiState.value = state.copy(
            filteredFiles = applyFilter(state.files, state.selectedFilter, state.searchQuery)
        )
    }

    private fun applyFilter(files: List<MediaFile>, filter: FileFilter, query: String): List<MediaFile> {
        var result = when (filter) {
            FileFilter.ALL -> files
            FileFilter.VIDEO -> files.filter { it.isVideo }
            FileFilter.SUBTITLE -> files.filter { it.isSubtitle }
            FileFilter.AUDIO -> files.filter { it.isAudio }
        }

        if (query.isNotBlank()) {
            result = result.filter { it.name.contains(query, ignoreCase = true) }
        }

        return result
    }

    fun findDuplicates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val duplicates = fileManager.findDuplicates()
            _uiState.value = _uiState.value.copy(
                duplicates = duplicates,
                showDuplicateDialog = duplicates.isNotEmpty(),
                isLoading = false
            )
            if (duplicates.isEmpty()) {
                _uiState.value = _uiState.value.copy(successMessage = "لا توجد ملفات مكررة")
            }
        }
    }

    fun dismissDuplicateDialog() {
        _uiState.value = _uiState.value.copy(showDuplicateDialog = false)
    }

    fun deleteDuplicates(duplicates: List<DuplicateGroup>) {
        viewModelScope.launch {
            var deletedCount = 0
            for (group in duplicates) {
                // الاحتفاظ بالملف الأول وحذف الباقي
                for (i in 1 until group.files.size) {
                    val result = fileManager.deleteFile(group.files[i].path)
                    if (result.isSuccess) deletedCount++
                }
            }
            _uiState.value = _uiState.value.copy(
                showDuplicateDialog = false,
                successMessage = "تم حذف $deletedCount ملف مكرر"
            )
            loadFiles()
        }
    }

    fun showRenameDialog(file: MediaFile) {
        _uiState.value = _uiState.value.copy(showRenameDialog = file)
    }

    fun dismissRenameDialog() {
        _uiState.value = _uiState.value.copy(showRenameDialog = null)
    }

    fun renameFile(newName: String) {
        val file = _uiState.value.showRenameDialog ?: return
        viewModelScope.launch {
            val result = fileManager.renameFile(file.path, newName)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        showRenameDialog = null,
                        successMessage = "تم تغيير الاسم بنجاح"
                    )
                    loadFiles()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "فشل تغيير اسم الملف"
                    )
                }
            )
        }
    }

    fun showDeleteDialog(file: MediaFile) {
        _uiState.value = _uiState.value.copy(showDeleteDialog = file)
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = null)
    }

    fun deleteFile() {
        val file = _uiState.value.showDeleteDialog ?: return
        viewModelScope.launch {
            val result = fileManager.deleteFile(file.path)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        showDeleteDialog = null,
                        successMessage = "تم حذف الملف بنجاح"
                    )
                    loadFiles()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "فشل حذف الملف"
                    )
                }
            )
        }
    }

    fun cleanTempFiles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = fileManager.cleanTempFiles()
            result.fold(
                onSuccess = { count ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "تم تنظيف $count ملف مؤقت بنجاح"
                    )
                    loadFiles()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "فشل تنظيف الملفات المؤقتة"
                    )
                }
            )
        }
    }

    fun getShareIntent(file: MediaFile): android.content.Intent? {
        return fileManager.getShareIntent(file.path)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
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
