package com.example.domain.usecases

import com.example.data.models.InstantRequestEntity
import com.example.data.repositories.InstantRequestRepository
import com.example.utils.AppError
import com.example.utils.AppResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.resume

class CreateInstantRequestUseCase @Inject constructor(
    private val repository: InstantRequestRepository
) {
    suspend operator fun invoke(request: InstantRequestEntity): AppResult<InstantRequestEntity> {
        val serviceLabel = request.serviceTitle.ifBlank {
            request.categoryName.ifBlank { request.categoryId }
        }.trim()
        if (serviceLabel.isBlank() && request.description.isBlank()) {
            return AppResult.Error(AppError.ValidationError("serviceType", "يرجى تحديد نوع الخدمة أو وصف الطلب"))
        }
        val normalizedPhone = ValidatePhoneUseCase.normalizePhone(request.userPhone)
        val normalizedRequest = request.copy(
            userPhone = normalizedPhone,
            serviceTitle = request.serviceTitle.trim().ifBlank { serviceLabel },
            categoryName = request.categoryName.trim().ifBlank { serviceLabel },
            description = request.description.trim()
        )

        return withTimeoutOrNull(30_000L) {
            suspendCancellableCoroutine { cont ->
                try {
                    repository.createInstantRequest(
                        request = normalizedRequest,
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
        } ?: AppResult.Error(AppError.UnknownError("انتهت مهلة الاتصال أثناء إرسال الطلب العاجل"))
    }
}
