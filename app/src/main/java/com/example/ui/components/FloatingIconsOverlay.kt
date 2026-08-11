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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminSettingsEntity
import com.example.utils.VisualThemePalette

@Composable
fun BoxScope.FloatingIconsOverlay(
    settings: AdminSettingsEntity,
    themeColors: VisualThemePalette,
    onAssistantClick: () -> Unit,
    onRequestServiceClick: () -> Unit
) {
    // 1. Primary Action FAB: "اطلب خدمتك الآن" (Compact 30% smaller, placed slightly higher to not block map controls)
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 12.dp, bottom = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF10B981), Color(0xFF059669))
                )
            )
            .clickable { onRequestServiceClick() }
            .border(1.dp, Color.White, RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
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
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "اطلب خدمتك ⚡",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }

    // 2. Secondary FAB: "المساعد الذكي" (Compact 30% smaller, placed slightly higher)
    if (!settings.assistantHidden) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(themeColors.accent)
                .clickable { onAssistantClick() }
                .border(1.dp, Color.White, RoundedCornerShape(24.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("🤖", fontSize = 13.sp)
                Text(
                    text = "المساعد الذكي",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}
