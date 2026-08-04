package com.agon.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pause // <-- تمت إضافة هذا الاستيراد
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agon.app.data.AiModel
import com.agon.app.data.EngineInfo
import com.agon.app.ui.components.CircularProgressDownloadButton
import com.agon.app.ui.components.GlassCard
import com.agon.app.ui.theme.PastelBlue
import com.agon.app.ui.theme.PastelGreen
import com.agon.app.ui.theme.SuccessGreen
import com.agon.app.ui.theme.WarningAmber
import com.agon.app.viewmodel.ModelsViewModel
import kotlinx.coroutines.delay

@Composable
fun ModelsScreen(
    viewModel: ModelsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("محرك الاستخراج", "نماذج Whisper", "حزم الترجمة")

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Filled.Dns,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "النماذج والمحركات",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "إدارة محركات الاستخراج ونماذج الذكاء الاصطناعي المحلية",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                elevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Filled.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "المساحة المستخدمة:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.totalStorageFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = uiState.errorMessage != null || uiState.successMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val isError = uiState.errorMessage != null
            val message = uiState.errorMessage ?: uiState.successMessage ?: ""
            val bgColor = if (isError) MaterialTheme.colorScheme.errorContainer else SuccessGreen.copy(alpha = 0.15f)
            val contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else SuccessGreen

            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp),
                elevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isError) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor
                        )
                    }
                    IconButton(onClick = { viewModel.clearMessage() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = contentColor, modifier = Modifier.size(18.dp))
                    }
                }
            }

            LaunchedEffect(message) {
                delay(5000)
                viewModel.clearMessage()
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = uiState.selectedTab == index

                val indicatorWidth by animateDpAsState(
                    targetValue = if (isSelected) 40.dp else 0.dp,
                    label = "tabIndicatorWidth"
                )
                val indicatorColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    label = "tabIndicatorColor"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "tabTextColor"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.selectTab(index) }
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(indicatorWidth)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .shadow(
                                elevation = if (isSelected) 6.dp else 0.dp,
                                shape = RoundedCornerShape(2.dp),
                                ambientColor = indicatorColor,
                                spotColor = indicatorColor
                            )
                            .background(indicatorColor)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (uiState.selectedTab) {
            0 -> YtDlpSection(
                engine = uiState.ytDlpEngine,
                onCheckUpdate = viewModel::checkYtDlpUpdate,
                onUpdate = viewModel::updateYtDlp,
                onReinstall = viewModel::reinstallYtDlp
            )
            1 -> WhisperSection(
                models = uiState.aiModels.filter { it.type == "whisper" },
                downloadingModels = uiState.downloadingModels,
                onDownload = viewModel::downloadModel,
                onDelete = viewModel::deleteModel,
                onCancelDownload = viewModel::cancelDownload,
                onPauseDownload = viewModel::pauseDownload,
                onResumeDownload = viewModel::resumeDownload
            )
            2 -> ArgosSection(
                models = uiState.aiModels.filter { it.type == "argos" },
                downloadingModels = uiState.downloadingModels,
                onDownload = viewModel::downloadModel,
                onDelete = viewModel::deleteModel,
                onCancelDownload = viewModel::cancelDownload,
                onPauseDownload = viewModel::pauseDownload,
                onResumeDownload = viewModel::resumeDownload
            )
        }
    }
}

@Composable
private fun YtDlpSection(
    engine: EngineInfo,
    onCheckUpdate: () -> Unit,
    onUpdate: () -> Unit,
    onReinstall: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(PastelBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "yt-dlp", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "محرك استخراج الفيديو من المواقع", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (engine.isInstalled) {
                            Surface(shape = RoundedCornerShape(20.dp), color = SuccessGreen.copy(alpha = 0.15f)) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(14.dp), tint = SuccessGreen)
                                    Text(text = "مثبت", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                }
                            }
                        }
                    }

                    GlassCard(elevation = 0.dp) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "الإصدار الحالي", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = engine.version.ifEmpty { "غير مثبت" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "يدعم المواقع", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "+1000 موقع", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = onCheckUpdate, modifier = Modifier.weight(1f), enabled = !engine.isChecking) {
                            if (engine.isChecking) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فحص التحديثات", style = MaterialTheme.typography.labelLarge)
                        }
                        if (engine.hasUpdate) {
                            OutlinedButton(onClick = onUpdate, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, WarningAmber)) {
                                Icon(Icons.Filled.Update, contentDescription = null, modifier = Modifier.size(18.dp), tint = WarningAmber)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تحديث", color = WarningAmber, style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            OutlinedButton(onClick = onReinstall, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إعادة تثبيت", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WhisperSection(
    models: List<AiModel>,
    downloadingModels: Map<String, Int>,
    onDownload: (String) -> Unit,
    onDelete: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(models) { model ->
            AiModelCard(
                model = model,
                downloadProgress = downloadingModels[model.id],
                onDownload = { onDownload(model.id) },
                onDelete = { onDelete(model.id) },
                onCancel = { onCancelDownload(model.id) },
                onPause = { onPauseDownload(model.id) },
                onResume = { onResumeDownload(model.id) }
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ArgosSection(
    models: List<AiModel>,
    downloadingModels: Map<String, Int>,
    onDownload: (String) -> Unit,
    onDelete: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(models) { model ->
            AiModelCard(
                model = model,
                downloadProgress = downloadingModels[model.id],
                onDownload = { onDownload(model.id) },
                onDelete = { onDelete(model.id) },
                onCancel = { onCancelDownload(model.id) },
                onPause = { onPauseDownload(model.id) },
                onResume = { onResumeDownload(model.id) }
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun AiModelCard(
    model: AiModel,
    downloadProgress: Int?,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    val currentProgress = downloadProgress ?: if (model.isPaused && model.sizeBytes > 0) {
        ((model.downloadedBytes.toFloat() / model.sizeBytes) * 100).toInt().coerceIn(0, 100)
    } else {
        null
    }

    if (model.isCorrupted) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            elevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = model.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "⚠️ الملف تالف أو غير مكتمل", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                OutlinedButton(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إعادة التحميل", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                }
            }
        }
        return
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (model.type == "whisper") PastelBlue else PastelGreen
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (model.type == "whisper") Icons.Filled.AudioFile else Icons.Filled.Translate,
                        contentDescription = null,
                        tint = if (model.type == "whisper") MaterialTheme.colorScheme.primary else SuccessGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = model.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = model.sizeFormatted, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                CircularProgressDownloadButton(
                    progress = currentProgress,
                    onClick = {
                        if (model.isPaused) onResume() else onDownload()
                    },
                    modifier = Modifier.size(44.dp)
                )
            }

            if (currentProgress != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (model.isPaused && downloadProgress == null) {
                        Text(
                            text = "متوقف مؤقتاً",
                            style = MaterialTheme.typography.labelMedium,
                            color = WarningAmber,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onResume, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "استئناف", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "إلغاء", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Text(
                            text = "جاري التحميل...",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        // ✅ التعديل هنا: تغيير الأيقونة من Close إلى Pause
                        IconButton(onClick = onPause, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Pause, contentDescription = "إيقاف مؤقت", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "إلغاء", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } else if (model.isDownloaded) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حذف", fontWeight = FontWeight.Medium)
                    }
                }
            } else if (model.downloadUrl.isBlank()) {
                Text(
                    text = "يتطلب تحميل حزمتين أساسيتين (عبر الإنجليزية)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}