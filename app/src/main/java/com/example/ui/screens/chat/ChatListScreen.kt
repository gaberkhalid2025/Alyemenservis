package com.example.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.data.models.ChatFilterCategory
import com.example.utils.VisualThemePalette
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    currentUserId: String,
    currentUserName: String,
    themeColors: VisualThemePalette,
    chatListViewModel: ChatListViewModel = viewModel(),
    onChannelClick: (ChatChannel) -> Unit,
    onBackClick: () -> Unit
) {
    val filteredChannels by chatListViewModel.filteredChannels.collectAsState()
    val selectedFilter by chatListViewModel.selectedFilter.collectAsState()
    val searchQuery by chatListViewModel.searchQuery.collectAsState()
    val isLoading by chatListViewModel.isLoading.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var channelToDelete by remember { mutableStateOf<ChatChannel?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val filterTabs = listOf(
        Pair(ChatFilterCategory.ALL, "الكل"),
        Pair(ChatFilterCategory.UNREAD, "غير مقروءة"),
        Pair(ChatFilterCategory.TECHNICIANS, "فنيين"),
        Pair(ChatFilterCategory.STORES, "متاجر"),
        Pair(ChatFilterCategory.RESTAURANTS, "مطاعم"),
        Pair(ChatFilterCategory.SUPPORT, "دعم فني")
    )

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotBlank()) {
            chatListViewModel.loadUserChannels(currentUserId)
            chatListViewModel.setUserPresence(currentUserId, true)
        }
    }

    // Dialog for deleting single channel
    channelToDelete?.let { channel ->
        val otherUserId = channel.participants.firstOrNull { it != currentUserId } ?: ""
        val otherName = channel.participantNames[otherUserId] ?: "المحادثة"

        AlertDialog(
            onDismissRequest = { channelToDelete = null },
            title = { Text("حذف المحادثة مع $otherName", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("هل تريد حذف هذه المحادثة من قائمتك أم حذفها للجميع؟", fontSize = 13.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        chatListViewModel.deleteChannel(channel.id, currentUserId, forEveryone = true)
                        channelToDelete = null
                    }
                ) {
                    Text("حذف للطرفين", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            chatListViewModel.deleteChannel(channel.id, currentUserId, forEveryone = false)
                            channelToDelete = null
                        }
                    ) {
                        Text("حذف لدي فقط", color = Color(0xFF1E88E5))
                    }
                    TextButton(onClick = { channelToDelete = null }) {
                        Text("إلغاء", color = Color.Gray)
                    }
                }
            },
            containerColor = Color(0xFF142030),
            textContentColor = Color.LightGray,
            titleContentColor = Color.White
        )
    }

    // Dialog for deleting all channels
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("حذف جميع المحادثات", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("هل أنت متأكد من رغبتك في مسح كافة المحادثات والرسائل؟", fontSize = 13.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        chatListViewModel.deleteAllChannels(currentUserId, forEveryone = false)
                        showDeleteAllDialog = false
                    }
                ) {
                    Text("مسح الكل لدي", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF142030),
            textContentColor = Color.LightGray,
            titleContentColor = Color.White
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D151F))
    ) {
        // Top App Bar
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
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

            // Options menu (Delete all chats)
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "خيارات", tint = Color.White)
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF142030))
                ) {
                    DropdownMenuItem(
                        text = { Text("حذف جميع المحادثات", color = Color(0xFFEF5350), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF5350)) },
                        onClick = {
                            showMenu = false
                            showDeleteAllDialog = true
                        }
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { chatListViewModel.setSearchQuery(it) },
            placeholder = { Text("بحث في الأسماء، الرسائل، أو أرقام الطلبات...", fontSize = 12.sp, color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = Color.Gray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { chatListViewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            },
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

        // Filter Tabs
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterTabs) { (filterCat, title) ->
                val isSelected = selectedFilter == filterCat
                Surface(
                    color = if (isSelected) Color(0xFF1E88E5) else Color(0xFF142030),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF1E88E5) else Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.clickable { chatListViewModel.setFilter(filterCat) }
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "لا توجد نتائج بحث مطابقة." else "لا توجد محادثات في هذا القسم حالياً.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
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
                        onLongClick = { channelToDelete = channel }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelItemCard(
    channel: ChatChannel,
    otherName: String,
    otherPhoto: String,
    unreadCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val formattedTime = if (channel.lastMessageTime > 0) timeFormat.format(Date(channel.lastMessageTime)) else ""

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF142030)),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
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

                // Entity context badge if available (e.g. Booking #XXX)
                if (!channel.relatedEntityId.isNullOrBlank()) {
                    val badgeLabel = when (channel.relatedEntityType?.uppercase()) {
                        "BOOKING" -> "حجز #${channel.relatedEntityId.takeLast(6)}"
                        "URGENT_REQUEST" -> "طلب عاجل #${channel.relatedEntityId.takeLast(6)}"
                        else -> "طلب #${channel.relatedEntityId.takeLast(6)}"
                    }
                    Text(
                        text = badgeLabel,
                        fontSize = 10.sp,
                        color = Color(0xFF4FC3F7),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

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
        }
    }
}
