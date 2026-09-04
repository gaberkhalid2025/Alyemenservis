package com.example.ui.screens.chat.components

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.models.ChatMessage
import com.example.data.models.MediaType
import com.example.ui.screens.chat.ChatAttachmentManager
import com.example.utils.ChatIcons
import com.example.utils.ChatValidationUtils
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ChatInputBar(
    channelId: String = "direct_chat",
    replyingTo: ChatMessage?,
    onCancelReply: () -> Unit,
    onSendMessage: (text: String, mediaType: MediaType, mediaUrl: String) -> Unit,
    onTyping: (String) -> Unit,
    themeColors: VisualThemePalette? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var textInput by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableIntStateOf(0) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var uploadProgress by remember { mutableStateOf<Float?>(null) }

    val primaryColor = themeColors?.primary ?: Color(0xFF1E88E5)
    val surfaceColor = themeColors?.surface ?: Color(0xFF142030)
    val inputBgColor = themeColors?.surface ?: Color(0xFF1E293B)
    val textPrimary = themeColors?.textPrimary ?: Color.White
    val textSecondary = themeColors?.textSecondary ?: Color.Gray
    val accentColor = themeColors?.accent ?: Color(0xFF64B5F6)
    val borderColor = themeColors?.border ?: Color.White.copy(alpha = 0.15f)

    val attachmentManager = remember { ChatAttachmentManager(context) }

    // 500ms Debounce for Typing Indicator
    LaunchedEffect(textInput) {
        if (textInput.isNotEmpty()) {
            onTyping(textInput)
            delay(500)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recording_scale"
    )

    // Image Picker with Strict Validation & Compression
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val validation = ChatValidationUtils.validateFile(uri, context)
            if (!validation.isValid) {
                Toast.makeText(context, validation.message, Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }

            scope.launch {
                uploadProgress = 0.2f
                val uploadResult = attachmentManager.uploadAttachment(
                    channelId = channelId,
                    uri = uri,
                    type = "image"
                )
                uploadProgress = 1.0f
                delay(200)
                uploadProgress = null

                uploadResult.onSuccess { downloadUrl ->
                    onSendMessage("", MediaType.IMAGE, downloadUrl)
                }.onFailure { exception ->
                    Toast.makeText(context, "فشل الرفع: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Permission launcher for Recording Audio
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "⚠️ يلزم منح إذن الميكروفون لتسجيل وإرسال الرسائل الصوتية", Toast.LENGTH_LONG).show()
        }
    }

    // Timer for active recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDuration = 0
            while (isRecording) {
                delay(1000)
                recordingDuration++
            }
        }
    }

    fun startRecording() {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        try {
            val file = File(context.cacheDir, "audio_rec_${System.currentTimeMillis()}.mp3")
            audioFile = file
            val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            isRecording = true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "تعذر تشغيل مسجل الصوت", Toast.LENGTH_SHORT).show()
            audioFile?.delete()
            audioFile = null
            isRecording = false
        }
    }

    fun stopAndSendRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            isRecording = false
        }

        val file = audioFile
        if (file != null && file.exists() && recordingDuration >= 1) {
            val uri = Uri.fromFile(file)
            val validation = ChatValidationUtils.validateFile(uri, context)
            if (!validation.isValid) {
                Toast.makeText(context, validation.message, Toast.LENGTH_LONG).show()
                file.delete()
                audioFile = null
                recordingDuration = 0
                return
            }

            scope.launch {
                uploadProgress = 0.2f
                val uploadResult = attachmentManager.uploadAttachment(
                    channelId = channelId,
                    uri = uri,
                    type = "audio"
                )
                uploadProgress = 1.0f
                delay(200)
                uploadProgress = null

                uploadResult.onSuccess { downloadUrl ->
                    onSendMessage(
                        "تسجيل صوتي (${recordingDuration}ث)",
                        MediaType.AUDIO,
                        downloadUrl
                    )
                }.onFailure { exception ->
                    Toast.makeText(context, "فشل الرفع: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
                // Cleanup temporary audio file from cache
                file.delete()
            }
        } else {
            file?.delete()
        }
        audioFile = null
        recordingDuration = 0
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            isRecording = false
        }
        audioFile?.delete()
        audioFile = null
        recordingDuration = 0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Upload Progress Indicator
        if (uploadProgress != null) {
            LinearProgressIndicator(
                progress = uploadProgress ?: 0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = primaryColor,
                trackColor = borderColor
            )
        }

        // Reply banner
        if (replyingTo != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .background(inputBgColor, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الرد على ${replyingTo.senderName}:",
                        fontSize = 11.sp,
                        color = accentColor
                    )
                    Text(
                        text = replyingTo.message,
                        fontSize = 12.sp,
                        color = textSecondary,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "إلغاء", tint = textSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Active Recording Status Indicator Bar
        AnimatedVisibility(visible = isRecording) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .background(Color(0xFF111827), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFFE53935).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .scale(pulseScale)
                            .background(Color(0xFFE53935), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val minutes = recordingDuration / 60
                    val seconds = recordingDuration % 60
                    val timerStr = String.format("%02d:%02d", minutes, seconds)
                    Text(
                        text = "🎙️ جاري التسجيل... ($timerStr)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { cancelRecording() },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "إلغاء التسجيل", tint = Color(0xFFFF8A80), modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = { stopAndSendRecording() },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال التسجيل", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Input controls row
        if (!isRecording) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Attachment Button
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(40.dp)
                        .background(textPrimary.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إرفاق صورة", tint = textPrimary, modifier = Modifier.size(20.dp))
                }

                // Text field (Restricted to 500 characters max)
                OutlinedTextField(
                    value = textInput,
                    onValueChange = {
                        if (it.length <= ChatValidationUtils.MAX_TEXT_LENGTH) {
                            textInput = it
                        }
                    },
                    placeholder = { Text("اكتب رسالتك هنا (الحد 500 حرف)...", color = textSecondary, fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = borderColor,
                        focusedContainerColor = inputBgColor,
                        unfocusedContainerColor = inputBgColor
                    ),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        val trimmed = textInput.trim()
                        if (trimmed.isNotBlank()) {
                            onSendMessage(trimmed, MediaType.TEXT, "")
                            textInput = ""
                        }
                    })
                )

                // Send or Record Button
                if (textInput.isNotBlank()) {
                    IconButton(
                        onClick = {
                            val trimmed = textInput.trim()
                            if (trimmed.isNotBlank()) {
                                onSendMessage(trimmed, MediaType.TEXT, "")
                                textInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(primaryColor, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                } else {
                    // Audio Record Mic Button
                    IconButton(
                        onClick = { startRecording() },
                        modifier = Modifier
                            .size(42.dp)
                            .background(primaryColor, CircleShape)
                    ) {
                        Icon(ChatIcons.Mic, contentDescription = "تسجيل صوتي", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}
