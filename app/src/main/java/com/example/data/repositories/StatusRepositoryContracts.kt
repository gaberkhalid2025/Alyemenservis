package com.example.data.repositories

import com.example.data.BookingEntity
import com.example.data.NotificationEntity
import com.example.data.PendingProviderEntity
import com.example.data.models.InstantRequestEntity
import kotlinx.coroutines.flow.Flow

/**
 * 🏛️ Domain Entity for System Overview Metrics
 */
data class SystemStatusMetrics(
    val providersCount: Int = 0,
    val storesCount: Int = 0,
    val propertiesCount: Int = 0,
    val instantRequestsCount: Int = 0,
    val bookingsCount: Int = 0,
    val pendingJoinRequestsCount: Int = 0,
    val unreadNotificationsCount: Int = 0,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

/**
 * 🏛️ Contract Interface: IStatusRepository
 */
interface IStatusRepository {
    fun getSystemMetricsFlow(): Flow<SystemStatusMetrics>
    fun getPendingJoinRequestsFlow(): Flow<List<PendingProviderEntity>>
    fun getSystemBookingsFlow(): Flow<List<BookingEntity>>
    fun getInstantRequestsFlow(): Flow<List<InstantRequestEntity>>
    fun getNotificationsFlow(): Flow<List<NotificationEntity>>
    suspend fun approveJoinRequest(request: PendingProviderEntity): Result<Unit>
    suspend fun rejectJoinRequest(request: PendingProviderEntity, reason: String = ""): Result<Unit>
    suspend fun clearNotifications(): Result<Unit>
    suspend fun refreshSystemStatus(): Result<Unit>
}

/**
 * 🏛️ Contract Interface: IUrgentRepository
 */
interface IUrgentRepository {
    fun getUrgentRequestsFlow(userId: String, isProvider: Boolean): Flow<List<InstantRequestEntity>>
    fun getUrgentRequestDetailsFlow(requestId: String): Flow<InstantRequestEntity?>
    fun getOffersForUrgentRequestFlow(requestId: String): Flow<List<com.example.data.models.RequestOfferEntity>>
    suspend fun createUrgentRequest(request: InstantRequestEntity): Result<String>
    suspend fun submitUrgentOffer(offer: com.example.data.models.RequestOfferEntity): Result<String>
    suspend fun acceptUrgentOffer(requestId: String, offerId: String, providerPhone: String): Result<Unit>
    suspend fun cancelUrgentRequest(requestId: String, userPin: String): Result<Unit>
}
