package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.utils.VisualThemePalette

/**
 * 📢 Standardized 10/10 Reusable Dialogs for "دليل خدمات اليمن"
 * Centralized to streamline maintenance, consistent Material 3 styling, and clean RTL support.
 */

@Composable
fun YemenAlertDialog(
    title: String,
    message: String,
    confirmText: String = "حسناً",
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 13.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                modifier = Modifier.testTag("dialog_alert_confirm")
            ) {
                Text(confirmText, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF1E293B),
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
fun YemenConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "تأكيد",
    cancelText: String = "إلغاء",
    themeColors: VisualThemePalette,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 13.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                modifier = Modifier.testTag("dialog_confirm_ok")
            ) {
                Text(confirmText, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_confirm_cancel")
            ) {
                Text(cancelText, color = Color.LightGray, fontSize = 12.sp)
            }
        },
        containerColor = Color(0xFF1E293B),
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
fun YemenLoadingDialog(
    statusText: String = "جاري تنفيذ العملية..."
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier
                .width(240.dp)
                .testTag("dialog_loading_card")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFFFB300),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun DialogsContainer() {
    // Top-level container can listen to a shared dialog state flow if required.
}
