package com.example.data.repositories.contracts

import com.example.data.StoreEntity
import com.example.data.utils.AppResult
import kotlinx.coroutines.flow.Flow

interface IStoreRepository {
    fun clearListeners()
    fun observeAllStores(): Flow<List<StoreEntity>>
    fun observeStoresBySection(sectionId: String): Flow<List<StoreEntity>>
    suspend fun getStoreById(storeId: String): AppResult<StoreEntity?>
    suspend fun saveOrUpdateStore(store: StoreEntity): AppResult<StoreEntity>
    suspend fun deleteStore(storeId: String): AppResult<Unit>
}
