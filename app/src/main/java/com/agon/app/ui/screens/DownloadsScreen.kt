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
                            if (uiState.isSelectionMode) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (uiState.isSelectionMode) "إلغاء التحديد" else "الرجوع للمتصفح",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (uiState.isSelectionMode) {
                        val allSelected = uiState.selectedIds.size == filteredDownloads.size && filteredDownloads.isNotEmpty()
                        IconButton(onClick = {
                            if (allSelected) viewModel.clearSelection()
                            else viewModel.selectAllVisible(filteredDownloads.map { it.id })
                        }) {
                            Icon(
                                if (allSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                contentDescription = "تحديد الكل",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { viewModel.deleteSelectedItems() }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "حذف المحدد",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                            Icon(
                                Icons.Filled.CheckBoxOutlineBlank,
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

            // Stats: Mini Radial Progress Bars (تظهر فقط في الوضع العادي)
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

                // Filter Elements
                SegmentedControl(
                    items = DownloadFilter.entries.toList(),
                    selectedItem = uiState.selectedFilter,
                    onItemSelected = { viewModel.setFilter(it) },
                    itemLabel = { it.label },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Downloads List or Empty State
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

    // Delete Confirmation Dialog
    if (uiState.showDeleteConfirm != null && !uiState.isSelectionMode) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("حذف التنزيل", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف هذا التنزيل؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) { Text("إلغاء") }
            }
        )
    }

    // SRT Preview Sheet
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
        Row(
            modifier = Modifier.padding(16.dp),
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

        // ... (هنا يبقى باقي كود البطاقة كما هو: Error Message Block, Progress, Status Badge + Actions)
        // تأكد من نسخ باقي محتوى DownloadItemCard من ملفك الأصلي للحفاظ على شريط التقدم وأزرار إعادة المحاطة
    }
}

// ... (تأكد من بقاء دوال SrtPreviewSheet, toArabicLabel, toIcon كما هي في ملفك الأصلي)