package com.example.domain.usecases

import com.example.data.PendingProviderEntity
import com.example.data.repositories.IStatusRepository
import javax.inject.Inject

class ApproveJoinRequestUseCase @Inject constructor(
    private val statusRepository: IStatusRepository
) {

    suspend operator fun invoke(request: PendingProviderEntity): Result<Unit> {
        if (request.id.isBlank()) {
            return Result.failure(IllegalArgumentException("معرف طلب الانضمام غير صالح"))
        }
        val normalized = request.copy(
            id = request.id.trim(),
            name = request.name.trim(),
            phone = ValidatePhoneUseCase.normalizePhone(request.phone)
        )
        return statusRepository.approveJoinRequest(normalized)
    }
}

