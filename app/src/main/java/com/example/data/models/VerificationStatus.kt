package com.example.data

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color

/**
 * 🔒 Verification Status Enum
 * يحدد حالات توثيق المتقدمين والعملاء والمنشآت في نظام دليل خدمات اليمن
 */
@Keep
enum class VerificationStatus(
    val code: String,
    val labelAr: String,
    val descriptionAr: String,
    val colorHex: Long
) {
    PENDING(
        code = "PENDING",
        labelAr = "قيد الانتظار",
        descriptionAr = "حسابك قيد المراجعة، سيتم تفعيله بعد موافقة الإدارة.",
        colorHex = 0xFFF59E0B // أصفر / برتقالي
    ),
    UNDER_REVIEW(
        code = "UNDER_REVIEW",
        labelAr = "قيد التوثيق والمراجعة",
        descriptionAr = "جاري التحقق من المستندات والبيانات المقدمة من قبل فريق الدعم.",
        colorHex = 0xFF3B82F6 // أزرق
    ),
    VERIFIED(
        code = "VERIFIED",
        labelAr = "موثق ومفعل",
        descriptionAr = "تمت الموافقة وتوثيق الحساب بنجاح. يمكنك استخدام كامل الصلاحيات.",
        colorHex = 0xFF10B981 // أخضر
    ),
    REJECTED(
        code = "REJECTED",
        labelAr = "مرفوض",
        descriptionAr = "تم رفض الطلب. يرجى مراجعة سبب الرفض وإعادة استكمال البيانات.",
        colorHex = 0xFFEF4444 // أحمر
    ),
    SUSPENDED(
        code = "SUSPENDED",
        labelAr = "موقوف مؤقتاً",
        descriptionAr = "تم إيقاف الحساب مؤقتاً بواسطة الإدارة. يرجى التواصل مع الدعم الفني.",
        colorHex = 0xFF6B7280 // رمادي
    );

    companion object {
        fun fromCode(code: String?): VerificationStatus {
            if (code.isNullOrEmpty()) return PENDING
            return values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: PENDING
        }
    }
}
