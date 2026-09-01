package com.example.domain.usecases

import com.example.data.repositories.IRegistrationRepository
import com.example.domain.entities.RegistrationEntity

/**
 * 🎯 RegisterStoreUseCase - منطق عمل تسجيل المتجر التجاري
 *
 * @param repository مستودع التسجيل
 * @param validatePhone التحقق من صحة رقم الهاتف
 * @param validatePassword التحقق من كلمة المرور
 */
class RegisterStoreUseCase(
    private val repository: IRegistrationRepository,
    private val validatePhone: ValidatePhoneUseCase = ValidatePhoneUseCase(),
    private val validatePassword: ValidatePasswordUseCase = ValidatePasswordUseCase()
) {
    /**
     * تنفيذ طلب تسجيل المتجر مع التحقق التام من البيانات المدخلة
     *
     * @param store نموذج بيانات المتجر
     * @return [Result] يحوي المعرف المولد أو رسالة الخطأ
     */
    suspend operator fun invoke(store: RegistrationEntity.Store): Result<String> {
        if (store.storeName.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم المتجر/المحل التجاري"))
        }

        if (store.ownerName.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم صاحب المتجر"))
        }

        val phoneCheck = validatePhone(store.phone)
        if (!phoneCheck.isValid) {
            return Result.failure(IllegalArgumentException(phoneCheck.errorMessage))
        }

        val passCheck = validatePassword(store.passwordHash)
        if (!passCheck.isValid) {
            return Result.failure(IllegalArgumentException(passCheck.errorMessage))
        }

        if (store.storeCategory.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى اختيار تصنيف نشاط المتجر"))
        }

        if (store.city.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى اختيار المدينة/المحافظة"))
        }

        return repository.registerStore(store)
    }
}
