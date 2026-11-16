package com.example.data.repositories

import android.util.Log
import com.example.data.local.ChatLocalDataSource
import com.example.data.models.UserPresence
import com.example.utils.AppError
import com.example.utils.AppResult
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * 🟢 ChatPresenceDelegate
 * مسؤول عن حالة الاتصال (Online/LastSeen)، مؤشر الكتابة، وحظر المستخدمين
 */
internal class ChatPresenceDelegate(
    private val channelsCollection: CollectionReference,
    private val presenceCollection: CollectionReference,
    private val realtimeDbProvider: () -> FirebaseDatabase?,
    private val localProvider: () -> ChatLocalDataSource?
) {

    suspend fun setTyping(channelId: String, userId: String, isTyping: Boolean): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (channelId.isBlank() || userId.isBlank()) return@withContext AppResult.Success(Unit)
        try {
            try {
                realtimeDbProvider()?.getReference("typing/$channelId/$userId")?.let { ref ->
                    ref.setValue(isTyping)
                    if (isTyping) {
                        ref.onDisconnect().setValue(false)
                    }
                }
            } catch (e: Exception) {
                channelsCollection.document(channelId).update("isTyping.$userId", isTyping)
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.NetworkError(e))
        }
    }

    suspend fun toggleBlockUser(channelId: String, userIdToBlock: String, isBlocked: Boolean): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (channelId.isBlank() || userIdToBlock.isBlank()) return@withContext AppResult.Success(Unit)
        val local = localProvider()
        try {
            channelsCollection.document(channelId).update("blockedUsers.$userIdToBlock", isBlocked).await()
            val cached = local?.getChannelById(channelId)
            if (cached != null) {
                val updatedBlocked = cached.blockedUsers.toMutableMap().apply { put(userIdToBlock, isBlocked) }
                local?.saveOrUpdateChannel(cached.copy(blockedUsers = updatedBlocked))
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.NetworkError(e))
        }
    }

    suspend fun setUserPresence(userId: String, isOnline: Boolean): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext AppResult.Success(Unit)
        val local = localProvider()
        try {
            val now = System.currentTimeMillis()
            val presence = UserPresence(
                userId = userId,
                isOnline = isOnline,
                lastSeen = now
            )
            local?.saveUserPresence(presence)

            // 1. Primary: Realtime Database with onDisconnect support (Zero Spark Plan Firestore write quota)
            try {
                realtimeDbProvider()?.getReference("presence/$userId")?.let { ref ->
                    val data = mapOf(
                        "userId" to userId,
                        "isOnline" to isOnline,
                        "lastSeen" to now
                    )
                    ref.setValue(data)
                    if (isOnline) {
                        ref.child("isOnline").onDisconnect().setValue(false)
                        ref.child("lastSeen").onDisconnect().setValue(com.google.firebase.database.ServerValue.TIMESTAMP)
                    }
                }
            } catch (e: Exception) {
                Log.w("ChatRepository", "Realtime DB presence error: ${e.message}")
            }

            // 2. Sync to Firestore presence document
            try {
                presenceCollection.document(userId).set(presence, SetOptions.merge())
            } catch (_: Exception) {}

            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.NetworkError(e))
        }
    }

    fun getUserPresence(userId: String): Flow<UserPresence?> = callbackFlow {
        if (userId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val local = localProvider()
        val realtimeDb = realtimeDbProvider()

        // Local cache emission first
        val localJob = local?.observeUserPresence(userId)?.let { localFlow ->
            launch(Dispatchers.IO) {
                localFlow.collect { cached ->
                    trySend(cached)
                }
            }
        }

        var rtdbListener: com.google.firebase.database.ValueEventListener? = null
        val rtdbRef = try { realtimeDb?.getReference("presence/$userId") } catch (_: Exception) { null }
        var firestoreListener: ListenerRegistration? = null

        if (rtdbRef != null) {
            rtdbListener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false
                    val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                    val presence = UserPresence(userId = userId, isOnline = isOnline, lastSeen = lastSeen)
                    launch(Dispatchers.IO) {
                        local?.saveUserPresence(presence)
                    }
                    trySend(presence)
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    if (firestoreListener == null) {
                        firestoreListener = presenceCollection.document(userId).addSnapshotListener { snap, _ ->
                            val p = snap?.toObject(UserPresence::class.java)
                            if (p != null) {
                                launch(Dispatchers.IO) { local?.saveUserPresence(p) }
                            }
                            trySend(p)
                        }
                    }
                }
            }
            rtdbRef.addValueEventListener(rtdbListener)
        } else {
            firestoreListener = presenceCollection.document(userId).addSnapshotListener { snap, _ ->
                val p = snap?.toObject(UserPresence::class.java)
                if (p != null) {
                    launch(Dispatchers.IO) { local?.saveUserPresence(p) }
                }
                trySend(p)
            }
        }

        awaitClose {
            try {
                localJob?.cancel()
                if (rtdbRef != null && rtdbListener != null) {
                    rtdbRef.removeEventListener(rtdbListener)
                }
                firestoreListener?.remove()
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    fun getTypingStatus(channelId: String, userId: String): Flow<Boolean> = callbackFlow {
        if (channelId.isBlank() || userId.isBlank()) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val realtimeDb = realtimeDbProvider()

        var rtdbListener: com.google.firebase.database.ValueEventListener? = null
        val rtdbRef = try { realtimeDb?.getReference("typing/$channelId/$userId") } catch (_: Exception) { null }
        var firestoreListener: ListenerRegistration? = null

        if (rtdbRef != null) {
            rtdbListener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val isTyping = snapshot.getValue(Boolean::class.java) ?: false
                    trySend(isTyping)
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    if (firestoreListener == null) {
                        firestoreListener = channelsCollection.document(channelId).addSnapshotListener { snap, _ ->
                            val isTypingMap = snap?.get("isTyping") as? Map<*, *>
                            val isTyping = (isTypingMap?.get(userId) as? Boolean) ?: false
                            trySend(isTyping)
                        }
                    }
                }
            }
            rtdbRef.addValueEventListener(rtdbListener)
        } else {
            firestoreListener = channelsCollection.document(channelId).addSnapshotListener { snap, _ ->
                val isTypingMap = snap?.get("isTyping") as? Map<*, *>
                val isTyping = (isTypingMap?.get(userId) as? Boolean) ?: false
                trySend(isTyping)
            }
        }

        awaitClose {
            try {
                if (rtdbRef != null && rtdbListener != null) {
                    rtdbRef.removeEventListener(rtdbListener)
                }
                firestoreListener?.remove()
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)
}
