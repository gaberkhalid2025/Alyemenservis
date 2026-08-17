package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminSettingsEntity
import com.example.utils.VisualThemePalette

/**
 * 🌟 High-Craft Floating Action Buttons (FABs)
 * 1. "اطلب خدمتك الآن": 30% compact, vivid red gradient, instant reverse marketplace request.
 * 2. "المساعد الذكي": 30% compact, royal indigo-blue gradient, AI & offline assistance.
 */
@Composable
fun BoxScope.FloatingIconsOverlay(
    settings: AdminSettingsEntity,
    themeColors: VisualThemePalette,
    onAssistantClick: () -> Unit,
    onRequestServiceClick: () -> Unit
) {
    // 1. Primary Action FAB: "اطلب خدمتك الآن" (Compact 30% smaller, vivid red gradient)
    if (!settings.footerMessage.contains("hide_urgent_fab")) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, bottom = 10.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFE11D48), Color(0xFFBE123C))
                    )
                )
                .clickable { onRequestServiceClick() }
                .border(0.9.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                .padding(horizontal = 8.dp, vertical = 4.5.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "اطلب خدمتك الآن",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "اطلب خدمتك الآن ⚡",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }

    // 2. Secondary FAB: "المساعد الذكي" (Compact 30% smaller, indigo-blue gradient)
    if (!settings.assistantHidden) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 10.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                    )
                )
                .clickable { onAssistantClick() }
                .border(0.9.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                .padding(horizontal = 8.dp, vertical = 4.5.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("🤖", fontSize = 10.sp)
                Text(
                    text = "المساعد الذكي",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

