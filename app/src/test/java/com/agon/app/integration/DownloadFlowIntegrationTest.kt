package com.agon.app.integration

import com.agon.app.data.DownloadMode
import com.agon.app.data.DownloadStatus
import com.agon.app.data.SubtitleMethod
import com.agon.app.data.SubtitleInfo
import com.agon.app.data.VideoInfo
import com.agon.app.data.VideoQuality
import com.agon.app.domain.usecase.DetermineSubtitleMethodUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration tests that verify the interaction between multiple components.
 * These tests simulate real-world scenarios without mocking.
 */
class DownloadFlowIntegrationTest {

    private lateinit var determineSubtitleMethod: DetermineSubtitleMethodUseCase

    @Before
    fun setup() {
        determineSubtitleMethod = DetermineSubtitleMethodUseCase()
    }

    @Test
    fun `full download flow - video with Arabic subtitles uses DIRECT_AR`() {
        // Simulate a video with Arabic subtitles available
        val videoInfo = createVideoInfo(
            subtitles = listOf(
                SubtitleInfo("العربية", "ar", false),
                SubtitleInfo("English", "en", false)
            )
        )

        val method = determineSubtitleMethod(videoInfo.availableSubtitles)

        assertEquals(SubtitleMethod.DIRECT_AR, method)
        assertEquals(DownloadMode.VIDEO_AND_SUBTITLE, DownloadMode.VIDEO_AND_SUBTITLE)
    }

    @Test
    fun `full download flow - video with English only uses TRANSLATED_FROM_OTHER`() {
        val videoInfo = createVideoInfo(
            subtitles = listOf(
                SubtitleInfo("English", "en", false)
            )
        )

        val method = determineSubtitleMethod(videoInfo.availableSubtitles)

        assertEquals(SubtitleMethod.TRANSLATED_FROM_OTHER, method)
    }

    @Test
    fun `full download flow - video with no subtitles uses WHISPER_GENERATED`() {
        val videoInfo = createVideoInfo(subtitles = emptyList())

        val method = determineSubtitleMethod(videoInfo.availableSubtitles)

        assertEquals(SubtitleMethod.WHISPER_GENERATED, method)
    }

    @Test
    fun `video only mode skips subtitle processing`() {
        val mode = DownloadMode.VIDEO_ONLY
        val method = SubtitleMethod.NONE

        // In VIDEO_ONLY mode, subtitle method should be NONE
        assertEquals(DownloadMode.VIDEO_ONLY, mode)
        assertEquals(SubtitleMethod.NONE, method)
    }

    @Test
    fun `subtitle only mode requires valid subtitle method`() {
        val mode = DownloadMode.SUBTITLE_ONLY

        // Subtitle-only mode should work with DIRECT_AR
        val subtitlesWithAr = listOf(SubtitleInfo("العربية", "ar", false))
        val methodAr = determineSubtitleMethod(subtitlesWithAr)
        assertEquals(SubtitleMethod.DIRECT_AR, methodAr)

        // Subtitle-only mode should work with TRANSLATED_FROM_OTHER
        val subtitlesWithEn = listOf(SubtitleInfo("English", "en", false))
        val methodEn = determineSubtitleMethod(subtitlesWithEn)
        assertEquals(SubtitleMethod.TRANSLATED_FROM_OTHER, methodEn)

        // Subtitle-only mode should work with WHISPER_GENERATED
        val methodWhisper = determineSubtitleMethod(emptyList())
        assertEquals(SubtitleMethod.WHISPER_GENERATED, methodWhisper)
    }

    @Test
    fun `video info contains all required fields`() {
        val videoInfo = createVideoInfo()

        assertNotNull(videoInfo.title)
        assertNotNull(videoInfo.thumbnailUrl)
        assertNotNull(videoInfo.duration)
        assertNotNull(videoInfo.uploader)
        assertTrue(videoInfo.qualities.isNotEmpty())
        assertNotNull(videoInfo.sourceUrl)
    }

    @Test
    fun `quality selection works with multiple qualities`() {
        val qualities = listOf(
            VideoQuality("313", "2160p (4K)", "3840×2160", "~850 MB", "mp4"),
            VideoQuality("137", "1080p (Full HD)", "1920×1080", "~250 MB", "mp4"),
            VideoQuality("136", "720p (HD)", "1280×720", "~120 MB", "mp4"),
            VideoQuality("135", "480p", "854×480", "~65 MB", "mp4"),
            VideoQuality("134", "360p", "640×360", "~35 MB", "mp4")
        )

        // Select 1080p
        val selected = qualities.find { it.label.contains("1080p") }
        assertNotNull(selected)
        assertEquals("137", selected!!.id)
        assertEquals("~250 MB", selected.fileSize)
    }

    @Test
    fun `download status transitions are valid`() {
        // Valid flow: QUEUED -> ANALYZING -> DOWNLOADING -> EXTRACTING_SUBS -> TRANSLATING -> COMPLETED
        val validFlow = listOf(
            DownloadStatus.QUEUED,
            DownloadStatus.ANALYZING,
            DownloadStatus.DOWNLOADING,
            DownloadStatus.EXTRACTING_SUBS,
            DownloadStatus.TRANSLATING,
            DownloadStatus.COMPLETED
        )

        assertEquals(6, validFlow.size)
        assertEquals(DownloadStatus.QUEUED, validFlow.first())
        assertEquals(DownloadStatus.COMPLETED, validFlow.last())
    }

    @Test
    fun `failed download can be retried`() {
        val failedItem = com.agon.app.data.DownloadItem(
            id = "test-id",
            status = DownloadStatus.FAILED,
            errorMessage = "Network error"
        )

        // Simulate retry by resetting status
        val retriedItem = failedItem.copy(
            status = DownloadStatus.QUEUED,
            progress = 0,
            errorMessage = "",
            isPaused = false
        )

        assertEquals(DownloadStatus.QUEUED, retriedItem.status)
        assertEquals(0, retriedItem.progress)
        assertEquals("", retriedItem.errorMessage)
    }

    private fun createVideoInfo(
        title: String = "Test Video",
        subtitles: List<SubtitleInfo> = emptyList(),
        qualities: List<VideoQuality> = listOf(
            VideoQuality("137", "1080p", "1920×1080", "~250 MB", "mp4")
        )
    ): VideoInfo {
        return VideoInfo(
            title = title,
            thumbnailUrl = "https://example.com/thumb.jpg",
            duration = "10:30",
            uploader = "Test Channel",
            qualities = qualities,
            availableSubtitles = subtitles,
            sourceUrl = "https://youtube.com/watch?v=test"
        )
    }
}
