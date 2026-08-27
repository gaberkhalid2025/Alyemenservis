package com.example.ui.screens.chat.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.core.content.ContextCompat
import com.example.data.models.ChatMessage
import com.example.data.models.MediaType
import com.example.util.ImageUtils
import com.example.util.VoiceNoteManager
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun ChatInputBar(
    replyingTo: ChatMessage?,
    onCancelReply: () -> Unit,
    onSendMessage: (text: String, mediaType: MediaType, mediaUrl: String, durationSec: Int) -> Unit,
    onSendVoiceFile: (file: File, durationSec: Int) -> Unit,
    onTyping: (String) -> Unit
) {
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    val voiceNoteManager = remember { VoiceNoteManager(context) }

    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }

    // Coroutine timer for recording duration
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceNoteManager.release()
        }
    }

    // Permission launcher for Recording Audio
    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = voiceNoteManager.startRecording()
            if (file != null) {
                isRecording = true
            }
        }
    }

    // Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val base64 = ImageUtils.uriToBase64(context, uri, 900, 80)
            if (base64.isNotEmpty()) {
                onSendMessage("", MediaType.IMAGE, "data:image/jpeg;base64,$base64", 0)
            }
        }
    }

    // File / Document Picker
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val docUriStr = uri.toString()
            onSendMessage("مرفق ملف", MediaType.FILE, docUriStr, 0)
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
                        text = replyingTo.message.ifBlank { "مرفق وسائط" },
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

        // Recording active bar
        if (isRecording) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(24.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Cancel recording button
                IconButton(
                    onClick = {
                        isRecording = false
                        voiceNoteManager.cancelRecording()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "إلغاء التسجيل", tint = Color(0xFFEF5350))
                }

                // Recording timer & pulsating indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFEF5350), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val mins = recordingSeconds / 60
                    val secs = recordingSeconds % 60
                    Text(
                        text = String.format("%02d:%02d", mins, secs),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "جارِ التسجيل...",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }

                // Finish and send audio
                IconButton(
                    onClick = {
                        val result = voiceNoteManager.stopRecording()
                        isRecording = false
                        if (result != null) {
                            onSendVoiceFile(result.first, result.second)
                        }
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF1E88E5), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال الصوت", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        } else {
            // Standard Input controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Image Attachment Button
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إرفاق صورة", tint = Color(0xFF90CAF9), modifier = Modifier.size(19.dp))
                }

                // File Attachment Button
                IconButton(
                    onClick = { docPickerLauncher.launch("*/*") },
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "إرفاق ملف", tint = Color.LightGray, modifier = Modifier.size(19.dp))
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
                            onSendMessage(textInput, MediaType.TEXT, "", 0)
                            textInput = ""
                        }
                    })
                )

                // Voice Recording or Text Send Button
                if (textInput.isNotBlank()) {
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                onSendMessage(textInput, MediaType.TEXT, "", 0)
                                textInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF1E88E5), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = Color.White, modifier = Modifier.size(19.dp))
                    }
                } else {
                    // Mic button for recording
                    IconButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                val file = voiceNoteManager.startRecording()
                                if (file != null) {
                                    isRecording = true
                                }
                            } else {
                                recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF1E88E5), CircleShape)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "تسجيل صوتي", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
