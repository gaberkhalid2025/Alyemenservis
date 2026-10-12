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

    const val MAX_FILE_SIZE = (1.5 * 1024 * 1024).toLong() // 1.5MB - الحد الأقصى المسموح للرفع (بعد الضغط للصور أو خام للصوت)
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

        // للملفات الصوتية: نطبق فحص الحجم الأقصى فوراً قبل الرفع لتوفير الباندويث
        if (!isImage && fileSize > MAX_FILE_SIZE) {
            val sizeMb = String.format(java.util.Locale.US, "%.1f", fileSize.toDouble() / (1024 * 1024))
            return ValidationResult(
                isValid = false,
                message = "⚠️ التسجيل الصوتي كبير جداً ($sizeMb MB). الحد الأقصى 1.5 ميجابايت."
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
     * تطبيق ضغط شديد للصور (600x600) وجودة 60% لتقليل استهلاك Firebase Storage مع الحماية من OOM
     */
    fun compressImage(context: Context, uri: Uri): ByteArray {
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            var inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
            inputStream?.close()

            val maxWidth = 600
            val maxHeight = 600
            var sampleSize = 1
            while (boundsOptions.outWidth / sampleSize > maxWidth * 2 || boundsOptions.outHeight / sampleSize > maxHeight * 2) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (bitmap == null) return byteArrayOf()

            val scaledBitmap = if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
                val scale = minOf(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
                val newW = (bitmap.width * scale).toInt().coerceAtLeast(1)
                val newH = (bitmap.height * scale).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
                if (scaled != bitmap) bitmap.recycle()
                scaled
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            // جودة 60% كافية جداً للمعاينة في الشات مع حجم ملف صغير جداً (بالكيلوبايت)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            if (!scaledBitmap.isRecycled) scaledBitmap.recycle()
            val result = outputStream.toByteArray()
            
            android.util.Log.d("ChatValidation", "Final compressed size: ${result.size / 1024} KB")
            result
        } catch (e: Exception) {
            android.util.Log.e("ChatValidation", "Compression error", e)
            byteArrayOf()
        }
    }

    fun validateProcessedData(data: ByteArray, isImage: Boolean): ValidationResult {
        if (data.isEmpty()) {
            return ValidationResult(isValid = false, message = "تعذر معالجة بيانات الملف.")
        }
        if (data.size > MAX_FILE_SIZE) {
            val sizeMb = String.format(java.util.Locale.US, "%.1f", data.size.toDouble() / (1024 * 1024))
            return ValidationResult(isValid = false, message = "⚠️ حجم الملف ($sizeMb MB) يتجاوز الحد المسموح (1.5MB).")
        }
        return ValidationResult(isValid = true, message = "")
    }

    fun generateThumbnail(context: Context, uri: Uri): ByteArray {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inSampleSize = 4 }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (bitmap == null) return byteArrayOf()

            val thumbHeight = if (bitmap.width > 0) (150f * bitmap.height / bitmap.width).toInt().coerceAtLeast(1) else 150
            val thumbBitmap = Bitmap.createScaledBitmap(bitmap, 150, thumbHeight, true)
            val outputStream = ByteArrayOutputStream()
            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
            if (thumbBitmap != bitmap) bitmap.recycle()
            if (!thumbBitmap.isRecycled) thumbBitmap.recycle()
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

