package com.example.chat.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.chat.domain.ChatMessageModel
import com.example.chat.domain.MessageType
import com.example.chat.utils.AttachmentManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 💬 MessageItemComposable
 * Production-ready, ultra-optimized chat bubble composable for LazyColumn.
 * Handles:
 * - Text with reply banner
 * - Voice notes with playback controls & progress
 * - Image & Video thumbnails with preview action
 * - Documents (PDF, Word, Excel) with file size & open intent
 * - Contacts with direct call button
 * - Location pins with Google Maps navigation
 * - Swipe-to-Reply gesture
 */
@Composable
fun MessageItemComposable(
    message: ChatMessageModel,
    isMe: Boolean,
    isPlayingAudio: Boolean = false,
    audioProgress: Float = 0f,
    onPlayAudioClick: (String, String) -> Unit = { _, _ -> },
    onMediaClick: (ChatMessageModel) -> Unit = {},
    onReplySwipe: (ChatMessageModel) -> Unit = {},
    onRetrySend: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var swipeOffset by remember { mutableFloatStateOf(0f) }

    val bubbleBg = if (isMe) Color(0xFF0284C7) else Color(0xFF1E293B)
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart

    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .offset { IntOffset(swipeOffset.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (swipeOffset > 80f) {
                            onReplySwipe(message)
                        }
                        swipeOffset = 0f
                    },
                    onDragCancel = { swipeOffset = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        swipeOffset = (swipeOffset + dragAmount).coerceIn(0f, 120f)
                    }
                )
            },
        contentAlignment = alignment
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleBg,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(min = 90.dp, max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {

                // 1. Quoted Reply Header (if replying to a message)
                if (!message.replyToText.isNullOrBlank()) {
                    QuotedMessagePreview(
                        senderName = message.replyToSenderName ?: "مستخدم",
                        quotedText = message.replyToText,
                        isMe = isMe
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 2. Message Payload Content by Type
                when (message.type) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.content,
                            fontSize = 14.5.sp,
                            color = Color.White,
                            lineHeight = 20.sp
                        )
                    }

                    MessageType.IMAGE -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onMediaClick(message) }
                        ) {
                            val imgUrl = message.thumbnailUrl ?: message.mediaUrl
                            AsyncImage(
                                model = imgUrl,
                                contentDescription = "صورة",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        if (message.content.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = message.content, fontSize = 13.5.sp, color = Color.White)
                        }
                    }

                    MessageType.VIDEO -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onMediaClick(message) },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = message.thumbnailUrl ?: message.mediaUrl,
                                contentDescription = "فيديو",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "تشغيل",
                                    tint = Color.White,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    MessageType.AUDIO -> {
                        VoiceNoteBubble(
                            messageId = message.id,
                            audioUrl = message.mediaUrl ?: message.localFilePath ?: "",
                            durationMs = message.durationMillis ?: 0L,
                            isPlaying = isPlayingAudio,
                            progress = audioProgress,
                            onPlayClick = { onPlayAudioClick(message.id, it) },
                            isMe = isMe
                        )
                    }

                    MessageType.DOCUMENT, MessageType.PDF -> {
                        DocumentBubble(
                            fileName = message.fileName.ifBlank { "مستند مرفق" },
                            fileSize = message.fileSize,
                            isPdf = message.type == MessageType.PDF,
                            onOpenClick = {
                                val url = message.mediaUrl ?: message.localFilePath
                                if (!url.isNullOrBlank()) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(Intent.createChooser(intent, "فتح المستند"))
                                }
                            }
                        )
                    }

                    MessageType.CONTACT -> {
                        ContactBubble(
                            name = message.contactName ?: "جهة اتصال",
                            phone = message.contactPhone ?: "",
                            onCallClick = { phone ->
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            }
                        )
                    }

                    MessageType.LOCATION -> {
                        LocationBubble(
                            lat = message.latitude ?: 0.0,
                            lng = message.longitude ?: 0.0,
                            label = message.content.ifBlank { "موقع مشارك" },
                            onMapClick = { lat, lng ->
                                val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(موقع)")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                // 3. Bottom Meta: Timestamp & Read Receipt
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    val timeStr = sdf.format(Date(message.timestamp))

                    Text(
                        text = timeStr,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )

                    if (isMe) {
                        ReadReceiptComponent(
                            status = message.status,
                            onRetryClick = { onRetrySend(message.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuotedMessagePreview(
    senderName: String,
    quotedText: String,
    isMe: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isMe) Color(0xFF0369A1) else Color(0xFF334155),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(6.dp)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .background(Color(0xFF00E5FF), RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )
                Text(
                    text = quotedText,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun VoiceNoteBubble(
    messageId: String,
    audioUrl: String,
    durationMs: Long,
    isPlaying: Boolean,
    progress: Float,
    onPlayClick: (String) -> Unit,
    isMe: Boolean
) {
    val totalSecs = (durationMs / 1000).toInt()
    val formattedDuration = String.format("%02d:%02d", totalSecs / 60, totalSecs % 60)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onPlayClick(audioUrl) },
            modifier = Modifier
                .size(36.dp)
                .background(if (isMe) Color(0xFF0284C7) else Color(0xFF00E5FF), CircleShape)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "إيقاف" else "تشغيل",
                tint = if (isMe) Color.White else Color(0xFF0F172A),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress = { if (isPlaying) progress else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF00E5FF),
                trackColor = Color.White.copy(alpha = 0.25f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formattedDuration,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun DocumentBubble(
    fileName: String,
    fileSize: Long,
    isPdf: Boolean,
    onOpenClick: () -> Unit
) {
    Surface(
        onClick = onOpenClick,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isPdf) Color(0xFFEF4444) else Color(0xFF3B82F6),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Document",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = AttachmentManager.formatFileSize(fileSize),
                    fontSize = 10.5.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "فتح",
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ContactBubble(
    name: String,
    phone: String,
    onCallClick: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF10B981),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Contact",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = phone, fontSize = 11.sp, color = Color(0xFF94A3B8))
            }

            IconButton(
                onClick = { onCallClick(phone) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "اتصال",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun LocationBubble(
    lat: Double,
    lng: Double,
    label: String,
    onMapClick: (Double, Double) -> Unit
) {
    Surface(
        onClick = { onMapClick(lat, lng) },
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFEC4899),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = "📍 $lat, $lng",
                    fontSize = 10.5.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "فتح في الخريطة",
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// Backward-compatibility wrapper
@Composable
fun ChatMessageItem(
    message: ChatMessageModel,
    isMe: Boolean,
    modifier: Modifier = Modifier
) {
    MessageItemComposable(
        message = message,
        isMe = isMe,
        modifier = modifier
    )
}
