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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.models.ChannelType
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

    var channelToDelete by remember { mutableStateOf<ChatChannel?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }

    val filters = listOf(
        Pair("ALL", "الكل"),
        Pair("UNREAD", "غير مقروءة 🔔"),
        Pair("SUPPORT", "الدعم الفني 🛠️"),
        Pair("TECHNICIANS", "الفنيين 🧰"),
        Pair("STORES", "المتاجر 🏬"),
        Pair("RESTAURANTS", "المطاعم 🍽️")
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
                    channel.lastMessage.contains(searchQuery, ignoreCase = true) ||
                    channel.title.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "UNREAD" -> channel.getUnreadFor(currentUserId) > 0
                "SUPPORT" -> channel.type == ChannelType.SUPPORT ||
                        channel.relatedEntityType == "SUPPORT" ||
                        channel.title.contains("دعم", ignoreCase = true) ||
                        channel.description.contains("دعم", ignoreCase = true)
                "TECHNICIANS" -> channel.type == ChannelType.PROVIDER ||
                        channel.relatedEntityType == "TECHNICIAN" ||
                        channel.relatedEntityType == "URGENT_REQUEST" ||
                        channel.relatedEntityType == "BOOKING"
                "STORES" -> channel.type == ChannelType.STORE ||
                        channel.relatedEntityType == "STORE" ||
                        channel.title.contains("متجر", ignoreCase = true)
                "RESTAURANTS" -> channel.relatedEntityType == "RESTAURANT" ||
                        channel.title.contains("مطعم", ignoreCase = true) ||
                        channel.title.contains("كافيه", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesFilter
        }
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
                Box {
                    IconButton(onClick = { showTopMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "خيارات", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showTopMenu,
                        onDismissRequest = { showTopMenu = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        DropdownMenuItem(
                            text = { Text("حذف جميع المحادثات", color = Color(0xFFE53935), fontSize = 13.sp) },
                            onClick = {
                                showTopMenu = false
                                showDeleteAllDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935)) }
                        )
                    }
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

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))

        // Channels List
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF1E88E5))
            }
        } else if (filteredChannels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchQuery.isNotBlank()) "لا توجد نتائج بحث مطابقة." else "لا توجد محادثات في هذا التصنيف حالياً.",
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
                    val otherName = channel.participantNames[otherUserId] ?: channel.title.ifBlank { "مستخدم" }
                    val otherPhoto = channel.participantPhotos[otherUserId] ?: channel.groupAvatarUrl
                    val unread = channel.getUnreadFor(currentUserId)

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

    // Single Channel Delete Dialog
    channelToDelete?.let { targetChannel ->
        AlertDialog(
            onDismissRequest = { channelToDelete = null },
            title = { Text("حذف المحادثة", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت تأكد من رغبتك في حذف هذه المحادثة بالكامل؟ لا يمكن التراجع بعد الحذف.", color = Color.LightGray, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        chatListViewModel.deleteChannel(targetChannel.id)
                        channelToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("حذف الآن", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { channelToDelete = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // Delete All Channels Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("حذف جميع المحادثات", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = { Text("سيتم حذف كافة المحادثات والرسائل الخاصة بك بشكل نهائي. هل تريد الاستمرار؟", color = Color.LightGray, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        chatListViewModel.deleteAllChannels(channels)
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("حذف الكل نعم", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
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

                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف المحادثة",
                                tint = Color.Gray.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

