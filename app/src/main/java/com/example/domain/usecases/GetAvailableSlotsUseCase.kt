package com.example.domain.usecases

import com.example.data.BookingEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 🎯 GetAvailableSlotsUseCase
 * Calculates available time slots for a given date and provider,
 * excluding booked slots.
 */
class GetAvailableSlotsUseCase {

    data class TimeSlot(
        val timeLabel: String,
        val isAvailable: Boolean,
        val slotTimestamp: Long
    )

    private val defaultStandardSlots = listOf(
        "08:00 AM" to "08:00 ص",
        "09:00 AM" to "09:00 ص",
        "10:00 AM" to "10:00 ص",
        "11:00 AM" to "11:00 ص",
        "12:00 PM" to "12:00 م",
        "02:00 PM" to "02:00 م",
        "03:00 PM" to "03:00 م",
        "04:00 PM" to "04:00 م",
        "05:00 PM" to "05:00 م",
        "06:00 PM" to "06:00 م",
        "07:00 PM" to "07:00 م",
        "08:00 PM" to "08:00 م"
    )

    operator fun invoke(
        selectedDateString: String,
        providerId: String,
        existingBookings: List<BookingEntity>
    ): List<TimeSlot> {
        val cleanDate = selectedDateString.trim()
        val bookedTimesOnDate = existingBookings
            .filter { it.providerId == providerId && (it.date == cleanDate || it.dateString == cleanDate) && it.status in listOf("PENDING", "APPROVED") }
            .map { it.time.ifBlank { it.timeString }.trim() }
            .toSet()

        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdf.format(cal.time)
        val isToday = cleanDate == todayStr
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)

        return defaultStandardSlots.map { (enSlot, arSlot) ->
            // Extract hour for today filtering
            val slotHour24 = parseHour24(enSlot)
            val isPassedToday = isToday && (slotHour24 <= currentHour)
            val isBooked = bookedTimesOnDate.any { it.contains(arSlot) || it.contains(enSlot) }

            TimeSlot(
                timeLabel = arSlot,
                isAvailable = !isBooked && !isPassedToday,
                slotTimestamp = System.currentTimeMillis()
            )
        }
    }

    private fun parseHour24(slotEn: String): Int {
        val parts = slotEn.split(":")
        var hour = parts.firstOrNull()?.toIntOrNull() ?: 0
        if (slotEn.contains("PM") && hour < 12) hour += 12
        if (slotEn.contains("AM") && hour == 12) hour = 0
        return hour
    }
}
