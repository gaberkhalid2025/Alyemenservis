package com.example.ui.screens.map

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VoiceState
import com.example.utils.VoiceStatus

/**
 * 🎙️ VoiceSearchDialog - نافذة الاستماع للبحث الصوتي داخل التطبيق بدون فتح شاشات خارجية
 */
@Composable
fun VoiceSearchDialog(
    state: VoiceState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onUseKeyboard: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = Color(0xFF0F172A),
        title = {
            Text(
                if (state.status == VoiceStatus.LISTENING) "🎙️ تحدث الآن…" else "البحث الصوتي",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.status == VoiceStatus.LISTENING) {
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color(0xFF00E5FF))
                    Text(
                        state.partial.ifBlank { "أستمع إليك… (مثال: شارع الزبيري، جولة حدة)" },
                        color = Color(0xFFCBD5E1),
                        fontSize = 15.sp
                    )
                }
                state.message?.let {
                    Text(it, color = Color(0xFFF59E0B), fontSize = 13.sp)
                }
                if (state.status == VoiceStatus.ERROR) {
                    if (state.canUseKeyboard) {
                        OutlinedButton(
                            onClick = onUseKeyboard,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("⌨️ استعمل ميكروفون لوحة المفاتيح", color = Color(0xFF10B981))
                        }
                    }
                    if (state.canOpenSettings) {
                        OutlinedButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("⚙️ إعدادات الإدخال الصوتي", color = Color.White)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (state.status == VoiceStatus.ERROR) {
                TextButton(onClick = onRetry) {
                    Text("إعادة المحاولة", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("إغلاق", color = Color(0xFF94A3B8))
            }
        }
    )
}
