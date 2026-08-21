package com.example.ui.screens.chat

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.ChatMessageEntity
import com.example.ui.components.ChatMessageComponent
import kotlinx.coroutines.delay

/**
 * 💬 ChatScreen
 * شاشة المحادثة الفورية التفاعلية المتكاملة (البحث، الردود، التفاعلات، المرفقات، الحظر، والمؤشرات)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    channelId: String,
    currentUserId: String = "user_default",
    currentUserName: String = "المستخدم",
    otherUserId: String = "provider_default",
    otherUserName: String = "مقدم الخدمة",
    otherUserPhoto: String = "",
    otherUserPhone: String = "",
    isOtherUserOnline: Boolean = true,
    userRole: String = "USER", // USER, PROVIDER, STORE, RESTAURANT, ADMIN
    onBack: () -> Unit = {},
    chatViewModel: ChatViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val messages by chatViewModel.messages.collectAsState()
    val inputText by chatViewModel.inputText.collectAsState()
    val otherUserTyping by chatViewModel.otherUserTyping.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val replyingTo by chatViewModel.replyingTo.collectAsState()
    val searchQuery by chatViewModel.searchQuery.collectAsState()
    val isSearchActive by chatViewModel.isSearchActive.collectAsState()
    val isChannelBlocked by chatViewModel.isChannelBlocked.collectAsState()

    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableIntStateOf(0) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var messageToForward by remember { mutableStateOf<ChatMessageEntity?>(null) }

    // Start listening on launch
    LaunchedEffect(channelId) {
        chatViewModel.startListeningToChannel(channelId, currentUserId, otherUserId)
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Voice recording timer
    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingDuration = 0
            while (isRecordingAudio) {
                delay(1000)
                recordingDuration++
            }
        }
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            chatViewModel.sendMessage(
                channelId = channelId,
                senderId = currentUserId,
                senderName = currentUserName,
                recipientId = otherUserId,
                msgText = "📷 صورة مرفقة",
                mediaType = "IMAGE",
                mediaUrl = uri.toString()
            )
            Toast.makeText(context, "تم إرسال الصورة بنجاح 🖼️", Toast.LENGTH_SHORT).show()
        }
    }

    // Document / File Picker Launcher
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment ?: "document.pdf"
            chatViewModel.sendMessage(
                channelId = channelId,
                senderId = currentUserId,
                senderName = currentUserName,
                recipientId = otherUserId,
                msgText = "📄 $fileName",
                mediaType = "FILE",
                mediaUrl = uri.toString(),
                fileName = fileName,
                fileSize = 1024 * 1024 * 2 // 2MB typical indicator
            )
            Toast.makeText(context, "تم إرسال المستند بنجاح 📎", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { chatViewModel.setSearchQuery(it) },
                            placeholder = { Text("بحث في الرسائل...", fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00668B),
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { chatViewModel.toggleSearchActive(false) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "إغلاق البحث")
                        }
                    },
                    actions = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { chatViewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح")
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                if (otherUserPhoto.isNotBlank()) {
                                    AsyncImage(
                                        model = otherUserPhoto,
                                        contentDescription = otherUserName,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(Color(0xFFE2E8F0), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color(0xFF00668B),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                // مؤشر الاتصال (Online Status)
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            if (isOtherUserOnline) Color(0xFF4CAF50) else Color.Gray,
                                            CircleShape
                                        )
                                        .padding(2.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = otherUserName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (otherUserTyping) "✍️ يكتب الآن..." else if (isOtherUserOnline) "🟢 متصل الآن" else "آخر ظهور قريباً",
                                    fontSize = 11.5.sp,
                                    color = if (otherUserTyping) Color(0xFF00668B) else if (isOtherUserOnline) Color(0xFF2E7D32) else Color.Gray
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        IconButton(onClick = { chatViewModel.toggleSearchActive(true) }) {
                            Icon(Icons.Default.Search, contentDescription = "بحث في المحادثة", tint = Color(0xFF00668B))
                        }
                        if (otherUserPhone.isNotBlank()) {
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$otherUserPhone"))
                                context.startActivity(intent)
                            }) {
                                Icon(Icons.Default.Phone, contentDescription = "اتصال هاتف", tint = Color(0xFF00668B))
                            }
                        }
                        Box {
                            IconButton(onClick = { showOptionsMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "خيارات إضافية")
                            }
                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isChannelBlocked) "إلغاء حظر المحادثة" else "حظر المستخدم 🚫") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    onClick = {
                                        chatViewModel.toggleBlockChannel(channelId, !isChannelBlocked)
                                        Toast.makeText(context, if (!isChannelBlocked) "تم حظر المحادثة" else "تم إلغاء الحظر", Toast.LENGTH_SHORT).show()
                                        showOptionsMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("إبلاغ عن محتوى مخالف ⚠️") },
                                    leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null) },
                                    onClick = {
                                        Toast.makeText(context, "تم إرسال البلاغ للإدارة للمراجعة", Toast.LENGTH_SHORT).show()
                                        showOptionsMenu = false
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (isChannelBlocked) {
                Surface(
                    color = Color(0xFFFFEBEE),
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                ) {
                    Text(
                        "⛔ هذه المحادثة محظورة حالياً، لا يمكنك إرسال رسائل جديدة.",
                        color = Color(0xFFD32F2F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                        
                        // مؤشر الرد على رسالة معينة (Replying Quote Preview)
                        if (replyingTo != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE0F2FE),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(28.dp)
                                            .background(Color(0xFF00668B), RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "الرد على ${replyingTo?.senderName?.ifBlank { "الرسالة" }}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF00668B)
                                        )
                                        Text(
                                            text = replyingTo?.message ?: "",
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            color = Color.DarkGray
                                        )
                                    }
                                    IconButton(
                                        onClick = { chatViewModel.setReplyingTo(null) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "إلغاء الرد", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // مؤشر الكتابة للطرف الآخر
                        if (otherUserTyping) {
                            Text(
                                text = "✍️ $otherUserName يكتب الآن...",
                                fontSize = 11.sp,
                                color = Color(0xFF00668B),
                                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                            )
                        }

                        // وضع التسجيل الصوتي
                        if (isRecordingAudio) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFEBEE), RoundedCornerShape(24.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFFD32F2F)
                                    )
                                    Text(
                                        text = "جاري التسجيل: ${recordingDuration}ث",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD32F2F),
                                        fontSize = 14.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { isRecordingAudio = false }) {
                                        Text("إلغاء", color = Color.Gray)
                                    }
                                    Button(
                                        onClick = {
                                            isRecordingAudio = false
                                            chatViewModel.sendMessage(
                                                channelId = channelId,
                                                senderId = currentUserId,
                                                senderName = currentUserName,
                                                recipientId = otherUserId,
                                                msgText = "🎤 تسجيل صوتي (${recordingDuration} ثانية)",
                                                mediaType = "AUDIO"
                                            )
                                            Toast.makeText(context, "تم إرسال التسجيل الصوتي 🎙️", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00668B))
                                    ) {
                                        Text("إرسال")
                                    }
                                }
                            }
                        } else {
                            // شريط الإدخال العادي
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // زر المرفقات من المعرض (صور)
                                IconButton(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "إرفاق صورة",
                                        tint = Color(0xFF00668B)
                                    )
                                }

                                // زر إرفاق مستندات (PDF / Word / Excel)
                                IconButton(
                                    onClick = { docPickerLauncher.launch("*/*") },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "إرفاق ملف",
                                        tint = Color(0xFF64748B)
                                    )
                                }

                                // حقل النص
                                OutlinedTextField(
                                    value = inputText,
                                    onValueChange = { newText ->
                                        chatViewModel.updateInputText(newText, channelId, currentUserId)
                                    },
                                    placeholder = { Text("اكتب رسالتك هنا...", fontSize = 14.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 46.dp, max = 120.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF00668B),
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    )
                                )

                                // زر الإرسال أو التسجيل
                                if (inputText.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            chatViewModel.sendMessage(
                                                channelId = channelId,
                                                senderId = currentUserId,
                                                senderName = currentUserName,
                                                recipientId = otherUserId,
                                                msgText = inputText.trim()
                                            )
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(Color(0xFF00668B), CircleShape)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "إرسال",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = { isRecordingAudio = true },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(Color(0xFF00668B).copy(alpha = 0.1f), CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "تسجيل صوتي",
                                            tint = Color(0xFF00668B),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            if (isLoading && messages.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (messages.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
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
                        "لا توجد رسائل سابقة",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                    Text(
                        "ابدأ المحادثة الآن بطرح استفسارك أو طلبك",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(messages, key = { _, msg -> msg.id }) { index, msg ->
                        val isMe = msg.senderId == currentUserId
                        
                        // تجميع الرسائل (Message Grouping): إظهار رأس التوقيت إذا كان الفارق أكثر من 5 دقائق
                        val prevMsg = messages.getOrNull(index - 1)
                        val isTimeGap = prevMsg == null || (msg.timestamp - prevMsg.timestamp) > (5 * 60 * 1000)

                        if (isTimeGap && msg.timestamp > 0) {
                            val timeHeader = java.text.SimpleDateFormat("dd MMM - hh:mm a", java.util.Locale("ar")).format(java.util.Date(msg.timestamp))
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFE2E8F0),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = timeHeader,
                                        fontSize = 10.sp,
                                        color = Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        ChatMessageComponent(
                            message = msg,
                            isMe = isMe,
                            showSenderName = userRole == "ADMIN",
                            searchQuery = searchQuery,
                            onReplyMessage = { msgToReply ->
                                chatViewModel.setReplyingTo(msgToReply)
                            },
                            onReactionSelect = { targetMsg, emoji ->
                                chatViewModel.toggleReaction(channelId, targetMsg.id, emoji, targetMsg.reactions)
                            },
                            onDeleteMessage = { msgId ->
                                chatViewModel.deleteMessage(channelId, msgId, currentUserId, deleteForEveryone = true)
                                Toast.makeText(context, "تم حذف الرسالة للجميع", Toast.LENGTH_SHORT).show()
                            },
                            onForwardMessage = { msgToForward ->
                                messageToForward = msgToForward
                            }
                        )
                    }
                }
            }
        }
    }

    // نافذة اختيار المحادثة لإعادة التوجيه (Forward Dialog)
    if (messageToForward != null) {
        Dialog(onDismissRequest = { messageToForward = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "إعادة توجيه الرسالة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "\"${messageToForward?.message}\"",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 2
                    )
                    HorizontalDivider()
                    Text("إرسال إلى:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                chatViewModel.sendMessage(
                                    channelId = channelId,
                                    senderId = currentUserId,
                                    senderName = currentUserName,
                                    recipientId = otherUserId,
                                    msgText = messageToForward?.message ?: "",
                                    mediaType = messageToForward?.mediaType ?: "TEXT",
                                    mediaUrl = messageToForward?.mediaUrl ?: "",
                                    forwardedFrom = messageToForward?.senderName ?: "مستخدم"
                                )
                                Toast.makeText(context, "تمت إعادة التوجيه بنجاح", Toast.LENGTH_SHORT).show()
                                messageToForward = null
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF00668B))
                        Text(otherUserName, fontSize = 14.sp)
                    }
                    Button(
                        onClick = { messageToForward = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                    ) {
                        Text("إلغاء", color = Color.Black)
                    }
                }
            }
        }
    }
}
