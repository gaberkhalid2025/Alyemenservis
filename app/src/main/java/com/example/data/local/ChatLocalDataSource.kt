package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.models.*
import com.example.utils.SecurityCryptoUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * 🔒 ChatLocalDataSource
 * طبقة التخزين المحلي المشفرة والسريعة للمحادثات (Offline-First Local Engine)
 * - توفر تخزيناً مشفراً للرسائل الحساسة باستخدام SecurityCryptoUtils
 * - تدعم استرجاع وتحديث قنوات المحادثات والرسائل محلياً
 * - تدير طابور الرسائل المعلقة للإرسال في وضع عدم الاتصال (Offline Queue)
 * - تزامن التغييرات وتطلق إشعارات التدفق (Flows) للتحديث الفوري للواجهات
 */
class ChatLocalDataSource(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("YS_Chat_Encrypted_Cache_v2026", Context.MODE_PRIVATE)
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    // Moshi Adapters
    private val channelsListAdapter = moshi.adapter<List<ChatChannel>>(
        Types.newParameterizedType(List::class.java, ChatChannel::class.java)
    )
    private val messagesListAdapter = moshi.adapter<List<ChatMessage>>(
        Types.newParameterizedType(List::class.java, ChatMessage::class.java)
    )
    private val presenceAdapter = moshi.adapter(UserPresence::class.java)

    // Memory Cache for ultra-fast UI rendering
    private val channelsMemoryCache = MutableStateFlow<List<ChatChannel>>(emptyList())
    private val messagesMemoryCache = ConcurrentHashMap<String, MutableStateFlow<List<ChatMessage>>>()
    private val presenceMemoryCache = ConcurrentHashMap<String, MutableStateFlow<UserPresence?>>()

    init {
        // Load initial channels into memory cache
        val initialChannels = getCachedChannelsInternal()
        channelsMemoryCache.value = initialChannels
    }

    companion object {
        private const val KEY_CHANNELS = "KEY_CACHED_CHANNELS"
        private const val KEY_PREFIX_MESSAGES = "KEY_CHANNEL_MSGS_"
        private const val KEY_PREFIX_SYNC_TIME = "KEY_CHANNEL_SYNC_TIME_"
        private const val KEY_OFFLINE_PENDING_MSGS = "KEY_OFFLINE_PENDING_MSGS"
        private const val KEY_PREFIX_PRESENCE = "KEY_PRESENCE_"
        private const val MAX_CACHE_AGE_MILLIS = 30L * 24 * 60 * 60 * 1000 // 30 Days

        @Volatile
        private var INSTANCE: ChatLocalDataSource? = null

        fun getInstance(context: Context): ChatLocalDataSource {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChatLocalDataSource(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // ==========================================
    // 1. CHANNELS LOCAL OPERATIONS
    // ==========================================

    fun observeChannels(): Flow<List<ChatChannel>> = channelsMemoryCache.asStateFlow()

    suspend fun getChannels(): List<ChatChannel> = withContext(ioDispatcher) {
        getCachedChannelsInternal()
    }

    suspend fun getChannelById(channelId: String): ChatChannel? = withContext(ioDispatcher) {
        getCachedChannelsInternal().firstOrNull { it.id == channelId }
    }

    suspend fun saveChannels(channels: List<ChatChannel>) = withContext(ioDispatcher) {
        val json = channelsListAdapter.toJson(channels)
        prefs.edit().putString(KEY_CHANNELS, json).apply()
        channelsMemoryCache.value = channels
    }

    suspend fun saveOrUpdateChannel(channel: ChatChannel) = withContext(ioDispatcher) {
        val current = getCachedChannelsInternal().toMutableList()
        val index = current.indexOfFirst { it.id == channel.id }
        if (index >= 0) {
            current[index] = channel
        } else {
            current.add(0, channel)
        }
        val sorted = current.sortedByDescending { it.lastMessageTime }
        saveChannels(sorted)
    }

    suspend fun deleteChannel(channelId: String) = withContext(ioDispatcher) {
        val current = getCachedChannelsInternal().filter { it.id != channelId }
        saveChannels(current)
        prefs.edit().remove(KEY_PREFIX_MESSAGES + channelId).remove(KEY_PREFIX_SYNC_TIME + channelId).apply()
        messagesMemoryCache.remove(channelId)
    }

    suspend fun clearAllChannels() = withContext(ioDispatcher) {
        prefs.edit().clear().apply()
        channelsMemoryCache.value = emptyList()
        messagesMemoryCache.clear()
        presenceMemoryCache.clear()
    }

    private fun getCachedChannelsInternal(): List<ChatChannel> {
        val raw = prefs.getString(KEY_CHANNELS, null) ?: return emptyList()
        return try {
            channelsListAdapter.fromJson(raw) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==========================================
    // 2. MESSAGES LOCAL OPERATIONS (WITH ENCRYPTION)
    // ==========================================

    fun observeMessages(channelId: String): Flow<List<ChatMessage>> {
        val flow = messagesMemoryCache.getOrPut(channelId) {
            val initial = getCachedMessagesInternal(channelId)
            MutableStateFlow(initial)
        }
        return flow.asStateFlow()
    }

    suspend fun getMessages(channelId: String): List<ChatMessage> = withContext(ioDispatcher) {
        getCachedMessagesInternal(channelId)
    }

    suspend fun saveMessages(channelId: String, messages: List<ChatMessage>) = withContext(ioDispatcher) {
        // Sort and decrypt if needed
        val sorted = messages.sortedBy { it.timestamp }
        val json = messagesListAdapter.toJson(sorted)
        // Store encrypted payload
        val encrypted = SecurityCryptoUtils.encrypt(json)
        prefs.edit().putString(KEY_PREFIX_MESSAGES + channelId, encrypted).apply()

        // Update in-memory stream
        val flow = messagesMemoryCache.getOrPut(channelId) { MutableStateFlow(emptyList()) }
        flow.value = sorted
    }

    suspend fun insertOrUpdateMessage(message: ChatMessage) = withContext(ioDispatcher) {
        val current = getCachedMessagesInternal(message.channelId).toMutableList()
        val index = current.indexOfFirst { it.id == message.id }
        if (index >= 0) {
            current[index] = message
        } else {
            current.add(message)
        }
        val sorted = current.sortedBy { it.timestamp }
        saveMessages(message.channelId, sorted)
    }

    suspend fun updateMessageStatus(channelId: String, messageId: String, status: MessageStatus) = withContext(ioDispatcher) {
        val current = getCachedMessagesInternal(channelId).toMutableList()
        val index = current.indexOfFirst { it.id == messageId }
        if (index >= 0) {
            current[index] = current[index].copy(status = status)
            saveMessages(channelId, current)
        }
    }

    suspend fun deleteMessage(channelId: String, messageId: String) = withContext(ioDispatcher) {
        val current = getCachedMessagesInternal(channelId).filter { it.id != messageId }
        saveMessages(channelId, current)
    }

    suspend fun markMessagesAsRead(channelId: String, currentUserId: String) = withContext(ioDispatcher) {
        val current = getCachedMessagesInternal(channelId).toMutableList()
        var modified = false
        for (i in current.indices) {
            val msg = current[i]
            if (msg.senderId != currentUserId && msg.status != MessageStatus.READ) {
                current[i] = msg.copy(status = MessageStatus.READ)
                modified = true
            }
        }
        if (modified) {
            saveMessages(channelId, current)
        }
    }

    private fun getCachedMessagesInternal(channelId: String): List<ChatMessage> {
        val rawEncrypted = prefs.getString(KEY_PREFIX_MESSAGES + channelId, null) ?: return emptyList()
        return try {
            val decrypted = SecurityCryptoUtils.decrypt(rawEncrypted)
            if (decrypted.isNotBlank() && decrypted != "[]") {
                messagesListAdapter.fromJson(decrypted) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            // Fallback plain parse if not encrypted
            try {
                messagesListAdapter.fromJson(rawEncrypted) ?: emptyList()
            } catch (ex: Exception) {
                emptyList()
            }
        }
    }

    // ==========================================
    // 3. OFFLINE PENDING QUEUE OPERATIONS
    // ==========================================

    suspend fun queuePendingMessage(message: ChatMessage) = withContext(ioDispatcher) {
        val pending = getPendingMessagesInternal().toMutableList()
        val index = pending.indexOfFirst { it.id == message.id }
        if (index >= 0) {
            pending[index] = message
        } else {
            pending.add(message)
        }
        val json = messagesListAdapter.toJson(pending)
        prefs.edit().putString(KEY_OFFLINE_PENDING_MSGS, json).apply()

        // Also add to local channel messages as PENDING
        insertOrUpdateMessage(message.copy(status = MessageStatus.PENDING, syncStatus = SyncStatus.PENDING_UPLOAD))
    }

    suspend fun getPendingMessages(): List<ChatMessage> = withContext(ioDispatcher) {
        getPendingMessagesInternal()
    }

    suspend fun removePendingMessage(messageId: String) = withContext(ioDispatcher) {
        val pending = getPendingMessagesInternal().filter { it.id != messageId }
        val json = messagesListAdapter.toJson(pending)
        prefs.edit().putString(KEY_OFFLINE_PENDING_MSGS, json).apply()
    }

    private fun getPendingMessagesInternal(): List<ChatMessage> {
        val raw = prefs.getString(KEY_OFFLINE_PENDING_MSGS, null) ?: return emptyList()
        return try {
            messagesListAdapter.fromJson(raw) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==========================================
    // 4. PRESENCE LOCAL OPERATIONS
    // ==========================================

    fun observeUserPresence(userId: String): Flow<UserPresence?> {
        val flow = presenceMemoryCache.getOrPut(userId) {
            val cached = getCachedPresenceInternal(userId)
            MutableStateFlow(cached)
        }
        return flow.asStateFlow()
    }

    suspend fun saveUserPresence(presence: UserPresence) = withContext(ioDispatcher) {
        val json = presenceAdapter.toJson(presence)
        prefs.edit().putString(KEY_PREFIX_PRESENCE + presence.userId, json).apply()
        val flow = presenceMemoryCache.getOrPut(presence.userId) { MutableStateFlow(null) }
        flow.value = presence
    }

    private fun getCachedPresenceInternal(userId: String): UserPresence? {
        val raw = prefs.getString(KEY_PREFIX_PRESENCE + userId, null) ?: return null
        return try {
            presenceAdapter.fromJson(raw)
        } catch (e: Exception) {
            null
        }
    }

    // ==========================================
    // 5. DELTA SYNC METADATA & CACHE MANAGEMENT
    // ==========================================

    suspend fun getLastSyncTimestamp(channelId: String): Long = withContext(ioDispatcher) {
        prefs.getLong(KEY_PREFIX_SYNC_TIME + channelId, 0L)
    }

    suspend fun setLastSyncTimestamp(channelId: String, timestamp: Long) = withContext(ioDispatcher) {
        prefs.edit().putLong(KEY_PREFIX_SYNC_TIME + channelId, timestamp).apply()
    }

    /**
     * تنظيف الكاش القديم (أقدم من 30 يوماً) لتوفير الذاكرة والمساحة
     */
    suspend fun pruneStaleCache() = withContext(ioDispatcher) {
        try {
            val now = System.currentTimeMillis()
            val channels = getCachedChannelsInternal()
            val activeChannelIds = channels.map { it.id }.toSet()

            val allKeys = prefs.all.keys
            val editor = prefs.edit()
            var modified = false

            for (key in allKeys) {
                if (key.startsWith(KEY_PREFIX_SYNC_TIME)) {
                    val channelId = key.removePrefix(KEY_PREFIX_SYNC_TIME)
                    val lastSync = prefs.getLong(key, 0L)
                    if (!activeChannelIds.contains(channelId) && (now - lastSync > MAX_CACHE_AGE_MILLIS)) {
                        editor.remove(key)
                        editor.remove(KEY_PREFIX_MESSAGES + channelId)
                        modified = true
                    }
                }
            }

            if (modified) {
                editor.apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
