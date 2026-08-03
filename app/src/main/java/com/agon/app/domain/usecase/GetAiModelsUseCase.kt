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
        // جلب جميع النماذج المعروفة لدى المدير
        val allModelsFromManager = modelManager.getAllModels()
        
        // تطبيق فلتر ذكي للتحكم في ما يظهر للمستخدم:
        val modelsToSync = allModelsFromManager.filter { modelInfo ->
            when {
                // 1. إظهار جميع نماذج Whisper دائماً
                modelInfo.type == AiModelManager.ModelType.WHISPER -> true
                
                // 2. إظهار حزمة الترجمة الأساسية (إنجليزي <-> عربي) دائماً
                modelInfo.id == "argos-en-ar" -> true
                
                // 3. بالنسبة لحزم Argos الأخرى (مثل fr-en, es-en):
                // إظهارها فقط إذا كانت محملة بالفعل (isModelInstalled == true)
                // هذا يخفيها في البداية، ويظهرها تلقائياً مع زر الحذف إذا قام النظام بتحميلها في الخلفية
                modelInfo.type == AiModelManager.ModelType.ARGOS -> {
                    modelManager.isModelInstalled(modelInfo.id)
                }
                
                // 4. استبعاد أي شيء آخر (مثل الحزم الوهمية للترجمة المتتابعة fr-ar التي لا تملك رابط تحميل)
                else -> false
            }
        }
        
        for (modelInfo in modelsToSync) {
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
                        isDownloaded = modelManager.isModelInstalled(modelInfo.id),
                        isCorrupted = false,
                        downloadUrl = modelInfo.downloadUrl,
                        description = modelInfo.description,
                        language = modelInfo.language
                    )
                )
            } else {
                // تحديث حالة التحميل والفساد في حال تغيرت
                val isInstalled = modelManager.isModelInstalled(modelInfo.id)
                val isCorrupted = if (isInstalled) modelManager.isModelCorrupted(modelInfo.id) else false
                
                if (existing.isDownloaded != isInstalled || existing.isCorrupted != isCorrupted) {
                    repository.updateModel(existing.copy(
                        isDownloaded = isInstalled,
                        isCorrupted = isCorrupted
                    ))
                }
            }
        }
    }
}
