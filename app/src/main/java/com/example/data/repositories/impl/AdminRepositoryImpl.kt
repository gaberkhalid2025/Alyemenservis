package com.example.data.repositories.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.example.data.repositories.contracts.IAdminRepository
import com.example.data.utils.AppError
import com.example.data.utils.AppResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AdminRepositoryImpl(
    private val firestore: FirebaseFirestore
) : IAdminRepository {

    override suspend fun approveJoinRequest(entityId: String, entityType: String): AppResult<Unit> {
        return try {
            firestore.collection(entityType).document(entityId)
                .update("status", "APPROVED", "updatedAt", System.currentTimeMillis())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل قبول الطلب"))
        }
    }

    override suspend fun rejectJoinRequest(entityId: String, reason: String): AppResult<Unit> {
        return try {
            firestore.collection("requests").document(entityId)
                .update("status", "REJECTED", "rejectReason", reason, "updatedAt", System.currentTimeMillis())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل رفض الطلب"))
        }
    }

    override suspend fun blockEntity(entityId: String, entityType: String): AppResult<Unit> {
        return try {
            firestore.collection(entityType).document(entityId)
                .update("isBlocked", true, "blockedAt", System.currentTimeMillis())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل حظر الكيان"))
        }
    }

    override suspend fun unblockEntity(entityId: String, entityType: String): AppResult<Unit> {
        return try {
            firestore.collection(entityType).document(entityId)
                .update("isBlocked", false, "unblockedAt", System.currentTimeMillis())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل إلغاء حظر الكيان"))
        }
    }

    override suspend fun deleteEntity(entityId: String, entityType: String): AppResult<Unit> {
        return try {
            firestore.collection(entityType).document(entityId)
                .update("isDeleted", true, "deletedAt", System.currentTimeMillis())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل حذف الكيان"))
        }
    }

    override suspend fun restoreEntity(entityId: String, entityType: String): AppResult<Unit> {
        return try {
            firestore.collection(entityType).document(entityId)
                .update("isDeleted", false, "deletedAt", null)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل استعادة الكيان"))
        }
    }

    override fun getSystemMetrics(): Flow<AppResult<Map<String, Long>>> = callbackFlow {
        val listener = firestore.collection("metrics").document("system_stats")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(AppError.DatabaseError(error.localizedMessage ?: "خطأ في جلب المؤشرات")))
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data as? Map<String, Long> ?: emptyMap()
                    trySend(Result.success(data))
                } else {
                    trySend(Result.success(emptyMap()))
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun managePermissions(userId: String, permissions: List<String>): AppResult<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .update("permissions", permissions)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل تحديث الصلاحيات"))
        }
    }
}
