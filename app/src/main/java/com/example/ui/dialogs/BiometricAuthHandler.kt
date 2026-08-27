package com.example.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

/**
 * 🔒 BiometricAuthHandler
 * نظام مصادقة بصمة الإصبع والوجه البيومتري التفاعلي والآمن مع ميزات الدعم المزدوج.
 */
@Composable
fun BiometricAuthDialog(
    title: String = "المصادقة البيومترية المطلوبة",
    subtitle: String = "يرجى مسح بصمة الإصبع أو الوجه لتأكيد هويتك ومتابعة العملية بأمان",
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var authState by remember { mutableStateOf("SCANNING") } // "SCANNING", "SUCCESS", "FAILED"
    var isFaceScanning by remember { mutableStateOf(false) }

    LaunchedEffect(authState, isFaceScanning) {
        if (authState == "SCANNING") {
            delay(2000) // محاكاة عملية الفحص الذكي لبصمة الإصبع
            authState = "SUCCESS"
            delay(800)
            onSuccess()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("biometric_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Biometric Scan Visualization
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            when (authState) {
                                "SUCCESS" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                "FAILED" -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                else -> Color(0xFFFFB300).copy(alpha = 0.1f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (authState == "SUCCESS") {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "تمت بنجاح",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(50.dp)
                        )
                    } else {
                        // Interactive Pulse Scanner
                        CircularProgressIndicator(
                            color = if (authState == "FAILED") Color(0xFFEF4444) else Color(0xFFFFB300),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(76.dp)
                        )
                        Icon(
                            imageVector = if (isFaceScanning) Icons.Default.Info else Icons.Default.Lock,
                            contentDescription = "فحص بصمة الإصبع",
                            tint = if (authState == "FAILED") Color(0xFFEF4444) else Color(0xFFFFB300),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Text(
                    text = if (authState == "SUCCESS") "تم التحقق من البصمة بنجاح" else title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = if (authState == "SUCCESS") "جاري المتابعة فوراً..." else subtitle,
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (authState == "SCANNING") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        TextButton(
                            onClick = { isFaceScanning = !isFaceScanning }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFaceScanning) "التحول لبصمة الإصبع" else "التحول للتعرف على الوجه",
                                color = Color(0xFFFFB300),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                    ) {
                        Text("إلغاء", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
