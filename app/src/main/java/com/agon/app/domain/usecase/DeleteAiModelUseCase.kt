package com.agon.app.domain.usecase

import com.agon.app.data.AiModelRepository
import com.agon.app.engine.AiModelManager
import javax.inject.Inject

class DeleteAiModelUseCase @Inject constructor(
    private val repository: AiModelRepository,
    private val modelManager: AiModelManager
) {
    suspend operator fun invoke(modelId: String): Result<Unit> {
        return try {
            val deleted = modelManager.deleteModel(modelId)
            if (deleted) {
                val model = repository.getModelById(modelId)
                if (model != null) {
                    repository.updateModel(
                        model.copy(
                            isDownloaded = false,
                            filePath = ""
                        )
                    )
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete model"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
