package com.example.utils

import android.content.Context
import androidx.annotation.Keep
import java.util.UUID
import com.example.BuildConfig
import com.example.data.AdminSettingsEntity

@Keep
data class Payment(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val amount: Double = 0.0,
    val method: String = "JEEB", // "JEEB", "ALKARIMI", "JAWALY", "YEMENCASH", "BANK"
    val currency: String = "YER", // "YER", "USD", "SAR"
    val walletNumber: String = "",
    val accountName: String = "",
    val transferId: String = "",
    val transferPhoto: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Keep
data class PaymentResult(
    val success: Boolean = false,
    val transactionId: String? = null,
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Keep
data class PaymentVerification(
    val transactionId: String = "",
    val isValid: Boolean = false,
    val status: String = "PENDING", // "VERIFIED", "PENDING", "REJECTED"
    val amount: Double = 0.0,
    val verifiedAt: Long = System.currentTimeMillis()
)

@Keep
data class PaymentConfirmation(
    val transactionId: String = "",
    val isConfirmed: Boolean = false,
    val confirmedBy: String = "SYSTEM",
    val confirmationCode: String = "",
    val confirmedAt: Long = System.currentTimeMillis()
)

@Keep
data class PaymentMethod(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val type: String, // "JEEB", "ALKARIMI", "JAWALY", "YEMENCASH", "BANK"
    val iconUrl: String = "",
    val description: String = "",
    val minAmount: Double = 500.0,
    val maxAmount: Double = 5000000.0,
    val isActive: Boolean = true
)

/**
 * ⚠️ ملاحظة مهمة:
 * هذه الفئة محاكاة داخلية — لا تتصل ببوابة دفع حقيقية.
 * 
 * 🎯 حالة التفعيل الحالية:
 * - افتراضياً: معطّل (isPaymentEnabled = false).
 * - يمكن للأدمن تفعيله من لوحة التحكم بعد:
 *   1. الاتفاق مع بوابة دفع يمنية (الكريمي / جوّال باي / فلوسك).
 *   2. الحصول على API keys.
 *   3. تخزينها في Firebase Secrets.
 *   4. تعديل processPayment لاستدعاء API حقيقي.
 *   5. تعديل verifyPayment للتحقق من Firestore.
 *   6. تغيير BuildConfig.IS_PAYMENT_ENABLED = true.
 *   7. تفعيل المفتاح من لوحة التحكم.
 * 
 * ⚠️ لا تفعّل المفتاح قبل ربط بوابة حقيقية.
 */
class PaymentGatewayIntegration(@Suppress("UNUSED_PARAMETER") context: Context? = null) {

    private val isPaymentEnabledFromBuild: Boolean = BuildConfig.IS_PAYMENT_ENABLED
    private val activeTransactions = java.util.concurrent.ConcurrentHashMap<String, Payment>()

    /**
     * معالجة وتنفيذ عملية الدفع
     */
    fun processPayment(payment: Payment, settings: AdminSettingsEntity? = null): Result<PaymentResult> {
        if (!payment.amount.isFinite() || payment.amount <= 0.0) {
            return Result.failure(IllegalArgumentException("مبلغ الدفع يجب أن يكون رقماً صالحاً أكبر من الصفر."))
        }
        if (payment.method.isBlank() || !validatePaymentMethod(payment.method)) {
            return Result.failure(IllegalArgumentException("وسيلة الدفع غير صالحة أو غير مدعومة: ${payment.method}"))
        }
        val matchedMethod = getAvailablePaymentMethods().find { it.id.equals(payment.method.trim(), ignoreCase = true) }
        if (matchedMethod != null) {
            if (!matchedMethod.isActive) {
                return Result.failure(IllegalStateException("وسيلة الدفع المختارة متوقفة مؤقتاً: ${matchedMethod.nameAr}"))
            }
            if (payment.amount < matchedMethod.minAmount || payment.amount > matchedMethod.maxAmount) {
                return Result.failure(
                    IllegalArgumentException("المبلغ خارج النطاق المسموح لوسيلة الدفع (${matchedMethod.minAmount} - ${matchedMethod.maxAmount}).")
                )
            }
        } else if (payment.amount > 10_000_000.0) {
            return Result.failure(IllegalArgumentException("المبلغ يتجاوز الحد الأقصى المسموح به للعملية الواحدة."))
        }
        val enabled = isPaymentEnabledFromBuild && (settings?.isPaymentEnabled == true)
        if (!enabled) {
            return Result.failure(UnsupportedOperationException("Payment gateway not implemented"))
        }
        return Result.failure(UnsupportedOperationException("Payment gateway not implemented"))
    }

    /**
     * التحقق من صحة عملية الدفع ورقم الحوالة
     */
    fun verifyPayment(transactionId: String, settings: AdminSettingsEntity? = null): Result<PaymentVerification> {
        val cleanTxId = transactionId.trim()
        if (cleanTxId.isBlank()) {
            return Result.failure(IllegalArgumentException("رقم المعاملة المالية مطلوب."))
        }
        val enabled = isPaymentEnabledFromBuild && (settings?.isPaymentEnabled == true)
        if (!enabled) {
            return Result.failure(
                UnsupportedOperationException("التحقق من الدفع غير مُفعّل حالياً.")
            )
        }

        return try {
            val payment = activeTransactions[cleanTxId]
            if (payment != null && payment.amount.isFinite() && payment.amount > 0.0) {
                val verification = PaymentVerification(
                    transactionId = cleanTxId,
                    isValid = true,
                    status = "VERIFIED",
                    amount = payment.amount,
                    verifiedAt = System.currentTimeMillis()
                )
                Result.success(verification)
            } else {
                Result.failure(IllegalArgumentException("لم يتم العثور على المعاملة المالية أو أنها غير صالحة: $cleanTxId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * تأكيد استلام المبلغ وإصدار إيصال السداد
     */
    fun confirmPayment(transactionId: String, settings: AdminSettingsEntity? = null): Result<PaymentConfirmation> {
        if (transactionId.isBlank()) {
            return Result.failure(IllegalArgumentException("رقم المعاملة المالية مطلوب."))
        }
        val enabled = isPaymentEnabledFromBuild && (settings?.isPaymentEnabled == true)
        if (!enabled) {
            return Result.failure(UnsupportedOperationException("Payment gateway not implemented"))
        }
        return Result.failure(UnsupportedOperationException("Payment gateway not implemented"))
    }

    /**
     * إلغاء عملية الدفع
     */
    fun cancelPayment(transactionId: String, reason: String): Result<Boolean> {
        val cleanTxId = transactionId.trim()
        if (cleanTxId.isBlank()) {
            return Result.failure(IllegalArgumentException("رقم المعاملة المالية مطلوب."))
        }
        return try {
            activeTransactions.remove(cleanTxId)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * استرداد المبلغ
     */
    fun refundPayment(transactionId: String, amount: Double): Result<Boolean> {
        if (transactionId.isBlank() || !amount.isFinite() || amount <= 0.0) {
            return Result.failure(IllegalArgumentException("بيانات الاسترداد غير صالحة."))
        }
        return Result.failure(UnsupportedOperationException("Payment gateway not implemented"))
    }

    /**
     * الحصول على سجل المعاملات لمستخدم معين
     */
    fun getTransactionHistory(userId: String): List<Transaction> {
        val cleanUserId = userId.trim()
        if (cleanUserId.isBlank()) return emptyList()
        return activeTransactions.values
            .filter { it.userId == cleanUserId }
            .sortedByDescending { it.timestamp }
            .map { p ->
                Transaction(
                    id = p.id,
                    walletId = "wallet_${p.userId}",
                    type = "PAYMENT",
                    amount = p.amount,
                    balanceAfter = 0.0,
                    note = "دفع عبر ${getPaymentMethodName(p.method)} - إشعار: ${p.transferId}",
                    timestamp = p.timestamp,
                    status = "COMPLETED"
                )
            }
    }

    /**
     * التحقق من نوع وسيلة الدفع
     */
    fun validatePaymentMethod(method: String): Boolean {
        if (method.isBlank()) return false
        val validMethods = listOf("JEEB", "ALKARIMI", "JAWALY", "YEMENCASH", "BANK", "CASH")
        return validMethods.any { it.equals(method.trim(), ignoreCase = true) }
    }

    /**
     * قائمة المحافظ وطرق الدفع اليمنية المتاحة
     */
    fun getAvailablePaymentMethods(): List<PaymentMethod> {
        return listOf(
            PaymentMethod(
                id = "JEEB",
                nameAr = "محفظة جيب (بنك الكريمي)",
                nameEn = "Jeeb Wallet",
                type = "JEEB",
                description = "دفع فوري مباشر عبر حساب محفظة جيب الإلكترونية",
                minAmount = 500.0,
                maxAmount = 2000000.0
            ),
            PaymentMethod(
                id = "ALKARIMI",
                nameAr = "الكريمي إكسبرس / حاسب",
                nameEn = "AlKarimi Express",
                type = "ALKARIMI",
                description = "سداد عبر خدمة حاسب أو إرسال حوالة كريمي إكسبرس",
                minAmount = 1000.0,
                maxAmount = 5000000.0
            ),
            PaymentMethod(
                id = "JAWALY",
                nameAr = "محفظة جوالي (بنك اليمن والكويت)",
                nameEn = "Jawaly Wallet",
                type = "JAWALY",
                description = "دفع آمن وسريع عبر محفظة جوالي",
                minAmount = 500.0,
                maxAmount = 1500000.0
            ),
            PaymentMethod(
                id = "YEMENCASH",
                nameAr = "يمن كاش (Yemen Cash)",
                nameEn = "Yemen Cash",
                type = "YEMENCASH",
                description = "دفع عبر شبكة يمن كاش للمدفوعات الرقمية",
                minAmount = 500.0,
                maxAmount = 1000000.0
            ),
            PaymentMethod(
                id = "BANK",
                nameAr = "حوالة بنكية مباشرة",
                nameEn = "Bank Transfer",
                type = "BANK",
                description = "تحويل مصرفي مباشر عبر البنوك اليمنية المعتمدة",
                minAmount = 5000.0,
                maxAmount = 10000000.0
            ),
            PaymentMethod(
                id = "CASH",
                nameAr = "دفع نقدي مباشر",
                nameEn = "Cash Payment",
                type = "CASH",
                description = "دفع نقدي مباشر عند إنجاز الخدمة أو التسليم",
                minAmount = 100.0,
                maxAmount = 5000000.0
            )
        )
    }

    private fun getPaymentMethodName(type: String): String {
        return when (type.trim().uppercase(java.util.Locale.ROOT)) {
            "JEEB" -> "محفظة جيب"
            "ALKARIMI" -> "الكريمي"
            "JAWALY" -> "جوالي"
            "YEMENCASH" -> "يمن كاش"
            "BANK" -> "حوالة بنكية"
            "CASH" -> "نقدي"
            else -> "غير محدد"
        }
    }
}
