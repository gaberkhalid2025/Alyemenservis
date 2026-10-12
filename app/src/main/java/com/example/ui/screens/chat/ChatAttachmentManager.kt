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

        // 4. تنفيذ عملية الرفع إلى Firebase Storage مع دعم البيانات الوصفية والاحتياطي المباشر
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // التأكد من وجود جلسة مصادقة Firebase لضمان اجتياز قواعد الأمان
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                if (auth.currentUser == null) {
                    try {
                        auth.signInAnonymously().await()
                    } catch (authEx: Exception) {
                        android.util.Log.w("ChatAttachmentManager", "Anonymous auth note: ${authEx.message}")
                    }
                }

                val extension = if (isImage) "jpg" else {
                    val uriStr = uri.toString().lowercase()
                    val mime = try { context.contentResolver.getType(uri)?.lowercase() } catch (_: Exception) { null }
                    when {
                        uriStr.endsWith(".m4a") || mime == "audio/m4a" || mime == "audio/x-m4a" -> "m4a"
                        uriStr.endsWith(".mp3") || mime == "audio/mpeg" || mime == "audio/mp3" -> "mp3"
                        uriStr.endsWith(".ogg") || mime == "audio/ogg" -> "ogg"
                        uriStr.endsWith(".wav") || mime == "audio/wav" -> "wav"
                        uriStr.endsWith(".mp4") || mime == "audio/mp4" -> "m4a"
                        else -> {
                            val extFromMime = mime?.let { android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                            if (extFromMime.isNullOrBlank() || extFromMime == "bin") "m4a" else extFromMime
                        }
                    }
                }

                val mimeType = if (isImage) "image/jpeg" else when (extension) {
                    "m4a", "mp4" -> "audio/mp4"
                    "mp3" -> "audio/mpeg"
                    "ogg" -> "audio/ogg"
                    "wav" -> "audio/wav"
                    "aac" -> "audio/aac"
                    else -> "audio/mp4"
                }

                val fileName = "${UUID.randomUUID()}_${System.currentTimeMillis()}.$extension"
                // استخدام مسار chat المعتمد في قواعد الأمان
                val ref = storageRef.child("chat/$channelId/$fileName")

                val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                    .setContentType(mimeType)
                    .build()

                android.util.Log.d("ChatAttachmentManager", "Uploading ${finalData.size / 1024} KB to Firebase Storage (mime: $mimeType)...")
                ref.putBytes(finalData, metadata).await()
                
                val url = ref.downloadUrl.await().toString()
                
                // تسجيل استهلاك الحصة اليومية بعد نجاح الرفع
                ChatValidationUtils.recordUploadToday(context)
                
                Result.success(url)
            } catch (e: Exception) {
                android.util.Log.e("ChatAttachmentManager", "Firebase Storage Upload Error, activating smart data fallback: ${e.message}", e)
                try {
                    // نظام الاحتياط الذكي الفوري: تحويل الوسائط المضغوطة إلى data URL آمنة
                    // لضمان استمرار المحادثات وتخطي أخطاء الحصة المجانية دون أي توقف
                    val base64Data = android.util.Base64.encodeToString(finalData, android.util.Base64.NO_WRAP)
                    val mimeType = if (isImage) "image/jpeg" else "audio/mp4"
                    val fallbackDataUrl = "data:$mimeType;base64,$base64Data"
                    ChatValidationUtils.recordUploadToday(context)
                    Result.success(fallbackDataUrl)
                } catch (fallbackEx: Exception) {
                    Result.failure(e)
                }
            }
        }
    }
}

