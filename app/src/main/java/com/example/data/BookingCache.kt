package com.example.data

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.toEntity
import com.example.data.local.toRoomEntity

/**
 * 🗄️ BookingCache
 * تم إزالة الكاش المؤقت في الذاكرة (ConcurrentHashMap) والاعتماد حصرياً على Room Database (AppDatabase) كمصدر محلي وحيد للحجوزات.
 */
@Deprecated("Use Room AppDatabase (bookingDao) directly as the single local source of truth for bookings")
class BookingCache(private val context: Context? = null) {

    suspend fun getBookingsFromRoom(): List<BookingEntity> {
        val ctx = context ?: return emptyList()
        return AppDatabase.getInstance(ctx).bookingDao().getAllBookingsList().map { it.toEntity() }
    }

    suspend fun putBookingsToRoom(bookings: List<BookingEntity>) {
        val ctx = context ?: return
        val dao = AppDatabase.getInstance(ctx).bookingDao()
        dao.replaceAllBookings(bookings.map { it.toRoomEntity() })
    }

    suspend fun invalidateRoom() {
        val ctx = context ?: return
        AppDatabase.getInstance(ctx).bookingDao().deleteAllBookings()
    }
}
