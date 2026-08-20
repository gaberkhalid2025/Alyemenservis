package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.ChatMessageEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * 💬 ChatMessageComponent
 * مكون عرض الرسالة الفردية التفاعلية مع دعم النصوص، الصور، التسجيلات الصوتية، وحالات التسليم
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageComponent(
    message: ChatMessageEntity,
    isMe: Boolean,
    showSenderName: Boolean = false,
    onDeleteMessage: ((String) -> Unit)? = null,
    onForwardMessage: ((ChatMessageEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showContextMenu by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }
    var isPlayingAudio by remember { mutableStateOf(false) }
    var audioProgress by remember { mutableFloatStateOf(0.3f) }

    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    val bubbleColor = if (isMe) {
        Color(0xFF00668B)
    } else {
        Color(0xFFF0F4F8)
    }

    val textColor = if (isMe) Color.White else Color(0xFF1E293B)
    val timeColor = if (isMe) Color(0xFFB0D7EB) else Color(0xFF64748B)

    val timeFormatted = remember(message.timestamp) {
        if (message.timestamp > 0) {
            SimpleDateFormat("hh:mm a", Locale("ar")).format(Date(message.timestamp))
        } else ""
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            // اسم المرسل في المجموعات أو المحادثات المشتركة
            if (showSenderName && !isMe && message.senderName.isNotBlank()) {
                Text(
                    text = message.senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00668B),
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            }

            Surface(
                shape = bubbleShape,
                color = bubbleColor,
                shadowElevation = 1.5.dp,
                modifier = Modifier
                    .clip(bubbleShape)
                    .combinedClickable(
                        onClick = {
                            if (message.mediaType == "IMAGE" || message.imageUrl.isNotBlank()) {
                                showImageDialog = true
                            }
                        },
                        onLongClick = {
                            showContextMenu = true
                        }
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    
                    // 1. عرض الصورة المرفقة
                    if (message.mediaType == "IMAGE" || message.imageUrl.isNotBlank() || (message.mediaUrl.isNotBlank() && message.mediaType != "AUDIO" && message.mediaType != "VIDEO")) {
                        val imgUrl = message.imageUrl.ifEmpty { message.mediaUrl }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.1f))
                        ) {
                            AsyncImage(
                                model = imgUrl,
                                contentDescription = "الصورة المرفقة",
                                modifier = Modifier.fillMaxWidth().height(180.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // 2. عرض الفيديو المرفق
                    if (message.mediaType == "VIDEO") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "تشغيل مقطع الفيديو...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.3f), CircleShape)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل", tint = Color.White)
                            }
                            Text(
                                "مقطع فيديو",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // 3. عرض التسجيل الصوتي
                    if (message.mediaType == "AUDIO") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            IconButton(
                                onClick = { isPlayingAudio = !isPlayingAudio },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (isMe) Color.White.copy(alpha = 0.2f) else Color(0xFF00668B).copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(
                                    if (isPlayingAudio) Icons.Default.Close else Icons.Default.PlayArrow,
                                    contentDescription = "صوت",
                                    tint = if (isMe) Color.White else Color(0xFF00668B)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                LinearProgressIndicator(
                                    progress = { if (isPlayingAudio) 0.65f else audioProgress },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                    color = if (isMe) Color.White else Color(0xFF00668B),
                                    trackColor = if (isMe) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.2f),
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${message.audioDurationSec.coerceAtLeast(5)} ثانية",
                                        fontSize = 10.sp,
                                        color = timeColor
                                    )
                                    Text(
                                        text = "تسجيل صوتي",
                                        fontSize = 10.sp,
                                        color = timeColor
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // 4. نص الرسالة
                    if (message.message.isNotBlank()) {
                        Text(
                            text = message.message,
                            color = textColor,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    // 5. الوقت وحالة القراءة
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 2.dp)
                    ) {
                        Text(
                            text = timeFormatted,
                            fontSize = 10.sp,
                            color = timeColor
                        )

                        if (isMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            when (message.status.uppercase()) {
                                "READ" -> {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "مقروء",
                                        tint = Color(0xFF64B5F6),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                "DELIVERED" -> {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "واصل",
                                        tint = timeColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                else -> {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "مرسل",
                                        tint = timeColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // قائمة السياق عند الضغط المطول
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("نسخ الرسالة") },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("chat_msg", message.message)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "تم نسخ النص", Toast.LENGTH_SHORT).show()
                        showContextMenu = false
                    }
                )
                if (onForwardMessage != null) {
                    DropdownMenuItem(
                        text = { Text("إعادة توجيه") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = {
                            onForwardMessage(message)
                            showContextMenu = false
                        }
                    )
                }
                if (isMe && onDeleteMessage != null) {
                    DropdownMenuItem(
                        text = { Text("حذف الرسالة", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onDeleteMessage(message.id)
                            showContextMenu = false
                        }
                    )
                }
            }
        }
    }

    // نافذة تكبير الصورة
    if (showImageDialog) {
        Dialog(onDismissRequest = { showImageDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(12.dp)
                ) {
                    val imgUrl = message.imageUrl.ifEmpty { message.mediaUrl }
                    AsyncImage(
                        model = imgUrl,
                        contentDescription = "عرض الصورة بالكامل",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showImageDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                    ) {
                        Text("إغلاق", color = Color.White)
                    }
                }
            }
        }
    }
}
