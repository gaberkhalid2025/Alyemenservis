package com.example.ui.screens.chat.managers

import com.example.data.models.ChatAttachment
import com.example.data.models.ChatMessage
import com.example.data.models.MediaType
import com.example.data.models.MessageStatus
import com.example.data.repositories.ChatRepository
import com.example.utils.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatMessagesManager(
    private val repository: ChatRepository,
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
        onSuccess: (ChatMessage) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            _isSending.value = true
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
                    attachment = attachment
                )
                _isSending.value = false
                when (result) {
                    is AppResult.Success -> onSuccess(result.data)
                    is AppResult.Error -> onError(result.error.messageArabic)
                }
            } catch (e: Exception) {
                _isSending.value = false
                onError(e.message ?: "حدث خطأ أثناء الإرسال")
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
