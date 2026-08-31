package com.example.ui.components
import com.example.ui.MainViewModel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.ChatMessageEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * 💬 ChatMessageComponent
 * مكون عرض الرسالة الفردية التفاعلية مع دعم النصوص، الصور، التسجيلات الصوتية، الملفات، الردود، والتفاعلات
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageComponent(
    message: ChatMessageEntity,
    isMe: Boolean,
    showSenderName: Boolean = false,
    searchQuery: String = "",
    onDeleteMessage: ((String) -> Unit)? = null,
    onForwardMessage: ((ChatMessageEntity) -> Unit)? = null,
    onReplyMessage: ((ChatMessageEntity) -> Unit)? = null,
    onReactionSelect: ((ChatMessageEntity, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showContextMenu by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }
    var isPlayingAudio by remember { mutableStateOf(false) }
    var audioProgress by remember { mutableFloatStateOf(0.3f) }
    var showReactionPicker by remember { mutableStateOf(false) }

    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    val bubbleColor = if (message.isDeleted) {
        if (isMe) Color(0xFF37474F) else Color(0xFFE2E8F0)
    } else if (isMe) {
        Color(0xFF00668B)
    } else {
        Color(0xFFF0F4F8)
    }

    val textColor = if (message.isDeleted) {
        Color.Gray
    } else if (isMe) {
        Color.White
    } else {
        Color(0xFF1E293B)
    }

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
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            // اسم المرسل
            if (showSenderName && !isMe && message.senderName.isNotBlank()) {
                Text(
                    text = message.senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00668B),
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            }

            // إشارة لإعادة التوجيه
            if (message.forwardedFrom.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Text("معاد توجيهها", fontSize = 10.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
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
                    
                    // اقتباس الرد على رسالة سابقة
                    if (message.replyToText.isNotBlank() && !message.isDeleted) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isMe) Color.Black.copy(alpha = 0.25f) else Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(28.dp)
                                        .background(if (isMe) Color(0xFFFFA000) else Color(0xFF00668B), RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = message.replyToSender.ifBlank { "رسالة سابقة" },
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMe) Color(0xFFFFA000) else Color(0xFF00668B)
                                    )
                                    Text(
                                        text = message.replyToText,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        color = if (isMe) Color.LightGray else Color.DarkGray
                                    )
                                }
                            }
                        }
                    }

                    // 1. عرض الصورة المرفقة
                    if ((message.mediaType == "IMAGE" || message.imageUrl.isNotBlank() || (message.mediaUrl.isNotBlank() && message.mediaType != "AUDIO" && message.mediaType != "VIDEO" && message.mediaType != "FILE")) && !message.isDeleted) {
                        val imgUrl = message.imageUrl.ifEmpty { message.mediaUrl }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.1f))
                        ) {
                            AsyncImage(
                                model = imgUrl,
                                contentDescription = "الصورة المرفقة",
                                modifier = Modifier.fillMaxWidth().height(180.dp),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "تم حفظ الصورة في معرض الهاتف بنجاح 💾", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .size(32.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "حفظ الصورة", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // 2. عرض الفيديو المرفق
                    if (message.mediaType == "VIDEO" && !message.isDeleted) {
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
                                "مقطع فيديو 🎥",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                            )
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "تم حفظ مقطع الفيديو في معرض الهاتف بنجاح 💾", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).size(30.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "حفظ الفيديو", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // 3. عرض الملفات والمستندات (PDF / Word / Excel)
                    if (message.mediaType == "FILE" && !message.isDeleted) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isMe) Color.White.copy(alpha = 0.15f) else Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    Toast.makeText(context, "جاري فتح الملف: ${message.fileName}", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (isMe) Color(0xFFFFA000) else Color(0xFF00668B),
                                    modifier = Modifier.size(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message.fileName.ifBlank { "مستند مرفق" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = textColor,
                                        maxLines = 1
                                    )
                                    val sizeMb = if (message.fileSize > 0) String.format(Locale.US, "%.1f ميغابايت", message.fileSize / (1024.0 * 1024.0)) else "ملف"
                                    Text(
                                        text = sizeMb,
                                        fontSize = 10.sp,
                                        color = timeColor
                                    )
                                }
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "تحميل الملف",
                                    tint = if (isMe) Color.White else Color(0xFF00668B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // 4. عرض التسجيل الصوتي
                    if (message.mediaType == "AUDIO" && !message.isDeleted) {
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

                    // 5. نص الرسالة مع تمييز البحث (Yellow highlighting)
                    if (message.message.isNotBlank()) {
                        if (searchQuery.isNotBlank() && message.message.contains(searchQuery, ignoreCase = true) && !message.isDeleted) {
                            val annotatedString = buildAnnotatedString {
                                val text = message.message
                                var startIndex = 0
                                while (true) {
                                    val index = text.indexOf(searchQuery, startIndex, ignoreCase = true)
                                    if (index == -1) {
                                        append(text.substring(startIndex))
                                        break
                                    }
                                    append(text.substring(startIndex, index))
                                    withStyle(style = SpanStyle(background = Color(0xFFFFEB3B), color = Color.Black, fontWeight = FontWeight.Bold)) {
                                        append(text.substring(index, index + searchQuery.length))
                                    }
                                    startIndex = index + searchQuery.length
                                }
                            }
                            Text(
                                text = annotatedString,
                                color = textColor,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        } else {
                            Text(
                                text = message.message,
                                color = textColor,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        // معاينة الروابط إن وجدت في النص
                        if (message.message.contains("http://") || message.message.contains("https://") && !message.isDeleted) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isMe) Color.Black.copy(alpha = 0.2f) else Color.White,
                                border = BorderStroke(0.5.dp, Color.Gray.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(6.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = if (isMe) Color(0xFFFFA000) else Color(0xFF00668B), modifier = Modifier.size(16.dp))
                                    Text("معاينة الرابط الذكية 🔗", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = textColor)
                                }
                            }
                        }
                    }

                    // 6. الوقت وحالة القراءة
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

                        if (isMe && !message.isDeleted) {
                            Spacer(modifier = Modifier.width(4.dp))
                            when (message.status.uppercase()) {
                                "READ" -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            val readTimeStr = if (message.readAt > 0) SimpleDateFormat("hh:mm a", Locale("ar")).format(Date(message.readAt)) else "الآن"
                                            Toast.makeText(context, "تمت القراءة الساعة: $readTimeStr", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "مقروء",
                                            tint = Color(0xFF64B5F6),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                "DELIVERED" -> {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "تم التسليم",
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

            // عرض التفاعلات أسفل الرسالة
            if (message.reactions.isNotBlank() && !message.isDeleted) {
                val reactionList = remember(message.reactions) { message.reactions.split(",").filter { it.isNotBlank() } }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                ) {
                    reactionList.forEach { emoji ->
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 2.dp,
                            border = BorderStroke(0.5.dp, Color.LightGray),
                            modifier = Modifier.clickable {
                                onReactionSelect?.invoke(message, emoji)
                            }
                        ) {
                            Text(emoji, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
            }

            // قائمة السياق عند الضغط المطول
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                // شريط التفاعلات السريعة
                DropdownMenuItem(
                    text = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("👍", "❤️", "😂", "😮", "😢", "😡").forEach { emoji ->
                                Text(
                                    emoji,
                                    fontSize = 18.sp,
                                    modifier = Modifier.clickable {
                                        onReactionSelect?.invoke(message, emoji)
                                        showContextMenu = false
                                    }
                                )
                            }
                        }
                    },
                    onClick = {}
                )
                HorizontalDivider()
                if (onReplyMessage != null && !message.isDeleted) {
                    DropdownMenuItem(
                        text = { Text("رد على الرسالة 💬") },
                        leadingIcon = { Icon(Icons.Default.Send, contentDescription = null) },
                        onClick = {
                            onReplyMessage(message)
                            showContextMenu = false
                        }
                    )
                }
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
                if (onForwardMessage != null && !message.isDeleted) {
                    DropdownMenuItem(
                        text = { Text("إعادة توجيه ↗️") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = {
                            onForwardMessage(message)
                            showContextMenu = false
                        }
                    )
                }
                if (isMe && onDeleteMessage != null && !message.isDeleted) {
                    DropdownMenuItem(
                        text = { Text("حذف للجميع 🗑️", color = MaterialTheme.colorScheme.error) },
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                Toast.makeText(context, "تم حفظ الصورة في المعرض 💾", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00668B))
                        ) {
                            Text("حفظ في المعرض")
                        }
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
}
