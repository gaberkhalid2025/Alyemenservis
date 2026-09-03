package com.example.domain.usecases

import com.example.data.repositories.IRegistrationRepository
import com.example.domain.entities.RegistrationEntity

/**
 * 🎯 RegisterRestaurantUseCase - منطق عمل تسجيل المطعم/الكافيه
 *
 * @param repository مستودع التسجيل
 * @param validatePhone التحقق من صحة رقم الهاتف
 * @param validatePassword التحقق من كلمة المرور
 */
class RegisterRestaurantUseCase(
    private val repository: IRegistrationRepository,
    private val validatePhone: ValidatePhoneUseCase = ValidatePhoneUseCase(),
    private val validatePassword: ValidatePasswordUseCase = ValidatePasswordUseCase()
) {
    /**
     * تنفيذ طلب تسجيل المطعم
     *
     * @param restaurant نموذج بيانات المطعم
     * @return [Result] يحوي معرف الطلب المولد أو الخطأ
     */
    suspend operator fun invoke(restaurant: RegistrationEntity.Restaurant): Result<String> {
        if (restaurant.restaurantName.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم المطعم أو البوفيه"))
        }

        if (restaurant.ownerName.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم صاحب المطعم/المدير المسؤول"))
        }

        val phoneCheck = validatePhone(restaurant.phone)
        if (!phoneCheck.isValid) {
            return Result.failure(IllegalArgumentException(phoneCheck.errorMessage))
        }

        val passCheck = validatePassword(restaurant.passwordHash)
        if (!passCheck.isValid) {
            return Result.failure(IllegalArgumentException(passCheck.errorMessage))
        }

        if (restaurant.cuisineType.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى اختيار نوع المأكولات والمطبخ"))
        }

        if (restaurant.city.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى تحديد المدينة/المحافظة"))
        }

        return repository.registerRestaurant(restaurant)
    }
}
