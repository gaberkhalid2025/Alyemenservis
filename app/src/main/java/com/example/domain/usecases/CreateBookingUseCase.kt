package com.example.domain.usecases

import com.example.data.BookingEntity
import com.example.data.repositories.BookingRepository

/**
 * 🎯 CreateBookingUseCase
 * Handles booking creation validation, code generation, and storage.
 */
class CreateBookingUseCase(private val bookingRepository: BookingRepository) {

    operator fun invoke(
        booking: BookingEntity,
        rawPasswordPin: String = "",
        onSuccess: (BookingEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        if (booking.customerName.isBlank() && booking.clientName.isBlank()) {
            onError("يرجى إدخال اسم العميل الكامل")
            return
        }
        if (booking.customerPhone.isBlank() && booking.clientPhone.isBlank()) {
            onError("يرجى إدخال رقم هاتف للتواصل")
            return
        }
        if (booking.date.isBlank() && booking.dateString.isBlank()) {
            onError("يرجى تحديد تاريخ الموعد")
            return
        }
        if (booking.time.isBlank() && booking.timeString.isBlank()) {
            onError("يرجى تحديد وقت الموعد")
            return
        }

        bookingRepository.createBooking(
            booking = booking,
            rawPasswordPin = rawPasswordPin,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}
