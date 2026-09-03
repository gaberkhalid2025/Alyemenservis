package com.example.data.repositories.contracts

import com.example.data.CategoryEntity
import com.example.data.utils.AppResult
import kotlinx.coroutines.flow.Flow

interface ICategoryRepository {
    fun observeCategories(): Flow<AppResult<List<CategoryEntity>>>
}
