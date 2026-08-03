package com.agon.app.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun AppPermissionHandler(
    onPermissionsGranted: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    // تحديد الصلاحيات المطلوبة بناءً على إصدار الأندرويد
    val requiredPermissions = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.READ_MEDIA_VIDEO)
            add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            // للأصدارات الأقدم (أندرويد 12 وما دون)
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    var showRationale by remember { mutableStateOf(false) }
    var isPermanentlyDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            onPermissionsGranted()
        } else {
            // التحقق مما إذا كان المستخدم قد رفض الصلاحية بشكل نهائي (Don't ask again)
            isPermanentlyDenied = permissions.any { (permission, isGranted) ->
                !isGranted && activity?.shouldShowRequestPermissionRationale(permission) == false
            }
            showRationale = true
        }
    }

    // طلب الصلاحيات فور تشغيل المكون لأول مرة
    LaunchedEffect(Unit) {
        val notGranted = requiredPermissions.any { 
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED 
        }
        
        if (notGranted) {
            permissionLauncher.launch(requiredPermissions)
        } else {
            onPermissionsGranted()
        }
    }

    // إذا تم منح الصلاحيات (أو كانت ممنوحة مسبقاً)، اعرض محتوى التطبيق
    if (!showRationale) {
        content()
    } else {
        // في حال الرفض، اعرض شاشة التوضيح
        PermissionRationaleScreen(
            isPermanentlyDenied = isPermanentlyDenied,
            onRetry = { 
                showRationale = false
                permissionLauncher.launch(requiredPermissions)
            },
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
    }
}

@Composable
private fun PermissionRationaleScreen(
    isPermanentlyDenied: Boolean,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "صلاحيات مطلوبة",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isPermanentlyDenied) {
                "تم رفض الصلاحيات بشكل نهائي. يرجى الذهاب إلى إعدادات التطبيق ومنح صلاحيات (الإشعارات والوسائط) يدوياً ليعمل التطبيق بشكل صحيح."
            } else {
                "يحتاج هذا التطبيق إلى بعض الصلاحيات (مثل الإشعارات والوصول للوسائط) ليعمل بشكل صحيح وإدارة عمليات التحميل والترجمة."
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isPermanentlyDenied) {
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("فتح الإعدادات")
                }
            } else {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إعادة المحاولة")
                }
            }
        }
    }
}
