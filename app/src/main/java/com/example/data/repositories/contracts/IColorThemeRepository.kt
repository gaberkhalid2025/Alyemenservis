package com.example.data.repositories.contracts

import com.example.data.utils.AppResult
import kotlinx.coroutines.flow.Flow

interface IColorThemeRepository {
    fun getThemePreference(): Flow<String>
    suspend fun setThemePreference(theme: String): AppResult<Unit>
    suspend fun syncThemeWithServer(userId: String, theme: String): AppResult<Unit>
    suspend fun fetchThemeFromServer(userId: String): AppResult<String>
}
