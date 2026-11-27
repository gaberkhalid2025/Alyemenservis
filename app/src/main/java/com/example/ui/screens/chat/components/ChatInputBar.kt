package com.example.ui.screens.chat.components

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.models.ChatMessage
import com.example.data.models.MediaType
import com.example.ui.screens.chat.ChatAttachmentManager
import com.example.utils.ChatIcons
import com.example.utils.ChatValidationUtils
import com.example.utils.OfflineYemenGazetteer
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ChatInputBar(
    channelId: String = "direct_chat",
    replyingTo: ChatMessage? = null,
    editingMessage: ChatMessage? = null,
    onCancelReply: () -> Unit = {},
    onCancelEdit: () -> Unit = {},
    onSendMessage: (text: String, mediaType: MediaType, mediaUrl: String) -> Unit,
    onSendAudioMessage: ((text: String, mediaUrl: String, waveform: List<Int>) -> Unit)? = null,
    onMediaUploaded: ((localUri: String, downloadUrl: String) -> Unit)? = null,
    onMediaUploadFailed: ((localUri: String, error: String) -> Unit)? = null,
    onEditMessage: ((messageId: String, newText: String) -> Unit)? = null,
    onTyping: (String) -> Unit = {},
    themeColors: VisualThemePalette? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    var textInput by remember { mutableStateOf("") }
    var uploadProgress by remember { mutableStateOf<Float?>(null) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showMapLocationPicker by remember { mutableStateOf(false) }

    var previewImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageCaption by remember { mutableStateOf("") }

    var previewVideoUri by remember { mutableStateOf<Uri?>(null) }
    var videoCaption by remember { mutableStateOf("") }

    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            pendingCameraFile?.delete()
        }
    }

    LaunchedEffect(editingMessage) {
        if (editingMessage != null) {
            textInput = editingMessage.message
        }
    }

    val primaryColor = themeColors?.primary ?: Color(0xFF1E88E5)
    val surfaceColor = themeColors?.surface ?: Color(0xFF142030)
    val inputBgColor = themeColors?.surface ?: Color(0xFF1E293B)
    val textPrimary = themeColors?.textPrimary ?: Color.White
    val textSecondary = themeColors?.textSecondary ?: Color.Gray
    val accentColor = themeColors?.accent ?: Color(0xFF64B5F6)
    val borderColor = themeColors?.border ?: Color.White.copy(alpha = 0.15f)

    val attachmentManager = remember { ChatAttachmentManager(context) }

    val recorderState = rememberChatAudioRecorderState(
        context = context,
        scope = scope,
        hapticFeedback = hapticFeedback,
        channelId = channelId,
        attachmentManager = attachmentManager,
        onSendMessage = onSendMessage,
        onSendAudioMessage = onSendAudioMessage,
        onMediaUploaded = onMediaUploaded,
        onMediaUploadFailed = onMediaUploadFailed
    )

    LaunchedEffect(textInput) {
        if (textInput.isNotEmpty()) {
            onTyping(textInput)
            delay(500)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val validation = ChatValidationUtils.validateFile(uri, context, true)
            if (!validation.isValid) {
                Toast.makeText(context, validation.message, Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }
            previewImageUri = uri
            imageCaption = ""
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val validation = ChatValidationUtils.validateFile(uri, context, isImage = false, isVideo = true)
            if (!validation.isValid) {
                Toast.makeText(context, validation.message, Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }
            previewVideoUri = uri
            videoCaption = ""
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val cameraUri = pendingCameraUri
        if (success && cameraUri != null) {
            val validation = ChatValidationUtils.validateFile(cameraUri, context, true)
            if (validation.isValid) {
                previewImageUri = cameraUri
                imageCaption = ""
            } else {
                Toast.makeText(context, validation.message, Toast.LENGTH_LONG).show()
                pendingCameraFile?.delete()
                pendingCameraFile = null
                pendingCameraUri = null
            }
        } else {
            pendingCameraFile?.delete()
            pendingCameraFile = null
            pendingCameraUri = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
                val file = File(sharedDir, "camera_${System.currentTimeMillis()}.jpg")
                pendingCameraFile = file
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "تعذر تشغيل الكاميرا: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "⚠️ يلزم منح إذن الكاميرا لالتقاط الصور", Toast.LENGTH_LONG).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            sendCurrentLocation(context, onSendMessage) {
                showMapLocationPicker = true
            }
        } else {
            Toast.makeText(context, "⚠️ يمكنك تحديد موقعك مباشرة على خريطة التطبيق", Toast.LENGTH_SHORT).show()
            showMapLocationPicker = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .border(1.dp, borderColor)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        if (uploadProgress != null) {
            LinearProgressIndicator(
                progress = { uploadProgress ?: 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = primaryColor,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (editingMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "✏️ تعديل الرسالة...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Text(
                        text = editingMessage.message,
                        fontSize = 12.sp,
                        color = textSecondary,
                        maxLines = 1
                    )
                }
                IconButton(onClick = {
                    onCancelEdit()
                    textInput = ""
                }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "إلغاء التعديل", tint = textSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

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
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = replyingTo.message.ifBlank { "مرفق وسائط" },
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

        ChatRecordingBar(
            state = recorderState,
            hapticFeedback = hapticFeedback
        )

        if (!recorderState.isRecording) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box {
                    IconButton(
                        onClick = { showAttachmentMenu = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(textPrimary.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "إرفاق", tint = textPrimary, modifier = Modifier.size(20.dp))
                    }

                    ChatAttachmentDropdownMenu(
                        expanded = showAttachmentMenu,
                        onDismiss = { showAttachmentMenu = false },
                        surfaceColor = surfaceColor,
                        textPrimary = textPrimary,
                        accentColor = accentColor,
                        context = context,
                        onPickImage = { imagePickerLauncher.launch("image/*") },
                        onPickVideo = { videoPickerLauncher.launch("video/*") },
                        onLaunchCamera = { file, uri ->
                            pendingCameraFile = file
                            pendingCameraUri = uri
                            cameraLauncher.launch(uri)
                        },
                        onRequestCameraPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        onSendCurrentLocation = {
                            sendCurrentLocation(context, onSendMessage) {
                                showMapLocationPicker = true
                            }
                        },
                        onRequestLocationPermissions = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        onSendLiveLocation = { currentLoc ->
                            val lat = currentLoc.latitude
                            val lng = currentLoc.longitude
                            val summary = OfflineYemenGazetteer.nearestReferenceSummary(context, lat, lng).ifBlank { "موقعي المباشر" }
                            val appUri = OfflineYemenGazetteer.buildAppLocationUri(lat, lng, summary) + "&live=1"
                            onSendMessage("🟢 بث موقع حي مباشر: $summary", MediaType.LOCATION, appUri)
                            Toast.makeText(context, "✅ تم إرسال رابط التتبع الحي على خريطة التطبيق", Toast.LENGTH_SHORT).show()
                        },
                        onOpenMapPicker = { showMapLocationPicker = true }
                    )
                }

                OutlinedTextField(
                    value = textInput,
                    onValueChange = {
                        if (it.length <= ChatValidationUtils.MAX_TEXT_LENGTH) {
                            textInput = it
                        }
                    },
                    leadingIcon = {
                        IconButton(
                            onClick = { showEmojiPicker = !showEmojiPicker },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text(
                                text = if (showEmojiPicker) "⌨️" else "😊",
                                fontSize = 16.sp
                            )
                        }
                    },
                    placeholder = { Text("اكتب رسالتك هنا...", color = textSecondary, fontSize = 13.sp) },
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
                            if (editingMessage != null && onEditMessage != null) {
                                onEditMessage(editingMessage.id, trimmed)
                                onCancelEdit()
                            } else {
                                onSendMessage(trimmed, MediaType.TEXT, "")
                            }
                            textInput = ""
                            showEmojiPicker = false
                        }
                    })
                )

                if (textInput.isNotBlank()) {
                    IconButton(
                        onClick = {
                            val trimmed = textInput.trim()
                            if (trimmed.isNotBlank()) {
                                if (editingMessage != null && onEditMessage != null) {
                                    onEditMessage(editingMessage.id, trimmed)
                                    onCancelEdit()
                                } else {
                                    onSendMessage(trimmed, MediaType.TEXT, "")
                                }
                                textInput = ""
                                showEmojiPicker = false
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(primaryColor, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                } else {
                    IconButton(
                        onClick = { recorderState.startRecording() },
                        modifier = Modifier
                            .size(42.dp)
                            .background(primaryColor, CircleShape)
                    ) {
                        Icon(ChatIcons.Mic, contentDescription = "تسجيل صوتي", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }

            AnimatedVisibility(visible = showEmojiPicker) {
                ChatEmojiTray(
                    accentColor = accentColor,
                    textSecondary = textSecondary,
                    onClose = { showEmojiPicker = false },
                    onEmojiSelected = { emoji ->
                        if (textInput.length + emoji.length <= ChatValidationUtils.MAX_TEXT_LENGTH) {
                            textInput += emoji
                        }
                    }
                )
            }
        }
    }

    val activePreviewUri = previewImageUri
    if (activePreviewUri != null) {
        ChatImagePreviewDialog(
            activePreviewUri = activePreviewUri,
            imageCaption = imageCaption,
            onCaptionChange = { imageCaption = it },
            primaryColor = primaryColor,
            surfaceColor = surfaceColor,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            borderColor = borderColor,
            onSend = { uriToSend, captionToSend ->
                previewImageUri = null
                imageCaption = ""
                val localImageUriStr = uriToSend.toString()
                onSendMessage(captionToSend, MediaType.IMAGE, localImageUriStr)

                scope.launch {
                    val uploadResult = attachmentManager.uploadAttachment(
                        channelId = channelId,
                        uri = uriToSend,
                        type = "image"
                    )
                    uploadResult.fold(
                        onSuccess = { downloadUrl ->
                            onMediaUploaded?.invoke(localImageUriStr, downloadUrl)
                        },
                        onFailure = { error ->
                            onMediaUploadFailed?.invoke(localImageUriStr, error.message ?: "فشل رفع الصورة")
                        }
                    )
                    pendingCameraFile?.delete()
                    pendingCameraFile = null
                }
            },
            onDismiss = {
                previewImageUri = null
                imageCaption = ""
                pendingCameraFile?.delete()
                pendingCameraFile = null
            }
        )
    }

    val activePreviewVideoUri = previewVideoUri
    if (activePreviewVideoUri != null) {
        ChatVideoPreviewDialog(
            activePreviewVideoUri = activePreviewVideoUri,
            videoCaption = videoCaption,
            onCaptionChange = { videoCaption = it },
            primaryColor = primaryColor,
            surfaceColor = surfaceColor,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            borderColor = borderColor,
            onSend = { uriToSend, captionToSend ->
                previewVideoUri = null
                videoCaption = ""
                val localVideoUriStr = uriToSend.toString()
                onSendMessage(captionToSend, MediaType.VIDEO, localVideoUriStr)

                scope.launch {
                    val uploadResult = attachmentManager.uploadAttachment(
                        channelId = channelId,
                        uri = uriToSend,
                        type = "video"
                    )
                    uploadResult.fold(
                        onSuccess = { downloadUrl ->
                            onMediaUploaded?.invoke(localVideoUriStr, downloadUrl)
                        },
                        onFailure = { error ->
                            onMediaUploadFailed?.invoke(localVideoUriStr, error.message ?: "فشل رفع الفيديو")
                        }
                    )
                }
            },
            onDismiss = {
                previewVideoUri = null
                videoCaption = ""
            }
        )
    }

    if (showMapLocationPicker) {
        ChatMapLocationPickerHost(
            context = context,
            onDismiss = { showMapLocationPicker = false },
            onSendMessage = onSendMessage
        )
    }
}

@Composable
private fun ChatEmojiTray(
    accentColor: Color,
    textSecondary: Color,
    onClose: () -> Unit,
    onEmojiSelected: (String) -> Unit
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        val emojiCategories = remember {
            listOf(
                listOf("😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇"),
                listOf("🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😋", "😛"),
                listOf("👍", "👎", "👏", "🙌", "🤝", "🙏", "✌️", "🤞", "💪", "❤️"),
                listOf("🔥", "⭐", "🎉", "✨", "💯", "✅", "⚠️", "🚨", "⏳", "📌"),
                listOf("💼", "🛠️", "🔧", "🏪", "🏥", "🍕", "🚗", "🏠", "📍", "📞")
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "رموز تعبيرية وتفاعلات سريعة",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = textSecondary, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            emojiCategories.forEach { rowEmojis ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowEmojis.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable { onEmojiSelected(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}
