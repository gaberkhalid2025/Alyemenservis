package com.example.domain.usecases

import com.example.data.repositories.IChatRepository
import com.example.data.models.ChannelType
import com.example.data.models.ChatChannel
import com.example.utils.AppResult
import javax.inject.Inject

class OpenSupportChatUseCase @Inject constructor(
    private val chatRepository: IChatRepository
) {
    suspend operator fun invoke(
        userId: String,
        userName: String,
        userPhone: String
    ): AppResult<ChatChannel> {
        return chatRepository.getOrCreateChannel(
            currentUserId = userId,
            currentUserName = userName,
            currentUserPhoto = "",
            otherUserId = "ADMIN",
            otherUserName = "الدعم الفني للإدارة",
            otherUserPhoto = "",
            type = ChannelType.SUPPORT,
            relatedEntityId = null,
            relatedEntityType = null
        )
    }
}
