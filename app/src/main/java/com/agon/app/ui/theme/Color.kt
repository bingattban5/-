package com.agon.app.ui.theme

import androidx.compose.ui.graphics.Color

// الألوان الأساسية للتطبيق (التدرج من السماوي إلى النيلي)
val SkyBlue = Color(0xFF4FC3F7)
val DeepIndigo = Color(0xFF1A237E)
val VibrantOrange = Color(0xFFFFB74D)

// ألوان التوهج والنيون
val NeonBlueGlow = Color(0xFF4FC3F7).copy(alpha = 0.4f)
val NeonBlueSpot = Color(0xFF4FC3F7).copy(alpha = 0.7f)

// ألوان الزجاج والخلفيات الشفافة
val GlassWhiteLight = Color.White.copy(alpha = 0.7f)
val GlassWhiteDark = Color(0xFF1E1E1E).copy(alpha = 0.7f)
val GlassBorderLight = Color.White.copy(alpha = 0.4f)
val GlassBorderDark = Color.White.copy(alpha = 0.1f)

// ألوان الحالات الخاصة
val SuccessGreen = Color(0xFF4CAF50)
val WarningAmber = Color(0xFFFFC107)
val ErrorRed = Color(0xFFEF5350)
val ErrorRedTinted = ErrorRed.copy(alpha = 0.15f)

// ألوان الباستيل الناعمة
val PastelBlue = Color(0xFFE3F2FD)
val PastelGreen = Color(0xFFE8F5E9)
val PastelPurple = Color(0xFFF3E5F5)
val PastelOrange = Color(0xFFFFF3E0)

// ==========================================
// تعريفات ألوان Material 3 القياسية (لإصلاح أخطاء Theme.kt)
// ==========================================
val PrimaryLight = DeepIndigo
val OnPrimaryLight = Color.White
val PrimaryContainerLight = SkyBlue.copy(alpha = 0.2f)
val OnPrimaryContainerLight = DeepIndigo

val SecondaryLight = Color(0xFF5C6BC0)
val OnSecondaryLight = Color.White
val SecondaryContainerLight = Color(0xFFE8EAF6)
val OnSecondaryContainerLight = Color(0xFF1A237E)

val TertiaryLight = VibrantOrange
val OnTertiaryLight = Color.Black
val TertiaryContainerLight = VibrantOrange.copy(alpha = 0.2f)
val OnTertiaryContainerLight = Color(0xFF3E2723)

val ErrorLight = ErrorRed
val OnErrorLight = Color.White
val ErrorContainerLight = ErrorRedTinted
val OnErrorContainerLight = Color(0xFF410002)

val BackgroundLight = Color(0xFFFAFAFA)
val OnBackgroundLight = Color(0xFF1C1B1F)
val SurfaceLight = Color(0xFFFAFAFA)
val OnSurfaceLight = Color(0xFF1C1B1F)
val SurfaceVariantLight = Color(0xFFE7E0EC)
val OnSurfaceVariantLight = Color(0xFF49454F)
val OutlineLight = Color(0xFF79747E)

// ألوان الوضع الليلي
val PrimaryDark = SkyBlue
val OnPrimaryDark = DeepIndigo
val PrimaryContainerDark = DeepIndigo
val OnPrimaryContainerDark = SkyBlue

val SecondaryDark = Color(0xFFC5CAE9)
val OnSecondaryDark = Color(0xFF283593)
val SecondaryContainerDark = Color(0xFF3949AB)
val OnSecondaryContainerDark = Color(0xFFE8EAF6)

val TertiaryDark = VibrantOrange
val OnTertiaryDark = Color(0xFF3E2723)
val TertiaryContainerDark = Color(0xFF5D4037)
val OnTertiaryContainerDark = VibrantOrange

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFB4AB)

val BackgroundDark = Color(0xFF121212)
val OnBackgroundDark = Color(0xFFE6E1E5)
val SurfaceDark = Color(0xFF121212)
val OnSurfaceDark = Color(0xFFE6E1E5)
val SurfaceVariantDark = Color(0xFF49454F)
val OnSurfaceVariantDark = Color(0xFFCAC4D0)
val OutlineDark = Color(0xFF938F99)
