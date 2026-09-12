package com.example.data.models

import androidx.annotation.Keep

@Keep
enum class Currency(val code: String, val symbolArabic: String, val nameArabic: String) {
    YER("YER", "ر.ي", "ريال يمني"),
    SAR("SAR", "ر.س", "ريال سعودي"),
    USD("USD", "$", "دولار أمريكي")
}

@Keep
enum class AdminRole(val code: String, val titleArabic: String) {
    SUPER_ADMIN("SUPER_ADMIN", "مدير النظام الشامل"),
    ADMIN("ADMIN", "مدير للنظام"),
    AUDITOR("AUDITOR", "مراقب مالي"),
    SUPPORT("SUPPORT", "الدعم الفني"),
    OPERATIONS("OPERATIONS", "إدارة العمليات")
}

@Keep
enum class PaymentStatus(val code: String, val labelArabic: String) {
    PENDING("PENDING", "قيد الانتظار"),
    PROCESSING("PROCESSING", "قيد المعالجة"),
    COMPLETED("COMPLETED", "مكتملة"),
    FAILED("FAILED", "فاشلة"),
    REFUNDED("REFUNDED", "مسترجعة"),
    CANCELLED("CANCELLED", "ملغاة"),
    DISPUTED("DISPUTED", "قيد النزاع")
}

@Keep
enum class NotificationType(val code: String, val titleArabic: String) {
    NORMAL("NORMAL", "إشعار عام"),
    BOOKING("BOOKING", "إشعار حجز"),
    MESSAGE("MESSAGE", "رسالة جديدة"),
    SYSTEM("SYSTEM", "إشعار نظام"),
    ADMIN("ADMIN", "إشعار إدارة"),
    REGISTRATION_APPROVED("REGISTRATION_APPROVED", "قبول التسجيل"),
    SPECIAL_OFFER("SPECIAL_OFFER", "عرض خاص"),
    JOIN_REQUEST("JOIN_REQUEST", "طلب انضمام"),
    JOIN_APPROVED("JOIN_APPROVED", "قبول انضمام"),
    JOIN_REJECTED("JOIN_REJECTED", "رفض انضمام")
}

@Keep
enum class JoinRequestStatus(val code: String, val labelArabic: String) {
    PENDING("PENDING", "قيد المراجعة"),
    APPROVED("APPROVED", "مقبول"),
    REJECTED("REJECTED", "مرفوض"),
    ACTIVE("ACTIVE", "نشط")
}

@Keep
enum class InstantRequestStatus(val code: String, val labelArabic: String) {
    WAITING_FOR_OFFERS("WAITING_FOR_OFFERS", "بانتظار العروض"),
    REVIEWING_OFFERS("REVIEWING_OFFERS", "مراجعة العروض"),
    ACCEPTED("ACCEPTED", "تم قبول العرض"),
    IN_PROGRESS("IN_PROGRESS", "قيد التنفيذ"),
    COMPLETED("COMPLETED", "مكتمل"),
    EXPIRED("EXPIRED", "منتهي الصلاحية"),
    CANCELLED("CANCELLED", "ملغى")
}
