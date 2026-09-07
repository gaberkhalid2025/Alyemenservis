package com.example.ui.screens.chat

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.models.ChatChannel
import com.example.data.models.ChatMessage
import com.example.data.models.MediaType
import com.example.data.models.MessageStatus
import com.example.ui.screens.chat.components.ChatBubbleItem
import com.example.ui.screens.chat.components.ChatHeaderBar
import com.example.ui.screens.chat.components.ChatInputBar
import com.example.ui.screens.chat.components.TypingIndicator
import com.example.utils.AudioPlayerManager
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    currentUserId: String,
    currentUserName: String,
    currentUserPhoto: String = "",
    channel: ChatChannel? = null,
    channelId: String? = null,
    targetUserId: String? = null,
    targetUserName: String? = null,
    targetUserPhoto: String? = null,
    relatedEntityId: String? = null,
    relatedEntityType: String? = null,
    themeColors: VisualThemePalette,
    chatViewModel: ChatViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    if (currentUserId.isBlank()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(themeColors.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = themeColors.accent)
        }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val currentChannel by chatViewModel.currentChannel.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    val presence by chatViewModel.otherUserPresence.collectAsState()
    val isTypingOther by chatViewModel.isTypingOther.collectAsState()
    val replyingTo by chatViewModel.replyingToMessage.collectAsState()
    val searchQuery by chatViewModel.searchQuery.collectAsState()

    var isSearchOpen by remember { mutableStateOf(false) }
    var selectedMessageForAction by remember { mutableStateOf<ChatMessage?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showDeleteChannelDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        chatViewModel.eventFlow.collect { event ->
            when (event) {
                is ChatEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is ChatEvent.MessageSent -> {
                    // Success
                }
            }
        }
    }

    // Initialize Channel
    LaunchedEffect(channel, channelId, targetUserId) {
        if (channel != null) {
            chatViewModel.openChannel(channel, currentUserId)
        } else if (!channelId.isNullOrBlank()) {
            chatViewModel.openChannelById(
                channelId = channelId,
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                fallbackTargetUserId = targetUserId,
                fallbackUserName = targetUserName
            )
        } else if (!targetUserId.isNullOrBlank()) {
            chatViewModel.startDirectChat(
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                currentUserPhoto = currentUserPhoto,
                otherUserId = targetUserId,
                otherUserName = targetUserName ?: "مستخدم",
                otherUserPhoto = targetUserPhoto ?: "",
                relatedEntityId = relatedEntityId,
                relatedEntityType = relatedEntityType
            )
        }
    }

    val activeChannel = currentChannel

    // Automatically mark channel as read whenever messages arrive
    LaunchedEffect(messages, activeChannel) {
        val chId = activeChannel?.id ?: channelId
        if (!chId.isNullOrBlank()) {
            chatViewModel.markAsRead(chId, currentUserId)
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Stop active audio on screen exit
    DisposableEffect(Unit) {
        onDispose {
            AudioPlayerManager.stop()
        }
    }

    val otherUserId = remember(activeChannel, currentUserId, targetUserId) {
        activeChannel?.participants?.firstOrNull { it != currentUserId } ?: targetUserId ?: ""
    }
    val otherUserName = remember(activeChannel, otherUserId, targetUserName) {
        activeChannel?.participantNames?.get(otherUserId) ?: targetUserName ?: "مستخدم"
    }
    val otherUserPhoto = remember(activeChannel, otherUserId, targetUserPhoto) {
        activeChannel?.participantPhotos?.get(otherUserId) ?: targetUserPhoto ?: ""
    }

    val filteredMessages = remember(messages, searchQuery) {
        if (searchQuery.isBlank()) messages
        else messages.filter { it.message.contains(searchQuery, ignoreCase = true) }
    }

    val showScrollToBottom by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 3 && lastVisibleItemIndex < totalItems - 2
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // 1. Fixed Top Header Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                ChatHeaderBar(
                    name = otherUserName,
                    photoUrl = otherUserPhoto,
                    presence = presence,
                    isTyping = isTypingOther,
                    relatedEntityId = activeChannel?.relatedEntityId ?: relatedEntityId,
                    relatedEntityType = activeChannel?.relatedEntityType ?: relatedEntityType,
                    onBackClick = onBackClick,
                    onSearchToggle = {
                        isSearchOpen = !isSearchOpen
                        if (!isSearchOpen) chatViewModel.setSearchQuery("")
                    },
                    onBlockClick = {
                        if (otherUserId.isNotBlank()) {
                            chatViewModel.toggleBlock(otherUserId, true)
                            Toast.makeText(context, "تم حظر المستخدم", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDeleteChannelClick = {
                        showDeleteChannelDialog = true
                    },
                    themeColors = themeColors
                )

                AnimatedVisibility(visible = isSearchOpen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(themeColors.surface)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { chatViewModel.setSearchQuery(it) },
                            placeholder = { Text("بحث في المحادثة...", fontSize = 12.sp, color = themeColors.textSecondary) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = themeColors.textPrimary,
                                unfocusedTextColor = themeColors.textPrimary,
                                focusedBorderColor = themeColors.accent
                            )
                        )
                        IconButton(onClick = {
                            isSearchOpen = false
                            chatViewModel.setSearchQuery("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = themeColors.textPrimary)
                        }
                    }
                }
            }

            // 2. Messages List (takes available vertical space)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (filteredMessages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "لا توجد رسائل مطابقة للبحث" else "لا توجد رسائل سابقة. ابدأ المحادثة الآن!",
                            color = themeColors.textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item(key = "load_more_header") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(onClick = { chatViewModel.loadMoreMessages() }) {
                                    Text(
                                        text = "⬆️ تحميل الرسائل السابقة",
                                        fontSize = 12.sp,
                                        color = themeColors.accent,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        itemsIndexed(filteredMessages, key = { _, msg -> msg.id }) { index, msg ->
                            val showDateSeparator = index == 0 || !isSameDay(filteredMessages[index - 1].timestamp, msg.timestamp)
                            if (showDateSeparator && msg.timestamp > 0) {
                                DateSeparatorChip(dateMillis = msg.timestamp)
                            }

                            val isMe = msg.senderId == currentUserId
                            ChatBubbleItem(
                                message = msg,
                                isMe = isMe,
                                onReplyClick = { chatViewModel.setReplyingTo(msg) },
                                onLongClick = { selectedMessageForAction = msg },
                                onRetryClick = { chatViewModel.resendMessage(msg.id) },
                                themeColors = themeColors
                            )
                        }
                    }
                }

                // Floating Scroll to Bottom button
                androidx.compose.animation.AnimatedVisibility(
                    visible = showScrollToBottom,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                if (filteredMessages.isNotEmpty()) {
                                    listState.animateScrollToItem(filteredMessages.size - 1)
                                }
                            }
                        },
                        containerColor = themeColors.accent,
                        contentColor = Color.White,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "الانتقال لأسفل",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 3. Bottom Chat Input (Anchored directly above keyboard)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                if (isTypingOther) {
                    TypingIndicator(
                        userName = activeChannel?.participantNames?.get(otherUserId) ?: otherUserName
                    )
                }
                ChatInputBar(
                    channelId = activeChannel?.id ?: channelId ?: "direct_chat",
                    replyingTo = replyingTo,
                    editingMessage = editingMessage,
                    onCancelReply = { chatViewModel.setReplyingTo(null) },
                    onCancelEdit = { editingMessage = null },
                    onSendMessage = { text, mediaType, mediaUrl ->
                        chatViewModel.sendMessage(
                            senderId = currentUserId,
                            senderName = currentUserName,
                            text = text,
                            mediaType = mediaType,
                            mediaUrl = mediaUrl
                        )
                    },
                    onEditMessage = { messageId, newText ->
                        chatViewModel.editMessage(messageId, newText)
                        editingMessage = null
                    },
                    onTyping = { text ->
                        chatViewModel.onUserTyping(currentUserId, text)
                    },
                    themeColors = themeColors
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Message options dialog
    if (selectedMessageForAction != null) {
        val targetMsg = selectedMessageForAction!!
        val isMe = targetMsg.senderId == currentUserId

        AlertDialog(
            onDismissRequest = { selectedMessageForAction = null },
            title = { Text("خيارات الرسالة", fontSize = 14.sp, color = themeColors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isMe && targetMsg.mediaType == MediaType.TEXT) {
                        TextButton(onClick = {
                            editingMessage = targetMsg
                            selectedMessageForAction = null
                        }) {
                            Text("✏️ تعديل الرسالة", color = themeColors.accent, fontSize = 13.sp)
                        }
                    }
                    TextButton(onClick = {
                        chatViewModel.setReplyingTo(targetMsg)
                        selectedMessageForAction = null
                    }) {
                        Text("↩️ الرد على الرسالة", color = themeColors.textPrimary, fontSize = 13.sp)
                    }
                    if (isMe) {
                        TextButton(onClick = {
                            chatViewModel.deleteMessage(targetMsg.id, true, currentUserId)
                            selectedMessageForAction = null
                        }) {
                            Text("🗑️ حذف لدى الجميع", color = Color(0xFFE53935), fontSize = 13.sp)
                        }
                    }
                    TextButton(onClick = {
                        chatViewModel.deleteMessage(targetMsg.id, false, currentUserId)
                        selectedMessageForAction = null
                    }) {
                        Text("🗑️ حذف لدي فقط", color = themeColors.textSecondary, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {},
            containerColor = themeColors.surface
        )
    }

    // Channel Deletion Confirmation Dialog
    if (showDeleteChannelDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteChannelDialog = false },
            title = { Text("حذف المحادثة بالكامل", color = themeColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف هذه المحادثة وجميع رسائلها؟ لا يمكن استرجاع البيانات بعد الحذف.", color = themeColors.textSecondary, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteChannelDialog = false
                        chatViewModel.deleteCurrentChannel {
                            onBackClick()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("تأكيد الحذف", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteChannelDialog = false }) {
                    Text("إلغاء", color = themeColors.textSecondary)
                }
            },
            containerColor = themeColors.surface
        )
    }
}

@Composable
private fun DateSeparatorChip(dateMillis: Long) {
    val dateText = remember(dateMillis) {
        formatChatDateHeader(dateMillis)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF1E293B).copy(alpha = 0.9f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shadowElevation = 2.dp
        ) {
            Text(
                text = dateText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }
    }
}

private fun isSameDay(time1: Long, time2: Long): Boolean {
    if (time1 <= 0 || time2 <= 0) return false
    val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun formatChatDateHeader(timeMillis: Long): String {
    val now = Calendar.getInstance()
    val msgCal = Calendar.getInstance().apply { timeInMillis = timeMillis }

    val currentYear = now.get(Calendar.YEAR)
    val msgYear = msgCal.get(Calendar.YEAR)
    val currentDay = now.get(Calendar.DAY_OF_YEAR)
    val msgDay = msgCal.get(Calendar.DAY_OF_YEAR)

    return when {
        currentYear == msgYear && currentDay == msgDay -> "اليوم"
        currentYear == msgYear && (currentDay - msgDay == 1) -> "أمس"
        currentYear == msgYear -> SimpleDateFormat("EEEE، d MMMM", Locale("ar")).format(Date(timeMillis))
        else -> SimpleDateFormat("d MMMM yyyy", Locale("ar")).format(Date(timeMillis))
    }
}
