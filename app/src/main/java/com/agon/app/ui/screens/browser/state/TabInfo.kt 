package com.agon.app.ui.screens.browser.state

/**
 * نموذج بيانات يمثل تبويب واحد في المتصفح
 */
data class TabInfo(
    val id: String,                    // معرف فريد للتبويب
    val title: String,                 // عنوان الصفحة الحالية
    val url: String,                   // الرابط الحالي
    val favicon: String? = null,       // رابط الأيقونة (favicon)
    val canGoBack: Boolean = false,    // هل يمكن الرجوع للخلف؟
    val canGoForward: Boolean = false, // هل يمكن التقدم للأمام؟
    val isLoading: Boolean = false,    // هل الصفحة قيد التحميل؟
    val progress: Int = 0              // نسبة تحميل الصفحة (0-100)
) {
    companion object {
        /**
         * إنشاء تبويب جديد بصفحة Google افتراضية
         */
        fun createNew(): TabInfo {
            return TabInfo(
                id = java.util.UUID.randomUUID().toString(),
                title = "تبويب جديد",
                url = "https://www.google.com",
                favicon = null,
                canGoBack = false,
                canGoForward = false,
                isLoading = false,
                progress = 0
            )
        }
    }
}
