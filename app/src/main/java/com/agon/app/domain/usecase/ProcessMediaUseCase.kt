package com.agon.app.domain.usecase

import com.agon.app.engine.FFmpegEngine
import com.agon.app.engine.FFmpegProgress
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProcessMediaUseCase @Inject constructor(
    private val ffmpegEngine: FFmpegEngine
) {
    suspend fun isFFmpegInstalled(): Boolean {
        return ffmpegEngine.isFFmpegInstalled()
    }

    suspend fun installFFmpeg(): Result<Unit> {
        return ffmpegEngine.installFFmpeg()
    }

    fun mergeAudioVideo(
        videoPath: String,
        audioPath: String,
        outputPath: String
    ): Flow<FFmpegProgress> {
        return ffmpegEngine.mergeAudioVideo(videoPath, audioPath, outputPath)
    }

    fun extractAudio(
        videoPath: String,
        outputPath: String,
        format: String = "mp3"
    ): Flow<FFmpegProgress> {
        return ffmpegEngine.extractAudio(videoPath, outputPath, format)
    }

    fun burnSubtitles(
        videoPath: String,
        subtitlePath: String,
        outputPath: String
    ): Flow<FFmpegProgress> {
        return ffmpegEngine.burnSubtitles(videoPath, subtitlePath, outputPath)
    }

    fun attachSoftSubtitles(
        videoPath: String,
        subtitlePath: String,
        outputPath: String,
        language: String = "ara"
    ): Flow<FFmpegProgress> {
        return ffmpegEngine.attachSoftSubtitles(videoPath, subtitlePath, outputPath, language)
    }

    fun trimMedia(
        inputPath: String,
        outputPath: String,
        startTime: String,
        duration: String
    ): Flow<FFmpegProgress> {
        return ffmpegEngine.trimMedia(inputPath, outputPath, startTime, duration)
    }

    fun compressVideo(
        inputPath: String,
        outputPath: String,
        crf: Int = 23,
        preset: String = "medium"
    ): Flow<FFmpegProgress> {
        return ffmpegEngine.compressVideo(inputPath, outputPath, crf, preset)
    }

    fun convertFormat(
        inputPath: String,
        outputPath: String,
        videoCodec: String = "copy",
        audioCodec: String = "copy"
    ): Flow<FFmpegProgress> {
        return ffmpegEngine.convertFormat(inputPath, outputPath, videoCodec, audioCodec)
    }

    suspend fun getMediaDuration(filePath: String): Result<Long> {
        return ffmpegEngine.getMediaDuration(filePath)
    }
}
