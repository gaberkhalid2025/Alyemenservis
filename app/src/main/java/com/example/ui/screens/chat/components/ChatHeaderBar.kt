package com.example.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
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
    isBlocked: Boolean = false,
    isMuted: Boolean = false,
    onBackClick: () -> Unit,
    onSearchToggle: () -> Unit,
    onBlockToggle: () -> Unit,
    onViewProfile: (() -> Unit)? = null,
    onMuteToggle: (() -> Unit)? = null,
    onReportUser: (() -> Unit)? = null,
    onClearChat: (() -> Unit)? = null,
    onExportChat: (() -> Unit)? = null
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

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onViewProfile?.invoke() }
        ) {
            Text(
                text = name.ifBlank { "محادثة" },
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val statusText = when {
                isBlocked -> "🚫 مستخدم محظور"
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
                color = if (isBlocked) Color.Red else if (isTyping || presence?.isOnline == true) Color(0xFF64FFDA) else Color.Gray
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
                    text = { Text("👤 عرض الملف الشخصي", color = Color.White, fontSize = 13.sp) },
                    onClick = {
                        showMenu = false
                        onViewProfile?.invoke()
                    }
                )

                DropdownMenuItem(
                    text = { Text(if (isMuted) "🔔 إلغاء كتم الإشعارات" else "🔕 كتم الإشعارات", color = Color.White, fontSize = 13.sp) },
                    onClick = {
                        showMenu = false
                        onMuteToggle?.invoke()
                    }
                )

                DropdownMenuItem(
                    text = { Text("⚠️ الإبلاغ عن المستخدم", color = Color(0xFFFFB74D), fontSize = 13.sp) },
                    onClick = {
                        showMenu = false
                        onReportUser?.invoke()
                    }
                )

                DropdownMenuItem(
                    text = { Text("🧹 مسح المحادثة", color = Color.White, fontSize = 13.sp) },
                    onClick = {
                        showMenu = false
                        onClearChat?.invoke()
                    }
                )

                DropdownMenuItem(
                    text = { Text("📤 تصدير المحادثة", color = Color.White, fontSize = 13.sp) },
                    onClick = {
                        showMenu = false
                        onExportChat?.invoke()
                    }
                )

                Divider(color = Color.White.copy(alpha = 0.1f))

                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (isBlocked) "✅ إلغاء حظر المستخدم" else "🚫 حظر المستخدم",
                            color = if (isBlocked) Color(0xFF00C853) else Color(0xFFE53935),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    },
                    onClick = {
                        showMenu = false
                        onBlockToggle()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isBlocked) Icons.Filled.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isBlocked) Color(0xFF00C853) else Color(0xFFE53935)
                        )
                    }
                )
            }
        }
    }
}
