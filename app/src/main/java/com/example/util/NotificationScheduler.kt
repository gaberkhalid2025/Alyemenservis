package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.Keep
import com.example.data.local.ScheduledNotificationDatabase
import com.example.data.local.ScheduledNotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

@Keep
data class ScheduledNotification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val scheduledTime: Long = 0L,
    val isActive: Boolean = true,
    val type: String = "REMINDER",
    val bookingId: String = ""
)

/**
 * 📅 NotificationScheduler
 * 
 * جدولة الإشعارات والتنبيهات المباشرة والدورية.
 * تم تحديثه ليعتمد بالكامل على **Room Database (`ScheduledNotificationDatabase`)** بدلاً من `SharedPreferences`
 * لحفظ الجداول الزمنية للإنذارات وضمان عمل التذكيرات بنجاح حتى عند إعادة تشغيل الجهاز.
 */
class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val db = ScheduledNotificationDatabase.getInstance(context)
    private val dao = db.scheduledNotificationDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "NotificationScheduler"
    }

    /**
     * جدولة تذكير بموعد محدد
     */
    fun scheduleReminder(bookingId: String, hoursBefore: Int, appointmentTimeMillis: Long = System.currentTimeMillis() + (hoursBefore + 1) * 3600 * 1000L): Boolean {
        try {
            val triggerTime = appointmentTimeMillis - (hoursBefore * 3600 * 1000L)
            if (triggerTime <= System.currentTimeMillis()) {
                Log.w(TAG, "Trigger time already passed for booking $bookingId")
                return false
            }

            val title = "تذكير بالموعد ⏰"
            val message = if (hoursBefore == 24) {
                "تذكير: موعد خدمتك غداً للطلب رقم #$bookingId"
            } else {
                "تذكير: موعد خدمتك بعد $hoursBefore ساعة للطلب رقم #$bookingId"
            }

            val notif = ScheduledNotification(
                id = "REMINDER_${bookingId}_${hoursBefore}H",
                title = title,
                message = message,
                scheduledTime = triggerTime,
                isActive = true,
                type = "REMINDER",
                bookingId = bookingId
            )

            setExactAlarm(notif)
            saveToRoom(notif)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling reminder", e)
            return false
        }
    }

    /**
     * جدولة إشعار تذكيري يومي
     */
    fun scheduleDailyNotification(title: String, message: String, hour: Int): Boolean {
        try {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val notif = ScheduledNotification(
                id = "DAILY_${UUID.randomUUID()}",
                title = title,
                message = message,
                scheduledTime = calendar.timeInMillis,
                isActive = true,
                type = "DAILY"
            )

            setRepeatingAlarm(notif, AlarmManager.INTERVAL_DAY)
            saveToRoom(notif)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling daily notification", e)
            return false
        }
    }

    /**
     * جدولة إشعار أسبوعي
     */
    fun scheduleWeeklyNotification(title: String, message: String, dayOfWeek: Int, hour: Int): Boolean {
        try {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            val notif = ScheduledNotification(
                id = "WEEKLY_${UUID.randomUUID()}",
                title = title,
                message = message,
                scheduledTime = calendar.timeInMillis,
                isActive = true,
                type = "WEEKLY"
            )

            setRepeatingAlarm(notif, AlarmManager.INTERVAL_DAY * 7)
            saveToRoom(notif)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling weekly notification", e)
            return false
        }
    }

    /**
     * إلغاء إشعار مجدول
     */
    fun cancelScheduledNotification(notificationId: String): Boolean {
        try {
            scope.launch {
                dao.deleteById(notificationId)
            }
            val dummy = ScheduledNotification(id = notificationId)
            cancelAlarm(dummy)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling scheduled notification", e)
            return false
        }
    }

    /**
     * جلب قائمة الإشعارات المجدولة من Room
     */
    suspend fun getScheduledNotificationsFromDb(): List<ScheduledNotification> {
        return dao.getActiveNotificationsList().map {
            ScheduledNotification(
                id = it.id,
                title = it.title,
                message = it.message,
                scheduledTime = it.scheduledTime,
                isActive = it.isActive,
                type = it.type,
                bookingId = it.bookingId
            )
        }
    }

    private fun setExactAlarm(notif: ScheduledNotification) {
        val intent = getPendingIntent(notif)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notif.scheduledTime, intent)
        } else {
            alarmManager?.setExact(AlarmManager.RTC_WAKEUP, notif.scheduledTime, intent)
        }
    }

    private fun setRepeatingAlarm(notif: ScheduledNotification, intervalMillis: Long) {
        val intent = getPendingIntent(notif)
        alarmManager?.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            notif.scheduledTime,
            intervalMillis,
            intent
        )
    }

    private fun cancelAlarm(notif: ScheduledNotification) {
        val intent = getPendingIntent(notif)
        alarmManager?.cancel(intent)
    }

    private fun getPendingIntent(notif: ScheduledNotification): PendingIntent {
        val intent = Intent(context, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_id", notif.id)
            putExtra("notification_title", notif.title)
            putExtra("notification_message", notif.message)
            putExtra("target_screen", "bookings")
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, notif.id.hashCode(), intent, flags)
    }

    private fun saveToRoom(notif: ScheduledNotification) {
        scope.launch {
            val entity = ScheduledNotificationEntity(
                id = notif.id,
                title = notif.title,
                message = notif.message,
                scheduledTime = notif.scheduledTime,
                isActive = notif.isActive,
                type = notif.type,
                bookingId = notif.bookingId
            )
            dao.insert(entity)
        }
    }
}
