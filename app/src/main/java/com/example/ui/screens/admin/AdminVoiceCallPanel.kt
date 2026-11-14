package com.example.ui.screens.admin

import androidx.compose.foundation.layout.*
import com.example.ui.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

/**
 * 🎙️ إدارة ميزة المكالمات الصوتية والاتصال السحابي (Voice Calls Admin Panel)
 * تتيح التحكم بمكتبات الاتصال الصوتي المستقبلية وحالة التفعيل لتوفير حجم الحزمة والبيانات
 */
@Composable
fun AdminVoiceCallPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val cardBg = themeColors.surface
    val textColor = themeColors.textPrimary
    val primaryColor = themeColors.primary

    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { com.google.firebase.firestore.FirebaseFirestore.getInstance() }
    var voiceCallsEnabled by remember { mutableStateOf(false) }
    var highQualityAudio by remember { mutableStateOf(false) }
    var recordCallsEnabled by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        db.collection("settings").document("voice_calls").get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    voiceCallsEnabled = doc.getBoolean("voiceCallsEnabled") ?: false
                    highQualityAudio = doc.getBoolean("highQualityAudio") ?: false
                    recordCallsEnabled = doc.getBoolean("recordCallsEnabled") ?: false
                }
            }
    }

    fun persistVoiceSettings(enabled: Boolean, hq: Boolean, rec: Boolean) {
        isSaving = true
        val payload = mapOf(
            "voiceCallsEnabled" to enabled,
            "highQualityAudio" to hq,
            "recordCallsEnabled" to rec,
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection("settings").document("voice_calls").set(payload)
            .addOnSuccessListener {
                isSaving = false
                android.widget.Toast.makeText(context, "تم حفظ إعدادات المكالمات الصوتية ✅", android.widget.Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                isSaving = false
                android.widget.Toast.makeText(context, "تعذر حفظ الإعدادات: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
            }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        "إدارة ميزة المكالمات الصوتية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        "التحكم بمحرك الاتصال الصوتي المباشر بين العملاء والفنيين",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = textColor.copy(alpha = 0.1f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "تفعيل المكالمات الصوتية (مكتبة Agora)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = textColor
                    )
                    Text(
                        "إتاحة زر الاتصال الصوتي عبر الإنترنت بالدردشة والطلبات",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = voiceCallsEnabled,
                    enabled = !isSaving,
                    onCheckedChange = { newVal ->
                        voiceCallsEnabled = newVal
                        persistVoiceSettings(newVal, highQualityAudio, recordCallsEnabled)
                    }
                )
            }

            if (voiceCallsEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("جودة الصوت العالية (HD Audio)", fontSize = 13.sp, color = textColor)
                    Switch(
                        checked = highQualityAudio,
                        enabled = !isSaving,
                        onCheckedChange = { newHq ->
                            highQualityAudio = newHq
                            persistVoiceSettings(voiceCallsEnabled, newHq, recordCallsEnabled)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = if (voiceCallsEnabled) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = if (voiceCallsEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (voiceCallsEnabled) {
                            "المكالمات الصوتية مفعلة حالياً ومحفوظة على الخادم."
                        } else {
                            "ملاحظة: الميزة معطلة حالياً لتوفير مساحة وحجم التطبيق وسرعة التحميل على شبكات الإنترنت اليمنية."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (voiceCallsEnabled) textColor else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
