package com.example.data.repositories

import com.example.data.models.InstantRequestEntity
import com.example.data.models.RequestOfferEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class InstantRequestRepository {
    fun getUserInstantRequests(userId: String): Flow<List<InstantRequestEntity>> = flowOf(emptyList())
    fun getAvailableInstantRequests(category: String = "", city: String = ""): Flow<List<InstantRequestEntity>> = flowOf(emptyList())
    fun createInstantRequest(
        request: InstantRequestEntity,
        onSuccess: (InstantRequestEntity) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        onSuccess(request)
    }
    fun submitOffer(
        offer: RequestOfferEntity,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        onSuccess()
    }
    fun acceptOffer(
        requestId: String,
        offerId: String,
        providerId: String,
        providerName: String,
        providerPhone: String,
        acceptedPrice: Double,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        onSuccess()
    }
    fun clearListeners() {}
}
