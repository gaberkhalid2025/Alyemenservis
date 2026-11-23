package com.example.utils

/**
 * 📋 ApprovalValidator
 * مدقق بيانات اعتماد طلبات الانضمام (Document 74)
 * يمنع إنشاء أي كيان بإحداثيات صفرية أو بيانات ناقصة
 */
object ApprovalValidator {
    data class Problem(val field: String, val message: String)

    fun validate(lat: Double?, lng: Double?, cityId: String?, name: String?, phone: String?): List<Problem> = buildList {
        if (lat == null || lng == null || !LandmarkAddress.isValid(lat, lng)) {
            add(Problem("location", "الموقع الجغرافي غير محدد؛ حدّده من الخريطة قبل الاعتماد"))
        }
        if (cityId.isNullOrBlank()) {
            add(Problem("city", "المدينة/المحافظة غير محددة"))
        }
        if (name.isNullOrBlank()) {
            add(Problem("name", "الاسم مطلوب"))
        }
        if (phone.isNullOrBlank()) {
            add(Problem("phone", "الهاتف مطلوب"))
        }
    }
}
