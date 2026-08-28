package com.example.data.repositories

import android.content.Context
import android.util.Log
import com.example.data.LocalAppCacheManager
import com.example.domain.entities.DashboardStatsEntity
import com.example.domain.entities.FavoriteItemEntity
import com.example.domain.entities.GalleryAlbumEntity
import com.example.domain.entities.ProductItemEntity
import com.example.domain.entities.RatingReviewEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * 📦 DashboardRepositoryImpl
 * Handles stats calculation and dashboard data aggregation with offline fallback.
 */
class DashboardRepositoryImpl(
    private val context: Context
) : IDashboardRepository {

    private val firestore = FirebaseFirestore.getInstance()

    override fun getDashboardStats(ownerId: String, role: String): Flow<DashboardStatsEntity> = callbackFlow {
        if (ownerId.isBlank()) {
            trySend(DashboardStatsEntity())
            return@callbackFlow
        }

        val collectionName = when (role.uppercase()) {
            "PROVIDER", "TECHNICIAN" -> "users"
            "STORE" -> "stores"
            "RESTAURANT" -> "restaurants"
            "MEDICAL" -> "medical_centers"
            "PROPERTY" -> "properties"
            "JOB" -> "job_listings"
            else -> "users"
        }

        val listener: ListenerRegistration = firestore.collection(collectionName).document(ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(DashboardStatsEntity())
                    return@addSnapshotListener
                }

                val stats = DashboardStatsEntity(
                    totalViews = (snapshot.getLong("viewsCount") ?: 0L).toInt(),
                    activeBookingsCount = (snapshot.getLong("activeBookings") ?: 0L).toInt(),
                    completedBookingsCount = (snapshot.getLong("completedBookings") ?: 0L).toInt(),
                    averageRating = snapshot.getDouble("rating") ?: 5.0,
                    totalReviewsCount = (snapshot.getLong("reviewsCount") ?: 0L).toInt(),
                    totalRevenueYer = snapshot.getDouble("totalRevenue") ?: 0.0,
                    totalProductsCount = (snapshot.getLong("productsCount") ?: 0L).toInt(),
                    pendingRequestsCount = (snapshot.getLong("pendingRequests") ?: 0L).toInt()
                )
                trySend(stats)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun refreshDashboardStats(ownerId: String, role: String): Result<Unit> {
        return try {
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 📦 FavoritesRepositoryImpl
 */
class FavoritesRepositoryImpl(
    private val context: Context
) : IFavoritesRepository {

    private val firestore = FirebaseFirestore.getInstance()

    override fun getUserFavorites(userId: String): Flow<List<FavoriteItemEntity>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    FavoriteItemEntity(
                        id = doc.id,
                        userId = userId,
                        targetId = doc.getString("targetId") ?: "",
                        targetType = doc.getString("targetType") ?: "PROVIDER",
                        title = doc.getString("title") ?: "",
                        category = doc.getString("category") ?: "",
                        city = doc.getString("city") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        rating = doc.getDouble("rating") ?: 5.0,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                }
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun addFavorite(favorite: FavoriteItemEntity): Result<Unit> {
        return try {
            val favId = if (favorite.id.isBlank()) favorite.targetId else favorite.id
            val map = mapOf(
                "targetId" to favorite.targetId,
                "targetType" to favorite.targetType,
                "title" to favorite.title,
                "category" to favorite.category,
                "city" to favorite.city,
                "imageUrl" to favorite.imageUrl,
                "rating" to favorite.rating,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(favorite.userId)
                .collection("favorites").document(favId).set(map).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFavorite(userId: String, targetId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .collection("favorites").document(targetId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isFavorite(userId: String, targetId: String): Boolean {
        if (userId.isBlank() || targetId.isBlank()) return false
        return try {
            val doc = firestore.collection("users").document(userId)
                .collection("favorites").document(targetId).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * 📦 ProductsRepositoryImpl
 */
class ProductsRepositoryImpl(
    private val context: Context
) : IProductsRepository {

    private val firestore = FirebaseFirestore.getInstance()

    override fun getOwnerProducts(ownerId: String): Flow<List<ProductItemEntity>> = callbackFlow {
        if (ownerId.isBlank()) {
            trySend(emptyList())
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("products")
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val items = snapshot.documents.mapNotNull { doc ->
                    ProductItemEntity(
                        id = doc.id,
                        ownerId = doc.getString("ownerId") ?: ownerId,
                        title = doc.getString("title") ?: doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        category = doc.getString("category") ?: "",
                        priceYer = doc.getDouble("priceYer") ?: doc.getDouble("price") ?: 0.0,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        isAvailable = doc.getBoolean("isAvailable") ?: true,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                }
                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    override fun getAllAvailableProducts(): Flow<List<ProductItemEntity>> = callbackFlow {
        val listener: ListenerRegistration = firestore.collection("products")
            .whereEqualTo("isAvailable", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val items = snapshot.documents.mapNotNull { doc ->
                    ProductItemEntity(
                        id = doc.id,
                        ownerId = doc.getString("ownerId") ?: "",
                        title = doc.getString("title") ?: doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        category = doc.getString("category") ?: "",
                        priceYer = doc.getDouble("priceYer") ?: doc.getDouble("price") ?: 0.0,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        isAvailable = true,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                }
                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun addProduct(product: ProductItemEntity): Result<String> {
        return try {
            val id = UUID.randomUUID().toString()
            val map = mapOf(
                "id" to id,
                "ownerId" to product.ownerId,
                "title" to product.title,
                "description" to product.description,
                "category" to product.category,
                "priceYer" to product.priceYer,
                "imageUrl" to product.imageUrl,
                "isAvailable" to product.isAvailable,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("products").document(id).set(map).await()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProduct(product: ProductItemEntity): Result<Unit> {
        return try {
            val map = mapOf(
                "title" to product.title,
                "description" to product.description,
                "category" to product.category,
                "priceYer" to product.priceYer,
                "imageUrl" to product.imageUrl,
                "isAvailable" to product.isAvailable
            )
            firestore.collection("products").document(product.id).update(map).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            firestore.collection("products").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 📦 RatingsRepositoryImpl
 */
class RatingsRepositoryImpl(
    private val context: Context
) : IRatingsRepository {

    private val firestore = FirebaseFirestore.getInstance()

    override fun getTargetRatings(targetId: String): Flow<List<RatingReviewEntity>> = callbackFlow {
        if (targetId.isBlank()) {
            trySend(emptyList())
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("ratings")
            .whereEqualTo("targetId", targetId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    RatingReviewEntity(
                        id = doc.id,
                        targetId = targetId,
                        authorName = doc.getString("authorName") ?: doc.getString("userName") ?: "عميل",
                        authorPhone = doc.getString("authorPhone") ?: "",
                        rating = doc.getDouble("rating") ?: 5.0,
                        comment = doc.getString("comment") ?: doc.getString("review") ?: "",
                        dateTimestamp = doc.getLong("dateTimestamp") ?: System.currentTimeMillis()
                    )
                }
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun addRating(rating: RatingReviewEntity): Result<String> {
        return try {
            val id = UUID.randomUUID().toString()
            val map = mapOf(
                "id" to id,
                "targetId" to rating.targetId,
                "authorName" to rating.authorName,
                "authorPhone" to rating.authorPhone,
                "rating" to rating.rating,
                "comment" to rating.comment,
                "dateTimestamp" to System.currentTimeMillis()
            )
            firestore.collection("ratings").document(id).set(map).await()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 📦 GalleryRepositoryImpl
 */
class GalleryRepositoryImpl(
    private val context: Context
) : IGalleryRepository {

    private val firestore = FirebaseFirestore.getInstance()

    override fun getOwnerGallery(ownerId: String): Flow<List<GalleryAlbumEntity>> = callbackFlow {
        if (ownerId.isBlank()) {
            trySend(emptyList())
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("galleries")
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    @Suppress("UNCHECKED_CAST")
                    GalleryAlbumEntity(
                        id = doc.id,
                        ownerId = ownerId,
                        title = doc.getString("title") ?: "معرض الصور",
                        imageUrls = (doc.get("imageUrls") as? List<String>) ?: emptyList(),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                }
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun saveGalleryAlbum(album: GalleryAlbumEntity): Result<String> {
        return try {
            val id = if (album.id.isBlank()) UUID.randomUUID().toString() else album.id
            val map = mapOf(
                "id" to id,
                "ownerId" to album.ownerId,
                "title" to album.title,
                "imageUrls" to album.imageUrls,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("galleries").document(id).set(map).await()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteGalleryAlbum(albumId: String): Result<Unit> {
        return try {
            firestore.collection("galleries").document(albumId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
