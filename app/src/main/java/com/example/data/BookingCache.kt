package com.example.data

import java.util.concurrent.ConcurrentHashMap

/**
 * 🗄️ BookingCache
 * ذاكرة مؤقتة لطلبات الحجوزات مع صلاحية TTL مدتها 5 دقائق لتقليل استعلامات القراءة المفرطة في Firestore
 */
class BookingCache {
    private val cache = ConcurrentHashMap<String, Pair<Long, List<BookingEntity>>>()
    private val TTL = 5 * 60 * 1000L // 5 دقائق

    fun getBookings(key: String): List<BookingEntity>? {
        val (timestamp, bookings) = cache[key] ?: return null
        if (System.currentTimeMillis() - timestamp > TTL) {
            cache.remove(key)
            return null
        }
        return bookings
    }

    fun putBookings(key: String, bookings: List<BookingEntity>) {
        cache[key] = System.currentTimeMillis() to bookings
    }

    fun invalidate(key: String? = null) {
        if (key != null) {
            cache.remove(key)
        } else {
            cache.clear()
        }
    }
}
