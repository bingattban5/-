package com.agon.app.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
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

    fun isYtDlpInstalled(): Boolean {
        return true 
    }

    suspend fun installYtDlp(): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit)
    }

    suspend fun getVersion(): Result<String> = withContext(Dispatchers.IO) {
        try {
            // تم حل الخطأ الأول: تمرير الـ context لدالة version
            val version = YoutubeDL.getInstance().version(context)
            Result.success(version ?: "Unknown")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get version: ${e.message}"))
        }
    }

    suspend fun analyzeUrl(url: String): Result<YtDlpVideoInfo> = withContext(Dispatchers.IO) {
        try {
            if (url.isBlank()) {
                return@withContext Result.failure(Exception("URL is empty"))
            }

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return@withContext Result.failure(Exception("Invalid URL - must start with http:// or https://"))
            }

            val request = YoutubeDLRequest(url)
            request.addOption("--dump-json")
            request.addOption("--no-warnings")
            request.addOption("--no-playlist")

            // تمرير null للبارامترات الإضافية لتجنب أي تعارض
            val response = YoutubeDL.getInstance().execute(request, null, null)
            
            val videoInfo = json.decodeFromString<YtDlpVideoInfo>(response.out)
            Result.success(videoInfo)
            
        } catch (e: Exception) {
            Result.failure(Exception("Failed to analyze URL: ${e.message}"))
        }
    }

    // تم حل الخطأ الثاني: استخدام callbackFlow بدلاً من flow للسماح بإرسال البيانات من الـ Callback
    fun downloadVideo(
        url: String,
        formatId: String,
        outputPath: String
    ): Flow<DownloadProgress> = callbackFlow {
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()

        val request = YoutubeDLRequest(url)
        request.addOption("-f", formatId)
        request.addOption("-o", outputPath)
        request.addOption("--no-warnings")

        val processId = "Download_${System.currentTimeMillis()}"

        launch(Dispatchers.IO) {
            try {
                YoutubeDL.getInstance().execute(request, processId) { progress, _, line ->
                    // استخدام trySend بدلاً من emit داخل الـ Callback
                    trySend(DownloadProgress(progress.toInt(), line ?: "Downloading..."))
                }

                if (outputFile.exists()) {
                    trySend(DownloadProgress(100, "Download completed"))
                } else {
                    close(Exception("Output file was not created. Path: $outputPath"))
                }
                close()
            } catch (e: Exception) {
                trySend(DownloadProgress(-1, "Error: ${e.message}"))
                close(e)
            }
        }

        awaitClose {
            try {
                YoutubeDL.getInstance().destroyProcessById(processId)
            } catch (e: Exception) {
                // تجاهل أخطاء الإلغاء
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun downloadSubtitles(
        url: String,
        language: String,
        outputPath: String,
        autoGenerated: Boolean = false
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()

            val request = YoutubeDLRequest(url)
            request.addOption(if (autoGenerated) "--write-auto-subs" else "--write-subs")
            request.addOption("--sub-lang", language)
            request.addOption("--sub-format", "srt")
            request.addOption("--skip-download")
            request.addOption("-o", outputPath)

            YoutubeDL.getInstance().execute(request, null, null)

            val srtFile = File(outputPath.replace("%(ext)s", "srt"))
            if (srtFile.exists()) {
                Result.success(srtFile)
            } else {
                Result.failure(Exception("Subtitle file was not created."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Subtitle download failed: ${e.message}"))
        }
    }
}
