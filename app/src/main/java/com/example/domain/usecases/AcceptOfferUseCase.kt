package com.example.domain.usecases

import com.example.data.repositories.OfferRepository
import com.example.data.repositories.RequestRepository

/**
 * 🎯 AcceptOfferUseCase
 * Coordinates accepting an offer across both OfferRepository and RequestRepository.
 */
class AcceptOfferUseCase(
    private val offerRepository: OfferRepository,
    private val requestRepository: RequestRepository
) {

    operator fun invoke(
        requestId: String,
        offerId: String,
        technicianId: String,
        technicianName: String,
        technicianPhone: String,
        acceptedPrice: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (requestId.isBlank() || offerId.isBlank()) {
            onError("بيانات الطلب أو العرض غير صحيحة")
            return
        }

        offerRepository.acceptOffer(
            requestId = requestId,
            acceptedOfferId = offerId,
            onSuccess = {
                requestRepository.acceptRequestOffer(
                    requestId = requestId,
                    offerId = offerId,
                    technicianId = technicianId,
                    technicianName = technicianName,
                    technicianPhone = technicianPhone,
                    acceptedPrice = acceptedPrice,
                    onSuccess = onSuccess,
                    onError = onError
                )
            },
            onError = onError
        )
    }
}
