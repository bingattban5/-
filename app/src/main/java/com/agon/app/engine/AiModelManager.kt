package com.agon.app.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

/**
 * AI Model Manager for Whisper and Argos Translate models
 * Handles download, verification, and lifecycle management
 */
@Singleton
class AiModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val modelsDir: File by lazy {
        File(context.filesDir, "ai_models").apply { mkdirs() }
    }

    private val tempDir: File by lazy {
        File(context.cacheDir, "model_downloads").apply { mkdirs() }
    }

    // خريطة لتتبع اتصالات التحميل النشطة
    private val activeDownloads = ConcurrentHashMap<String, java.net.HttpURLConnection>()
    
    // مجموعة لتتبع النماذج التي تم إيقافها مؤقتاً
    private val pausedDownloads = ConcurrentHashMap.newKeySet<String>()

    data class ModelInfo(
        val id: String,
        val name: String,
        val type: ModelType,
        val sizeBytes: Long,
        val sizeFormatted: String,
        val downloadUrl: String,
        val checksum: String,
        val description: String,
        val language: String? = null,
        val isDownloaded: Boolean = false,
        val isCorrupted: Boolean = false
    )

    enum class ModelType {
        WHISPER,
        ARGOS
    }

    fun getAvailableWhisperModels(): List<ModelInfo> {
        return listOf(
            ModelInfo(
                id = "whisper-tiny",
                name = "Whisper Tiny",
                type = ModelType.WHISPER,
                sizeBytes = 75_000_000L,
                sizeFormatted = "75 MB",
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
                checksum = "",
                description = "Fastest model, lowest accuracy. Good for testing and low-end devices."
            ),
            ModelInfo(
                id = "whisper-base",
                name = "Whisper Base",
                type = ModelType.WHISPER,
                sizeBytes = 142_000_000L,
                sizeFormatted = "142 MB",
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
                checksum = "",
                description = "Good balance of speed and accuracy. Recommended for general use."
            ),
            ModelInfo(
                id = "whisper-small",
                name = "Whisper Small",
                type = ModelType.WHISPER,
                sizeBytes = 466_000_000L,
                sizeFormatted = "466 MB",
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",
                checksum = "",
                description = "High accuracy, slower. Good for professional transcription."
            ),
            ModelInfo(
                id = "whisper-medium",
                name = "Whisper Medium",
                type = ModelType.WHISPER,
                sizeBytes = 1_500_000_000L,
                sizeFormatted = "1.5 GB",
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium.bin",
                checksum = "",
                description = "Very high accuracy, requires significant resources."
            )
        )
    }

    fun getAvailableArgosModels(): List<ModelInfo> {
        return listOf(
            ModelInfo(
                id = "argos-en-ar",
                name = "English → Arabic",
                type = ModelType.ARGOS,
                sizeBytes = 45_000_000L,
                sizeFormatted = "45 MB",
                downloadUrl = "https://argos-net.com/v1/translate-en_ar-1_0.argosmodel",
                checksum = "",
                description = "Translate English subtitles to Arabic",
                language = "en-ar"
            ),
            ModelInfo(
                id = "argos-fr-en",
                name = "French → English",
                type = ModelType.ARGOS,
                sizeBytes = 45_000_000L,
                sizeFormatted = "45 MB",
                downloadUrl = "https://argos-net.com/v1/translate-fr_en-1_9.argosmodel",
                checksum = "",
                description = "French to English pivot model",
                language = "fr-en"
            ),
            ModelInfo(
                id = "argos-es-en",
                name = "Spanish → English",
                type = ModelType.ARGOS,
                sizeBytes = 45_000_000L,
                sizeFormatted = "45 MB",
                downloadUrl = "https://argos-net.com/v1/translate-es_en-1_9.argosmodel",
                checksum = "",
                description = "Spanish to English pivot model",
                language = "es-en"
            ),
            ModelInfo(
                id = "argos-de-en",
                name = "German → English",
                type = ModelType.ARGOS,
                sizeBytes = 45_000_000L,
                sizeFormatted = "45 MB",
                downloadUrl = "https://argos-net.com/v1/translate-de_en-1_3.argosmodel",
                checksum = "",
                description = "German to English pivot model",
                language = "de-en"
            ),
            ModelInfo(
                id = "argos-tr-en",
                name = "Turkish → English",
                type = ModelType.ARGOS,
                sizeBytes = 45_000_000L,
                sizeFormatted = "45 MB",
                downloadUrl = "https://argos-net.com/v1/translate-tr_en-1_5.argosmodel",
                checksum = "",
                description = "Turkish to English pivot model",
                language = "tr-en"
            ),
            ModelInfo(
                id = "argos-fr-ar",
                name = "French → Arabic (via English)",
                type = ModelType.ARGOS,
                sizeBytes = 90_000_000L,
                sizeFormatted = "90 MB",
                downloadUrl = "",
                checksum = "",
                description = "French to Arabic via English pivot. Requires fr→en + en→ar",
                language = "fr-ar"
            ),
            ModelInfo(
                id = "argos-es-ar",
                name = "Spanish → Arabic (via English)",
                type = ModelType.ARGOS,
                sizeBytes = 90_000_000L,
                sizeFormatted = "90 MB",
                downloadUrl = "",
                checksum = "",
                description = "Spanish to Arabic via English pivot. Requires es→en + en→ar",
                language = "es-ar"
            ),
            ModelInfo(
                id = "argos-de-ar",
                name = "German → Arabic (via English)",
                type = ModelType.ARGOS,
                sizeBytes = 90_000_000L,
                sizeFormatted = "90 MB",
                downloadUrl = "",
                checksum = "",
                description = "German to Arabic via English pivot. Requires de→en + en→ar",
                language = "de-ar"
            ),
            ModelInfo(
                id = "argos-tr-ar",
                name = "Turkish → Arabic (via English)",
                type = ModelType.ARGOS,
                sizeBytes = 90_000_000L,
                sizeFormatted = "90 MB",
                downloadUrl = "",
                checksum = "",
                description = "Turkish to Arabic via English pivot. Requires tr→en + en→ar",
                language = "tr-ar"
            )
        )
    }

    fun getPivotModels(language: String): List<String>? {
        return when (language) {
            "fr-ar" -> listOf("argos-fr-en", "argos-en-ar")
            "es-ar" -> listOf("argos-es-en", "argos-en-ar")
            "de-ar" -> listOf("argos-de-en", "argos-en-ar")
            "tr-ar" -> listOf("argos-tr-en", "argos-en-ar")
            "en-ar" -> null
            else -> null
        }
    }

    suspend fun isPivotPairInstalled(sourceLang: String, targetLang: String): Boolean {
        val language = "$sourceLang-$targetLang"
        val pivotModels = getPivotModels(language) ?: return isModelInstalled("argos-$language")
        return pivotModels.all { isModelInstalled(it) }
    }

    suspend fun isModelInstalled(modelId: String): Boolean = withContext(Dispatchers.IO) {
        val modelFile = File(modelsDir, modelId)
        modelFile.exists() && !isModelCorrupted(modelId)
    }

    suspend fun isModelCorrupted(modelId: String): Boolean = withContext(Dispatchers.IO) {
        val modelFile = File(modelsDir, modelId)
        if (!modelFile.exists()) return@withContext true

        try {
            val expectedChecksum = getExpectedChecksum(modelId)
            if (expectedChecksum.isNullOrBlank()) return@withContext false

            val actualChecksum = calculateChecksum(modelFile)
            actualChecksum != expectedChecksum
        } catch (e: Exception) {
            true
        }
    }

    suspend fun downloadModel(
        modelId: String,
        onProgress: (Int, Long, Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var connection: java.net.HttpURLConnection? = null
        try {
            val modelInfo = getAllModels().find { it.id == modelId }
                ?: return@withContext Result.failure(Exception("Model not found in the list"))

            if (modelInfo.downloadUrl.isBlank()) {
                return@withContext Result.failure(Exception(
                    "This model (${modelInfo.id}) requires pivot translation and cannot be downloaded directly."
                ))
            }

            val tempFile = File(tempDir, "${modelId}.tmp")
            val finalFile = File(modelsDir, modelId)

            if (finalFile.exists() && !isModelCorrupted(modelId)) {
                return@withContext Result.success(finalFile)
            }

            var downloadedBytes = if (tempFile.exists()) tempFile.length() else 0L
            
            connection = java.net.URL(modelInfo.downloadUrl).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000
            
            if (downloadedBytes > 0) {
                connection.setRequestProperty("Range", "bytes=$downloadedBytes-")
            }
            
            activeDownloads[modelId] = connection
            connection.connect()

            val responseCode = connection.responseCode
            
            if (responseCode == 200 && downloadedBytes > 0) {
                tempFile.delete()
                downloadedBytes = 0L
                connection.disconnect()
                
                connection = java.net.URL(modelInfo.downloadUrl).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 30_000
                connection.readTimeout = 30_000
                activeDownloads[modelId] = connection
                connection.connect()
            }

            if (connection.responseCode != 200 && connection.responseCode != 206) {
                connection.disconnect()
                return@withContext Result.failure(Exception("Download failed: HTTP ${connection.responseCode} from ${modelInfo.downloadUrl}"))
            }

            val totalBytes = if (connection.responseCode == 206) {
                val contentRange = connection.getHeaderField("Content-Range")
                contentRange?.substringAfter("/")?.toLongOrNull() ?: modelInfo.sizeBytes
            } else {
                connection.contentLengthLong.takeIf { it > 0 } ?: modelInfo.sizeBytes
            }

            var currentBytes = downloadedBytes
            val shouldAppend = downloadedBytes > 0 && connection.responseCode == 206

            connection.inputStream.use { input ->
                if (shouldAppend) {
                    java.io.FileOutputStream(tempFile, true).use { appendOutput ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (!activeDownloads.containsKey(modelId)) {
                                if (pausedDownloads.contains(modelId)) {
                                    throw java.io.IOException("PAUSE_REQUESTED")
                                } else {
                                    throw java.io.IOException("CANCEL_REQUESTED")
                                }
                            }
                            appendOutput.write(buffer, 0, bytesRead)
                            currentBytes += bytesRead
                            val progress = ((currentBytes.toFloat() / totalBytes) * 100).toInt().coerceIn(0, 100)
                            onProgress(progress, currentBytes, totalBytes)
                        }
                    }
                } else {
                    tempFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (!activeDownloads.containsKey(modelId)) {
                                if (pausedDownloads.contains(modelId)) {
                                    throw java.io.IOException("PAUSE_REQUESTED")
                                } else {
                                    throw java.io.IOException("CANCEL_REQUESTED")
                                }
                            }
                            output.write(buffer, 0, bytesRead)
                            currentBytes += bytesRead
                            val progress = ((currentBytes.toFloat() / totalBytes) * 100).toInt().coerceIn(0, 100)
                            onProgress(progress, currentBytes, totalBytes)
                        }
                    }
                }
            }
            connection.disconnect()

            if (modelInfo.checksum.isNotBlank()) {
                val checksum = calculateChecksum(tempFile)
                if (checksum != modelInfo.checksum) {
                    tempFile.delete()
                    return@withContext Result.failure(Exception("Checksum verification failed. Expected: ${modelInfo.checksum}, Got: $checksum"))
                }
            }

            tempFile.renameTo(finalFile)

            Result.success(finalFile)
        } catch (e: Exception) {
            val tempFile = File(tempDir, "$modelId.tmp")
            
            // التصحيح الجذري: التعرف على "Socket closed" كإشارة إيقاف/إلغاء يدوي
            val isPaused = pausedDownloads.contains(modelId) || e.message?.contains("PAUSE_REQUESTED", ignoreCase = true) == true
            
            val isManualDisconnect = e.message?.contains("CANCEL_REQUESTED", ignoreCase = true) == true ||
                                     e.message?.contains("Socket closed", ignoreCase = true) == true || // <-- الإضافة هنا
                                     e.message?.contains("stream closed", ignoreCase = true) == true

            if (isPaused) {
                Result.failure(Exception("PAUSE_REQUESTED"))
            } else if (isManualDisconnect) {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                Result.failure(Exception("CANCEL_REQUESTED"))
            } else {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                Result.failure(Exception("Download error for $modelId: ${e.message}"))
            }
        } finally {
            activeDownloads.remove(modelId)
            pausedDownloads.remove(modelId)
            connection?.disconnect()
        }
    }

    fun pauseDownload(modelId: String) {
        pausedDownloads.add(modelId)
        val connection = activeDownloads.remove(modelId)
        connection?.disconnect()
    }

    fun cancelDownload(modelId: String) {
        pausedDownloads.remove(modelId)
        val connection = activeDownloads.remove(modelId)
        connection?.disconnect()
        
        val tempFile = File(tempDir, "$modelId.tmp")
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    fun deleteModel(modelId: String): Boolean {
        val modelFile = File(modelsDir, modelId)
        return if (modelFile.exists()) {
            modelFile.delete()
        } else {
            false
        }
    }

    fun getTotalStorageUsed(): Long {
        return modelsDir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    fun getAllModels(): List<ModelInfo> {
        return getAvailableWhisperModels() + getAvailableArgosModels()
    }

    private suspend fun calculateChecksum(file: File): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun getExpectedChecksum(modelId: String): String? {
        return getAllModels().find { it.id == modelId }?.checksum
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
