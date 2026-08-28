package com.example.domain.usecases

/**
 * 🔒 ValidatePhoneUseCase
 * Validates Yemeni phone numbers format (77, 73, 71, 70, 78) - 9 digits.
 */
class ValidatePhoneUseCase {
    operator fun invoke(phone: String): ValidationResult {
        val clean = phone.trim()
        if (clean.isEmpty()) {
            return ValidationResult(isValid = false, errorMessage = "يرجى إدخال رقم الهاتف")
        }
        val digitsOnly = clean.replace("+967", "").replace("00967", "").filter { it.isDigit() }
        if (digitsOnly.length != 9) {
            return ValidationResult(isValid = false, errorMessage = "يجب أن يتكون رقم الهاتف من 9 أرقام (مثال: 771234567)")
        }
        val validPrefixes = listOf("77", "73", "71", "70", "78")
        if (validPrefixes.none { digitsOnly.startsWith(it) }) {
            return ValidationResult(isValid = false, errorMessage = "رمز مزود الخدمة غير معروف (يجب أن يبدأ بـ 77, 73, 71, 70, 78)")
        }
        return ValidationResult(isValid = true)
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String = ""
    )
}
