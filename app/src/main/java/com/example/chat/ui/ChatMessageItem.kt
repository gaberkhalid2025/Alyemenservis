package com.example.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chat.domain.ChatMessage
import com.example.chat.domain.MessageStatus
import com.example.chat.domain.MessageType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 💬 ChatMessageItem
 * A generic, optimized Compose component for rendering a single chat bubble.
 * Supports Text, Image Thumbnails, and Audio Chunks cleanly.
 */
@Composable
fun ChatMessageItem(
    message: ChatMessage,
    isMe: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 0.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 16.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .background(color = backgroundColor, shape = shape)
                .padding(12.dp)
                .widthIn(min = 80.dp, max = 280.dp)
        ) {
            // Message Body Based on Type
            when (message.type) {
                MessageType.TEXT -> {
                    Text(text = message.content, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                MessageType.IMAGE -> {
                    // Placeholder for AsyncImage loading the thumbnail URL
                    Text(text = "📷 صورة", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                MessageType.AUDIO -> {
                    // Placeholder for Audio Player Waveform
                    val secs = message.durationMillis?.div(1000) ?: 0
                    Text(text = "🎵 مقطع صوتي ($secs ث)", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                MessageType.DOCUMENT -> {
                    Text(text = "📄 مستند: ${message.content}", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            // Timestamp and Read Receipts
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val timeString = timeFormat.format(Date(message.timestamp))
                
                Text(
                    text = timeString,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                
                if (isMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    val tint = if (message.status == MessageStatus.READ) Color(0xFF34B7F1) else Color.Gray // WhatsApp Blue for read
                    
                    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Status",
                            modifier = Modifier.size(14.dp),
                            tint = tint
                        )
                        if (message.status == MessageStatus.DELIVERED || message.status == MessageStatus.READ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Status",
                                modifier = Modifier.size(14.dp),
                                tint = tint
                            )
                        }
                    }
                }
            }
        }
    }
}
