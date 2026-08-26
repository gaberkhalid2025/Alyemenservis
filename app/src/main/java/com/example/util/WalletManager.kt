package com.example.util

import android.content.Context
import androidx.annotation.Keep
import com.example.data.PaymentEntity
import com.example.data.PaymentWalletEntity
import com.example.data.repositories.WalletRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Keep
data class Wallet(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val type: String = "USER", // "USER", "PROVIDER", "STORE", "RESTAURANT"
    val balance: Double = 0.0,
    val currency: String = "YER",
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Keep
data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val walletId: String = "",
    val type: String = "DEPOSIT",
    val amount: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "COMPLETED"
)

/**
 * 💰 WalletManager - مدير المحافظ الإلكترونية المعزز
 * 
 * الميزات:
 * 1. الاتصال بـ Firebase Firestore عبر `WalletRepository` لمزامنة الأرصدة والعمليات المباشرة.
 * 2. دعم الـ Flow والتحديثات اللحظية عبر SnapshotListeners.
 * 3. العمل في وضع الأوفلاين مع تخزين مؤقت محلي وسريع.
 */
class WalletManager(private val context: Context? = null) {

    private val repository = WalletRepository()
    private val localWallets = mutableMapOf<String, Wallet>()
    private val localTransactions = mutableListOf<Transaction>()

    init {
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
    }

    /**
     * جلب تدفق المحفظة من Firestore
     */
    fun getWalletFlow(walletId: String): Flow<PaymentWalletEntity?> {
        return repository.getWalletFlow(walletId)
    }

    /**
     * جلب تدفق المعاملات المالية لحظياً
     */
    fun getTransactionsFlow(walletId: String): Flow<List<PaymentEntity>> {
        return repository.getTransactionsFlow(walletId)
    }

    /**
     * إيداع رصيد في المحفظة بواسطة Firestore Transaction مع تعليق الاستجابة
     */
    suspend fun deposit(walletId: String, amount: Double, note: String = ""): Result<Transaction> {
        val result = repository.deposit(walletId, amount, note)
        return if (result.isSuccess) {
            val payment = result.getOrNull()
            val tx = Transaction(
                id = payment?.id ?: UUID.randomUUID().toString(),
                walletId = walletId,
                type = "DEPOSIT",
                amount = amount,
                note = note,
                status = "COMPLETED"
            )
            Result.success(tx)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("فشل الإيداع"))
        }
    }

    /**
     * سحب رصيد من المحفظة بواسطة Firestore Transaction
     */
    suspend fun withdraw(walletId: String, amount: Double, note: String = ""): Result<Transaction> {
        val result = repository.withdraw(walletId, amount, note)
        return if (result.isSuccess) {
            val payment = result.getOrNull()
            val tx = Transaction(
                id = payment?.id ?: UUID.randomUUID().toString(),
                walletId = walletId,
                type = "WITHDRAWAL",
                amount = amount,
                note = note,
                status = "COMPLETED"
            )
            Result.success(tx)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("فشل السحب"))
        }
    }

    /**
     * إنشاء محفظة رقمية جديدة
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
                status = "ACTIVE"
            )
            localWallets[wallet.id] = wallet
            localWallets[userId] = wallet
            Result.success(wallet)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getBalance(walletId: String): Double {
        return localWallets[walletId]?.balance ?: 0.0
    }

    fun freezeWallet(walletId: String, reason: String): Result<Boolean> {
        val wallet = localWallets[walletId] ?: return Result.failure(Exception("المحفظة غير موجودة"))
        localWallets[walletId] = wallet.copy(status = "FROZEN")
        return Result.success(true)
    }

    fun unfreezeWallet(walletId: String): Result<Boolean> {
        val wallet = localWallets[walletId] ?: return Result.failure(Exception("المحفظة غير موجودة"))
        localWallets[walletId] = wallet.copy(status = "ACTIVE")
        return Result.success(true)
    }
}
