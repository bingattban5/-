package com.agon.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class CircleMenuItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color
)

@Composable
fun StackedCirclesMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    if (!expanded) return

    val menuItems = listOf(
        CircleMenuItem(
            route = "downloads",
            label = "التنزيلات",
            icon = Icons.Filled.Download,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        CircleMenuItem(
            route = "files",
            label = "مدير الملفات",
            icon = Icons.Filled.FolderOpen,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ),
        CircleMenuItem(
            route = "models",
            label = "النماذج",
            icon = Icons.Filled.Engineering,
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary
        ),
        CircleMenuItem(
            route = "settings",
            label = "الإعدادات",
            icon = Icons.Filled.Settings,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )

    // خلفية شفافة قابلة للضغط لإغلاق القائمة
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 70.dp, end = 16.dp), // موضع القائمة أسفل شريط الأدوات
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            menuItems.forEachIndexed { index, item ->
                AnimatedVisibility(
                    visible = expanded,
                    enter = slideInVertically(
                        initialOffsetY = { -50 },
                        animationSpec = tween(durationMillis = 300, delayMillis = index * 50)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 300, delayMillis = index * 50)
                    ) + scaleIn(
                        initialScale = 0.8f,
                        animationSpec = tween(durationMillis = 300, delayMillis = index * 50)
                    ),
                    exit = fadeOut(animationSpec = tween(durationMillis = 150))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        // النص بجانب الدائرة
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 4.dp,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = item.label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // الدائرة
                        Surface(
                            shape = CircleShape,
                            color = item.containerColor,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .size(56.dp)
                                .clickable {
                                    onNavigate(item.route)
                                    onDismiss()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = item.contentColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}