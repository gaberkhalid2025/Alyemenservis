package com.example.data.repositories.contracts

import com.example.data.models.InstantRequestEntity
import com.example.data.models.RequestOfferEntity
import com.example.data.utils.AppResult
import kotlinx.coroutines.flow.Flow

interface IUrgentRequestRepository {
    fun clearListeners()
    suspend fun createInstantRequest(request: InstantRequestEntity): AppResult<InstantRequestEntity>
    fun getUserInstantRequests(userId: String): Flow<List<InstantRequestEntity>>
    fun getAvailableInstantRequests(category: String = "", city: String = ""): Flow<List<InstantRequestEntity>>
    suspend fun submitOffer(offer: RequestOfferEntity): AppResult<Unit>
    suspend fun acceptOffer(
        requestId: String,
        offerId: String,
        providerId: String,
        providerName: String,
        providerPhone: String,
        acceptedPrice: Double
    ): AppResult<Unit>
    suspend fun cancelInstantRequest(requestId: String, userPin: String = ""): AppResult<Unit>
    suspend fun completeInstantRequest(requestId: String): AppResult<Unit>
}
