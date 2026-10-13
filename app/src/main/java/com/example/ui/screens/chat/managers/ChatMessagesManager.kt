package com.example.ui.screens.chat.managers

import com.example.data.models.ChatAttachment
import com.example.data.models.ChatMessage
import com.example.data.models.MediaType
import com.example.data.models.MessageStatus
import com.example.data.repositories.IChatRepository
import com.example.utils.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatMessagesManager(
    private val repository: IChatRepository,
    private val scope: CoroutineScope
) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    fun updateMessagesList(list: List<ChatMessage>) {
        _messages.value = list
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
        scope.launch {
            _isSending.value = true
            // التحديث الفوري المحلي (Optimistic Update) لإظهار الرسالة دون تأخير
            val tempMessage = ChatMessage(
                id = "temp_${System.currentTimeMillis()}",
                channelId = channelId,
                senderId = senderId,
                senderName = senderName,
                message = text,
                mediaType = mediaType,
                mediaUrl = mediaUrl,
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.SENDING
            )
            _messages.value = _messages.value + tempMessage

            var attempt = 0
            var finalResult: AppResult<ChatMessage>? = null
            while (attempt < 2) {
                attempt++
                try {
                    val result = repository.sendMessage(
                        channelId = channelId,
                        senderId = senderId,
                        senderName = senderName,
                        messageText = text,
                        mediaType = mediaType,
                        mediaUrl = mediaUrl,
                        replyToId = replyTo?.id,
                        replyToText = replyTo?.message,
                        attachment = attachment,
                        audioWaveform = audioWaveform
                    )
                    finalResult = result
                    if (result is AppResult.Success) {
                        break
                    } else if (attempt < 2) {
                        kotlinx.coroutines.delay(400L)
                    }
                } catch (e: Exception) {
                    if (attempt >= 2) {
                        _isSending.value = false
                        // تحديث حالة الرسالة المؤقتة إلى FAILED عند فشل الإرسال النهائي
                        _messages.value = _messages.value.map { if (it.id == tempMessage.id) it.copy(status = MessageStatus.FAILED) else it }
                        onError(e.message ?: "حدث خطأ أثناء الإرسال")
                        return@launch
                    }
                    kotlinx.coroutines.delay(400L)
                }
            }
            _isSending.value = false
            // إزالة الرسالة المؤقتة واستبدالها بالرسالة الناجحة من الخادم
            _messages.value = _messages.value.filter { it.id != tempMessage.id }
            when (finalResult) {
                is AppResult.Success -> onSuccess(finalResult.data)
                is AppResult.Error -> {
                    _messages.value = _messages.value + tempMessage.copy(status = MessageStatus.FAILED)
                    onError(finalResult.error.messageArabic)
                }
                null -> {
                    _messages.value = _messages.value + tempMessage.copy(status = MessageStatus.FAILED)
                    onError("فشل إرسال الرسالة، يرجى الناعرة لإعادة المحاولة")
                }
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
            try {
                val result = repository.sendMessage(
                    channelId = channelId,
                    senderId = targetMsg.senderId,
                    senderName = targetMsg.senderName,
                    messageText = targetMsg.message,
                    mediaType = targetMsg.mediaType,
                    mediaUrl = targetMsg.mediaUrl,
                    replyToId = targetMsg.replyToId,
                    replyToText = targetMsg.replyToText,
                    attachment = targetMsg.attachment
                )
                when (result) {
                    is AppResult.Success -> {
                        repository.deleteMessage(channelId, targetMsg.id, forEveryone = false, currentUserId = currentUserId)
                        onSuccess(result.data)
                    }
                    is AppResult.Error -> onError(result.error.messageArabic)
                }
            } catch (e: Exception) {
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
