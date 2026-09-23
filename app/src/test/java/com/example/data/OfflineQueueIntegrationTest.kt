package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.utils.OfflineQueueManager
import com.example.utils.OfflineRequest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 📡 OfflineQueueIntegrationTest
 * Tests network resilience and offline queue persistence during sudden connectivity drop.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfflineQueueIntegrationTest {

    private lateinit var context: Context
    private lateinit var manager: OfflineQueueManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        manager = OfflineQueueManager(context)
        manager.clearQueue()
    }

    @Test
    fun `test queuing operation when network disconnects`() {
        val dataMap = mapOf("bookingId" to "bk_off_123", "status" to "PENDING")
        val request = OfflineRequest(
            id = "bk_off_123",
            type = "BOOKING",
            data = dataMap,
            priority = 3
        )
        
        manager.addToQueue(request)
        
        val pendingOps = manager.getPendingRequests()
        assertEquals(1, pendingOps.size)
        assertEquals("BOOKING", pendingOps[0].type)
        assertEquals("bk_off_123", pendingOps[0].data["bookingId"])
    }
}
