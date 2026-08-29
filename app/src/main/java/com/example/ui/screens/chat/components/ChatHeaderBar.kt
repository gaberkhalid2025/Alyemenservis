package com.example.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
    relatedEntityId: String? = null,
    relatedEntityType: String? = null,
    onBackClick: () -> Unit,
    onSearchToggle: () -> Unit,
    onBlockClick: () -> Unit,
    onDeleteChannelClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF142030))
            .border(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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

                val isOnline = presence?.isOnline == true
                val isAway = !isOnline && presence != null && (System.currentTimeMillis() - presence.lastSeen < 15 * 60 * 1000L)

                // Online/Away indicator dot
                if (isOnline || isAway) {
                    val dotColor = if (isOnline) Color(0xFF00C853) else Color(0xFFFFB300)
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(dotColor, CircleShape)
                            .border(2.dp, Color(0xFF142030), CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.ifBlank { "محادثة" },
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val isOnline = presence?.isOnline == true
                val isAway = !isOnline && presence != null && (System.currentTimeMillis() - presence.lastSeen < 15 * 60 * 1000L)

                val statusText = when {
                    isTyping -> "يكتب الآن..."
                    isOnline -> "متصل الآن"
                    isAway -> "نشط مؤخراً"
                    presence != null && presence.lastSeen > 0 -> {
                        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        "آخر ظهور ${sdf.format(Date(presence.lastSeen))}"
                    }
                    else -> "غير متصل"
                }

                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    color = if (isTyping || isOnline) Color(0xFF64FFDA) else if (isAway) Color(0xFFFFB300) else Color.Gray
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
                        text = { Text("حظر المستخدم", color = Color(0xFFFF8A80), fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            onBlockClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFFF8A80)) }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    DropdownMenuItem(
                        text = { Text("حذف المحادثة بالكامل", color = Color(0xFFE53935), fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                        onClick = {
                            showMenu = false
                            onDeleteChannelClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935)) }
                    )
                }
            }
        }

        // Booking / Urgent Request Context Strip
        if (!relatedEntityId.isNullOrBlank() || !relatedEntityType.isNullOrBlank()) {
            val contextText = when (relatedEntityType) {
                "BOOKING" -> "📌 محادثة بخصوص الحجز رقم ${relatedEntityId ?: ""}"
                "URGENT_REQUEST" -> "🚨 محادثة بخصوص الطلب العاجل رقم ${relatedEntityId ?: ""}"
                "SUPPORT" -> "🛠️ محادثة تذكرة الدعم الفني ${relatedEntityId ?: ""}"
                else -> if (!relatedEntityId.isNullOrBlank()) "📋 محادثة خاصة بطلب رقم $relatedEntityId" else null
            }

            if (contextText != null) {
                Surface(
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = contextText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64B5F6),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

