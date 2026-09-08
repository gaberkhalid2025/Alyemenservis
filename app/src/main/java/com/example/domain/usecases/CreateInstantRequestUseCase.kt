package com.example.domain.usecases

import com.example.data.models.InstantRequestEntity
import com.example.data.repositories.InstantRequestRepository
import com.example.utils.AppError
import com.example.utils.AppResult
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CreateInstantRequestUseCase @Inject constructor(
    private val repository: InstantRequestRepository
) {
    suspend operator fun invoke(request: InstantRequestEntity): AppResult<InstantRequestEntity> =
        suspendCoroutine { cont ->
            try {
                repository.createInstantRequest(
                    request = request,
                    onSuccess = { cont.resume(AppResult.Success(it)) },
                    onError = { cont.resume(AppResult.Error(AppError.UnknownError(it))) }
                )
            } catch (e: Exception) {
                cont.resume(AppResult.Error(AppError.UnknownError(e.localizedMessage ?: "فشل إنشاء الطلب العاجل", e)))
            }
        }
}
