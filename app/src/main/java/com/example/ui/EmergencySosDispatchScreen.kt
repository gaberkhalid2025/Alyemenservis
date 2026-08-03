package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class EmergencySosState(
    val isEmergencyActive: Boolean = false,
    val timeRemainingSeconds: Int = 180,
    val nearbyRespondersCount: Int = 7
)

@Composable
fun EmergencySosTriggerBar(
    onTriggerSos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SosGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    Surface(
        onClick = onTriggerSos,
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .border(2.dp, Color(0xFFEF4444).copy(alpha = glowAlpha), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF7F1D1D),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDC2626)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "SOS",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "طلب طوارئ عاجل 🚨",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "بث مباشر لأقرب الفنيين والمراكز المتاحة",
                        fontSize = 11.sp,
                        color = Color(0xFFFCA5A5)
                    )
                }
            }

            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFDC2626)) {
                Text(
                    text = "نداء SOS",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun EmergencySosDispatchScreen(
    sosState: EmergencySosState = EmergencySosState(),
    onCancelSos: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var timer by remember { mutableIntStateOf(sosState.timeRemainingSeconds) }

    LaunchedEffect(sosState.isEmergencyActive) {
        while (timer > 0) {
            delay(1000L)
            timer--
        }
    }

    val formattedTime = remember(timer) {
        val m = timer / 60
        val s = timer % 60
        String.format(java.util.Locale.US, "%02d:%02d", m, s)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "SosPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRadius"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFFEF4444).copy(alpha = (1f - (pulseRadius / 300f)).coerceIn(0f, 1f)),
                radius = pulseRadius,
                style = Stroke(width = 6.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDC2626)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "جاري بث نداء الطوارئ...",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "تم تنبيه ${sosState.nearbyRespondersCount} مقدم خدمة في محيطك الجغرافي",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "الوقت المتبقي لانتهاء مهلة النداء",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedTime,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
            OutlinedButton(
                onClick = onCancelSos,
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFEF4444))
                ),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("إلغاء نداء الطوارئ")
            }
        }
    }
}
