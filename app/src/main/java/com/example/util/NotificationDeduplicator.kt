package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.NotificationEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * 🛡️ NotificationDeduplicator
 * 
 * فحص وتصفية الإشعارات المتكررة لمنع إرسال نفس التنبيه للمستخدم أكثر من مرة خلال فترة زمنية محددة.
 * يتضمن آليات الفرز والتنظيف الآلي للإشعارات القديمة وإدارة بصمات التنبيه الفريدة.
 */
class NotificationDeduplicator(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("notification_dedup_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "NotificationDedup"
        private const val KEY_SENT_IDS = "sent_notification_ids"
        private const val KEY_RECORD_TIMESTAMPS = "notification_timestamps"
    }

    /**
     * توليد معرّف فريد للإشعار يعتمد على النوع، الهدف والتوقيت
     */
    fun generateUniqueId(notification: NotificationEntity): String {
        val target = if (notification.targetValue.isNotBlank()) notification.targetValue else notification.customerPhone
        val timeBucket = notification.timestamp / (60 * 1000L)
        return "${notification.notificationType}_${target}_${timeBucket}"
    }

    /**
     * فحص هل الإشعار مكرر
     */
    fun isDuplicate(notification: NotificationEntity): Boolean {
        val uniqueId = if (notification.id.isNotBlank()) notification.id else generateUniqueId(notification)
        val sentSet = getSentNotifications().toSet()
        val isDup = sentSet.contains(uniqueId)
        if (isDup) {
            Log.d(TAG, "Duplicate notification detected: $uniqueId")
        }
        return isDup
    }

    /**
     * تعليم الإشعار كمرسل
     */
    fun markAsSent(notification: NotificationEntity) {
        val uniqueId = if (notification.id.isNotBlank()) notification.id else generateUniqueId(notification)
        val list = getSentNotifications().toMutableList()
        if (!list.contains(uniqueId)) {
            list.add(uniqueId)
            saveSentNotifications(list)

            val timestamps = getNotificationTimestamps().toMutableMap()
            timestamps[uniqueId] = System.currentTimeMillis()
            saveNotificationTimestamps(timestamps)
        }
    }

    /**
     * تصفية القائمة واستبعاد الإشعارات المكررة
     */
    fun removeDuplicates(notifications: List<NotificationEntity>): List<NotificationEntity> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<NotificationEntity>()
        for (notif in notifications) {
            val uid = if (notif.id.isNotBlank()) notif.id else generateUniqueId(notif)
            if (seen.add(uid)) {
                result.add(notif)
            }
        }
        return result
    }

    /**
     * مسح الإشعارات القديمة أقدم من عدد أيام معين
     */
    fun cleanOldNotifications(daysToKeep: Int = 30) {
        try {
            val cutoff = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
            val timestamps = getNotificationTimestamps().toMutableMap()
            val sentList = getSentNotifications().toMutableList()

            val toRemove = timestamps.filter { it.value < cutoff }.keys
            for (key in toRemove) {
                timestamps.remove(key)
                sentList.remove(key)
            }

            saveNotificationTimestamps(timestamps)
            saveSentNotifications(sentList)
            Log.d(TAG, "Cleaned ${toRemove.size} old notifications older than $daysToKeep days")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning old notifications", e)
        }
    }

    /**
     * جلب المعرفات التي تم إرسالها
     */
    fun getSentNotifications(): List<String> {
        val jsonStr = prefs.getString(KEY_SENT_IDS, null) ?: return emptyList()
        val list = mutableListOf<String>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing sent notifications", e)
        }
        return list
    }

    private fun saveSentNotifications(list: List<String>) {
        val arr = JSONArray(list)
        prefs.edit().putString(KEY_SENT_IDS, arr.toString()).apply()
    }

    private fun getNotificationTimestamps(): Map<String, Long> {
        val jsonStr = prefs.getString(KEY_RECORD_TIMESTAMPS, null) ?: return emptyMap()
        val map = mutableMapOf<String, Long>()
        try {
            val obj = JSONObject(jsonStr)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.optLong(key)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing notification timestamps", e)
        }
        return map
    }

    private fun saveNotificationTimestamps(map: Map<String, Long>) {
        val obj = JSONObject()
        for ((k, v) in map) {
            obj.put(k, v)
        }
        prefs.edit().putString(KEY_RECORD_TIMESTAMPS, obj.toString()).apply()
    }
}
