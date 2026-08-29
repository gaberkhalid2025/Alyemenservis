package com.example.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.ChatMessage
import com.example.data.models.MediaType
import com.example.data.models.MessageStatus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    isMe: Boolean,
    onReplyClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isSupport = !isMe && (
        message.senderId.contains("admin", ignoreCase = true) ||
        message.senderId.contains("support", ignoreCase = true) ||
        message.senderName.contains("الدعم", ignoreCase = true) ||
        message.senderName.contains("إدارة", ignoreCase = true) ||
        message.senderName.contains("الادارة", ignoreCase = true)
    )

    val bubbleColor = when {
        isMe -> Color(0xFF1E88E5)
        isSupport -> Color(0xFF112618)
        else -> Color(0xFF1E293B)
    }

    val textColor = Color.White
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .then(
                    if (isSupport) {
                        Modifier.border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFFFD700), Color(0xFF10B981))
                            ),
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = 4.dp,
                                bottomEnd = 16.dp
                            )
                        )
                    } else Modifier
                )
                .background(bubbleColor)
                .clickable { onLongClick() }
                .padding(10.dp)
        ) {
            // Support badge
            if (isSupport) {
                Text(
                    text = "🛠️ الدعم الفني والإدارة",
                    fontSize = 10.sp,
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Replying quote preview
            if (!message.replyToText.isNullOrBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(28.dp)
                                .background(Color(0xFF64B5F6), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = message.replyToText,
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Media attachment
            when (message.mediaType) {
                MediaType.IMAGE -> {
                    if (message.mediaUrl.isNotBlank()) {
                        AsyncImage(
                            model = message.mediaUrl,
                            contentDescription = "صورة مرفقة",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
                MediaType.AUDIO -> {
                    var isPlaying by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "إيقاف" else "تشغيل",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = message.message.ifBlank { "مقطع صوتي 🎤" },
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { if (isPlaying) 0.65f else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFF64FFDA),
                                trackColor = Color.White.copy(alpha = 0.2f),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                MediaType.FILE -> {
                    Text("📎 ${message.message.ifBlank { "ملف مرفق" }}", color = Color(0xFF90CAF9), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {}
            }

            // Message text
            if (message.message.isNotBlank() && message.mediaType != MediaType.FILE) {
                Text(
                    text = message.message,
                    color = textColor,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Time & Status Indicators
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formattedTime,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                if (isMe) {
                    when (message.status) {
                        MessageStatus.PENDING, MessageStatus.SENDING -> {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "قيد الإرسال",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        MessageStatus.SENT -> {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "تم الإرسال",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        MessageStatus.DELIVERED -> {
                            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "تم الاستلام",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "تم الاستلام",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        MessageStatus.READ -> {
                            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "تمت القراءة",
                                    tint = Color(0xFF64FFDA),
                                    modifier = Modifier.size(13.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "تمت القراءة",
                                    tint = Color(0xFF64FFDA),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        MessageStatus.FAILED -> {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "فشل الإرسال",
                                tint = Color(0xFFFF8A80),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
