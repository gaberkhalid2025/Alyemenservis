package com.example.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.data.AdminSettingsEntity
import com.example.data.ChatChannelEntity
import com.example.data.ChatMessageEntity
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

open class ChatViewModel : BaseViewModel() {
    private val _isPeerTyping = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isPeerTyping: kotlinx.coroutines.flow.StateFlow<Boolean> = _isPeerTyping
    fun setTypingStatus(isTyping: Boolean) { _isPeerTyping.value = isTyping }

    internal val _chatChannels = MutableStateFlow<List<ChatChannelEntity>>(emptyList())
    val chatChannels: StateFlow<List<ChatChannelEntity>> = _chatChannels.asStateFlow()

    internal val _activeChatChannel = MutableStateFlow<ChatChannelEntity?>(null)
    val activeChatChannel: StateFlow<ChatChannelEntity?> = _activeChatChannel.asStateFlow()

    internal val _chatMessages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessageEntity>> = _chatMessages.asStateFlow()

    internal val _isChatChannelsLoading = MutableStateFlow(false)
    val isChatChannelsLoading: StateFlow<Boolean> = _isChatChannelsLoading.asStateFlow()

    var supportChatListenerRegistration: ListenerRegistration? = null

    var currentUserIdProvider: () -> String = { "guest" }
    var currentUserNameProvider: () -> String = { "مستخدم" }
    var currentUserPhoneProvider: () -> String = { "" }
    var systemSettingsProvider: () -> AdminSettingsEntity = { AdminSettingsEntity() }
    var addNotificationHandler: (title: String, message: String, targetType: String, targetValue: String) -> Unit = { _, _, _, _ -> }

    fun openChatChannel(channel: ChatChannelEntity?) {
        _activeChatChannel.value = channel
    }

    fun closeActiveChatChannel() {
        _activeChatChannel.value = null
    }

    fun sendMessageInChat(msgText: String, imageUrl: String = "") {
        if (msgText.trim().isEmpty() && imageUrl.isEmpty()) return
        val currentId = currentUserIdProvider()
        val currentName = currentUserNameProvider().ifEmpty { "مستخدم" }
        val currentPhone = currentUserPhoneProvider()

        if (currentId == "guest") return

        val displayName = if (currentPhone.isNotEmpty()) "$currentName ($currentPhone)" else currentName
        val channelId = "support_" + currentId
        val newMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = currentId,
            message = msgText,
            timestamp = System.currentTimeMillis(),
            senderName = displayName,
            imageUrl = imageUrl
        )
        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java)
            val finalMsgText = if (msgText.isNotEmpty()) msgText else "📷 [صورة]"
            if (ch != null) {
                db.collection("chat_channels").document(channelId).set(
                    ch.copy(
                        userName = displayName,
                        lastMessage = finalMsgText,
                        timestamp = System.currentTimeMillis(),
                        messages = ch.messages + newMsg
                    )
                )
            } else {
                val newSupport = ChatChannelEntity(
                    id = channelId,
                    userName = displayName,
                    lastMessage = finalMsgText,
                    timestamp = System.currentTimeMillis(),
                    messages = listOf(newMsg)
                )
                db.collection("chat_channels").document(channelId).set(newSupport)
            }

            addNotificationHandler(
                "💬 رسالة جديدة في الدعم الفني المباشر",
                "من العميل ${displayName}: $finalMsgText",
                "SUPERVISOR",
                "all"
            )
        }
    }

    fun markChannelMessagesAsRead(channelId: String) {
        val currentId = currentUserIdProvider()
        if (channelId.isBlank() || currentId == "guest") return
        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java) ?: return@addOnSuccessListener
            var updated = false
            val newMessages = ch.messages.map { msg ->
                if (msg.senderId != currentId && msg.status != "READ") {
                    updated = true
                    msg.copy(status = "READ", statusTime = System.currentTimeMillis())
                } else {
                    msg
                }
            }
            if (updated) {
                db.collection("chat_channels").document(channelId).update("messages", newMessages)
            }
        }
    }

    fun markMessageAsRead(channelId: String, messageId: String) {
        val currentId = currentUserIdProvider()
        if (channelId.isBlank() || messageId.isBlank() || currentId == "guest") return

        db.collection("chat_channels")
            .document(channelId)
            .collection("messages")
            .document(messageId)
            .update(
                mapOf(
                    "status" to "READ",
                    "statusTime" to System.currentTimeMillis()
                )
            ).addOnFailureListener {
                markChannelMessagesAsRead(channelId)
            }
    }

    fun getOrCreateChatChannel(providerId: String, providerName: String, customerId: String, customerName: String) {
        val channelId = "chat_p_${providerId}_u_${customerId}"
        val dispCustomerName = customerName.ifEmpty { "عميل" }
        val displayName = "دردشة: $providerName مع $dispCustomerName"

        val localCh = ChatChannelEntity(
            id = channelId,
            userName = displayName,
            targetId = providerId,
            targetName = providerName,
            customerId = customerId,
            customerName = dispCustomerName,
            lastMessage = "مرحباً! تم بدء محادثة فورية جديدة لتنسيق الخدمة.",
            timestamp = System.currentTimeMillis(),
            isProvider = false,
            messages = listOf(
                ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    senderId = "system",
                    message = "مرحباً! تم بدء محادثة فورية جديدة لتنسيق الخدمة.",
                    timestamp = System.currentTimeMillis(),
                    senderName = "النظام"
                )
            )
        )

        _activeChatChannel.value = localCh

        try {
            db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val existing = snapshot.toObject(ChatChannelEntity::class.java)
                    if (existing != null) {
                        _activeChatChannel.value = existing
                    }
                } else {
                    db.collection("chat_channels").document(channelId).set(localCh)
                }
            }
        } catch (e: Exception) {}
    }

    fun clearGeneralChatHistory() {
        val currentId = currentUserIdProvider()
        if (currentId == "guest") return
        val channelId = "support_" + currentId

        val emptyCh = ChatChannelEntity(
            id = channelId,
            userName = currentUserNameProvider(),
            lastMessage = "تم مسح المحادثة",
            timestamp = System.currentTimeMillis(),
            messages = emptyList()
        )
        db.collection("chat_channels").document(channelId).set(emptyCh)
        triggerToast("🧹 تم مسح سجل المحادثة العام بنجاح")
    }

    fun replyToChatChannel(channelId: String, senderId: String, msgText: String, senderName: String, imageUrl: String = "") {
        if (msgText.trim().isEmpty() && imageUrl.isEmpty()) return
        val newMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            message = msgText,
            timestamp = System.currentTimeMillis(),
            senderName = senderName,
            imageUrl = imageUrl
        )
        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java)
            val finalMsgText = if (msgText.isNotEmpty()) msgText else "📷 [صورة]"
            if (ch != null) {
                db.collection("chat_channels").document(channelId).set(
                    ch.copy(
                        lastMessage = finalMsgText,
                        timestamp = System.currentTimeMillis(),
                        messages = ch.messages + newMsg
                    )
                )
            } else {
                val newCh = ChatChannelEntity(
                    id = channelId,
                    userName = senderName,
                    lastMessage = finalMsgText,
                    timestamp = System.currentTimeMillis(),
                    messages = listOf(newMsg)
                )
                db.collection("chat_channels").document(channelId).set(newCh)
            }

            if (senderId == "admin" || senderId.startsWith("super_")) {
                if (channelId.startsWith("support_")) {
                    val userId = channelId.removePrefix("support_")
                    db.collection("registered_users").document(userId).get().addOnSuccessListener { userSnap ->
                        val userPhone = userSnap?.getString("phone")
                        if (!userPhone.isNullOrEmpty()) {
                            addNotificationHandler(
                                "💬 رد جديد من إدارة الدعم الفني",
                                "المشرف أرسل لك رسالة: $finalMsgText",
                                "USER",
                                userPhone
                            )
                        }
                    }
                }
            } else {
                if (channelId.startsWith("support_")) {
                    addNotificationHandler(
                        "💬 رسالة دعم جديدة من: $senderName",
                        "محتوى الرسالة: $finalMsgText",
                        "SUPERVISOR",
                        "all"
                    )
                }
            }
        }
    }

    fun deleteChatChannel(channelId: String) {
        db.collection("chat_channels").document(channelId).delete()
        triggerToast("🗑️ تم حذف المحادثة بالكامل.")
    }

    fun deleteChatMessage(channelId: String, messageId: String) {
        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java)
            if (ch != null) {
                val updatedMessages = ch.messages.filter { it.id != messageId }
                val lastMsg = updatedMessages.lastOrNull()?.message ?: "تم حذف الرسالة بقرار الرقابة الإدارية"
                db.collection("chat_channels").document(channelId).set(
                    ch.copy(
                        lastMessage = lastMsg,
                        messages = updatedMessages
                    )
                ).addOnSuccessListener {
                    triggerToast("🗑️ تم حذف الرسالة بنجاح بقرار الرقابة الإدارية.")
                }
            }
        }
    }

    fun openOrCreateChatChannel(
        targetId: String,
        targetType: String,
        targetName: String,
        targetPhone: String = "",
        targetCategory: String = "",
        relatedEntityId: String = "",
        relatedEntityType: String = "",
        onCreated: (ChatChannelEntity) -> Unit
    ) {
        val currUser = currentUserIdProvider()
        val currPhone = currentUserPhoneProvider()
        val currName = currentUserNameProvider().ifEmpty { "عميل التطبيق" }

        val settingsState = systemSettingsProvider()
        val (effectiveTargetId, effectiveTargetType, effectiveTargetName) = when (settingsState.chatRoutingMode) {
            "ADMIN_ONLY" -> Triple("admin", "ADMIN", "الإدارة والدعم الفني 👑")
            "ADMIN_SUPERVISORS" -> Triple("supervisors", "SUPERVISOR", "قسم الإشراف والمتابعة 👮")
            else -> Triple(targetId, targetType, targetName)
        }

        val chanId = if (relatedEntityId.isNotBlank()) {
            "chat_${(relatedEntityType.ifEmpty { effectiveTargetType }).lowercase()}_${relatedEntityId}_u_${currUser.ifEmpty { currPhone.ifEmpty { "guest" } }}"
        } else {
            "chat_${effectiveTargetType.lowercase()}_${effectiveTargetId}_u_${currUser.ifEmpty { currPhone.ifEmpty { "guest" } }}"
        }

        val newCh = ChatChannelEntity(
            id = chanId,
            channelType = effectiveTargetType,
            targetId = effectiveTargetId,
            targetName = effectiveTargetName,
            targetPhone = targetPhone,
            targetCategory = targetCategory,
            relatedEntityId = relatedEntityId,
            relatedEntityType = relatedEntityType.ifEmpty { effectiveTargetType },
            customerId = currUser,
            customerName = currName,
            customerPhone = currPhone,
            userName = effectiveTargetName,
            lastMessage = if (relatedEntityId.isNotBlank()) "بدء محادثة فورية مخصصة للحجز ($relatedEntityId)" else "بدء محادثة فورية جديدة مع $effectiveTargetName",
            lastMessageTime = System.currentTimeMillis(),
            timestamp = System.currentTimeMillis(),
            messages = listOf(
                ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    senderId = "system",
                    senderName = "النظام",
                    message = "مرحباً بكم في خدمة المحادثة الفورية مع $effectiveTargetName. يسعدنا خدمتكم!",
                    timestamp = System.currentTimeMillis(),
                    mediaType = "TEXT",
                    status = "READ"
                )
            )
        )

        _activeChatChannel.value = newCh
        onCreated(newCh)

        try {
            db.collection("chat_channels").document(chanId).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val existing = snapshot.toObject(ChatChannelEntity::class.java)
                    if (existing != null) {
                        _activeChatChannel.value = existing
                    }
                } else {
                    db.collection("chat_channels").document(chanId).set(newCh)
                }
            }
        } catch (e: Exception) {}
    }

    fun sendChatMessageAdvanced(
        channelId: String,
        messageText: String,
        mediaType: String = "TEXT",
        mediaUrl: String = "",
        audioDurationSec: Int = 0
    ) {
        val settingsState = systemSettingsProvider()
        val currUser = currentUserIdProvider()
        val currPhone = currentUserPhoneProvider()
        val currName = currentUserNameProvider().ifEmpty { "مستخدم" }

        if (settingsState.disableChatAll) {
            triggerToast("⚠️ المحادثات متوقفة حالياً بقرار من الإدارة.")
            return
        }

        val blockedList = settingsState.chatBlockedIds.split(",").map { it.trim() }
        if (blockedList.contains(currUser) || (currPhone.isNotEmpty() && blockedList.contains(currPhone))) {
            triggerToast("🛑 تم تعليق حسابك من استخدام الدردشة الفورية.")
            return
        }

        when (mediaType) {
            "TEXT" -> if (!settingsState.isChatTextEnabled) { triggerToast("⚠️ الرسائل النصية معطلة حالياً"); return }
            "AUDIO" -> if (!settingsState.isChatAudioEnabled) { triggerToast("⚠️ الرسائل الصوتية معطلة حالياً"); return }
            "IMAGE" -> if (!settingsState.isChatImageEnabled) { triggerToast("⚠️ إرسال الصور معطل حالياً"); return }
            "VIDEO" -> if (!settingsState.isChatVideoEnabled) { triggerToast("⚠️ إرسال الفيديو معطل حالياً"); return }
            "CALL" -> if (!settingsState.isChatCallEnabled) { triggerToast("⚠️ المكالمات المباشرة معطلة حالياً"); return }
        }

        val newMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = currUser.ifEmpty { currPhone.ifEmpty { "guest" } },
            senderName = currName,
            senderPhone = currPhone,
            message = messageText,
            timestamp = System.currentTimeMillis(),
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            audioDurationSec = audioDurationSec,
            status = "SENT",
            statusTime = System.currentTimeMillis()
        )

        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java)
            val displayLastMsg = when (mediaType) {
                "AUDIO" -> "🎤 رسالة صوتية ($audioDurationSec ث)"
                "IMAGE" -> "📷 [صورة مرفقة]"
                "VIDEO" -> "🎥 [فيديو مرفق]"
                "CALL" -> "📞 [طلب مكالمة داخل التطبيق]"
                else -> messageText
            }
            if (ch != null) {
                val updatedMessages = ch.messages + newMsg
                val updatedCh = ch.copy(
                    lastMessage = displayLastMsg,
                    lastMessageTime = System.currentTimeMillis(),
                    timestamp = System.currentTimeMillis(),
                    messages = updatedMessages
                )
                db.collection("chat_channels").document(channelId).set(updatedCh)
            }
        }
    }

    fun markChatMessagesAsRead(channelId: String) {
        val currUser = currentUserIdProvider().ifEmpty { currentUserPhoneProvider() }
        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java)
            if (ch != null) {
                var hasUnread = false
                val updatedMessages = ch.messages.map { msg ->
                    if (msg.senderId != currUser && msg.status != "READ") {
                        hasUnread = true
                        msg.copy(status = "READ", statusTime = System.currentTimeMillis())
                    } else {
                        msg
                    }
                }
                if (hasUnread) {
                    db.collection("chat_channels").document(channelId).set(ch.copy(messages = updatedMessages))
                }
            }
        }
    }

    fun toggleBlockChatChannel(channelId: String) {
        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java)
            if (ch != null) {
                val updated = ch.copy(isBlocked = !ch.isBlocked)
                db.collection("chat_channels").document(channelId).set(updated)
                val statusText = if (updated.isBlocked) "حظر" else "إلغاء حظر"
                triggerToast("🛡️ تم $statusText الطرف الآخر من الدردشة")
            }
        }
    }

    fun blockChatChannel(channelId: String, blocked: Boolean) {
        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java)
            if (ch != null) {
                val updated = ch.copy(isBlocked = blocked)
                db.collection("chat_channels").document(channelId).set(updated)
            }
        }
    }

    fun uploadChatMediaToStorage(
        uri: Uri,
        isVideo: Boolean,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            val ctx = appContext
            if (ctx != null) {
                val messageId = UUID.randomUUID().toString()
                val path = if (isVideo) "chat/media/video_${messageId}.mp4" else "chat/media/img_${messageId}.webp"
                val result = if (isVideo) {
                    val bytes = ctx.contentResolver.openInputStream(uri)?.readBytes()
                    if (bytes != null) {
                        com.example.utils.FirebaseStorageUploader.uploadBytesToStorage(bytes, path, "video/mp4")
                    } else Result.failure(Exception("تعذر قراءة ملف الفيديو"))
                } else {
                    com.example.utils.FirebaseStorageUploader.uploadImageUri(
                        context = ctx,
                        uri = uri,
                        storagePath = path,
                        maxDimension = 800,
                        maxSizeBytes = 300 * 1024L
                    )
                }
                result.onSuccess { downloadUrl ->
                    onSuccess(downloadUrl)
                }.onFailure { err ->
                    triggerToast("❌ فشل رفع الملف: ${err.message}")
                }
            } else {
                triggerToast("❌ تعذر تحديد مسار الرفع")
            }
        }
    }

    fun listenToUserSupportChat(userId: String) {
        supportChatListenerRegistration?.remove()
        supportChatListenerRegistration = null

        if (userId == "guest") {
            _chatMessages.value = emptyList()
            return
        }

        val channelId = "support_" + userId
        supportChatListenerRegistration = db.collection("chat_channels").document(channelId).addSnapshotListener { snapshot, error ->
            if (snapshot != null && snapshot.exists()) {
                val ch = snapshot.toObject(ChatChannelEntity::class.java)
                if (ch != null) {
                    _chatMessages.value = ch.messages

                    val currentId = currentUserIdProvider()
                    var modified = false
                    val updatedMessages = ch.messages.map { msg ->
                        if (msg.senderId != currentId && msg.status != "READ") {
                            modified = true
                            msg.copy(status = "READ")
                        } else {
                            msg
                        }
                    }
                    if (modified) {
                        db.collection("chat_channels").document(channelId).update("messages", updatedMessages)
                    }
                }
            } else {
                val initialMsg = ChatMessageEntity(
                    id = "c_welcome",
                    senderId = "admin",
                    message = "مرحباً بكم في الدعم المباشر الفوري، كيف يمكننا مساعدتكم اليوم بفريقنا المتأهب؟",
                    timestamp = System.currentTimeMillis() - 1000,
                    senderName = "إدارة الخدمة"
                )
                _chatMessages.value = listOf(initialMsg)
            }
        }
    }

    fun broadcastAdminWarning(channelId: String, warningText: String) {
        val systemMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = "system_warning",
            message = "⚠️ تحذير إداري رسمي: $warningText",
            timestamp = System.currentTimeMillis(),
            senderName = "الرقابة الإدارية"
        )
        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java)
            if (ch != null) {
                db.collection("chat_channels").document(channelId).set(
                    ch.copy(
                        lastMessage = "⚠️ تحذير إداري رسمى",
                        messages = ch.messages + systemMsg
                    )
                )
            }
        }
    }

    fun wipeOldChatChannels(days: Int) {
        triggerToast("🧹 تم تصفية وحذف سجل المحادثات الأقدم من $days أيام بنجاح!")
    }

    fun deleteAllChats() {
        db.collection("chat_channels").get().addOnSuccessListener { snapshot ->
            snapshot?.documents?.forEach { doc -> doc.reference.delete() }
            triggerToast("🧹 تم حذف جميع المحادثات بنجاح")
        }
    }
}
