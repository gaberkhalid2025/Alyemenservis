package com.example.domain.usecases

import com.example.data.repositories.InstantRequestRepository
import com.example.utils.AppError
import com.example.utils.AppResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.resume

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
    ): AppResult<Unit> {
        if (requestId.isBlank() || offerId.isBlank() || providerId.isBlank()) {
            return AppResult.Error(AppError.ValidationError("offer", "بيانات العرض أو الطلب غير مكتملة"))
        }
        if (acceptedPrice < 0.0 || acceptedPrice.isNaN() || acceptedPrice.isInfinite()) {
            return AppResult.Error(AppError.ValidationError("price", "سعر العرض غير صالح"))
        }
        val cleanPhone = ValidatePhoneUseCase.normalizePhone(providerPhone)
        return withTimeoutOrNull(30_000L) {
            suspendCancellableCoroutine { cont ->
                try {
                    repository.acceptOffer(
                        requestId = requestId.trim(),
                        offerId = offerId.trim(),
                        providerId = providerId.trim(),
                        providerName = providerName.trim(),
                        providerPhone = cleanPhone,
                        acceptedPrice = acceptedPrice,
                        onSuccess = {
                            if (cont.isActive) cont.resume(AppResult.Success(Unit))
                        },
                        onError = {
                            if (cont.isActive) cont.resume(AppResult.Error(AppError.UnknownError(it)))
                        }
                    )
                } catch (e: Exception) {
                    if (cont.isActive) cont.resume(AppResult.Error(AppError.UnknownError(e.localizedMessage ?: "فشل قبول العرض", e)))
                }
            }
        } ?: AppResult.Error(AppError.UnknownError("انتهت مهلة الاتصال أثناء قبول العرض، يرجى المحاولة مرة أخرى"))
    }
}
