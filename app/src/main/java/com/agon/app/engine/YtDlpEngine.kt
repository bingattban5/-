package com.agon.app.engine

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

// ==========================================
// ✅ Data Classes (التي كانت مفقودة)
// ==========================================

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

// ==========================================
// ✅ المحرك الرئيسي
// ==========================================

@Singleton
class YtDlpEngine @Inject constructor(
    private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("yt_dlp_prefs", Context.MODE_PRIVATE)
    }

    private val cookiesDir: File by lazy {
        File(context.filesDir, "yt_dlp_cookies").apply { mkdirs() }
    }

    companion object {
        private const val KEY_LAST_UPDATE_TIME = "last_yt_dlp_update_time"
        private const val KEY_CURRENT_VERSION = "current_yt_dlp_version"
        private const val UPDATE_INTERVAL_MS = 24 * 60 * 60 * 1000L
        private const val GITHUB_API_LATEST = "https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest"
    }

    fun isYtDlpInstalled(): Boolean = true

    suspend fun getVersion(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val version = YoutubeDL.getInstance().version(context)
            Result.success(version ?: "Unknown")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get version: ${e.message}"))
        }
    }

    private suspend fun fetchLatestReleaseInfo(): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(GITHUB_API_LATEST).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "VSub-App")
                connectTimeout = 15000
                readTimeout = 15000
            }

            if (connection.responseCode != 200) {
                return@withContext Result.failure(Exception("GitHub API returned ${connection.responseCode}"))
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonResponse = json.parseToJsonElement(responseText).jsonObject

            val tagName = jsonResponse["tag_name"]?.jsonPrimitive?.contentOrNull
                ?: return@withContext Result.failure(Exception("لم يتم العثور على tag_name"))

            val assets = jsonResponse["assets"]?.jsonArray
                ?: return@withContext Result.failure(Exception("لم يتم العثور على assets"))

            val ytDlpAsset = assets.firstOrNull { asset ->
                val name = asset.jsonObject["name"]?.jsonPrimitive?.contentOrNull ?: ""
                name == "yt-dlp"
            } ?: return@withContext Result.failure(Exception("لم يتم العثور على yt-dlp"))

            val downloadUrl = ytDlpAsset.jsonObject["browser_download_url"]?.jsonPrimitive?.contentOrNull
                ?: return@withContext Result.failure(Exception("لم يتم العثور على رابط التحميل"))

            Result.success(Pair(tagName, downloadUrl))
        } catch (e: Exception) {
            Result.failure(Exception("فشل جلب معلومات الإصدار: ${e.message}"))
        }
    }

    suspend fun updateYtDlpIfNeeded(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val lastUpdateTime = prefs.getLong(KEY_LAST_UPDATE_TIME, 0L)
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastUpdateTime < UPDATE_INTERVAL_MS) {
                return@withContext Result.success(false)
            }

            val (latestVersion, _) = fetchLatestReleaseInfo().getOrThrow()
            val currentVersion = prefs.getString(KEY_CURRENT_VERSION, null)
            
            if (currentVersion == latestVersion) {
                prefs.edit().putLong(KEY_LAST_UPDATE_TIME, currentTime).apply()
                return@withContext Result.success(false)
            }

            YoutubeDL.getInstance().updateYoutubeDL(context)

            prefs.edit()
                .putLong(KEY_LAST_UPDATE_TIME, currentTime)
                .putString(KEY_CURRENT_VERSION, latestVersion)
                .apply()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(Exception("فشل التحديث التلقائي: ${e.message}"))
        }
    }

    suspend fun installYtDlp(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val (latestVersion, _) = fetchLatestReleaseInfo().getOrThrow()
            YoutubeDL.getInstance().updateYoutubeDL(context)
            prefs.edit()
                .putLong(KEY_LAST_UPDATE_TIME, System.currentTimeMillis())
                .putString(KEY_CURRENT_VERSION, latestVersion)
                .apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("فشل تحديث yt-dlp: ${e.message}"))
        }
    }

    private fun extractDomain(url: String): String? {
        return try {
            val uri = URI(url)
            uri.host?.removePrefix("www.")
        } catch (e: Exception) {
            null
        }
    }

    private fun getCookieFileForDomain(domain: String?): File? {
        if (domain.isNullOrBlank()) return null
        val cookieFile = File(cookiesDir, "$domain.txt")
        return if (cookieFile.exists() && cookieFile.length() > 0) cookieFile else null
    }

    suspend fun analyzeUrl(url: String): Result<YtDlpVideoInfo> = withContext(Dispatchers.IO) {
        try {
            if (url.isBlank()) return@withContext Result.failure(Exception("URL is empty"))
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return@withContext Result.failure(Exception("Invalid URL"))
            }

            val request = YoutubeDLRequest(url).apply {
                addOption("--dump-json")
                addOption("--no-warnings")
                addOption("--no-playlist")
                getCookieFileForDomain(extractDomain(url))?.let {
                    addOption("--cookies=${it.absolutePath}")
                }
            }

            val response = YoutubeDL.getInstance().execute(request, null, null)
            val videoInfo = json.decodeFromString<YtDlpVideoInfo>(response.out)
            Result.success(videoInfo)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to analyze URL: ${e.message}"))
        }
    }

    fun downloadVideo(url: String, formatId: String, outputPath: String): Flow<DownloadProgress> = callbackFlow {
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()

        val request = YoutubeDLRequest(url).apply {
            addOption("-f", formatId)
            addOption("-o", outputPath)
            addOption("--no-warnings")
            getCookieFileForDomain(extractDomain(url))?.let {
                addOption("--cookies=${it.absolutePath}")
            }
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
                    close(Exception("Output file was not created"))
                }
                close()
            } catch (e: Exception) {
                trySend(DownloadProgress(-1, "Error: ${e.message}"))
                close(e)
            }
        }

        awaitClose {
            try { YoutubeDL.getInstance().destroyProcessById(processId) } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    suspend fun downloadSubtitles(
        url: String, language: String, outputPath: String, autoGenerated: Boolean = false
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()

            val request = YoutubeDLRequest(url).apply {
                addOption(if (autoGenerated) "--write-auto-subs" else "--write-subs")
                addOption("--sub-lang", language)
                addOption("--sub-format", "srt")
                addOption("--skip-download")
                addOption("-o", outputPath)
                getCookieFileForDomain(extractDomain(url))?.let {
                    addOption("--cookies=${it.absolutePath}")
                }
            }

            YoutubeDL.getInstance().execute(request, null, null)

            val srtFile = File(outputPath.replace("%(ext)s", "srt"))
            if (srtFile.exists()) Result.success(srtFile)
            else Result.failure(Exception("Subtitle file was not created"))
        } catch (e: Exception) {
            Result.failure(Exception("Subtitle download failed: ${e.message}"))
        }
    }

    suspend fun saveCookieFile(domainName: String, fileContent: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanDomain = domainName.removePrefix("www.").removePrefix("http://")
                .removePrefix("https://").split("/").first()
            val cookieFile = File(cookiesDir, "$cleanDomain.txt")
            cookieFile.writeText(fileContent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("فشل في حفظ ملف الـ Cookies: ${e.message}"))
        }
    }

    suspend fun getSavedCookieFiles(): List<Pair<String, File>> = withContext(Dispatchers.IO) {
        cookiesDir.listFiles { file -> file.extension == "txt" }
            ?.map { file -> Pair(file.nameWithoutExtension, file) }
            ?: emptyList()
    }

    suspend fun deleteCookieFile(domainName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cookieFile = File(cookiesDir, "$domainName.txt")
            if (cookieFile.exists()) { cookieFile.delete(); Result.success(Unit) }
            else Result.failure(Exception("الملف غير موجود"))
        } catch (e: Exception) {
            Result.failure(Exception("فشل في حذف الملف: ${e.message}"))
        }
    }
}
