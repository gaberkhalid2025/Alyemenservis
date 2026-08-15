package com.example.utils

import com.example.utils.*

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

    fun generateBookingPassword(length: Int = 4): String {
        val builder = StringBuilder()
        for (i in 0 until length) {
            builder.append(Random.nextInt(0, 10))
        }
        return builder.toString()
    }

    fun isBookingWithin6Hours(dateString: String, timeString: String): Boolean {
        return try {
            val cleanDate = dateString.trim().replace("/", "-")
            val cleanTime = timeString.trim().replace("م", "PM").replace("ص", "AM")
            val combined = "$cleanDate $cleanTime"
            val formats = listOf(
                SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US),
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US),
                SimpleDateFormat("yyyy-MM-dd", Locale.US)
            )
            var parsedDate: Date? = null
            for (fmt in formats) {
                try {
                    parsedDate = fmt.parse(combined) ?: fmt.parse(cleanDate)
                    if (parsedDate != null) break
                } catch (e: Exception) {}
            }
            if (parsedDate != null) {
                val diffMs = parsedDate.time - System.currentTimeMillis()
                val sixHoursMs = 6 * 60 * 60 * 1000L
                diffMs in 0..sixHoursMs
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
