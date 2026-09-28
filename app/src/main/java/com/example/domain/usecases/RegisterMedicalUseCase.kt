package com.example.domain.usecases

import com.example.data.repositories.IRegistrationRepository
import com.example.domain.entities.RegistrationEntity
import javax.inject.Inject

/**
 * 🎯 RegisterMedicalUseCase - منطق عمل تسجيل المركز الطبي أو الطبيب
 *
 * @param repository مستودع التسجيل
 * @param validatePhone التحقق من صحة رقم الهاتف
 * @param validatePassword التحقق من كلمة المرور
 */
class RegisterMedicalUseCase @Inject constructor(
    private val repository: IRegistrationRepository,
    private val validatePhone: ValidatePhoneUseCase = ValidatePhoneUseCase(),
    private val validatePassword: ValidatePasswordUseCase = ValidatePasswordUseCase()
) {
    /**
     * تنفيذ طلب تسجيل المركز الطبي/العيادة
     *
     * @param medical نموذج بيانات المركز الطبي
     * @return [Result] يحوي المعرف أو الاستثناء
     */
    suspend operator fun invoke(medical: RegistrationEntity.MedicalCenter): Result<String> {
        if (medical.centerName.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم العيادة أو المركز الطبي"))
        }

        if (medical.doctorName.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم الطبيب المسؤول"))
        }

        val phoneCheck = validatePhone(medical.phone)
        if (!phoneCheck.isValid) {
            return Result.failure(IllegalArgumentException(phoneCheck.errorMessage))
        }

        val passCheck = validatePassword(medical.rawPassword)
        if (!passCheck.isValid) {
            return Result.failure(IllegalArgumentException(passCheck.errorMessage))
        }

        if (medical.specialtyCategory.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى تحديد التخصص الطبي الرئيسي"))
        }

        if (medical.city.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى اختيار المدينة/المحافظة"))
        }

        val normalized = medical.copy(
            centerName = medical.centerName.trim(),
            specialtyCategory = medical.specialtyCategory.trim(),
            doctorName = medical.doctorName.trim(),
            phone = ValidatePhoneUseCase.normalizePhone(medical.phone),
            city = medical.city.trim(),
            addressDetails = medical.addressDetails.trim(),
            licenseNumber = medical.licenseNumber.trim(),
            logoUrl = medical.logoUrl.trim(),
            rawPassword = medical.rawPassword.trim()
        )
        return repository.registerMedicalCenter(normalized)
    }
}
