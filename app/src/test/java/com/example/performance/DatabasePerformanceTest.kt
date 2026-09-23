package com.example.performance

import com.example.data.BookingEntity
import org.junit.Assert.*
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * ⚡ DatabasePerformanceTest
 * Measures execution time for bulk operations on core data structures to ensure responsiveness.
 */
class DatabasePerformanceTest {

    @Test
    fun `test bulk booking processing performance`() {
        val count = 1000
        val bookings = mutableListOf<BookingEntity>()

        val generationTime = measureTimeMillis {
            for (i in 1..count) {
                bookings.add(
                    BookingEntity(
                        id = "bk_perf_$i",
                        customerName = "عميل $i",
                        customerPhone = "77123$i",
                        customerArea = "صنعاء",
                        date = "2026-06-01",
                        time = "10:00",
                        serviceName = "خدمة $i",
                        status = "PENDING",
                        totalAmount = 1000.0 * i,
                        currency = "YER"
                    )
                )
            }
        }

        assertTrue("1000 bookings creation should take less than 1000ms", generationTime < 1000)
        assertEquals(1000, bookings.size)

        val searchTime = measureTimeMillis {
            val filtered = bookings.filter { it.totalAmount > 500000.0 }
            assertNotNull(filtered)
        }

        assertTrue("Filtering 1000 items should take less than 200ms", searchTime < 200)
    }
}
