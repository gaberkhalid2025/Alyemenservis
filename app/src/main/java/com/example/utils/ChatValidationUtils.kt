package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

/**
 * 📦 ChatValidationUtils
 * أدوات التحقق من نوع الملف، الحجم الأقصى (2MB)، وتطبيق الضغط الشديد للأبعاد (800x800) وجودة 60%
 * لتوفير استهلاك باقة Firebase Free Tier وتقليل حجم البيانات.
 */
object ChatValidationUtils {

    const val MAX_FILE_SIZE = 15 * 1024 * 1024L // 15 ميجابايت كحد أقصى للملف الخام قبل الضغط
    const val MAX_TEXT_LENGTH = 500 // الحد الأقصى لطول الرسالة النصية
    const val MAX_DAILY_UPLOADS = 15 // الحد الأقصى لرفع الصور اليومي لكل مستخدم

    val allowedMimeTypes = listOf(
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp",
        "audio/mpeg", "audio/mp3", "audio/aac", "audio/amr", "audio/wav", "audio/ogg", "audio/3gpp",
        "audio/mp4", "audio/m4a", "audio/x-m4a",
        "video/mp4", "video/3gpp"
    )

    fun validateFile(uri: Uri, context: Context): ValidationResult {
        var mimeType = context.contentResolver.getType(uri)
        if (mimeType.isNullOrBlank()) {
            val extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            if (extension.isNotBlank()) {
                mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            }
        }
        android.util.Log.d("ChatValidation", "validateFile: uri=$uri, resolvedMime=$mimeType")

        if (mimeType != null && mimeType !in allowedMimeTypes && !mimeType.startsWith("image/") && !mimeType.startsWith("audio/")) {
            android.util.Log.w("ChatValidation", "validateFile: Mime rejected: $mimeType")
            return ValidationResult(
                isValid = false,
                message = "⚠️ نوع الملف ($mimeType) غير مدعوم. يرجى اختيار صورة أو تسجيل صوتي فقط."
            )
        }

        val fileSize = try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                it.length
            } ?: 0L
        } catch (e: Exception) {
            android.util.Log.w("ChatValidation", "validateFile: Failed to read file length: ${e.message}")
            0L
        }

        android.util.Log.d("ChatValidation", "validateFile: fileSize=$fileSize bytes")

        if (fileSize > MAX_FILE_SIZE) {
            val sizeMb = String.format(java.util.Locale.US, "%.1f", fileSize.toDouble() / (1024 * 1024))
            android.util.Log.w("ChatValidation", "validateFile: File size exceeded: $sizeMb MB")
            return ValidationResult(
                isValid = false,
                message = "⚠️ الملف كبير جداً ($sizeMb ميجابايت). الحد الأقصى المسموح 15 ميجابايت."
            )
        }

        val uploadAllowed = canUploadToday(context)
        android.util.Log.d("ChatValidation", "validateFile: canUploadToday=$uploadAllowed")
        if (!uploadAllowed) {
            return ValidationResult(
                isValid = false,
                message = "⚠️ تجاوزت الحد اليومي المسموح به لرفع الملفات ($MAX_DAILY_UPLOADS وسائط يومياً)."
            )
        }

        return ValidationResult(isValid = true, message = "")
    }

    fun compressImage(context: Context, uri: Uri): ByteArray {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) return byteArrayOf()

            val maxWidth = 800
            val maxHeight = 800
            val scaledBitmap = if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
                val scale = minOf(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 65, outputStream)
            val result = outputStream.toByteArray()
            android.util.Log.d("ChatValidation", "compressImage: raw=${scaledBitmap.byteCount}, compressed=${result.size} bytes")
            result
        } catch (e: Exception) {
            android.util.Log.e("ChatValidation", "compressImage failed: ${e.message}", e)
            byteArrayOf()
        }
    }

    fun generateThumbnail(context: Context, uri: Uri): ByteArray {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) return byteArrayOf()

            val thumbWidth = 150
            val thumbHeight = (150f * bitmap.height / bitmap.width.coerceAtLeast(1)).toInt().coerceIn(60, 200)
            val thumbBitmap = Bitmap.createScaledBitmap(bitmap, thumbWidth, thumbHeight, true)

            val outputStream = ByteArrayOutputStream()
            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
            outputStream.toByteArray()
        } catch (_: Exception) {
            byteArrayOf()
        }
    }

    fun canUploadToday(context: Context): Boolean {
        val prefs = context.getSharedPreferences("chat_upload_limits", Context.MODE_PRIVATE)
        val todayStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
        val lastDate = prefs.getString("last_upload_date", "")
        val count = if (lastDate == todayStr) prefs.getInt("upload_count", 0) else 0
        val isAllowed = count < MAX_DAILY_UPLOADS
        android.util.Log.d("ChatValidation", "canUploadToday: today=$todayStr, lastDate=$lastDate, count=$count/$MAX_DAILY_UPLOADS -> allowed=$isAllowed")
        return isAllowed
    }

    fun recordUploadToday(context: Context) {
        val prefs = context.getSharedPreferences("chat_upload_limits", Context.MODE_PRIVATE)
        val todayStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
        val lastDate = prefs.getString("last_upload_date", "")
        val count = if (lastDate == todayStr) prefs.getInt("upload_count", 0) else 0
        prefs.edit()
            .putString("last_upload_date", todayStr)
            .putInt("upload_count", count + 1)
            .apply()
        android.util.Log.d("ChatValidation", "recordUploadToday: incremented to ${count + 1} for $todayStr")
    }
}
