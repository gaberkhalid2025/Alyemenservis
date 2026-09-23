package com.example.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Test

/**
 * 💬 ChatScreenScreenshotTest
 * Visual screenshot testing for the Chat Screen between client and technician, featuring:
 * 1. Regular text message
 * 2. Replied message (Quote reply)
 * 3. Message with emoji reactions
 * Across Light, Dark, RTL, and LTR.
 */
class ChatScreenScreenshotTest : RoborazziTestBase() {

    @Test
    fun chatScreen_light() {
        captureScreen(
            screenshotName = "chat_screen_light",
            darkTheme = false,
            layoutDirection = LayoutDirection.Rtl
        ) {
            ChatScreenTestView()
        }
    }

    @Test
    fun chatScreen_dark() {
        captureScreen(
            screenshotName = "chat_screen_dark",
            darkTheme = true,
            layoutDirection = LayoutDirection.Rtl
        ) {
            ChatScreenTestView()
        }
    }

    @Test
    fun chatScreen_rtl() {
        captureScreen(
            screenshotName = "chat_screen_rtl",
            darkTheme = false,
            layoutDirection = LayoutDirection.Rtl
        ) {
            ChatScreenTestView()
        }
    }

    @Test
    fun chatScreen_ltr() {
        captureScreen(
            screenshotName = "chat_screen_ltr",
            darkTheme = false,
            layoutDirection = LayoutDirection.Ltr
        ) {
            ChatScreenTestView()
        }
    }
}

private data class ChatScreenshotMessage(
    val id: String,
    val senderName: String,
    val messageText: String,
    val time: String,
    val isFromMe: Boolean,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val reactions: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreenTestView() {
    val messages = listOf(
        ChatScreenshotMessage(
            id = "msg_1",
            senderName = "علي محمد الطالب (العميل)",
            messageText = "السلام عليكم يا باشمهندس، متى تقدر تمر لمعاينة لوحة الكهرباء بالمنزل؟",
            time = "02:15 PM",
            isFromMe = true
        ),
        ChatScreenshotMessage(
            id = "msg_2",
            senderName = "المهندس أحمد علي (فني كهرباء)",
            messageText = "وعليكم السلام ورحمة الله، بإذن الله بكون عندك اليوم الساعة 4:30 عصراً بعد الانتهاء من الطلب السابق.",
            time = "02:18 PM",
            isFromMe = false,
            replyToText = "السلام عليكم يا باشمهندس، متى تقدر تمر لمعاينة لوحة الكهرباء بالمنزل؟",
            replyToSender = "علي محمد الطالب"
        ),
        ChatScreenshotMessage(
            id = "msg_3",
            senderName = "علي محمد الطالب (العميل)",
            messageText = "ممتاز جداً وفي انتظارك، الموقع في حدة بجانب جولة الرويشان.",
            time = "02:20 PM",
            isFromMe = true,
            reactions = listOf("👍", "❤️")
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "المهندس أحمد علي (فني معتمد)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "متصل الآن",
                                    fontSize = 11.sp,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = "خيارات")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = "إرفاق ملف أو صورة",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        placeholder = { Text("اكتب رسالتك هنا...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val alignment = if (msg.isFromMe) Alignment.End else Alignment.Start
                val bubbleColor = if (msg.isFromMe) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                val textColor = if (msg.isFromMe) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = alignment
                ) {
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (msg.isFromMe) 16.dp else 4.dp,
                            bottomEnd = if (msg.isFromMe) 4.dp else 16.dp
                        ),
                        colors = CardDefaults.cardColors(containerColor = bubbleColor),
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Quote / Reply Block
                            if (msg.replyToText != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (msg.isFromMe) Color.Black.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = msg.replyToSender ?: "",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (msg.isFromMe) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = msg.replyToText,
                                            fontSize = 11.sp,
                                            color = if (msg.isFromMe) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // Message body
                            Text(
                                text = msg.messageText,
                                fontSize = 14.sp,
                                color = textColor,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.time,
                                fontSize = 10.sp,
                                color = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }

                    // Emoji Reactions
                    if (msg.reactions.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .offset(y = (-8).dp)
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            msg.reactions.forEach { emoji ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    shadowElevation = 2.dp
                                ) {
                                    Text(
                                        text = emoji,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
