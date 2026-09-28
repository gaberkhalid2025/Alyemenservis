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
    val success: Boolean = true,
    val transactionId: String? = null,
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Keep
data class PaymentVerification(
    val transactionId: String = "",
    val isValid: Boolean = true,
    val status: String = "VERIFIED", // "VERIFIED", "PENDING", "REJECTED"
    val amount: Double = 0.0,
    val verifiedAt: Long = System.currentTimeMillis()
)

@Keep
data class PaymentConfirmation(
    val transactionId: String = "",
    val isConfirmed: Boolean = true,
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
class PaymentGatewayIntegration(context: Context? = null) {

    private val isPaymentEnabledFromBuild: Boolean = BuildConfig.IS_PAYMENT_ENABLED
    private val activeTransactions = mutableMapOf<String, Payment>()

    /**
     * معالجة وتنفيذ عملية الدفع
     */
    fun processPayment(payment: Payment, settings: AdminSettingsEntity? = null): Result<PaymentResult> {
        return Result.failure(UnsupportedOperationException("Payment gateway not implemented"))
    }

    /**
     * التحقق من صحة عملية الدفع ورقم الحوالة
     */
    fun verifyPayment(transactionId: String, settings: AdminSettingsEntity? = null): Result<PaymentVerification> {
        val enabled = isPaymentEnabledFromBuild && (settings?.isPaymentEnabled == true)
        if (!enabled) {
            return Result.failure(
                UnsupportedOperationException("التحقق من الدفع غير مُفعّل حالياً.")
            )
        }

        return try {
            val payment = activeTransactions[transactionId]
            if (payment != null) {
                val verification = PaymentVerification(
                    transactionId = transactionId,
                    isValid = true,
                    status = "VERIFIED",
                    amount = payment.amount,
                    verifiedAt = System.currentTimeMillis()
                )
                Result.success(verification)
            } else {
                Result.failure(IllegalArgumentException("لم يتم العثور على المعاملة المالية أو أنها غير صالحة: $transactionId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * تأكيد استلام المبلغ وإصدار إيصال السداد
     */
    fun confirmPayment(transactionId: String, settings: AdminSettingsEntity? = null): Result<PaymentConfirmation> {
        return Result.failure(UnsupportedOperationException("Payment gateway not implemented"))
    }

    /**
     * إلغاء عملية الدفع
     */
    fun cancelPayment(transactionId: String, reason: String): Result<Boolean> {
        return try {
            activeTransactions.remove(transactionId)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * استرداد المبلغ
     */
    fun refundPayment(transactionId: String, amount: Double): Result<Boolean> {
        return Result.failure(UnsupportedOperationException("Payment gateway not implemented"))
    }

    /**
     * الحصول على سجل المعاملات لمستخدم معين
     */
    fun getTransactionHistory(userId: String): List<Transaction> {
        if (userId.isBlank()) return emptyList()
        return activeTransactions.values
            .filter { it.userId == userId }
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
            )
        )
    }

    private fun getPaymentMethodName(type: String): String {
        return when (type.uppercase()) {
            "JEEB" -> "محفظة جيب"
            "ALKARIMI" -> "الكريمي"
            "JAWALY" -> "جوالي"
            "YEMENCASH" -> "يمن كاش"
            "BANK" -> "حوالة بنكية"
            else -> "نقدي"
        }
    }
}
