package com.example.utils.date

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 📅 DateUtils
 * مركز توحيد معالجة وتنسيق التواريخ والأوقات في التطبيق
 */
object DateUtils {

    fun formatTimestamp(
        timestamp: Long,
        pattern: String = "yyyy-MM-dd HH:mm",
        locale: Locale = Locale.getDefault()
    ): String {
        if (timestamp <= 0L) return ""
        return try {
            val sdf = SimpleDateFormat(pattern, locale)
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    fun formatDateOnly(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        return formatTimestamp(timestamp, "yyyy-MM-dd", locale)
    }

    fun formatTimeOnly(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        return formatTimestamp(timestamp, "hh:mm a", locale)
    }
}
