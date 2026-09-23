package com.example.domain.usecases

/**
 * 🔒 ValidatePhoneUseCase
 * Validates Yemeni phone numbers format (77, 73, 71, 70, 78) - 9 digits.
 */
class ValidatePhoneUseCase {
    operator fun invoke(phone: String): ValidationResult {
        val sanitized = phone.replace(" ", "").replace("-", "").trim()
        if (sanitized.isEmpty()) {
            return ValidationResult(isValid = false, errorMessage = "يرجى إدخال رقم الهاتف")
        }

        var processed = sanitized
        // Remove international code prefixes
        if (processed.startsWith("+967")) {
            processed = processed.substring(4)
        } else if (processed.startsWith("00967")) {
            processed = processed.substring(5)
        } else if (processed.startsWith("967")) {
            if (processed.length == 12 && (processed.substring(3).startsWith("77") || processed.substring(3).startsWith("73") || processed.substring(3).startsWith("71") || processed.substring(3).startsWith("70") || processed.substring(3).startsWith("78"))) {
                processed = processed.substring(3)
            }
        }

        // Remove leading 0 if it is followed by 9 digits
        if (processed.startsWith("0") && processed.length == 10) {
            processed = processed.substring(1)
        }

        // If contains non-digit characters, it is invalid
        if (processed.any { !it.isDigit() }) {
            return ValidationResult(isValid = false, errorMessage = "يجب أن يتكون رقم الهاتف من أرقام فقط")
        }

        // Verify length is exactly 9
        if (processed.length != 9) {
            return ValidationResult(isValid = false, errorMessage = "يجب أن يتكون رقم الهاتف من 9 أرقام (مثال: 771234567)")
        }

        // Verify prefix is valid
        val validPrefixes = listOf("77", "73", "71", "70", "78")
        if (validPrefixes.none { processed.startsWith(it) }) {
            return ValidationResult(isValid = false, errorMessage = "رمز مزود الخدمة غير معروف (يجب أن يبدأ بـ 77, 73, 71, 70, 78)")
        }

        return ValidationResult(isValid = true)
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String = ""
    )
}
