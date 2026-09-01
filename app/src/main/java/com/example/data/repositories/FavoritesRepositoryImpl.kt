package com.example.data.repositories

import android.content.Context
import com.example.data.utils.AppResult
import com.example.domain.entities.FavoriteItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FavoritesRepositoryImpl(private val context: Context? = null) : IFavoritesRepository {
    override fun getUserFavorites(userId: String): Flow<List<FavoriteItemEntity>> = flowOf(emptyList())
    override fun getUserFavorites(userId: String, type: String): Flow<List<FavoriteItemEntity>> = flowOf(emptyList())
    override fun isFavorite(userId: String, itemId: String): Flow<Boolean> = flowOf(false)
    override suspend fun addFavorite(userId: String, itemId: String): AppResult<Unit> = AppResult.success(Unit)
    override suspend fun addFavorite(item: FavoriteItemEntity): AppResult<Unit> = AppResult.success(Unit)
    override suspend fun removeFavorite(userId: String, itemId: String): AppResult<Unit> = AppResult.success(Unit)
    override suspend fun removeFavorite(itemId: String): AppResult<Unit> = AppResult.success(Unit)
}
