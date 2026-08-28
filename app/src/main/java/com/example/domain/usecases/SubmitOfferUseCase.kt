package com.example.domain.usecases

import com.example.data.models.RequestOfferEntity
import com.example.data.repositories.OfferRepository

/**
 * 🎯 SubmitOfferUseCase
 * Handles technician offer submission and validation.
 */
class SubmitOfferUseCase(private val offerRepository: OfferRepository) {

    operator fun invoke(
        offer: RequestOfferEntity,
        onSuccess: (RequestOfferEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        if (offer.price <= 0) {
            onError("يرجى إدخال سعر العرض المقترح بشكل صحيح")
            return
        }
        if (offer.technicianName.isBlank()) {
            onError("يرجى إدخال اسم مقدم العرض")
            return
        }
        if (offer.technicianPhone.isBlank()) {
            onError("يرجى إدخال رقم الهاتف للتواصل")
            return
        }

        offerRepository.submitOffer(
            offer = offer,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}
