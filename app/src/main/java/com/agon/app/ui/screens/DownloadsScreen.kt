package com.agon.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.agon.app.data.DownloadItem
import com.agon.app.data.DownloadMode
import com.agon.app.data.DownloadStatus
import com.agon.app.data.SubtitleMethod
import com.agon.app.ui.components.BreathingEmptyState
import com.agon.app.ui.components.GlassCard
import com.agon.app.ui.components.NeonLinearProgressIndicator
import com.agon.app.ui.components.RadialStatChip
import com.agon.app.ui.components.SegmentedControl
import com.agon.app.ui.theme.SuccessGreen
import com.agon.app.ui.theme.WarningAmber
import com.agon.app.viewmodel.DownloadFilter
import com.agon.app.viewmodel.DownloadsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    navController: NavHostController? = null
) {
    val downloads by viewModel.downloads.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val filteredDownloads by viewModel.filteredDownloads.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isSelectionMode) {
                        Text(
                            text = "تم تحديد ${uiState.selectedIds.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.DownloadDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "إدارة التنزيلات",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isSelectionMode) {
                            viewModel.clearSelection()
                        } else {
                            navController?.popBackStack("home", inclusive = false)
                        }
                    }) {
                        Icon(
                            imageVector = if (uiState.isSelectionMode) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (uiState.isSelectionMode) "إلغاء التحديد" else "الرجوع للمتصفح",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (uiState.isSelectionMode) {
                        val allSelected = uiState.selectedIds.size == filteredDownloads.size && filteredDownloads.isNotEmpty()
                        
                        // زر تحديد الكل
                        IconButton(onClick = {
                            if (allSelected) viewModel.clearSelection()
                            else viewModel.selectAllVisible(filteredDownloads.map { it.id })
                        }) {
                            Icon(
                                imageVector = if (allSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                contentDescription = "تحديد الكل",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        // زر حذف المحدد
                        IconButton(onClick = { viewModel.deleteSelectedItems() }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "حذف المحدد",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        // زر تفعيل وضع التحديد
                        IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                            Icon(
                                imageVector = Icons.Filled.CheckBoxOutlineBlank,
                                contentDescription = "تفعيل وضع التحديد",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // الإحصائيات وفلاتر البحث (تظهر فقط في الوضع العادي)
            if (!uiState.isSelectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadialStatChip(
                        label = "الكل",
                        count = downloads.size,
                        total = downloads.size.coerceAtLeast(1),
                        icon = Icons.Filled.FilterList,
                        color = MaterialTheme.colorScheme.primary
                    )
                    RadialStatChip(
                        label = "مكتمل",
                        count = downloads.count { it.status == DownloadStatus.COMPLETED },
                        total = downloads.size.coerceAtLeast(1),
                        icon = Icons.Filled.CheckCircle,
                        color = SuccessGreen
                    )
                    RadialStatChip(
                        label = "نشط",
                        count = downloads.count { 
                            it.status in listOf(
                                DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED, DownloadStatus.QUEUED,
                                DownloadStatus.ANALYZING, DownloadStatus.EXTRACTING_SUBS, DownloadStatus.TRANSLATING
                            ) 
                        },
                        total = downloads.size.coerceAtLeast(1),
                        icon = Icons.Filled.Download,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                SegmentedControl(
                    items = DownloadFilter.entries.toList(),
                    selectedItem = uiState.selectedFilter,
                    onItemSelected = { viewModel.setFilter(it) },
                    itemLabel = { it.label },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // قائمة التنزيلات أو الحالة الفارغة
            if (filteredDownloads.isEmpty()) {
                BreathingEmptyState(
                    icon = Icons.Filled.HourglassEmpty,
                    title = "لا توجد تنزيلات",
                    subtitle = "الصق رابط فيديو في الشاشة الرئيسية لبدء التحميل"
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredDownloads, key = { it.id }) { item ->
                        DownloadItemCard(
                            item = item,
                            isSelected = uiState.selectedIds.contains(item.id),
                            isSelectionMode = uiState.isSelectionMode,
                            onToggleSelect = { viewModel.toggleItemSelection(item.id) },
                            onDelete = { viewModel.requestDelete(item.id) },
                            onCancel = { viewModel.cancelDownload(item.id) },
                            onRetry = { viewModel.retryDownload(item.id) },
                            onShowSrt = { viewModel.showSrtPreview(item) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    // نافذة تأكيد الحذف الفردي (تظهر فقط خارج وضع التحديد)
    if (uiState.showDeleteConfirm != null && !uiState.isSelectionMode) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("حذف التنزيل", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف هذا التنزيل؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) {
                    Text("إلغاء")
                }
            }
        )
    }

    // نافذة معاينة الترجمة
    if (uiState.showSrtPreview != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissSrtPreview,
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            SrtPreviewSheet(
                item = uiState.showSrtPreview!!,
                onDismiss = viewModel::dismissSrtPreview
            )
        }
    }
}

@Composable
private fun DownloadItemCard(
    item: DownloadItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelect: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onShowSrt: () -> Unit
) {
    val statusColor by animateColorAsState(
        targetValue = when (item.status) {
            DownloadStatus.COMPLETED -> SuccessGreen
            DownloadStatus.FAILED, DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.error
            DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
            DownloadStatus.PAUSED -> WarningAmber
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "statusColor"
    )

    // خلفية مميزة للعنصر المحدد
    val cardBgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (isSelectionMode) onToggleSelect() },
        containerColor = cardBgColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ➕ Checkbox يظهر فقط في وضع التحديد
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.selectedQuality} • ${item.downloadMode.toArabicLabel()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // إخفاء زر الحذف الفردي أثناء وضع التحديد المتعدد لتجنب الالتباس
                if (!isSelectionMode) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Error Message Block
            AnimatedVisibility(
                visible = (item.status == DownloadStatus.FAILED || item.status == DownloadStatus.CANCELLED) && item.errorMessage.isNotEmpty()
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = item.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Progress (Neon-like)
            if (item.status in listOf(
                DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED, DownloadStatus.QUEUED,
                DownloadStatus.ANALYZING, DownloadStatus.EXTRACTING_SUBS, DownloadStatus.TRANSLATING
            )) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NeonLinearProgressIndicator(
                            progress = item.progress / 100f,
                            modifier = Modifier.weight(1f),
                            color = statusColor
                        )
                        Text(
                            text = "${item.progress}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.width(40.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.downloadedSize.ifEmpty { "0 MB" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.downloadSpeed.isNotEmpty()) {
                            Text(
                                text = item.downloadSpeed,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (item.eta.isNotEmpty()) {
                            Text(
                                text = "متبقي: ${item.eta}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = item.totalSize,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (item.status == DownloadStatus.DOWNLOADING) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إلغاء التحميل", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Status Badge + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassCard(
                    modifier = Modifier,
                    elevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = item.status.toIcon(),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = statusColor
                        )
                        Text(
                            text = item.status.toArabicLabel(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                if (item.status == DownloadStatus.COMPLETED && item.srtFilePath.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onShowSrt, modifier = Modifier.size(40.dp)) {
                            Icon(
                                Icons.Filled.Description,
                                contentDescription = "معاينة SRT",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(onClick = { /* Share */ }, modifier = Modifier.size(40.dp)) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = "مشاركة",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                if (item.status == DownloadStatus.FAILED || item.status == DownloadStatus.CANCELLED) {
                    androidx.compose.material3.FilledTonalButton(
                        onClick = onRetry,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إعادة المحاولة", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SrtPreviewSheet(
    item: DownloadItem,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.Subtitles,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = "ملف الترجمة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 0.dp
        ) {
            Text(
                text = "1\n00:00:01,000 --> 00:00:05,000\nمرحباً بكم في هذا الفيديو\n\n2\n00:00:06,000 --> 00:00:10,000\nسنتحدث اليوم عن موضوع مهم\n\n3\n00:00:11,000 --> 00:00:15,000\nدعونا نبدأ بالجزء الأول\n\n4\n00:00:16,000 --> 00:00:20,000\nهذه نقطة مهمة جداً\n\n5\n00:00:21,000 --> 00:00:25,000\nشكراً لمتابعتكم",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("إغلاق")
            }
            androidx.compose.material3.Button(
                onClick = { /* Share */ },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("مشاركة SRT")
            }
        }
    }
}

private fun DownloadStatus.toArabicLabel(): String = when (this) {
    DownloadStatus.QUEUED -> "في الانتظار"
    DownloadStatus.ANALYZING -> "جاري التحليل"
    DownloadStatus.DOWNLOADING -> "جاري التحميل"
    DownloadStatus.PAUSED -> "متوقف مؤقتاً"
    DownloadStatus.EXTRACTING_SUBS -> "استخراج الترجمة"
    DownloadStatus.TRANSLATING -> "جاري الترجمة"
    DownloadStatus.COMPLETED -> "مكتمل"
    DownloadStatus.FAILED -> "فشل"
    DownloadStatus.CANCELLED -> "ملغي"
}

private fun DownloadStatus.toIcon() = when (this) {
    DownloadStatus.QUEUED -> Icons.Filled.HourglassEmpty
    DownloadStatus.ANALYZING -> Icons.Filled.Download
    DownloadStatus.DOWNLOADING -> Icons.Filled.Download
    DownloadStatus.PAUSED -> Icons.Filled.Pause
    DownloadStatus.EXTRACTING_SUBS -> Icons.Filled.Subtitles
    DownloadStatus.TRANSLATING -> Icons.Filled.Translate
    DownloadStatus.COMPLETED -> Icons.Filled.CheckCircle
    DownloadStatus.FAILED -> Icons.Filled.Error
    DownloadStatus.CANCELLED -> Icons.Filled.Error
}

private fun SubtitleMethod.toArabicLabel(): String = when (this) {
    SubtitleMethod.DIRECT_AR -> "ترجمة مباشرة"
    SubtitleMethod.TRANSLATED_FROM_OTHER -> "ترجمة Argos"
    SubtitleMethod.WHISPER_GENERATED -> "توليد Whisper"
    SubtitleMethod.NONE -> "بدون ترجمة"
}

private fun DownloadMode.toArabicLabel(): String = when (this) {
    DownloadMode.VIDEO_AND_SUBTITLE -> "فيديو + ترجمة"
    DownloadMode.VIDEO_ONLY -> "فيديو فقط"
    DownloadMode.SUBTITLE_ONLY -> "ترجمة فقط"
}