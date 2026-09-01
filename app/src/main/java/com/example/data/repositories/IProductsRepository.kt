package com.example.data.repositories

import com.example.data.utils.AppResult
import com.example.domain.entities.ProductItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface IProductsRepository {
    fun getOwnerProducts(ownerId: String): Flow<List<ProductItemEntity>> = flowOf(emptyList())
    fun getAllAvailableProducts(): Flow<List<ProductItemEntity>> = flowOf(emptyList())
    suspend fun addProduct(product: ProductItemEntity): AppResult<ProductItemEntity> = AppResult.success(product)
    suspend fun addProduct(product: Any): AppResult<Unit> = AppResult.success(Unit)
    suspend fun deleteProduct(productId: String): AppResult<Unit> = AppResult.success(Unit)
}
