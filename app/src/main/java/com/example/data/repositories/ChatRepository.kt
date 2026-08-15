package com.example.data.repositories

import androidx.annotation.Keep
import com.example.data.ChatChannelEntity
import com.example.data.ChatMessageEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

@Keep
class ChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val channelsCollection = firestore.collection("chat_channels")
    
    fun getMessagesCollection(channelId: String) = 
        channelsCollection.document(channelId).collection("chat_messages")

    /**
     * Create or retrieve a chat channel between users
     */
    fun createChannel(
        channel: ChatChannelEntity,
        onResult: (Boolean, String?) -> Unit
    ) {
        val channelId = if (channel.id.isBlank()) {
            "chat_p_${channel.targetId}_u_${channel.customerId}"
        } else {
            channel.id
        }

        val finalChannel = channel.copy(
            id = channelId,
            timestamp = System.currentTimeMillis(),
            lastMessageTime = System.currentTimeMillis()
        )

        channelsCollection.document(channelId)
            .set(finalChannel)
            .addOnSuccessListener {
                onResult(true, channelId)
            }
            .addOnFailureListener { e ->
                onResult(false, e.localizedMessage)
            }
    }

    /**
     * Send a new message inside a specific chat channel
     */
    fun sendMessage(
        channelId: String,
        message: ChatMessageEntity,
        onResult: (Boolean, String?) -> Unit
    ) {
        val msgId = if (message.id.isBlank()) UUID.randomUUID().toString() else message.id
        val finalMessage = message.copy(
            id = msgId,
            timestamp = System.currentTimeMillis(),
            status = "SENT",
            statusTime = System.currentTimeMillis()
        )

        val batch = firestore.batch()
        
        // 1. Add message to the subcollection
        val msgDoc = getMessagesCollection(channelId).document(msgId)
        batch.set(msgDoc, finalMessage)

        // 2. Update channel metadata
        val channelDoc = channelsCollection.document(channelId)
        batch.update(
            channelDoc,
            mapOf(
                "lastMessage" to message.message,
                "lastMessageTime" to System.currentTimeMillis(),
                "timestamp" to System.currentTimeMillis()
            )
        )

        batch.commit()
            .addOnSuccessListener {
                onResult(true, msgId)
            }
            .addOnFailureListener { e ->
                onResult(false, e.localizedMessage)
            }
    }

    /**
     * Listen in real-time to messages in a specific channel with a cost-saving limit (e.g. last 50 messages)
     */
    fun listenToMessages(channelId: String, limit: Int = 50): Flow<List<ChatMessageEntity>> = callbackFlow {
        if (channelId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val query = getMessagesCollection(channelId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChatMessageEntity::class.java)?.copy(id = doc.id)
                }.sortedBy { it.timestamp } // Ascending order for chat history UI flow
                
                trySend(list)
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Listen to active chat channels for a specific user ID
     */
    fun getMyChannelsFlow(userId: String, isProvider: Boolean = false): Flow<List<ChatChannelEntity>> = callbackFlow {
        if (userId.isBlank() || userId == "guest") {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val query = if (isProvider) {
            channelsCollection.whereEqualTo("targetId", userId)
        } else {
            channelsCollection.whereEqualTo("customerId", userId)
        }

        val listener = query
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatChannelEntity::class.java)?.copy(id = doc.id)
                    }
                    trySend(list)
                }
            }

        awaitClose { listener.remove() }
    }
}
