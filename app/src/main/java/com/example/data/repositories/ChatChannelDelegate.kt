package com.example.data.repositories

import android.content.Context
import android.util.Log
import com.example.data.local.ChatLocalDataSource
import com.example.data.models.ChannelType
import com.example.data.models.ChatChannel
import com.example.utils.AnalyticsEventsHelper
import com.example.utils.AppError
import com.example.utils.AppResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * 📡 ChatChannelDelegate
 * مسؤول عن إنشاء القنوات وجلبها ومراقبتها لحظياً وحذفها (نظام Offline-First)
 */
internal class ChatChannelDelegate(
    private val context: Context?,
    private val firestore: FirebaseFirestore,
    private val channelsCollection: CollectionReference,
    private val localProvider: () -> ChatLocalDataSource?
) {

    suspend fun getOrCreateChannel(
        currentUserId: String,
        currentUserName: String,
        currentUserPhoto: String,
        otherUserId: String,
        otherUserName: String,
        otherUserPhoto: String,
        type: ChannelType,
        relatedEntityId: String?,
        relatedEntityType: String?
    ): AppResult<ChatChannel> = withContext(Dispatchers.IO) {
        val local = localProvider()
        try {
            val cleanCurrent = currentUserId.trim()
            val cleanOther = if (type == ChannelType.SUPPORT) ChatRepository.SUPPORT_ADMIN_ID else otherUserId.trim()
            val finalOtherName = if (type == ChannelType.SUPPORT) ChatRepository.SUPPORT_ADMIN_NAME else otherUserName

            if (cleanCurrent.isBlank()) {
                return@withContext AppResult.Error(AppError.ValidationError("currentUserId", "معرف المستخدم الحالي فارغ"))
            }

            val sortedParticipants = listOf(cleanCurrent, cleanOther).filter { it.isNotBlank() }.sorted()

            var customChannelId: String? = null

            val authUid = try {
                FirebaseAuth.getInstance().currentUser?.uid?.takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            }

            if (type == ChannelType.SUPPORT) {
                try {
                    val supportQuery = if (authUid != null) {
                        channelsCollection.whereArrayContains("participantUids", authUid).limit(25)
                    } else {
                        channelsCollection.whereArrayContains("participants", cleanCurrent).limit(25)
                    }
                    val existingSupportQuery = supportQuery.get().await()

                    val existingSupportDoc = existingSupportQuery.documents.find { doc ->
                        val docType = doc.getString("type") ?: ""
                        val docTitle = doc.getString("title") ?: ""
                        val docId = doc.id
                        docType.equals("SUPPORT", ignoreCase = true) ||
                            docTitle == ChatRepository.SUPPORT_ADMIN_NAME ||
                            docId.startsWith("support_") ||
                            docId == "channel_support_${cleanCurrent}"
                    }

                    if (existingSupportDoc != null) {
                        customChannelId = existingSupportDoc.id
                    }
                } catch (e: Exception) {
                    Log.w("ChatRepository", "Error searching existing support channel: ${e.message}")
                }
            }

            val finalChannelId = customChannelId ?: when {
                type == ChannelType.SUPPORT -> "channel_support_${cleanCurrent}"
                sortedParticipants.size == 2 -> "channel_${sortedParticipants[0]}_${sortedParticipants[1]}"
                relatedEntityId != null -> "channel_${type.name.lowercase()}_${relatedEntityId.trim()}_${cleanCurrent}"
                else -> channelsCollection.document().id
            }

            val docRef = channelsCollection.document(finalChannelId)

            val snapshot = docRef.get().await()

            val channelToReturn = if (snapshot.exists()) {
                val existing = snapshot.toObject(ChatChannel::class.java)?.copy(id = snapshot.id) ?: ChatChannel(id = finalChannelId)
                val updatedNames = existing.participantNames.toMutableMap().apply {
                    put(cleanCurrent, currentUserName)
                    if (cleanOther.isNotBlank() && finalOtherName.isNotBlank()) put(cleanOther, finalOtherName)
                }
                val updatedPhotos = existing.participantPhotos.toMutableMap().apply {
                    put(cleanCurrent, currentUserPhoto)
                    if (cleanOther.isNotBlank() && otherUserPhoto.isNotBlank()) put(cleanOther, otherUserPhoto)
                }
                val mergedUids = if (authUid != null && !existing.participantUids.contains(authUid)) {
                    (existing.participantUids + authUid).distinct()
                } else {
                    existing.participantUids
                }

                val finalChannel = existing.copy(
                    participantNames = updatedNames,
                    participantPhotos = updatedPhotos,
                    type = if (type == ChannelType.SUPPORT) ChannelType.SUPPORT else existing.type,
                    title = if (type == ChannelType.SUPPORT) "الدعم الفني" else existing.title,
                    participantUids = mergedUids
                )

                val needsUidSync = authUid != null && !existing.participantUids.contains(authUid)
                if (updatedNames != existing.participantNames || updatedPhotos != existing.participantPhotos || needsUidSync) {
                    val patch = mutableMapOf<String, Any>(
                        "participantNames" to updatedNames,
                        "participantPhotos" to updatedPhotos,
                        "type" to finalChannel.type.name,
                        "title" to finalChannel.title
                    )
                    if (authUid != null) {
                        patch["participantUids"] = FieldValue.arrayUnion(authUid)
                    }
                    docRef.update(patch).await()
                }
                local?.saveOrUpdateChannel(finalChannel)
                finalChannel
            } else {
                val initialUids = listOfNotNull(authUid).distinct()
                val newChannel = ChatChannel(
                    id = finalChannelId,
                    participants = listOf(cleanCurrent, cleanOther).filter { it.isNotBlank() },
                    participantNames = mutableMapOf(cleanCurrent to currentUserName).apply {
                        if (cleanOther.isNotBlank() && finalOtherName.isNotBlank()) put(cleanOther, finalOtherName)
                    },
                    participantPhotos = mutableMapOf(cleanCurrent to currentUserPhoto).apply {
                        if (cleanOther.isNotBlank() && otherUserPhoto.isNotBlank()) put(cleanOther, otherUserPhoto)
                    },
                    type = type,
                    title = if (type == ChannelType.SUPPORT) "الدعم الفني" else finalOtherName.ifBlank { "" },
                    relatedEntityId = relatedEntityId,
                    relatedEntityType = relatedEntityType,
                    lastMessageTime = System.currentTimeMillis(),
                    participantUids = initialUids
                )
                docRef.set(newChannel).await()
                local?.saveOrUpdateChannel(newChannel)
                newChannel
            }
            AnalyticsEventsHelper.logChatOpened(context, channelToReturn.id, otherUserId)
            AppResult.Success(channelToReturn)
        } catch (e: Exception) {
            val cleanCurrent = currentUserId.trim()
            val cleanOther = if (type == ChannelType.SUPPORT) ChatRepository.SUPPORT_ADMIN_ID else otherUserId.trim()
            val sortedParticipants = listOf(cleanCurrent, cleanOther).filter { it.isNotBlank() }.sorted()
            val fallbackChannelId = when {
                type == ChannelType.SUPPORT -> "channel_support_${cleanCurrent}"
                sortedParticipants.size == 2 -> "channel_${sortedParticipants[0]}_${sortedParticipants[1]}"
                relatedEntityId != null -> "channel_${type.name.lowercase()}_${relatedEntityId.trim()}_${cleanCurrent}"
                else -> ""
            }
            val localChannel = if (fallbackChannelId.isNotBlank()) local?.getChannelById(fallbackChannelId) else null
            if (localChannel != null && localChannel.participants.contains(cleanCurrent)) {
                AppResult.Success(localChannel)
            } else {
                AppResult.Error(AppError.NetworkError(e))
            }
        }
    }

    suspend fun getChannelById(channelId: String): AppResult<ChatChannel?> = withContext(Dispatchers.IO) {
        if (channelId.isBlank()) return@withContext AppResult.Success(null)
        val local = localProvider()
        try {
            // Local first
            val cached = local?.getChannelById(channelId)
            if (cached != null) {
                return@withContext AppResult.Success(cached)
            }

            // Remote fallback
            val snapshot = channelsCollection.document(channelId).get().await()
            if (snapshot.exists()) {
                val channel = snapshot.toObject(ChatChannel::class.java)?.copy(id = snapshot.id)
                if (channel != null) {
                    local?.saveOrUpdateChannel(channel)
                }
                AppResult.Success(channel)
            } else {
                AppResult.Success(null)
            }
        } catch (e: Exception) {
            val cached = local?.getChannelById(channelId)
            if (cached != null) AppResult.Success(cached)
            else AppResult.Error(AppError.NetworkError(e))
        }
    }

    fun getUserChannels(userId: String): Flow<List<ChatChannel>> = callbackFlow {
        val local = localProvider()
        val cleanUserId = userId.trim()
        if (cleanUserId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // 1. Emit local cache immediately for instant UI
        local?.getChannels()?.let { cached ->
            if (cached.isNotEmpty()) {
                trySend(cached)
            }
        }

        // 2. Attach Firestore Realtime Listener aligned with Firestore auth UID rules (Limited to 40 active channels to conserve Spark quota)
        val fbUser = try {
            FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        }
        val query = when {
            cleanUserId.equals("ADMIN", ignoreCase = true) ->
                channelsCollection.whereArrayContains("participants", cleanUserId).limit(40)
            fbUser != null && !fbUser.isAnonymous &&
                (cleanUserId.startsWith("prov_") || cleanUserId.startsWith("store_") || cleanUserId.startsWith("prop_") || cleanUserId.all { it.isDigit() }) ->
                channelsCollection.whereArrayContains("participants", cleanUserId).limit(40)
            fbUser != null ->
                channelsCollection.whereArrayContains("participantUids", fbUser.uid).limit(40)
            else ->
                channelsCollection.whereArrayContains("participants", cleanUserId).limit(40)
        }
        val listener: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Firestore channels listener error: ${error.message}")
                    return@addSnapshotListener
                }

                val allRemoteChannels = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatChannel::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.lastMessageTime } ?: emptyList()

                // For ADMIN: see all support channels from all users
                // For Regular User: Deduplicate SUPPORT channels: keep only their own newest one
                val remoteChannels = if (cleanUserId.equals("ADMIN", ignoreCase = true)) {
                    allRemoteChannels
                } else {
                    val supportChannels = allRemoteChannels.filter {
                        it.type == ChannelType.SUPPORT || it.title == "الدعم الفني" || it.id.startsWith("support_")
                    }
                    val otherChannels = allRemoteChannels.filter {
                        it.type != ChannelType.SUPPORT && it.title != "الدعم الفني" && !it.id.startsWith("support_")
                    }
                    val latestSupport = supportChannels.maxByOrNull { it.lastMessageTime }
                    (otherChannels + listOfNotNull(latestSupport)).sortedByDescending { it.lastMessageTime }
                }

                // Save to local cache & emit using callbackFlow scope
                launch(Dispatchers.IO) {
                    local?.saveChannels(remoteChannels)
                }
                trySend(remoteChannels)
            }

        awaitClose {
            try {
                listener.remove()
            } catch (e: Exception) {
                // تجاهل
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun deleteChannel(channelId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (channelId.isBlank()) return@withContext AppResult.Success(Unit)
        val local = localProvider()
        try {
            local?.deleteChannel(channelId)
            val channelDocRef = channelsCollection.document(channelId)
            try {
                // Bulk recursive delete via Cloud Function (avoids reading/deleting messages one-by-one on client)
                com.google.firebase.functions.FirebaseFunctions.getInstance()
                    .getHttpsCallable("deleteChatChannel")
                    .call(mapOf("channelId" to channelId))
                    .await()
            } catch (_: Exception) {
                // Deleting the channel document directly also triggers server-side onChatChannelDeleted (db.recursiveDelete)
                channelDocRef.delete().await()
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "deleteChannel error: ${e.message}")
            AppResult.Error(AppError.NetworkError(e))
        }
    }

    suspend fun deleteAllChannels(channelsList: List<ChatChannel>): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (channelsList.isEmpty()) return@withContext AppResult.Success(Unit)
        val local = localProvider()
        try {
            channelsList.forEach { ch ->
                if (ch.id.isNotBlank()) {
                    local?.deleteChannel(ch.id)
                }
            }
            channelsList.chunked(400).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { ch ->
                    if (ch.id.isNotBlank()) {
                        // Deleting channel document triggers onChatChannelDeleted recursiveDelete on server
                        batch.delete(channelsCollection.document(ch.id))
                    }
                }
                batch.commit().await()
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.NetworkError(e))
        }
    }
}
