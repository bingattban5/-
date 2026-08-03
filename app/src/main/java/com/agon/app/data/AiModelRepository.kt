package com.agon.app.data

import com.agon.app.data.local.AiModelDao
import com.agon.app.data.local.AiModelEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiModelRepository @Inject constructor(
    private val aiModelDao: AiModelDao
) {
    fun getAllModels(): Flow<List<AiModel>> {
        return aiModelDao.getAllModels().map { entities ->
            entities.map { it.toAiModel() }
        }
    }

    fun getModelsByType(type: String): Flow<List<AiModel>> {
        return aiModelDao.getModelsByType(type).map { entities ->
            entities.map { it.toAiModel() }
        }
    }

    fun getDownloadedModels(): Flow<List<AiModel>> {
        return aiModelDao.getDownloadedModels().map { entities ->
            entities.map { it.toAiModel() }
        }
    }

    suspend fun getModelById(id: String): AiModel? {
        return aiModelDao.getModelById(id)?.toAiModel()
    }

    suspend fun getTotalStorageUsed(): Long {
        return aiModelDao.getTotalStorageUsed() ?: 0L
    }

    suspend fun addModel(model: AiModel) {
        aiModelDao.insertModel(model.toEntity())
    }

    suspend fun updateModel(model: AiModel) {
        aiModelDao.updateModel(model.toEntity())
    }

    // تم تحديث هذه الدالة لدعم الإيقاف المؤقت وحجم التحميل
    suspend fun updateDownloadState(
        id: String,
        isDownloaded: Boolean,
        filePath: String? = null,
        isCorrupted: Boolean = false,
        isPaused: Boolean = false,
        downloadedBytes: Long = 0L
    ) {
        val currentModel = getModelById(id)
        if (currentModel != null) {
            val updatedModel = currentModel.copy(
                isDownloaded = isDownloaded,
                filePath = filePath ?: currentModel.filePath,
                isCorrupted = isCorrupted,
                isPaused = isPaused,
                downloadedBytes = downloadedBytes
            )
            updateModel(updatedModel)
        }
    }

    suspend fun deleteModel(id: String) {
        aiModelDao.deleteModelById(id)
    }

    suspend fun updateLastUsed(id: String) {
        aiModelDao.updateLastUsed(id, System.currentTimeMillis())
    }

    private fun AiModelEntity.toAiModel(): AiModel {
        return AiModel(
            id = id,
            name = name,
            type = type,
            version = version,
            sizeBytes = sizeBytes,
            sizeFormatted = sizeFormatted,
            filePath = filePath,
            checksum = checksum,
            isDownloaded = isDownloaded,
            isCorrupted = isCorrupted,
            downloadUrl = downloadUrl,
            description = description,
            language = language,
            isPaused = isPaused,
            downloadedBytes = downloadedBytes,
            createdAt = createdAt,
            lastUsedAt = lastUsedAt
        )
    }

    private fun AiModel.toEntity(): AiModelEntity {
        return AiModelEntity(
            id = id,
            name = name,
            type = type,
            version = version,
            sizeBytes = sizeBytes,
            sizeFormatted = sizeFormatted,
            filePath = filePath,
            checksum = checksum,
            isDownloaded = isDownloaded,
            isCorrupted = isCorrupted,
            downloadUrl = downloadUrl,
            description = description,
            language = language,
            isPaused = isPaused,
            downloadedBytes = downloadedBytes,
            createdAt = createdAt,
            lastUsedAt = lastUsedAt
        )
    }
}
