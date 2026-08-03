package com.agon.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agon.app.ui.theme.SuccessGreen
import com.agon.app.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val savePath by viewModel.savePath.collectAsState()
    val autoTranslate by viewModel.autoTranslate.collectAsState()
    val defaultQuality by viewModel.defaultQuality.collectAsState()
    val cacheEnabled by viewModel.cacheEnabled.collectAsState()
    val subtitleFormat by viewModel.subtitleFormat.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showSubtitleFormatDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "الإعدادات",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Appearance Section
        item {
            SectionHeader(title = "المظهر", icon = Icons.Filled.ColorLens)
        }

        item {
            SettingsCard {
                SettingsToggleRow(
                    icon = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "الوضع الليلي",
                    subtitle = if (isDarkTheme) "الوضع الداكن مفعل" else "الوضع الفاتح مفعل",
                    checked = isDarkTheme,
                    onCheckedChange = viewModel::setDarkTheme
                )
            }
        }

        // Download Section
        item {
            SectionHeader(title = "التحميل", icon = Icons.Filled.Folder)
        }

        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SettingsInfoRow(
                        icon = Icons.Filled.Folder,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = "مسار الحفظ",
                        value = savePath
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    SettingsInfoRow(
                        icon = Icons.Filled.Hd,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        title = "الجودة الافتراضية",
                        value = when (defaultQuality) {
                            "best" -> "أفضل جودة متاحة"
                            "1080p" -> "1080p (Full HD)"
                            "720p" -> "720p (HD)"
                            else -> "أفضل جودة متاحة"
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    SettingsInfoRow(
                        icon = Icons.Filled.Description,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "صيغة الترجمة",
                        value = subtitleFormat.uppercase(),
                        onClick = { showSubtitleFormatDialog = true }
                    )
                }
            }
        }

        // Translation Section
        item {
            SectionHeader(title = "الترجمة", icon = Icons.Filled.Translate)
        }

        item {
            SettingsCard {
                SettingsToggleRow(
                    icon = Icons.Filled.Translate,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    title = "الترجمة التلقائية",
                    subtitle = "ترجمة النصوص غير العربية إلى العربية تلقائياً عبر ML Kit",
                    checked = autoTranslate,
                    onCheckedChange = viewModel::setAutoTranslate
                )
            }
        }

        // Storage Section
        item {
            SectionHeader(title = "التخزين", icon = Icons.Filled.Storage)
        }

        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SettingsToggleRow(
                        icon = Icons.Filled.Memory,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        title = "ذاكرة التخزين المؤقت",
                        subtitle = "تخزين مؤقت لتسريع العمليات المتكررة",
                        checked = cacheEnabled,
                        onCheckedChange = viewModel::setCacheEnabled
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.showClearCacheDialog() }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.CleaningServices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "مسح ذاكرة التخزين المؤقت",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "الحجم الحالي: ${uiState.cacheSize}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // About Section
        item {
            SectionHeader(title = "حول التطبيق", icon = Icons.Filled.Info)
        }

        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AboutRow("اسم التطبيق", "SubVIDD")
                    AboutRow("الإصدار", "1.0.0")
                    AboutRow("المطور", "Saeed Bingattban")
                    AboutRow("الترخيص", "مفتوح المصدر - MIT")
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "المكتبات المستخدمة",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val libs = listOf(
                            "yt-dlp" to "محرك استخراج الفيديو",
                            "Whisper.cpp" to "تحويل الصوت إلى نص",
                            "ML Kit" to "الترجمة المحلية",
                            "Jetpack Compose" to "واجهة المستخدم",
                            "Material Design 3" to "تصميم الواجهة"
                        )
                        libs.forEach { (name, desc) ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "• $name",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "- $desc",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    // Clear Cache Dialog
    if (uiState.showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissClearCacheDialog,
            title = { Text("مسح ذاكرة التخزين المؤقت", fontWeight = FontWeight.Bold) },
            text = { Text("سيتم حذف جميع الملفات المؤقتة (${uiState.cacheSize}). هل تريد المتابعة؟") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::clearCache,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("مسح")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissClearCacheDialog) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Subtitle Format Selection Dialog
    if (showSubtitleFormatDialog) {
        val availableFormats = listOf("SRT", "VTT", "ASS", "TXT")
        AlertDialog(
            onDismissRequest = { showSubtitleFormatDialog = false },
            title = { Text("اختر صيغة الترجمة", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    availableFormats.forEach { format ->
                        val isSelected = subtitleFormat.uppercase() == format
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSubtitleFormat(format)
                                    showSubtitleFormatDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = format,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "محدد",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (format != availableFormats.last()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubtitleFormatDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onClick != null) {
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = "تغيير",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}
