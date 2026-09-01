package com.example.data.repositories.contracts

import com.example.data.utils.AppResult
import kotlinx.coroutines.flow.Flow

interface IUserRepository {
    fun clearListeners()
    fun observeUserFavorites(userId: String): Flow<Set<String>>
    suspend fun toggleFavorite(userId: String, itemId: String): AppResult<Boolean>
    suspend fun isFavorite(userId: String, itemId: String): AppResult<Boolean>
    suspend fun updateUserProfile(userId: String, name: String, city: String, neighborhood: String): AppResult<Unit>
    suspend fun adjustUserPoints(userId: String, deltaPoints: Int): AppResult<Unit>
    suspend fun updateUserLocation(userId: String, lat: Double, lng: Double): AppResult<Unit>
    fun startLocationUpdates(userId: String): Flow<Pair<Double, Double>>
}
