package com.agon.app.domain.usecase

import com.agon.app.data.AiModel
import com.agon.app.data.AiModelRepository
import com.agon.app.engine.AiModelManager
import java.io.IOException
import javax.inject.Inject

class DownloadAiModelUseCase @Inject constructor(
    private val repository: AiModelRepository,
    private val modelManager: AiModelManager
) {
    suspend operator fun invoke(
        modelId: String,
        onProgress: (Int, Long, Long) -> Unit
    ): Result<Unit> {
        return try {
            var model = repository.getModelById(modelId)
            
            if (model == null) {
                val modelInfo = modelManager.getAllModels().find { it.id == modelId }
                    ?: return Result.failure(Exception("لم يتم العثور على النموذج في قائمة النماذج المتاحة."))
                
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
                
                model = repository.getModelById(modelId)
            }

            if (model == null) {
                return Result.failure(Exception("فشل في إضافة النموذج إلى قاعدة البيانات."))
            }

            repository.updateDownloadState(
                id = modelId,
                isDownloaded = false,
                isCorrupted = false,
                isPaused = false
            )

            val result = modelManager.downloadModel(modelId, onProgress)

            result.fold(
                onSuccess = { file ->
                    repository.updateDownloadState(
                        id = modelId,
                        isDownloaded = true,
                        filePath = file.absolutePath,
                        isCorrupted = false,
                        isPaused = false
                    )
                    Result.success(Unit)
                },
                onFailure = { error ->
                    val isPaused = error.message?.contains("PAUSE_REQUESTED", ignoreCase = true) == true
                    
                    // التصحيح: إضافة "Socket closed" هنا أيضاً
                    val isCancelled = error is java.util.concurrent.CancellationException || 
                                      error.message?.contains("CANCEL_REQUESTED", ignoreCase = true) == true ||
                                      error.message?.contains("تم إلغاء التحميل", ignoreCase = true) == true ||
                                      error.message?.contains("Socket closed", ignoreCase = true) == true

                    if (isPaused) {
                        repository.updateDownloadState(
                            id = modelId,
                            isDownloaded = false,
                            isCorrupted = false,
                            isPaused = true
                        )
                        Result.failure(Exception("تم إيقاف التحميل مؤقتاً"))
                    } else if (isCancelled) {
                        repository.updateDownloadState(
                            id = modelId,
                            isDownloaded = false,
                            isCorrupted = false,
                            isPaused = false,
                            downloadedBytes = 0L
                        )
                        Result.failure(Exception("تم إلغاء التحميل"))
                    } else {
                        repository.updateDownloadState(
                            id = modelId,
                            isDownloaded = false,
                            isCorrupted = true,
                            isPaused = false
                        )
                        Result.failure(Exception(handleErrorMessage(error)))
                    }
                }
            )
        } catch (e: Exception) {
            val isPaused = e.message?.contains("PAUSE_REQUESTED", ignoreCase = true) == true
            
            val isCancelled = e is java.util.concurrent.CancellationException || 
                              e.message?.contains("CANCEL_REQUESTED", ignoreCase = true) == true ||
                              e.message?.contains("تم إلغاء التحميل", ignoreCase = true) == true ||
                              e.message?.contains("Socket closed", ignoreCase = true) == true
            
            if (isPaused) {
                repository.updateDownloadState(
                    id = modelId,
                    isDownloaded = false,
                    isCorrupted = false,
                    isPaused = true
                )
            } else if (isCancelled) {
                repository.updateDownloadState(
                    id = modelId,
                    isDownloaded = false,
                    isCorrupted = false,
                    isPaused = false,
                    downloadedBytes = 0L
                )
            } else {
                repository.updateDownloadState(
                    id = modelId,
                    isDownloaded = false,
                    isCorrupted = true,
                    isPaused = false
                )
            }
            
            Result.failure(Exception(handleErrorMessage(e)))
        }
    }

    fun pauseDownload(modelId: String) {
        modelManager.pauseDownload(modelId)
    }

    fun cancelDownload(modelId: String) {
        modelManager.cancelDownload(modelId)
    }

    private fun handleErrorMessage(error: Throwable): String {
        val message = error.message ?: "حدث خطأ غير معروف"
        
        return when {
            message.contains("تم إيقاف التحميل مؤقتاً", ignoreCase = true) || message.contains("PAUSE_REQUESTED", ignoreCase = true) -> 
                "تم إيقاف التحميل مؤقتاً."
            
            message.contains("تم إلغاء التحميل", ignoreCase = true) || message.contains("CANCEL_REQUESTED", ignoreCase = true) || message.contains("Socket closed", ignoreCase = true) -> 
                "تم إلغاء التحميل."
            
            message.contains("requires pivot translation", ignoreCase = true) || message.contains("cannot be downloaded directly", ignoreCase = true) -> 
                "هذا النموذج يعتمد على الترجمة المتتابعة. يرجى تحميل الحزم الأساسية بشكل منفصل."
            
            message.contains("HTTP") -> 
                "فشل التنزيل: تأكد من صحة الرابط أو توفر خادم النماذج."
            message.contains("Checksum", ignoreCase = true) -> 
                "فشل التحقق من الملف: الملف تالف أو غير مكتمل."
            error is IOException || message.contains("network", ignoreCase = true) || message.contains("timeout", ignoreCase = true) -> 
                "انقطع الاتصال بالشبكة. يرجى التحقق من الإنترنت وإعادة المحاولة."
            message.contains("space", ignoreCase = true) || message.contains("nospc", ignoreCase = true) -> 
                "لا توجد مساحة كافية على الجهاز لتنزيل النموذج."
            message.contains("Permission denied", ignoreCase = true) -> 
                "تم رفض الصلاحيات. يرجى التأكد من منح التطبيق صلاحيات التخزين."
            else -> "فشل التنزيل: $message"
        }
    }
}
