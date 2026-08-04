package com.agon.app.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.agon.app.data.DownloadMode
import com.agon.app.data.DownloadRepository
import com.agon.app.data.DownloadStatus
import com.agon.app.data.SubtitleMethod
import com.agon.app.engine.ArgosTranslateEngine
import com.agon.app.engine.FFmpegEngine
import com.agon.app.engine.WhisperEngine
import com.agon.app.engine.YtDlpEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

// Typealias to avoid conflict with androidx.work.ListenableWorker.Result
private typealias KResult<T> = kotlin.Result<T>

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val ytDlpEngine: YtDlpEngine,
    private val downloadRepository: DownloadRepository,
    private val argosTranslateEngine: ArgosTranslateEngine,
    private val ffmpegEngine: FFmpegEngine,
    private val whisperEngine: WhisperEngine
) : CoroutineWorker(context, workerParams) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationId = inputData.getInt(KEY_NOTIFICATION_ID, 1)
    private val downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: ""
    
    private var startTime = System.currentTimeMillis()

    override suspend fun doWork(): Result {
        createNotificationChannel()

        val url = inputData.getString(KEY_URL) ?: return failureResult("URL is missing")
        val formatId = inputData.getString(KEY_FORMAT_ID) ?: return failureResult("Format ID is missing")
        val outputPath = inputData.getString(KEY_OUTPUT_PATH) ?: return failureResult("Output path is missing")
        val downloadModeStr = inputData.getString(KEY_DOWNLOAD_MODE) ?: return failureResult("Download mode is missing")
        val subtitleMethodStr = inputData.getString(KEY_SUBTITLE_METHOD) ?: "NONE"
        val subtitleLang = inputData.getString(KEY_SUBTITLE_LANG) ?: ""

        val downloadMode = try {
            DownloadMode.valueOf(downloadModeStr)
        } catch (e: Exception) {
            return failureResult("Invalid download mode")
        }

        val subtitleMethod = try {
            SubtitleMethod.valueOf(subtitleMethodStr)
        } catch (e: Exception) {
            SubtitleMethod.NONE
        }

        return try {
            when (downloadMode) {
                DownloadMode.VIDEO_AND_SUBTITLE -> downloadVideoAndSubtitle(
                    url, formatId, outputPath, subtitleMethod, subtitleLang
                )
                DownloadMode.VIDEO_ONLY -> downloadVideoOnly(url, formatId, outputPath)
                DownloadMode.SUBTITLE_ONLY -> downloadSubtitleOnly(url, outputPath, subtitleMethod, subtitleLang)
            }
        } catch (e: Exception) {
            if (shouldRetry(e)) {
                retryResult("انقطع الاتصال، جاري إعادة المحاولة...")
            } else {
                failureResult("Download failed: ${e.message}")
            }
        }
    }

    private suspend fun downloadVideoOnly(
        url: String,
        formatId: String,
        outputPath: String
    ): Result {
        updateStatus(DownloadStatus.DOWNLOADING, 0, "0 B", "", "")

        var errorMessage: String? = null

        try {
            ytDlpEngine.downloadVideo(url, formatId, outputPath)
                .catch { e ->
                    errorMessage = e.message
                }
                .collect { progress ->
                    if (progress.progress == -1) {
                        errorMessage = progress.message
                    } else {
                        val stats = calculateStats(progress.progress)
                        updateStatus(
                            DownloadStatus.DOWNLOADING,
                            progress.progress,
                            stats.downloadedSize,
                            stats.speed,
                            stats.eta
                        )
                        updateNotification(progress.progress, "Downloading video: ${progress.progress}%")
                    }
                }

            if (errorMessage != null) {
                val exception = Exception(errorMessage)
                return if (shouldRetry(exception)) {
                    retryResult(handleYtDlpError(exception))
                } else {
                    failureResult(handleYtDlpError(exception))
                }
            }

            val outputFile = File(outputPath)
            if (!outputFile.exists()) {
                return failureResult("Output file was not created. Path: $outputPath")
            }

            updateStatus(DownloadStatus.COMPLETED, 100, outputFile.length().toString(), "", "")
            showFinalNotification("اكتمل التحميل بنجاح")

            return Result.success(
                Data.Builder()
                    .putString(KEY_OUTPUT_PATH, outputFile.absolutePath)
                    .build()
            )
        } catch (e: Exception) {
            return if (shouldRetry(e)) retryResult(handleYtDlpError(e)) else failureResult(handleYtDlpError(e))
        }
    }

    private suspend fun downloadVideoAndSubtitle(
        url: String,
        formatId: String,
        outputPath: String,
        subtitleMethod: SubtitleMethod,
        subtitleLang: String
    ): Result {
        updateStatus(DownloadStatus.DOWNLOADING, 0, "0 B", "", "")
        updateNotification(0, "Downloading video...")

        var errorMessage: String? = null

        try {
            ytDlpEngine.downloadVideo(url, formatId, outputPath)
                .catch { e ->
                    errorMessage = e.message
                }
                .collect { progress ->
                    if (progress.progress == -1) {
                        errorMessage = progress.message
                    } else {
                        val stats = calculateStats(progress.progress)
                        updateStatus(
                            DownloadStatus.DOWNLOADING,
                            progress.progress,
                            stats.downloadedSize,
                            stats.speed,
                            stats.eta
                        )
                        updateNotification(progress.progress, "Downloading video: ${progress.progress}%")
                    }
                }

            if (errorMessage != null) {
                val exception = Exception(errorMessage)
                return if (shouldRetry(exception)) {
                    retryResult(handleYtDlpError(exception))
                } else {
                    failureResult(handleYtDlpError(exception))
                }
            }

            val videoFile = File(outputPath)
            if (!videoFile.exists()) {
                return failureResult("Video file was not created. Path: $outputPath")
            }

            updateStatus(DownloadStatus.EXTRACTING_SUBS, 100, "", "", "")
            updateNotification(100, "Extracting subtitles...")

            val subtitlePath = outputPath.replaceAfterLast('.', "srt")
            val subtitleResult = when (subtitleMethod) {
                SubtitleMethod.DIRECT_AR -> {
                    ytDlpEngine.downloadSubtitles(url, "ar", subtitlePath, autoGenerated = false)
                }
                SubtitleMethod.TRANSLATED_FROM_OTHER -> {
                    val tempPath = outputPath.replaceAfterLast('.', "temp.srt")
                    val downloadResult = ytDlpEngine.downloadSubtitles(url, subtitleLang, tempPath, autoGenerated = false)
                    
                    if (downloadResult.isSuccess) {
                        val tempFile = downloadResult.getOrNull()!!
                        updateStatus(DownloadStatus.TRANSLATING, 100, "", "", "")
                        updateNotification(100, "Translating to Arabic...")
                        argosTranslateEngine.translateSrtFile(
                            tempFile.absolutePath,
                            subtitlePath,
                            subtitleLang,
                            "ar"
                        )
                    } else {
                        downloadResult
                    }
                }
                SubtitleMethod.WHISPER_GENERATED -> {
                    val audioPath = outputPath.replaceAfterLast('.', "wav")
                    updateStatus(DownloadStatus.EXTRACTING_SUBS, 100, "", "", "")
                    updateNotification(100, "Extracting audio for Whisper...")
                    
                    var extractError: String? = null
                    try {
                        ffmpegEngine.extractAudio(videoFile.absolutePath, audioPath, "wav")
                            .catch { e -> extractError = e.message }
                            .collect { progress ->
                                updateNotification(progress.progress, "Extracting audio: ${progress.progress}%")
                            }
                        
                        if (extractError != null) {
                            KResult.failure<File>(Exception("Audio extraction failed: $extractError"))
                        } else {
                            val availableRam = whisperEngine.getAvailableMemoryMB()
                            val model = whisperEngine.selectBestModel(availableRam)
                            
                            updateStatus(DownloadStatus.TRANSLATING, 100, "", "", "")
                            updateNotification(100, "Transcribing with Whisper...")
                            
                            whisperEngine.transcribeToSRT(audioPath, subtitlePath, model, "auto", false)
                        }
                    } catch (e: Exception) {
                        KResult.failure<File>(e)
                    }
                }
                SubtitleMethod.NONE -> {
                    KResult.success(File(subtitlePath))
                }
            }

            return if (subtitleResult.isSuccess) {
                val subtitleFile = subtitleResult.getOrNull()!!
                updateStatus(DownloadStatus.COMPLETED, 100, "", "", "")
                showFinalNotification("اكتمل تحميل الفيديو والترجمة")
                Result.success(
                    Data.Builder()
                        .putString(KEY_OUTPUT_PATH, videoFile.absolutePath)
                        .putString(KEY_SUBTITLE_PATH, subtitleFile.absolutePath)
                        .build()
                )
            } else {
                val error = subtitleResult.exceptionOrNull()
                updateStatus(DownloadStatus.COMPLETED, 100, "", "", "")
                showFinalNotification("تم تحميل الفيديو (تعذرت الترجمة)")
                Result.success(
                    Data.Builder()
                        .putString(KEY_OUTPUT_PATH, videoFile.absolutePath)
                        .putString(KEY_SUBTITLE_ERROR, error?.message ?: "Subtitle download failed")
                        .build()
                )
            }
        } catch (e: Exception) {
            return if (shouldRetry(e)) retryResult(handleYtDlpError(e)) else failureResult(handleYtDlpError(e))
        }
    }

    private suspend fun downloadSubtitleOnly(
        url: String,
        outputPath: String,
        subtitleMethod: SubtitleMethod,
        subtitleLang: String
    ): Result {
        updateStatus(DownloadStatus.EXTRACTING_SUBS, 0, "", "", "")
        updateNotification(0, "Downloading subtitle...")

        val subtitlePath = outputPath.replaceAfterLast('.', "srt")
        
        val subtitleResult = when (subtitleMethod) {
            SubtitleMethod.DIRECT_AR -> {
                ytDlpEngine.downloadSubtitles(url, "ar", subtitlePath, autoGenerated = false)
            }
            SubtitleMethod.TRANSLATED_FROM_OTHER -> {
                val tempPath = outputPath.replaceAfterLast('.', "temp.srt")
                val downloadResult = ytDlpEngine.downloadSubtitles(url, subtitleLang, tempPath, autoGenerated = false)
                
                if (downloadResult.isSuccess) {
                    val tempFile = downloadResult.getOrNull()!!
                    updateStatus(DownloadStatus.TRANSLATING, 50, "", "", "")
                    updateNotification(50, "Translating to Arabic...")
                    argosTranslateEngine.translateSrtFile(
                        tempFile.absolutePath,
                        subtitlePath,
                        subtitleLang,
                        "ar"
                    )
                } else {
                    downloadResult
                }
            }
            SubtitleMethod.WHISPER_GENERATED -> {
                val tempVideoPath = outputPath.replaceAfterLast('.', "temp.mp4")
                updateStatus(DownloadStatus.DOWNLOADING, 0, "", "", "")
                updateNotification(0, "Downloading video for audio extraction...")
                
                var downloadError: String? = null
                try {
                    ytDlpEngine.downloadVideo(url, "bestaudio", tempVideoPath)
                        .catch { e -> downloadError = e.message }
                        .collect { progress ->
                            updateNotification(progress.progress, "Downloading: ${progress.progress}%")
                        }
                    
                    if (downloadError != null) {
                        KResult.failure<File>(Exception("Download failed: $downloadError"))
                    } else {
                        val audioPath = outputPath.replaceAfterLast('.', "wav")
                        updateStatus(DownloadStatus.EXTRACTING_SUBS, 100, "", "", "")
                        updateNotification(100, "Extracting audio...")
                        
                        var extractError: String? = null
                        ffmpegEngine.extractAudio(tempVideoPath, audioPath, "wav")
                            .catch { e -> extractError = e.message }
                            .collect { progress ->
                                updateNotification(progress.progress, "Extracting audio: ${progress.progress}%")
                            }
                        
                        if (extractError != null) {
                            KResult.failure<File>(Exception("Audio extraction failed: $extractError"))
                        } else {
                            val availableRam = whisperEngine.getAvailableMemoryMB()
                            val model = whisperEngine.selectBestModel(availableRam)
                            
                            updateStatus(DownloadStatus.TRANSLATING, 100, "", "", "")
                            updateNotification(100, "Transcribing with Whisper...")
                            
                            val result = whisperEngine.transcribeToSRT(audioPath, subtitlePath, model, "auto", false)
                            
                            File(tempVideoPath).delete()
                            File(audioPath).delete()
                            
                            result
                        }
                    }
                } catch (e: Exception) {
                    KResult.failure<File>(e)
                }
            }
            SubtitleMethod.NONE -> {
                KResult.failure(Exception("No subtitle method specified"))
            }
        }

        return if (subtitleResult.isSuccess) {
            val subtitleFile = subtitleResult.getOrNull()!!
            updateStatus(DownloadStatus.COMPLETED, 100, "", "", "")
            showFinalNotification("اكتمل تحميل الترجمة")
            Result.success(
                Data.Builder()
                    .putString(KEY_SUBTITLE_PATH, subtitleFile.absolutePath)
                    .build()
            )
        } else {
            val error = subtitleResult.exceptionOrNull()
            val exception = error ?: Exception("Unknown error")
            if (shouldRetry(exception)) retryResult(handleYtDlpError(exception)) else failureResult(handleYtDlpError(exception))
        }
    }

    private fun shouldRetry(error: Throwable): Boolean {
        val message = error.message ?: return false
        if (message.contains("error=13") || message.contains("Permission denied", true)) return false
        if (message.contains("not found", true) || message.contains("not installed", true)) return false
        
        return error is IOException || 
               message.contains("network", true) || 
               message.contains("connection", true) ||
               message.contains("timeout", true)
    }

    private suspend fun retryResult(errorMsg: String): Result {
        updateNotification(0, "Retrying: $errorMsg")
        return Result.retry()
    }

    private fun calculateStats(progress: Int): DownloadStats {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = (currentTime - startTime) / 1000.0
        val estimatedTotalBytes = 100_000_000L
        val downloadedBytes = (estimatedTotalBytes * progress / 100)
        
        val speed = if (elapsedTime > 0) downloadedBytes / elapsedTime else 0.0
        val remainingBytes = estimatedTotalBytes - downloadedBytes
        val eta = if (speed > 0) (remainingBytes / speed).toLong() else 0L
        
        return DownloadStats(
            downloadedSize = formatBytes(downloadedBytes),
            speed = formatSpeed(speed.toLong()),
            eta = formatEta(eta)
        )
    }

    private data class DownloadStats(
        val downloadedSize: String,
        val speed: String,
        val eta: String
    )

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    private fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond >= 1_048_576 -> String.format("%.1f MB/s", bytesPerSecond / 1_048_576.0)
            bytesPerSecond >= 1024 -> String.format("%.1f KB/s", bytesPerSecond / 1024.0)
            else -> "$bytesPerSecond B/s"
        }
    }

    private fun formatEta(seconds: Long): String {
        return when {
            seconds >= 3600 -> String.format("%dh %dm", seconds / 3600, (seconds % 3600) / 60)
            seconds >= 60 -> String.format("%dm %ds", seconds / 60, (seconds % 60))
            else -> "${seconds}s"
        }
    }

    private fun handleYtDlpError(error: Throwable): String {
        val message = error.message ?: "Unknown error"
        return when {
            message.contains("error=13") || message.contains("Permission denied") ->
                "خطأ في الصلاحيات (Error=13). الملف التنفيذي غير مدعوم أو النظام يمنعه."
            message.contains("403") || message.contains("Forbidden") ->
                "يوتيوب يحجب النسخة الحالية من المحرك (403). جرّب تحديث المحرك من شاشة النماذج أو أعد المحاولة لاحقاً."
            message.contains("CRITICAL") -> 
                message
            message.contains("Private video", ignoreCase = true) ->
                "هذا فيديو خاص ولا يمكن الوصول إليه."
            message.contains("Video unavailable", ignoreCase = true) ->
                "الفيديو غير متاح أو تم حذفه."
            message.contains("Geo-restricted", ignoreCase = true) ->
                "الفيديو محجوب جغرافياً في منطقتك."
            message.contains("Sign in", ignoreCase = true) || message.contains("login", ignoreCase = true) ->
                "يتطلب هذا الفيديو تسجيل الدخول."
            message.contains("network", ignoreCase = true) || message.contains("connection", ignoreCase = true) ->
                "خطأ في الاتصال بالشبكة. تحقق من اتصالك بالإنترنت."
            message.contains("URL", ignoreCase = true) ->
                "رابط غير صالح أو غير مدعوم."
            else -> "فشل التحميل: $message"
        }
    }

    private suspend fun updateStatus(
        status: DownloadStatus,
        progress: Int,
        downloadedSize: String,
        speed: String,
        eta: String
    ) {
        downloadRepository.updateProgress(downloadId, status, progress, downloadedSize, speed, eta)
    }

    // عند الفشل: سجّل الخطأ وألغِ الإشعار العالق نهائياً
    private suspend fun failureResult(error: String): Result {
        downloadRepository.updateError(downloadId, DownloadStatus.FAILED, error)
        notificationManager.cancel(notificationId)
        return Result.failure(
            Data.Builder()
                .putString(KEY_ERROR, error)
                .build()
        )
    }

    // إشعار نهائي قابل للمسح (غير Ongoing) عند النجاح
    private fun showFinalNotification(text: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("تحميل الفيديو و الترجمة")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId, notification)
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(notificationId, createNotification(0, "Starting download..."))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Download progress notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(progress: Int, text: String) {
        val notification = createNotification(progress, text)
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotification(progress: Int, text: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("تحميل الفيديو و الترجمة")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val KEY_URL = "url"
        const val KEY_FORMAT_ID = "format_id"
        const val KEY_OUTPUT_PATH = "output_path"
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_NOTIFICATION_ID = "notification_id"
        const val KEY_DOWNLOAD_MODE = "download_mode"
        const val KEY_SUBTITLE_METHOD = "subtitle_method"
        const val KEY_SUBTITLE_LANG = "subtitle_lang"
        const val KEY_SUBTITLE_PATH = "subtitle_path"
        const val KEY_SUBTITLE_ERROR = "subtitle_error"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
        const val CHANNEL_ID = "download_channel"
    }
}
