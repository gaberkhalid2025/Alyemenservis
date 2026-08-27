package com.example.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.UserPresence
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatHeaderBar(
    name: String,
    photoUrl: String,
    presence: UserPresence?,
    isTyping: Boolean,
    onBackClick: () -> Unit,
    onSearchToggle: () -> Unit,
    onBlockClick: () -> Unit,
    onDeleteChannelClick: () -> Unit,
    onCloseClick: () -> Unit,
    isBlocked: Boolean = false,
    isMuted: Boolean = false,
    onViewProfileClick: () -> Unit = {},
    onToggleMuteClick: () -> Unit = {},
    onReportUserClick: () -> Unit = {},
    onClearChatClick: () -> Unit = {},
    onExportChatClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF142030))
            .border(1.dp, Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
        }

        Box(modifier = Modifier.size(42.dp)) {
            if (photoUrl.isNotBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E88E5), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).ifBlank { "💬" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            // Online green dot
            if (presence?.isOnline == true) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFF00C853), CircleShape)
                        .border(2.dp, Color(0xFF142030), CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name.ifBlank { "محادثة" },
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isMuted) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Info, contentDescription = "مكتوم", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                }
            }

            val statusText = when {
                isTyping -> "يكتب الآن..."
                presence?.isOnline == true -> "متصل الآن"
                presence != null && presence.lastSeen > 0 -> {
                    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    "آخر ظهور ${sdf.format(Date(presence.lastSeen))}"
                }
                else -> "غير متصل"
            }

            Text(
                text = statusText,
                fontSize = 11.sp,
                color = if (isTyping || presence?.isOnline == true) Color(0xFF64FFDA) else Color.Gray
            )
        }

        IconButton(onClick = onSearchToggle) {
            Icon(Icons.Default.Search, contentDescription = "بحث", tint = Color.White)
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "خيارات", tint = Color.White)
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color(0xFF1E293B))
            ) {
                DropdownMenuItem(
                    text = { Text("عرض الملف الشخصي 👤", color = Color.White, fontSize = 13.sp) },
                    onClick = {
                        showMenu = false
                        onViewProfileClick()
                    },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF38BDF8)) }
                )

                DropdownMenuItem(
                    text = { Text(if (isMuted) "إلغاء كتم الإشعارات 🔔" else "كتم الإشعارات 🔕", color = Color.White, fontSize = 13.sp) },
                    onClick = {
                        showMenu = false
                        onToggleMuteClick()
                    },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFBBF24)) }
                )

                DropdownMenuItem(
                    text = { Text("الإبلاغ عن المستخدم ⚠️", color = Color(0xFFF97316), fontSize = 13.sp) },
                    onClick = {
                        showMenu = false
                        onReportUserClick()
                    },
                    leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF97316)) }
                )

                DropdownMenuItem(
                    text = { Text("مسح الرسائل 🧹", color = Color.White, fontSize = 13.sp) },
                    onClick = {
                        showMenu = false
                        onClearChatClick()
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFA855F7)) }
                )

                DropdownMenuItem(
                    text = { Text("تصدير المحادثة 📤", color = Color.White, fontSize = 13.sp) },
                    onClick = {
                        showMenu = false
                        onExportChatClick()
                    },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF00E5FF)) }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                DropdownMenuItem(
                    text = {
                        Text(
                            if (isBlocked) "إلغاء حظر المستخدم ✅" else "حظر المستخدم 🚫",
                            color = if (isBlocked) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    onClick = {
                        showMenu = false
                        onBlockClick()
                    },
                    leadingIcon = {
                        Icon(
                            if (isBlocked) Icons.Default.CheckCircle else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isBlocked) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                )

                DropdownMenuItem(
                    text = { Text("حذف المحادثة بالكامل 🗑️", color = Color(0xFFEF4444), fontSize = 13.sp) },
                    onClick = {
                        showMenu = false
                        onDeleteChannelClick()
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) }
                )

                DropdownMenuItem(
                    text = { Text("إغلاق 🚪", color = Color.Gray, fontSize = 13.sp) },
                    onClick = {
                        showMenu = false
                        onCloseClick()
                    },
                    leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray) }
                )
            }
        }
    }
}
