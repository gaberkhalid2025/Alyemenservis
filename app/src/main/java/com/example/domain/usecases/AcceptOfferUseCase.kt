package com.example.domain.usecases

import com.example.data.repositories.InstantRequestRepository
import com.example.utils.AppError
import com.example.utils.AppResult
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AcceptOfferUseCase @Inject constructor(
    private val repository: InstantRequestRepository
) {
    suspend operator fun invoke(
        requestId: String,
        offerId: String,
        providerId: String,
        providerName: String,
        providerPhone: String,
        acceptedPrice: Double
    ): AppResult<Unit> = suspendCoroutine { cont ->
        try {
            repository.acceptOffer(
                requestId = requestId,
                offerId = offerId,
                providerId = providerId,
                providerName = providerName,
                providerPhone = providerPhone,
                acceptedPrice = acceptedPrice,
                onSuccess = { cont.resume(AppResult.Success(Unit)) },
                onError = { cont.resume(AppResult.Error(AppError.UnknownError(it))) }
            )
        } catch (e: Exception) {
            cont.resume(AppResult.Error(AppError.UnknownError(e.localizedMessage ?: "فشل قبول العرض", e)))
        }
    }
}
