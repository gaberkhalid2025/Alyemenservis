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
import com.example.utils.VisualThemePalette
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
    onDeleteChannelClick: () -> Unit,
    themeColors: VisualThemePalette? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    val surfaceColor = themeColors?.surface ?: Color(0xFF142030)
    val primaryColor = themeColors?.primary ?: Color(0xFF1E88E5)
    val accentColor = themeColors?.accent ?: Color(0xFF64FFDA)
    val textPrimary = themeColors?.textPrimary ?: Color.White
    val textSecondary = themeColors?.textSecondary ?: Color.Gray
    val borderColor = themeColors?.border ?: Color.White.copy(alpha = 0.08f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .border(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = textPrimary)
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
                            .background(primaryColor, CircleShape),
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
                            .border(2.dp, surfaceColor, CircleShape)
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
                    color = textPrimary,
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
                    color = if (isTyping || isOnline) accentColor else if (isAway) Color(0xFFFFB300) else textSecondary
                )
            }

            IconButton(onClick = onSearchToggle) {
                Icon(Icons.Default.Search, contentDescription = "بحث", tint = textPrimary)
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "خيارات", tint = textPrimary)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(surfaceColor)
                ) {
                    DropdownMenuItem(
                        text = { Text("حظر المستخدم", color = Color(0xFFFF8A80), fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            onBlockClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFFF8A80)) }
                    )
                    HorizontalDivider(color = borderColor)
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
                    color = primaryColor.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = contextText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

