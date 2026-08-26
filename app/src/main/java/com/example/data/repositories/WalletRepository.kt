package com.example.data.repositories

import com.example.data.PaymentEntity
import com.example.data.PaymentWalletEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * 💰 WalletRepository - مستودع المحافظ والمعاملات المالية
 * 
 * الميزات:
 * 1. الربط مع Firebase Firestore لجلب المحافظ والتحديث الآلي لحظة بلحظة.
 * 2. المزامنة والعمل بدعم التخزين المحلي والـ SnapshotListener.
 * 3. تنفيذ عمليات الإيداع والسحب وتحديث رصيد المحفظة بشكل آمن عبر Firestore Transaction.
 */
class WalletRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * جلب تدفق بيانات المحفظة حسب معرّف المحفظة (walletId)
     */
    fun getWalletFlow(walletId: String): Flow<PaymentWalletEntity?> = callbackFlow {
        if (walletId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("wallets")
            .document(walletId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val wallet = snapshot?.toObject(PaymentWalletEntity::class.java)
                trySend(wallet)
            }

        awaitClose { listener.remove() }
    }

    /**
     * جلب تدفق بيانات المحفظة حسب معرّف المالك (ownerId)
     */
    fun getWalletByOwnerFlow(ownerId: String): Flow<PaymentWalletEntity?> = callbackFlow {
        if (ownerId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("wallets")
            .whereEqualTo("ownerId", ownerId)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val wallet = snapshot?.documents?.firstOrNull()?.toObject(PaymentWalletEntity::class.java)
                trySend(wallet)
            }

        awaitClose { listener.remove() }
    }

    /**
     * جلب تدفق معاملات المحفظة (Transactions)
     */
    fun getTransactionsFlow(walletId: String): Flow<List<PaymentEntity>> = callbackFlow {
        if (walletId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("transactions")
            .whereEqualTo("walletNumber", walletId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PaymentEntity::class.java)
                } ?: emptyList()
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    /**
     * تنفيذ إيداع مالي وتحديث الرصيد في Firestore
     */
    suspend fun deposit(walletId: String, amount: Double, note: String = ""): Result<PaymentEntity> {
        return try {
            val txId = "TX_${UUID.randomUUID().toString().take(8)}"
            val transaction = PaymentEntity(
                id = txId,
                userId = walletId,
                walletNumber = walletId,
                type = "DEPOSIT",
                amount = amount,
                status = "COMPLETED",
                adminNote = note,
                createdAt = System.currentTimeMillis()
            )

            val walletRef = firestore.collection("wallets").document(walletId)
            firestore.runTransaction { tx ->
                val snapshot = tx.get(walletRef)
                val currentBalance = snapshot.getDouble("balance") ?: 0.0
                tx.update(walletRef, "balance", currentBalance + amount)
                tx.set(firestore.collection("transactions").document(txId), transaction)
            }.await()

            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * تنفيذ سحب مالي وتحديث الرصيد في Firestore
     */
    suspend fun withdraw(walletId: String, amount: Double, note: String = ""): Result<PaymentEntity> {
        return try {
            val walletRef = firestore.collection("wallets").document(walletId)
            val snapshot = walletRef.get().await()
            val currentBalance = snapshot.getDouble("balance") ?: 0.0

            if (currentBalance < amount) {
                return Result.failure(IllegalStateException("رصيد المحفظة غير كافٍ لإتمام السحب"))
            }

            val txId = "TX_${UUID.randomUUID().toString().take(8)}"
            val transaction = PaymentEntity(
                id = txId,
                userId = walletId,
                walletNumber = walletId,
                type = "WITHDRAW",
                amount = amount,
                status = "COMPLETED",
                adminNote = note,
                createdAt = System.currentTimeMillis()
            )

            firestore.runTransaction { tx ->
                tx.update(walletRef, "balance", currentBalance - amount)
                tx.set(firestore.collection("transactions").document(txId), transaction)
            }.await()

            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
