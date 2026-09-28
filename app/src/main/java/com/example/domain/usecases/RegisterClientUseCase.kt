package com.example.domain.usecases

import com.example.data.repositories.IRegistrationRepository
import com.example.domain.entities.RegistrationEntity
import javax.inject.Inject

/**
 * 🎯 RegisterClientUseCase
 */
class RegisterClientUseCase @Inject constructor(
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

        val passCheck = validatePassword(client.rawPassword)
        if (!passCheck.isValid) {
            return Result.failure(IllegalArgumentException(passCheck.errorMessage))
        }

        if (client.city.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى اختيار المحافظة/المدينة"))
        }

        val normalized = client.copy(
            fullName = client.fullName.trim(),
            phone = ValidatePhoneUseCase.normalizePhone(client.phone),
            city = client.city.trim(),
            rawPassword = client.rawPassword.trim(),
            profileImageUrl = client.profileImageUrl.trim()
        )
        return repository.registerClient(normalized)
    }
}
