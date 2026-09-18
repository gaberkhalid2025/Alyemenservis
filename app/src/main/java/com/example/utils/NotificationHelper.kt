// utils/NotificationHelper.kt
package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build

/**
 * 🔔 مساعد الإشعارات الموحد
 * ينشئ كل قنوات الإشعارات المطلوبة للتطبيق ولوحة تحكم الإدارة
 */
object NotificationHelper {
    
    // ثوابت القنوات (مستخدمة في Cloud Functions والخدمات السحابية)
    const val CHANNEL_ADMIN_CRITICAL = "admin_critical"
    const val CHANNEL_ADMIN_NORMAL = "admin_normal"
    const val CHANNEL_USER_BOOKINGS = "user_bookings"
    const val CHANNEL_USER_CHAT = "user_chat"
    const val CHANNEL_USER_GENERAL = "user_general"
    
    /**
     * 🎯 إنشاء قنوات الإشعارات
     * يُستدعى مرة واحدة عند بدء التطبيق (MainActivity.onCreate)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        
        // قناة الأدمن الحرجة
        val adminCritical = NotificationChannel(
            CHANNEL_ADMIN_CRITICAL,
            "إشعارات الأدمن الحرجة",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "طلبات استعادة كلمة المرور، البلاغات العاجلة، والمدفوعات"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500)
            enableLights(true)
            lightColor = Color.RED
            setShowBadge(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setBypassDnd(true)
            }
        }
        manager.createNotificationChannel(adminCritical)
        
        // قناة الأدمن العادية
        val adminNormal = NotificationChannel(
            CHANNEL_ADMIN_NORMAL,
            "إشعارات الأدمن العادية",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "تحديثات، إحصائيات، تنبيهات عامة للإدارة"
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(adminNormal)
        
        // قناة الحجوزات
        val userBookings = NotificationChannel(
            CHANNEL_USER_BOOKINGS,
            "إشعارات الحجوزات",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "حجوزاتك وتحديثاتها أولاً بأول"
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(userBookings)
        
        // قناة المحادثات
        val userChat = NotificationChannel(
            CHANNEL_USER_CHAT,
            "إشعارات المحادثات",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "رسائل المحادثات والدعم الفني الجديدة"
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(userChat)
        
        // قناة عامة
        val userGeneral = NotificationChannel(
            CHANNEL_USER_GENERAL,
            "إشعارات عامة",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "تنبيهات عامة وعروض المنصة"
        }
        manager.createNotificationChannel(userGeneral)
    }
}
