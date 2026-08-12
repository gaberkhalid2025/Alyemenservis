package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChatTypingIndicator(
    userName: String = "الطرف الآخر",
    tintColor: Color = Color(0xFF00B0FF)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "$userName يكتب الآن",
            fontSize = 11.sp,
            color = Color.LightGray
        )

        val infiniteTransition = rememberInfiniteTransition(label = "typing")
        val anim1 by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot1"
        )
        val anim2 by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, delayMillis = 150, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot2"
        )
        val anim3 by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, delayMillis = 300, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot3"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(tintColor.copy(alpha = anim1)))
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(tintColor.copy(alpha = anim2)))
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(tintColor.copy(alpha = anim3)))
        }
    }
}
