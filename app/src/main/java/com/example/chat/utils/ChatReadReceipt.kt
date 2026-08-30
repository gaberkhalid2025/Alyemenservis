package com.example.chat.utils

import com.example.chat.domain.MessageStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 👁️‍🗨️ ChatReadReceipt
 * Handles Optimistic UI updates for Sent/Delivered/Read statuses.
 * Enables instant UI changes before awaiting server confirmation.
 */
class ChatReadReceiptManager {
    // Maps messageId to its current optimistic status
    private val _receiptStatuses = MutableStateFlow<Map<String, MessageStatus>>(emptyMap())
    val receiptStatuses: StateFlow<Map<String, MessageStatus>> = _receiptStatuses.asStateFlow()

    fun markAsSentOptimistically(messageId: String) {
        updateStatus(messageId, MessageStatus.SENT)
    }

    fun markAsDeliveredOptimistically(messageId: String) {
        updateStatus(messageId, MessageStatus.DELIVERED)
    }

    fun markAsReadOptimistically(messageId: String) {
        updateStatus(messageId, MessageStatus.READ)
    }

    private fun updateStatus(messageId: String, status: MessageStatus) {
        _receiptStatuses.update { currentMap ->
            val newMap = currentMap.toMutableMap()
            newMap[messageId] = status
            newMap
        }
    }
}
