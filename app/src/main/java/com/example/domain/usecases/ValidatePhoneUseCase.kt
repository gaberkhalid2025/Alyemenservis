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
        var digitsOnly = clean.filter { it.isDigit() }
        if (digitsOnly.startsWith("00967")) {
            digitsOnly = digitsOnly.substring(5)
        } else if (digitsOnly.startsWith("967")) {
            digitsOnly = digitsOnly.substring(3)
        } else if (digitsOnly.startsWith("0") && digitsOnly.length == 10) {
            digitsOnly = digitsOnly.substring(1)
        }

        if (digitsOnly.length != 9) {
            return ValidationResult(isValid = false, errorMessage = "يجب أن يتكون رقم الهاتف من 9 أرقام (مثال: 771234567)")
        }
        val validPrefixes = listOf("77", "73", "71", "70", "78")
        if (validPrefixes.none { digitsOnly.startsWith(it) }) {
            return ValidationResult(isValid = false, errorMessage = "رمز مزود الخدمة غير معروف (يجب أن يبدأ بـ 77, 73, 71, 70, 78)")
        }
        return ValidationResult(isValid = true)
    }

    companion object {
        fun normalizePhone(phone: String): String {
            if (phone.isBlank()) return phone
            val digits = phone.filter { it.isDigit() }
            if (digits.length < 7) return phone

            var processed = digits
            if (processed.startsWith("00967")) {
                processed = processed.substring(5)
            } else if (processed.startsWith("967")) {
                processed = processed.substring(3)
            }

            if (processed.startsWith("0") && processed.length == 10) {
                processed = processed.substring(1)
            }

            if (processed.length == 9) {
                return processed
            }

            if (processed.length > 9) {
                return processed.takeLast(9)
            }

            return phone
        }
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String = ""
    )
}
