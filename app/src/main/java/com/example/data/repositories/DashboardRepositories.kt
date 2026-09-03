package com.example.data.repositories

import android.content.Context
import com.example.domain.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface IDashboardRepository {
    fun getDashboardStats(ownerId: String, type: String): Flow<DashboardStatsEntity>
}

interface IProductsRepository {
    fun getOwnerProducts(ownerId: String): Flow<List<ProductItemEntity>>
    fun getAllAvailableProducts(): Flow<List<ProductItemEntity>>
    suspend fun addProduct(product: ProductItemEntity): Result<Unit>
    suspend fun deleteProduct(productId: String): Result<Unit>
}

interface IRatingsRepository {
    fun getOwnerRatings(ownerId: String): Flow<List<RatingReviewEntity>>
    fun getTargetRatings(targetId: String): Flow<List<RatingReviewEntity>>
}

interface IGalleryRepository {
    fun getGalleryAlbums(ownerId: String): Flow<List<GalleryAlbumEntity>>
    fun getOwnerGallery(ownerId: String): Flow<List<String>>
}

interface IFavoritesRepository {
    fun getUserFavorites(userId: String): Flow<List<FavoriteItemEntity>>
    suspend fun isFavorite(userId: String, targetId: String): Boolean
    suspend fun addFavorite(favorite: FavoriteItemEntity): Result<Unit>
    suspend fun removeFavorite(userId: String, targetId: String): Result<Unit>
}

class DashboardRepositoryImpl(private val context: Context?) : IDashboardRepository {
    override fun getDashboardStats(ownerId: String, type: String): Flow<DashboardStatsEntity> {
        return flowOf(DashboardStatsEntity(totalViews = 150, activeBookingsCount = 3, averageRating = 4.9))
    }
}

class ProductsRepositoryImpl(private val context: Context?) : IProductsRepository {
    override fun getOwnerProducts(ownerId: String): Flow<List<ProductItemEntity>> = flowOf(emptyList())
    override fun getAllAvailableProducts(): Flow<List<ProductItemEntity>> = flowOf(emptyList())
    override suspend fun addProduct(product: ProductItemEntity): Result<Unit> = Result.success(Unit)
    override suspend fun deleteProduct(productId: String): Result<Unit> = Result.success(Unit)
}

class RatingsRepositoryImpl(private val context: Context?) : IRatingsRepository {
    override fun getOwnerRatings(ownerId: String): Flow<List<RatingReviewEntity>> = flowOf(emptyList())
    override fun getTargetRatings(targetId: String): Flow<List<RatingReviewEntity>> = flowOf(emptyList())
}

class GalleryRepositoryImpl(private val context: Context?) : IGalleryRepository {
    override fun getGalleryAlbums(ownerId: String): Flow<List<GalleryAlbumEntity>> = flowOf(emptyList())
    override fun getOwnerGallery(ownerId: String): Flow<List<String>> = flowOf(emptyList())
}

class FavoritesRepositoryImpl(private val context: Context?) : IFavoritesRepository {
    override fun getUserFavorites(userId: String): Flow<List<FavoriteItemEntity>> = flowOf(emptyList())
    override suspend fun isFavorite(userId: String, targetId: String): Boolean = false
    override suspend fun addFavorite(favorite: FavoriteItemEntity): Result<Unit> = Result.success(Unit)
    override suspend fun removeFavorite(userId: String, targetId: String): Result<Unit> = Result.success(Unit)
}

typealias ChatRepository = com.example.data.repositories.impl.ChatRepositoryImpl
