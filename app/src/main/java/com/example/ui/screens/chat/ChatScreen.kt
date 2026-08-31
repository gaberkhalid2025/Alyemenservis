package com.example.ui.screens.chat
import com.example.ui.MainViewModel

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.models.ChatChannel
import com.example.data.models.ChatMessage
import com.example.ui.screens.chat.components.ChatBubbleItem
import com.example.ui.screens.chat.components.ChatHeaderBar
import com.example.ui.screens.chat.components.ChatInputBar
import com.example.util.AudioPlayerManager
import com.example.utils.VisualThemePalette

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
    viewModel: ChatViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    if (currentUserId.isBlank() || currentUserId == "guest") {
        Box(modifier = Modifier.fillMaxSize().background(themeColors.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("⚠️ يرجى تسجيل الدخول أولاً للتمكن من المحادثة", fontSize = 16.sp, color = Color.Red, textAlign = TextAlign.Center)
                Button(onClick = onBackClick, colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)) {
                    Text("رجوع", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    val context = LocalContext.current
    val listState = rememberLazyListState()

    val currentChannel by viewModel.currentChannel.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val presence by viewModel.otherUserPresence.collectAsState()
    val isTypingOther by viewModel.isTypingOther.collectAsState()
    val replyingTo by viewModel.replyingToMessage.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var isSearchOpen by remember { mutableStateOf(false) }
    var selectedMessageForAction by remember { mutableStateOf<ChatMessage?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ChatEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is ChatEvent.MessageSent -> {
                    // Message sent successfully
                }
            }
        }
    }

    LaunchedEffect(messages) {
        messages.filter { it.status == com.example.data.models.MessageStatus.FAILED }.forEach {
            snackbarHostState.showSnackbar("فشل إرسال رسالة: ${it.message}")
        }
    }

    // Initialize Chat
    LaunchedEffect(channel, channelId, targetUserId) {
        if (channel != null) {
            viewModel.openChannel(channel, currentUserId)
        } else if (!channelId.isNullOrBlank()) {
            viewModel.openChannelById(
                channelId = channelId,
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                fallbackTargetUserId = targetUserId,
                fallbackUserName = targetUserName
            )
        } else if (!targetUserId.isNullOrBlank()) {
            viewModel.startDirectChat(
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

    val activeChannel = currentChannel
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

    var showDeleteChannelDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D151F))
        ) {
        // Header
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
                if (!isSearchOpen) viewModel.setSearchQuery("")
            },
            onBlockClick = {
                if (otherUserId.isNotBlank()) {
                    viewModel.toggleBlock(otherUserId, true)
                    Toast.makeText(context, "تم حظر المستخدم", Toast.LENGTH_SHORT).show()
                }
            },
            onDeleteChannelClick = {
                showDeleteChannelDialog = true
            }
        )

        // In-chat search bar
        AnimatedVisibility(visible = isSearchOpen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("بحث في المحادثة...", fontSize = 12.sp, color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF1E88E5)
                    )
                )
                IconButton(onClick = {
                    isSearchOpen = false
                    viewModel.setSearchQuery("")
                }) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                }
            }
        }

        // Messages List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (filteredMessages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "لا توجد رسائل مطابقة للبحث" else "لا توجد رسائل سابقة. ابدأ المحادثة الآن!",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredMessages, key = { it.id }) { msg ->
                        val isMe = msg.senderId == currentUserId
                        ChatBubbleItem(
                            message = msg,
                            isMe = isMe,
                            onReplyClick = { viewModel.setReplyingTo(msg) },
                            onLongClick = { selectedMessageForAction = msg },
                            onRetryClick = { viewModel.resendMessage(msg.id) }
                        )
                    }
                }
            }
        }

        // Input Bar
        ChatInputBar(
            replyingTo = replyingTo,
            onCancelReply = { viewModel.setReplyingTo(null) },
            onSendMessage = { text, mediaType, mediaUrl ->
                viewModel.sendMessage(
                    senderId = currentUserId,
                    senderName = currentUserName,
                    text = text,
                    mediaType = mediaType,
                    mediaUrl = mediaUrl
                )
            },
            onTyping = { text ->
                viewModel.onUserTyping(currentUserId, text)
            }
        )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )
    }

    // Message options action sheet
    if (selectedMessageForAction != null) {
        val targetMsg = selectedMessageForAction!!
        val isMe = targetMsg.senderId == currentUserId

        AlertDialog(
            onDismissRequest = { selectedMessageForAction = null },
            title = { Text("خيارات الرسالة", fontSize = 14.sp, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        viewModel.setReplyingTo(targetMsg)
                        selectedMessageForAction = null
                    }) {
                        Text("↩️ الرد على الرسالة", color = Color.White, fontSize = 13.sp)
                    }
                    if (isMe) {
                        TextButton(onClick = {
                            viewModel.deleteMessage(targetMsg.id, true, currentUserId)
                            selectedMessageForAction = null
                        }) {
                            Text("🗑️ حذف لدى الجميع", color = Color(0xFFE53935), fontSize = 13.sp)
                        }
                    }
                    TextButton(onClick = {
                        viewModel.deleteMessage(targetMsg.id, false, currentUserId)
                        selectedMessageForAction = null
                    }) {
                        Text("🗑️ حذف لدي فقط", color = Color.LightGray, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {},
            containerColor = Color(0xFF1E293B)
        )
    }

    // Channel Deletion Confirmation Dialog
    if (showDeleteChannelDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteChannelDialog = false },
            title = { Text("حذف المحادثة بالكامل", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف هذه المحادثة وجميع رسائلها؟ لا يمكن استرجاع البيانات بعد الحذف.", color = Color.LightGray, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteChannelDialog = false
                        viewModel.deleteCurrentChannel {
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
                    Text("إلغاء", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
