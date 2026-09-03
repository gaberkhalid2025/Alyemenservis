package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.ChatMessageEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 💬 ChatMessageItem
 * مكون عرض الرسائل المتكامل في شاشات المحادثة.
 * يدعم جميع أنواع الوسائط (نص، صورة، صوت، فيديو، مستند، موقع)،
 * الرد المقتبس، التفاعلات السريعة (Reactions)، وحالات التسليم والقراءة.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageItem(
    message: ChatMessageEntity,
    isMe: Boolean,
    onReply: (ChatMessageEntity) -> Unit = {},
    onReaction: (messageId: String, emoji: String) -> Unit = { _, _ -> },
    onDelete: (ChatMessageEntity) -> Unit = {},
    onMediaClick: (url: String, type: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showReactionsPicker by remember { mutableStateOf(false) }

    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    val bubbleBg = if (isMe) Color(0xFF0284C7) else Color(0xFF1E293B)
    val textColor = Color.White
    val metaColor = if (isMe) Color(0xFFBAE6FD) else Color(0xFF94A3B8)

    val timeFormatted = remember(message.timestamp) {
        val sdf = SimpleDateFormat("hh:mm a", Locale("ar"))
        sdf.format(Date(if (message.timestamp > 0) message.timestamp else System.currentTimeMillis()))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        // Quick Reaction Picker popup
        AnimatedVisibility(
            visible = showReactionsPicker,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0F172A),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val emojis = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")
                    emojis.forEach { emoji ->
                        TextButton(
                            onClick = {
                                onReaction(message.id, emoji)
                                showReactionsPicker = false
                            },
                            contentPadding = PaddingValues(4.dp),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(text = emoji, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // Main Bubble Container
        Box {
            Card(
                shape = bubbleShape,
                colors = CardDefaults.cardColors(containerColor = bubbleBg),
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            showReactionsPicker = true
                            showMenu = true
                        }
                    )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Sender name if in group or received
                    if (!isMe && message.senderName.isNotBlank()) {
                        Text(
                            text = message.senderName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    // 1. Quoted / Reply Preview
                    if (message.replyToText.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.25f),
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
                                        .background(Color(0xFF38BDF8), RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    if (message.replyToSender.isNotBlank()) {
                                        Text(
                                            text = message.replyToSender,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                    Text(
                                        text = message.replyToText,
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // 2. Media Content rendering
                    when (message.mediaType.uppercase()) {
                        "IMAGE" -> {
                            val imgUrl = message.imageUrl.ifBlank { message.mediaUrl }
                            if (imgUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imgUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "صورة مرفقة",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .combinedClickable(
                                            onClick = { onMediaClick(imgUrl, "IMAGE") },
                                            onLongClick = { showMenu = true }
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                        "AUDIO", "VOICE" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledIconButton(
                                    onClick = { onMediaClick(message.mediaUrl, "AUDIO") },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (isMe) Color.White.copy(alpha = 0.2f) else Color(0xFF0284C7)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "تشغيل الصوت",
                                        tint = Color.White
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    LinearProgressIndicator(
                                        progress = { 0f },
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                        color = Color.White,
                                        trackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (message.audioDurationSec > 0) "${message.audioDurationSec} ثانية" else "رسالة صوتية",
                                        fontSize = 10.sp,
                                        color = metaColor
                                    )
                                }
                            }
                        }
                        "FILE", "DOCUMENT", "PDF", "EXCEL" -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (message.mediaUrl.isNotBlank()) {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(message.mediaUrl))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {}
                                            }
                                        },
                                        onLongClick = { showMenu = true }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = message.fileName.ifBlank { "مستند مرفق" },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (message.fileSize > 0) {
                                            Text(
                                                text = "${message.fileSize / 1024} KB",
                                                fontSize = 9.sp,
                                                color = metaColor
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    // 3. Text Body
                    if (message.message.isNotBlank()) {
                        Text(
                            text = message.message,
                            color = textColor,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 4. Timestamp and Status checks
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = timeFormatted,
                            fontSize = 10.sp,
                            color = metaColor
                        )

                        if (isMe) {
                            when (message.status.uppercase()) {
                                "READ" -> Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = "تمت القراءة",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(13.dp)
                                )
                                "DELIVERED" -> Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = "تم التسليم",
                                    tint = metaColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                "FAILED" -> Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "فشل الإرسال",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(13.dp)
                                )
                                else -> Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "تم الإرسال",
                                    tint = metaColor,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Options Dropdown Menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color(0xFF1E293B))
            ) {
                DropdownMenuItem(
                    text = { Text("رد", color = Color.White) },
                    onClick = {
                        showMenu = false
                        onReply(message)
                    },
                    leadingIcon = { Icon(Icons.Default.Share, null, tint = Color.White) }
                )
                if (message.message.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text("نسخ النص", color = Color.White) },
                        onClick = {
                            showMenu = false
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Chat Message", message.message))
                        },
                        leadingIcon = { Icon(Icons.Default.ThumbUp, null, tint = Color.White) }
                    )
                }
                DropdownMenuItem(
                    text = { Text("حذف الرسالة", color = Color(0xFFEF4444)) },
                    onClick = {
                        showMenu = false
                        onDelete(message)
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444)) }
                )
            }
        }

        // 5. Reactions bubble displayed underneath
        if (message.reactions.isNotBlank()) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF0F172A),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .offset(y = (-6).dp)
                    .border(1.dp, Color(0xFF334155), CircleShape)
            ) {
                Text(
                    text = message.reactions,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
