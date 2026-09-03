package com.example.chat.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * 🛡️ ValidationResult
 * نتيجة فحص والتحقق من الملفات والوسائط قبل الرفع
 */
data class ValidationResult(val isValid: Boolean, val message: String)

/**
 * 📦 ChatValidationUtils
 * أدوات التحقق من نوع الملف، الحجم الأقصى (2MB)، وتطبيق الضغط الشديد للأبعاد (800x800) وجودة 60%
 * لتوفير استهلاك باقة Firebase Free Tier وتقليل حجم البيانات.
 */
object ChatValidationUtils {

    const val MAX_FILE_SIZE = 2 * 1024 * 1024L // 2 ميجابايت للحفاظ على المساحة
    const val MAX_TEXT_LENGTH = 500 // الحد الأقصى لطول الرسالة النصية
    const val MAX_DAILY_UPLOADS = 10 // الحد الأقصى لرفع الصور اليومي لكل مستخدم

    val allowedMimeTypes = listOf(
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "audio/mpeg", "audio/mp3", "audio/aac", "audio/amr"
    )

    fun validateFile(uri: Uri, context: Context): ValidationResult {
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType != null && mimeType !in allowedMimeTypes) {
            return ValidationResult(
                isValid = false,
                message = "⚠️ نوع الملف غير مدعوم. يرجى اختيار صورة أو تسجيل صوتي فقط."
            )
        }

        val fileSize = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
            it.length
        } ?: 0L

        if (fileSize > MAX_FILE_SIZE) {
            val sizeMb = String.format("%.1f", fileSize.toDouble() / (1024 * 1024))
            return ValidationResult(
                isValid = false,
                message = "⚠️ الملف كبير جداً ($sizeMb ميجابايت). الحد الأقصى المسموح 2 ميجابايت."
            )
        }

        return ValidationResult(isValid = true, message = "")
    }

    fun compressImage(context: Context, uri: Uri): ByteArray {
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
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
        return outputStream.toByteArray()
    }

    fun canUploadToday(context: Context): Boolean {
        val prefs = context.getSharedPreferences("chat_upload_limits", Context.MODE_PRIVATE)
        val todayStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
        val lastDate = prefs.getString("last_upload_date", "")
        val count = if (lastDate == todayStr) prefs.getInt("upload_count", 0) else 0
        return count < MAX_DAILY_UPLOADS
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
    }
}
