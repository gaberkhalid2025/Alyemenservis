package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 🌟 SplashScreen
 * شاشة البداية والترحيب الاحترافية لتطبيق دليل خدمات اليمن.
 */
@Composable
fun SplashScreen(
    welcomeMessage: String = ""
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF000000)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        ) {
            // الدائرة والشعار WAM
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1D58B8), Color(0xFF0E2C60))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "WAM",
                    color = Color(0xFF00DC82),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 36.sp,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // العنوان الرئيسي
            Text(
                text = "كل خدمات اليمن",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // العنوان الفرعي
            Text(
                text = "دليلك الشامل لكل الخدمات والمهن في اليمن 🇾🇪",
                fontSize = 15.sp,
                color = Color(0xFFD1D5DB),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(38.dp))

            // مؤشر التحميل الأخضر/الفيروزي
            CircularProgressIndicator(
                color = Color(0xFF00DC82),
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(38.dp))

            // النص الترحيبي التعريفي
            val displayMessage = if (welcomeMessage.isNotBlank()) {
                welcomeMessage
            } else {
                "التطبيق الأول في اليمن الذي يربط مقدمي الخدمات وأصحاب المهن بالمستخدمين والعملاء فورياً وبكل موثوقية."
            }

            Text(
                text = displayMessage,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
