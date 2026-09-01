package com.example.data.repositories.impl

import android.content.Context
import com.example.data.repositories.contracts.IProductsRepository
import com.example.data.utils.AppResult
import com.example.domain.entities.ProductItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ProductsRepositoryImpl(private val context: Context? = null) : IProductsRepository {
    override fun getOwnerProducts(ownerId: String): Flow<List<ProductItemEntity>> = flowOf(emptyList())
    override fun getAllAvailableProducts(): Flow<List<ProductItemEntity>> = flowOf(emptyList())
    override suspend fun addProduct(product: ProductItemEntity): AppResult<ProductItemEntity> = AppResult.success(product)
    override suspend fun addProduct(product: Any): AppResult<Unit> = AppResult.success(Unit)
    override suspend fun deleteProduct(productId: String): AppResult<Unit> = AppResult.success(Unit)
}
