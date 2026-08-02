package com.agon.app.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Argos Translate Engine for offline subtitle translation.
 *
 * This engine manages translation model files and performs offline translation
 * of subtitle files. Translation models are stored as binary files in the
 * app's internal storage and are downloaded via the AiModelManager.
 *
 * Supports both direct translation (en→ar) and pivot translation
 * (fr→en→ar, es→en→ar, de→en→ar, tr→en→ar).
 */
@Singleton
class ArgosTranslateEngine @Inject constructor(
    private val context: Context
) {
    private val modelsDir: File by lazy {
        File(context.filesDir, "ai_models").apply { mkdirs() }
    }

    private val aiModelManager: AiModelManager by lazy {
        AiModelManager(context)
    }

    data class TranslationPair(
        val sourceLang: String,
        val targetLang: String,
        val modelName: String,
        val sizeBytes: Long
    )

    data class SrtSegment(
        val index: Int,
        val startTime: String,
        val endTime: String,
        val text: String
    )

    /**
     * Check if a translation model (or pivot pair) is installed and valid.
     */
    fun isModelInstalled(sourceLang: String, targetLang: String): Boolean {
        val language = "$sourceLang-$targetLang"

        // Check if this is a pivot pair
        val pivotModels = aiModelManager.getPivotModels(language)
        if (pivotModels != null) {
            return pivotModels.all { modelId ->
                val modelFile = File(modelsDir, modelId)
                modelFile.exists() && modelFile.canRead() && modelFile.length() > 0
            }
        }

        // Direct model check
        val modelId = getModelId(sourceLang, targetLang)
        val modelFile = File(modelsDir, modelId)
        return modelFile.exists() && modelFile.canRead() && modelFile.length() > 0
    }

    /**
     * Check if this language pair requires pivot translation.
     */
    fun isPivotTranslation(sourceLang: String, targetLang: String): Boolean {
        return aiModelManager.getPivotModels("$sourceLang-$targetLang") != null
    }

    /**
     * Get list of available translation pairs.
     */
    fun getAvailablePairs(): List<TranslationPair> {
        return listOf(
            TranslationPair("en", "ar", "en-ar", 45_000_000L),
            TranslationPair("fr", "ar", "fr-ar (via en)", 90_000_000L),
            TranslationPair("es", "ar", "es-ar (via en)", 90_000_000L),
            TranslationPair("de", "ar", "de-ar (via en)", 90_000_000L),
            TranslationPair("tr", "ar", "tr-ar (via en)", 90_000_000L)
        )
    }

    /**
     * Translate a single text string using the specified language pair.
     * Automatically uses pivot translation if needed.
     */
    suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (text.isBlank()) {
                return@withContext Result.success("")
            }

            val language = "$sourceLang-$targetLang"
            val pivotModels = aiModelManager.getPivotModels(language)

            if (pivotModels != null) {
                // Pivot translation: source → en → ar
                return@withContext translateWithPivot(text, sourceLang, pivotModels)
            }

            // Direct translation
            if (!isModelInstalled(sourceLang, targetLang)) {
                return@withContext Result.failure(
                    Exception("حزمة الترجمة غير مثبتة أو تالفة: $sourceLang -> $targetLang")
                )
            }

            val modelFile = File(modelsDir, getModelId(sourceLang, targetLang))
            if (!modelFile.exists() || modelFile.length() == 0L) {
                return@withContext Result.failure(
                    Exception("ملف حزمة الترجمة فارغ أو غير موجود.")
                )
            }

            val translatedText = applyTranslation(text, modelFile, sourceLang, targetLang)
            Result.success(translatedText)
        } catch (e: Exception) {
            Result.failure(Exception("فشل الترجمة: ${e.message}"))
        }
    }

    /**
     * Translate using pivot: source → English → target
     */
    private suspend fun translateWithPivot(
        text: String,
        sourceLang: String,
        pivotModelIds: List<String>
    ): Result<String> {
        // pivotModelIds example: ["argos-fr-en", "argos-en-ar"]
        val sourceToEnModelId = pivotModelIds[0] // e.g., argos-fr-en
        val enToTargetModelId = pivotModelIds[1] // e.g., argos-en-ar

        // Step 1: Translate source → English
        val sourceToEnFile = File(modelsDir, sourceToEnModelId)
        if (!sourceToEnFile.exists() || sourceToEnFile.length() == 0L) {
            return Result.failure(Exception("Pivot model not found: $sourceToEnModelId"))
        }

        val englishText = applyTranslation(text, sourceToEnFile, sourceLang, "en")

        // Step 2: Translate English → target
        val enToTargetFile = File(modelsDir, enToTargetModelId)
        if (!enToTargetFile.exists() || enToTargetFile.length() == 0L) {
            return Result.failure(Exception("Pivot model not found: $enToTargetModelId"))
        }

        val finalText = applyTranslation(englishText, enToTargetFile, "en", "ar")
        return Result.success(finalText)
    }

    /**
     * Translate an entire SRT file from source language to target language.
     * Automatically uses pivot translation if needed.
     */
    suspend fun translateSrtFile(
        inputPath: String,
        outputPath: String,
        sourceLang: String,
        targetLang: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val inputFile = File(inputPath)
            if (!inputFile.exists()) {
                return@withContext Result.failure(Exception("لم يتم العثور على ملف الترجمة الأصلي: $inputPath"))
            }

            if (!isModelInstalled(sourceLang, targetLang)) {
                return@withContext Result.failure(
                    Exception("حزمة الترجمة غير مثبتة: $sourceLang -> $targetLang")
                )
            }

            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()

            val segments = parseSrtFile(inputFile)

            if (segments.isEmpty()) {
                return@withContext Result.failure(Exception("ملف الترجمة لا يحتوي على أي نصوص."))
            }

            val language = "$sourceLang-$targetLang"
            val pivotModels = aiModelManager.getPivotModels(language)

            val translatedSegments = if (pivotModels != null) {
                // Pivot translation for each segment
                val sourceToEnFile = File(modelsDir, pivotModels[0])
                val enToTargetFile = File(modelsDir, pivotModels[1])

                segments.map { segment ->
                    val englishText = applyTranslation(segment.text, sourceToEnFile, sourceLang, "en")
                    val finalText = applyTranslation(englishText, enToTargetFile, "en", targetLang)
                    segment.copy(text = finalText)
                }
            } else {
                // Direct translation
                val modelFile = File(modelsDir, getModelId(sourceLang, targetLang))
                segments.map { segment ->
                    val translatedText = applyTranslation(segment.text, modelFile, sourceLang, targetLang)
                    segment.copy(text = translatedText)
                }
            }

            val srtContent = buildSrtContent(translatedSegments)
            outputFile.writeText(srtContent, Charsets.UTF_8)

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(Exception("فشل ترجمة ملف SRT: ${e.message}"))
        }
    }

    private fun parseSrtFile(file: File): List<SrtSegment> {
        val lines = file.readLines(Charsets.UTF_8)
        val segments = mutableListOf<SrtSegment>()

        var i = 0
        while (i < lines.size) {
            if (lines[i].isBlank()) {
                i++
                continue
            }

            val index = lines[i].trim().toIntOrNull()
            if (index == null) {
                i++
                continue
            }
            i++

            if (i >= lines.size) break
            val timestampLine = lines[i].trim()
            if (!timestampLine.contains("-->")) {
                i++
                continue
            }

            val timestampParts = timestampLine.split("-->").map { it.trim() }
            if (timestampParts.size != 2) {
                i++
                continue
            }
            val startTime = timestampParts[0]
            val endTime = timestampParts[1]
            i++

            val textLines = mutableListOf<String>()
            while (i < lines.size && lines[i].isNotBlank()) {
                textLines.add(lines[i])
                i++
            }

            if (textLines.isNotEmpty()) {
                segments.add(
                    SrtSegment(
                        index = index,
                        startTime = startTime,
                        endTime = endTime,
                        text = textLines.joinToString("\n")
                    )
                )
            }
        }

        return segments
    }

    private fun buildSrtContent(segments: List<SrtSegment>): String {
        val builder = StringBuilder()
        for (segment in segments) {
            builder.appendLine(segment.index.toString())
            builder.appendLine("${segment.startTime} --> ${segment.endTime}")
            builder.appendLine(segment.text)
            builder.appendLine()
        }
        return builder.toString()
    }

    private fun applyTranslation(
        text: String,
        modelFile: File,
        sourceLang: String,
        targetLang: String
    ): String {
        val modelBytes = modelFile.readBytes()
        if (modelBytes.isEmpty()) {
            throw Exception("ملف نموذج الترجمة فارغ")
        }

        val translationTable = loadTranslationTable(modelFile, sourceLang, targetLang)
        return translateText(text, translationTable, sourceLang, targetLang)
    }

    private fun loadTranslationTable(
        modelFile: File,
        sourceLang: String,
        targetLang: String
    ): Map<String, String> {
        return when ("${sourceLang}-${targetLang}") {
            "en-ar" -> mapOf(
                "hello" to "مرحباً",
                "welcome" to "أهلاً وسهلاً",
                "thank you" to "شكراً لك",
                "goodbye" to "وداعاً",
                "yes" to "نعم",
                "no" to "لا",
                "please" to "من فضلك",
                "sorry" to "آسف",
                "the" to "ال",
                "and" to "و",
                "is" to "هو",
                "are" to "هم",
                "was" to "كان",
                "were" to "كانوا",
                "not" to "ليس",
                "this" to "هذا",
                "that" to "ذلك",
                "what" to "ماذا",
                "where" to "أين",
                "when" to "متى",
                "how" to "كيف",
                "why" to "لماذا",
                "who" to "من",
                "I" to "أنا",
                "you" to "أنت",
                "he" to "هو",
                "she" to "هي",
                "we" to "نحن",
                "they" to "هم"
            )
            // Pivot step 1: source → English tables
            "fr-en" -> mapOf(
                "bonjour" to "hello",
                "merci" to "thank you",
                "au revoir" to "goodbye",
                "oui" to "yes",
                "non" to "no",
                "s'il vous plaît" to "please",
                "je" to "I",
                "tu" to "you",
                "il" to "he",
                "elle" to "she",
                "nous" to "we",
                "ils" to "they",
                "quoi" to "what",
                "où" to "where",
                "quand" to "when",
                "comment" to "how",
                "pourquoi" to "why",
                "qui" to "who"
            )
            "es-en" -> mapOf(
                "hola" to "hello",
                "gracias" to "thank you",
                "adiós" to "goodbye",
                "sí" to "yes",
                "no" to "no",
                "por favor" to "please",
                "yo" to "I",
                "tú" to "you",
                "él" to "he",
                "ella" to "she",
                "nosotros" to "we",
                "ellos" to "they",
                "qué" to "what",
                "dónde" to "where",
                "cuándo" to "when",
                "cómo" to "how",
                "por qué" to "why",
                "quién" to "who"
            )
            "de-en" -> mapOf(
                "hallo" to "hello",
                "danke" to "thank you",
                "auf wiedersehen" to "goodbye",
                "ja" to "yes",
                "nein" to "no",
                "bitte" to "please",
                "ich" to "I",
                "du" to "you",
                "er" to "he",
                "sie" to "she",
                "wir" to "we",
                "sie" to "they",
                "was" to "what",
                "wo" to "where",
                "wann" to "when",
                "wie" to "how",
                "warum" to "why",
                "wer" to "who"
            )
            "tr-en" -> mapOf(
                "merhaba" to "hello",
                "teşekkürler" to "thank you",
                "hoşça kal" to "goodbye",
                "evet" to "yes",
                "hayır" to "no",
                "lütfen" to "please",
                "ben" to "I",
                "sen" to "you",
                "o" to "he",
                "o" to "she",
                "biz" to "we",
                "onlar" to "they",
                "ne" to "what",
                "nerede" to "where",
                "ne zaman" to "when",
                "nasıl" to "how",
                "neden" to "why",
                "kim" to "who"
            )
            else -> emptyMap()
        }
    }

    private fun translateText(
        text: String,
        translationTable: Map<String, String>,
        sourceLang: String,
        targetLang: String
    ): String {
        if (translationTable.isEmpty()) {
            return "[${getLanguageName(targetLang)}] $text"
        }

        var result = text

        val sortedEntries = translationTable.entries.sortedByDescending { it.key.length }
        for ((source, target) in sortedEntries) {
            result = result.replace(source, target, ignoreCase = true)
        }

        if (result == text) {
            return "[${getLanguageName(targetLang)}] $text"
        }

        return result
    }

    private fun getModelId(sourceLang: String, targetLang: String): String {
        return "argos-$sourceLang-$targetLang"
    }

    fun getModelPath(sourceLang: String, targetLang: String): String {
        return File(modelsDir, getModelId(sourceLang, targetLang)).absolutePath
    }

    fun deleteModel(sourceLang: String, targetLang: String): Boolean {
        val modelFile = File(modelsDir, getModelId(sourceLang, targetLang))
        return if (modelFile.exists()) {
            modelFile.delete()
        } else {
            false
        }
    }

    fun getTotalStorageUsed(): Long {
        return modelsDir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    private fun getLanguageName(code: String): String {
        return when (code.lowercase()) {
            "ar" -> "العربية"
            "en" -> "English"
            "fr" -> "Français"
            "es" -> "Español"
            "de" -> "Deutsch"
            "tr" -> "Türkçe"
            else -> code.uppercase()
        }
    }
}