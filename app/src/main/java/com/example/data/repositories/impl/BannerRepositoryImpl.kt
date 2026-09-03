package com.example.data.repositories.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.example.data.BannerEntity
import com.example.data.repositories.contracts.IBannerRepository
import com.example.data.utils.AppError
import com.example.data.utils.AppResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BannerRepositoryImpl(
    private val firestore: FirebaseFirestore
) : IBannerRepository {

    override fun observeBanners(): Flow<AppResult<List<BannerEntity>>> = callbackFlow {
        val listener = firestore.collection("banners")
            .orderBy("order")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(AppError.DatabaseError(error.localizedMessage ?: "خطأ في جلب البانرات")))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val banners = snapshot.toObjects(BannerEntity::class.java)
                    trySend(Result.success(banners))
                } else {
                    trySend(Result.success(emptyList()))
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addBanner(banner: BannerEntity): AppResult<Unit> {
        return try {
            val docRef = firestore.collection("banners").document()
            val bannerWithId = banner.copy(id = docRef.id)
            docRef.set(bannerWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل إضافة البانر"))
        }
    }

    override suspend fun updateBanner(banner: BannerEntity): AppResult<Unit> {
        return try {
            firestore.collection("banners").document(banner.id).set(banner).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل تحديث البانر"))
        }
    }

    override suspend fun deleteBanner(bannerId: String): AppResult<Unit> {
        return try {
            firestore.collection("banners").document(bannerId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل حذف البانر"))
        }
    }
}
