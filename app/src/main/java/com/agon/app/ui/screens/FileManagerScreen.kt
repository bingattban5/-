package com.agon.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agon.app.engine.DuplicateGroup
import com.agon.app.engine.MediaFile
import com.agon.app.ui.components.BreathingEmptyState
import com.agon.app.ui.components.GlassCard
import com.agon.app.ui.theme.PastelBlue
import com.agon.app.ui.theme.PastelGreen
import com.agon.app.ui.theme.PastelOrange
import com.agon.app.ui.theme.PastelPurple
import com.agon.app.ui.theme.SuccessGreen
import com.agon.app.ui.theme.VibrantOrange
import com.agon.app.ui.theme.WarningAmber
import com.agon.app.viewmodel.FileFilter
import com.agon.app.viewmodel.FileManagerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileManagerScreen(
    viewModel: FileManagerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    // حالة التبديل بين عرض الشبكة وعرض القائمة
    var isGridView by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header & Toggle View
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "مدير الملفات",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                // أيقونة التبديل بين الشبكة والقائمة
                IconButton(
                    onClick = { isGridView = !isGridView },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Filled.ViewList else Icons.Filled.ViewModule,
                        contentDescription = "تبديل العرض",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. ملخص التخزين: شريط تقدم أفقي متصل ومقوس
            StorageSummaryBar(
                totalSize = uiState.totalSize,
                tempSize = uiState.tempSize,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. شريط البحث الزجاجي
            var isSearchFocused by remember { mutableStateOf(false) }
            val glowColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isSearchFocused) 0.3f else 0f)
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (isSearchFocused) 8.dp else 2.dp,
                        shape = RoundedCornerShape(50),
                        ambientColor = glowColor,
                        spotColor = glowColor
                    )
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isSearchFocused = it.isFocused },
                    placeholder = { Text("بحث في الملفات...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "مسح", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. فلاتر الملفات: أزرار بيضاوية عائمة + أزرار الإجراءات
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FileFilter.entries.forEach { filter ->
                        val isSelected = uiState.selectedFilter == filter
                        val bgColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            label = "filterBg"
                        )
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "filterText"
                        )
                        val borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = bgColor,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier.clickable { viewModel.setFilter(filter) }
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = textColor,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                
                // أزرار الإجراءات السريعة
                IconButton(
                    onClick = { viewModel.findDuplicates() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Filled.FileCopy, contentDescription = "بحث عن المكرر", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { viewModel.cleanTempFiles() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "تنظيف المؤقت", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // File count
            Text(
                text = "${uiState.filteredFiles.size} ملف",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. عرض الملفات (قائمة أو شبكة)
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.filteredFiles.isEmpty()) {
                BreathingEmptyState(
                    icon = Icons.Filled.Folder,
                    title = "لا توجد ملفات",
                    subtitle = "حمّل فيديو أو ترجمة لتظهر الملفات هنا"
                )
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.filteredFiles, key = { it.path }) { file ->
                            FileGridItemCard(
                                file = file,
                                onRename = { viewModel.showRenameDialog(file) },
                                onDelete = { viewModel.showDeleteDialog(file) },
                                onShare = {
                                    val intent = viewModel.getShareIntent(file)
                                    if (intent != null) {
                                        context.startActivity(android.content.Intent.createChooser(intent, "مشاركة الملف"))
                                    }
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.filteredFiles, key = { it.path }) { file ->
                            FileItemCard(
                                file = file,
                                onRename = { viewModel.showRenameDialog(file) },
                                onDelete = { viewModel.showDeleteDialog(file) },
                                onShare = {
                                    val intent = viewModel.getShareIntent(file)
                                    if (intent != null) {
                                        context.startActivity(android.content.Intent.createChooser(intent, "مشاركة الملف"))
                                    }
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        )
    }

    // Rename Dialog
    if (uiState.showRenameDialog != null) {
        val file = uiState.showRenameDialog!!
        var newName by remember { mutableStateOf(file.name) }

        AlertDialog(
            onDismissRequest = viewModel::dismissRenameDialog,
            title = { Text("تغيير اسم الملف", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("الاسم الجديد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.renameFile(newName) },
                    enabled = newName.isNotBlank() && newName != file.name
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRenameDialog) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Dialog
    if (uiState.showDeleteDialog != null) {
        val file = uiState.showDeleteDialog!!
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text("حذف الملف", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف \"${file.name}.${file.extension}\"؟") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::deleteFile,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Duplicate Dialog
    if (uiState.showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDuplicateDialog,
            title = { Text("ملفات مكررة", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("تم العثور على ${uiState.duplicates.size} مجموعة من الملفات المكررة:")
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.duplicates.forEachIndexed { index, group ->
                        Text(
                            text = "المجموعة ${index + 1}: ${group.files.size} ملفات (${group.files.first().sizeFormatted})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteDuplicates(uiState.duplicates) },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("حذف المكرر")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDuplicateDialog) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// ==========================================
// مكونات واجهة المستخدم المخصصة لمدير الملفات
// ==========================================

@Composable
private fun StorageSummaryBar(
    totalSize: String,
    tempSize: String,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier, elevation = 4.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(text = "المساحة المستخدمة", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = totalSize, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "الملفات المؤقتة", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = tempSize, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = VibrantOrange)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // الشريط المتصل والمقوس
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // الجزء الأزرق (المساحة الرئيسية) - نسبة تقديرية 75% للعرض البصري
                    Box(
                        modifier = Modifier
                            .weight(3f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(start = 6.dp, end = 0.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    // الجزء البرتقالي (المؤقتة) - نسبة تقديرية 25% للعرض البصري
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(start = 0.dp, end = 6.dp))
                            .background(VibrantOrange)
                    )
                }
            }
        }
    }
}

@Composable
private fun FileItemCard(
    file: MediaFile,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // File type icon with Pastel Background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            file.isVideo -> PastelBlue
                            file.isSubtitle -> PastelGreen
                            file.isAudio -> PastelPurple
                            else -> PastelOrange
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        file.isVideo -> Icons.Filled.Videocam
                        file.isSubtitle -> Icons.Filled.Subtitles
                        file.isAudio -> Icons.Filled.AudioFile
                        else -> Icons.Filled.Description
                    },
                    contentDescription = null,
                    tint = when {
                        file.isVideo -> MaterialTheme.colorScheme.primary
                        file.isSubtitle -> SuccessGreen
                        file.isAudio -> MaterialTheme.colorScheme.secondary
                        else -> WarningAmber
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = file.sizeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = file.extension.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = formatDate(file.lastModified),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "خيارات",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("تغيير الاسم") },
                        onClick = { showMenu = false; onRename() },
                        leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("مشاركة") },
                        onClick = { showMenu = false; onShare() },
                        leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("حذف", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileGridItemCard(
    file: MediaFile,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth().aspectRatio(0.8f), elevation = 2.dp) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        when {
                            file.isVideo -> PastelBlue
                            file.isSubtitle -> PastelGreen
                            file.isAudio -> PastelPurple
                            else -> PastelOrange
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        file.isVideo -> Icons.Filled.Videocam
                        file.isSubtitle -> Icons.Filled.Subtitles
                        file.isAudio -> Icons.Filled.AudioFile
                        else -> Icons.Filled.Description
                    },
                    contentDescription = null,
                    tint = when {
                        file.isVideo -> MaterialTheme.colorScheme.primary
                        file.isSubtitle -> SuccessGreen
                        file.isAudio -> MaterialTheme.colorScheme.secondary
                        else -> WarningAmber
                    },
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${file.sizeFormatted} • ${file.extension.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Share, contentDescription = "مشاركة", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "خيارات", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    // Dropdown for Grid View (positioned absolutely or via Box wrapper in real app, simplified here)
    // For simplicity in Grid, we can just trigger the dialog directly or use a Box wrapper like the list view.
    // Reusing the same Dropdown logic wrapped in a Box at the top right:
    Box(modifier = Modifier.fillMaxSize()) {
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            DropdownMenuItem(
                text = { Text("تغيير الاسم") },
                onClick = { showMenu = false; onRename() },
                leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            DropdownMenuItem(
                text = { Text("حذف", color = MaterialTheme.colorScheme.error) },
                onClick = { showMenu = false; onDelete() },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))
    return sdf.format(Date(timestamp))
}
