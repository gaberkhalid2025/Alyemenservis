package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.Keep
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

@Keep
data class ScheduledNotification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val scheduledTime: Long = 0L,
    val isActive: Boolean = true,
    val type: String = "REMINDER", // "REMINDER", "DAILY", "WEEKLY"
    val bookingId: String = ""
)

/**
 * 📅 NotificationScheduler
 * مسؤول عن جدولة الإشعارات في أوقات محددة وتذكير المواعيد والإشعارات الدورية
 */
class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val prefs = context.getSharedPreferences("scheduled_notifications_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "NotificationScheduler"
        private const val KEY_NOTIFICATIONS = "saved_scheduled_notifications"
    }

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
            saveNotification(notif)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling reminder", e)
            return false
        }
    }

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
            saveNotification(notif)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling daily notification", e)
            return false
        }
    }

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
            saveNotification(notif)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling weekly notification", e)
            return false
        }
    }

    fun cancelScheduledNotification(notificationId: String): Boolean {
        try {
            val list = getScheduledNotifications().toMutableList()
            val index = list.indexOfFirst { it.id == notificationId }
            if (index != -1) {
                val notif = list[index]
                cancelAlarm(notif)
                list.removeAt(index)
                saveAllNotifications(list)
                return true
            }
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling scheduled notification", e)
            return false
        }
    }

    fun getScheduledNotifications(): List<ScheduledNotification> {
        val jsonStr = prefs.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        val list = mutableListOf<ScheduledNotification>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ScheduledNotification(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        message = obj.optString("message"),
                        scheduledTime = obj.optLong("scheduledTime"),
                        isActive = obj.optBoolean("isActive", true),
                        type = obj.optString("type", "REMINDER"),
                        bookingId = obj.optString("bookingId")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing scheduled notifications", e)
        }
        return list
    }

    fun rescheduleNotification(notificationId: String, newTime: Long): Boolean {
        try {
            val list = getScheduledNotifications().toMutableList()
            val index = list.indexOfFirst { it.id == notificationId }
            if (index != -1) {
                val old = list[index]
                cancelAlarm(old)
                val updated = old.copy(scheduledTime = newTime)
                setExactAlarm(updated)
                list[index] = updated
                saveAllNotifications(list)
                return true
            }
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error rescheduling notification", e)
            return false
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

    private fun saveNotification(notif: ScheduledNotification) {
        val current = getScheduledNotifications().filter { it.id != notif.id }.toMutableList()
        current.add(notif)
        saveAllNotifications(current)
    }

    private fun saveAllNotifications(list: List<ScheduledNotification>) {
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("message", item.message)
            obj.put("scheduledTime", item.scheduledTime)
            obj.put("isActive", item.isActive)
            obj.put("type", item.type)
            obj.put("bookingId", item.bookingId)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_NOTIFICATIONS, jsonArray.toString()).apply()
    }
}
