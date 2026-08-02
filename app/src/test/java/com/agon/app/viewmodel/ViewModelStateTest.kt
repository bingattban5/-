package com.agon.app.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewModelStateTest {

    @Test
    fun `HomeUiState default values are correct`() {
        val state = HomeUiState()

        assertEquals("", state.url)
        assertFalse(state.isAnalyzing)
        assertNull(state.videoInfo)
        assertNull(state.selectedQuality)
        assertEquals(com.agon.app.data.DownloadMode.VIDEO_AND_SUBTITLE, state.selectedMode)
        assertEquals(com.agon.app.data.SubtitleMethod.NONE, state.subtitleMethod)
        assertNull(state.errorMessage)
        assertFalse(state.showBottomSheet)
        assertFalse(state.isDownloading)
        assertEquals(0, state.downloadProgress)
        assertEquals("", state.analysisStep)
        assertEquals("", state.cpuArch)
        assertEquals("", state.srtContent)
        assertFalse(state.showSrtPreview)
        assertNull(state.successMessage)
    }

    @Test
    fun `DownloadsUiState default values are correct`() {
        val state = DownloadsUiState()

        assertEquals(DownloadFilter.ALL, state.selectedFilter)
        assertNull(state.showDeleteConfirm)
        assertNull(state.showSrtPreview)
    }

    @Test
    fun `ModelsUiState default values are correct`() {
        val state = ModelsUiState()

        assertEquals(0, state.selectedTab)
        assertNull(state.successMessage)
        assertNull(state.errorMessage)
        assertTrue(state.aiModels.isEmpty())
        assertEquals(0L, state.totalStorageUsed)
        assertEquals("0 MB", state.totalStorageFormatted)
        assertFalse(state.ytDlpEngine.isInstalled)
        assertTrue(state.downloadingModels.isEmpty())
    }

    @Test
    fun `SettingsUiState default values are correct`() {
        val state = SettingsUiState()

        assertFalse(state.showClearCacheDialog)
        assertEquals("45.2 MB", state.cacheSize)
        assertNull(state.successMessage)
    }

    @Test
    fun `FileManagerUiState default values are correct`() {
        val state = FileManagerUiState()

        assertTrue(state.files.isEmpty())
        assertTrue(state.filteredFiles.isEmpty())
        assertEquals(FileFilter.ALL, state.selectedFilter)
        assertFalse(state.isLoading)
        assertEquals("0 MB", state.totalSize)
        assertEquals("0 MB", state.tempSize)
        assertTrue(state.duplicates.isEmpty())
        assertFalse(state.showDuplicateDialog)
        assertNull(state.showRenameDialog)
        assertNull(state.showDeleteDialog)
        assertNull(state.showMoveDialog)
        assertNull(state.errorMessage)
        assertNull(state.successMessage)
        assertEquals("", state.searchQuery)
    }

    @Test
    fun `HomeUiState copy updates url correctly`() {
        val state = HomeUiState()
        val updated = state.copy(url = "https://youtube.com/watch?v=test")

        assertEquals("https://youtube.com/watch?v=test", updated.url)
        assertFalse(updated.isAnalyzing)
    }

    @Test
    fun `HomeUiState copy updates analyzing state correctly`() {
        val state = HomeUiState()
        val analyzing = state.copy(
            isAnalyzing = true,
            analysisStep = "جاري التحليل..."
        )

        assertTrue(analyzing.isAnalyzing)
        assertEquals("جاري التحليل...", analyzing.analysisStep)
    }

    @Test
    fun `DownloadsUiState filter changes work correctly`() {
        val state = DownloadsUiState()

        val activeFilter = state.copy(selectedFilter = DownloadFilter.ACTIVE)
        assertEquals(DownloadFilter.ACTIVE, activeFilter.selectedFilter)

        val completedFilter = state.copy(selectedFilter = DownloadFilter.COMPLETED)
        assertEquals(DownloadFilter.COMPLETED, completedFilter.selectedFilter)

        val failedFilter = state.copy(selectedFilter = DownloadFilter.FAILED)
        assertEquals(DownloadFilter.FAILED, failedFilter.selectedFilter)
    }

    @Test
    fun `ModelsUiState tab selection works correctly`() {
        val state = ModelsUiState()

        val whisperTab = state.copy(selectedTab = 1)
        assertEquals(1, whisperTab.selectedTab)

        val argosTab = state.copy(selectedTab = 2)
        assertEquals(2, argosTab.selectedTab)
    }

    @Test
    fun `FileManagerUiState search query works correctly`() {
        val state = FileManagerUiState()
        val updated = state.copy(searchQuery = "video")

        assertEquals("video", updated.searchQuery)
    }
}
