package com.example.domain.usecases

import com.example.data.models.ChatChannel
import com.example.data.models.ChatMessage
import com.example.data.models.MediaType
import com.example.data.repositories.IChatRepository
import com.example.utils.AppResult
import kotlinx.coroutines.flow.Flow

class GetChannelsUseCase(private val repository: IChatRepository) {
    operator fun invoke(userId: String): Flow<List<ChatChannel>> {
        return repository.getUserChannels(userId)
    }
}

class GetMessagesUseCase(private val repository: IChatRepository) {
    operator fun invoke(channelId: String, currentUserId: String): Flow<List<ChatMessage>> {
        return repository.getChannelMessages(channelId, currentUserId)
    }
}

class SendMessageUseCase(private val repository: IChatRepository) {
    suspend operator fun invoke(
        channelId: String,
        senderId: String,
        senderName: String,
        messageText: String,
        mediaType: MediaType = MediaType.TEXT,
        mediaUrl: String = "",
        replyToId: String? = null,
        replyToText: String? = null
    ): AppResult<ChatMessage> {
        return repository.sendMessage(
            channelId = channelId,
            senderId = senderId,
            senderName = senderName,
            messageText = messageText,
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            replyToId = replyToId,
            replyToText = replyToText,
            attachment = null
        )
    }
}

class DeleteMessageUseCase(private val repository: IChatRepository) {
    suspend operator fun invoke(
        channelId: String,
        messageId: String,
        forEveryone: Boolean,
        currentUserId: String
    ): AppResult<Unit> {
        return repository.deleteMessage(channelId, messageId, forEveryone, currentUserId)
    }
}

class MarkAsReadUseCase(private val repository: IChatRepository) {
    suspend operator fun invoke(channelId: String, currentUserId: String): AppResult<Unit> {
        return repository.markChannelAsRead(channelId, currentUserId)
    }
}
