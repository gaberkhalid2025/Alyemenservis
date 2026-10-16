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
        type: String // "image", "audio", "video"
    ): Result<String> {
        val isImage = type == "image"
        val isVideo = type == "video"
        android.util.Log.d("ChatAttachmentManager", "uploadAttachment: channelId=$channelId, type=$type, isImage=$isImage, isVideo=$isVideo")

        // 1. التحقق من نوع الملف والصلاحيات والحصص اليومية
        val validation = ChatValidationUtils.validateFile(uri, context, isImage = isImage, isVideo = isVideo)
        if (!validation.isValid) {
            return Result.failure(Exception(validation.message))
        }

        // 2. معالجة البيانات وضغط الصور في خلفية النظام (Dispatchers.IO)
        val finalData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (isImage) {
                ChatValidationUtils.compressImage(context, uri)
            } else {
                try {
                    val uriPath = uri.path
                    if (uri.scheme == "file" || (uriPath != null && java.io.File(uriPath).exists())) {
                        java.io.File(uriPath ?: uri.toString()).readBytes()
                    } else {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()
                    }
                } catch (e: Exception) {
                    byteArrayOf()
                }
            }
        }

        // 3. التحقق النهائي من الحجم بعد عملية الضغط أو القراءة
        val processedValidation = ChatValidationUtils.validateProcessedData(finalData, isImage = isImage, isVideo = isVideo)
        if (!processedValidation.isValid) {
            return Result.failure(Exception(processedValidation.message))
        }

        // التأكد من تسجيل الدخول قبل الرفع لتجنب أخطاء الصلاحيات
        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                kotlinx.coroutines.withTimeoutOrNull(4000L) {
                    auth.signInAnonymously().await()
                }
            }
        } catch (_: Exception) {}

        // 4. تنفيذ عملية الرفع إلى Firebase Storage مع مهلة محددة لمنع تجميد الشاشة
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            var uploadAttempt = 0
            var lastException: Exception? = null
            while (uploadAttempt < 2) {
                uploadAttempt++
                try {
                    val extension = when {
                        isImage -> "jpg"
                        isVideo -> "mp4"
                        else -> {
                            val uriStr = uri.toString().lowercase()
                            val mime = try { context.contentResolver.getType(uri)?.lowercase() } catch (_: Exception) { null }
                            when {
                                uriStr.endsWith(".m4a") || mime == "audio/m4a" || mime == "audio/x-m4a" -> "m4a"
                                uriStr.endsWith(".mp3") || mime == "audio/mpeg" || mime == "audio/mp3" -> "mp3"
                                uriStr.endsWith(".ogg") || mime == "audio/ogg" -> "ogg"
                                uriStr.endsWith(".wav") || mime == "audio/wav" -> "wav"
                                uriStr.endsWith(".mp4") || mime == "audio/mp4" -> "m4a"
                                else -> "m4a"
                            }
                        }
                    }
                    
                    val mimeType = when {
                        isImage -> "image/jpeg"
                        isVideo -> "video/mp4"
                        else -> {
                            when (extension) {
                                "m4a", "mp4" -> "audio/mp4"
                                "mp3" -> "audio/mpeg"
                                "ogg" -> "audio/ogg"
                                "wav" -> "audio/wav"
                                else -> "audio/mp4"
                            }
                        }
                    }
                    
                    val fileName = "${UUID.randomUUID()}_${System.currentTimeMillis()}.$extension"
                    val ref = storageRef.child("chats/$channelId/$type/$fileName")
                    val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                        .setContentType(mimeType)
                        .setCustomMetadata("channelId", channelId)
                        .setCustomMetadata("type", type)
                        .build()

                    android.util.Log.d("ChatAttachmentManager", "Uploading ${finalData.size / 1024} KB to Firebase at ${ref.path} (attempt $uploadAttempt)...")
                    val uploadTaskResult = kotlinx.coroutines.withTimeoutOrNull(20000L) {
                        ref.putBytes(finalData, metadata).await()
                        val url = ref.downloadUrl.await().toString()
                        if (url.isBlank()) throw Exception("رابط التحميل فارغ")
                        url
                    }

                    if (uploadTaskResult != null) {
                        ChatValidationUtils.recordUploadToday(context)
                        return@withContext Result.success(uploadTaskResult)
                    } else {
                        throw Exception("انتهت مهلة الرفع، جاري الحفظ محلياً والمزامنة لاحقاً")
                    }
                } catch (e: Exception) {
                    lastException = e
                    android.util.Log.e("ChatAttachmentManager", "Firebase Upload Attempt $uploadAttempt Error", e)
                    if (uploadAttempt < 2) {
                        kotlinx.coroutines.delay(600L * uploadAttempt)
                    }
                }
            }
            Result.failure(Exception(lastException?.localizedMessage ?: "فشل رفع الملف إلى الخادم. تم الحفظ في قائمة الانتظار للمزامنة فور توفر الإنترنت."))
        }
    }
}

