package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log

/**
 * 🚀 AppSetup - إعداد وتهيئة الخدمات الأساسية للتطبيق عند الإقلاع
 * 
 * الميزات:
 * 1. إنشاء وتجهيز قنوات الإشعارات الفورية لنظام أندرويد 8.0 فما فوق (Oreo+).
 * 2. تهيئة الكاش المبدئي للصور والبيانات المحلية وإزالة التالف منها.
 * 3. فحص بيئة الأمان وسجلات الأخطاء.
 */
object AppSetup {
    private const val TAG = "AppSetup"
    const val DEFAULT_NOTIFICATION_CHANNEL_ID = "YEMEN_SERVICES_CHANNEL"
    const val CHAT_NOTIFICATION_CHANNEL_ID = "YEMEN_SERVICES_CHAT_CHANNEL"

    /**
     * التهيئة المركزية لجميع مكونات ومرافق التطبيق
     * @param context سياق التطبيق
     */
    fun initializeApplication(context: Context) {
        try {
            Log.i(TAG, "Initializing Yemen Services platform configuration...")
            setupNotificationChannels(context)
            setupImageCacheDefaults(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete app setup: ${e.message}", e)
        }
    }

    /**
     * إعداد قنوات الإشعارات للطلبات والمحادثات
     */
    private fun setupNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // 1. قناة الطلبات والخدمات
            val serviceChannel = NotificationChannel(
                DEFAULT_NOTIFICATION_CHANNEL_ID,
                "تنبيهات الخدمات والطلبات",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قناة الإشعارات الفورية لطلبات الصيانة والحجوزات في اليمن"
                enableVibration(true)
            }

            // 2. قناة المحادثات الفورية
            val chatChannel = NotificationChannel(
                CHAT_NOTIFICATION_CHANNEL_ID,
                "المحادثات والرسائل المباشرة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قناة الإشعارات للرسائل الجديدة بين العملاء والفنيين"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(chatChannel)
        }
    }

    /**
     * تنظيف الكاش الزائد وإعداد الحدود التلقائية
     */
    private fun setupImageCacheDefaults(context: Context) {
        ImageOptimizer.clearExcessCache(context)
        Log.d(TAG, "Image caching and offline storage initialized successfully.")
    }
}
