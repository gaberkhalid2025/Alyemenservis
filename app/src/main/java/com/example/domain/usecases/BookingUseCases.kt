package com.example.domain.usecases

import com.example.data.BookingEntity
import com.example.data.repositories.BookingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBookingsUseCase @Inject constructor(private val repository: BookingRepository) {
    operator fun invoke(userId: String, isProvider: Boolean = false): Flow<List<BookingEntity>> {
        return repository.getBookingsFlow(userId.trim(), isProvider)
    }
}

class UpdateBookingUseCase @Inject constructor(private val repository: BookingRepository) {
    operator fun invoke(
        updatedBooking: BookingEntity,
        inputPin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (updatedBooking.id.isBlank()) {
            onError("معرف الحجز غير صالح")
            return
        }
        repository.updateBookingDetails(updatedBooking, inputPin.trim(), onSuccess, onError)
    }
}
