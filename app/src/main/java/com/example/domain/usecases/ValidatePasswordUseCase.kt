package com.example.domain.usecases

import com.example.utils.Validators

/**
 * 🔒 ValidatePasswordUseCase
 * Ensures strong password/PIN requirements (minimum 7 characters with letters and numbers).
 */
class ValidatePasswordUseCase {
    operator fun invoke(password: String): ValidationResult {
        val clean = password.trim()
        if (clean.isEmpty()) {
            return ValidationResult(isValid = false, errorMessage = "يرجى إدخال كلمة المرور")
        }
        val validation = Validators.validatePassword(clean)
        return ValidationResult(
            isValid = validation.isValid,
            errorMessage = validation.errorMessage ?: ""
        )
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String = ""
    )
}
