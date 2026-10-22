package com.example.ui.screens.chat.managers

import android.util.Log
import com.example.data.models.UserPresence
import com.example.data.repositories.IChatRepository
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * 🟢 ChatPresenceManager
 * [إصلاح نهائي] إدارة حالة التواجد الحية عبر Firebase Realtime Database
 * مع استخدام Debouncing لتقليل استهلاك الكوتة المجانية لـ Firebase
 */
class ChatPresenceManager(
    private val repository: IChatRepository,
    private val scope: CoroutineScope
) {
    private val _otherUserPresence = MutableStateFlow<UserPresence?>(null)
    val otherUserPresence: StateFlow<UserPresence?> = _otherUserPresence.asStateFlow()

    private var presenceJob: Job? = null
    private var debouncePresenceJob: Job? = null
    private val presenceFlowCache = ConcurrentHashMap<String, Flow<UserPresence?>>()

    private val realtimeDb: FirebaseDatabase? by lazy {
        try {
            FirebaseDatabase.getInstance()
        } catch (e: Exception) {
            Log.w("ChatPresenceManager", "RealtimeDatabase unavailable: ${e.message}")
            null
        }
    }

    // [إصلاح نهائي] تتبع حالة اتصال الطرف الآخر
    fun listenToPresence(otherUserId: String) {
        presenceJob?.cancel()
        if (otherUserId.isBlank()) return

        presenceJob = scope.launch {
            getPresenceFlow(otherUserId).collect { presence ->
                _otherUserPresence.value = presence
            }
        }
    }

    // [تحسين مدمج] جلب تدفق حالة التواجد مع الكاش المحلي و distinctUntilChanged لتجنب التكرار
    fun getPresenceFlow(userId: String): Flow<UserPresence?> {
        if (userId.isBlank()) return flowOf(null)
        return presenceFlowCache.getOrPut(userId) {
            repository.getUserPresence(userId)
                .distinctUntilChanged()
        }
    }

    // [إصلاح نهائي] تحديث حالة اتصال المستخدم مع Debouncing (600ms) لتوفير كوتة Firebase المجانية
    fun setUserPresence(userId: String, isOnline: Boolean) {
        if (userId.isBlank()) return
        debouncePresenceJob?.cancel()
        debouncePresenceJob = scope.launch(Dispatchers.IO) {
            delay(600L) // Debounce network writes
            try {
                // 1. تحديث فوري عبر Realtime Database (صفر تكلفة لقراءة وكتابة المستندات في Firestore)
                realtimeDb?.getReference("presence/$userId")?.let { ref ->
                    val now = System.currentTimeMillis()
                    ref.setValue(
                        mapOf(
                            "userId" to userId,
                            "isOnline" to isOnline,
                            "lastSeen" to now
                        )
                    )
                    if (isOnline) {
                        ref.child("isOnline").onDisconnect().setValue(false)
                        ref.child("lastSeen").onDisconnect().setValue(com.google.firebase.database.ServerValue.TIMESTAMP)
                    }
                }
            } catch (e: Exception) {
                Log.w("ChatPresenceManager", "RTDB presence update fallback: ${e.message}")
            }

            // 2. مزامنة المستودع المحلي والسحابي كإجراء احتياطي
            repository.setUserPresence(userId, isOnline)
        }
    }

    fun clear() {
        presenceJob?.cancel()
        debouncePresenceJob?.cancel()
        _otherUserPresence.value = null
    }
}
