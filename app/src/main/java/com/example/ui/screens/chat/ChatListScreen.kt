package com.example.ui.screens.chat

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.ChatChannelEntity
import com.example.viewmodels.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 📋 ChatListScreen
 * شاشة عرض قائمة المحادثات النشطة مع البحث، التصفية حسب نوع المحادثة، وإحصائيات الرسائل غير المقروءة
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    currentUserId: String = "user_default",
    userRole: String = "USER", // USER, PROVIDER, STORE, RESTAURANT, ADMIN
    onChannelClick: (channelId: String, otherUserId: String, otherUserName: String, otherUserPhoto: String) -> Unit = { _, _, _, _ -> },
    onBack: () -> Unit = {},
    chatViewModel: ChatViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PROVIDER, STORE, RESTAURANT, ADMIN, UNREAD

    val channels: List<ChatChannelEntity> by chatViewModel.activeChannels.collectAsState()
    val unreadCount: Int by chatViewModel.unreadCount.collectAsState()
    val isLoading: Boolean by chatViewModel.isLoading.collectAsState()

    LaunchedEffect(currentUserId) {
        chatViewModel.loadChannelsForUser(currentUserId)
    }

    val filteredChannels = remember(channels, searchQuery, selectedFilter) {
        channels.filter { channel ->
            val matchesFilter = when (selectedFilter) {
                "UNREAD" -> channel.unreadCount > 0
                "PROVIDER" -> channel.channelType.equals("PROVIDER", ignoreCase = true)
                "STORE" -> channel.channelType.equals("STORE", ignoreCase = true)
                "RESTAURANT" -> channel.channelType.equals("RESTAURANT", ignoreCase = true)
                "ADMIN" -> channel.channelType.equals("ADMIN", ignoreCase = true)
                else -> true
            }

            val otherName = if (channel.providerId == currentUserId) channel.clientName else channel.providerName
            val matchesSearch = searchQuery.isBlank() ||
                    otherName.contains(searchQuery, ignoreCase = true) ||
                    channel.lastMessage.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "المحادثات والرسائل",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (unreadCount > 0) {
                            Text(
                                text = "$unreadCount رسائل جديدة غير مقروءة",
                                fontSize = 12.sp,
                                color = Color(0xFF00668B)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            // شريط البحث
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("بحث في المحادثات...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00668B),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )

            // شريط الفلاتر
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    "ALL" to "الكل",
                    "UNREAD" to "غير مقروءة 🔴",
                    "PROVIDER" to "الفنيين 🔧",
                    "STORE" to "المتاجر 🛍️",
                    "RESTAURANT" to "المطاعم 🍽️",
                    "ADMIN" to "الدعم الفني 🛡️"
                )

                items(filters) { (key, label) ->
                    val isSelected = selectedFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00668B),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // قائمة المحادثات
            if (isLoading && channels.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredChannels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "لا توجد محادثات مطابقة",
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = "عند بدء حجز أو طلب خدمة ستظهر محادثتك هنا",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredChannels, key = { it.id }) { channel ->
                        val isMeProvider = channel.providerId == currentUserId
                        val otherId = if (isMeProvider) channel.clientId else channel.providerId
                        val otherName = if (isMeProvider) channel.clientName.ifEmpty { "العميل" } else channel.providerName.ifEmpty { "مقدم الخدمة" }
                        val otherPhoto = if (isMeProvider) channel.clientPhoto else channel.providerPhoto

                        val timeFormatted = remember(channel.lastMessageTime) {
                            if (channel.lastMessageTime > 0) {
                                SimpleDateFormat("hh:mm a", Locale("ar")).format(Date(channel.lastMessageTime))
                            } else ""
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onChannelClick(channel.id, otherId, otherName, otherPhoto)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // صورة الطرف الآخر
                                Box {
                                    if (otherPhoto.isNotBlank()) {
                                        AsyncImage(
                                            model = otherPhoto,
                                            contentDescription = otherName,
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .background(Color(0xFFE0F2FE), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = Color(0xFF00668B),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }

                                // تفاصيل الرسالة
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = otherName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF1E293B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = timeFormatted,
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = channel.lastMessage.ifEmpty { "بدء محادثة جديدة" },
                                            fontSize = 13.sp,
                                            color = if (channel.unreadCount > 0) Color(0xFF1E293B) else Color(0xFF64748B),
                                            fontWeight = if (channel.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // شارة عدد غير المقروء
                                        if (channel.unreadCount > 0) {
                                            Badge(
                                                containerColor = Color(0xFF00668B),
                                                contentColor = Color.White,
                                                modifier = Modifier.padding(start = 6.dp)
                                            ) {
                                                Text(
                                                    text = "${channel.unreadCount}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
