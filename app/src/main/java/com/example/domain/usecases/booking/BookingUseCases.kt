package com.example.domain.usecases.booking

import com.example.data.BookingEntity
import com.example.data.ProviderEntity
import com.example.utils.AppResult
import com.example.utils.BookingUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// Mocks to allow compilation
class BookingRepository {
    fun getUserBookings(userId: String): Flow<List<BookingEntity>> = emptyFlow()
    fun getProviderBookings(providerId: String): Flow<List<BookingEntity>> = emptyFlow()
    suspend fun createBooking(booking: BookingEntity, rawPasswordPin: String = ""): AppResult<BookingEntity> = com.example.utils.AppResult.Success(booking)
    suspend fun updateBookingStatus(bookingId: String, newStatus: String): AppResult<Unit> = com.example.utils.AppResult.Success(Unit)
    suspend fun cancelBookingWithSecurity(booking: BookingEntity, inputPinOrPassword: String, cancellationReason: String, cancelledBy: String): AppResult<Unit> = com.example.utils.AppResult.Success(Unit)
    suspend fun updateBookingDetails(updatedBooking: BookingEntity, inputPin: String): AppResult<Unit> = com.example.utils.AppResult.Success(Unit)
    suspend fun deleteBooking(bookingId: String): AppResult<Unit> = com.example.utils.AppResult.Success(Unit)
}

class ProviderRepository {
    suspend fun getProviderById(providerId: String): ProviderEntity? = null
}

/**
 * 📅 BookingUseCases
 * UseCases موحدة لإدارة الحجوزات والمواعيد
 */
class BookingUseCases(
    private val bookingRepository: BookingRepository,
    private val providerRepository: ProviderRepository
) {
    
    fun getUserBookings(userId: String): Flow<List<BookingEntity>> {
        return bookingRepository.getUserBookings(userId)
    }
    
    fun getProviderBookings(providerId: String): Flow<List<BookingEntity>> {
        return bookingRepository.getProviderBookings(providerId)
    }
    
    suspend fun createBooking(
        booking: BookingEntity,
        rawPasswordPin: String = ""
    ): AppResult<BookingEntity> {
        val validationError = validateBooking(booking)
        if (validationError != null) {
            return com.example.utils.AppResult.Error(com.example.utils.AppError.UnknownError(validationError))
        }
        
        return try {
            val result = bookingRepository.createBooking(booking, rawPasswordPin)
            result
        } catch (e: Exception) {
            com.example.utils.AppResult.Error(com.example.utils.AppError.UnknownError(e.message ?: "فشل إنشاء الحجز"))
        }
    }
    
    suspend fun updateBookingStatus(
        bookingId: String,
        newStatus: String
    ): AppResult<Unit> {
        return try {
            bookingRepository.updateBookingStatus(bookingId, newStatus)
            com.example.utils.AppResult.Success(Unit)
        } catch (e: Exception) {
            com.example.utils.AppResult.Error(com.example.utils.AppError.UnknownError(e.message ?: "فشل تحديث حالة الحجز"))
        }
    }
    
    suspend fun cancelBookingWithSecurity(
        booking: BookingEntity,
        inputPinOrPassword: String,
        cancellationReason: String,
        cancelledBy: String
    ): AppResult<Unit> {
        return try {
            bookingRepository.cancelBookingWithSecurity(
                booking = booking,
                inputPinOrPassword = inputPinOrPassword,
                cancellationReason = cancellationReason,
                cancelledBy = cancelledBy
            )
            com.example.utils.AppResult.Success(Unit)
        } catch (e: Exception) {
            com.example.utils.AppResult.Error(com.example.utils.AppError.UnknownError(e.message ?: "فشل إلغاء الحجز"))
        }
    }
    
    suspend fun updateBookingDetails(
        updatedBooking: BookingEntity,
        inputPin: String
    ): AppResult<Unit> {
        return try {
            bookingRepository.updateBookingDetails(updatedBooking, inputPin)
            com.example.utils.AppResult.Success(Unit)
        } catch (e: Exception) {
            com.example.utils.AppResult.Error(com.example.utils.AppError.UnknownError(e.message ?: "فشل تحديث الحجز"))
        }
    }
    
    suspend fun deleteBooking(bookingId: String): AppResult<Unit> {
        return try {
            bookingRepository.deleteBooking(bookingId)
            com.example.utils.AppResult.Success(Unit)
        } catch (e: Exception) {
            com.example.utils.AppResult.Error(com.example.utils.AppError.UnknownError(e.message ?: "فشل حذف الحجز"))
        }
    }
    
    private fun validateBooking(booking: BookingEntity): String? {
        if (booking.customerName.isBlank() && booking.clientName.isBlank()) {
            return "يرجى إدخال اسم العميل"
        }
        if (booking.customerPhone.isBlank() && booking.clientPhone.isBlank()) {
            return "يرجى إدخال رقم الهاتف"
        }
        if (booking.date.isBlank() && booking.dateString.isBlank()) {
            return "يرجى تحديد التاريخ"
        }
        if (booking.time.isBlank() && booking.timeString.isBlank()) {
            return "يرجى تحديد الوقت"
        }
        if (booking.providerId.isBlank()) {
            return "يرجى تحديد مقدم الخدمة"
        }
        return null
    }
}
