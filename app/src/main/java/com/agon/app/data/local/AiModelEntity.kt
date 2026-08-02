package com.agon.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_models")
data class AiModelEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String, // "whisper" or "argos"
    val version: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val filePath: String,
    val checksum: String,
    val isDownloaded: Boolean,
    val isCorrupted: Boolean,
    val downloadUrl: String,
    val description: String,
    val language: String?, // For Argos models
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null
)
