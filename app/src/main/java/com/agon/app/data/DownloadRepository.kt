package com.agon.app.data

import com.agon.app.data.local.DownloadDao
import com.agon.app.data.local.DownloadEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    private val downloadDao: DownloadDao
) {
    fun getAllDownloads(): Flow<List<DownloadItem>> {
        return downloadDao.getAllDownloads().map { entities ->
            entities.map { it.toDownloadItem() }
        }
    }

    suspend fun getDownloadById(id: String): DownloadItem? {
        return downloadDao.getDownloadById(id)?.toDownloadItem()
    }

    suspend fun addDownload(item: DownloadItem) {
        downloadDao.insertDownload(item.toEntity())
    }

    suspend fun updateDownload(item: DownloadItem) {
        downloadDao.updateDownload(item.toEntity())
    }

    suspend fun deleteDownload(id: String) {
        downloadDao.deleteDownloadById(id)
    }

    suspend fun clearCompletedDownloads() {
        downloadDao.clearCompletedDownloads()
    }

    suspend fun updateProgress(
        id: String,
        status: DownloadStatus,
        progress: Int,
        downloadedSize: String = "",
        speed: String = "",
        eta: String = ""
    ) {
        downloadDao.updateProgress(id, status.name, progress, downloadedSize, speed, eta)
    }

    suspend fun updatePauseState(id: String, status: DownloadStatus, isPaused: Boolean) {
        downloadDao.updatePauseState(id, status.name, isPaused)
    }

    suspend fun updateError(id: String, status: DownloadStatus, error: String) {
        downloadDao.updateError(id, status.name, error)
    }

    private fun DownloadEntity.toDownloadItem(): DownloadItem {
        return DownloadItem(
            id = id,
            url = url,
            title = title,
            thumbnailUrl = thumbnailUrl,
            selectedQuality = selectedQuality,
            downloadMode = downloadMode,
            subtitleMethod = subtitleMethod,
            status = status,
            progress = progress,
            totalSize = totalSize,
            downloadedSize = downloadedSize,
            downloadSpeed = downloadSpeed,
            eta = eta,
            srtFilePath = srtFilePath,
            videoFilePath = videoFilePath,
            errorMessage = errorMessage,
            timestamp = timestamp,
            isPaused = isPaused,
            workManagerId = workManagerId
        )
    }

    private fun DownloadItem.toEntity(): DownloadEntity {
        return DownloadEntity(
            id = id,
            url = url,
            title = title,
            thumbnailUrl = thumbnailUrl,
            selectedQuality = selectedQuality,
            downloadMode = downloadMode,
            subtitleMethod = subtitleMethod,
            status = status,
            progress = progress,
            totalSize = totalSize,
            downloadedSize = downloadedSize,
            downloadSpeed = downloadSpeed,
            eta = eta,
            srtFilePath = srtFilePath,
            videoFilePath = videoFilePath,
            errorMessage = errorMessage,
            timestamp = timestamp,
            isPaused = isPaused,
            workManagerId = workManagerId
        )
    }
}
