package com.example.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.ChatMessage
import com.example.data.models.MediaType
import com.example.data.models.MessageStatus
import com.example.util.VoiceNoteManager
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    isMe: Boolean,
    voiceNoteManager: VoiceNoteManager? = null,
    onReplyClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bubbleColor = if (message.isDeleted) {
        Color(0xFF263238)
    } else if (isMe) {
        Color(0xFF1E88E5)
    } else {
        Color(0xFF1E293B)
    }

    val textColor = if (message.isDeleted) Color.LightGray else Color.White
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    val currentlyPlayingUrl by voiceNoteManager?.currentlyPlayingUrl?.collectAsState() ?: remember { mutableStateOf(null) }
    val isPlayingThisAudio = currentlyPlayingUrl == message.mediaUrl && message.mediaUrl.isNotBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .clickable { onLongClick() }
                .padding(10.dp)
        ) {
            // Deleted message banner
            if (message.isDeleted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Text(
                        text = "تم حذف هذه الرسالة",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            } else {
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
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                    MediaType.AUDIO -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play/Pause button
                            IconButton(
                                onClick = {
                                    if (message.mediaUrl.isNotBlank()) {
                                        voiceNoteManager?.playAudio(message.mediaUrl)
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (isPlayingThisAudio) Color(0xFFEF5350) else Color(0xFF64B5F6), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isPlayingThisAudio) Icons.Default.Close else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlayingThisAudio) "إيقاف" else "تشغيل",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isPlayingThisAudio) "جارِ الاستماع..." else "تسجيل صوتي",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    val durSec = message.mediaDurationSeconds
                                    val durationText = if (durSec > 0) {
                                        String.format("%02d:%02d", durSec / 60, durSec % 60)
                                    } else "00:00"
                                    Text(
                                        text = durationText,
                                        color = Color(0xFF90CAF9),
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { if (isPlayingThisAudio) 0.6f else 0f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = Color(0xFF64B5F6),
                                    trackColor = Color.White.copy(alpha = 0.2f),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    MediaType.FILE -> {
                        Surface(
                            color = Color.Black.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF90CAF9))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message.message.ifBlank { "ملف مرفق" },
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    else -> {}
                }

                // Message text
                if (message.message.isNotBlank() && message.mediaType != MediaType.FILE && message.mediaType != MediaType.AUDIO) {
                    Text(
                        text = message.message,
                        color = textColor,
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp
                    )
                }
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

                if (isMe && !message.isDeleted) {
                    when (message.status) {
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
                    }
                }
            }
        }
    }
}
