package com.example.data.repositories.contracts

import com.example.data.utils.AppResult
import kotlinx.coroutines.flow.Flow

interface IAdminRepository {
    suspend fun approveJoinRequest(entityId: String, entityType: String): AppResult<Unit>
    suspend fun rejectJoinRequest(entityId: String, reason: String): AppResult<Unit>
    suspend fun blockEntity(entityId: String, entityType: String): AppResult<Unit>
    suspend fun unblockEntity(entityId: String, entityType: String): AppResult<Unit>
    suspend fun deleteEntity(entityId: String, entityType: String): AppResult<Unit>
    suspend fun restoreEntity(entityId: String, entityType: String): AppResult<Unit>
    fun getSystemMetrics(): Flow<AppResult<Map<String, Long>>>
    suspend fun managePermissions(userId: String, permissions: List<String>): AppResult<Unit>
}
