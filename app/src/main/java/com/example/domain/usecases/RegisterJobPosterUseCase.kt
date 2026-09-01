package com.example.domain.usecases

import com.example.data.repositories.IRegistrationRepository
import com.example.domain.entities.RegistrationEntity

/**
 * 🎯 RegisterJobPosterUseCase - منطق عمل تسجيل معلن الوظائف أو الشركات
 *
 * @param repository مستودع التسجيل
 * @param validatePhone التحقق من صحة رقم الهاتف
 * @param validatePassword التحقق من كلمة المرور
 */
class RegisterJobPosterUseCase(
    private val repository: IRegistrationRepository,
    private val validatePhone: ValidatePhoneUseCase = ValidatePhoneUseCase(),
    private val validatePassword: ValidatePasswordUseCase = ValidatePasswordUseCase()
) {
    /**
     * تنفيذ طلب تسجيل إعلان الوظيفة
     *
     * @param job نموذج بيانات الوظيفة
     * @return [Result] يحوي معرف الطلب المولد أو الاستثناء
     */
    suspend operator fun invoke(job: RegistrationEntity.Job): Result<String> {
        if (job.jobTitle.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال المسمى الوظيفي المطلوب"))
        }

        if (job.companyName.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم الشركة أو معلن الوظيفة"))
        }

        val phoneCheck = validatePhone(job.contactPhone)
        if (!phoneCheck.isValid) {
            return Result.failure(IllegalArgumentException(phoneCheck.errorMessage))
        }

        val passCheck = validatePassword(job.passwordHash)
        if (!passCheck.isValid) {
            return Result.failure(IllegalArgumentException(passCheck.errorMessage))
        }

        if (job.city.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى تحديد المدينة/المحافظة"))
        }

        return repository.registerJob(job)
    }
}
