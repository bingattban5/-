package com.agon.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.agon.app.data.DownloadMode
import com.agon.app.data.DownloadStatus
import com.agon.app.data.SubtitleMethod

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String,
    val selectedQuality: String,
    val downloadMode: DownloadMode,
    val subtitleMethod: SubtitleMethod,
    val status: DownloadStatus,
    val progress: Int,
    val totalSize: String,
    val downloadedSize: String,
    val downloadSpeed: String,
    val eta: String,
    val srtFilePath: String,
    val videoFilePath: String,
    val errorMessage: String,
    val timestamp: Long,
    val isPaused: Boolean,
    val workManagerId: String
)
