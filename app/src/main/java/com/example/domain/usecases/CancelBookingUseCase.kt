package com.example.domain.usecases

import com.example.data.BookingEntity
import com.example.data.repositories.BookingRepository
import javax.inject.Inject

class CancelBookingUseCase @Inject constructor(
    private val repository: BookingRepository
) {
    fun execute(
        booking: BookingEntity,
        inputPin: String,
        reason: String,
        cancelledBy: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        repository.cancelBookingWithSecurity(booking, inputPin, reason, cancelledBy, onSuccess, onError)
    }
}
