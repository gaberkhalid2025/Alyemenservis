package com.example.util

import com.example.data.BookingEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 📅 BookingManager - المدير الموحد للحجوزات والجداول الزمنية والعطلات
 * 
 * الميزات:
 * 1. توحيد وإدارة آلة حالة الحجز (BookingStateMachine) والتحقق من سلامة الانتقالات والإلغاء.
 * 2. توليد وفحص الفترات الزمنية المتاحة (ScheduleManager) للخدمات والمحلات والفنيين.
 * 3. إدارة العطلات الرسمية اليمنية والإجازات الأسبوعية والخاصة (HolidayManager).
 * 4. توليد التواريخ والتكرارات للحجوزات الدورية (أسبوعياً أو شهرياً).
 */
class BookingManager {

    // ==========================================
    // 1. ⚙️ Booking State Machine Rules
    // ==========================================
    private val allowedTransitions = mapOf(
        "PENDING" to listOf("UNDER_REVIEW", "ACCEPTED", "REJECTED", "CANCELLED"),
        "UNDER_REVIEW" to listOf("ACCEPTED", "REJECTED", "CANCELLED", "PENDING"),
        "ACCEPTED" to listOf("IN_PROGRESS", "CANCELLED"),
        "IN_PROGRESS" to listOf("COMPLETED", "CANCELLED"),
        "COMPLETED" to listOf("PAID", "CLOSED"),
        "PAID" to listOf("CLOSED"),
        "CLOSED" to emptyList(),
        "CANCELLED" to emptyList(),
        "REJECTED" to emptyList()
    )

    /**
     * التحقق من إمكانية الانتقال من الحالة الحالية إلى الحالة الجديدة
     */
    fun canTransition(currentStatus: String, newStatus: String): Boolean {
        val curr = currentStatus.uppercase(Locale.ROOT)
        val target = newStatus.uppercase(Locale.ROOT)
        if (curr == target) return true
        val validNext = allowedTransitions[curr] ?: emptyList()
        return validNext.contains(target)
    }

    /**
     * الحصول على قائمة الانتقالات المتاحة للحالة الحالية
     */
    fun getAvailableTransitions(currentStatus: String): List<String> {
        val curr = currentStatus.uppercase(Locale.ROOT)
        return allowedTransitions[curr] ?: emptyList()
    }

    /**
     * هل الحالة نهائية لا تقبل التعديل
     */
    fun isTerminalStatus(status: String): Boolean {
        val s = status.uppercase(Locale.ROOT)
        return s == "CLOSED" || s == "CANCELLED" || s == "REJECTED"
    }

    /**
     * فحص إمكانية إلغاء الحجز (قاعدة 8 ساعات وتوفر الشروط)
     */
    fun canCancel(booking: BookingEntity): Boolean {
        if (booking.isLocked) return false
        if (isTerminalStatus(booking.status)) return false
        if (booking.status.uppercase(Locale.ROOT) == "IN_PROGRESS" || booking.status.uppercase(Locale.ROOT) == "COMPLETED") {
            return false
        }

        val appointmentTime = parseAppointmentTimestamp(
            booking.dateString.ifEmpty { booking.date },
            booking.timeString.ifEmpty { booking.time }
        )
        if (appointmentTime > 0) {
            val diffMs = appointmentTime - System.currentTimeMillis()
            val eightHoursMs = 8 * 60 * 60 * 1000L
            if (diffMs in 1..eightHoursMs) {
                return false
            }
        }
        return true
    }

    private fun parseAppointmentTimestamp(dateStr: String, timeStr: String): Long {
        if (dateStr.isBlank()) return 0L
        return try {
            val formats = listOf("yyyy-MM-dd HH:mm", "yyyy/MM/dd HH:mm", "dd/MM/yyyy HH:mm", "yyyy-MM-dd")
            val fullStr = "$dateStr ${timeStr.ifBlank { "00:00" }}".trim()
            for (fmt in formats) {
                try {
                    val sdf = SimpleDateFormat(fmt, Locale.US)
                    val d = sdf.parse(fullStr)
                    if (d != null) return d.time
                } catch (ignored: Exception) {}
            }
            0L
        } catch (e: Exception) {
            0L
        }
    }

    // ==========================================
    // 2. 📅 Schedule & Slots Management
    // ==========================================

    data class TimeSlot(
        val timeString: String,
        val hour24: Int,
        val isAvailable: Boolean,
        val reasonIfNotAvailable: String? = null
    )

    data class ProviderWorkingHours(
        val startHour: Int = 8,
        val endHour: Int = 20,
        val breakStartHour: Int = 13,
        val breakEndHour: Int = 14,
        val slotDurationHours: Int = 1
    )

    private val providerCustomHours = mutableMapOf<String, ProviderWorkingHours>()

    fun setProviderWorkingHours(providerId: String, hours: ProviderWorkingHours) {
        providerCustomHours[providerId] = hours
    }

    fun getProviderWorkingHours(providerId: String): ProviderWorkingHours {
        return providerCustomHours[providerId] ?: ProviderWorkingHours()
    }

    /**
     * الحصول على الفترات الزمنية المتاحة في تاريخ محدد
     */
    fun getAvailableSlots(
        dateString: String,
        providerId: String,
        existingBookings: List<BookingEntity> = emptyList()
    ): List<TimeSlot> {
        val (isHoliday, holidayName) = isHoliday(dateString, providerId)
        if (isHoliday) return emptyList()

        val hours = getProviderWorkingHours(providerId)
        val slots = mutableListOf<TimeSlot>()

        val bookedTimes = existingBookings.filter {
            it.providerId == providerId &&
            (it.date == dateString || it.dateString == dateString) &&
            it.status != "CANCELLED" && it.status != "REJECTED"
        }.map { (if (it.time.isNotBlank()) it.time else it.timeString).trim() }.toSet()

        for (h in hours.startHour until hours.endHour step hours.slotDurationHours) {
            val isBreak = h >= hours.breakStartHour && h < hours.breakEndHour
            val formattedTime = formatHourToArabicString(h)
            val isBooked = bookedTimes.any { it.contains(formattedTime) || formattedTime.contains(it) }

            val available = !isBreak && !isBooked
            val reason = when {
                isBreak -> "استراحة وصلاة 🕌"
                isBooked -> "محجوز مسبقاً 🔒"
                else -> null
            }

            slots.add(
                TimeSlot(
                    timeString = formattedTime,
                    hour24 = h,
                    isAvailable = available,
                    reasonIfNotAvailable = reason
                )
            )
        }
        return slots
    }

    /**
     * توليد التواريخ للحجوزات الدورية
     */
    fun generateRecurringDates(
        startDateString: String,
        rule: String, // "WEEKLY", "MONTHLY", "NONE"
        occurrences: Int = 4
    ): List<String> {
        if (rule == "NONE" || rule.isBlank()) return listOf(startDateString)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val result = mutableListOf<String>()

        try {
            val date = sdf.parse(startDateString) ?: return listOf(startDateString)
            val cal = Calendar.getInstance().apply { time = date }

            for (i in 0 until occurrences) {
                result.add(sdf.format(cal.time))
                if (rule.equals("WEEKLY", ignoreCase = true)) {
                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                } else if (rule.equals("MONTHLY", ignoreCase = true)) {
                    cal.add(Calendar.MONTH, 1)
                }
            }
        } catch (e: Exception) {
            result.add(startDateString)
        }
        return result
    }

    private fun formatHourToArabicString(hour24: Int): String {
        val period = if (hour24 < 12) "ص" else "م"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.US, "%02d:00 %s", hour12, period)
    }

    // ==========================================
    // 3. 🏖️ Holidays & Vacations Management
    // ==========================================

    private val customProviderHolidays = mutableMapOf<String, MutableSet<String>>()
    private val fixedHolidays = mapOf(
        "05-01" to "عيد العمال العالمي 🛠️",
        "05-22" to "عيد الوحدة اليمنية 🇾🇪",
        "09-26" to "ذكرى ثورة 26 سبتمبر 🇾🇪",
        "10-14" to "ذكرى ثورة 14 أكتوبر 🇾🇪",
        "11-30" to "عيد الاستقلال 30 نوفمبر 🇾🇪",
        "01-01" to "رأس السنة الميلادية 🎆"
    )

    /**
     * فحص هل تاريخ معين عطلة أسبوعية أو رسمية أو إجازة شخصية
     */
    fun isHoliday(dateString: String, providerId: String? = null): Pair<Boolean, String?> {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(dateString) ?: return Pair(false, null)
            val cal = Calendar.getInstance().apply { time = date }

            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
                return Pair(true, "يوم الجمعة (عطلة أسبوعية) 🕌")
            }

            val monthDay = String.format(Locale.US, "%02d-%02d", cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            if (fixedHolidays.containsKey(monthDay)) {
                return Pair(true, fixedHolidays[monthDay])
            }

            if (providerId != null) {
                val providerDays = customProviderHolidays[providerId]
                if (providerDays?.contains(dateString) == true) {
                    return Pair(true, "إجازة شخصية لمقدم الخدمة 🏖️")
                }
            }
            return Pair(false, null)
        } catch (e: Exception) {
            return Pair(false, null)
        }
    }

    fun addProviderHoliday(providerId: String, dateString: String) {
        customProviderHolidays.getOrPut(providerId) { mutableSetOf() }.add(dateString)
    }

    fun removeProviderHoliday(providerId: String, dateString: String) {
        customProviderHolidays[providerId]?.remove(dateString)
    }

    fun getProviderHolidays(providerId: String): Set<String> {
        return customProviderHolidays[providerId] ?: emptySet()
    }
}
