package com.agon.app.domain.usecase

import com.agon.app.data.SubtitleInfo
import com.agon.app.data.SubtitleMethod
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DetermineSubtitleMethodUseCaseTest {

    private lateinit var useCase: DetermineSubtitleMethodUseCase

    @Before
    fun setup() {
        useCase = DetermineSubtitleMethodUseCase()
    }

    @Test
    fun `invoke with Arabic subtitle returns DIRECT_AR`() {
        val subtitles = listOf(
            SubtitleInfo("العربية", "ar", false),
            SubtitleInfo("English", "en", false)
        )

        val result = useCase(subtitles)

        assertEquals(SubtitleMethod.DIRECT_AR, result)
    }

    @Test
    fun `invoke with auto-generated Arabic subtitle returns DIRECT_AR`() {
        val subtitles = listOf(
            SubtitleInfo("العربية (Auto)", "ar", true)
        )

        val result = useCase(subtitles)

        assertEquals(SubtitleMethod.DIRECT_AR, result)
    }

    @Test
    fun `invoke with non-Arabic subtitle returns TRANSLATED_FROM_OTHER`() {
        val subtitles = listOf(
            SubtitleInfo("English", "en", false),
            SubtitleInfo("Français", "fr", false)
        )

        val result = useCase(subtitles)

        assertEquals(SubtitleMethod.TRANSLATED_FROM_OTHER, result)
    }

    @Test
    fun `invoke with empty subtitles returns WHISPER_GENERATED`() {
        val subtitles = emptyList<SubtitleInfo>()

        val result = useCase(subtitles)

        assertEquals(SubtitleMethod.WHISPER_GENERATED, result)
    }

    @Test
    fun `invoke with only English subtitle returns TRANSLATED_FROM_OTHER`() {
        val subtitles = listOf(
            SubtitleInfo("English", "en", false)
        )

        val result = useCase(subtitles)

        assertEquals(SubtitleMethod.TRANSLATED_FROM_OTHER, result)
    }

    @Test
    fun `invoke prioritizes Arabic over other languages`() {
        val subtitles = listOf(
            SubtitleInfo("English", "en", false),
            SubtitleInfo("العربية", "ar", false),
            SubtitleInfo("Français", "fr", false)
        )

        val result = useCase(subtitles)

        assertEquals(SubtitleMethod.DIRECT_AR, result)
    }
}
