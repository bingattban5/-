package com.agon.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agon.app.data.DownloadMode
import com.agon.app.data.SubtitleMethod
import com.agon.app.viewmodel.HomeUiState
import com.agon.app.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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

    if (uiState.isResultScreenVisible && uiState.videoInfo != null) {
        BackHandler {
            viewModel.onBackPressed()
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("تفاصيل التحميل", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = viewModel::onBackPressed) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            ResultScreenContent(
                modifier = Modifier.padding(paddingValues),
                viewModel = viewModel,
                uiState = uiState
            )
        }

        if (uiState.showExitDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissExitDialog,
                title = { Text("تأكيد الخروج", fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد أنك تريد الرجوع لصفحة تحليل الرابط؟") },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmExit) {
                        Text("نعم", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissExitDialog) {
                        Text("لا")
                    }
                }
            )
        }
    } else {
        MainInputScreen(viewModel, uiState, snackbarHostState)
    }
}

@Composable
fun MainInputScreen(
    viewModel: HomeViewModel,
    uiState: HomeUiState,
    snackbarHostState: SnackbarHostState
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(48.dp)) }

            // 1. Hero Section: الشعار، الاسم، وشارة المعالج
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // أيقونة تشغيل ثلاثية الأبعاد بظلال متدرجة
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .shadow(
                                elevation = 16.dp, 
                                shape = CircleShape, 
                                ambientColor = Color(0xFF4FC3F7).copy(alpha = 0.4f), 
                                spotColor = Color(0xFF1A237E).copy(alpha = 0.4f)
                            )
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF4FC3F7), Color(0xFF1A237E)),
                                    center = Offset(0f, 0f),
                                    radius = 150f
                                ), 
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.Center)
                                .offset(x = 4.dp) // إزاحة بسيطة لتعزيز تأثير الـ 3D
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // اسم التطبيق بتدرج لوني
                    Text(
                        text = "SubVIDD",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF4FC3F7), Color(0xFF1A237E)),
                                start = Offset(0f, 0f),
                                end = Offset(0f, Float.POSITIVE_INFINITY)
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "تحميل الفيديوهات واستخراج الترجمات بالذكاء الاصطناعي",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    // شارة المعالج بتصميم زجاجي شفاف (Glass Pill)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.Memory, 
                                contentDescription = null, 
                                modifier = Modifier.size(14.dp),
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                uiState.cpuArch, 
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 2. منطقة الإدخال والزر
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("رابط الفيديو", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        // حقل إدخال كبسولي مع توهج عند النقر
                        var isFocused by remember { mutableStateOf(false) }
                        val glowColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isFocused) 0.4f else 0f)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = if (isFocused) 12.dp else 2.dp,
                                    shape = RoundedCornerShape(50),
                                    ambientColor = glowColor,
                                    spotColor = glowColor
                                )
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            OutlinedTextField(
                                value = uiState.url,
                                onValueChange = viewModel::onUrlChange,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("الصق رابط الفيديو هنا...") },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                trailingIcon = {
                                    if (uiState.url.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onUrlChange("") }) {
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
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Go),
                                keyboardActions = KeyboardActions(onGo = { viewModel.analyzeUrl() }),
                                enabled = !uiState.isAnalyzing,
                                onFocusedChanged = { isFocused = it }
                            )
                        }

                        // زر تحليل الرابط بتدرج لوني حيوي
                        Button(
                            onClick = viewModel::analyzeUrl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            enabled = uiState.url.isNotBlank() && !uiState.isAnalyzing,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF1A237E), Color(0xFF4FC3F7))
                                        )
                                    )
                                    .clickable(
                                        enabled = uiState.url.isNotBlank() && !uiState.isAnalyzing,
                                        indication = null, // نستخدم تموج الزر الافتراضي
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { viewModel.analyzeUrl() },
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.isAnalyzing) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("جاري التحليل...", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Analytics, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("تحليل الرابط", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. مؤشر التحليل
            item {
                AnimatedVisibility(visible = uiState.isAnalyzing, enter = fadeIn() + slideInVertically { it }) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("جاري تحليل الرابط", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(uiState.analysisStep, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primaryContainer)
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@Composable
fun ResultScreenContent(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    uiState: HomeUiState
) {
    val videoInfo = uiState.videoInfo ?: return

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Video Header Info
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = videoInfo.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "${videoInfo.uploader} • ${videoInfo.duration}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Quality Selection
        item {
            Text("الصيغ المتاحة للتحميل:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                videoInfo.qualities.forEach { quality ->
                    val isSelected = uiState.selectedQuality?.id == quality.id
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                        label = "qualityBg"
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.selectQuality(quality) },
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        shape = RoundedCornerShape(16.dp),
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                if (isSelected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Icon(Icons.Filled.Hd, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = quality.label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                Text(text = "${quality.resolution} • ${quality.format.uppercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(text = quality.fileSize, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.startSpecificDownload(DownloadMode.VIDEO_ONLY, SubtitleMethod.NONE) },
                modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                enabled = uiState.selectedQuality != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            ) {
                Icon(Icons.Filled.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تحميل الفيديو", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        // Subtitle Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("بيانات الترجمة:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    when (uiState.subtitleMethod) {
                        SubtitleMethod.DIRECT_AR -> {
                            Text("الترجمة العربية متوفرة لهذا الفيديو.", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.startSpecificDownload(DownloadMode.SUBTITLE_ONLY, SubtitleMethod.DIRECT_AR) },
                                modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Filled.Subtitles, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تحميل الترجمة مباشرة")
                            }
                        }
                        
                        SubtitleMethod.TRANSLATED_FROM_OTHER -> {
                            if (uiState.subtitleSearchStep == 0) {
                                Text("الترجمة للعربية غير متوفرة، هل تريد البحث عن ترجمة متوفرة وترجمتها للعربية؟", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = viewModel::performSubtitleSearch,
                                    modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(Icons.Filled.Search, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("البحث عن ترجمة متاحة")
                                }
                            } else {
                                val foreignSub = videoInfo.availableSubtitles.firstOrNull { it.languageCode != "ar" }
                                Text("تم العثور على ترجمة: ${foreignSub?.language ?: "أجنبية"}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.startSpecificDownload(DownloadMode.SUBTITLE_ONLY, SubtitleMethod.TRANSLATED_FROM_OTHER) },
                                    modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(Icons.Filled.Translate, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ترجمة الترجمة للعربية")
                                }
                            }
                        }

                        else -> { // WHISPER_GENERATED or NONE
                            Text("لا توجد أي ترجمة نصية متوفرة لهذا الفيديو.", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.startSpecificDownload(DownloadMode.SUBTITLE_ONLY, SubtitleMethod.WHISPER_GENERATED) },
                                modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Icon(Icons.Filled.Memory, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إنشاء الترجمة بالذكاء الاصطناعي")
                            }
                        }
                    }
                }
            }
        }
    }
}
