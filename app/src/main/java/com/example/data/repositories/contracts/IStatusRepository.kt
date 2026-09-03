package com.example.data.repositories.contracts

import com.example.data.BookingEntity
import com.example.data.NotificationEntity
import com.example.data.PendingProviderEntity
import com.example.data.models.InstantRequestEntity
import com.example.data.models.SystemStatusMetrics
import com.example.data.utils.AppResult
import kotlinx.coroutines.flow.Flow

interface IStatusRepository {
    fun getSystemMetricsFlow(): Flow<SystemStatusMetrics>
    fun getPendingJoinRequestsFlow(): Flow<List<PendingProviderEntity>>
    fun getSystemBookingsFlow(): Flow<List<BookingEntity>>
    fun getInstantRequestsFlow(): Flow<List<InstantRequestEntity>>
    fun getNotificationsFlow(): Flow<List<NotificationEntity>>
    suspend fun refreshSystemStatus(): AppResult<Unit>
    suspend fun approveJoinRequest(request: PendingProviderEntity): AppResult<Unit>
    suspend fun rejectJoinRequest(request: PendingProviderEntity, reason: String = ""): AppResult<Unit>
    suspend fun clearNotifications(): AppResult<Unit>
}
