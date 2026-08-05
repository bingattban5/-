package com.agon.app.ui.screens.browser.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.agon.app.ui.screens.browser.state.BrowserState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserTopBar(
    state: BrowserState,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onTabsClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val activeTab = state.activeTab
    val context = LocalContext.current
    var searchText by remember(activeTab?.url) { 
        mutableStateOf(activeTab?.url ?: "") 
    }
    var isFocused by remember { mutableStateOf(false) }
    
    val glowColor = MaterialTheme.colorScheme.primary.copy(
        alpha = if (isFocused) 0.3f else 0f
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // زر الرجوع
            AnimatedContent(
                targetState = state.canGoBack,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "backButton"
            ) { canGoBack ->
                if (canGoBack) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // شريط البحث والرابط الدائري
            Box(
                modifier = Modifier
                    .weight(1f)
                    .shadow(
                        elevation = if (isFocused) 8.dp else 2.dp,
                        shape = RoundedCornerShape(50),
                        ambientColor = glowColor,
                        spotColor = glowColor
                    )
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .height(48.dp)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .onFocusChanged { isFocused = it.isFocused },
                    placeholder = { 
                        Text(
                            "ابحث أو أدخل رابطاً...",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            if (activeTab?.isLoading == true) Icons.Filled.Refresh 
                            else Icons.Filled.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = if (activeTab?.isLoading == true) {
                                Modifier
                                    .size(20.dp)
                                    .clickable(onClick = onStop)
                            } else Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // زر النسخ السريع للرابط
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("URL", searchText)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "تم نسخ الرابط", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.ContentCopy,
                                        contentDescription = "نسخ الرابط",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                // زر مسح النص
                                IconButton(
                                    onClick = { searchText = "" },
                                    modifier = Modifier.size(32.dp).padding(end = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "مسح",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            onNavigate(searchText)
                            isFocused = false
                        }
                    )
                )

                // شريط التقدم أثناء التحميل
                if (activeTab?.isLoading == true) {
                    LinearProgressIndicator(
                        progress = { (activeTab.progress / 100f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
            }

            // عداد التبويبات المفتوحة (يعرض الـ Favicon في الخلفية أو الرقم)
            TabsCounterButton(
                count = state.tabsCount, // تأكد أن state.tabsCount يتم تمريرها بشكل صحيح من الـ State
                favicon = activeTab?.favicon,
                onClick = onTabsClick
            )

            // زر القائمة
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "القائمة",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun TabsCounterButton(
    count: Int,
    favicon: String?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            if (favicon != null && count > 0) {
                AsyncImage(
                    model = favicon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            }
            
            Text(
                text = if (count > 99) "99+" else count.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (favicon != null) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
