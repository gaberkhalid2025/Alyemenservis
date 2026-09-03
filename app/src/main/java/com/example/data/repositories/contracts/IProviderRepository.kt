package com.example.data.repositories.contracts

import com.example.data.ProviderEntity
import com.example.data.utils.AppResult
import kotlinx.coroutines.flow.Flow

interface IProviderRepository {
    fun clearListeners()
    fun observeApprovedProviders(): Flow<List<ProviderEntity>>
    fun observeProviderById(providerId: String): Flow<ProviderEntity?>
    suspend fun getProvidersByCategoryAndCity(categoryId: String, cityId: String = ""): AppResult<List<ProviderEntity>>
    suspend fun saveOrUpdateProvider(provider: ProviderEntity): AppResult<ProviderEntity>
    suspend fun updateAvailability(providerId: String, isAvailable: Boolean): AppResult<Unit>
    suspend fun deleteProvider(providerId: String): AppResult<Unit>
}
