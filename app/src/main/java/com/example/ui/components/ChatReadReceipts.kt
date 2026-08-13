package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChatReadReceipts(
    status: String // SENT, DELIVERED, READ
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val (icon, color) = when (status.uppercase()) {
            "SENT" -> Pair("✓", Color.Gray)
            "DELIVERED" -> Pair("✓✓", Color.Gray)
            "READ" -> Pair("✓✓", Color(0xFF00B0FF)) // Blue read checkmarks
            else -> Pair("✓", Color.Gray)
        }

        Text(
            text = icon,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
