package com.agon.app.domain.usecase

import com.agon.app.data.SubtitleInfo
import com.agon.app.data.SubtitleMethod
import javax.inject.Inject

class DetermineSubtitleMethodUseCase @Inject constructor() {
    operator fun invoke(subtitles: List<SubtitleInfo>): SubtitleMethod {
        val hasArabic = subtitles.any { it.languageCode == "ar" }
        if (hasArabic) return SubtitleMethod.DIRECT_AR

        if (subtitles.isNotEmpty()) return SubtitleMethod.TRANSLATED_FROM_OTHER

        return SubtitleMethod.WHISPER_GENERATED
    }
}
