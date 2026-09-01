package com.example.domain.usecases

import com.example.data.BookingEntity
import com.example.data.repositories.BookingRepository

/**
 * 🎯 CancelBookingUseCase
 * Enforces business rules:
 * - 8-hour cancellation countdown rule
 * - SHA-256 PIN / Password verification
 * - 3 failed attempts lockout prevention (5 minutes lockout)
 */
class CancelBookingUseCase(private val bookingRepository: BookingRepository) {

    operator fun invoke(
        booking: BookingEntity,
        inputPinOrPassword: String,
        cancellationReason: String = "طلب العميل إلغاء الموعد",
        cancelledBy: String = "USER",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (inputPinOrPassword.isBlank()) {
            onError("يرجى إدخال رمز PIN أو كلمة مرور الحجز لتأكيد الإلغاء")
            return
        }

        bookingRepository.cancelBookingWithSecurity(
            booking = booking,
            inputPinOrPassword = inputPinOrPassword,
            cancellationReason = cancellationReason,
            cancelledBy = cancelledBy,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}
