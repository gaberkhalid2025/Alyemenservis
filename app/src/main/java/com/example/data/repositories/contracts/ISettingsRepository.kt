package com.example.data.repositories.contracts

import com.example.data.AdminSettingsEntity
import com.example.data.utils.AppResult
import kotlinx.coroutines.flow.Flow

interface ISettingsRepository {
    fun clearListeners()
    fun observeSettings(): Flow<AdminSettingsEntity>
    suspend fun getSettings(): AppResult<AdminSettingsEntity>
    suspend fun saveSettings(settings: AdminSettingsEntity): AppResult<Unit>
    suspend fun updatePartialSettings(updates: Map<String, Any>): AppResult<Unit>
}
