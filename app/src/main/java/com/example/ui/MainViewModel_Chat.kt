package com.example.ui

import com.example.data.*
import java.util.UUID

fun MainViewModel.openOrCreateChatChannel(targetId: String, targetName: String) {
    _activeChatChannel.value = ChatChannelEntity(
        id = UUID.randomUUID().toString(),
        targetId = targetId,
        targetName = targetName
    )
}

fun MainViewModel.startVoiceCall(callerName: String, callerRole: String) {
    _activeVoiceCallPair.value = Pair(callerName, callerRole)
}

fun MainViewModel.startVoiceCall(targetId: String) {
    _activeVoiceCallPair.value = Pair(targetId, "مستخدم")
}

fun MainViewModel.endVoiceCall() {
    _activeVoiceCallPair.value = null
}

// ------ Advanced Chat Routing & Purchase Protection System ------

fun MainViewModel.transferChatChannelToProvider(channelId: String, providerId: String, providerName: String) {
    db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
        val ch = snapshot.toObject(ChatChannelEntity::class.java)
        if (ch != null) {
            val newChannelId = "chat_p_${providerId}_u_${ch.customerId.ifEmpty { "user" }}"
            val updatedCh = ch.copy(
                id = newChannelId,
                targetId = providerId,
                targetName = providerName,
                userName = "دردشة: $providerName مع ${ch.customerName.ifEmpty { "عميل" }}"
            )
            db.collection("chat_channels").document(newChannelId).set(updatedCh)
            
            val systemMsg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                senderId = "system",
                message = "🔄 تم تحويل المحادثة وتوجيهها إلى مقدم الخدمة: $providerName للمتابعة المباشرة.",
                timestamp = System.currentTimeMillis(),
                senderName = "النظام"
            )
            db.collection("chat_channels").document(newChannelId).update("messages", updatedCh.messages + systemMsg)
            db.collection("chat_channels").document(channelId).delete()
            
            // Add notification
            addNotification(
                title = "🔄 تحويل محادثة تلقائي",
                message = "تم تحويل المحادثة #${channelId.takeLast(6)} إلى مقدم الخدمة: $providerName",
                targetType = "SUPERVISOR",
                targetValue = "all"
            )
            triggerNotification("🔄 تم تحويل المحادثة بنجاح إلى مقدم الخدمة!")
        }
    }
}

fun MainViewModel.editChatMessageAdmin(channelId: String, messageId: String, newText: String) {
    db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
        if (snapshot.exists()) {
            val ch = snapshot.toObject(ChatChannelEntity::class.java)
            if (ch != null) {
                val updatedMsgs = ch.messages.map { 
                    if (it.id == messageId) it.copy(message = "$newText (تم تعديله إدارياً ✏️)") else it 
                }
                db.collection("chat_channels").document(channelId).update("messages", updatedMsgs)
                triggerNotification("✏️ تم تعديل الرسالة بواسطة الإدارة")
            }
        }
    }
}

fun MainViewModel.freezeWalletOrAccount(ownerPhone: String, isFrozen: Boolean, reason: String) {
    addNotification(
        title = if (isFrozen) "🚫 تم تجميد الحساب والمحفظة" else "🔓 تم إلغاء تجميد الحساب",
        message = "الرقم: $ownerPhone - السبب/المبرر: $reason",
        targetType = "SUPERVISOR",
        targetValue = "all"
    )
    triggerNotification(if (isFrozen) "🚫 تم تجميد حساب ومحفظة $ownerPhone بنجاح لمنع الاحتيال!" else "🔓 تم تنشيط الحساب بنجاح!")
}

fun MainViewModel.updateOrderProtection(orderId: String, isVerified: Boolean, disputeStatus: String, adminNotes: String) {
    val currentOrders = _orders.value
    val updatedOrders = currentOrders.map { ord ->
        if (ord.id == orderId) {
            val updated = ord.copy(
                isVerifiedByAdmin = isVerified,
                disputeStatus = disputeStatus,
                adminNotes = adminNotes,
                status = if (disputeStatus == "FRAUD_DETECTED") "CANCELLED" else ord.status
            )
            try {
                db.collection("orders").document(orderId).set(updated)
            } catch (e: Exception) {}
            updated
        } else ord
    }
    _orders.value = updatedOrders
    
    addNotification(
        title = "🛡️ تحديث حماية المشتريات للطلب #${orderId.takeLast(6)}",
        message = "التحقق: ${if (isVerified) "مؤكد وموثق ✅" else "معلق ⏳"} - النزاع: $disputeStatus - ملاحظات إدارية: $adminNotes",
        targetType = "SUPERVISOR",
        targetValue = "all"
    )
    triggerNotification("🛡️ تم تحديث حالة الحماية وتوثيق الطلب إدارياً!")
}

