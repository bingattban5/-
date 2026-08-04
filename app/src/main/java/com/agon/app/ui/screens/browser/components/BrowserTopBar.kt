package com.agon.app.ui.screens.browser.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.agon.app.ui.screens.browser.state.BrowserState
import com.agon.app.ui.screens.browser.state.TabInfo

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
            // زر الرجوع (يظهر فقط إذا كان يمكن الرجوع)
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

            // شريط البحث الدائري
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
                    .height(44.dp)
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
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            if (activeTab?.isLoading == true) Icons.Filled.Refresh
                            else Icons.Filled.Search,
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
                            IconButton(
                                onClick = { searchText = "" },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "مسح",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
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
                    textStyle = MaterialTheme.typography.bodyMedium,
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

            // عداد التبويبات
            TabsCounterButton(
                count = state.tabsCount,
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
            .size(40.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            // إذا كان لدينا favicon، نعرضه كخلفية
            if (favicon != null) {
                AsyncImage(
                    model = favicon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // مربع برقم التبويبات
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (count > 99) "99+" else count.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
