package com.agon.app.ui.screens.browser.state

import com.agon.app.data.DownloadMode
import com.agon.app.data.SubtitleMethod
import com.agon.app.data.VideoInfo
import com.agon.app.data.VideoQuality

/**
 * الحالة الشاملة للمتصفح والتبويبات
 */
data class BrowserState(
    // التبويبات
    val tabs: List<TabInfo> = listOf(TabInfo.createNew()),
    val activeTabIndex: Int = 0,

    // حالة التحليل
    val isAnalyzing: Boolean = false,
    val analysisStep: String = "",

    // الفيديو المكتشف
    val detectedVideoUrl: String? = null,
    val videoInfo: VideoInfo? = null,
    val selectedQuality: VideoQuality? = null,
    val selectedMode: DownloadMode = DownloadMode.VIDEO_AND_SUBTITLE,
    val subtitleMethod: SubtitleMethod = SubtitleMethod.NONE,
    val subtitleSearchStep: Int = 0,

    // قوائم الحوار
    val showTabsList: Boolean = false,
    val showVideoSheet: Boolean = false,
    val showExitDialog: Boolean = false,
    val showMainMenu: Boolean = false,

    // رسائل الخطأ والنجاح
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // معلومات النظام
    val cpuArch: String = "ARM 64-bit / Universal",

    // الضغط المطول على الروابط
    val longPressedLink: String? = null,
    val showLinkMenu: Boolean = false
) {
    /**
     * التبويب النشط حالياً
     */
    val activeTab: TabInfo?
        get() = tabs.getOrNull(activeTabIndex)

    /**
     * عدد التبويبات المفتوحة
     */
    val tabsCount: Int
        get() = tabs.size

    /**
     * هل يمكن الرجوع في التبويب النشط؟
     */
    val canGoBack: Boolean
        get() = activeTab?.canGoBack ?: false
}
