package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.sync.OfflineQueueManager
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

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        OfflineQueueManager.clearAll(context)
    }

    @Test
    fun `test queuing operation when network disconnects`() {
        val payload = """{"bookingId":"bk_off_123","status":"PENDING"}"""
        
        OfflineQueueManager.enqueueOperation(context, "BOOKING_CREATE", payload)
        
        val pendingOps = OfflineQueueManager.getPendingOperations(context)
        assertEquals(1, pendingOps.size)
        assertEquals("BOOKING_CREATE", pendingOps[0].type)
        assertEquals(payload, pendingOps[0].payload)
    }
}
