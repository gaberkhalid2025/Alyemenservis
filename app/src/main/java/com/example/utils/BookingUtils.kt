package com.example.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

object BookingUtils {
    fun generateBookingNumber(prefix: String = "BK"): String {
        val sdf = SimpleDateFormat("yyMMddHHmmss", Locale.US)
        val datePart = sdf.format(Date())
        val randomPart = String.format(Locale.US, "%04d", Random.nextInt(1000, 9999))
        return "$prefix-$datePart-$randomPart"
    }

    fun generateBookingCode(prefix: String = "BK"): String {
        return generateBookingNumber(prefix)
    }

    fun generateBookingPassword(length: Int = 4): String {
        val builder = StringBuilder()
        for (i in 0 until length) {
            builder.append(Random.nextInt(0, 10))
        }
        return builder.toString()
    }

    /**
     * Parse date and time strings to Unix timestamp (millis).
     */
    fun parseScheduledTimestamp(dateString: String, timeString: String): Long {
        return try {
            val cleanDate = dateString.trim().replace("/", "-")
            val cleanTime = timeString.trim().replace("م", "PM").replace("ص", "AM")
            val combined = "$cleanDate $cleanTime"
            val formats = listOf(
                SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US),
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US),
                SimpleDateFormat("yyyy-MM-dd", Locale.US)
            )
            for (fmt in formats) {
                try {
                    val parsed = fmt.parse(combined) ?: fmt.parse(cleanDate)
                    if (parsed != null) return parsed.time
                } catch (e: Exception) {}
            }
            0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Checks if a booking can be modified or cancelled.
     * Rule: Allowed ONLY if there are MORE than 8 hours remaining before scheduledAt.
     */
    fun canModifyOrCancelBooking(scheduledAtTimestamp: Long, dateString: String = "", timeString: String = ""): Boolean {
        val scheduledMs = if (scheduledAtTimestamp > 0) scheduledAtTimestamp else parseScheduledTimestamp(dateString, timeString)
        if (scheduledMs <= 0) return true // If no valid date/time, allow by default
        val now = System.currentTimeMillis()
        val diffMs = scheduledMs - now
        val eightHoursMs = 8 * 60 * 60 * 1000L
        return diffMs > eightHoursMs
    }

    /**
     * Returns the remaining time in millis until the 8-hour cancellation deadline.
     * Deadline = scheduledAt - 8 hours.
     */
    fun getRemainingCancellationWindowMs(scheduledAtTimestamp: Long, dateString: String = "", timeString: String = ""): Long {
        val scheduledMs = if (scheduledAtTimestamp > 0) scheduledAtTimestamp else parseScheduledTimestamp(dateString, timeString)
        if (scheduledMs <= 0) return Long.MAX_VALUE
        val eightHoursMs = 8 * 60 * 60 * 1000L
        val cancellationDeadline = scheduledMs - eightHoursMs
        val diff = cancellationDeadline - System.currentTimeMillis()
        return if (diff > 0) diff else 0L
    }

    /**
     * Formats remaining cancellation window to user-friendly Arabic text.
     */
    fun formatRemainingCancellationTime(scheduledAtTimestamp: Long, dateString: String = "", timeString: String = ""): String {
        val remainingMs = getRemainingCancellationWindowMs(scheduledAtTimestamp, dateString, timeString)
        if (remainingMs == Long.MAX_VALUE) return "متاح للإلغاء والتعديل"
        if (remainingMs <= 0) return "انتهت فترة السماح بالتعديل/الإلغاء (أقل من 8 ساعات)"

        val totalSeconds = remainingMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            "باقي $hours ساعة و $minutes دقيقة على انتهاء مهلة التعديل/الإلغاء"
        } else {
            "باقي $minutes دقيقة و $seconds ثانية على إغلاق التعديل"
        }
    }

    /**
     * Backwards compatibility helper for 6 hours check.
     */
    fun isBookingWithin6Hours(dateString: String, timeString: String): Boolean {
        val scheduledMs = parseScheduledTimestamp(dateString, timeString)
        if (scheduledMs <= 0) return false
        val diffMs = scheduledMs - System.currentTimeMillis()
        val sixHoursMs = 6 * 60 * 60 * 1000L
        return diffMs in 0..sixHoursMs
    }
}
