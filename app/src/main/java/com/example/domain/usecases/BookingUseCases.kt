package com.example.domain.usecases

import com.example.data.BookingEntity
import com.example.data.repositories.BookingRepository
import kotlinx.coroutines.flow.Flow

class GetBookingsUseCase(private val repository: BookingRepository) {
    operator fun invoke(userId: String, isProvider: Boolean = false): Flow<List<BookingEntity>> {
        return repository.getBookingsFlow(userId, isProvider)
    }
}

class UpdateBookingUseCase(private val repository: BookingRepository) {
    operator fun invoke(
        updatedBooking: BookingEntity,
        inputPin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        repository.updateBookingDetails(updatedBooking, inputPin, onSuccess, onError)
    }
}
