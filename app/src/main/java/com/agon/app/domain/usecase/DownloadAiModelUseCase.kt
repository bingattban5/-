package com.agon.app.domain.usecase

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
            val model = repository.getModelById(modelId)
                ?: return Result.failure(Exception("لم يتم العثور على النموذج في قاعدة البيانات."))

            val result = modelManager.downloadModel(modelId, onProgress)

            result.fold(
                onSuccess = { file ->
                    // استخدام الدالة الجديدة لتحديث الحالة بأمان
                    repository.updateDownloadState(
                        id = modelId,
                        isDownloaded = true,
                        filePath = file.absolutePath,
                        isCorrupted = false
                    )
                    Result.success(Unit)
                },
                onFailure = { error ->
                    // تحديث الحالة لضمان عدم بقاء النموذج معلقاً إذا فشل
                    repository.updateDownloadState(
                        id = modelId,
                        isDownloaded = false,
                        isCorrupted = true
                    )
                    Result.failure(Exception(handleErrorMessage(error)))
                }
            )
        } catch (e: Exception) {
            Result.failure(Exception(handleErrorMessage(e)))
        }
    }

    private fun handleErrorMessage(error: Throwable): String {
        val message = error.message ?: "حدث خطأ غير معروف"
        
        return when {
            message.contains("HTTP") -> "فشل التنزيل: تأكد من صحة الرابط أو توفر خادم النماذج ($message)."
            message.contains("Checksum", ignoreCase = true) -> "فشل التحقق من الملف: الملف تالف أو غير مكتمل."
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
