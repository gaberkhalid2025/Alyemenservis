package com.example.domain.usecases

import com.example.data.models.InstantRequestEntity
import com.example.data.repositories.RequestRepository

/**
 * 🎯 CreateInstantRequestUseCase
 * Business logic for validating and publishing urgent service requests.
 */
class CreateInstantRequestUseCase(private val requestRepository: RequestRepository) {

    operator fun invoke(
        request: InstantRequestEntity,
        onSuccess: (InstantRequestEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        if (request.serviceTitle.isBlank() && request.description.isBlank()) {
            onError("يرجى وصف المشكلة أو نوع الخدمة المطلوبة")
            return
        }
        if (request.userPhone.isBlank()) {
            onError("يرجى إدخال رقم الهاتف للتواصل")
            return
        }
        if (request.userCity.isBlank()) {
            onError("يرجى تحديد المدينة")
            return
        }

        requestRepository.createInstantRequest(
            request = request,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}
