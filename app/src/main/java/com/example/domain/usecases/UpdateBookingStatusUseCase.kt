package com.example.domain.usecases

import com.example.data.repositories.BookingRepository

/**
 * 🎯 UpdateBookingStatusUseCase
 * Updates the progression state of a booking (APPROVED, IN_PROGRESS, COMPLETED, REJECTED).
 */
class UpdateBookingStatusUseCase(private val bookingRepository: BookingRepository) {

    operator fun invoke(
        bookingId: String,
        newStatus: String,
        currentStatus: String = "",
        userRole: String = "USER",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (bookingId.isBlank()) {
            onError("معرف الحجز غير صالح")
            return
        }
        if (currentStatus.isNotBlank() && !com.example.utils.BookingStateMachine.canTransition(currentStatus, newStatus)) {
            onError("انتقال غير مسموح من الحالة ($currentStatus) إلى ($newStatus)")
            return
        }
        bookingRepository.updateBookingStatus(
            bookingId = bookingId,
            newStatus = newStatus,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}
