package com.example.domain.usecases

import com.example.data.PendingProviderEntity
import com.example.data.repositories.IStatusRepository
import javax.inject.Inject

class ApproveJoinRequestUseCase @Inject constructor(
    private val statusRepository: IStatusRepository
) {

    suspend operator fun invoke(request: PendingProviderEntity): Result<Unit> {
        return statusRepository.approveJoinRequest(request)
    }
}

