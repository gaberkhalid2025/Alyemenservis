package com.example.util

import android.content.Context
import android.net.Uri

/**
 * 🖼️ ImageUtils - أدوات مساعدة لمعالجة وتحويل الصور
 * يفوض العمليات إلى المحرك المركزي [ImageOptimizer] لضمان أعلى أداء وتوفير البيانات
 */
object ImageUtils {

    /**
     * تحويل مسار الصورة (Uri) إلى سلسلة Base64 مضغوطة
     * 
     * @param context سياق التطبيق
     * @param uri مسار الصورة
     * @param maxWidth أقصى عرض
     * @param quality جودة الضغط (1-100)
     * @return النص المشفر بتنسيق Base64
     */
    fun uriToBase64(context: Context, uri: Uri, maxWidth: Int = 800, quality: Int = 75): String {
        return ImageOptimizer.uriToOptimizedBase64(context, uri, maxWidth, maxWidth, quality)
    }

    /**
     * تحويل مسار الصورة (Uri) إلى سلسلة Base64 مع تحديد أقصى طول وعرض
     */
    fun uriToCompressedBase64(
        context: Context,
        uri: Uri,
        maxWidth: Int = 800,
        maxHeight: Int = 800,
        quality: Int = 75
    ): String {
        return ImageOptimizer.uriToOptimizedBase64(context, uri, maxWidth, maxHeight, quality)
    }
}
