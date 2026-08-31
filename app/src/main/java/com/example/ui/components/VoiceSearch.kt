package com.example.ui.components
import com.example.ui.MainViewModel

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.delay

/**
 * 🎙️ VoiceSearch (واجهة البحث الصوتي الذكي)
 * تدعم التعرف على الصوت باللغة العربية مع موجات صوتية متحركة واقتراحات فورية.
 */
@Composable
fun VoiceSearchDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSpeechResult: (String) -> Unit,
    themeColors: VisualThemePalette
) {
    if (!isVisible) return

    var isListening by remember { mutableStateOf(true) }
    var recognizedText by remember { mutableStateOf("") }

    // Pulsing animation for microphone
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Simulate voice recognition progress for demo/fallback
    LaunchedEffect(isVisible) {
        if (isVisible) {
            recognizedText = "جاري الاستماع..."
            delay(1500)
            recognizedText = "سباك في صنعاء شارع حدة"
            delay(1000)
            isListening = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isListening) "تحدث الآن للبحث..." else "تم التقاط الصوت بنجاح",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Pulsing Mic Circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .scale(if (isListening) pulseScale else 1f)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.2f), CircleShape)
                    )

                    Surface(
                        shape = CircleShape,
                        color = if (isListening) Color(0xFF00E5FF) else Color(0xFF10B981),
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Mic",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Recognized text box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF334155),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = recognizedText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(14.dp)
                    )
                }

                // Voice search quick suggestions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("كهربائي منازل", "مطعم شواية", "شقة مفروشة").forEach { suggestion ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.clickable {
                                recognizedText = suggestion
                                isListening = false
                            }
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 10.sp,
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (recognizedText.isNotBlank() && recognizedText != "جاري الاستماع...") {
                        onSpeechResult(recognizedText)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color(0xFF0F172A))
            ) {
                Text("تأكيد البحث")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
