package com.agon.app.engine

import android.content.Context
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

data class FFmpegProgress(
    val progress: Int,
    val message: String,
    val currentTime: String = "",
    val totalTime: String = ""
)

@Singleton
class FFmpegEngine @Inject constructor(
    private val context: Context
) {
    // دالة ذكية للبحث عن مسار الأدوات سواء كانت مستخرجة من المكتبة أو مضمنة في النظام
    private fun getExecutable(name: String): File {
        // 1. البحث في مجلدات المكتبة الرسمية (مسار الاستخراج التلقائي)
        val libraryDir = File(context.noBackupFilesDir, "youtubedl-android")
        if (libraryDir.exists()) {
            val extractedFile = libraryDir.walkTopDown().firstOrNull { 
                it.name == name || it.name == "lib${name}.so" 
            }
            if (extractedFile != null && extractedFile.exists()) {
                return extractedFile
            }
        }

        // 2. المسار الاحتياطي القديم
        val nativeDir = context.applicationInfo.nativeLibraryDir
        return File(nativeDir, "lib${name}.so")
    }

    private val ffmpegBinary: File by lazy { getExecutable("ffmpeg") }
    private val ffprobeBinary: File by lazy { getExecutable("ffprobe") }

    fun isFFmpegInstalled(): Boolean {
        return ffmpegBinary.exists()
    }

    fun isFFprobeInstalled(): Boolean {
        return ffprobeBinary.exists()
    }

    suspend fun installFFmpeg(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (isFFmpegInstalled() && isFFprobeInstalled()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("CRITICAL: FFmpeg binaries not found. Make sure FFmpeg.getInstance().init(this) was called in Application class."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to verify FFmpeg installation: ${e.message}"))
        }
    }

    suspend fun getVersion(): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isFFmpegInstalled()) {
                return@withContext Result.failure(Exception("FFmpeg is not installed or missing from native libraries"))
            }

            val process = ProcessBuilder(ffmpegBinary.absolutePath, "-version")
                .redirectErrorStream(true)
                .start()

            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                val versionLine = output.lines().firstOrNull() ?: "Unknown version"
                Result.success(versionLine)
            } else {
                Result.failure(Exception("Failed to get version: exit code $exitCode"))
            }
        } catch (e: IOException) {
            if (e.message?.contains("error=13") == true || e.message?.contains("Permission denied") == true) {
                Result.failure(Exception("Permission denied (error=13). FFmpeg execution blocked by system."))
            } else {
                Result.failure(Exception("IO Error: ${e.message}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get version: ${e.message}"))
        }
    }

    suspend fun getMediaDuration(filePath: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            if (!isFFprobeInstalled()) {
                return@withContext Result.failure(Exception("FFprobe is not installed or missing from native libraries"))
            }

            val process = ProcessBuilder(
                ffprobeBinary.absolutePath,
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                filePath
            ).redirectErrorStream(true).start()

            val output = BufferedReader(InputStreamReader(process.inputStream)).readText().trim()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                val duration = output.toDoubleOrNull()?.toLong() ?: 0L
                Result.success(duration)
            } else {
                Result.failure(Exception("Failed to get duration: exit code $exitCode"))
            }
        } catch (e: IOException) {
            if (e.message?.contains("error=13") == true) {
                Result.failure(Exception("Permission denied (error=13) for FFprobe."))
            } else {
                Result.failure(Exception("IO Error getting duration: ${e.message}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get duration: ${e.message}"))
        }
    }

    fun mergeAudioVideo(
        videoPath: String,
        audioPath: String,
        outputPath: String
    ): Flow<FFmpegProgress> = flow {
        if (!isFFmpegInstalled()) throw Exception("FFmpeg is not installed or missing from native libraries")
        
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()
        val duration = getMediaDuration(videoPath).getOrNull() ?: 0L

        try {
            val process = ProcessBuilder(
                ffmpegBinary.absolutePath,
                "-i", videoPath,
                "-i", audioPath,
                "-c:v", "copy",
                "-c:a", "aac",
                "-shortest",
                "-y",
                outputPath
            ).redirectErrorStream(true).start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                
                val timeMatch = Regex("""time=(\d+):(\d+):(\d+\.\d+)""").find(currentLine)
                if (timeMatch != null && duration > 0) {
                    val hours = timeMatch.groupValues[1].toLong()
                    val minutes = timeMatch.groupValues[2].toLong()
                    val seconds = timeMatch.groupValues[3].toDouble()
                    val currentTime = hours * 3600 + minutes * 60 + seconds.toLong()
                    
                    val progress = ((currentTime.toFloat() / duration) * 100).toInt().coerceIn(0, 100)
                    emit(FFmpegProgress(progress, currentLine, currentTime.toString(), duration.toString()))
                }
            }

            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw Exception("Merge failed with exit code $exitCode")
            }
            if (!outputFile.exists()) {
                throw Exception("Output file was not created")
            }
            emit(FFmpegProgress(100, "Merge completed"))

        } catch (e: IOException) {
            if (e.message?.contains("error=13") == true) {
                throw Exception("Permission denied (error=13). FFmpeg execution blocked by system.")
            } else {
                throw Exception("IO Error during merge: ${e.message}")
            }
        }
    }.flowOn(Dispatchers.IO)

    fun extractAudio(
        videoPath: String,
        outputPath: String,
        format: String = "mp3"
    ): Flow<FFmpegProgress> = flow {
        if (!isFFmpegInstalled()) throw Exception("FFmpeg is not installed or missing from native libraries")

        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()
        val duration = getMediaDuration(videoPath).getOrNull() ?: 0L

        val codec = when (format.lowercase()) {
            "mp3" -> "libmp3lame"
            "aac" -> "aac"
            "wav" -> "pcm_s16le"
            "flac" -> "flac"
            else -> "libmp3lame"
        }

        try {
            val process = ProcessBuilder(
                ffmpegBinary.absolutePath,
                "-i", videoPath,
                "-vn",
                "-acodec", codec,
                "-y",
                outputPath
            ).redirectErrorStream(true).start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                
                val timeMatch = Regex("""time=(\d+):(\d+):(\d+\.\d+)""").find(currentLine)
                if (timeMatch != null && duration > 0) {
                    val hours = timeMatch.groupValues[1].toLong()
                    val minutes = timeMatch.groupValues[2].toLong()
                    val seconds = timeMatch.groupValues[3].toDouble()
                    val currentTime = hours * 3600 + minutes * 60 + seconds.toLong()
                    
                    val progress = ((currentTime.toFloat() / duration) * 100).toInt().coerceIn(0, 100)
                    emit(FFmpegProgress(progress, currentLine, currentTime.toString(), duration.toString()))
                }
            }

            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw Exception("Audio extraction failed with exit code $exitCode")
            }
            if (!outputFile.exists()) {
                throw Exception("Output file was not created")
            }
            emit(FFmpegProgress(100, "Audio extraction completed"))

        } catch (e: IOException) {
            if (e.message?.contains("error=13") == true) {
                throw Exception("Permission denied (error=13). FFmpeg execution blocked by system.")
            } else {
                throw Exception("IO Error during extraction: ${e.message}")
            }
        }
    }.flowOn(Dispatchers.IO)

    fun burnSubtitles(
        videoPath: String,
        subtitlePath: String,
        outputPath: String
    ): Flow<FFmpegProgress> = flow {
        if (!isFFmpegInstalled()) throw Exception("FFmpeg is not installed or missing from native libraries")

        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()
        val duration = getMediaDuration(videoPath).getOrNull() ?: 0L

        try {
            val process = ProcessBuilder(
                ffmpegBinary.absolutePath,
                "-i", videoPath,
                "-vf", "subtitles=$subtitlePath",
                "-c:a", "copy",
                "-y",
                outputPath
            ).redirectErrorStream(true).start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                
                val timeMatch = Regex("""time=(\d+):(\d+):(\d+\.\d+)""").find(currentLine)
                if (timeMatch != null && duration > 0) {
                    val hours = timeMatch.groupValues[1].toLong()
                    val minutes = timeMatch.groupValues[2].toLong()
                    val seconds = timeMatch.groupValues[3].toDouble()
                    val currentTime = hours * 3600 + minutes * 60 + seconds.toLong()
                    
                    val progress = ((currentTime.toFloat() / duration) * 100).toInt().coerceIn(0, 100)
                    emit(FFmpegProgress(progress, currentLine, currentTime.toString(), duration.toString()))
                }
            }

            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw Exception("Subtitle burning failed with exit code $exitCode")
            }
            if (!outputFile.exists()) {
                throw Exception("Output file was not created")
            }
            emit(FFmpegProgress(100, "Subtitle burning completed"))

        } catch (e: IOException) {
            if (e.message?.contains("error=13") == true) {
                throw Exception("Permission denied (error=13). FFmpeg execution blocked by system.")
            } else {
                throw Exception("IO Error during burning subtitles: ${e.message}")
            }
        }
    }.flowOn(Dispatchers.IO)

    fun attachSoftSubtitles(
        videoPath: String,
        subtitlePath: String,
        outputPath: String,
        language: String = "ara"
    ): Flow<FFmpegProgress> = flow {
        if (!isFFmpegInstalled()) throw Exception("FFmpeg is not installed or missing from native libraries")

        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()
        val duration = getMediaDuration(videoPath).getOrNull() ?: 0L

        try {
            val process = ProcessBuilder(
                ffmpegBinary.absolutePath,
                "-i", videoPath,
                "-i", subtitlePath,
                "-c:v", "copy",
                "-c:a", "copy",
                "-c:s", "mov_text",
                "-metadata:s:s:0", "language=$language",
                "-y",
                outputPath
            ).redirectErrorStream(true).start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                
                val timeMatch = Regex("""time=(\d+):(\d+):(\d+\.\d+)""").find(currentLine)
                if (timeMatch != null && duration > 0) {
                    val hours = timeMatch.groupValues[1].toLong()
                    val minutes = timeMatch.groupValues[2].toLong()
                    val seconds = timeMatch.groupValues[3].toDouble()
                    val currentTime = hours * 3600 + minutes * 60 + seconds.toLong()
                    
                    val progress = ((currentTime.toFloat() / duration) * 100).toInt().coerceIn(0, 100)
                    emit(FFmpegProgress(progress, currentLine, currentTime.toString(), duration.toString()))
                }
            }

            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw Exception("Subtitle attachment failed with exit code $exitCode")
            }
            if (!outputFile.exists()) {
                throw Exception("Output file was not created")
            }
            emit(FFmpegProgress(100, "Subtitle attachment completed"))

        } catch (e: IOException) {
            if (e.message?.contains("error=13") == true) {
                throw Exception("Permission denied (error=13). FFmpeg execution blocked by system.")
            } else {
                throw Exception("IO Error during attaching subtitles: ${e.message}")
            }
        }
    }.flowOn(Dispatchers.IO)

    fun trimMedia(
        inputPath: String,
        outputPath: String,
        startTime: String,
        duration: String
    ): Flow<FFmpegProgress> = flow {
        if (!isFFmpegInstalled()) throw Exception("FFmpeg is not installed or missing from native libraries")

        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()

        try {
            val process = ProcessBuilder(
                ffmpegBinary.absolutePath,
                "-i", inputPath,
                "-ss", startTime,
                "-t", duration,
                "-c", "copy",
                "-y",
                outputPath
            ).redirectErrorStream(true).start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                emit(FFmpegProgress(0, currentLine))
            }

            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw Exception("Trim failed with exit code $exitCode")
            }
            if (!outputFile.exists()) {
                throw Exception("Output file was not created")
            }
            emit(FFmpegProgress(100, "Trim completed"))

        } catch (e: IOException) {
            if (e.message?.contains("error=13") == true) {
                throw Exception("Permission denied (error=13). FFmpeg execution blocked by system.")
            } else {
                throw Exception("IO Error during trimming: ${e.message}")
            }
        }
    }.flowOn(Dispatchers.IO)

    fun compressVideo(
        inputPath: String,
        outputPath: String,
        crf: Int = 23,
        preset: String = "medium"
    ): Flow<FFmpegProgress> = flow {
        if (!isFFmpegInstalled()) throw Exception("FFmpeg is not installed or missing from native libraries")

        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()
        val duration = getMediaDuration(inputPath).getOrNull() ?: 0L

        try {
            val process = ProcessBuilder(
                ffmpegBinary.absolutePath,
                "-i", inputPath,
                "-c:v", "libx264",
                "-crf", crf.toString(),
                "-preset", preset,
                "-c:a", "aac",
                "-b:a", "128k",
                "-y",
                outputPath
            ).redirectErrorStream(true).start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                
                val timeMatch = Regex("""time=(\d+):(\d+):(\d+\.\d+)""").find(currentLine)
                if (timeMatch != null && duration > 0) {
                    val hours = timeMatch.groupValues[1].toLong()
                    val minutes = timeMatch.groupValues[2].toLong()
                    val seconds = timeMatch.groupValues[3].toDouble()
                    val currentTime = hours * 3600 + minutes * 60 + seconds.toLong()
                    
                    val progress = ((currentTime.toFloat() / duration) * 100).toInt().coerceIn(0, 100)
                    emit(FFmpegProgress(progress, currentLine, currentTime.toString(), duration.toString()))
                }
            }

            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw Exception("Compression failed with exit code $exitCode")
            }
            if (!outputFile.exists()) {
                throw Exception("Output file was not created")
            }
            emit(FFmpegProgress(100, "Compression completed"))

        } catch (e: IOException) {
            if (e.message?.contains("error=13") == true) {
                throw Exception("Permission denied (error=13). FFmpeg execution blocked by system.")
            } else {
                throw Exception("IO Error during compression: ${e.message}")
            }
        }
    }.flowOn(Dispatchers.IO)

    fun convertFormat(
        inputPath: String,
        outputPath: String,
        videoCodec: String = "copy",
        audioCodec: String = "copy"
    ): Flow<FFmpegProgress> = flow {
        if (!isFFmpegInstalled()) throw Exception("FFmpeg is not installed or missing from native libraries")

        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()
        val duration = getMediaDuration(inputPath).getOrNull() ?: 0L

        try {
            val process = ProcessBuilder(
                ffmpegBinary.absolutePath,
                "-i", inputPath,
                "-c:v", videoCodec,
                "-c:a", audioCodec,
                "-y",
                outputPath
            ).redirectErrorStream(true).start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                
                val timeMatch = Regex("""time=(\d+):(\d+):(\d+\.\d+)""").find(currentLine)
                if (timeMatch != null && duration > 0) {
                    val hours = timeMatch.groupValues[1].toLong()
                    val minutes = timeMatch.groupValues[2].toLong()
                    val seconds = timeMatch.groupValues[3].toDouble()
                    val currentTime = hours * 3600 + minutes * 60 + seconds.toLong()
                    
                    val progress = ((currentTime.toFloat() / duration) * 100).toInt().coerceIn(0, 100)
                    emit(FFmpegProgress(progress, currentLine, currentTime.toString(), duration.toString()))
                }
            }

            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw Exception("Conversion failed with exit code $exitCode")
            }
            if (!outputFile.exists()) {
                throw Exception("Output file was not created")
            }
            emit(FFmpegProgress(100, "Conversion completed"))

        } catch (e: IOException) {
            if (e.message?.contains("error=13") == true) {
                throw Exception("Permission denied (error=13). FFmpeg execution blocked by system.")
            } else {
                throw Exception("IO Error during conversion: ${e.message}")
            }
        }
    }.flowOn(Dispatchers.IO)
}
