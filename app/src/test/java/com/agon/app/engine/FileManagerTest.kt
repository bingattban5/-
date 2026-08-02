package com.agon.app.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileManagerTest {

    @Test
    fun `MediaFile identifies video files correctly`() {
        val videoFile = MediaFile(
            path = "/test/video.mp4",
            name = "video",
            extension = "mp4",
            sizeBytes = 100_000_000L,
            sizeFormatted = "95.4 MB",
            lastModified = System.currentTimeMillis(),
            isVideo = true,
            isSubtitle = false,
            isAudio = false
        )

        assertTrue(videoFile.isVideo)
        assertFalse(videoFile.isSubtitle)
        assertFalse(videoFile.isAudio)
    }

    @Test
    fun `MediaFile identifies subtitle files correctly`() {
        val subtitleFile = MediaFile(
            path = "/test/subtitle.srt",
            name = "subtitle",
            extension = "srt",
            sizeBytes = 50_000L,
            sizeFormatted = "48.8 KB",
            lastModified = System.currentTimeMillis(),
            isVideo = false,
            isSubtitle = true,
            isAudio = false
        )

        assertFalse(subtitleFile.isVideo)
        assertTrue(subtitleFile.isSubtitle)
        assertFalse(subtitleFile.isAudio)
    }

    @Test
    fun `MediaFile identifies audio files correctly`() {
        val audioFile = MediaFile(
            path = "/test/audio.mp3",
            name = "audio",
            extension = "mp3",
            sizeBytes = 5_000_000L,
            sizeFormatted = "4.8 MB",
            lastModified = System.currentTimeMillis(),
            isVideo = false,
            isSubtitle = false,
            isAudio = true
        )

        assertFalse(audioFile.isVideo)
        assertFalse(audioFile.isSubtitle)
        assertTrue(audioFile.isAudio)
    }

    @Test
    fun `DuplicateGroup holds correct data`() {
        val files = listOf(
            MediaFile(
                path = "/test/video1.mp4",
                name = "video1",
                extension = "mp4",
                sizeBytes = 100_000_000L,
                sizeFormatted = "95.4 MB",
                lastModified = System.currentTimeMillis(),
                isVideo = true,
                isSubtitle = false,
                isAudio = false,
                checksum = "abc123"
            ),
            MediaFile(
                path = "/test/video2.mp4",
                name = "video2",
                extension = "mp4",
                sizeBytes = 100_000_000L,
                sizeFormatted = "95.4 MB",
                lastModified = System.currentTimeMillis(),
                isVideo = true,
                isSubtitle = false,
                isAudio = false,
                checksum = "abc123"
            )
        )

        val group = DuplicateGroup(
            checksum = "abc123",
            files = files
        )

        assertEquals("abc123", group.checksum)
        assertEquals(2, group.files.size)
        assertEquals(files[0].checksum, files[1].checksum)
    }

    @Test
    fun `MediaFile copy updates fields correctly`() {
        val original = MediaFile(
            path = "/test/video.mp4",
            name = "video",
            extension = "mp4",
            sizeBytes = 100_000_000L,
            sizeFormatted = "95.4 MB",
            lastModified = System.currentTimeMillis(),
            isVideo = true,
            isSubtitle = false,
            isAudio = false
        )

        val updated = original.copy(
            name = "renamed_video",
            path = "/test/renamed_video.mp4"
        )

        assertEquals("renamed_video", updated.name)
        assertEquals("/test/renamed_video.mp4", updated.path)
        assertEquals(original.sizeBytes, updated.sizeBytes)
        assertEquals(original.isVideo, updated.isVideo)
    }

    @Test
    fun `FileFilter enum has all expected values`() {
        val filters = com.agon.app.viewmodel.FileFilter.entries
        assertEquals(4, filters.size)
        assertEquals("الكل", com.agon.app.viewmodel.FileFilter.ALL.label)
        assertEquals("فيديو", com.agon.app.viewmodel.FileFilter.VIDEO.label)
        assertEquals("ترجمة", com.agon.app.viewmodel.FileFilter.SUBTITLE.label)
        assertEquals("صوت", com.agon.app.viewmodel.FileFilter.AUDIO.label)
    }
}
