package com.example.domain.usecases

import com.example.data.models.RequestOfferEntity
import com.example.data.repositories.OfferRepository
import kotlinx.coroutines.flow.Flow

/**
 * 🎯 GetOffersForRequestUseCase
 * Retrieves offers for a specific request with proper privacy controls.
 */
class GetOffersForRequestUseCase(private val offerRepository: OfferRepository) {

    operator fun invoke(requestId: String, isCustomerOrAccepted: Boolean = false): Flow<List<RequestOfferEntity>> {
        return offerRepository.getOffersForRequestFlow(requestId, isCustomerOrAccepted)
    }
}
