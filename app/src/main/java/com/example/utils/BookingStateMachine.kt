package com.example.utils

import com.example.data.BookingEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BookingStatus(val label: String, val colorHex: String) {
    PENDING("قيد الانتظار", "#F59E0B"),
    UNDER_REVIEW("قيد المراجعة", "#FCD34D"),
    ACCEPTED("مقبول", "#10B981"),
    REJECTED("مرفوض", "#EF4444"),
    IN_PROGRESS("قيد التنفيذ", "#3B82F6"),
    COMPLETED("مكتمل", "#059669"),
    PAID("تم الدفع", "#8B5CF6"),
    CLOSED("مغلق ومؤرشف", "#6B7280"),
    CANCELLED("ملغي", "#EF4444")
}

/**
 * ⚙️ BookingStateMachine
 * محرك إدارة دورة حياة وحالات الحجز وضبط الانتقالات المسموحة وقواعد الأمان والإلغاء.
 */
object BookingStateMachine {

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
     * 1. التحقق من إمكانية الانتقال من الحالة الحالية للحالة الجديدة
     */
    fun canTransition(currentStatus: String, newStatus: String): Boolean {
        val curr = currentStatus.uppercase(Locale.ROOT)
        val target = newStatus.uppercase(Locale.ROOT)
        if (curr == target) return true
        val validNext = allowedTransitions[curr] ?: emptyList()
        return validNext.contains(target)
    }

    /**
     * 2. الحصول على قائمة الحالات المتاحة للانتقال إليها
     */
    fun getAvailableTransitions(currentStatus: String): List<String> {
        val curr = currentStatus.uppercase(Locale.ROOT)
        return allowedTransitions[curr] ?: emptyList()
    }

    /**
     * 3. الحصول على المسمى العربي للحالة
     */
    fun getStatusLabel(status: String): String {
        return try {
            BookingStatus.valueOf(status.uppercase(Locale.ROOT)).label
        } catch (e: Exception) {
            when (status.uppercase(Locale.ROOT)) {
                "APPROVED" -> "مقبول"
                "REJECTED" -> "مرفوض"
                else -> status
            }
        }
    }

    /**
     * 4. الحصول على كود اللون للحالة
     */
    fun getStatusColor(status: String): String {
        return try {
            BookingStatus.valueOf(status.uppercase(Locale.ROOT)).colorHex
        } catch (e: Exception) {
            "#F59E0B"
        }
    }

    /**
     * 5. هل الحالة نهائية لا تقبل التعديل
     */
    fun isTerminalStatus(status: String): Boolean {
        val s = status.uppercase(Locale.ROOT)
        return s == "CLOSED" || s == "CANCELLED" || s == "REJECTED"
    }

    /**
     * 6. التحقق من إمكانية الإلغاء (تطبيق قاعدة 8 ساعات وقفل المحاولات)
     */
    fun canCancel(booking: BookingEntity): Boolean {
        if (booking.isLocked) return false
        if (isTerminalStatus(booking.status)) return false
        if (booking.status.uppercase(Locale.ROOT) == "IN_PROGRESS" || booking.status.uppercase(Locale.ROOT) == "COMPLETED") {
            return false
        }

        // فحص قاعدة الـ 8 ساعات قبل موعد الحجز
        val appointmentTime = parseAppointmentTimestamp(booking.dateString.ifEmpty { booking.date }, booking.timeString.ifEmpty { booking.time })
        if (appointmentTime > 0) {
            val diffMs = appointmentTime - System.currentTimeMillis()
            val eightHoursMs = 8 * 60 * 60 * 1000L
            if (diffMs in 1..eightHoursMs) {
                // متبقي أقل من 8 ساعات على الموعد
                return false
            }
        }
        return true
    }

    /**
     * 7. الحصول على عدد محاولات الإلغاء
     */
    fun getCancelAttempts(booking: BookingEntity): Int {
        return booking.cancellationAttempts
    }

    /**
     * 8. استخراج توقيت الموعد
     */
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
}
