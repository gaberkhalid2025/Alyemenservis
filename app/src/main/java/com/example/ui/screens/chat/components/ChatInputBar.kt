package com.example.ui.screens.chat.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ChatMessage
import com.example.data.models.MediaType
import com.example.util.ImageUtils
import kotlinx.coroutines.delay

@Composable
fun ChatInputBar(
    replyingTo: ChatMessage?,
    onCancelReply: () -> Unit,
    onSendMessage: (text: String, mediaType: MediaType, mediaUrl: String) -> Unit,
    onTyping: (String) -> Unit
) {
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingTimeSeconds by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    // Timer effect for audio recording
    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingTimeSeconds = 0
            while (isRecordingAudio) {
                delay(1000)
                recordingTimeSeconds++
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val base64 = ImageUtils.uriToBase64(context, uri, 800, 75)
            if (base64.isNotEmpty()) {
                onSendMessage("صورة", MediaType.IMAGE, "data:image/jpeg;base64,$base64")
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "document.pdf"
            onSendMessage(fileName, MediaType.FILE, uri.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF142030))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Reply banner
        if (replyingTo != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الرد على ${replyingTo.senderName}:",
                        fontSize = 11.sp,
                        color = Color(0xFF64B5F6)
                    )
                    Text(
                        text = replyingTo.message,
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "إلغاء", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Attachment Sheet Option Row
        AnimatedVisibility(visible = showAttachmentMenu) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                    showAttachmentMenu = false
                    imagePickerLauncher.launch("image/*")
                }) {
                    Box(modifier = Modifier.size(40.dp).background(Color(0xFF2563EB), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Text("معرض الصور", fontSize = 11.sp, color = Color.White)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                    showAttachmentMenu = false
                    filePickerLauncher.launch("*/*")
                }) {
                    Box(modifier = Modifier.size(40.dp).background(Color(0xFF9333EA), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Text("مستند/ملف", fontSize = 11.sp, color = Color.White)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                    showAttachmentMenu = false
                    imagePickerLauncher.launch("video/*")
                }) {
                    Box(modifier = Modifier.size(40.dp).background(Color(0xFF059669), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Text("فيديو", fontSize = 11.sp, color = Color.White)
                }
            }
        }

        // Recording Audio Bar
        if (isRecordingAudio) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF991B1B), RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري التسجيل... ${recordingTimeSeconds}s", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { isRecordingAudio = false }) {
                        Text("إلغاء", color = Color.LightGray, fontSize = 12.sp)
                    }
                    IconButton(
                        onClick = {
                            isRecordingAudio = false
                            onSendMessage("تسجيل صوتي (${recordingTimeSeconds}ث)", MediaType.AUDIO, "audio_recorded.mp3")
                        },
                        modifier = Modifier.size(36.dp).background(Color.White, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال الصوت", tint = Color(0xFF991B1B), modifier = Modifier.size(18.dp))
                    }
                }
            }
        } else {
            // Standard Input controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Attachment Toggle Button
                IconButton(
                    onClick = { showAttachmentMenu = !showAttachmentMenu },
                    modifier = Modifier
                        .size(40.dp)
                        .background(if (showAttachmentMenu) Color(0xFF1E88E5) else Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        if (showAttachmentMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "إرفاق",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Text field
                OutlinedTextField(
                    value = textInput,
                    onValueChange = {
                        textInput = it
                        onTyping(it)
                    },
                    placeholder = { Text("اكتب رسالتك هنا...", color = Color.Gray, fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF1E88E5),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (textInput.isNotBlank()) {
                            onSendMessage(textInput, MediaType.TEXT, "")
                            textInput = ""
                        }
                    })
                )

                // Mic or Send Button
                if (textInput.isBlank()) {
                    IconButton(
                        onClick = { isRecordingAudio = true },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF334155), CircleShape)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "تسجيل صوتي", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                onSendMessage(textInput, MediaType.TEXT, "")
                                textInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF1E88E5), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
