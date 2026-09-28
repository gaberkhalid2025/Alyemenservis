package com.example.domain.usecases

import com.example.data.repositories.BookingRepository
import java.util.Locale
import javax.inject.Inject

/**
 * 🎯 UpdateBookingStatusUseCase
 * Updates the progression state of a booking (ACCEPTED/APPROVED, IN_PROGRESS, COMPLETED, REJECTED).
 */
class UpdateBookingStatusUseCase @Inject constructor(
    private val bookingRepository: BookingRepository
) {

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
        val cleanNewStatus = newStatus.trim().uppercase(Locale.ROOT).let {
            if (it == "APPROVED") "ACCEPTED" else it
        }
        if (cleanNewStatus.isBlank()) {
            onError("حالة الحجز الجديدة غير صالحة")
            return
        }

        val roleUpper = userRole.trim().uppercase(Locale.ROOT)
        if ((roleUpper == "CLIENT" || roleUpper == "GUEST") &&
            cleanNewStatus in listOf("ACCEPTED", "IN_PROGRESS", "REJECTED", "CLOSED", "COMPLETED", "PAID")
        ) {
            onError("ليس لديك صلاحية لتغيير حالة الحجز إلى ($cleanNewStatus)")
            return
        }

        if (currentStatus.isNotBlank() && !com.example.utils.BookingStateMachine.canTransition(currentStatus, cleanNewStatus)) {
            onError("انتقال غير مسموح من الحالة ($currentStatus) إلى ($cleanNewStatus)")
            return
        }
        bookingRepository.updateBookingStatus(
            bookingId = bookingId.trim(),
            newStatus = cleanNewStatus,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}
