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
 * إدارة ورفع وسائط المحادثات إلى Firebase Storage مع الضغط المسبق الصارم لتوفير استهلاك الباندويث.
 */
class ChatAttachmentManager(private val context: Context) {
    private val storageRef = FirebaseStorage.getInstance().reference

    suspend fun uploadAttachment(
        channelId: String,
        uri: Uri,
        type: String // "image" or "audio"
    ): Result<String> {
        val isImage = type == "image"
        android.util.Log.d("ChatAttachmentManager", "uploadAttachment: channelId=$channelId, type=$type, isImage=$isImage")

        // 1. التحقق من صلاحية الملف والنوع والحصص اليومية (مع تجاوز حجم الصورة الخام)
        val validation = ChatValidationUtils.validateFile(uri, context, isImage)
        if (!validation.isValid) {
            return Result.failure(Exception(validation.message))
        }

        // 2. معالجة البيانات وضغط الصور (أهم خطوة لتوفير Firebase Spark Plan)
        val finalData = if (isImage) {
            // ضغط الصورة فوراً (600x600 جودة 60%)
            ChatValidationUtils.compressImage(context, uri)
        } else {
            // قراءة الملف الصوتي كما هو
            try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()
            } catch (e: Exception) {
                byteArrayOf()
            }
        }

        if (finalData.isEmpty()) {
            return Result.failure(Exception("تعذر معالجة بيانات الملف."))
        }

        // 3. التحقق النهائي من الحجم (الذي سيرفع فعلياً لـ Firebase)
        if (finalData.size > ChatValidationUtils.MAX_FILE_SIZE) {
            val finalSizeMb = String.format(java.util.Locale.US, "%.1f", finalData.size.toDouble() / (1024 * 1024))
            return Result.failure(Exception("حجم الملف النهائي ($finalSizeMb MB) يتجاوز الحد المسموح (2MB)."))
        }

        // 4. تنفيذ عملية الرفع
        return try {
            val extension = if (isImage) "jpg" else {
                val mime = context.contentResolver.getType(uri)
                android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "bin"
            }
            
            val fileName = "${UUID.randomUUID()}_${System.currentTimeMillis()}.$extension"
            val ref = storageRef.child("chats/$channelId/$type/$fileName")

            android.util.Log.d("ChatAttachmentManager", "Uploading ${finalData.size / 1024} KB to Firebase...")
            ref.putBytes(finalData).await()
            
            val url = ref.downloadUrl.await().toString()
            
            // تسجيل استهلاك الحصة اليومية بعد نجاح الرفع
            ChatValidationUtils.recordUploadToday(context)
            
            Result.success(url)
        } catch (e: Exception) {
            android.util.Log.e("ChatAttachmentManager", "Firebase Upload Error", e)
            Result.failure(e)
        }
    }
}

