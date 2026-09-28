package com.example.domain.usecases

import com.example.data.repositories.IRegistrationRepository
import com.example.domain.entities.RegistrationEntity
import javax.inject.Inject

/**
 * 🎯 RegisterPropertyUseCase - منطق عمل تسجيل إعلان العقار أو مكتب العقارات
 *
 * @param repository مستودع التسجيل
 * @param validatePhone التحقق من صحة رقم الهاتف
 * @param validatePassword التحقق من كلمة المرور
 */
class RegisterPropertyUseCase @Inject constructor(
    private val repository: IRegistrationRepository,
    private val validatePhone: ValidatePhoneUseCase = ValidatePhoneUseCase(),
    private val validatePassword: ValidatePasswordUseCase = ValidatePasswordUseCase()
) {
    /**
     * تنفيذ طلب تسجيل العقار
     *
     * @param property نموذج بيانات العقار
     * @return [Result] يحوي المعرف أو الاستثناء
     */
    suspend operator fun invoke(property: RegistrationEntity.Property): Result<String> {
        if (property.title.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال عنوان الإعلان العقاري"))
        }

        if (property.ownerName.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم صاحب العقار/الوكيل"))
        }

        val phoneCheck = validatePhone(property.phone)
        if (!phoneCheck.isValid) {
            return Result.failure(IllegalArgumentException(phoneCheck.errorMessage))
        }

        val passCheck = validatePassword(property.rawPassword)
        if (!passCheck.isValid) {
            return Result.failure(IllegalArgumentException(passCheck.errorMessage))
        }

        if (property.city.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى تحديد المدينة/المحافظة"))
        }

        if (property.priceYer < 0.0 || property.priceYer.isNaN() || property.priceYer.isInfinite()) {
            return Result.failure(IllegalArgumentException("سعر العقار غير صالح"))
        }

        val normalized = property.copy(
            title = property.title.trim(),
            propertyType = property.propertyType.trim(),
            category = property.category.trim(),
            ownerName = property.ownerName.trim(),
            phone = ValidatePhoneUseCase.normalizePhone(property.phone),
            city = property.city.trim(),
            areaDetails = property.areaDetails.trim(),
            description = property.description.trim(),
            rawPassword = property.rawPassword.trim()
        )
        return repository.registerProperty(normalized)
    }
}
