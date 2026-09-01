package com.example.domain.usecases

import com.example.data.repositories.IRegistrationRepository
import com.example.domain.entities.RegistrationEntity

/**
 * 🎯 RegisterClientUseCase
 */
class RegisterClientUseCase(
    private val repository: IRegistrationRepository,
    private val validatePhone: ValidatePhoneUseCase = ValidatePhoneUseCase(),
    private val validatePassword: ValidatePasswordUseCase = ValidatePasswordUseCase()
) {
    suspend operator fun invoke(client: RegistrationEntity.Client): Result<String> {
        if (client.fullName.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال الاسم الكامل"))
        }

        val phoneCheck = validatePhone(client.phone)
        if (!phoneCheck.isValid) {
            return Result.failure(IllegalArgumentException(phoneCheck.errorMessage))
        }

        val passCheck = validatePassword(client.passwordHash)
        if (!passCheck.isValid) {
            return Result.failure(IllegalArgumentException(passCheck.errorMessage))
        }

        if (client.city.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى اختيار المحافظة/المدينة"))
        }

        return repository.registerClient(client)
    }
}
