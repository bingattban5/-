package com.agon.app.engine

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class YtDlpVideoInfo(
    val id: String = "",
    val title: String = "",
    val thumbnail: String? = null,
    val duration: Int? = null,
    val uploader: String = "",
    val webpage_url: String = "",
    val formats: List<YtDlpFormat> = emptyList(),
    val subtitles: Map<String, List<YtDlpSubtitle>> = emptyMap(),
    val automatic_captions: Map<String, List<YtDlpSubtitle>> = emptyMap()
)

@Serializable
data class YtDlpFormat(
    val format_id: String = "",
    val ext: String = "",
    val height: Int? = null,
    val width: Int? = null,
    val filesize: Long? = null,
    val filesize_approx: Long? = null,
    val format_note: String? = null,
    val vcodec: String? = null,
    val acodec: String? = null,
    val resolution: String? = null
)

@Serializable
data class YtDlpSubtitle(
    val ext: String = "",
    val url: String = "",
    val name: String? = null
)

data class DownloadProgress(
    val progress: Int,
    val message: String
)

@Singleton
class YtDlpEngine @Inject constructor(
    private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val ytDlpBinary: File by lazy {
        val filesDir = context.filesDir
        File(filesDir, "yt-dlp")
    }

    data class CpuArchitecture(
        val abi: String,
        val isSupported: Boolean,
        val displayName: String
    )

    fun detectCpuArchitecture(): CpuArchitecture {
        val supportedAbis = Build.SUPPORTED_ABIS
        val primaryAbi = supportedAbis.firstOrNull() ?: "unknown"
        val isSupported = primaryAbi in listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        val displayName = when (primaryAbi) {
            "arm64-v8a" -> "ARM 64-bit (arm64-v8a)"
            "armeabi-v7a" -> "ARM 32-bit (armeabi-v7a)"
            "x86_64" -> "x86 64-bit"
            "x86" -> "x86 32-bit"
            else -> "Unknown ($primaryAbi)"
        }
        return CpuArchitecture(
            abi = primaryAbi,
            isSupported = isSupported,
            displayName = displayName
        )
    }

    fun isYtDlpInstalled(): Boolean {
        return ytDlpBinary.exists() && ytDlpBinary.canExecute()
    }

    suspend fun installYtDlp(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val arch = detectCpuArchitecture()
            val binaryName = when (arch.abi) {
                "arm64-v8a", "armeabi-v7a", "x86_64", "x86" -> "yt-dlp" // Assuming unified binary or handled externally
                else -> return@withContext Result.failure(Exception("Unsupported architecture: ${arch.abi}"))
            }

            val assetManager = context.assets
            val assetsList = assetManager.list("") ?: emptyArray()

            // 1. التحقق الاستباقي من وجود الملف في assets
            if (!assetsList.contains(binaryName)) {
                return@withContext Result.failure(Exception("CRITICAL: '$binaryName' not found in assets/. Please place it in app/src/main/assets/"))
            }

            val inputStream = assetManager.open(binaryName)
            val outputFile = ytDlpBinary
            
            outputFile.parentFile?.mkdirs()

            inputStream.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // 2. التحقق الفعلي من منح الصلاحيات
            val isExecutable = outputFile.setExecutable(true)
            if (!isExecutable) {
                return@withContext Result.failure(Exception("CRITICAL: Failed to grant execute permission (setExecutable=false) to yt-dlp."))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to install yt-dlp: ${e.javaClass.simpleName} - ${e.message}"))
        }
    }

    suspend fun getVersion(): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isYtDlpInstalled()) {
                return@withContext Result.failure(Exception("yt-dlp is not installed or not executable"))
            }

            val process = ProcessBuilder(ytDlpBinary.absolutePath, "--version")
                .redirectErrorStream(true)
                .start()

            val output = BufferedReader(InputStreamReader(process.inputStream)).readText().trim()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Result.success(output)
            } else {
                Result.failure(Exception("Failed to get version: exit code $exitCode. Output: $output"))
            }
        } catch (e: IOException) {
            if (e.message?.contains("error=13") == true || e.message?.contains("Permission denied") == true) {
                Result.failure(Exception("Permission denied (error=13). The system blocked yt-dlp execution."))
            } else {
                Result.failure(Exception("IO Error: ${e.message}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get version: ${e.message}"))
        }
    }

    suspend fun analyzeUrl(url: String): Result<YtDlpVideoInfo> = withContext(Dispatchers.IO) {
        try {
            if (!isYtDlpInstalled()) {
                return@withContext Result.failure(Exception("yt-dlp is not installed or not executable"))
            }

            if (url.isBlank()) {
                return@withContext Result.failure(Exception("URL is empty"))
            }

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return@withContext Result.failure(Exception("Invalid URL - must start with http:// or https://"))
            }

            val process = ProcessBuilder(
                ytDlpBinary.absolutePath,
                "--dump-json",
                "--no-warnings",
                "--no-playlist",
                url
            ).redirectErrorStream(true).start()

            val output = StringBuilder()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }

            val exitCode = process.waitFor()

            if (exitCode != 0) {
                return@withContext Result.failure(Exception("yt-dlp failed (exit code $exitCode): ${output.toString().trim()}"))
            }

            val jsonOutput = output.toString()
            val videoInfo = json.decodeFromString<YtDlpVideoInfo>(jsonOutput)

            Result.success(videoInfo)
        } catch (e: IOException) {
            if (e.message?.contains("error=13") == true) {
                Result.failure(Exception("Execution blocked (error=13). Try reinstalling yt-dlp."))
            } else {
                Result.failure(Exception("Failed to analyze URL (IO): ${e.message}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to analyze URL: ${e.message}"))
        }
    }

    fun downloadVideo(
        url: String,
        formatId: String,
        outputPath: String
    ): Flow<DownloadProgress> = flow {
        if (!isYtDlpInstalled()) {
            throw Exception("yt-dlp is not installed or not executable")
        }

        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()

        val process = ProcessBuilder(
            ytDlpBinary.absolutePath,
            "-f", formatId,
            "-o", outputPath,
            "--newline",
            "--no-warnings",
            url
        ).redirectErrorStream(true).start()

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            val currentLine = line ?: continue
            val progressMatch = Regex("""\[download\]\s+(\d+\.?\d*)%""").find(currentLine)
            if (progressMatch != null) {
                val progress = progressMatch.groupValues[1].toFloatOrNull()?.toInt() ?: 0
                emit(DownloadProgress(progress, currentLine))
            } else if (currentLine.contains("ERROR:") || currentLine.contains("error")) {
                // التقاط أخطاء yt-dlp وإرسالها
                emit(DownloadProgress(-1, currentLine)) 
            }
        }

        val exitCode = process.waitFor()

        if (exitCode != 0) {
            throw Exception("Download failed with exit code $exitCode")
        }

        if (!outputFile.exists()) {
            throw Exception("Output file was not created. Path: $outputPath")
        }

        emit(DownloadProgress(100, "Download completed"))
    }.flowOn(Dispatchers.IO)

    suspend fun downloadSubtitles(
        url: String,
        language: String,
        outputPath: String,
        autoGenerated: Boolean = false
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!isYtDlpInstalled()) {
                return@withContext Result.failure(Exception("yt-dlp is not installed"))
            }

            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()

            val subtitleFlag = if (autoGenerated) "--write-auto-subs" else "--write-subs"
            val process = ProcessBuilder(
                ytDlpBinary.absolutePath,
                subtitleFlag,
                "--sub-lang", language,
                "--sub-format", "srt",
                "--skip-download",
                "-o", outputPath,
                url
            ).redirectErrorStream(true).start()

            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                return@withContext Result.failure(Exception("Subtitle download failed: $output"))
            }

            val srtFile = File(outputPath.replace("%(ext)s", "srt"))
            if (!srtFile.exists()) {
                return@withContext Result.failure(Exception("Subtitle file was not created. Output logs: $output"))
            }

            Result.success(srtFile)
        } catch (e: Exception) {
            Result.failure(Exception("Subtitle download failed: ${e.message}"))
        }
    }
}
