package com.agon.app.ui.screens.browser.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                text = link,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DropdownMenuItem(
                    text = { Text("فتح في تبويب جديد (خلفي)") },
                    onClick = {
                        onOpenInBackground()
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Tab, contentDescription = null)
                    }
                )

                DropdownMenuItem(
                    text = { Text("نسخ الرابط") },
                    onClick = {
                        onCopyLink()
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null)
                    }
                )

                DropdownMenuItem(
                    text = { Text("مشاركة الرابط") },
                    onClick = {
                        onShareLink()
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Share, contentDescription = null)
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
