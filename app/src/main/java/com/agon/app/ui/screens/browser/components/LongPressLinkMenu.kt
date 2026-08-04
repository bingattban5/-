package com.agon.app.ui.screens.browser.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun LongPressLinkMenu(
    link: String,
    onOpenInBackground: () -> Unit,
    onCopyLink: () -> Unit,
    onShareLink: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "خيارات الرابط",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // عرض الرابط المختصر
                Text(
                    text = link,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // خيار: فتح في خلفية
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenInBackground() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("فتح في تبويب جديد (خلفية)", style = MaterialTheme.typography.bodyLarge)
                }

                HorizontalDivider()

                // خيار: نسخ الرابط
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCopyLink() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("نسخ الرابط", style = MaterialTheme.typography.bodyLarge)
                }

                HorizontalDivider()

                // خيار: مشاركة الرابط
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShareLink() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("مشاركة الرابط", style = MaterialTheme.typography.bodyLarge)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
