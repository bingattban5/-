package com.agon.app.ui.theme

import androidx.compose.ui.graphics.Color

// الألوان الأساسية المتدرجة (Gradients)
val SkyBlue = Color(0xFF4FC3F7)
val DeepIndigo = Color(0xFF1A237E)
val VibrantOrange = Color(0xFFFFB74D)

// ألوان التوهج والنيون (Neon Glows)
val NeonBlueGlow = Color(0xFF4FC3F7).copy(alpha = 0.4f)
val NeonBlueSpot = Color(0xFF4FC3F7).copy(alpha = 0.7f)

// ألوان الزجاج والخلفيات الشفافة (Glassmorphism)
val GlassWhiteLight = Color.White.copy(alpha = 0.7f)
val GlassWhiteDark = Color(0xFF1E1E1E).copy(alpha = 0.7f)
val GlassBorderLight = Color.White.copy(alpha = 0.4f)
val GlassBorderDark = Color.White.copy(alpha = 0.1f)

// ألوان الحالات الخاصة
val SuccessGreen = Color(0xFF4CAF50)
val WarningAmber = Color(0xFFFFC107)
val ErrorRed = Color(0xFFEF5350)
val ErrorRedTinted = ErrorRed.copy(alpha = 0.15f)

// ألوان الباستيل الناعمة للبطاقات (Pastel)
val PastelBlue = Color(0xFFE3F2FD)
val PastelGreen = Color(0xFFE8F5E9)
val PastelPurple = Color(0xFFF3E5F5)
val PastelOrange = Color(0xFFFFF3E0)
