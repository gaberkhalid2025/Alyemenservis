package com.example.data.repositories.contracts

import com.example.data.utils.AppResult
import com.example.domain.entities.FavoriteItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface IFavoritesRepository {
    fun getUserFavorites(userId: String): Flow<List<FavoriteItemEntity>> = flowOf(emptyList())
    fun getUserFavorites(userId: String, type: String): Flow<List<FavoriteItemEntity>> = flowOf(emptyList())
    fun isFavorite(userId: String, itemId: String): Flow<Boolean> = flowOf(false)
    suspend fun addFavorite(userId: String, itemId: String): AppResult<Unit> = AppResult.success(Unit)
    suspend fun addFavorite(item: FavoriteItemEntity): AppResult<Unit> = AppResult.success(Unit)
    suspend fun removeFavorite(userId: String, itemId: String): AppResult<Unit> = AppResult.success(Unit)
    suspend fun removeFavorite(itemId: String): AppResult<Unit> = AppResult.success(Unit)
}
