package com.example.ui.screens.chat.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.models.MediaType
import com.example.ui.screens.chat.ChatAttachmentManager
import com.example.utils.ChatValidationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

internal class ChatAudioRecorderState(
    private val context: Context,
    private val scope: CoroutineScope,
    private val hapticFeedback: HapticFeedback,
    private val channelId: String,
    private val attachmentManager: ChatAttachmentManager,
    private val onSendMessage: (text: String, mediaType: MediaType, mediaUrl: String) -> Unit,
    private val onSendAudioMessage: ((text: String, mediaUrl: String, waveform: List<Int>) -> Unit)?,
    private val onMediaUploaded: ((localUri: String, downloadUrl: String) -> Unit)?,
    private val onMediaUploadFailed: ((localUri: String, error: String) -> Unit)?,
    private val onRequestMicPermission: () -> Unit
) {
    var isRecording by mutableStateOf(false)
    var isLockedRecording by mutableStateOf(false)
    var recordingDuration by mutableIntStateOf(0)
    var mediaRecorder by mutableStateOf<MediaRecorder?>(null)
    var audioFile by mutableStateOf<File?>(null)
    val liveAmplitudes: SnapshotStateList<Int> = mutableStateListOf()

    fun startRecording() {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            onRequestMicPermission()
            return
        }

        try {
            val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(sharedDir, "audio_rec_${System.currentTimeMillis()}.m4a")
            audioFile = file
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
            isLockedRecording = false
            liveAmplitudes.clear()
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "تعذر تشغيل مسجل الصوت", Toast.LENGTH_SHORT).show()
            audioFile?.delete()
            audioFile = null
            isRecording = false
            isLockedRecording = false
        }
    }

    fun stopAndSendRecording() {
        val capturedWaveform = liveAmplitudes.toList()
        try {
            mediaRecorder?.let { recorder ->
                try { recorder.stop() } catch (_: Exception) {}
                try { recorder.release() } catch (_: Exception) {}
            }
        } finally {
            mediaRecorder = null
            isRecording = false
            isLockedRecording = false
        }

        val file = audioFile
        if (file != null && file.exists() && recordingDuration >= 1) {
            val uri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            if (uri == null) {
                Toast.makeText(context, "فشل تجهيز التسجيل الصوتي", Toast.LENGTH_SHORT).show()
                file.delete()
                audioFile = null
                recordingDuration = 0
                return
            }
            val validation = ChatValidationUtils.validateFile(uri, context, false)
            if (!validation.isValid) {
                Toast.makeText(context, validation.message, Toast.LENGTH_LONG).show()
                file.delete()
                audioFile = null
                recordingDuration = 0
                return
            }

            val recordedDurationText = "تسجيل صوتي (${recordingDuration}ث)"
            val localAudioUri = uri.toString()
            val waveformToSend = capturedWaveform.toList()

            if (onSendAudioMessage != null) {
                onSendAudioMessage.invoke(recordedDurationText, localAudioUri, waveformToSend)
            } else {
                onSendMessage(recordedDurationText, MediaType.AUDIO, localAudioUri)
            }

            scope.launch {
                val uploadResult = attachmentManager.uploadAttachment(
                    channelId = channelId,
                    uri = uri,
                    type = "audio"
                )
                uploadResult.fold(
                    onSuccess = { downloadUrl ->
                        onMediaUploaded?.invoke(localAudioUri, downloadUrl)
                    },
                    onFailure = { error ->
                        onMediaUploadFailed?.invoke(localAudioUri, error.message ?: "فشل رفع التسجيل الصوتي")
                    }
                )
                file.delete()
            }
        } else {
            file?.delete()
        }
        audioFile = null
        recordingDuration = 0
    }

    fun cancelRecording() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        try {
            mediaRecorder?.let { recorder ->
                try { recorder.stop() } catch (_: Exception) {}
                try { recorder.release() } catch (_: Exception) {}
            }
        } finally {
            mediaRecorder = null
            isRecording = false
            isLockedRecording = false
        }
        audioFile?.delete()
        audioFile = null
        recordingDuration = 0
        liveAmplitudes.clear()
        Toast.makeText(context, "تم إلغاء التسجيل", Toast.LENGTH_SHORT).show()
    }

    fun releaseOnDispose() {
        try {
            mediaRecorder?.let { recorder ->
                try { recorder.stop() } catch (_: Exception) {}
                try { recorder.release() } catch (_: Exception) {}
            }
        } finally {
            mediaRecorder = null
            audioFile?.delete()
            audioFile = null
            isRecording = false
            isLockedRecording = false
        }
    }
}

@Composable
internal fun rememberChatAudioRecorderState(
    context: Context,
    scope: CoroutineScope,
    hapticFeedback: HapticFeedback,
    channelId: String,
    attachmentManager: ChatAttachmentManager,
    onSendMessage: (text: String, mediaType: MediaType, mediaUrl: String) -> Unit,
    onSendAudioMessage: ((text: String, mediaUrl: String, waveform: List<Int>) -> Unit)?,
    onMediaUploaded: ((localUri: String, downloadUrl: String) -> Unit)?,
    onMediaUploadFailed: ((localUri: String, error: String) -> Unit)?
): ChatAudioRecorderState {
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "⚠️ يلزم منح إذن الميكروفون لتسجيل وإرسال الرسائل الصوتية", Toast.LENGTH_LONG).show()
        }
    }

    val state = remember(channelId) {
        ChatAudioRecorderState(
            context = context,
            scope = scope,
            hapticFeedback = hapticFeedback,
            channelId = channelId,
            attachmentManager = attachmentManager,
            onSendMessage = onSendMessage,
            onSendAudioMessage = onSendAudioMessage,
            onMediaUploaded = onMediaUploaded,
            onMediaUploadFailed = onMediaUploadFailed,
            onRequestMicPermission = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        )
    }

    LaunchedEffect(state.isRecording) {
        if (state.isRecording) {
            state.recordingDuration = 0
            state.liveAmplitudes.clear()
            var tickCount = 0
            while (isActive && state.isRecording) {
                delay(100)
                tickCount++
                if (tickCount % 10 == 0) {
                    state.recordingDuration++
                }
                val amp = try { state.mediaRecorder?.maxAmplitude ?: 0 } catch (_: Exception) { 0 }
                val normalized = (amp / 32767f * 100).toInt().coerceIn(12, 100)
                if (state.liveAmplitudes.size >= 32) {
                    state.liveAmplitudes.removeAt(0)
                }
                state.liveAmplitudes.add(normalized)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            state.releaseOnDispose()
        }
    }

    return state
}

@Composable
internal fun ChatRecordingBar(
    state: ChatAudioRecorderState,
    hapticFeedback: HapticFeedback
) {
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recording_scale"
    )

    AnimatedVisibility(visible = state.isRecording) {
        var dragOffset by remember { mutableFloatStateOf(0f) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFFE53935).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragOffset < -80f) {
                                state.cancelRecording()
                            }
                            dragOffset = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffset += dragAmount
                            if (dragOffset < -80f) {
                                state.cancelRecording()
                                dragOffset = 0f
                            }
                        }
                    )
                }
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
                val minutes = state.recordingDuration / 60
                val seconds = state.recordingDuration % 60
                val timerStr = String.format("%02d:%02d", minutes, seconds)
                Text(
                    text = timerStr,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = 3.dp.toPx()
                    val barSpacing = 2.dp.toPx()
                    val totalBars = state.liveAmplitudes.size
                    val startX = (size.width - (totalBars * (barWidth + barSpacing))).coerceAtLeast(0f) / 2f

                    state.liveAmplitudes.forEachIndexed { index, amp ->
                        val barHeight = ((amp / 100f) * size.height).coerceAtLeast(4.dp.toPx())
                        val x = startX + index * (barWidth + barSpacing)
                        val y = (size.height - barHeight) / 2f
                        drawRoundRect(
                            color = Color(0xFF10B981),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = {
                        state.isLockedRecording = !state.isLockedRecording
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(if (state.isLockedRecording) Color(0xFF2563EB) else Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (state.isLockedRecording) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "قفل التسجيل",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { state.cancelRecording() },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "إلغاء التسجيل", tint = Color(0xFFFF8A80), modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = { state.stopAndSendRecording() },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF10B981), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال التسجيل", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
