package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.models.*
import com.example.utils.ChatCryptoManager
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
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences("YS_Chat_Encrypted_Cache_v2026", Context.MODE_PRIVATE)
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val chatDao: ChatDao by lazy { AppDatabase.getInstance(appContext).chatDao() }

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
        migrateLegacyChatData()
        val initialChannels = kotlinx.coroutines.runBlocking(ioDispatcher) { getCachedChannelsInternal() }
        channelsMemoryCache.value = initialChannels
    }

    /**
     * سكربت ترحيل البيانات القديمة وتوحيدها مع Room والكاش المشفر
     */
    private fun migrateLegacyChatData() {
        try {
            val legacyPrefs = appContext.getSharedPreferences("YS_Chat_Cache_Legacy", Context.MODE_PRIVATE)
            val legacyKeys = legacyPrefs.all
            if (legacyKeys.isNotEmpty()) {
                val editor = prefs.edit()
                for ((key, value) in legacyKeys) {
                    if (value is String && !prefs.contains(key)) {
                        editor.putString(key, value)
                    }
                }
                editor.apply()
                legacyPrefs.edit().clear().apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        try {
            val strListAdapter = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
            val mapAdapter = moshi.adapter<Map<String, Int>>(Types.newParameterizedType(Map::class.java, String::class.java, Int::class.javaObjectType))
            val roomChannels = channels.map { ch ->
                val encLastMsg = if (ch.lastMessage.isNotBlank()) SecurityCryptoUtils.encrypt(ch.lastMessage) else ""
                ChatChannelRoomEntity(
                    id = ch.id,
                    title = ch.title,
                    type = ch.type.name,
                    participantsJson = strListAdapter.toJson(ch.participants),
                    lastMessage = if (encLastMsg.isNotEmpty()) encLastMsg else ch.lastMessage,
                    lastMessageTime = ch.lastMessageTime,
                    lastMessageSenderId = ch.lastMessageSenderId,
                    unreadCountJson = mapAdapter.toJson(ch.unreadCount),
                    syncStatus = ch.syncStatus.name,
                    updatedAt = ch.updatedAt
                )
            }
            chatDao.insertChannels(roomChannels)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        try {
            chatDao.deleteChannel(channelId)
            chatDao.deleteMessagesByChannel(channelId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val current = getCachedChannelsInternal().filter { it.id != channelId }
        channelsMemoryCache.value = current
        prefs.edit().remove(KEY_PREFIX_SYNC_TIME + channelId).apply()
        messagesMemoryCache.remove(channelId)
    }

    suspend fun clearAllChannels() = withContext(ioDispatcher) {
        prefs.edit().clear().apply()
        channelsMemoryCache.value = emptyList()
        messagesMemoryCache.clear()
        presenceMemoryCache.clear()
    }

    private suspend fun getCachedChannelsInternal(): List<ChatChannel> {
        return try {
            val roomChannels = chatDao.getAllChannelsList()
            if (roomChannels.isNotEmpty()) {
                val strListAdapter = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
                val mapAdapter = moshi.adapter<Map<String, Int>>(Types.newParameterizedType(Map::class.java, String::class.java, Int::class.javaObjectType))
                roomChannels.map { entity ->
                    val decLastMsg = if (entity.lastMessage.isNotBlank()) {
                        try { SecurityCryptoUtils.decrypt(entity.lastMessage) } catch (_: Exception) { entity.lastMessage }
                    } else ""
                    ChatChannel(
                        id = entity.id,
                        title = entity.title,
                        type = try { ChannelType.valueOf(entity.type) } catch (_: Exception) { ChannelType.PRIVATE },
                        participants = try { strListAdapter.fromJson(entity.participantsJson) ?: emptyList() } catch (_: Exception) { emptyList() },
                        lastMessage = decLastMsg,
                        lastMessageTime = entity.lastMessageTime,
                        lastMessageSenderId = entity.lastMessageSenderId,
                        unreadCount = try { mapAdapter.fromJson(entity.unreadCountJson) ?: emptyMap() } catch (_: Exception) { emptyMap() },
                        syncStatus = try { SyncStatus.valueOf(entity.syncStatus) } catch (_: Exception) { SyncStatus.SYNCED },
                        updatedAt = entity.updatedAt
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==========================================
    // 2. MESSAGES LOCAL OPERATIONS (WITH ENCRYPTION)
    // ==========================================

    fun observeMessages(channelId: String): Flow<List<ChatMessage>> {
        val flow = messagesMemoryCache.getOrPut(channelId) {
            val initial = kotlinx.coroutines.runBlocking(ioDispatcher) { getCachedMessagesInternal(channelId) }
            MutableStateFlow(initial)
        }
        return flow.asStateFlow()
    }

    suspend fun getMessages(channelId: String): List<ChatMessage> = withContext(ioDispatcher) {
        getCachedMessagesInternal(channelId)
    }

    suspend fun saveMessages(channelId: String, messages: List<ChatMessage>) = withContext(ioDispatcher) {
        val sorted = messages.sortedBy { it.timestamp }
        try {
            val roomMessages = sorted.map { msg ->
                val encText = if (msg.message.isNotBlank()) SecurityCryptoUtils.encrypt(msg.message) else ""
                ChatMessageRoomEntity(
                    id = msg.id,
                    channelId = msg.channelId.ifBlank { channelId },
                    senderId = msg.senderId,
                    senderName = msg.senderName,
                    senderPhoto = msg.senderPhoto,
                    message = if (encText.isNotEmpty()) encText else msg.message,
                    mediaType = msg.mediaType.name,
                    mediaUrl = msg.mediaUrl,
                    status = msg.status.name,
                    isEncrypted = true,
                    timestamp = msg.timestamp,
                    syncStatus = msg.syncStatus.name
                )
            }
            chatDao.deleteMessagesByChannel(channelId)
            chatDao.insertMessages(roomMessages)
        } catch (e: Exception) {
            e.printStackTrace()
        }

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
        try {
            chatDao.deleteMessage(messageId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    private suspend fun getCachedMessagesInternal(channelId: String): List<ChatMessage> {
        return try {
            val roomList = chatDao.getMessagesList(channelId)
            if (roomList.isNotEmpty()) {
                roomList.map { entity ->
                    val decText = if (entity.isEncrypted && entity.message.isNotBlank()) {
                        try { SecurityCryptoUtils.decrypt(entity.message) } catch (_: Exception) { entity.message }
                    } else {
                        entity.message
                    }
                    ChatMessage(
                        id = entity.id,
                        channelId = entity.channelId,
                        senderId = entity.senderId,
                        senderName = entity.senderName,
                        senderPhoto = entity.senderPhoto,
                        message = decText,
                        mediaType = try { MediaType.valueOf(entity.mediaType) } catch (_: Exception) { MediaType.TEXT },
                        mediaUrl = entity.mediaUrl,
                        status = try { MessageStatus.valueOf(entity.status) } catch (_: Exception) { MessageStatus.SENT },
                        isEncrypted = entity.isEncrypted,
                        timestamp = entity.timestamp,
                        syncStatus = try { SyncStatus.valueOf(entity.syncStatus) } catch (_: Exception) { SyncStatus.SYNCED }
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==========================================
    // 3. OFFLINE PENDING QUEUE OPERATIONS
    // ==========================================

    suspend fun queuePendingMessage(message: ChatMessage) = withContext(ioDispatcher) {
        val pendingMsg = message.copy(status = MessageStatus.PENDING, syncStatus = SyncStatus.PENDING_UPLOAD)
        try {
            val encText = if (pendingMsg.message.isNotBlank()) SecurityCryptoUtils.encrypt(pendingMsg.message) else ""
            val entity = ChatMessageRoomEntity(
                id = pendingMsg.id,
                channelId = pendingMsg.channelId,
                senderId = pendingMsg.senderId,
                senderName = pendingMsg.senderName,
                senderPhoto = pendingMsg.senderPhoto,
                message = if (encText.isNotEmpty()) encText else pendingMsg.message,
                mediaType = pendingMsg.mediaType.name,
                mediaUrl = pendingMsg.mediaUrl,
                status = MessageStatus.PENDING.name,
                isEncrypted = true,
                timestamp = pendingMsg.timestamp,
                syncStatus = SyncStatus.PENDING_UPLOAD.name
            )
            chatDao.insertMessage(entity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        insertOrUpdateMessage(pendingMsg)
    }

    suspend fun getPendingMessages(): List<ChatMessage> = withContext(ioDispatcher) {
        getPendingMessagesInternal()
    }

    suspend fun removePendingMessage(messageId: String) = withContext(ioDispatcher) {
        try {
            chatDao.updateMessageSyncStatus(messageId, SyncStatus.SYNCED.name)
            chatDao.updateMessageStatus(messageId, MessageStatus.SENT.name)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun getPendingMessagesInternal(): List<ChatMessage> {
        return try {
            val roomPending = chatDao.getPendingMessagesList(SyncStatus.PENDING_UPLOAD.name, MessageStatus.PENDING.name)
            roomPending.map { entity ->
                val decText = if (entity.isEncrypted && entity.message.isNotBlank()) {
                    try { SecurityCryptoUtils.decrypt(entity.message) } catch (_: Exception) { entity.message }
                } else {
                    entity.message
                }
                ChatMessage(
                    id = entity.id,
                    channelId = entity.channelId,
                    senderId = entity.senderId,
                    senderName = entity.senderName,
                    senderPhoto = entity.senderPhoto,
                    message = decText,
                    mediaType = try { MediaType.valueOf(entity.mediaType) } catch (_: Exception) { MediaType.TEXT },
                    mediaUrl = entity.mediaUrl,
                    status = try { MessageStatus.valueOf(entity.status) } catch (_: Exception) { MessageStatus.PENDING },
                    isEncrypted = entity.isEncrypted,
                    timestamp = entity.timestamp,
                    syncStatus = try { SyncStatus.valueOf(entity.syncStatus) } catch (_: Exception) { SyncStatus.PENDING_UPLOAD }
                )
            }
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
