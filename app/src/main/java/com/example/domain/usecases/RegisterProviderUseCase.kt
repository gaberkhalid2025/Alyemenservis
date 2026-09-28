package com.example.domain.usecases

import com.example.data.repositories.IRegistrationRepository
import com.example.domain.entities.RegistrationEntity
import javax.inject.Inject

/**
 * 🎯 RegisterProviderUseCase
 */
class RegisterProviderUseCase @Inject constructor(
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

        val passCheck = validatePassword(provider.rawPassword)
        if (!passCheck.isValid) {
            return Result.failure(IllegalArgumentException(passCheck.errorMessage))
        }

        if (provider.professionCategory.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى اختيار تخصص المهنة/الحرفة"))
        }

        if (provider.city.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى تحديد المدينة/المحافظة"))
        }

        if (provider.experienceYears < 0) {
            return Result.failure(IllegalArgumentException("عدد سنوات الخبرة غير صالح"))
        }

        val normalized = provider.copy(
            fullName = provider.fullName.trim(),
            phone = ValidatePhoneUseCase.normalizePhone(provider.phone),
            professionCategory = provider.professionCategory.trim(),
            city = provider.city.trim(),
            bio = provider.bio.trim(),
            identityDocumentUrl = provider.identityDocumentUrl.trim(),
            licenseNumber = provider.licenseNumber.trim(),
            rawPassword = provider.rawPassword.trim()
        )
        return repository.registerProvider(normalized)
    }
}
