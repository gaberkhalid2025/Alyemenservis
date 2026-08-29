package com.example.ui.screens.map.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 📡 MapErrorOverlay
 * Fallback UI overlay displayed when map tiles fail to load over network.
 */
@Composable
fun MapErrorOverlay(
    onSwitchToRadar: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "📡 تعذر الاتصال بخرائط الإنترنت",
                    color = Color(0xFFFFD700),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "يمكنك التبديل إلى الرادار المحلي التفاعلي لاستعراض الخدمات بدون إنترنت.",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            }
            if (onSwitchToRadar != null) {
                Button(
                    onClick = onSwitchToRadar,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("الرادار المحلي", color = Color(0xFF0F172A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
