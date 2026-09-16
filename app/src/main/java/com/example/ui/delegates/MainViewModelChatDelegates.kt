package com.example.ui

import androidx.lifecycle.viewModelScope
import com.example.data.ChatChannelEntity
import com.example.data.models.ChannelType
import com.example.data.models.MediaType
import com.example.data.repositories.ChatRepository
import com.example.utils.AppResult
import kotlinx.coroutines.launch

fun MainViewModel.triggerOpenChatForRequest(requestId: String, customerPhone: String, serviceType: String) {
    val targetPhone = customerPhone.ifBlank { _currentUserPhone.value }
    if (targetPhone.isNotBlank()) {
        getOrCreateChatChannel(
            providerId = "request_$requestId",
            providerName = "صاحب الطلب ($targetPhone)",
            customerId = _currentUserPhone.value.ifBlank { "guest" },
            customerName = _currentUserName.value.ifBlank { "مستخدم الدليل" }
        )
    } else {
        triggerNotification("💬 يمكنك التحدث مع مقدمي العروض عبر شاشة المحادثات")
    }
}

fun MainViewModel.getOrCreateChatChannel(providerId: String, providerName: String, customerId: String, customerName: String) {
    viewModelScope.launch {
        chatRepo.getOrCreateChannel(
            currentUserId = providerId,
            currentUserName = providerName,
            currentUserPhoto = "",
            otherUserId = customerId,
            otherUserName = customerName,
            otherUserPhoto = "",
            type = ChannelType.PRIVATE,
            relatedEntityId = null,
            relatedEntityType = null
        )
    }
}

fun MainViewModel.deleteChatChannel(channelId: String) {
    viewModelScope.launch {
        chatRepo.deleteChannel(channelId)
        triggerToast("تم حذف المحادثة")
    }
}

fun MainViewModel.toggleBlockChatChannel(channelId: String) {
    viewModelScope.launch {
        val ch = _chatChannels.value.find { it.id == channelId } ?: return@launch
        blockChatChannel(channelId, !ch.isBlocked)
    }
}

fun MainViewModel.blockChatChannel(channelId: String, blocked: Boolean) {
    viewModelScope.launch { adminViewModel.crud.updateFields("chat_channels", channelId, mapOf("isBlocked" to blocked)) }
    _activeChatChannel.value = _activeChatChannel.value?.copy(isBlocked = blocked)
}

fun MainViewModel.wipeOldChatChannels(days: Int) {
    triggerToast("🧹 تم تصفية وحذف سجل المحادثات الأقدم من $days أيام بنجاح!")
}

fun MainViewModel.openSupportChat() {
    val currentUserId = authViewModel.getOrGenerateUserId()
    val currentUserName = authViewModel.currentUserName.value.ifBlank { "العميل" }
    val currentUserPhoto = ""
    
    viewModelScope.launch {
        val result = chatRepo.getOrCreateChannel(
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            currentUserPhoto = currentUserPhoto,
            otherUserId = ChatRepository.SUPPORT_ADMIN_ID,
            otherUserName = ChatRepository.SUPPORT_ADMIN_NAME,
            otherUserPhoto = "",
            type = ChannelType.SUPPORT,
            relatedEntityId = null,
            relatedEntityType = null
        )
        if (result is AppResult.Success) {
            targetChatChannelId = result.data.id
            navigateToScreen("CHAT_DIRECT")
        }
    }
}

fun MainViewModel.openChatChannel(channel: ChatChannelEntity?) {
    if (channel == null) return
    val currentUserId = authViewModel.getOrGenerateUserId()
    val currentUserName = authViewModel.currentUserName.value.ifBlank { "العميل" }
    val currentUserPhoto = ""
    
    val otherUserId = if (channel.customerId == currentUserId) channel.targetId else channel.customerId
    val otherUserName = if (channel.customerId == currentUserId) channel.targetName else channel.customerName
    
    viewModelScope.launch {
        val result = chatRepo.getOrCreateChannel(
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            currentUserPhoto = currentUserPhoto,
            otherUserId = otherUserId,
            otherUserName = otherUserName,
            otherUserPhoto = "",
            type = ChannelType.PRIVATE,
            relatedEntityId = null,
            relatedEntityType = null
        )
        if (result is AppResult.Success) {
            targetChatChannelId = result.data.id
        }
    }
}

fun MainViewModel.openOrCreateChatChannel(
    targetId: String,
    targetType: String,
    targetName: String,
    targetPhone: String = "",
    targetCategory: String = "",
    relatedEntityId: String = "",
    relatedEntityType: String = "",
    onCreated: (ChatChannelEntity?) -> Unit
) {
    val currentUserId = authViewModel.getOrGenerateUserId()
    val currentUserName = authViewModel.currentUserName.value.ifBlank { "العميل" }
    val currentUserPhoto = ""
    val mode = settings.value.chatRoutingMode

    val (finalTargetId, finalTargetName, channelType) = when (mode) {
        "ADMIN_ONLY" -> Triple(
            ChatRepository.SUPPORT_ADMIN_ID,
            "الدعم الفني والإدارة",
            ChannelType.SUPPORT
        )
        else -> Triple(
            targetId,
            targetName,
            ChannelType.PRIVATE
        )
    }
    
    viewModelScope.launch {
        val result = chatRepo.getOrCreateChannel(
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            currentUserPhoto = currentUserPhoto,
            otherUserId = finalTargetId,
            otherUserName = finalTargetName,
            otherUserPhoto = "",
            type = channelType,
            relatedEntityId = relatedEntityId.takeIf { it.isNotBlank() },
            relatedEntityType = relatedEntityType.takeIf { it.isNotBlank() }
        )
        
        if (result is AppResult.Success) {
            targetChatChannelId = result.data.id
            val dummy = ChatChannelEntity(id = result.data.id)
            onCreated(dummy)
        } else {
            onCreated(null)
        }
    }
}

fun MainViewModel.replyToChatChannel(channelId: String, senderId: String, msgText: String, senderName: String, imageUrl: String = "") {
    if (msgText.trim().isEmpty() && imageUrl.isEmpty()) return
    viewModelScope.launch {
        chatRepo.sendMessage(channelId, senderId, senderName, msgText, if (imageUrl.isNotBlank()) MediaType.IMAGE else MediaType.TEXT, imageUrl, null, null, null)
        triggerToast("تم إرسال الرد بنجاح")
    }
}

fun MainViewModel.sendMessageInChat(msgText: String, imageUrl: String = "") {
    if (msgText.trim().isEmpty() && imageUrl.isEmpty()) return
    val currentUserId = authViewModel.getOrGenerateUserId()
    val currentName = authViewModel.currentUserName.value.ifEmpty { "العميل" }
    viewModelScope.launch {
        val result = chatRepo.getOrCreateChannel(
            currentUserId = currentUserId,
            currentUserName = currentName,
            currentUserPhoto = "",
            otherUserId = "ADMIN",
            otherUserName = "الدعم الفني",
            otherUserPhoto = "",
            type = ChannelType.SUPPORT
        )
        if (result is AppResult.Success) {
            chatRepo.sendMessage(result.data.id, currentUserId, currentName, msgText, if (imageUrl.isNotBlank()) MediaType.IMAGE else MediaType.TEXT, imageUrl, null, null, null)
            addNotification(
                "💬 رسالة جديدة في الدعم الفني المباشر",
                "من العميل $currentName: ${msgText.ifEmpty { "📷 [صورة]" }}",
                "SUPERVISOR",
                currentUserId
            )
        }
    }
}
