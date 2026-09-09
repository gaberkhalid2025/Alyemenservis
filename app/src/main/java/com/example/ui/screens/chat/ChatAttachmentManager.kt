package com.example.ui.screens.chat

import android.content.Context
import com.example.ui.*
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
        // ✅ التحقق من نوع الملف
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType != null) {
            val allowedMimes = listOf(
                "image/jpeg", "image/png", "image/gif", "image/webp",
                "audio/mpeg", "audio/mp3", "audio/aac", "audio/amr", "audio/wav", "audio/ogg", "audio/3gpp",
                "video/mp4", "video/3gpp"
            )
            if (mimeType !in allowedMimes) {
                return Result.failure(Exception("نوع الملف غير مدعوم"))
            }
        }

        // منع الملفات التنفيذية
        val fileNameSegment = uri.lastPathSegment ?: ""
        val extension = fileNameSegment.substringAfterLast('.', "").lowercase()
        val blockedExtensions = listOf("exe", "bat", "sh", "apk", "vbs", "cmd", "msi", "dll", "so")
        if (extension in blockedExtensions) {
            return Result.failure(Exception("نوع الملف غير مسموح به"))
        }

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
