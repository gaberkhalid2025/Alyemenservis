package com.example.ui.screens.chat.managers

import android.content.Context
import android.net.Uri
import com.example.NetworkUtils
import com.example.data.models.ChatAttachment
import com.example.data.models.ChatMessage
import com.example.data.models.MediaType
import com.example.data.models.MessageStatus
import com.example.data.repositories.IChatRepository
import com.example.ui.screens.chat.ChatAttachmentManager
import com.example.utils.AppResult
import com.example.utils.EntityIdGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ChatMessagesManager(
    private val repository: IChatRepository,
    private val scope: CoroutineScope,
    private val context: Context? = null
) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val attachmentManager by lazy { context?.let { ChatAttachmentManager(it) } }

    /**
     * تحديث قائمة الرسائل مع الحفاظ الصارم على الرسائل المحلية المعلقة (SENDING / PENDING)
     * لمنع اختفاء الرسائل أو مسحها من الواجهة أثناء المزامنة.
     */
    fun updateMessagesList(list: List<ChatMessage>) {
        val current = _messages.value
        val pendingOrSending = current.filter { localMsg ->
            (localMsg.status == MessageStatus.SENDING || localMsg.status == MessageStatus.PENDING) &&
            list.none { remote -> remote.id == localMsg.id }
        }
        val combined = (list + pendingOrSending).distinctBy { it.id }.sortedBy { it.timestamp }
        _messages.value = combined
    }

    fun setSending(sending: Boolean) {
        _isSending.value = sending
    }

    fun sendMessage(
        channelId: String,
        senderId: String,
        senderName: String,
        text: String,
        mediaType: MediaType = MediaType.TEXT,
        mediaUrl: String = "",
        replyTo: ChatMessage? = null,
        attachment: ChatAttachment? = null,
        audioWaveform: List<Int> = emptyList(),
        onSuccess: (ChatMessage) -> Unit,
        onError: (String) -> Unit
    ) {
        val messageId = EntityIdGenerator.generate("msg")
        val timestamp = System.currentTimeMillis()
        val localMessage = ChatMessage(
            id = messageId,
            channelId = channelId,
            senderId = senderId,
            senderName = senderName,
            message = text,
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            replyToId = replyTo?.id,
            replyToText = replyTo?.message,
            attachment = attachment,
            audioWaveform = audioWaveform,
            status = MessageStatus.SENDING,
            timestamp = timestamp
        )

        // 1. عرض فوري محلي في الواجهة (Instant Optimistic UI كـ WhatsApp و Telegram)
        _messages.value = _messages.value + localMessage
        _isSending.value = true

        scope.launch {
            try {
                var finalMediaUrl = mediaUrl
                val isLocalFile = mediaUrl.isNotBlank() && (
                    mediaUrl.startsWith("/") || mediaUrl.startsWith("file:") || mediaUrl.startsWith("content:")
                )

                // إذا كان الملف محلياً (صورة أو صوت)، نرفع إلى Firebase Storage في الخلفية دون تعطيل الواجهة
                if (isLocalFile && (mediaType == MediaType.IMAGE || mediaType == MediaType.AUDIO || mediaType == MediaType.VIDEO)) {
                    val isOnline = context?.let { NetworkUtils.isNetworkAvailable(it) } ?: true
                    if (isOnline && attachmentManager != null) {
                        try {
                            val mediaUri = if (mediaUrl.startsWith("content:") || mediaUrl.startsWith("file:")) {
                                Uri.parse(mediaUrl)
                            } else {
                                Uri.fromFile(File(mediaUrl))
                            }
                            val uploadType = when (mediaType) {
                                MediaType.IMAGE -> "image"
                                MediaType.AUDIO -> "audio"
                                MediaType.VIDEO -> "video"
                                else -> "file"
                            }
                            val uploadResult = attachmentManager?.uploadAttachment(channelId, mediaUri, uploadType)
                            if (uploadResult != null && uploadResult.isSuccess) {
                                finalMediaUrl = uploadResult.getOrNull() ?: mediaUrl
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("ChatMessagesManager", "Media upload failed, saving local copy: ${e.message}")
                        }
                    }
                }

                val result = repository.sendMessage(
                    channelId = channelId,
                    senderId = senderId,
                    senderName = senderName,
                    messageText = text,
                    mediaType = mediaType,
                    mediaUrl = finalMediaUrl,
                    replyToId = replyTo?.id,
                    replyToText = replyTo?.message,
                    attachment = attachment,
                    audioWaveform = audioWaveform,
                    existingMessageId = messageId,
                    existingTimestamp = timestamp
                )

                _isSending.value = false
                when (result) {
                    is AppResult.Success -> {
                        val confirmed = result.data
                        _messages.value = _messages.value.map {
                            if (it.id == localMessage.id) confirmed else it
                        }
                        onSuccess(confirmed)
                    }
                    is AppResult.Error -> {
                        _messages.value = _messages.value.map {
                            if (it.id == localMessage.id) it.copy(status = MessageStatus.PENDING) else it
                        }
                        onError(result.error.messageArabic)
                    }
                }
            } catch (e: Exception) {
                _isSending.value = false
                _messages.value = _messages.value.map {
                    if (it.id == localMessage.id) it.copy(status = MessageStatus.PENDING) else it
                }
                onError(e.message ?: "جاري الحفظ للمزامنة التلقائية")
            }
        }
    }

    fun resendMessage(
        channelId: String,
        currentUserId: String,
        targetMsg: ChatMessage,
        onSuccess: (ChatMessage) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            _messages.value = _messages.value.map {
                if (it.id == targetMsg.id) it.copy(status = MessageStatus.SENDING) else it
            }
            try {
                var finalMediaUrl = targetMsg.mediaUrl
                val isLocalFile = targetMsg.mediaUrl.isNotBlank() && (
                    targetMsg.mediaUrl.startsWith("/") || targetMsg.mediaUrl.startsWith("file:") || targetMsg.mediaUrl.startsWith("content:")
                )

                if (isLocalFile && attachmentManager != null && context != null && NetworkUtils.isNetworkAvailable(context)) {
                    try {
                        val mediaUri = if (targetMsg.mediaUrl.startsWith("content:") || targetMsg.mediaUrl.startsWith("file:")) {
                            Uri.parse(targetMsg.mediaUrl)
                        } else {
                            Uri.fromFile(File(targetMsg.mediaUrl))
                        }
                        val uploadType = when (targetMsg.mediaType) {
                            MediaType.IMAGE -> "image"
                            MediaType.AUDIO -> "audio"
                            MediaType.VIDEO -> "video"
                            else -> "file"
                        }
                        val uploadResult = attachmentManager?.uploadAttachment(channelId, mediaUri, uploadType)
                        if (uploadResult != null && uploadResult.isSuccess) {
                            finalMediaUrl = uploadResult.getOrNull() ?: targetMsg.mediaUrl
                        }
                    } catch (_: Exception) {}
                }

                val result = repository.sendMessage(
                    channelId = channelId,
                    senderId = targetMsg.senderId,
                    senderName = targetMsg.senderName,
                    messageText = targetMsg.message,
                    mediaType = targetMsg.mediaType,
                    mediaUrl = finalMediaUrl,
                    replyToId = targetMsg.replyToId,
                    replyToText = targetMsg.replyToText,
                    attachment = targetMsg.attachment,
                    audioWaveform = targetMsg.audioWaveform,
                    existingMessageId = targetMsg.id,
                    existingTimestamp = targetMsg.timestamp
                )
                when (result) {
                    is AppResult.Success -> {
                        _messages.value = _messages.value.map {
                            if (it.id == targetMsg.id) result.data else it
                        }
                        onSuccess(result.data)
                    }
                    is AppResult.Error -> {
                        _messages.value = _messages.value.map {
                            if (it.id == targetMsg.id) it.copy(status = MessageStatus.PENDING) else it
                        }
                        onError(result.error.messageArabic)
                    }
                }
            } catch (e: Exception) {
                _messages.value = _messages.value.map {
                    if (it.id == targetMsg.id) it.copy(status = MessageStatus.PENDING) else it
                }
                onError(e.message ?: "فشلت إعادة الإرسال")
            }
        }
    }

    fun updateMessageStatus(messageId: String, status: MessageStatus) {
        _messages.value = _messages.value.map {
            if (it.id == messageId) it.copy(status = status) else it
        }
    }
}
