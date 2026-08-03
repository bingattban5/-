package com.agon.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agon.app.ui.components.AccordionList
import com.agon.app.ui.components.GlassCard
import com.agon.app.ui.components.GroupedSettingsCard
import com.agon.app.ui.components.SettingsRow
import com.agon.app.ui.theme.PastelBlue
import com.agon.app.ui.theme.PastelGreen
import com.agon.app.ui.theme.PastelOrange
import com.agon.app.ui.theme.PastelPurple
import com.agon.app.ui.theme.SuccessGreen
import com.agon.app.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val savePath by viewModel.savePath.collectAsState()
    val autoTranslate by viewModel.autoTranslate.collectAsState()
    val defaultQuality by viewModel.defaultQuality.collectAsState()
    val cacheEnabled by viewModel.cacheEnabled.collectAsState()
    val subtitleFormat by viewModel.subtitleFormat.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showSubtitleFormatDialog by remember { mutableStateOf(false) }
    
    // متغيرات خاصة بإدارة ملفات الـ Cookies
    var showDomainDialog by remember { mutableStateOf(false) }
    var pendingCookieContent by remember { mutableStateOf<String?>(null) }
    var domainInput by remember { mutableStateOf("") }
    
    // حالة قائمة المكتبات (Accordion)
    var isLibrariesExpanded by remember { mutableStateOf(false) }

    // Launcher لاختيار مجلد من ذاكرة الهاتف
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.setSavePath(it.toString())
        }
    }

    // Launcher لاختيار ملف Cookies (.txt)
    val cookiePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val content = try {
                context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                    reader.readText()
                }
            } catch (e: Exception) {
                null
            }
            
            if (!content.isNullOrBlank()) {
                pendingCookieContent = content
                domainInput = it.lastPathSegment?.substringBefore(".txt") ?: ""
                showDomainDialog = true
            }
        }
    }

    val displaySavePath = if (savePath.startsWith("content://")) {
        "مجلد مخصص (تم اختياره من الذاكرة)"
    } else {
        savePath
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
            Text(
                text = "المظهر",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            GroupedSettingsCard {
                SettingsRow(
                    icon = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "الوضع الليلي",
                    subtitle = if (isDarkTheme) "الوضع الداكن مفعل" else "الوضع الفاتح مفعل",
                    trailing = {
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = viewModel::setDarkTheme,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                )
            }
        }

        // Download Section
        item {
            Text(
                text = "التحميل",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            GroupedSettingsCard {
                SettingsRow(
                    icon = Icons.Filled.Folder,
                    iconTint = PastelBlue,
                    title = "مسار الحفظ",
                    subtitle = displaySavePath,
                    onClick = { folderPickerLauncher.launch(null) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                SettingsRow(
                    icon = Icons.Filled.Hd,
                    iconTint = PastelGreen,
                    title = "الجودة الافتراضية",
                    subtitle = when (defaultQuality) {
                        "best" -> "أفضل جودة متاحة"
                        "1080p" -> "1080p (Full HD)"
                        "720p" -> "720p (HD)"
                        else -> "أفضل جودة متاحة"
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                SettingsRow(
                    icon = Icons.Filled.Description,
                    iconTint = PastelPurple,
                    title = "صيغة الترجمة",
                    subtitle = subtitleFormat.uppercase(),
                    onClick = { showSubtitleFormatDialog = true }
                )
            }
        }

        // Cookies Section
        item {
            Text(
                text = "ملفات المصادقة (Cookies)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            GroupedSettingsCard {
                SettingsRow(
                    icon = Icons.Filled.Key,
                    iconTint = PastelOrange,
                    title = "إضافة ملف Cookies جديد",
                    subtitle = "للوصول أكثر",
                    onClick = { cookiePickerLauncher.launch("text/plain") }
                )
                
                if (uiState.savedCookieFiles.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "الملفات المحفوظة:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        uiState.savedCookieFiles.forEach { domain ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = domain,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteCookieFile(domain) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "حذف",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Translation Section
        item {
            Text(
                text = "الترجمة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            GroupedSettingsCard {
                SettingsRow(
                    icon = Icons.Filled.Translate,
                    iconTint = PastelGreen,
                    title = "الترجمة التلقائية",
                    subtitle = "ترجمة النصوص غير العربية إلى العربية تلقائياً عبر ML Kit",
                    trailing = {
                        Switch(
                            checked = autoTranslate,
                            onCheckedChange = viewModel::setAutoTranslate,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                )
            }
        }

        // Storage Section
        item {
            Text(
                text = "التخزين",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            GroupedSettingsCard {
                SettingsRow(
                    icon = Icons.Filled.Memory,
                    iconTint = PastelPurple,
                    title = "ذاكرة التخزين المؤقت",
                    subtitle = "تخزين مؤقت لتسريع العمليات المتكررة",
                    trailing = {
                        Switch(
                            checked = cacheEnabled,
                            onCheckedChange = viewModel::setCacheEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                SettingsRow(
                    icon = Icons.Filled.CleaningServices,
                    iconTint = MaterialTheme.colorScheme.error,
                    title = "مسح ذاكرة التخزين المؤقت",
                    subtitle = "الحجم الحالي: ${uiState.cacheSize}",
                    onClick = { viewModel.showClearCacheDialog() }
                )
            }
        }

        // About Section (Luxury Identity Card)
        item {
            Text(
                text = "حول التطبيق",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // شعار التطبيق أو أيقونة فاخرة
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "تحميل الفيديو و الترجمة",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "الإصدار 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "تطوير: سعيد بن قطبان",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "مفتوح المصدر - MIT License",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // قائمة المكتبات القابلة للطي (Accordion)
                    AccordionList(
                        title = "المكتبات المستخدمة",
                        isExpanded = isLibrariesExpanded,
                        onToggle = { isLibrariesExpanded = !isLibrariesExpanded }
                    ) {
                        val libs = listOf(
                            "yt-dlp" to "محرك استخراج الفيديو",
                            "Whisper.cpp" to "تحويل الصوت إلى نص",
                            "ML Kit" to "الترجمة المحلية",
                            "Jetpack Compose" to "واجهة المستخدم",
                            "Material Design 3" to "تصميم الواجهة"
                        )
                        libs.forEach { (name, desc) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• $name",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
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
                    colors = ButtonDefaults.textButtonColors(
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
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
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

    // Domain Input Dialog for Cookies
    if (showDomainDialog && pendingCookieContent != null) {
        AlertDialog(
            onDismissRequest = { 
                showDomainDialog = false
                pendingCookieContent = null
                domainInput = ""
            },
            title = { Text("اسم النطاق (Domain)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "أدخل اسم الموقع الذي ينتمي إليه هذا الملف (مثال: example.com)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = domainInput,
                        onValueChange = { domainInput = it },
                        label = { Text("اسم النطاق") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (domainInput.isNotBlank()) {
                            viewModel.saveCookieFile(domainInput.trim(), pendingCookieContent!!)
                            showDomainDialog = false
                            pendingCookieContent = null
                            domainInput = ""
                        }
                    },
                    enabled = domainInput.isNotBlank()
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDomainDialog = false
                    pendingCookieContent = null
                    domainInput = ""
                }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
