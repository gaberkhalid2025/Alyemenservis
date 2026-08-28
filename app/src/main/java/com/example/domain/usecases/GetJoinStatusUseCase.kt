package com.example.domain.usecases

import com.example.data.repositories.IRegistrationRepository
import com.example.domain.entities.JoinStatusEntity
import kotlinx.coroutines.flow.Flow

/**
 * 🎯 GetJoinStatusUseCase
 */
class GetJoinStatusUseCase(
    private val repository: IRegistrationRepository
) {
    operator fun invoke(phone: String): Flow<JoinStatusEntity?> {
        return repository.getJoinStatusFlow(phone)
    }
}
