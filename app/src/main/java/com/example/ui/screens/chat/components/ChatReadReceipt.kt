package com.example.ui.screens.chat.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ChatReadReceipt(
    isRead: Boolean,
    modifier: Modifier = Modifier
) {
    val tint = if (isRead) Color(0xFF3B82F6) else Color.Gray
    
    Row(modifier = modifier.padding(start = 4.dp)) {
        Icon(
            imageVector = Icons.Default.Done,
            contentDescription = if (isRead) "تم القراءة" else "تم الإرسال",
            tint = tint,
            modifier = Modifier.padding(2.dp)
        )
    }
}
