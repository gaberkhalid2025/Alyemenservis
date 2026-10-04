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
        val mimeType = context.contentResolver.getType(uri)
        android.util.Log.d("ChatAttachmentManager", "uploadAttachment started: channelId=$channelId, type=$type, uri=$uri, mimeType=$mimeType")

        // ✅ التحقق من نوع الملف
        if (mimeType != null) {
            val allowedMimes = listOf(
                "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp",
                "audio/mpeg", "audio/mp3", "audio/aac", "audio/amr", "audio/wav", "audio/ogg", "audio/3gpp",
                "audio/mp4", "audio/m4a", "audio/x-m4a",
                "video/mp4", "video/3gpp"
            )
            if (mimeType !in allowedMimes && !mimeType.startsWith("image/") && !mimeType.startsWith("audio/")) {
                android.util.Log.w("ChatAttachmentManager", "Rejected mimeType: $mimeType")
                return Result.failure(Exception("نوع الملف ($mimeType) غير مدعوم"))
            }
        }

        // منع الملفات التنفيذية
        val fileNameSegment = uri.lastPathSegment ?: ""
        val extension = fileNameSegment.substringAfterLast('.', "").lowercase()
        val blockedExtensions = listOf("exe", "bat", "sh", "apk", "vbs", "cmd", "msi", "dll", "so")
        if (extension in blockedExtensions) {
            android.util.Log.w("ChatAttachmentManager", "Rejected blocked extension: $extension")
            return Result.failure(Exception("نوع الملف غير مسموح به"))
        }

        val validation = ChatValidationUtils.validateFile(uri, context)
        if (!validation.isValid) {
            android.util.Log.w("ChatAttachmentManager", "Validation check failed: ${validation.message}")
            return Result.failure(Exception(validation.message))
        }

        val maxAllowedSize = if (type == "image") 15 * 1024 * 1024L else 20 * 1024 * 1024L
        val fileSize = try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
        } catch (_: Exception) { 0L }

        if (fileSize > maxAllowedSize) {
            val limitMb = if (type == "image") 15 else 20
            android.util.Log.w("ChatAttachmentManager", "File size exceeded max: $fileSize > $maxAllowedSize")
            return Result.failure(Exception("حجم الملف يتجاوز الحد المسموح به ($limitMb ميجابايت)"))
        }

        if (!ChatValidationUtils.canUploadToday(context)) {
            android.util.Log.w("ChatAttachmentManager", "Daily upload limit reached")
            return Result.failure(Exception("⚠️ تجاوزت الحد اليومي المسموح به لرفع الملفات."))
        }

        return try {
            val compressedData = if (type == "image") {
                ChatValidationUtils.compressImage(context, uri)
            } else {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()
            }

            if (compressedData.isEmpty()) {
                android.util.Log.e("ChatAttachmentManager", "Compressed/read data is empty")
                return Result.failure(Exception("تعذر قراءة بيانات الملف المرفق."))
            }

            val fileExt = when {
                type == "image" -> "jpg"
                type == "audio" -> "m4a"
                extension.isNotBlank() -> extension
                else -> "bin"
            }
            val fileName = "${UUID.randomUUID()}_${System.currentTimeMillis()}.$fileExt"
            val ref = storageRef.child("chats/$channelId/$type/$fileName")

            android.util.Log.d("ChatAttachmentManager", "Uploading ${compressedData.size} bytes to ${ref.path}...")
            ref.putBytes(compressedData).await()
            val url = ref.downloadUrl.await().toString()
            ChatValidationUtils.recordUploadToday(context)
            android.util.Log.d("ChatAttachmentManager", "Upload successful: $url")
            Result.success(url)
        } catch (e: Exception) {
            android.util.Log.e("ChatAttachmentManager", "Upload failed with exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
