package com.example.domain.usecases

import com.example.data.repositories.IRegistrationRepository
import com.example.domain.entities.RegistrationEntity

/**
 * 🎯 RegisterMedicalUseCase - منطق عمل تسجيل المركز الطبي أو الطبيب
 *
 * @param repository مستودع التسجيل
 * @param validatePhone التحقق من صحة رقم الهاتف
 * @param validatePassword التحقق من كلمة المرور
 */
class RegisterMedicalUseCase(
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

        val passCheck = validatePassword(medical.passwordHash)
        if (!passCheck.isValid) {
            return Result.failure(IllegalArgumentException(passCheck.errorMessage))
        }

        if (medical.specialtyCategory.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى تحديد التخصص الطبي الرئيسي"))
        }

        if (medical.city.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى اختيار المدينة/المحافظة"))
        }

        return repository.registerMedicalCenter(medical)
    }
}
