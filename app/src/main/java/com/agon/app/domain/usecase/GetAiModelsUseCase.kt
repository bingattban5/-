package com.agon.app.domain.usecase

import com.agon.app.data.AiModel
import com.agon.app.data.AiModelRepository
import com.agon.app.engine.AiModelManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAiModelsUseCase @Inject constructor(
    private val repository: AiModelRepository,
    private val modelManager: AiModelManager
) {
    operator fun invoke(): Flow<List<AiModel>> {
        return repository.getAllModels()
    }

    suspend fun syncWithAvailableModels() {
        val availableModels = modelManager.getAllModels()
        
        for (modelInfo in availableModels) {
            val existing = repository.getModelById(modelInfo.id)
            if (existing == null) {
                repository.addModel(
                    AiModel(
                        id = modelInfo.id,
                        name = modelInfo.name,
                        type = modelInfo.type.name.lowercase(),
                        version = "1.0",
                        sizeBytes = modelInfo.sizeBytes,
                        sizeFormatted = modelInfo.sizeFormatted,
                        filePath = "",
                        checksum = modelInfo.checksum,
                        isDownloaded = false,
                        isCorrupted = false,
                        downloadUrl = modelInfo.downloadUrl,
                        description = modelInfo.description,
                        language = modelInfo.language
                    )
                )
            } else {
                // Check for corruption
                val isCorrupted = modelManager.isModelCorrupted(modelInfo.id)
                if (existing.isCorrupted != isCorrupted) {
                    repository.updateModel(existing.copy(isCorrupted = isCorrupted))
                }
            }
        }
    }
}
