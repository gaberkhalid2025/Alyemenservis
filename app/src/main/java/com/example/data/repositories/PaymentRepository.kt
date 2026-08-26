package com.example.data.repositories

import com.example.data.PaymentEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * 💳 PaymentRepository - مستودع بوابات المعاملات والمدفوعات
 * 
 * الميزات:
 * 1. معالجة وتخزين سجل المعاملات المالية والمدفوعات في Firebase Firestore.
 * 2. دعم التأكيد الفوري، الإلغاء، المبالغ المستردة، والاستعلام المتزامن التفاعلي عبر Flow.
 */
class PaymentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * جلب سجل عمليات الدفع الخاصة بالمستخدم لحظياً
     */
    fun getTransactionHistoryFlow(userId: String): Flow<List<PaymentEntity>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("payments")
            .whereEqualTo("userId", userId)
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
     * معالجة عملية دفع جديدة وحفظها في Firestore
     */
    suspend fun processPayment(payment: PaymentEntity): Result<PaymentEntity> {
        return try {
            val paymentId = payment.id.ifBlank { "PAY_${UUID.randomUUID().toString().take(8)}" }
            val finalPayment = payment.copy(
                id = paymentId,
                status = if (payment.status.isBlank()) "PROCESSING" else payment.status,
                createdAt = if (payment.createdAt == 0L) System.currentTimeMillis() else payment.createdAt
            )

            firestore.collection("payments").document(paymentId).set(finalPayment).await()
            Result.success(finalPayment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * تأكيد الدفع وتحديث حالة المعاملة
     */
    suspend fun confirmPayment(paymentId: String, note: String = ""): Result<Boolean> {
        return try {
            firestore.collection("payments").document(paymentId).update(
                mapOf(
                    "status" to "COMPLETED",
                    "verificationStatus" to "VERIFIED",
                    "paidAt" to System.currentTimeMillis(),
                    "verificationNote" to note
                )
            ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * إلغاء عملية الدفع وتغيير الحالة
     */
    suspend fun cancelPayment(paymentId: String, reason: String = ""): Result<Boolean> {
        return try {
            firestore.collection("payments").document(paymentId).update(
                mapOf(
                    "status" to "CANCELLED",
                    "verificationStatus" to "REJECTED",
                    "adminNote" to reason,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
