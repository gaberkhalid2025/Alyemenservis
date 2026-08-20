package com.example.util

import android.content.Context
import androidx.annotation.Keep
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

@Keep
data class Wallet(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val type: String = "USER", // "USER", "PROVIDER", "STORE", "RESTAURANT"
    val balance: Double = 0.0,
    val currency: String = "YER",
    val status: String = "ACTIVE", // "ACTIVE", "FROZEN", "CLOSED"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Keep
data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val walletId: String = "",
    val type: String = "DEPOSIT", // "DEPOSIT", "WITHDRAWAL", "TRANSFER", "PAYMENT", "REFUND"
    val amount: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "COMPLETED" // "PENDING", "COMPLETED", "FAILED", "CANCELLED"
)

/**
 * 💰 WalletManager
 * إدارة المحافظ الرقمية الداخلية للمستخدمين، الفنيين، المتاجر والمطاعم
 */
class WalletManager(private val context: Context? = null) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    
    // In-memory cache for fast responsive offline access
    private val localWallets = mutableMapOf<String, Wallet>()
    private val localTransactions = mutableListOf<Transaction>()

    init {
        // Initial sample wallet for fallback
        val defaultUserWallet = Wallet(
            id = "wallet_user_default",
            userId = "user_default",
            type = "USER",
            balance = 45000.0,
            currency = "YER",
            status = "ACTIVE"
        )
        localWallets[defaultUserWallet.id] = defaultUserWallet
        localWallets[defaultUserWallet.userId] = defaultUserWallet

        localTransactions.add(
            Transaction(
                id = "tx_101",
                walletId = defaultUserWallet.id,
                type = "DEPOSIT",
                amount = 50000.0,
                balanceAfter = 50000.0,
                note = "شحن رصيد عبر محفظة الكريمي مميز",
                status = "COMPLETED",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 48
            )
        )
        localTransactions.add(
            Transaction(
                id = "tx_102",
                walletId = defaultUserWallet.id,
                type = "PAYMENT",
                amount = 5000.0,
                balanceAfter = 45000.0,
                note = "سداد قيمة فحص وصيانة منزلية",
                status = "COMPLETED",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 12
            )
        )
    }

    /**
     * 1. إنشاء محفظة رقمية جديدة
     */
    fun createWallet(userId: String, type: String = "USER"): Result<Wallet> {
        return try {
            val walletId = "wallet_${userId.ifBlank { UUID.randomUUID().toString() }}"
            val wallet = Wallet(
                id = walletId,
                userId = userId,
                type = type.uppercase(),
                balance = 0.0,
                currency = "YER",
                status = "ACTIVE",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            localWallets[wallet.id] = wallet
            localWallets[userId] = wallet

            try {
                firestore.collection("wallets").document(wallet.id).set(wallet)
            } catch (ignored: Exception) {}

            Result.success(wallet)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 2. إيداع رصيد في المحفظة
     */
    fun deposit(walletId: String, amount: Double, note: String): Result<Transaction> {
        return try {
            if (amount <= 0) return Result.failure(IllegalArgumentException("مبلغ الإيداع يجب أن يكون أكبر من الصفر"))
            
            val current = getOrCreateWallet(walletId)
            if (current.status == "FROZEN") {
                return Result.failure(IllegalStateException("المحفظة مجمدة ولا يمكن إجراء إيداعات حالياً"))
            }

            val newBalance = current.balance + amount
            val updated = current.copy(balance = newBalance, updatedAt = System.currentTimeMillis())
            localWallets[current.id] = updated
            localWallets[current.userId] = updated

            val tx = Transaction(
                id = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
                walletId = current.id,
                type = "DEPOSIT",
                amount = amount,
                balanceAfter = newBalance,
                note = note.ifBlank { "إيداع رصيد" },
                timestamp = System.currentTimeMillis(),
                status = "COMPLETED"
            )
            localTransactions.add(0, tx)

            try {
                firestore.collection("wallets").document(current.id).set(updated)
                firestore.collection("wallet_transactions").document(tx.id).set(tx)
            } catch (ignored: Exception) {}

            Result.success(tx)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 3. سحب رصيد من المحفظة
     */
    fun withdraw(walletId: String, amount: Double, note: String): Result<Transaction> {
        return try {
            if (amount <= 0) return Result.failure(IllegalArgumentException("مبلغ السحب يجب أن يكون أكبر من الصفر"))
            
            val current = getOrCreateWallet(walletId)
            if (current.status == "FROZEN") {
                return Result.failure(IllegalStateException("المحفظة مجمدة ولا يمكن السحب منها"))
            }
            if (current.balance < amount) {
                return Result.failure(IllegalStateException("الرصيد غير كافٍ. الرصيد الحالي: ${current.balance} YER"))
            }

            val newBalance = current.balance - amount
            val updated = current.copy(balance = newBalance, updatedAt = System.currentTimeMillis())
            localWallets[current.id] = updated
            localWallets[current.userId] = updated

            val tx = Transaction(
                id = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
                walletId = current.id,
                type = "WITHDRAWAL",
                amount = amount,
                balanceAfter = newBalance,
                note = note.ifBlank { "سحب رصيد" },
                timestamp = System.currentTimeMillis(),
                status = "COMPLETED"
            )
            localTransactions.add(0, tx)

            try {
                firestore.collection("wallets").document(current.id).set(updated)
                firestore.collection("wallet_transactions").document(tx.id).set(tx)
            } catch (ignored: Exception) {}

            Result.success(tx)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 4. تحويل رصيد بين محفظتين
     */
    fun transfer(fromWalletId: String, toWalletId: String, amount: Double): Result<Transaction> {
        return try {
            if (amount <= 0) return Result.failure(IllegalArgumentException("مبلغ التحويل غير صالح"))
            
            val sender = getOrCreateWallet(fromWalletId)
            val receiver = getOrCreateWallet(toWalletId)

            if (sender.balance < amount) {
                return Result.failure(IllegalStateException("رصيد المحفظة المصدر غير كافٍ"))
            }

            withdraw(fromWalletId, amount, "تحويل إلى محفظة ${receiver.userId}")
            deposit(toWalletId, amount, "استلام تحويل من محفظة ${sender.userId}")

            val tx = Transaction(
                id = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
                walletId = sender.id,
                type = "TRANSFER",
                amount = amount,
                balanceAfter = sender.balance - amount,
                note = "تحويل إلى $toWalletId",
                timestamp = System.currentTimeMillis(),
                status = "COMPLETED"
            )
            Result.success(tx)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 5. الحصول على الرصيد الحالي
     */
    fun getBalance(walletId: String): Double {
        return localWallets[walletId]?.balance ?: 0.0
    }

    /**
     * 6. سجل المعاملات لمحفظة معينة
     */
    fun getTransactions(walletId: String, limit: Int = 50): List<Transaction> {
        val clean = walletId.trim()
        return localTransactions
            .filter { it.walletId == clean || it.walletId.contains(clean) }
            .take(limit)
    }

    /**
     * 7. تجميد المحفظة
     */
    fun freezeWallet(walletId: String, reason: String): Result<Boolean> {
        return try {
            val wallet = getOrCreateWallet(walletId)
            val updated = wallet.copy(status = "FROZEN", updatedAt = System.currentTimeMillis())
            localWallets[wallet.id] = updated
            localWallets[wallet.userId] = updated
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 8. إلغاء تجميد وتفعيل المحفظة
     */
    fun unfreezeWallet(walletId: String): Result<Boolean> {
        return try {
            val wallet = getOrCreateWallet(walletId)
            val updated = wallet.copy(status = "ACTIVE", updatedAt = System.currentTimeMillis())
            localWallets[wallet.id] = updated
            localWallets[wallet.userId] = updated
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 9. حذف المحفظة
     */
    fun deleteWallet(walletId: String): Result<Boolean> {
        return try {
            localWallets.remove(walletId)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getOrCreateWallet(walletId: String): Wallet {
        return localWallets[walletId] ?: run {
            val w = Wallet(id = walletId, userId = walletId, balance = 0.0)
            localWallets[walletId] = w
            w
        }
    }
}
