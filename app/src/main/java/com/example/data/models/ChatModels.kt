package com.example.data.models

import androidx.annotation.Keep
import java.io.Serializable

/**
 * 🏷️ نوع قناة المحادثة (فردية، دعم فني، مجموعة، متجر، فني)
 */
@Keep
enum class ChannelType {
    PRIVATE,
    SUPPORT,
    GROUP,
    PROVIDER,
    STORE
}

/**
 * 📎 أنواع الوسائط المدعومة في الرسائل
 */
@Keep
enum class MediaType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    LOCATION,
    CALL
}

/**
 * 🚦 حالة إرسال واستلام الرسالة
 */
@Keep
enum class MessageStatus {
    PENDING,
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

/**
 * 🔄 حالة المزامنة المحلية للبيانات
 */
@Keep
enum class SyncStatus {
    SYNCED,
    PENDING_UPLOAD,
    PENDING_DELETE,
    CONFLICT
}

/**
 * 📎 تفاصيل المرفقات للرسائل
 */
@Keep
data class ChatAttachment(
    val url: String = "",
    val localUri: String = "",
    val fileName: String = "",
    val fileSize: Long = 0L,
    val mimeType: String = "",
    val durationSeconds: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val thumbnailUrl: String = ""
) : Serializable

/**
 * 👍 تفاعل المستخدمين مع الرسالة (Reactions)
 */
@Keep
data class ChatReaction(
    val userId: String = "",
    val userName: String = "",
    val emoji: String = "👍",
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

/**
 * 💬 نموذج القناة / المحادثة الكامل (ChatChannel)
 */
@Keep
data class ChatChannel(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantPhotos: Map<String, String> = emptyMap(),
    val participantRoles: Map<String, String> = emptyMap(), // "ADMIN", "MEMBER"
    val type: ChannelType = ChannelType.PRIVATE,
    val title: String = "",
    val description: String = "",
    val groupAvatarUrl: String = "",
    val relatedEntityId: String? = null,
    val relatedEntityType: String? = null, // BOOKING, URGENT_REQUEST, SUPPORT, STORE
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val lastMessageSenderId: String = "",
    val lastMessageStatus: MessageStatus = MessageStatus.SENT,
    val unreadCount: Map<String, Int> = emptyMap(),
    val isBlocked: Boolean = false,
    val blockedUsers: Map<String, Boolean> = emptyMap(),
    val isTyping: Map<String, Boolean> = emptyMap(),
    val isPinned: Map<String, Boolean> = emptyMap(),
    val isMuted: Map<String, Boolean> = emptyMap(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val channelType: String = "PROVIDER",
    val targetId: String = "",
    val targetName: String = "",
    val targetPhone: String = "",
    val targetCategory: String = "",
    val userName: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerId: String = "",
    val isProvider: Boolean = false,
    val timestamp: Long = 0L,
    val unreadCountUser: Int = 0,
    val unreadCountTarget: Int = 0,
    val providerId: String = "",
    val providerName: String = "",
    val providerPhoto: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val clientPhoto: String = "",
    val messages: List<ChatMessage> = emptyList()
) : Serializable {

    fun toCanonicalChannel(): ChatChannel = this

    /**
     * الحصول على اسم الطرف الآخر في المحادثات الثنائية
     */
    fun getOtherParticipantName(currentUserId: String): String {
        val otherId = participants.firstOrNull { it != currentUserId } ?: return title.ifBlank { "مستخدم" }
        return participantNames[otherId] ?: title.ifBlank { "مستخدم" }
    }

    /**
     * الحصول على صورة الطرف الآخر في المحادثات الثنائية
     */
    fun getOtherParticipantPhoto(currentUserId: String): String {
        val otherId = participants.firstOrNull { it != currentUserId } ?: return groupAvatarUrl
        return participantPhotos[otherId] ?: groupAvatarUrl
    }

    /**
     * الحصول على عدد الرسائل غير المقروءة للمستخدم الحالي
     */
    fun getUnreadFor(userId: String): Int {
        return unreadCount[userId] ?: 0
    }

    /**
     * فحص ما إذا كان المستخدم محظوراً في القناة
     */
    fun isUserBlocked(userId: String): Boolean {
        return isBlocked || blockedUsers[userId] == true
    }
}

/**
 * ✉️ نموذج الرسالة الشامل (ChatMessage)
 */
@Keep
data class ChatMessage(
    val id: String = "",
    val channelId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhoto: String = "",
    val senderPhone: String = "",
    val recipientId: String = "",
    val message: String = "",
    val mediaType: MediaType = MediaType.TEXT,
    val mediaUrl: String = "",
    val imageUrl: String = "",
    val audioDurationSec: Int = 0,
    val attachment: ChatAttachment? = null,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val status: MessageStatus = MessageStatus.SENT,
    val statusTime: Long = 0L,
    val reactions: Map<String, String> = emptyMap(), // Map of userId to emoji
    val isEncrypted: Boolean = false,
    val isEdited: Boolean = false,
    val editTimestamp: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedBy: String = "",
    val deletedForUsers: List<String> = emptyList(),
    val fileName: String = "",
    val fileSize: Long = 0L,
    val fileType: String = "",
    val forwardedFrom: String = "",
    val readAt: Long = 0L,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
) : Serializable {

    fun toCanonicalMessage(chId: String = channelId): ChatMessage = this

    /**
     * فحص ما إذا كانت الرسالة مرسلة من المستخدم الحالي
     */
    fun isSentBy(userId: String): Boolean = senderId == userId

    /**
     * التحقق مما إذا تم حذف الرسالة للمستخدم المحدد
     */
    fun isHiddenFor(userId: String): Boolean = isDeleted || deletedForUsers.contains(userId)
}

/**
 * 🟢 حالة تواجد المستخدم (UserPresence)
 */
@Keep
data class UserPresence(
    val userId: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val currentChannelId: String? = null,
    val deviceStatus: String = "ACTIVE"
) : Serializable

/**
 * 🔄 حالة المزامنة التفاضلية (Delta Sync State)
 */
@Keep
data class ChatSyncState(
    val channelId: String = "",
    val lastSyncedTimestamp: Long = 0L,
    val lastMessageCount: Int = 0,
    val isSyncing: Boolean = false
) : Serializable
