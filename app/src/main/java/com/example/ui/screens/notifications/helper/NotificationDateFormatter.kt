package com.example.ui.screens.notifications.helper

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * ⏰ NotificationDateFormatter
 * Helper to format notification timestamps into a unified Arabic format:
 * "الثلاثاء، 25 أغسطس 2026، 02:30 م"
 * with smart relative time support for recent events.
 */
object NotificationDateFormatter {

    private val fullDateFormat = SimpleDateFormat("EEEE، d MMMM yyyy، hh:mm a", Locale("ar")).apply {
        timeZone = TimeZone.getDefault()
    }

    private val shortTimeFormat = SimpleDateFormat("hh:mm a", Locale("ar")).apply {
        timeZone = TimeZone.getDefault()
    }

    fun format(timestamp: Long): String {
        if (timestamp <= 0L) return "الآن"
        val now = System.currentTimeMillis()
        val diffMillis = now - timestamp

        val seconds = diffMillis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        val date = Date(timestamp)

        return when {
            diffMillis < 0 -> fullDateFormat.format(date)
            minutes < 1 -> "منذ لحظات"
            minutes < 60 -> "منذ $minutes دقيقة"
            hours < 24 -> "منذ $hours ساعة (${shortTimeFormat.format(date)})"
            days == 1L -> "أمس في ${shortTimeFormat.format(date)}"
            days < 7 -> "منذ $days أيام (${shortTimeFormat.format(date)})"
            else -> fullDateFormat.format(date)
        }
    }

    fun formatFull(timestamp: Long): String {
        if (timestamp <= 0L) return "تاريخ غير محدد"
        return fullDateFormat.format(Date(timestamp))
    }
}
