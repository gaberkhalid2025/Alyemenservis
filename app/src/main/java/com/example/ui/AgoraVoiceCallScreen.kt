package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ==============================================================================
// بيانات جلسة المكالمة الصوتية (Agora Call Session Model)
// ==============================================================================
data class AgoraCallState(
    val channelId: String = "",
    val callerName: String = "الفني / مركز الخدمة",
    val callerRole: String = "كهربائي وسيارات",
    val securityCode: String = "YEM-8921",
    val isConnected: Boolean = false,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val callDurationSeconds: Int = 0
)

// ==============================================================================
// واجهة شاشة المكالمة الصوتية المنبثقة (VoIP Call UI Screen)
// ==============================================================================
@Composable
fun AgoraVoiceCallScreen(
    callState: AgoraCallState,
    onMuteToggle: (Boolean) -> Unit,
    onSpeakerToggle: (Boolean) -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSecurityCode by remember { mutableStateOf(false) }

    // عداد وقت المكالمة عند الاتصال
    var seconds by remember { mutableIntStateOf(callState.callDurationSeconds) }
    LaunchedEffect(callState.isConnected) {
        if (callState.isConnected) {
            while (true) {
                delay(1000L)
                seconds++
            }
        }
    }

    val formattedTime = remember(seconds) {
        val m = seconds / 60
        val s = seconds % 60
        String.format(java.util.Locale.US, "%02d:%02d", m, s)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp)
    ) {
        // --- الجزء العلوي: تفاصيل المتصل والحالة ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // صورة المتصل / الشعار
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // اسم وتخصص المتصل
            Text(
                text = callState.callerName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = callState.callerRole,
                fontSize = 14.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // حالة الاتصال والعداد الزمني
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (callState.isConnected) Color(0xFF064E3B) else Color(0xFF1E293B)
            ) {
                Text(
                    text = if (callState.isConnected) "متصل الآن • $formattedTime" else "جاري الاتصال عبر Agora...",
                    color = if (callState.isConnected) Color(0xFF34D399) else Color(0xFFFBBF24),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // --- الجزء الأوسط: عرض كود الضامن للأمان ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { showSecurityCode = !showSecurityCode },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "🛡️",
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (showSecurityCode) "إخفاء كود الضامن" else "إظهار كود الضامن (التحقق)",
                    color = Color.White,
                    fontSize = 13.sp
                )
            }

            AnimatedVisibility(visible = showSecurityCode) {
                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(0.80f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "رمز الضامن الخاص بالمهمة",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = callState.securityCode,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }

        // --- الجزء السفلي: أزرار التحكم بالمكالمة ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // زر كتم الصوت (Mute)
            IconButton(
                onClick = { onMuteToggle(!callState.isMuted) },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (callState.isMuted) Color(0xFFEF4444) else Color(0xFF1E293B))
            ) {
                Text(
                    text = if (callState.isMuted) "🔇" else "🎙️",
                    fontSize = 22.sp
                )
            }

            // زر إنهاء المكالمة (End Call)
            IconButton(
                onClick = onEndCall,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDC2626))
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // زر المكبر (Speaker)
            IconButton(
                onClick = { onSpeakerToggle(!callState.isSpeakerOn) },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (callState.isSpeakerOn) Color(0xFF0284C7) else Color(0xFF1E293B))
            ) {
                Text(
                    text = if (callState.isSpeakerOn) "🔊" else "🔈",
                    fontSize = 22.sp
                )
            }
        }
    }
}
