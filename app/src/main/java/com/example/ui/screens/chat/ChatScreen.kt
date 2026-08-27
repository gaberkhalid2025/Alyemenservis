package com.example.ui.screens.chat

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.models.MediaType
import com.example.ui.screens.chat.components.ChatBubbleItem
import com.example.ui.screens.chat.components.ChatHeaderBar
import com.example.ui.screens.chat.components.ChatInputBar
import com.example.util.VoiceNoteManager
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
    chatViewModel: ChatViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val voiceNoteManager = remember { VoiceNoteManager(context) }

    DisposableEffect(Unit) {
        onDispose {
            voiceNoteManager.release()
        }
    }

    val currentChannel by chatViewModel.currentChannel.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    val presence by chatViewModel.otherUserPresence.collectAsState()
    val isTypingOther by chatViewModel.isTypingOther.collectAsState()
    val replyingTo by chatViewModel.replyingToMessage.collectAsState()
    val searchQuery by chatViewModel.searchQuery.collectAsState()

    var isSearchOpen by remember { mutableStateOf(false) }
    var selectedMessageForAction by remember { mutableStateOf<ChatMessage?>(null) }
    var showDeleteChatDialog by remember { mutableStateOf(false) }

    // Initialize Chat
    LaunchedEffect(channel, channelId, targetUserId) {
        if (channel != null) {
            chatViewModel.openChannel(channel, currentUserId)
        } else if (!channelId.isNullOrBlank()) {
            chatViewModel.openChannelById(
                channelId = channelId,
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                fallbackTargetUserId = targetUserId,
                fallbackUserName = targetUserName,
                relatedEntityId = relatedEntityId,
                relatedEntityType = relatedEntityType
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

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
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

    val entityId = activeChannel?.relatedEntityId ?: relatedEntityId
    val entityType = activeChannel?.relatedEntityType ?: relatedEntityType

    val filteredMessages = remember(messages, searchQuery) {
        if (searchQuery.isBlank()) messages
        else messages.filter { it.message.contains(searchQuery, ignoreCase = true) }
    }

    // Dialog for deleting the entire chat
    if (showDeleteChatDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteChatDialog = false },
            title = { Text("حذف المحادثة بالكامل", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("هل تريد حذف هذه المحادثة من قائمتك فقط أم حذفها لكلا الطرفين؟", fontSize = 13.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        chatViewModel.deleteChannel(currentUserId, forEveryone = true) {
                            showDeleteChatDialog = false
                            onBackClick()
                        }
                    }
                ) {
                    Text("حذف للطرفين", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            chatViewModel.deleteChannel(currentUserId, forEveryone = false) {
                                showDeleteChatDialog = false
                                onBackClick()
                            }
                        }
                    ) {
                        Text("حذف لدي فقط", color = Color(0xFF1E88E5))
                    }
                    TextButton(onClick = { showDeleteChatDialog = false }) {
                        Text("إلغاء", color = Color.Gray)
                    }
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
        // Header
        ChatHeaderBar(
            name = otherUserName,
            photoUrl = otherUserPhoto,
            presence = presence,
            isTyping = isTypingOther,
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
            onDeleteChatClick = {
                showDeleteChatDialog = true
            }
        )

        // Related Context Bar (Booking / Urgent Request)
        if (!entityId.isNullOrBlank()) {
            val contextTitle = when (entityType?.uppercase()) {
                "BOOKING" -> "📅 محادثة بخصوص الحجز رقم #${entityId.takeLast(6)}"
                "URGENT_REQUEST" -> "🚨 محادثة بخصوص الطلب العاجل رقم #${entityId.takeLast(6)}"
                "SUPPORT" -> "🛡️ تذكرة الدعم الفني رقم #${entityId.takeLast(6)}"
                else -> "📋 بخصوص المعاملة رقم #${entityId.takeLast(6)}"
            }

            Surface(
                color = Color(0xFF19324D),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E88E5).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = contextTitle,
                        fontSize = 12.sp,
                        color = Color(0xFFE3F2FD),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

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
                    onValueChange = { chatViewModel.setSearchQuery(it) },
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
                    chatViewModel.setSearchQuery("")
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
                            voiceNoteManager = voiceNoteManager,
                            onReplyClick = { chatViewModel.setReplyingTo(msg) },
                            onLongClick = { selectedMessageForAction = msg }
                        )
                    }
                }
            }
        }

        // Input Bar
        ChatInputBar(
            replyingTo = replyingTo,
            onCancelReply = { chatViewModel.setReplyingTo(null) },
            onSendMessage = { text, mediaType, mediaUrl, durationSec ->
                chatViewModel.sendMessage(
                    senderId = currentUserId,
                    senderName = currentUserName,
                    text = text,
                    mediaType = mediaType,
                    mediaUrl = mediaUrl,
                    mediaDurationSeconds = durationSec
                )
            },
            onSendVoiceFile = { audioFile, durationSec ->
                chatViewModel.sendVoiceNote(
                    senderId = currentUserId,
                    senderName = currentUserName,
                    audioFile = audioFile,
                    durationSeconds = durationSec
                )
            },
            onTyping = { text ->
                chatViewModel.onUserTyping(currentUserId, text)
            }
        )
    }

    // Message options action sheet
    if (selectedMessageForAction != null) {
        val targetMsg = selectedMessageForAction!!
        val isMe = targetMsg.senderId == currentUserId

        AlertDialog(
            onDismissRequest = { selectedMessageForAction = null },
            title = { Text("خيارات الرسالة", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            chatViewModel.setReplyingTo(targetMsg)
                            selectedMessageForAction = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF64B5F6))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("الرد على هذه الرسالة", color = Color.White, fontSize = 13.sp)
                        }
                    }
                    if (isMe && !targetMsg.isDeleted) {
                        TextButton(
                            onClick = {
                                chatViewModel.deleteMessage(targetMsg.id, true, currentUserId)
                                selectedMessageForAction = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF5350))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("حذف لدى الطرفين (الجميع)", color = Color(0xFFEF5350), fontSize = 13.sp)
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            chatViewModel.deleteMessage(targetMsg.id, false, currentUserId)
                            selectedMessageForAction = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.LightGray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حذف لدي فقط", color = Color.LightGray, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = Color(0xFF1E293B)
        )
    }
}
