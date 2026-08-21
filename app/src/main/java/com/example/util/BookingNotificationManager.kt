package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.BookingEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 🔔 BookingNotificationManager
 * نظام الإشعارات المتقدم: تصنيف، منع التكرار (Deduplication)، الجدولة (Scheduling)، والمزامنة السحابية والمحلية
 */
class BookingNotificationManager(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val scope = CoroutineScope(Dispatchers.IO)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val mainHandler = Handler(Looper.getMainLooper())

    // منع تكرار الإشعارات المتطابقة خلال فترة زمنية محددة
    private val recentNotificationTimestamps = ConcurrentHashMap<String, Long>()

    companion object {
        private const val TAG = "BookingNotificationMgr"
        private const val CHANNEL_ID = "channel_bookings_alerts"
        private const val CHANNEL_NAME = "إشعارات الحجوزات والخدمات الشاملة"
        private const val DEDUPLICATION_WINDOW_MS = 10_000L // 10 ثواني منع تكرار
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات حالة الحجوزات والمواعيد والخدمات"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * التحقق من منع التكرار
     */
    private fun shouldDeliverNotification(key: String): Boolean {
        val now = System.currentTimeMillis()
        val lastTime = recentNotificationTimestamps[key] ?: 0L
        if (now - lastTime < DEDUPLICATION_WINDOW_MS) {
            Log.d(TAG, "Notification skipped due to deduplication: $key")
            return false
        }
        recentNotificationTimestamps[key] = now
        return true
    }

    /**
     * إظهار إشعار محلي مع منع التكرار
     */
    fun showLocalNotification(title: String, body: String, notificationId: Int = (1000..9999).random()) {
        val dedupKey = "$title:$body"
        if (!shouldDeliverNotification(dedupKey)) return

        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            notificationManager?.notify(notificationId, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Failed showing local notification: ${e.message}")
        }
    }

    /**
     * حفظ الإشعار في Firestore للمزامنة مع الأجهزة الأخرى
     */
    fun persistNotificationToCloud(
        targetUserId: String,
        targetRole: String, // "CLIENT", "PROVIDER", "ADMIN"
        title: String,
        body: String,
        bookingId: String,
        type: String
    ) {
        scope.launch {
            try {
                val docId = UUID.randomUUID().toString()
                val notificationData = hashMapOf(
                    "id" to docId,
                    "targetUserId" to targetUserId,
                    "targetRole" to targetRole,
                    "title" to title,
                    "body" to body,
                    "bookingId" to bookingId,
                    "type" to type,
                    "isRead" to false,
                    "createdAt" to System.currentTimeMillis()
                )
                firestore.collection("notifications").document(docId).set(notificationData)
            } catch (e: Exception) {
                Log.w(TAG, "Cloud notification push failed: ${e.message}")
            }
        }
    }

    /**
     * جدولة إشعار مستقبلي (مثل تذكير الموعد)
     */
    fun scheduleNotification(title: String, body: String, delayMs: Long, bookingId: String = "") {
        mainHandler.postDelayed({
            showLocalNotification(title, body)
            if (bookingId.isNotBlank()) {
                persistNotificationToCloud("all", "CLIENT", title, body, bookingId, "SCHEDULED_REMINDER")
            }
        }, delayMs.coerceAtLeast(0L))
    }

    /**
     * 1. إشعار عند إنشاء حجز جديد
     */
    fun notifyBookingCreated(booking: BookingEntity) {
        val title = "📅 حجز جديد #${booking.bookingNumber.ifEmpty { booking.id.take(8) }}"
        val body = "طلب حجز جديد من ${booking.customerName.ifEmpty { booking.clientName }} لخدمة ${booking.serviceType} في ${booking.customerArea.ifEmpty { booking.clientAddress }}."

        showLocalNotification(title, body)
        persistNotificationToCloud(booking.providerId, "PROVIDER", title, body, booking.id, "BOOKING_CREATED")
        persistNotificationToCloud("admin", "ADMIN", title, body, booking.id, "BOOKING_CREATED")
    }

    /**
     * 2. إشعار عند قبول الحجز
     */
    fun notifyBookingAccepted(booking: BookingEntity) {
        val title = "✅ تم قبول حجزك بنجاح!"
        val body = "وافق الفني ${booking.providerName} على حجزك لموعد ${booking.dateString.ifEmpty { booking.date }} ${booking.timeString.ifEmpty { booking.time }}."

        showLocalNotification(title, body)
        persistNotificationToCloud(booking.clientId.ifEmpty { booking.customerPhone }, "CLIENT", title, body, booking.id, "BOOKING_ACCEPTED")
        persistNotificationToCloud("admin", "ADMIN", title, body, booking.id, "BOOKING_ACCEPTED")
    }

    /**
     * 3. إشعار عند رفض الحجز
     */
    fun notifyBookingRejected(booking: BookingEntity, reason: String) {
        val title = "❌ تعذر قبول الحجز"
        val body = "تم الاعتذار عن الحجز من قبل الفني. السبب: ${reason.ifEmpty { "غير متفرغ حالياً" }}."

        showLocalNotification(title, body)
        persistNotificationToCloud(booking.clientId.ifEmpty { booking.customerPhone }, "CLIENT", title, body, booking.id, "BOOKING_REJECTED")
        persistNotificationToCloud("admin", "ADMIN", title, body, booking.id, "BOOKING_REJECTED")
    }

    /**
     * 4. إشعار عند بدء التنفيذ
     */
    fun notifyBookingStarted(booking: BookingEntity) {
        val title = "🛠️ الفني في الطريق أو بدأ التنفيذ"
        val body = "بدأ الفني ${booking.providerName} تنفيذ طلبك للخدمة (${booking.serviceType})."

        showLocalNotification(title, body)
        persistNotificationToCloud(booking.clientId.ifEmpty { booking.customerPhone }, "CLIENT", title, body, booking.id, "BOOKING_STARTED")
    }

    /**
     * 5. إشعار عند اكتمال الحجز
     */
    fun notifyBookingCompleted(booking: BookingEntity) {
        val title = "🎉 تم إكمال الخدمة بنجاح"
        val body = "تم تأكيد إنجاز الحجز #${booking.bookingNumber.ifEmpty { booking.id.take(8) }}. نرجو تقييم مستوى الخدمة."

        showLocalNotification(title, body)
        persistNotificationToCloud(booking.clientId.ifEmpty { booking.customerPhone }, "CLIENT", title, body, booking.id, "BOOKING_COMPLETED")
        persistNotificationToCloud(booking.providerId, "PROVIDER", title, body, booking.id, "BOOKING_COMPLETED")
        persistNotificationToCloud("admin", "ADMIN", title, body, booking.id, "BOOKING_COMPLETED")
    }

    /**
     * 6. إشعار عند إلغاء الحجز
     */
    fun notifyBookingCancelled(booking: BookingEntity, by: String, reason: String) {
        val title = "⚠️ تم إلغاء الحجز"
        val body = "تم إلغاء الحجز من قِبل ($by). السبب: ${reason.ifEmpty { "تم الإلغاء بناء على رغبة العميل" }}."

        showLocalNotification(title, body)
        persistNotificationToCloud(booking.clientId.ifEmpty { booking.customerPhone }, "CLIENT", title, body, booking.id, "BOOKING_CANCELLED")
        persistNotificationToCloud(booking.providerId, "PROVIDER", title, body, booking.id, "BOOKING_CANCELLED")
        persistNotificationToCloud("admin", "ADMIN", title, body, booking.id, "BOOKING_CANCELLED")
    }

    /**
     * 7. تذكير بموعد الحجز (جدولة)
     */
    fun notifyBookingReminder(booking: BookingEntity, hoursBefore: Int) {
        val title = "⏰ تذكير بموعدك القادم ($hoursBefore ${if (hoursBefore == 1) "ساعة" else "ساعات"})"
        val body = "موعد خدمتك (${booking.serviceType}) مع ${booking.providerName} سيحين في ${booking.dateString.ifEmpty { booking.date }} ${booking.timeString.ifEmpty { booking.time }}."

        showLocalNotification(title, body)
        persistNotificationToCloud(booking.clientId.ifEmpty { booking.customerPhone }, "CLIENT", title, body, booking.id, "BOOKING_REMINDER")
    }

    /**
     * 8. إشعار عند تغيير حالة الدفع
     */
    fun notifyPaymentStatusChanged(booking: BookingEntity) {
        val title = "💳 تحديث حالة الدفع"
        val body = "تم تحديث حالة دفع الحجز #${booking.bookingNumber.ifEmpty { booking.id.take(8) }} إلى: ${booking.paymentStatus}."

        showLocalNotification(title, body)
        persistNotificationToCloud(booking.clientId.ifEmpty { booking.customerPhone }, "CLIENT", title, body, booking.id, "PAYMENT_UPDATED")
    }

    fun sendBookingNotification(booking: BookingEntity, type: String) {
        when (type) {
            "CREATED" -> notifyBookingCreated(booking)
            "ACCEPTED" -> notifyBookingAccepted(booking)
            "STARTED" -> notifyBookingStarted(booking)
            "COMPLETED" -> notifyBookingCompleted(booking)
            "PAYMENT" -> notifyPaymentStatusChanged(booking)
            else -> showLocalNotification("تنبيه الحجز", "تم تحديث بيانات الحجز #${booking.bookingNumber}")
        }
    }
}
