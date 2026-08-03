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
import java.net.URI
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

    // مجلد مخصص لحفظ ملفات الـ Cookies
    private val cookiesDir: File by lazy {
        File(context.filesDir, "yt_dlp_cookies").apply { mkdirs() }
    }

    fun isYtDlpInstalled(): Boolean {
        return true 
    }

    suspend fun installYtDlp(): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit)
    }

    suspend fun getVersion(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val version = YoutubeDL.getInstance().version(context)
            Result.success(version ?: "Unknown")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get version: ${e.message}"))
        }
    }

    /**
     * استخراج اسم النطاق (Domain) من الرابط للبحث عن ملف الـ Cookies المناسب
     * مثال: https://www.pornhub.com/view_video.php -> pornhub.com
     */
    private fun extractDomain(url: String): String? {
        return try {
            val uri = URI(url)
            uri.host?.removePrefix("www.")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * البحث عن ملف Cookies مطابق للنطاق
     * يتوقع أن يكون اسم الملف مثل: pornhub.com.txt
     */
    private fun getCookieFileForDomain(domain: String?): File? {
        if (domain.isNullOrBlank()) return null
        
        val cookieFile = File(cookiesDir, "$domain.txt")
        return if (cookieFile.exists() && cookieFile.length() > 0) cookieFile else null
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
            
            // إضافة دعم الـ Cookies الديناميكي
            val domain = extractDomain(url)
            val cookieFile = getCookieFileForDomain(domain)
            if (cookieFile != null) {
                request.addOption("--cookies=${cookieFile.absolutePath}")
            }

            val response = YoutubeDL.getInstance().execute(request, null, null)
            
            val videoInfo = json.decodeFromString<YtDlpVideoInfo>(response.out)
            Result.success(videoInfo)
            
        } catch (e: Exception) {
            Result.failure(Exception("Failed to analyze URL: ${e.message}"))
        }
    }

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
        
        // إضافة دعم الـ Cookies الديناميكي
        val domain = extractDomain(url)
        val cookieFile = getCookieFileForDomain(domain)
        if (cookieFile != null) {
            request.addOption("--cookies=${cookieFile.absolutePath}")
        }

        val processId = "Download_${System.currentTimeMillis()}"

        launch(Dispatchers.IO) {
            try {
                YoutubeDL.getInstance().execute(request, processId) { progress, _, line ->
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
            
            // إضافة دعم الـ Cookies الديناميكي
            val domain = extractDomain(url)
            val cookieFile = getCookieFileForDomain(domain)
            if (cookieFile != null) {
                request.addOption("--cookies=${cookieFile.absolutePath}")
            }

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
    
    // ==========================================
    // دوال إدارة ملفات الـ Cookies (للاستخدام من الـ ViewModel)
    // ==========================================
    
    /**
     * حفظ ملف Cookies جديد
     * @param domainName اسم النطاق (مثال: pornhub.com)
     * @param fileContent محتوى ملف الـ Cookies بصيغة Netscape
     */
    suspend fun saveCookieFile(domainName: String, fileContent: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanDomain = domainName.removePrefix("www.").removePrefix("http://").removePrefix("https://").split("/").first()
            val cookieFile = File(cookiesDir, "$cleanDomain.txt")
            cookieFile.writeText(fileContent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("فشل في حفظ ملف الـ Cookies: ${e.message}"))
        }
    }

    /**
     * الحصول على قائمة ملفات الـ Cookies المحفوظة
     */
    suspend fun getSavedCookieFiles(): List<Pair<String, File>> = withContext(Dispatchers.IO) {
        cookiesDir.listFiles { file -> file.extension == "txt" }
            ?.map { file -> Pair(file.nameWithoutExtension, file) }
            ?: emptyList()
    }

    /**
     * حذف ملف Cookies محدد
     */
    suspend fun deleteCookieFile(domainName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cookieFile = File(cookiesDir, "$domainName.txt")
            if (cookieFile.exists()) {
                cookieFile.delete()
                Result.success(Unit)
            } else {
                Result.failure(Exception("الملف غير موجود"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في حذف الملف: ${e.message}"))
        }
    }
}
