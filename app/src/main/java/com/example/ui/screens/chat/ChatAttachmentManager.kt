package com.example.ui.screens.chat

import android.content.Context
import android.net.Uri
import com.example.utils.ChatValidationUtils
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * 📤 ChatAttachmentManager
 * إدارة ورفع وسائط المحادثات إلى Firebase Storage مع الفحص والضغط المسبق وتطبيق قيود الحصص
 */
class ChatAttachmentManager(private val context: Context) {
    private val storageRef = FirebaseStorage.getInstance().reference

    suspend fun uploadAttachment(
        channelId: String,
        uri: Uri,
        type: String
    ): Result<String> {
        val validation = ChatValidationUtils.validateFile(uri, context)
        if (!validation.isValid) {
            return Result.failure(Exception(validation.message))
        }

        if (!ChatValidationUtils.canUploadToday(context)) {
            return Result.failure(Exception("⚠️ تجاوزت الحد اليومي المسموح به لرفع الملفات (10 وسائط يومياً)."))
        }

        return try {
            val compressedData = if (type == "image") {
                ChatValidationUtils.compressImage(context, uri)
            } else {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()
            }

            if (compressedData.isEmpty()) {
                return Result.failure(Exception("تعذر قراءة بيانات الملف المرفق."))
            }

            val fileName = "${UUID.randomUUID()}_${System.currentTimeMillis()}"
            val ref = storageRef.child("chats/$channelId/$type/$fileName")

            ref.putBytes(compressedData).await()
            val url = ref.downloadUrl.await().toString()
            ChatValidationUtils.recordUploadToday(context)
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
