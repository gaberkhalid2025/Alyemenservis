package com.example.ui.screens.chat.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

@Composable
fun ChatInputBar(
    replyingTo: ChatMessage?,
    onCancelReply: () -> Unit,
    onSendMessage: (text: String, mediaType: MediaType, mediaUrl: String) -> Unit,
    onTyping: (String) -> Unit
) {
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordDurationSeconds by remember { mutableStateOf(0) }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    // Audio timer effect
    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordDurationSeconds = 0
            while (isRecordingAudio) {
                kotlinx.coroutines.delay(1000L)
                recordDurationSeconds++
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val base64 = ImageUtils.uriToBase64(context, uri, 800, 75)
            if (base64.isNotEmpty()) {
                onSendMessage("", MediaType.IMAGE, "data:image/jpeg;base64,$base64")
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment ?: "file.pdf"
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

        // Input controls row
        if (isRecordingAudio) {
            val mins = recordDurationSeconds / 60
            val secs = recordDurationSeconds % 60
            // Audio Recording Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(22.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color.Red, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "جاري التسجيل... %02d:%02d 🎤".format(mins, secs),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { isRecordingAudio = false },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.DarkGray, CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "إلغاء", tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = {
                            isRecordingAudio = false
                            onSendMessage("تسجيل صوتي (%02d:%02d)".format(mins, secs), MediaType.AUDIO, "audio_record_${System.currentTimeMillis()}")
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال التسجيل", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Attachment Button with Dropdown
                Box {
                    IconButton(
                        onClick = { showAttachmentMenu = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "إرفاق", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    DropdownMenu(
                        expanded = showAttachmentMenu,
                        onDismissRequest = { showAttachmentMenu = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        DropdownMenuItem(
                            text = { Text("🖼️ صورة من المعرض", color = Color.White, fontSize = 12.sp) },
                            onClick = {
                                showAttachmentMenu = false
                                imagePickerLauncher.launch("image/*")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📄 مستند / ملف", color = Color.White, fontSize = 12.sp) },
                            onClick = {
                                showAttachmentMenu = false
                                filePickerLauncher.launch("*/*")
                            }
                        )
                    }
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

                if (textInput.isBlank()) {
                    // Audio Mic Button
                    IconButton(
                        onClick = { isRecordingAudio = true },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF1E88E5), CircleShape)
                    ) {
                        Text("🎤", fontSize = 18.sp)
                    }
                } else {
                    // Send Button
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
