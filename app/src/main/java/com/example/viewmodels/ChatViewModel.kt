package com.example.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatChannelEntity
import com.example.data.ChatMessageEntity
import com.example.data.repositories.ChatRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 💬 ChatViewModel
 * إدارة المحادثات الفورية، الرسائل المشفرة، غرف الدردشة، مؤشرات الكتابة، والتحديثات المباشرة
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatRepository()
    private val firestore = FirebaseFirestore.getInstance()

    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    private val _activeChannels = MutableStateFlow<List<ChatChannelEntity>>(emptyList())
    val activeChannels: StateFlow<List<ChatChannelEntity>> = _activeChannels.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _otherUserTyping = MutableStateFlow(false)
    val otherUserTyping: StateFlow<Boolean> = _otherUserTyping.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentChannelId: String? = null

    companion object {
        private const val TAG = "ChatViewModel"
    }

    /**
     * بدء الاستماع لرسائل محادثة معينة
     */
    fun startListeningToChannel(channelId: String) {
        currentChannelId = channelId
        _isLoading.value = true
        viewModelScope.launch {
            repository.listenToMessages(channelId, limit = 100).collect { msgList ->
                _messages.value = msgList
                _isLoading.value = false
            }
        }
    }

    /**
     * تحميل جميع قنوات ومحادثات المستخدم
     */
    fun loadChannelsForUser(userId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                firestore.collection("chat_channels")
                    .addSnapshotListener { snapshot, error ->
                        _isLoading.value = false
                        if (error != null) {
                            Log.w(TAG, "Channels listen failed: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val list = snapshot.documents.mapNotNull { doc ->
                                val channel = doc.toObject(ChatChannelEntity::class.java)
                                channel?.let {
                                    val isMe = it.customerId == userId || it.targetId == userId || it.clientId == userId || it.providerId == userId
                                    if (isMe) it else null
                                }
                            }
                            _activeChannels.value = list
                            _unreadCount.value = list.sumOf { it.unreadCount }
                        }
                    }
            } catch (e: Exception) {
                _isLoading.value = false
                Log.e(TAG, "Error loading channels: ${e.message}")
            }
        }
    }

    /**
     * تحديث نص الإدخال وحالة الكتابة
     */
    fun updateInputText(text: String, channelId: String, userId: String) {
        _inputText.value = text
        _isTyping.value = text.isNotBlank()
    }

    /**
     * إرسال رسالة فورية جديدة
     */
    fun sendMessage(
        channelId: String,
        senderId: String,
        senderName: String,
        recipientId: String,
        msgText: String
    ) {
        if (msgText.isBlank()) return
        val entity = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            senderName = senderName,
            recipientId = recipientId,
            message = msgText.trim(),
            timestamp = System.currentTimeMillis(),
            status = "SENT"
        )

        _inputText.value = ""
        _isTyping.value = false

        repository.sendMessage(channelId, entity) { success, _ ->
            if (!success) {
                Log.e(TAG, "Failed sending message to $channelId")
            }
        }
    }
}
