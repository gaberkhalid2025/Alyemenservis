package com.example.domain.usecases

import com.example.data.repositories.IRegistrationRepository
import com.example.domain.entities.JoinStatusEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 🎯 GetJoinStatusUseCase
 */
class GetJoinStatusUseCase @Inject constructor(
    private val repository: IRegistrationRepository
) {
    operator fun invoke(phone: String): Flow<JoinStatusEntity?> {
        return repository.getJoinStatusFlow(ValidatePhoneUseCase.normalizePhone(phone))
    }
}
