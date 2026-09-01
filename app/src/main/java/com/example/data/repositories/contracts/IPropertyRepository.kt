package com.example.data.repositories.contracts

import com.example.data.PropertyEntity
import com.example.data.utils.AppResult
import kotlinx.coroutines.flow.Flow

interface IPropertyRepository {
    fun clearListeners()
    fun observeAllProperties(): Flow<List<PropertyEntity>>
    fun observePropertiesByType(type: String): Flow<List<PropertyEntity>>
    suspend fun getPropertyById(propertyId: String): AppResult<PropertyEntity?>
    suspend fun saveOrUpdateProperty(property: PropertyEntity): AppResult<PropertyEntity>
    suspend fun deleteProperty(propertyId: String): AppResult<Unit>
}
