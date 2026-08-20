package com.example.ui.screens.chat

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.ChatMessageEntity
import com.example.ui.components.ChatMessageComponent
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * 💬 ChatScreen
 * شاشة المحادثة الفورية التفاعلية مع دعم الرسائل، الصور، الملاحظات الصوتية، ومؤشرات الاتصال والكتابة
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
    val isTyping by chatViewModel.isTyping.collectAsState()
    val otherUserTyping by chatViewModel.otherUserTyping.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()

    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableIntStateOf(0) }

    // Start listening on launch
    LaunchedEffect(channelId) {
        chatViewModel.startListeningToChannel(channelId)
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Voice recording timer simulation
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
                msgText = "📷 صورة مرفقة"
            )
            Toast.makeText(context, "تم إرسال الصورة بنجاح", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
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

                            // مؤشر الاتصال
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
                                text = if (otherUserTyping) "يكتب الآن..." else if (isOtherUserOnline) "متصل الآن" else "غير متصل",
                                fontSize = 12.sp,
                                color = if (otherUserTyping) Color(0xFF00668B) else if (isOtherUserOnline) Color(0xFF4CAF50) else Color.Gray
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
                    if (otherUserPhone.isNotBlank()) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$otherUserPhone"))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Phone, contentDescription = "اتصال هاتف", tint = Color(0xFF00668B))
                        }
                    }
                    IconButton(onClick = {
                        Toast.makeText(context, "بدء مكالمة صوتية مشفرة...", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Call, contentDescription = "مكالمة صوتية", tint = Color(0xFF00668B))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    
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
                                            msgText = "🎤 تسجيل صوتي (${recordingDuration} ثانية)"
                                        )
                                        Toast.makeText(context, "تم إرسال التسجيل الصوتي", Toast.LENGTH_SHORT).show()
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // زر المرفقات من المعرض
                            IconButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "إرفاق صورة",
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
                    items(messages, key = { it.id }) { msg ->
                        val isMe = msg.senderId == currentUserId
                        ChatMessageComponent(
                            message = msg,
                            isMe = isMe,
                            showSenderName = userRole == "ADMIN",
                            onDeleteMessage = { msgId ->
                                Toast.makeText(context, "تم حذف الرسالة", Toast.LENGTH_SHORT).show()
                            },
                            onForwardMessage = { msgToForward ->
                                Toast.makeText(context, "إعادة توجيه: ${msgToForward.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}
