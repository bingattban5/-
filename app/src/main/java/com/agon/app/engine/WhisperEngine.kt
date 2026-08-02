package com.agon.app.engine

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

data class WhisperProgress(
    val progress: Int,
    val message: String,
    val currentSegment: String = ""
)

data class WhisperSegment(
    val startTime: Long,
    val endTime: Long,
    val text: String
)

data class WhisperResult(
    val segments: List<WhisperSegment>,
    val language: String,
    val duration: Long
)

@Singleton
class WhisperEngine @Inject constructor(
    private val context: Context
) {
    private val whisperBinary: File by lazy {
        val filesDir = context.filesDir
        File(filesDir, "whisper.cpp")
    }

    // تم التعديل: يجب أن يتطابق مع المجلد المستخدم في AiModelManager
    private val modelsDir: File by lazy {
        val filesDir = context.filesDir
        File(filesDir, "ai_models").apply { mkdirs() }
    }

    // تم التعديل: fileName يجب أن يتطابق مع الـ ID المستخدم عند التنزيل في AiModelManager
    enum class ModelSize(val displayName: String, val fileName: String, val sizeMB: Int) {
        TINY("Tiny", "whisper-tiny", 75),
        BASE("Base", "whisper-base", 142),
        SMALL("Small", "whisper-small", 466),
        MEDIUM("Medium", "whisper-medium", 1500),
        LARGE("Large", "whisper-large", 3000)
    }

    fun isWhisperInstalled(): Boolean {
        return whisperBinary.exists() && whisperBinary.canExecute()
    }

    suspend fun installWhisper(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val arch = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
            val binaryName = when (arch) {
                "arm64-v8a" -> "whisper-arm64"
                "armeabi-v7a" -> "whisper-arm"
                "x86_64" -> "whisper-x86_64"
                "x86" -> "whisper-x86"
                else -> return@withContext Result.failure(Exception("Unsupported architecture: $arch"))
            }

            val assetManager = context.assets
            val assetsList = assetManager.list("") ?: emptyArray()

            // التحقق الاستباقي من وجود الملف
            if (!assetsList.contains(binaryName)) {
                return@withContext Result.failure(Exception("CRITICAL: '$binaryName' not found in app/src/main/assets/"))
            }

            val whisperInput = assetManager.open(binaryName)
            whisperInput.use { input ->
                whisperBinary.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            val isExecutable = whisperBinary.setExecutable(true)
            if (!isExecutable) {
                return@withContext Result.failure(Exception("Failed to grant execute permission to Whisper binary."))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to install Whisper: ${e.message}"))
        }
    }

    fun isModelDownloaded(modelSize: ModelSize): Boolean {
        val modelFile = File(modelsDir, modelSize.fileName)
        // تم التعديل: التحقق من أن الملف موجود وحجمه أكبر من صفر (ليس تالفاً)
        return modelFile.exists() && modelFile.length() > 0L
    }

    fun getModelPath(modelSize: ModelSize): String {
        return File(modelsDir, modelSize.fileName).absolutePath
    }

    fun getAvailableModels(): List<ModelSize> {
        return ModelSize.values().filter { isModelDownloaded(it) }
    }

    fun selectBestModel(availableRamMB: Long): ModelSize {
        // Select model based on available RAM
        // Each model requires approximately 2x its size in RAM
        return when {
            availableRamMB >= 6000 && isModelDownloaded(ModelSize.LARGE) -> ModelSize.LARGE
            availableRamMB >= 3000 && isModelDownloaded(ModelSize.MEDIUM) -> ModelSize.MEDIUM
            availableRamMB >= 1000 && isModelDownloaded(ModelSize.SMALL) -> ModelSize.SMALL
            availableRamMB >= 300 && isModelDownloaded(ModelSize.BASE) -> ModelSize.BASE
            isModelDownloaded(ModelSize.TINY) -> ModelSize.TINY
            else -> throw Exception("No Whisper model is downloaded or models are corrupted.")
        }
    }

    fun transcribeAudio(
        audioPath: String,
        modelSize: ModelSize,
        language: String = "auto",
        translate: Boolean = false
    ): Flow<WhisperProgress> = flow {
        if (!isWhisperInstalled()) {
            throw Exception("Whisper is not installed or lacks execute permissions.")
        }

        if (!isModelDownloaded(modelSize)) {
            throw Exception("Whisper model '${modelSize.displayName}' is not found or corrupted. Path: ${getModelPath(modelSize)}")
        }

        val modelPath = getModelPath(modelSize)
        val audioFile = File(audioPath)
        if (!audioFile.exists()) {
            throw Exception("Audio file does not exist: $audioPath")
        }

        val args = mutableListOf(
            whisperBinary.absolutePath,
            "-m", modelPath,
            "-f", audioPath,
            "-nt", // No timestamps in output
            "-t", "4" // Use 4 threads
        )

        if (language != "auto") {
            args.addAll(listOf("-l", language))
        }

        if (translate) {
            args.add("--translate")
        }

        try {
            val process = ProcessBuilder(args)
                .redirectErrorStream(true)
                .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var segmentCount = 0

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                
                // Parse progress from Whisper output
                val progressMatch = Regex("""progress\s*=\s*(\d+)%""").find(currentLine)
                if (progressMatch != null) {
                    val progress = progressMatch.groupValues[1].toIntOrNull() ?: 0
                    emit(WhisperProgress(progress, currentLine))
                }
                
                // Count segments for progress estimation
                if (currentLine.contains("[")) {
                    segmentCount++
                    emit(WhisperProgress(
                        progress = (segmentCount * 10).coerceAtMost(90),
                        message = currentLine,
                        currentSegment = currentLine
                    ))
                } else if (currentLine.contains("error", ignoreCase = true) || currentLine.contains("failed", ignoreCase = true)) {
                    emit(WhisperProgress(-1, "Whisper Error: $currentLine"))
                }
            }

            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw Exception("Transcription failed with exit code $exitCode")
            }

            emit(WhisperProgress(100, "Transcription completed"))
        } catch (e: IOException) {
            if (e.message?.contains("error=13") == true) {
                throw Exception("Permission denied (error=13). Whisper execution blocked by system.")
            } else {
                throw Exception("IO Error during transcription: ${e.message}")
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun transcribeToSRT(
        audioPath: String,
        outputPath: String,
        modelSize: ModelSize,
        language: String = "auto",
        translate: Boolean = false
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!isWhisperInstalled()) {
                return@withContext Result.failure(Exception("Whisper is not installed or lacks execute permissions."))
            }

            if (!isModelDownloaded(modelSize)) {
                return@withContext Result.failure(Exception("Whisper model '${modelSize.displayName}' is not found or corrupted. Path: ${getModelPath(modelSize)}"))
            }

            val modelPath = getModelPath(modelSize)
            val audioFile = File(audioPath)
            if (!audioFile.exists()) {
                return@withContext Result.failure(Exception("Audio file does not exist: $audioPath"))
            }

            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()

            val args = mutableListOf(
                whisperBinary.absolutePath,
                "-m", modelPath,
                "-f", audioPath,
                "-osrt", // Output SRT format
                "-of", outputPath.removeSuffix(".srt"), // Output file without extension
                "-t", "4" // Use 4 threads
            )

            if (language != "auto") {
                args.addAll(listOf("-l", language))
            }

            if (translate) {
                args.add("--translate")
            }

            val process = ProcessBuilder(args)
                .redirectErrorStream(true)
                .start()

            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                return@withContext Result.failure(Exception("Transcription failed (Exit Code: $exitCode): \n$output"))
            }

            val srtFile = File(outputPath)
            if (!srtFile.exists()) {
                return@withContext Result.failure(Exception("SRT file was not created. Output log: \n$output"))
            }

            Result.success(srtFile)
        } catch (e: IOException) {
            if (e.message?.contains("error=13") == true) {
                Result.failure(Exception("Permission denied (error=13). Whisper execution blocked by system."))
            } else {
                Result.failure(Exception("IO Error during transcription: ${e.message}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Transcription failed: ${e.message}"))
        }
    }

    suspend fun parseSRTFile(srtPath: String): Result<List<WhisperSegment>> = withContext(Dispatchers.IO) {
        try {
            val file = File(srtPath)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("SRT file does not exist at path: $srtPath"))
            }

            val lines = file.readLines()
            val segments = mutableListOf<WhisperSegment>()
            
            var i = 0
            while (i < lines.size) {
                // Skip sequence number
                if (lines[i].isBlank()) {
                    i++
                    continue
                }
                
                // Parse timestamp line
                if (i + 1 < lines.size && lines[i + 1].contains("-->")) {
                    val timestampLine = lines[i + 1]
                    val parts = timestampLine.split("-->").map { it.trim() }
                    
                    if (parts.size == 2) {
                        val startTime = parseSRTTimestamp(parts[0])
                        val endTime = parseSRTTimestamp(parts[1])
                        
                        // Collect text lines
                        val textLines = mutableListOf<String>()
                        var j = i + 2
                        while (j < lines.size && lines[j].isNotBlank()) {
                            textLines.add(lines[j])
                            j++
                        }
                        
                        val text = textLines.joinToString(" ")
                        segments.add(WhisperSegment(startTime, endTime, text))
                        
                        i = j
                    } else {
                        i++
                    }
                } else {
                    i++
                }
            }

            Result.success(segments)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse SRT: ${e.message}"))
        }
    }

    private fun parseSRTTimestamp(timestamp: String): Long {
        // Format: HH:MM:SS,mmm
        val parts = timestamp.replace(",", ":").split(":")
        if (parts.size != 4) return 0L
        
        val hours = parts[0].toLongOrNull() ?: 0L
        val minutes = parts[1].toLongOrNull() ?: 0L
        val seconds = parts[2].toLongOrNull() ?: 0L
        val milliseconds = parts[3].toLongOrNull() ?: 0L
        
        return hours * 3600000 + minutes * 60000 + seconds * 1000 + milliseconds
    }

    suspend fun getAvailableMemoryMB(): Long = withContext(Dispatchers.IO) {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            memInfo.availMem / (1024 * 1024) // Convert to MB
        } catch (e: Exception) {
            1024L // Default to 1GB if we can't get memory info
        }
    }
}
