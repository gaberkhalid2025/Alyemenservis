package com.example.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.models.ChatChannel
import com.example.utils.VisualThemePalette
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatListScreen(
    currentUserId: String,
    currentUserName: String,
    themeColors: VisualThemePalette,
    chatListViewModel: ChatListViewModel = viewModel(),
    onChannelClick: (ChatChannel) -> Unit,
    onBackClick: () -> Unit
) {
    val channels by chatListViewModel.channels.collectAsState()
    val selectedFilter by chatListViewModel.selectedFilter.collectAsState()
    val searchQuery by chatListViewModel.searchQuery.collectAsState()
    val isLoading by chatListViewModel.isLoading.collectAsState()

    val filters = listOf(
        Pair("ALL", "الكل"),
        Pair("UNREAD", "غير مقروءة"),
        Pair("SUPPORT", "الدعم الفني")
    )

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotBlank()) {
            chatListViewModel.loadUserChannels(currentUserId)
            chatListViewModel.setUserPresence(currentUserId, true)
        }
    }

    val filteredChannels = remember(channels, selectedFilter, searchQuery, currentUserId) {
        channels.filter { channel ->
            val otherUserId = channel.participants.firstOrNull { it != currentUserId } ?: ""
            val otherName = channel.participantNames[otherUserId] ?: ""
            val matchesSearch = searchQuery.isBlank() ||
                    otherName.contains(searchQuery, ignoreCase = true) ||
                    channel.lastMessage.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "UNREAD" -> (channel.unreadCount[currentUserId] ?: 0) > 0
                "SUPPORT" -> channel.type.name == "SUPPORT"
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var channelToDelete by remember { mutableStateOf<ChatChannel?>(null) }

    if (showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = { Text("حذف كافة المحادثات 🗑️", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("هل أنت متأكد من رغبتك في حذف جميع المحادثات نهائياً؟ لا يمكن استرجاع الرسائل المحذوفة.", fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        chatListViewModel.deleteAllChannels(channels)
                        showDeleteAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("حذف الجميع", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirm = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }

    if (channelToDelete != null) {
        AlertDialog(
            onDismissRequest = { channelToDelete = null },
            title = { Text("حذف المحادثة 🗑️", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("هل أنت متأكد من حذف هذه المحادثة مع كافة رسائلها نهائياً؟", fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        channelToDelete?.let { chatListViewModel.deleteChannel(it.id) }
                        channelToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("حذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { channelToDelete = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D151F))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF142030))
                .border(1.dp, Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "المحادثات والرسائل",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "تواصل فوري وآمن 🔒",
                    fontSize = 11.sp,
                    color = Color(0xFF90CAF9)
                )
            }
            if (channels.isNotEmpty()) {
                IconButton(
                    onClick = { showDeleteAllConfirm = true },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFEF5350).copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف جميع المحادثات",
                        tint = Color(0xFFEF5350)
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { chatListViewModel.setSearchQuery(it) },
            placeholder = { Text("بحث في المحادثات...", fontSize = 12.sp, color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF1E88E5),
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedContainerColor = Color(0xFF142030),
                unfocusedContainerColor = Color(0xFF142030)
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Filter Pills
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { (key, title) ->
                val isSelected = selectedFilter == key
                Surface(
                    color = if (isSelected) Color(0xFF1E88E5) else Color(0xFF142030),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF1E88E5) else Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.clickable { chatListViewModel.setFilter(key) }
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))

        // Channels List
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF1E88E5))
            }
        } else if (filteredChannels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchQuery.isNotBlank()) "لا توجد نتائج بحث مطابقة." else "لا توجد محادثات نشطة حالياً. عند قبول عرض أو طلب خدمة ستظهر محادثتك هنا فوراً.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredChannels, key = { it.id }) { channel ->
                    val otherUserId = channel.participants.firstOrNull { it != currentUserId } ?: ""
                    val otherName = channel.participantNames[otherUserId] ?: "مستخدم"
                    val otherPhoto = channel.participantPhotos[otherUserId] ?: ""
                    val unread = channel.unreadCount[currentUserId] ?: 0

                    ChannelItemCard(
                        channel = channel,
                        otherName = otherName,
                        otherPhoto = otherPhoto,
                        unreadCount = unread,
                        onClick = { onChannelClick(channel) },
                        onDeleteClick = { channelToDelete = channel }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelItemCard(
    channel: ChatChannel,
    otherName: String,
    otherPhoto: String,
    unreadCount: Int,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val formattedTime = if (channel.lastMessageTime > 0) timeFormat.format(Date(channel.lastMessageTime)) else ""

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF142030)),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Photo
            if (otherPhoto.isNotBlank()) {
                AsyncImage(
                    model = otherPhoto,
                    contentDescription = otherName,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF1E88E5), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = otherName.take(1).ifBlank { "👤" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = otherName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 10.5.sp,
                        color = if (unreadCount > 0) Color(0xFF64B5F6) else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = channel.lastMessage.ifBlank { "بدء محادثة جديدة" },
                        fontSize = 12.sp,
                        color = if (unreadCount > 0) Color.White else Color.LightGray,
                        fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (unreadCount > 0) {
                        Surface(
                            color = Color(0xFF1E88E5),
                            shape = CircleShape,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف المحادثة",
                    tint = Color(0xFFEF5350).copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
