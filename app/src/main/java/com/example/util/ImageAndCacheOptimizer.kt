package com.example.util

import android.content.Context

/**
 * ⚡ ImageAndCacheOptimizer - تحسين استهلاك الذاكرة والكاش وتوفير مساحة التخزين
 * يفوض العمليات إلى المحرك المركزي [ImageOptimizer]
 */
object ImageAndCacheOptimizer {

    /**
     * حساب حجم الكاش الحالي بالميجابايت
     * @param context سياق التطبيق
     * @return حجم الكاش بالميجابايت
     */
    fun getCacheSizeMB(context: Context): Double {
        return ImageOptimizer.getCacheSizeMB(context)
    }

    /**
     * مسح وحذف كافة الملفات المؤقتة المخزنة في الكاش
     * @param context سياق التطبيق
     */
    fun clearAllAppCache(context: Context) {
        ImageOptimizer.clearAllAppCache(context)
    }

    /**
     * فحص وتنظيف الكاش الزائد إذا تجاوز الحد المسموح (50 ميجابايت)
     * @param context سياق التطبيق
     */
    fun clearExcessCache(context: Context) {
        ImageOptimizer.clearExcessCache(context)
    }
}
