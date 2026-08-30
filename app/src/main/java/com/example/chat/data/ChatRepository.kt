package com.example.chat.data

import com.example.chat.domain.ChatMessage
import com.example.chat.domain.ChatRoom
import com.example.chat.domain.MessageStatus
import kotlinx.coroutines.flow.Flow

/**
 * 🗄️ ChatRepository
 * Handles Paging3, Local Caching (Room), and Remote Sync (Firebase).
 */
interface ChatRepository {
    // Pagination: Load messages in batches (e.g., 30) for infinite scrolling
    fun getMessages(roomId: String, limit: Int): Flow<List<ChatMessage>>
    
    // Trigger to load older messages when user scrolls to the top
    suspend fun loadMoreMessages(roomId: String, lastMessageId: String, limit: Int)
    
    // Core Actions
    suspend fun sendMessage(message: ChatMessage): Result<Unit>
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)
    
    // Rooms Management
    fun getActiveRooms(userId: String): Flow<List<ChatRoom>>
    suspend fun markRoomAsRead(roomId: String, userId: String)
}
