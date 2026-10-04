package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

/**
 * 📦 ChatValidationUtils
 * أدوات التحقق من نوع الملف، الحجم الأقصى (2MB)، وتطبيق الضغط الشديد للأبعاد (800x800) وجودة 70%
 * لتوفير استهلاك باقة Firebase Free Tier وتقليل حجم البيانات المرفوعة.
 */
object ChatValidationUtils {

    const val MAX_FILE_SIZE = 2 * 1024 * 1024L // 2MB - الحد الأقصى المسموح للرفع (بعد الضغط للصور أو خام للصوت)
    const val MAX_TEXT_LENGTH = 1000
    const val MAX_DAILY_UPLOADS = 25

    // القائمة المحدثة للصيغ المسموح بها لتشمل كافة أنواع التسجيل الصوتي
    val allowedMimeTypes = listOf(
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp",
        "audio/mpeg", "audio/mp3", "audio/aac", "audio/amr", "audio/wav", "audio/ogg", "audio/3gpp",
        "audio/mp4", "audio/m4a", "audio/x-m4a",
        "video/mp4", "video/3gpp"
    )

    fun validateFile(uri: Uri, context: Context, isImage: Boolean): ValidationResult {
        var mimeType = context.contentResolver.getType(uri)
        if (mimeType.isNullOrBlank()) {
            val extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            if (extension.isNotBlank()) {
                mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            }
        }

        // 1. فحص نوع الملف (MIME Type)
        val isSupported = mimeType != null && (mimeType in allowedMimeTypes || mimeType.startsWith("image/") || mimeType.startsWith("audio/"))
        if (!isSupported) {
            return ValidationResult(
                isValid = false,
                message = "⚠️ نوع الملف ($mimeType) غير مدعوم حالياً."
            )
        }

        // 2. فحص الحجم الخام
        val fileSize = try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
        } catch (_: Exception) { 0L }

        // للملفات الصوتية: نطبق فحص الـ 2MB فوراً قبل الرفع لتوفير الباندويث
        if (!isImage && fileSize > MAX_FILE_SIZE) {
            val sizeMb = String.format(java.util.Locale.US, "%.1f", fileSize.toDouble() / (1024 * 1024))
            return ValidationResult(
                isValid = false,
                message = "⚠️ التسجيل الصوتي كبير جداً ($sizeMb MB). الحد الأقصى 2 ميجابايت."
            )
        }
        
        // للصور: نتجاوز فحص الحجم الخام هنا لأننا سنقوم بضغطها لاحقاً لتقليل حجمها

        // 3. فحص الحصص اليومية
        if (!canUploadToday(context)) {
            return ValidationResult(
                isValid = false,
                message = "⚠️ تجاوزت الحد اليومي المسموح به لرفع الوسائط."
            )
        }

        return ValidationResult(isValid = true, message = "")
    }

    /**
     * تطبيق ضغط شديد للصور (800x800) وجودة 70% لتقليل استهلاك Firebase Storage
     */
    fun compressImage(context: Context, uri: Uri): ByteArray {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) return byteArrayOf()

            // أقصى أبعاد مسموحة 800x800
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
            // جودة 70% كافية جداً للمعاينة في الشات مع حجم ملف صغير جداً (بالكيلوبايت)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val result = outputStream.toByteArray()
            
            android.util.Log.d("ChatValidation", "Final compressed size: ${result.size / 1024} KB")
            result
        } catch (e: Exception) {
            android.util.Log.e("ChatValidation", "Compression error", e)
            byteArrayOf()
        }
    }

    fun generateThumbnail(context: Context, uri: Uri): ByteArray {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inSampleSize = 4 }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (bitmap == null) return byteArrayOf()

            val thumbBitmap = Bitmap.createScaledBitmap(bitmap, 150, (150f * bitmap.height / bitmap.width).toInt(), true)
            val outputStream = ByteArrayOutputStream()
            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
            outputStream.toByteArray()
        } catch (_: Exception) { byteArrayOf() }
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

