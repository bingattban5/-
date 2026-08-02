package com.agon.app.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class MediaFile(
    val path: String,
    val name: String,
    val extension: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val lastModified: Long,
    val isVideo: Boolean,
    val isSubtitle: Boolean,
    val isAudio: Boolean,
    val checksum: String = ""
)

data class DuplicateGroup(
    val checksum: String,
    val files: List<MediaFile>
)

@Singleton
class FileManager @Inject constructor(
    private val context: Context
) {
    private val downloadDir: File by lazy {
        File(
            android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            ),
            "SubVIDD"
        ).apply { mkdirs() }
    }

    private val tempDir: File by lazy {
        File(context.cacheDir, "subvidd_temp").apply { mkdirs() }
    }

    private val videoExtensions = setOf("mp4", "mkv", "avi", "webm", "mov", "flv", "wmv", "m4v", "3gp")
    private val subtitleExtensions = setOf("srt", "vtt", "ass", "ssa", "sub")
    private val audioExtensions = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus")

    suspend fun listFiles(): List<MediaFile> = withContext(Dispatchers.IO) {
        if (!downloadDir.exists()) return@withContext emptyList()

        downloadDir.listFiles()
            ?.filter { it.isFile }
            ?.map { it.toMediaFile() }
            ?.sortedByDescending { it.lastModified }
            ?: emptyList()
    }

    suspend fun listVideoFiles(): List<MediaFile> = withContext(Dispatchers.IO) {
        listFiles().filter { it.isVideo }
    }

    suspend fun listSubtitleFiles(): List<MediaFile> = withContext(Dispatchers.IO) {
        listFiles().filter { it.isSubtitle }
    }

    suspend fun listAudioFiles(): List<MediaFile> = withContext(Dispatchers.IO) {
        listFiles().filter { it.isAudio }
    }

    suspend fun renameFile(filePath: String, newName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("File not found"))
            }

            val extension = file.extension
            val targetName = if (newName.endsWith(".$extension")) newName else "$newName.$extension"
            val newFile = File(file.parentFile, targetName)

            if (newFile.exists()) {
                return@withContext Result.failure(Exception("A file with this name already exists"))
            }

            val success = file.renameTo(newFile)
            if (success) {
                Result.success(newFile)
            } else {
                Result.failure(Exception("Failed to rename file"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Rename failed: ${e.message}"))
        }
    }

    suspend fun deleteFile(filePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("File not found"))
            }

            val success = file.delete()
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete file"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Delete failed: ${e.message}"))
        }
    }

    suspend fun moveFile(filePath: String, destinationDir: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(filePath)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(Exception("Source file not found"))
            }

            val destDir = File(destinationDir)
            destDir.mkdirs()

            val destFile = File(destDir, sourceFile.name)

            if (destFile.exists()) {
                return@withContext Result.failure(Exception("A file with this name already exists in the destination"))
            }

            // Try rename first (same filesystem)
            val renamed = sourceFile.renameTo(destFile)
            if (renamed) {
                return@withContext Result.success(destFile)
            }

            // Fall back to copy + delete
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            sourceFile.delete()
            Result.success(destFile)
        } catch (e: Exception) {
            Result.failure(Exception("Move failed: ${e.message}"))
        }
    }

    suspend fun copyFile(filePath: String, destinationDir: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(filePath)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(Exception("Source file not found"))
            }

            val destDir = File(destinationDir)
            destDir.mkdirs()

            val destFile = File(destDir, sourceFile.name)

            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            Result.success(destFile)
        } catch (e: Exception) {
            Result.failure(Exception("Copy failed: ${e.message}"))
        }
    }

    suspend fun findDuplicates(): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val files = listFiles()
        if (files.size < 2) return@withContext emptyList()

        // Group by size first (quick filter)
        val sizeGroups = files.groupBy { it.sizeBytes }.filter { it.value.size > 1 }

        val duplicates = mutableListOf<DuplicateGroup>()

        for ((_, sameSizeFiles) in sizeGroups) {
            // Calculate checksums only for same-size files
            val checksumMap = mutableMapOf<String, MutableList<MediaFile>>()

            for (file in sameSizeFiles) {
                val checksum = calculateFileChecksum(file.path)
                checksumMap.getOrPut(checksum) { mutableListOf() }.add(file.copy(checksum = checksum))
            }

            for ((checksum, group) in checksumMap) {
                if (group.size > 1) {
                    duplicates.add(DuplicateGroup(checksum, group))
                }
            }
        }

        duplicates
    }

    suspend fun cleanTempFiles(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var count = 0

            // Clean temp directory
            if (tempDir.exists()) {
                tempDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                        count++
                    }
                }
            }

            // Clean partial downloads (.part, .tmp files)
            if (downloadDir.exists()) {
                downloadDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".part") || file.name.endsWith(".tmp") ||
                        file.name.endsWith(".temp.srt") || file.name.endsWith(".ytdl")) {
                        file.delete()
                        count++
                    }
                }
            }

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(Exception("Cleanup failed: ${e.message}"))
        }
    }

    suspend fun getTotalSize(): Long = withContext(Dispatchers.IO) {
        if (!downloadDir.exists()) return@withContext 0L
        downloadDir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    suspend fun getTempSize(): Long = withContext(Dispatchers.IO) {
        var total = 0L
        if (tempDir.exists()) {
            total += tempDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
        // Also count partial files in download dir
        if (downloadDir.exists()) {
            downloadDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".part") || file.name.endsWith(".tmp") ||
                    file.name.endsWith(".temp.srt") || file.name.endsWith(".ytdl")) {
                    total += file.length()
                }
            }
        }
        total
    }

    fun getShareIntent(filePath: String): Intent? {
        val file = File(filePath)
        if (!file.exists()) return null

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(file.extension) ?: "*/*"

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun getDownloadDirectory(): File = downloadDir

    private fun File.toMediaFile(): MediaFile {
        val ext = extension.lowercase()
        return MediaFile(
            path = absolutePath,
            name = nameWithoutExtension,
            extension = ext,
            sizeBytes = length(),
            sizeFormatted = formatBytes(length()),
            lastModified = lastModified(),
            isVideo = ext in videoExtensions,
            isSubtitle = ext in subtitleExtensions,
            isAudio = ext in audioExtensions
        )
    }

    private suspend fun calculateFileChecksum(filePath: String): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("MD5")
        FileInputStream(filePath).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
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
