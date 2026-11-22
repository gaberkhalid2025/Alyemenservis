package com.example.utils

import androidx.annotation.Keep
import com.example.data.models.Transaction
import com.example.data.models.TransactionStatus
import com.example.data.models.TransactionType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.Serializable

@Keep
data class WalletTopUpRequest(
    val id: String = "",
    val walletId: String = "",
    val userId: String = "",
    val amount: Long = 0L,
    val currency: String = "YER",
    val referenceNumber: String = "",
    val paymentMethod: String = "BANK_TRANSFER",
    val note: String = "",
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val createdAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long = 0L,
    val reviewerId: String = "",
    val rejectionReason: String = ""
) : Serializable

/**
 * 🛡️ WalletRequests
 * إدارة طلبات شحن المحفظة الآمنة (PENDING) واعتمادها ذرياً عبر الإدارة دون تعديل رصيد من جهاز العميل.
 */
object WalletRequests {

    const val COLLECTION_TOPUPS = "wallet_topups"

    suspend fun submitTopUpRequest(
        firestore: FirebaseFirestore,
        walletId: String,
        userId: String,
        amount: Long,
        referenceNumber: String,
        currency: String = "YER",
        paymentMethod: String = "BANK_TRANSFER",
        note: String = ""
    ): Result<WalletTopUpRequest> = withContext(Dispatchers.IO) {
        try {
            val cleanWalletId = walletId.trim()
            val cleanUserId = userId.trim().ifBlank { cleanWalletId.removePrefix("wallet_") }
            val cleanRef = referenceNumber.trim()
            if (cleanWalletId.isBlank() || cleanUserId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("معرف المحفظة غير صالح"))
            }
            if (amount <= 0L) {
                return@withContext Result.failure(IllegalArgumentException("مبلغ الشحن يجب أن يكون أكبر من الصفر"))
            }
            if (cleanRef.length < 3) {
                return@withContext Result.failure(IllegalArgumentException("يرجى إدخال رقم مرجع أو حوالة صحيح"))
            }

            val req = WalletTopUpRequest(
                id = EntityIdGenerator.generate("TOPUP"),
                walletId = cleanWalletId,
                userId = cleanUserId,
                amount = amount,
                currency = currency.trim().uppercase().ifBlank { "YER" },
                referenceNumber = cleanRef,
                paymentMethod = paymentMethod.trim().ifBlank { "BANK_TRANSFER" },
                note = note.trim().ifBlank { "طلب شحن رصيد - مرجع: $cleanRef" },
                status = "PENDING",
                createdAt = System.currentTimeMillis()
            )

            firestore.collection(COLLECTION_TOPUPS)
                .document(req.id)
                .set(req)
                .await()

            Result.success(req)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approveTopUpRequest(
        firestore: FirebaseFirestore,
        requestId: String,
        reviewerId: String
    ): Result<Transaction> = withContext(Dispatchers.IO) {
        try {
            val reqRef = firestore.collection(COLLECTION_TOPUPS).document(requestId)
            val txResult = firestore.runTransaction { transaction ->
                val reqSnap = transaction.get(reqRef)
                val req = reqSnap.toObject(WalletTopUpRequest::class.java)
                    ?: throw IllegalStateException("طلب الشحن غير موجود")

                if (req.status.uppercase() != "PENDING") {
                    throw IllegalStateException("تمت معالجة طلب الشحن مسبقاً (${req.status})")
                }
                if (req.amount <= 0L) {
                    throw IllegalArgumentException("مبلغ الطلب غير صالح")
                }

                val walletRef = firestore.collection("wallets").document(req.walletId)
                val walletSnap = transaction.get(walletRef)
                val currentWallet = if (walletSnap.exists()) {
                    walletSnap.toObject(Wallet::class.java)
                        ?: Wallet(id = req.walletId, userId = req.userId)
                } else {
                    Wallet(id = req.walletId, userId = req.userId)
                }

                if (currentWallet.status == "FROZEN") {
                    throw IllegalStateException("المحفظة مجمدة ولا يمكن شحنها")
                }

                val cur = req.currency.uppercase()
                val amountDouble = req.amount.toDouble()
                val newYer = if (cur == "YER") currentWallet.balanceYer + amountDouble else currentWallet.balanceYer
                val newUsd = if (cur == "USD") currentWallet.balanceUsd + amountDouble else currentWallet.balanceUsd
                val newSar = if (cur == "SAR") currentWallet.balanceSar + amountDouble else currentWallet.balanceSar
                val now = System.currentTimeMillis()

                val updatedWallet = currentWallet.copy(
                    balanceYer = newYer,
                    balanceUsd = newUsd,
                    balanceSar = newSar,
                    balance = newYer,
                    updatedAt = now
                )
                transaction.set(walletRef, updatedWallet)

                transaction.update(
                    reqRef,
                    mapOf(
                        "status" to "APPROVED",
                        "reviewedAt" to now,
                        "reviewerId" to reviewerId
                    )
                )

                val tx = Transaction(
                    id = EntityIdGenerator.generate("TXN"),
                    walletId = updatedWallet.id,
                    userId = updatedWallet.userId,
                    type = TransactionType.DEPOSIT.name,
                    amount = amountDouble,
                    balanceAfter = when (cur) {
                        "USD" -> newUsd
                        "SAR" -> newSar
                        else -> newYer
                    },
                    currency = cur,
                    paymentMethod = req.paymentMethod,
                    referenceNumber = req.referenceNumber,
                    note = req.note.ifBlank { "شحن رصيد معتمد" },
                    timestamp = now,
                    status = TransactionStatus.COMPLETED.name
                )

                val txRef = firestore.collection("wallet_transactions").document(tx.id)
                transaction.set(txRef, tx)
                tx
            }.await()

            Result.success(txResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectTopUpRequest(
        firestore: FirebaseFirestore,
        requestId: String,
        reviewerId: String,
        reason: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            firestore.collection(COLLECTION_TOPUPS)
                .document(requestId)
                .update(
                    mapOf(
                        "status" to "REJECTED",
                        "reviewedAt" to now,
                        "reviewerId" to reviewerId,
                        "rejectionReason" to reason.trim()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
