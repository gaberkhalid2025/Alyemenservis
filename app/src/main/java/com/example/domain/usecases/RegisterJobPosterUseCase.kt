package com.example.domain.usecases

import com.example.data.repositories.IRegistrationRepository
import com.example.domain.entities.RegistrationEntity
import com.example.utils.Validators
import javax.inject.Inject

/**
 * 🎯 RegisterJobPosterUseCase - منطق عمل تسجيل معلن الوظائف أو الشركات
 *
 * @param repository مستودع التسجيل
 * @param validatePhone التحقق من صحة رقم الهاتف
 * @param validatePassword التحقق من كلمة المرور
 */
class RegisterJobPosterUseCase @Inject constructor(
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

        val passCheck = validatePassword(job.rawPassword)
        if (!passCheck.isValid) {
            return Result.failure(IllegalArgumentException(passCheck.errorMessage))
        }

        if (job.city.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى تحديد المدينة/المحافظة"))
        }

        if (job.contactEmail.isNotBlank()) {
            val emailCheck = Validators.validateEmail(job.contactEmail)
            if (!emailCheck.isValid) {
                return Result.failure(IllegalArgumentException(emailCheck.message))
            }
        }

        val normalized = job.copy(
            jobTitle = job.jobTitle.trim(),
            companyName = job.companyName.trim(),
            category = job.category.trim(),
            contactPhone = ValidatePhoneUseCase.normalizePhone(job.contactPhone),
            contactEmail = job.contactEmail.trim(),
            city = job.city.trim(),
            requirements = job.requirements.trim(),
            salaryRange = job.salaryRange.trim(),
            rawPassword = job.rawPassword.trim()
        )
        return repository.registerJob(normalized)
    }
}
