package com.agon.app.domain.usecase

import com.agon.app.engine.WhisperEngine
import com.agon.app.engine.WhisperProgress
import com.agon.app.engine.WhisperSegment
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject

class TranscribeAudioUseCase @Inject constructor(
    private val whisperEngine: WhisperEngine
) {
    suspend fun isWhisperInstalled(): Boolean {
        return whisperEngine.isWhisperInstalled()
    }

    suspend fun installWhisper(): Result<Unit> {
        return whisperEngine.installWhisper()
    }

    fun isModelDownloaded(modelSize: WhisperEngine.ModelSize): Boolean {
        return whisperEngine.isModelDownloaded(modelSize)
    }

    fun getAvailableModels(): List<WhisperEngine.ModelSize> {
        return whisperEngine.getAvailableModels()
    }

    suspend fun selectBestModel(): Result<WhisperEngine.ModelSize> {
        return try {
            val availableRam = whisperEngine.getAvailableMemoryMB()
            val model = whisperEngine.selectBestModel(availableRam)
            Result.success(model)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun transcribeAudio(
        audioPath: String,
        modelSize: WhisperEngine.ModelSize,
        language: String = "auto",
        translate: Boolean = false
    ): Flow<WhisperProgress> {
        return whisperEngine.transcribeAudio(audioPath, modelSize, language, translate)
    }

    suspend fun transcribeToSRT(
        audioPath: String,
        outputPath: String,
        modelSize: WhisperEngine.ModelSize,
        language: String = "auto",
        translate: Boolean = false
    ): Result<File> {
        return whisperEngine.transcribeToSRT(audioPath, outputPath, modelSize, language, translate)
    }

    suspend fun parseSRTFile(srtPath: String): Result<List<WhisperSegment>> {
        return whisperEngine.parseSRTFile(srtPath)
    }

    suspend fun getAvailableMemoryMB(): Long {
        return whisperEngine.getAvailableMemoryMB()
    }
}
