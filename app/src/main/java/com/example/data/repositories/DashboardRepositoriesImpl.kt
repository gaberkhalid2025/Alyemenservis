package com.example.data.repositories

import android.content.Context
import com.example.data.LocalAppCacheManager
import com.example.domain.entities.DashboardStatsEntity
import com.example.domain.entities.FavoriteItemEntity
import com.example.domain.entities.GalleryAlbumEntity
import com.example.domain.entities.ProductItemEntity
import com.example.domain.entities.RatingReviewEntity
import com.example.utils.AppConstants
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
    @Suppress("UNUSED_PARAMETER") context: Context? = null
) : IDashboardRepository {

    private val firestore = FirebaseFirestore.getInstance()

    override fun getDashboardStats(ownerId: String, role: String): Flow<DashboardStatsEntity> = callbackFlow {
        val cleanOwnerId = ownerId.trim()
        if (cleanOwnerId.isBlank()) {
            trySend(DashboardStatsEntity())
            close()
            return@callbackFlow
        }

        val collectionName = when (role.trim().uppercase()) {
            "PROVIDER", "TECHNICIAN" -> AppConstants.COL_PROVIDERS
            "STORE", "STORE_OWNER", "RESTAURANT", "RESTAURANT_OWNER", "MEDICAL", "MEDICAL_CENTER" -> AppConstants.COL_STORES
            "PROPERTY", "REAL_ESTATE", "PROPERTY_OWNER" -> AppConstants.COL_PROPERTIES
            "JOB", "JOB_POSTER" -> AppConstants.COL_JOBS
            else -> AppConstants.COL_USER_PROFILES
        }

        val listener: ListenerRegistration = firestore.collection(collectionName).document(cleanOwnerId)
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
        val cleanOwnerId = ownerId.trim()
        if (cleanOwnerId.isBlank()) return Result.failure(IllegalArgumentException("ownerId cannot be blank"))
        return try {
            val collectionName = when (role.trim().uppercase()) {
                "PROVIDER", "TECHNICIAN" -> AppConstants.COL_PROVIDERS
                "STORE", "STORE_OWNER", "RESTAURANT", "RESTAURANT_OWNER", "MEDICAL", "MEDICAL_CENTER" -> AppConstants.COL_STORES
                "PROPERTY", "REAL_ESTATE", "PROPERTY_OWNER" -> AppConstants.COL_PROPERTIES
                "JOB", "JOB_POSTER" -> AppConstants.COL_JOBS
                else -> AppConstants.COL_USER_PROFILES
            }
            firestore.collection(collectionName).document(cleanOwnerId)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun resolveCollection(role: String): String = when (role.trim().uppercase()) {
        "PROVIDER", "TECHNICIAN" -> AppConstants.COL_PROVIDERS
        "STORE", "STORE_OWNER", "MALL", "MALL_OWNER", "RESTAURANT", "RESTAURANT_OWNER", "MEDICAL", "MEDICAL_CENTER" -> AppConstants.COL_STORES
        "PROPERTY", "REAL_ESTATE", "PROPERTY_OWNER" -> AppConstants.COL_PROPERTIES
        "JOB", "JOB_POSTER" -> AppConstants.COL_JOBS
        else -> AppConstants.COL_USER_PROFILES
    }

    override suspend fun updateBusinessProfile(
        ownerId: String,
        role: String,
        name: String,
        cityArea: String,
        description: String,
        workingHours: String,
        isAvailable: Boolean,
        photoUrl: String?,
        coverUrl: String?,
        phone: String?,
        latitude: Double?,
        longitude: Double?
    ): Result<Unit> {
        val cleanOwnerId = ownerId.trim()
        if (cleanOwnerId.isBlank()) return Result.failure(IllegalArgumentException("معرف الحساب غير صالح"))
        if (name.isBlank()) return Result.failure(IllegalArgumentException("الاسم مطلوب"))
        return try {
            val collectionName = resolveCollection(role)
            val docRef = firestore.collection(collectionName).document(cleanOwnerId)
            val updates = mutableMapOf<String, Any>(
                "description" to description.trim(),
                "workingHours" to workingHours.trim(),
                "isAvailable" to isAvailable,
                "isActive" to isAvailable,
                "updatedAt" to System.currentTimeMillis()
            )
            val rawPhone = phone?.trim().orEmpty()
            var previousPhoneForSync = ""
            if (rawPhone.isNotBlank()) {
                val cleanPhone = com.example.domain.usecases.ValidatePhoneUseCase.normalizePhone(rawPhone)
                val existingSnap = try { docRef.get().await() } catch (_: Exception) { null }
                val storedPhone = existingSnap?.getString("phone")?.let {
                    com.example.domain.usecases.ValidatePhoneUseCase.normalizePhone(it)
                }.orEmpty()
                previousPhoneForSync = storedPhone
                if (cleanPhone != storedPhone) {
                    if (!Regex("^(77|78|73|71|70)\\d{7}$").matches(cleanPhone)) {
                        return Result.failure(IllegalArgumentException("رقم الهاتف غير صالح (يجب أن يكون 9 أرقام يبدأ بـ 77 أو 78 أو 73 أو 71 أو 70)"))
                    }
                    val activeCollections = listOf(
                        AppConstants.COL_PROVIDERS,
                        AppConstants.COL_STORES,
                        AppConstants.COL_PROPERTIES,
                        AppConstants.COL_USER_PROFILES
                    ).distinct()
                    for (col in activeCollections) {
                        val dupSnap = firestore.collection(col)
                            .whereEqualTo("phone", cleanPhone)
                            .limit(2)
                            .get()
                            .await()
                        val hasConflict = dupSnap.documents.any { doc ->
                            doc.id != cleanOwnerId &&
                                (storedPhone.isBlank() || doc.id != storedPhone) &&
                                doc.getBoolean("isDeleted") != true
                        }
                        if (hasConflict) {
                            return Result.failure(IllegalArgumentException("رقم الهاتف مستخدم بالفعل في حساب آخر نشط"))
                        }
                    }
                    updates["phone"] = cleanPhone
                }
            }
            if (latitude != null && longitude != null &&
                com.example.utils.LandmarkAddress.isValid(latitude, longitude)
            ) {
                updates["latitude"] = latitude
                updates["longitude"] = longitude
            }
            val cleanCityArea = cityArea.trim()
            when (collectionName) {
                AppConstants.COL_PROVIDERS -> {
                    updates["name"] = name.trim()
                    if (cleanCityArea.isNotBlank() && !cleanCityArea.startsWith("ye_")) {
                        updates["localNeighborhood"] = cleanCityArea
                    }
                    if (!photoUrl.isNullOrBlank()) updates["profileImage"] = photoUrl.trim()
                    if (!coverUrl.isNullOrBlank()) updates["coverImage"] = coverUrl.trim()
                }
                AppConstants.COL_PROPERTIES -> {
                    updates["title"] = name.trim()
                    if (cleanCityArea.isNotBlank() && !cleanCityArea.startsWith("ye_")) {
                        updates["localNeighborhood"] = cleanCityArea
                    }
                    val newImgs = listOfNotNull(
                        coverUrl?.trim()?.takeIf { it.isNotBlank() },
                        photoUrl?.trim()?.takeIf { it.isNotBlank() }
                    )
                    if (newImgs.isNotEmpty()) {
                        val existingSnap = try {
                            docRef.get().await()
                        } catch (_: Exception) { null }
                        @Suppress("UNCHECKED_CAST")
                        val existingImgs = (existingSnap?.get("images") as? List<String>)?.filter { it.isNotBlank() } ?: emptyList()
                        updates["images"] = (newImgs + existingImgs).distinct()
                    }
                }
                AppConstants.COL_JOBS -> {
                    updates["companyName"] = name.trim()
                    updates["address"] = cleanCityArea
                }
                else -> {
                    updates["name"] = name.trim()
                    if (cleanCityArea.isNotBlank() && !cleanCityArea.startsWith("ye_")) {
                        updates["localNeighborhood"] = cleanCityArea
                    }
                    if (!photoUrl.isNullOrBlank()) updates["logoImage"] = photoUrl.trim()
                    if (!coverUrl.isNullOrBlank()) updates["coverImage"] = coverUrl.trim()
                }
            }
            docRef.update(updates).await()
            val updatedPhone = updates["phone"] as? String
            if (!updatedPhone.isNullOrBlank()) {
                runCatching {
                    val regCol = firestore.collection(AppConstants.COL_USER_PROFILES)
                    val payload = mapOf("phone" to updatedPhone, "updatedAt" to System.currentTimeMillis())
                    val targetRegDoc = regCol.document(cleanOwnerId).get().await()
                    if (targetRegDoc.exists()) {
                        targetRegDoc.reference.update(payload).await()
                    } else if (previousPhoneForSync.isNotBlank()) {
                        val prevRegDoc = regCol.document(previousPhoneForSync).get().await()
                        if (prevRegDoc.exists()) {
                            prevRegDoc.reference.update(payload).await()
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changeBusinessPassword(
        ownerId: String,
        role: String,
        oldPassword: String,
        newPassword: String
    ): Result<Unit> {
        val cleanOwnerId = ownerId.trim()
        if (cleanOwnerId.isBlank()) return Result.failure(IllegalArgumentException("معرف الحساب غير صالح"))
        val cleanOld = oldPassword.trim()
        val cleanNew = newPassword.trim()
        if (cleanOld.isBlank() || cleanNew.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال كلمة المرور الحالية والجديدة"))
        }
        val (policyValid, policyMsg) = com.example.utils.SecurityCryptoUtils.validatePasswordPolicy(cleanNew)
        if (!policyValid) {
            return Result.failure(IllegalArgumentException(policyMsg ?: "كلمة المرور الجديدة ضعيفة"))
        }
        return try {
            val collectionName = resolveCollection(role)
            val docRef = firestore.collection(collectionName).document(cleanOwnerId)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                return Result.failure(IllegalStateException("لم يتم العثور على سجل الحساب"))
            }
            val storedHash = snapshot.getString("passwordHash")?.takeIf { it.isNotBlank() }
                ?: snapshot.getString("password")
                ?: ""
            if (storedHash.isBlank()) {
                return Result.failure(IllegalStateException("لا توجد كلمة مرور مسجلة لهذا الحساب"))
            }
            val verified = com.example.utils.PasswordHasher.verifyPassword(cleanOld, storedHash) ||
                com.example.utils.SecureHasher.verifyPassword(cleanOld, storedHash) ||
                storedHash == cleanOld
            if (!verified) {
                return Result.failure(IllegalArgumentException("كلمة المرور الحالية غير صحيحة ❌"))
            }
            val newHash = com.example.utils.PasswordHasher.hash(cleanNew)
            docRef.update(
                mapOf(
                    "passwordHash" to newHash,
                    "password" to "",
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
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
    @Suppress("UNUSED_PARAMETER") context: Context? = null
) : IFavoritesRepository {

    private val firestore = FirebaseFirestore.getInstance()

    override fun getUserFavorites(userId: String): Flow<List<FavoriteItemEntity>> = callbackFlow {
        val cleanUserId = userId.trim()
        if (cleanUserId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("users")
            .document(cleanUserId)
            .collection("favorites")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    FavoriteItemEntity(
                        id = doc.id,
                        userId = cleanUserId,
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
            val cleanUserId = favorite.userId.trim()
            val favId = favorite.id.trim().ifBlank { favorite.targetId.trim() }
            if (cleanUserId.isBlank() || favId.isBlank()) {
                return Result.failure(IllegalArgumentException("معرف المستخدم أو العنصر المفضل غير صالح"))
            }
            val map = mapOf(
                "targetId" to favorite.targetId.trim(),
                "targetType" to favorite.targetType.trim().ifBlank { "PROVIDER" },
                "title" to favorite.title.trim(),
                "category" to favorite.category.trim(),
                "city" to favorite.city.trim(),
                "imageUrl" to favorite.imageUrl.trim(),
                "rating" to favorite.rating.coerceIn(0.0, 5.0),
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(cleanUserId)
                .collection("favorites").document(favId).set(map).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFavorite(userId: String, targetId: String): Result<Unit> {
        return try {
            val cleanUserId = userId.trim()
            val cleanTargetId = targetId.trim()
            if (cleanUserId.isBlank() || cleanTargetId.isBlank()) {
                return Result.failure(IllegalArgumentException("معرف المستخدم أو العنصر المفضل غير صالح"))
            }
            firestore.collection("users").document(cleanUserId)
                .collection("favorites").document(cleanTargetId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isFavorite(userId: String, targetId: String): Boolean {
        val cleanUserId = userId.trim()
        val cleanTargetId = targetId.trim()
        if (cleanUserId.isBlank() || cleanTargetId.isBlank()) return false
        return try {
            val doc = firestore.collection("users").document(cleanUserId)
                .collection("favorites").document(cleanTargetId).get().await()
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
    context: Context? = null
) : IProductsRepository {

    private val firestore = FirebaseFirestore.getInstance()

    override fun getOwnerProducts(ownerId: String): Flow<List<ProductItemEntity>> = callbackFlow {
        val cleanOwnerId = ownerId.trim()
        if (cleanOwnerId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection(AppConstants.COL_PRODUCTS)
            .whereEqualTo("ownerId", cleanOwnerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val items = snapshot.documents.mapNotNull { doc ->
                    val isDel = doc.getBoolean("isDeleted") == true || doc.getBoolean("deleted") == true
                    if (isDel) return@mapNotNull null
                    ProductItemEntity(
                        id = doc.id,
                        ownerId = doc.getString("ownerId") ?: cleanOwnerId,
                        title = doc.getString("title") ?: doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        category = doc.getString("category")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("subCategory") ?: "",
                        priceYer = doc.getDouble("priceYer") ?: doc.getDouble("price") ?: 0.0,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        isAvailable = doc.getBoolean("isAvailable") ?: true,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        isOffer = doc.getBoolean("isOffer") == true,
                        discountPercent = (doc.getLong("discountPercent") ?: 0L).toInt(),
                        oldPriceYer = doc.getDouble("oldPriceYer") ?: doc.getDouble("oldPrice") ?: 0.0,
                        unit = doc.getString("unit") ?: ""
                    )
                }
                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    override fun getAllAvailableProducts(): Flow<List<ProductItemEntity>> = callbackFlow {
        val listener: ListenerRegistration = firestore.collection(AppConstants.COL_PRODUCTS)
            .whereEqualTo("isAvailable", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val items = snapshot.documents.mapNotNull { doc ->
                    val isDel = doc.getBoolean("isDeleted") == true || doc.getBoolean("deleted") == true
                    if (isDel) return@mapNotNull null
                    ProductItemEntity(
                        id = doc.id,
                        ownerId = doc.getString("ownerId") ?: "",
                        title = doc.getString("title") ?: doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        category = doc.getString("category")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("subCategory") ?: "",
                        priceYer = doc.getDouble("priceYer") ?: doc.getDouble("price") ?: 0.0,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        isAvailable = true,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        isOffer = doc.getBoolean("isOffer") == true,
                        discountPercent = (doc.getLong("discountPercent") ?: 0L).toInt(),
                        oldPriceYer = doc.getDouble("oldPriceYer") ?: doc.getDouble("oldPrice") ?: 0.0,
                        unit = doc.getString("unit") ?: ""
                    )
                }
                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun addProduct(product: ProductItemEntity): Result<String> {
        return try {
            val id = product.id.trim().ifBlank { UUID.randomUUID().toString() }
            val createdAt = if (product.createdAt > 0L) product.createdAt else System.currentTimeMillis()
            val safePrice = if (product.priceYer.isNaN() || product.priceYer.isInfinite() || product.priceYer < 0.0) 0.0 else product.priceYer
            val safeOldPrice = if (product.oldPriceYer.isNaN() || product.oldPriceYer.isInfinite() || product.oldPriceYer < 0.0) 0.0 else product.oldPriceYer
            val safeDiscount = product.discountPercent.coerceIn(0, 99)
            val map = mapOf(
                "id" to id,
                "ownerId" to product.ownerId.trim(),
                "storeId" to product.ownerId.trim(),
                "title" to product.title.trim(),
                "name" to product.title.trim(),
                "description" to product.description.trim(),
                "category" to product.category.trim(),
                "subCategory" to product.category.trim(),
                "priceYer" to safePrice,
                "price" to safePrice,
                "imageUrl" to product.imageUrl.trim(),
                "isAvailable" to product.isAvailable,
                "isDeleted" to false,
                "isOffer" to (product.isOffer || safeDiscount > 0),
                "discountPercent" to safeDiscount,
                "oldPriceYer" to safeOldPrice,
                "oldPrice" to safeOldPrice,
                "unit" to product.unit.trim(),
                "createdAt" to createdAt
            )
            firestore.collection(AppConstants.COL_PRODUCTS).document(id).set(map).await()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProduct(product: ProductItemEntity): Result<Unit> {
        return try {
            val cleanId = product.id.trim()
            if (cleanId.isBlank()) {
                return Result.failure(IllegalArgumentException("معرف المنتج غير صالح"))
            }
            val safePrice = if (product.priceYer.isNaN() || product.priceYer.isInfinite() || product.priceYer < 0.0) 0.0 else product.priceYer
            val safeOldPrice = if (product.oldPriceYer.isNaN() || product.oldPriceYer.isInfinite() || product.oldPriceYer < 0.0) 0.0 else product.oldPriceYer
            val safeDiscount = product.discountPercent.coerceIn(0, 99)
            val map = mapOf(
                "title" to product.title.trim(),
                "name" to product.title.trim(),
                "description" to product.description.trim(),
                "category" to product.category.trim(),
                "subCategory" to product.category.trim(),
                "priceYer" to safePrice,
                "price" to safePrice,
                "imageUrl" to product.imageUrl.trim(),
                "isAvailable" to product.isAvailable,
                "isOffer" to (product.isOffer || safeDiscount > 0),
                "discountPercent" to safeDiscount,
                "oldPriceYer" to safeOldPrice,
                "oldPrice" to safeOldPrice,
                "unit" to product.unit.trim()
            )
            firestore.collection(AppConstants.COL_PRODUCTS).document(cleanId).update(map).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            val cleanId = productId.trim()
            if (cleanId.isBlank()) {
                return Result.failure(IllegalArgumentException("معرف المنتج غير صالح"))
            }
            firestore.collection(AppConstants.COL_PRODUCTS).document(cleanId).delete().await()
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
    context: Context? = null
) : IRatingsRepository {

    private val firestore = FirebaseFirestore.getInstance()

    override fun getTargetRatings(targetId: String): Flow<List<RatingReviewEntity>> = callbackFlow {
        val cleanTargetId = targetId.trim()
        if (cleanTargetId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection(AppConstants.COL_RATINGS)
            .whereEqualTo("targetId", cleanTargetId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    val entity = try {
                        doc.toObject(com.example.data.RatingEntity::class.java)?.copy(id = doc.id)
                    } catch (_: Exception) {
                        null
                    }
                    RatingReviewEntity(
                        id = doc.id,
                        targetId = cleanTargetId,
                        authorName = doc.getString("userName")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("authorName")?.takeIf { it.isNotBlank() }
                            ?: entity?.userName?.takeIf { it.isNotBlank() }
                            ?: "عميل",
                        authorPhone = doc.getString("userPhone")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("authorPhone")
                            ?: entity?.userPhone
                            ?: "",
                        rating = doc.getDouble("rating") ?: entity?.rating?.toDouble() ?: 5.0,
                        comment = doc.getString("comment") ?: doc.getString("review") ?: entity?.comment ?: "",
                        dateTimestamp = doc.getLong("timestamp") ?: doc.getLong("dateTimestamp") ?: entity?.timestamp ?: System.currentTimeMillis(),
                        ownerReply = doc.getString("ownerReply") ?: doc.getString("replyText") ?: ""
                    )
                }
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun addRating(rating: RatingReviewEntity): Result<String> {
        return try {
            val cleanTargetId = rating.targetId.trim()
            if (cleanTargetId.isBlank()) {
                return Result.failure(IllegalArgumentException("معرف الجهة المقيمة غير صالح"))
            }
            val id = rating.id.trim().ifBlank { com.example.utils.EntityIdGenerator.generateReviewId() }
            val now = if (rating.dateTimestamp > 0L) rating.dateTimestamp else System.currentTimeMillis()
            val safeRating = if (rating.rating.isNaN() || rating.rating.isInfinite()) 5.0 else rating.rating.coerceIn(1.0, 5.0)
            val inferredTargetType = when {
                cleanTargetId.startsWith("p_", ignoreCase = true) || cleanTargetId.endsWith("_PROVIDER", ignoreCase = true) -> "PROVIDER"
                cleanTargetId.startsWith("prop_", ignoreCase = true) || cleanTargetId.endsWith("_PROPERTY", ignoreCase = true) -> "PROPERTY"
                cleanTargetId.endsWith("_RESTAURANT", ignoreCase = true) -> "RESTAURANT"
                cleanTargetId.endsWith("_MEDICAL", ignoreCase = true) -> "MEDICAL"
                else -> "STORE"
            }
            val map = mapOf(
                "id" to id,
                "targetId" to cleanTargetId,
                "targetType" to inferredTargetType,
                "userName" to rating.authorName.trim().ifBlank { "عميل" },
                "authorName" to rating.authorName.trim().ifBlank { "عميل" },
                "userPhone" to rating.authorPhone.trim(),
                "authorPhone" to rating.authorPhone.trim(),
                "rating" to safeRating,
                "qualityRating" to safeRating,
                "speedRating" to safeRating,
                "professionalismRating" to safeRating,
                "priceFairnessRating" to safeRating,
                "comment" to rating.comment.trim(),
                "isApproved" to true,
                "timestamp" to now,
                "dateTimestamp" to now
            )
            firestore.collection(AppConstants.COL_RATINGS).document(id).set(map).await()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun replyToRating(ratingId: String, replyText: String): Result<Unit> {
        val cleanId = ratingId.trim()
        val cleanReply = replyText.trim()
        if (cleanId.isBlank() || cleanReply.isBlank()) {
            return Result.failure(IllegalArgumentException("نص الرد ومعرف التقييم مطلوبان"))
        }
        return try {
            firestore.collection(AppConstants.COL_RATINGS).document(cleanId)
                .update(
                    mapOf(
                        "ownerReply" to cleanReply,
                        "replyText" to cleanReply,
                        "repliedAt" to System.currentTimeMillis()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 📦 GalleryRepositoryImpl
 */
class GalleryRepositoryImpl(
    context: Context? = null
) : IGalleryRepository {

    private val firestore = FirebaseFirestore.getInstance()

    override fun getOwnerGallery(ownerId: String): Flow<List<GalleryAlbumEntity>> = callbackFlow {
        val cleanOwnerId = ownerId.trim()
        if (cleanOwnerId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("galleries")
            .whereEqualTo("ownerId", cleanOwnerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    @Suppress("UNCHECKED_CAST")
                    GalleryAlbumEntity(
                        id = doc.id,
                        ownerId = cleanOwnerId,
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
            val id = album.id.trim().ifBlank { UUID.randomUUID().toString() }
            val createdAt = if (album.createdAt > 0L) album.createdAt else System.currentTimeMillis()
            val map = mapOf(
                "id" to id,
                "ownerId" to album.ownerId.trim(),
                "title" to album.title.trim().ifBlank { "معرض الصور" },
                "imageUrls" to album.imageUrls.filter { it.isNotBlank() },
                "createdAt" to createdAt
            )
            firestore.collection("galleries").document(id).set(map).await()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteGalleryAlbum(albumId: String): Result<Unit> {
        return try {
            val cleanId = albumId.trim()
            if (cleanId.isBlank()) {
                return Result.failure(IllegalArgumentException("معرف الألبوم غير صالح"))
            }
            firestore.collection("galleries").document(cleanId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
