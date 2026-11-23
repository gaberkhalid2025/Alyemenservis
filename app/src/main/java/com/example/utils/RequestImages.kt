package com.example.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

/**
 * 🖼️ RequestImages
 * فك تشفير صور الهوية والطلبات ومعاينتها بأمان (Document 74)
 */
object RequestImages {
    /** يفكّ نصاً مشفّراً إلى بايتات صورة، أو null. */
    fun decryptToBytes(encrypted: String?): ByteArray? {
        if (encrypted.isNullOrBlank()) return null
        return try {
            val plain = SecurityCryptoUtils.decryptCrossDevice(encrypted).ifBlank {
                SecurityCryptoUtils.decrypt(encrypted)
            }
            if (plain.isBlank()) return null
            val b64 = plain.substringAfter("base64,", plain).trim()
            Base64.decode(b64, Base64.DEFAULT)
        } catch (_: Exception) {
            null
        }
    }

    fun toBitmap(bytes: ByteArray?, maxSide: Int = 1280): Bitmap? {
        if (bytes == null || bytes.isEmpty()) return null
        return try {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
            var s = 1
            while (maxOf(o.outWidth, o.outHeight) / s > maxSide * 2) s *= 2
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = s })
        } catch (_: Exception) {
            null
        }
    }
}
