package com.example.domain.usecases

import com.example.data.repositories.contracts.IRegistrationRepository
import com.example.domain.entities.RegistrationEntity

/**
 * 🎯 RegisterProviderUseCase
 */
class RegisterProviderUseCase(
    private val repository: IRegistrationRepository,
    private val validatePhone: ValidatePhoneUseCase = ValidatePhoneUseCase(),
    private val validatePassword: ValidatePasswordUseCase = ValidatePasswordUseCase()
) {
    suspend operator fun invoke(provider: RegistrationEntity.Provider): Result<String> {
        if (provider.fullName.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم الفني الكامل"))
        }

        val phoneCheck = validatePhone(provider.phone)
        if (!phoneCheck.isValid) {
            return Result.failure(IllegalArgumentException(phoneCheck.errorMessage))
        }

        val passCheck = validatePassword(provider.passwordHash)
        if (!passCheck.isValid) {
            return Result.failure(IllegalArgumentException(passCheck.errorMessage))
        }

        if (provider.professionCategory.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى اختيار تخصص المهنة/الحرفة"))
        }

        if (provider.city.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى تحديد المدينة/المحافظة"))
        }

        return repository.registerProvider(provider)
    }
}
