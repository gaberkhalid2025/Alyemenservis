package com.example.util

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

/**
 * ⏱️ SessionManagerAndUxEnhancer - إدارة جلسات المستخدم وتحسين تجربة الاستخدام (UX)
 * 
 * المكونات:
 * 1. SessionTimeoutWarningModal: تنبيه انتهاء الجلسة عند الخمول مع إمكانية التمديد الفوري.
 * 2. OperationProgressModal: نافذة تقدم العمليات الثقيلة والرفع.
 * 3. CleanLogoutManager: تسجيل الخروج النظيف وتفريغ الذاكرة والرموز المؤقتة.
 */

// ==========================================
// 1. ⏱️ Session Timeout Warning Dialog
// ==========================================
@Composable
fun SessionTimeoutWarningModal(
    isVisible: Boolean,
    remainingSeconds: Int,
    onExtendSession: () -> Unit,
    onLogoutNow: () -> Unit,
    themeColors: VisualThemePalette
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = { /* إجبار المستخدم على الاختيار */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Session Warning",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تنبيه انتهاء الجلسة ⏱️", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "نظراً لعدم وجود نشاط مؤخراً، ستنتهي جلسة تسجيلك الحالية آلياً لحماية خصوصيتك خلال:",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Text(
                        text = "$remainingSeconds ثانية",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onExtendSession,
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("تمديد الجلسة الآن 🔄", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onLogoutNow) {
                    Text("تسجيل الخروج الآن 🚪", fontSize = 11.sp, color = Color(0xFFEF4444))
                }
            }
        )
    }
}

// ==========================================
// 2. 📊 Operation Progress Modal Overlay
// ==========================================
@Composable
fun OperationProgressModal(
    isVisible: Boolean,
    title: String,
    progress: Float,
    themeColors: VisualThemePalette
) {
    if (isVisible) {
        Surface(
            color = Color.Black.copy(alpha = 0.75f),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(32.dp).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Loading",
                            tint = themeColors.accent,
                            modifier = Modifier.size(32.dp)
                        )

                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = themeColors.accent,
                            trackColor = Color.DarkGray
                        )

                        Text(
                            text = "${(progress * 100).toInt()}% مكتمل",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. 🧼 Clean Logout Manager
// ==========================================
object CleanLogoutManager {

    /**
     * تنفيذ تسجيل خروج آمن وشامل وتفريغ بيانات الجلسة من الذاكرة
     * @param context سياق التطبيق
     * @param onComplete الإجراء المتبع بعد اكتمال التنظيف
     */
    fun executeCleanLogout(context: Context, onComplete: () -> Unit) {
        try {
            val prefs = context.getSharedPreferences("YS_Local_App_Cache_v2026", Context.MODE_PRIVATE)
            prefs.edit()
                .remove("USER_TOKEN")
                .remove("ADMIN_LOGGED")
                .remove("KEY_OFFLINE_QUEUE")
                .apply()

            onComplete()
        } catch (e: Exception) {
            onComplete()
        }
    }
}
