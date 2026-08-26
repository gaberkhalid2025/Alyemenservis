package com.example.util

import android.content.Context
import androidx.annotation.Keep
import com.example.data.PaymentEntity
import com.example.data.repositories.PaymentRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Keep
data class Payment(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val amount: Double = 0.0,
    val method: String = "JEEB", // "JEEB", "ALKARIMI", "JAWALY", "YEMENCASH", "BANK"
    val currency: String = "YER",
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
    val status: String = "VERIFIED",
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
    val type: String,
    val iconUrl: String = "",
    val description: String = "",
    val minAmount: Double = 500.0,
    val maxAmount: Double = 5000000.0,
    val isActive: Boolean = true
)

/**
 * 💳 PaymentGatewayIntegration - تكامل بوابات الدفع الإلكتروني
 * 
 * الميزات:
 * 1. دعم المحافظ اليمنية (جيب، الكريمي حاسب، جوالي، يمن كاش، الحوالات).
 * 2. الاتصال بـ Firestore عبر `PaymentRepository` لحفظ السجل وتأكيد / إلغاء المعاملات.
 * 3. إتاحة التدفق اللحظي Flow لسجل العمليات.
 */
class PaymentGatewayIntegration(private val context: Context? = null) {

    private val repository = PaymentRepository()

    /**
     * معالجة وتنفيذ عملية الدفع في Firestore
     */
    suspend fun processPayment(payment: Payment): Result<PaymentResult> {
        return try {
            if (!validatePaymentMethod(payment.method)) {
                return Result.failure(IllegalArgumentException("طريقة الدفع غير مدعومة: ${payment.method}"))
            }

            val entity = PaymentEntity(
                id = payment.id.ifBlank { "PAY_${UUID.randomUUID().toString().take(8)}" },
                userId = payment.userId,
                amount = payment.amount,
                method = payment.method,
                currency = payment.currency,
                walletNumber = payment.walletNumber,
                accountHolderName = payment.accountName,
                transferId = payment.transferId,
                transferPhoto = payment.transferPhoto,
                createdAt = payment.timestamp
            )

            val result = repository.processPayment(entity)
            if (result.isSuccess) {
                Result.success(
                    PaymentResult(
                        success = true,
                        transactionId = entity.id,
                        message = "تمت معالجة الدفع عبر محفظة ${getPaymentMethodName(payment.method)} بنجاح",
                        timestamp = System.currentTimeMillis()
                    )
                )
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("فشلت عملية معالجة الدفع"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * جلب تدفق معاملات الدفع الخاصة بالمستخدم
     */
    fun getTransactionHistoryFlow(userId: String): Flow<List<PaymentEntity>> {
        return repository.getTransactionHistoryFlow(userId)
    }

    /**
     * تأكيد عملية الدفع
     */
    suspend fun confirmPayment(transactionId: String, note: String = ""): Result<PaymentConfirmation> {
        val result = repository.confirmPayment(transactionId, note)
        return if (result.isSuccess) {
            Result.success(
                PaymentConfirmation(
                    transactionId = transactionId,
                    isConfirmed = true,
                    confirmedBy = "SYSTEM_GATEWAY",
                    confirmationCode = "CONF-${(100000..999999).random()}",
                    confirmedAt = System.currentTimeMillis()
                )
            )
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("فشل تأكيد عملية الدفع"))
        }
    }

    /**
     * إلغاء عملية الدفع
     */
    suspend fun cancelPayment(transactionId: String, reason: String = ""): Result<Boolean> {
        return repository.cancelPayment(transactionId, reason)
    }

    fun validatePaymentMethod(method: String): Boolean {
        val validMethods = listOf("JEEB", "ALKARIMI", "JAWALY", "YEMENCASH", "BANK", "CASH")
        return validMethods.any { it.equals(method.trim(), ignoreCase = true) }
    }

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
