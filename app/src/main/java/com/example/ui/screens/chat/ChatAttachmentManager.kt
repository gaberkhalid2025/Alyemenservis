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

        // 1. التحقق من نوع الملف والصلاحيات والحصص اليومية
        val validation = ChatValidationUtils.validateFile(uri, context, isImage)
        if (!validation.isValid) {
            return Result.failure(Exception(validation.message))
        }

        // 2. معالجة البيانات وضغط الصور في خلفية النظام (Dispatchers.IO)
        val finalData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (isImage) {
                ChatValidationUtils.compressImage(context, uri)
            } else {
                try {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()
                } catch (e: Exception) {
                    byteArrayOf()
                }
            }
        }

        // 3. التحقق النهائي من الحجم بعد عملية الضغط
        val processedValidation = ChatValidationUtils.validateProcessedData(finalData, isImage)
        if (!processedValidation.isValid) {
            return Result.failure(Exception(processedValidation.message))
        }

        // 4. تنفيذ عملية الرفع إلى Firebase Storage
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val extension = if (isImage) "jpg" else {
                    val uriStr = uri.toString().lowercase()
                    val mime = context.contentResolver.getType(uri)?.lowercase()
                    when {
                        uriStr.endsWith(".m4a") || mime == "audio/m4a" || mime == "audio/x-m4a" -> "m4a"
                        uriStr.endsWith(".mp3") || mime == "audio/mpeg" || mime == "audio/mp3" -> "mp3"
                        uriStr.endsWith(".ogg") || mime == "audio/ogg" -> "ogg"
                        uriStr.endsWith(".wav") || mime == "audio/wav" -> "wav"
                        uriStr.endsWith(".mp4") || mime == "audio/mp4" -> "m4a"
                        else -> {
                            val extFromMime = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
                            if (extFromMime.isNullOrBlank() || extFromMime == "bin") "m4a" else extFromMime
                        }
                    }
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
}

