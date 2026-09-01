package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminSettingsEntity
import com.example.utils.VisualThemePalette
import kotlin.math.roundToInt

/**
 * 🌟 High-Craft Floating Action Buttons (FABs)
 * 1. "اطلب خدمتك الآن": Compact, vivid red gradient, instant 3-step wizard.
 * 2. "المساعد الذكي": Draggable Floating Action Button (FAB قابلة للسحب), royal indigo-blue gradient.
 */
@Composable
fun BoxScope.FloatingIconsOverlay(
    settings: AdminSettingsEntity,
    themeColors: VisualThemePalette,
    isClientUser: Boolean = true,
    onAssistantClick: () -> Unit,
    onRequestServiceClick: () -> Unit
) {
    // 1. Primary Action FAB: "اطلب خدمتك الآن"
    if (!settings.footerMessage.contains("hide_urgent_fab")) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 12.dp, bottom = 16.dp)
                .shadow(6.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFE11D48), Color(0xFFBE123C))
                    )
                )
                .clickable { onRequestServiceClick() }
                .border(1.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "اطلب خدمتك الآن",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "اطلب خدمتك الآن ⚡",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }

    // 2. Draggable Smart Assistant FAB (قائم للسحب بحرية على الشاشة)
    if (!settings.assistantHidden) {
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .padding(end = 12.dp, bottom = 16.dp)
                .shadow(6.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
                .clickable { onAssistantClick() }
                .border(1.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text("🤖", fontSize = 12.sp)
                Text(
                    text = "المساعد الذكي",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
