package com.example.domain.usecases

import com.example.data.models.InstantRequestEntity
import com.example.data.repositories.InstantRequestRepository
import com.example.utils.AppError
import com.example.utils.AppResult
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class CreateInstantRequestUseCase @Inject constructor(
    private val repository: InstantRequestRepository
) {
    suspend operator fun invoke(request: InstantRequestEntity): AppResult<InstantRequestEntity> =
        suspendCancellableCoroutine { cont ->
            try {
                repository.createInstantRequest(
                    request = request,
                    onSuccess = {
                        if (cont.isActive) cont.resume(AppResult.Success(it))
                    },
                    onError = {
                        if (cont.isActive) cont.resume(AppResult.Error(AppError.UnknownError(it)))
                    }
                )
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(AppResult.Error(AppError.UnknownError(e.localizedMessage ?: "فشل إنشاء الطلب العاجل", e)))
            }
        }
}
