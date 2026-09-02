package com.example.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 🏖️ HolidayManager
 * إدارة العطلات الرسمية اليمنية والإجازات الأسبوعية والخاصة بمزودي الخدمة
 * لمنع الحجز في الأيام غير المتاحة وتنبيه المستخدم مسبقاً
 */
object HolidayManager {

    private val customProviderHolidays = mutableMapOf<String, MutableSet<String>>() // providerId -> Set of "yyyy-MM-dd"

    // Official Fixed & Common Yemeni Holidays
    private val fixedHolidays = mapOf(
        "05-01" to "عيد العمال العالمي 🛠️",
        "05-22" to "عيد الوحدة اليمنية 🇾🇪",
        "09-26" to "ذكرى ثورة 26 سبتمبر 🇾🇪",
        "10-14" to "ذكرى ثورة 14 أكتوبر 🇾🇪",
        "11-30" to "عيد الاستقلال 30 نوفمبر 🇾🇪",
        "01-01" to "رأس السنة الميلادية 🎆"
    )

    /**
     * التحقق مما إذا كان التاريخ عطلة رسمية أو يوم جمعة أو إجازة فني
     */
    fun isDateHoliday(dateString: String, providerId: String? = null): Pair<Boolean, String?> {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(dateString) ?: return Pair(false, null)
            val cal = Calendar.getInstance().apply { time = date }

            // 1. فحص يوم الجمعة (Friday)
            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
                return Pair(true, "يوم الجمعة (عطلة أسبوعية) 🕌")
            }

            // 2. فحص العطلات الرسمية الثابتة
            val monthDay = String.format(Locale.US, "%02d-%02d", cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            if (fixedHolidays.containsKey(monthDay)) {
                return Pair(true, fixedHolidays[monthDay])
            }

            // 3. فحص إجازات الفني الخاصة
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

    /**
     * إضافة يوم إجازة خاص بمزود خدمة
     */
    fun addProviderHoliday(providerId: String, dateString: String) {
        val set = customProviderHolidays.getOrPut(providerId) { mutableSetOf() }
        set.add(dateString)
    }

    /**
     * إزالة يوم إجازة خاص بمزود خدمة
     */
    fun removeProviderHoliday(providerId: String, dateString: String) {
        customProviderHolidays[providerId]?.remove(dateString)
    }

    /**
     * جلب جميع أيام إجازة المزود
     */
    fun getProviderHolidays(providerId: String): Set<String> {
        return customProviderHolidays[providerId] ?: emptySet()
    }
}
