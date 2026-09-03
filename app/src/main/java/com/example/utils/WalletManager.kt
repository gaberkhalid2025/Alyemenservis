package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Keep
import com.example.data.models.Transaction
import com.example.data.models.TransactionStatus
import com.example.data.models.TransactionType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

typealias Transaction = com.example.data.models.Transaction

@Keep
data class Wallet(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val type: String = "USER", // "USER", "PROVIDER", "STORE", "RESTAURANT"
    val balanceYer: Double = 0.0,
    val balanceUsd: Double = 0.0,
    val balanceSar: Double = 0.0,
    val balance: Double = 0.0, // Default YER compatibility
    val currency: String = "YER",
    val status: String = "ACTIVE", // "ACTIVE", "FROZEN", "CLOSED"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 💰 WalletManager
 * إدارة المحافظ الإلكترونية المتعددة العملات والمعاملات المالية مع المزامنة السحابية والتخزين المحلي
 */
class WalletManager(private val context: Context? = null) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val sharedPrefs: SharedPreferences? by lazy {
        context?.getSharedPreferences("yemen_wallet_secure_cache", Context.MODE_PRIVATE)
    }

    // In-memory / reactive state cache
    private val localWallets = mutableMapOf<String, Wallet>()
    private val _transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())
    val transactionsFlow: StateFlow<List<Transaction>> = _transactionsFlow.asStateFlow()

    init {
        loadFromLocalStorage()
    }

    private fun loadFromLocalStorage() {
        // Fallback default sample wallet
        val defaultUserWallet = Wallet(
            id = "wallet_user_default",
            userId = "user_default",
            type = "USER",
            balanceYer = 45000.0,
            balanceUsd = 100.0,
            balanceSar = 350.0,
            balance = 45000.0,
            currency = "YER",
            status = "ACTIVE"
        )
        localWallets[defaultUserWallet.id] = defaultUserWallet
        localWallets[defaultUserWallet.userId] = defaultUserWallet

        // Try load cached transactions
        sharedPrefs?.getString("cached_txs_json", null)?.let { json ->
            try {
                val type = Types.newParameterizedType(List::class.java, Transaction::class.java)
                val adapter = moshi.adapter<List<Transaction>>(type)
                val list = adapter.fromJson(json) ?: emptyList()
                _transactionsFlow.value = list
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }

        if (_transactionsFlow.value.isEmpty()) {
            val initialTxs = listOf(
                Transaction(
                    id = "tx_101",
                    walletId = defaultUserWallet.id,
                    userId = defaultUserWallet.userId,
                    type = TransactionType.DEPOSIT.name,
                    amount = 50000.0,
                    balanceAfter = 50000.0,
                    note = "شحن رصيد عبر محفظة الكريمي مميز",
                    currency = "YER",
                    status = TransactionStatus.COMPLETED.name,
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 48
                ),
                Transaction(
                    id = "tx_102",
                    walletId = defaultUserWallet.id,
                    userId = defaultUserWallet.userId,
                    type = TransactionType.PAYMENT.name,
                    amount = 5000.0,
                    balanceAfter = 45000.0,
                    note = "سداد قيمة فحص وصيانة منزلية",
                    currency = "YER",
                    status = TransactionStatus.COMPLETED.name,
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 12
                )
            )
            _transactionsFlow.value = initialTxs
            saveTransactionsToCache(initialTxs)
        }
    }

    private fun saveTransactionsToCache(list: List<Transaction>) {
        try {
            val type = Types.newParameterizedType(List::class.java, Transaction::class.java)
            val adapter = moshi.adapter<List<Transaction>>(type)
            val json = adapter.toJson(list)
            sharedPrefs?.edit()?.putString("cached_txs_json", json)?.apply()
        } catch (ignored: Exception) {}
    }

    /**
     * 1. إنشاء أو جلب محفظة رقمية
     */
    fun createWallet(userId: String, type: String = "USER"): Result<Wallet> {
        return try {
            val walletId = "wallet_${userId.ifBlank { UUID.randomUUID().toString() }}"
            val wallet = Wallet(
                id = walletId,
                userId = userId,
                type = type.uppercase(),
                balanceYer = 0.0,
                balanceUsd = 0.0,
                balanceSar = 0.0,
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
     * 2. إيداع رصيد في المحفظة بعملات متعددة
     */
    fun deposit(
        walletId: String,
        amount: Double,
        currency: String = "YER",
        note: String = "إيداع رصيد",
        paymentMethod: String = "INTERNAL_WALLET"
    ): Result<Transaction> {
        return try {
            if (amount <= 0) return Result.failure(IllegalArgumentException("مبلغ الإيداع يجب أن يكون أكبر من الصفر"))

            val current = getOrCreateWallet(walletId)
            if (current.status == "FROZEN") {
                return Result.failure(IllegalStateException("المحفظة مجمدة ولا يمكن إجراء إيداعات حالياً"))
            }

            val newYer = if (currency.uppercase() == "YER") current.balanceYer + amount else current.balanceYer
            val newUsd = if (currency.uppercase() == "USD") current.balanceUsd + amount else current.balanceUsd
            val newSar = if (currency.uppercase() == "SAR") current.balanceSar + amount else current.balanceSar
            val primaryBalance = newYer

            val updated = current.copy(
                balanceYer = newYer,
                balanceUsd = newUsd,
                balanceSar = newSar,
                balance = primaryBalance,
                updatedAt = System.currentTimeMillis()
            )
            localWallets[current.id] = updated
            localWallets[current.userId] = updated

            val balanceAfter = when (currency.uppercase()) {
                "USD" -> newUsd
                "SAR" -> newSar
                else -> newYer
            }

            val tx = Transaction(
                id = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
                walletId = current.id,
                userId = current.userId,
                type = TransactionType.DEPOSIT.name,
                amount = amount,
                balanceAfter = balanceAfter,
                currency = currency.uppercase(),
                paymentMethod = paymentMethod,
                note = note.ifBlank { "إيداع رصيد" },
                timestamp = System.currentTimeMillis(),
                status = TransactionStatus.COMPLETED.name
            )

            val currentList = _transactionsFlow.value.toMutableList()
            currentList.add(0, tx)
            _transactionsFlow.value = currentList
            saveTransactionsToCache(currentList)

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
    fun withdraw(
        walletId: String,
        amount: Double,
        currency: String = "YER",
        note: String = "سحب رصيد"
    ): Result<Transaction> {
        return try {
            if (amount <= 0) return Result.failure(IllegalArgumentException("مبلغ السحب يجب أن يكون أكبر من الصفر"))

            val current = getOrCreateWallet(walletId)
            if (current.status == "FROZEN") {
                return Result.failure(IllegalStateException("المحفظة مجمدة ولا يمكن السحب منها"))
            }

            val cur = currency.uppercase()
            val available = when (cur) {
                "USD" -> current.balanceUsd
                "SAR" -> current.balanceSar
                else -> current.balanceYer
            }

            if (available < amount) {
                return Result.failure(IllegalStateException("الرصيد غير كافٍ. الرصيد المتاح: $available $cur"))
            }

            val newYer = if (cur == "YER") current.balanceYer - amount else current.balanceYer
            val newUsd = if (cur == "USD") current.balanceUsd - amount else current.balanceUsd
            val newSar = if (cur == "SAR") current.balanceSar - amount else current.balanceSar

            val updated = current.copy(
                balanceYer = newYer,
                balanceUsd = newUsd,
                balanceSar = newSar,
                balance = newYer,
                updatedAt = System.currentTimeMillis()
            )
            localWallets[current.id] = updated
            localWallets[current.userId] = updated

            val balanceAfter = when (cur) {
                "USD" -> newUsd
                "SAR" -> newSar
                else -> newYer
            }

            val tx = Transaction(
                id = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
                walletId = current.id,
                userId = current.userId,
                type = TransactionType.WITHDRAWAL.name,
                amount = amount,
                balanceAfter = balanceAfter,
                currency = cur,
                note = note.ifBlank { "سحب رصيد" },
                timestamp = System.currentTimeMillis(),
                status = TransactionStatus.COMPLETED.name
            )

            val currentList = _transactionsFlow.value.toMutableList()
            currentList.add(0, tx)
            _transactionsFlow.value = currentList
            saveTransactionsToCache(currentList)

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
    fun transfer(
        fromWalletId: String,
        toWalletId: String,
        amount: Double,
        currency: String = "YER"
    ): Result<Transaction> {
        return try {
            if (amount <= 0) return Result.failure(IllegalArgumentException("مبلغ التحويل غير صالح"))

            val sender = getOrCreateWallet(fromWalletId)
            val receiver = getOrCreateWallet(toWalletId)

            val withdrawRes = withdraw(fromWalletId, amount, currency, "تحويل إلى محفظة ${receiver.userId}")
            if (withdrawRes.isFailure) return withdrawRes

            deposit(toWalletId, amount, currency, "استلام تحويل من محفظة ${sender.userId}")

            val tx = Transaction(
                id = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
                walletId = sender.id,
                userId = sender.userId,
                type = TransactionType.TRANSFER.name,
                amount = amount,
                balanceAfter = getBalance(fromWalletId, currency),
                currency = currency.uppercase(),
                targetWalletId = receiver.id,
                note = "تحويل إلى $toWalletId",
                timestamp = System.currentTimeMillis(),
                status = TransactionStatus.COMPLETED.name
            )
            Result.success(tx)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 5. الحصول على الرصيد الحالي حسب العملة
     */
    fun getBalance(walletId: String, currency: String = "YER"): Double {
        val wallet = getOrCreateWallet(walletId)
        return when (currency.uppercase()) {
            "USD" -> wallet.balanceUsd
            "SAR" -> wallet.balanceSar
            else -> wallet.balanceYer
        }
    }

    /**
     * 6. سجل المعاملات لمحفظة معينة
     */
    fun getTransactions(walletId: String, limit: Int = 50): List<Transaction> {
        val clean = walletId.trim()
        return _transactionsFlow.value
            .filter { it.walletId == clean || it.userId == clean }
            .take(limit)
    }

    /**
     * 7. سجل المعاملات لمستخدم معين
     */
    fun getTransactionHistory(userId: String): List<Transaction> {
        return getTransactions(userId)
    }

    /**
     * 8. تدفق حي لمعاملات المستخدم عبر Firestore
     */
    fun getLiveTransactionsFlow(userId: String): Flow<List<Transaction>> = callbackFlow {
        val listener = firestore.collection("wallet_transactions")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(getTransactions(userId))
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull { it.toObject(Transaction::class.java) }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun freezeWallet(walletId: String, reason: String = ""): Result<Boolean> {
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

    private fun getOrCreateWallet(walletId: String): Wallet {
        return localWallets[walletId] ?: run {
            val w = Wallet(id = walletId, userId = walletId, balanceYer = 0.0, balance = 0.0)
            localWallets[walletId] = w
            w
        }
    }
}
