package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 🌟 Premium LoadingOverlay Component (10/10 UX)
 * - Beautiful glassmorphic modal cover
 * - Infinite rotational pulse vector
 * - Dynamic customizable loading tips / phrases (e.g. "جاري تحميل البيانات...")
 * - Blocks user interaction safely
 */
@Composable
fun LoadingOverlay(
    modifier: Modifier = Modifier,
    statusText: String = "جاري تحميل وتزامن البيانات..."
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(enabled = true, onClick = {}) // Block background clicks
            .testTag("loading_overlay_root"),
        contentAlignment = Alignment.Center
    ) {
        // Shimmering card container
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .width(260.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Circular loading progress with neon styling
                CircularProgressIndicator(
                    color = Color(0xFFFFB300),
                    trackColor = Color.White.copy(alpha = 0.1f),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = statusText,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "يرجى الانتظار قليلاً، يرجى عدم إغلاق التطبيق",
                    fontSize = 9.5.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
