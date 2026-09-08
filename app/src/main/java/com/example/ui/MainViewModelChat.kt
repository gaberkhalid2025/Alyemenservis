package com.example.ui

import com.example.data.models.ChatChannel
import com.example.ui.*
import com.example.data.models.ChannelType

fun MainViewModel.openUnifiedSupportChat() {
    this.openSupportChat()
}

fun MainViewModel.replyToChannelMessage(channelId: String, text: String, senderId: String, senderName: String) {
    this.replyToChatChannel(channelId, text, senderId, senderName)
}

fun MainViewModel.deleteChatChannelById(channelId: String) {
    this.deleteChatChannel(channelId)
}

fun MainViewModel.blockChatChannelById(channelId: String, isBlocked: Boolean) {
    this.blockChatChannel(channelId, isBlocked)
}
