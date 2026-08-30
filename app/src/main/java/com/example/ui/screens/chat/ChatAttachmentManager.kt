package com.example.ui.screens.chat

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatAttachmentManager {
    private val storageRef = FirebaseStorage.getInstance().reference

    suspend fun uploadAttachment(channelId: String, uri: Uri, type: String): String {
        val fileName = "${UUID.randomUUID()}_${System.currentTimeMillis()}"
        val ref = storageRef.child("chats/$channelId/$type/$fileName")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
}
