package com.example.domain.usecases

/**
 * 🔒 ValidatePasswordUseCase
 * Ensures strong password/PIN requirements (minimum 4 characters).
 */
class ValidatePasswordUseCase {
    operator fun invoke(password: String): ValidationResult {
        val clean = password.trim()
        if (clean.isEmpty()) {
            return ValidationResult(isValid = false, errorMessage = "يرجى إدخال كلمة المرور أو رمز PIN")
        }
        if (clean.length < 4) {
            return ValidationResult(isValid = false, errorMessage = "يجب أن تحتوي كلمة المرور على 4 خانات على الأقل")
        }
        return ValidationResult(isValid = true)
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String = ""
    )
}
