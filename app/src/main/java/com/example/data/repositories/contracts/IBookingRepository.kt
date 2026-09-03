package com.example.data.repositories.contracts

import com.example.data.BookingEntity
import com.example.data.utils.AppResult
import kotlinx.coroutines.flow.Flow

interface IBookingRepository {
    fun getBookingsFlow(userId: String, isProvider: Boolean = false): Flow<List<BookingEntity>>
    fun observeBookings(): Flow<List<BookingEntity>>
    fun clearListeners()
    suspend fun createBooking(booking: BookingEntity, rawPasswordPin: String = ""): AppResult<BookingEntity>
    suspend fun updateBookingStatus(bookingId: String, newStatus: String): AppResult<Unit>
    suspend fun deleteBooking(bookingId: String): AppResult<Unit>
    suspend fun deleteAllBookings(): AppResult<Unit>
    suspend fun updateBooking(booking: BookingEntity, inputPin: String = ""): AppResult<Unit>
    suspend fun cancelByUser(booking: BookingEntity, inputPin: String, cancellationReason: String): AppResult<Unit>
    suspend fun attemptCancel(booking: BookingEntity, inputPin: String, cancellationReason: String): AppResult<Unit>
    suspend fun cancelByTechnician(bookingId: String, cancellationReason: String): AppResult<Unit>
    suspend fun cancelByAdmin(bookingId: String, cancellationReason: String): AppResult<Unit>
    fun getStatusColor(status: String): String
    fun getStatusLabel(status: String): String
    fun getProgress(status: String): Float
    suspend fun createDirectBooking(booking: BookingEntity): AppResult<BookingEntity>
    suspend fun updateBookingStatusEnum(bookingId: String, newStatus: String): AppResult<Unit>
}
