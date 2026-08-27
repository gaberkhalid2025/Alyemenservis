package com.example.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
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
    onBackClick: () -> Unit,
    onSearchToggle: () -> Unit,
    onBlockClick: () -> Unit,
    onDeleteChatClick: () -> Unit = {}
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
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
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
            Text(
                text = name.ifBlank { "محادثة" },
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

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
                    text = { Text("حذف هذه المحادثة", color = Color(0xFFEF5350), fontSize = 13.sp) },
                    onClick = {
                        showMenu = false
                        onDeleteChatClick()
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF5350)) }
                )
                DropdownMenuItem(
                    text = { Text("حظر المستخدم", color = Color.LightGray, fontSize = 13.sp) },
                    onClick = {
                        showMenu = false
                        onBlockClick()
                    },
                    leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null, tint = Color.LightGray) }
                )
            }
        }
    }
}
