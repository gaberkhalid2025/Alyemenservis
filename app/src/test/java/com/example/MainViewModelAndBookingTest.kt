package com.example

import com.example.data.models.BookingEntity
import com.example.data.repositories.contracts.IBookingRepository
import com.example.ui.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MainViewModelAndBookingTest {

    @Test
    fun testBookingStatusCalculations() {
        // Test status label and color mapping helper logic
        assertEquals("مؤكد", com.example.data.repositories.impl.BookingRepositoryImpl.getStatusLabelStatic("CONFIRMED"))
        assertEquals("قيد الانتظار", com.example.data.repositories.impl.BookingRepositoryImpl.getStatusLabelStatic("PENDING"))
        assertEquals("ملغي", com.example.data.repositories.impl.BookingRepositoryImpl.getStatusLabelStatic("CANCELLED"))
        
        val progress = com.example.data.repositories.impl.BookingRepositoryImpl.getProgressStatic("COMPLETED")
        assertEquals(1.0f, progress, 0.01f)
    }

    @Test
    fun testMainViewModelInitialState() {
        // Verify ViewModel default states without crashing
        // Note: MainViewModel initializes with application context
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = MainViewModel(context)
        
        assertNotNull(viewModel.notifications)
        assertNotNull(viewModel.bookings)
        assertNotNull(viewModel.stores)
        assertNotNull(viewModel.properties)
        assertNotNull(viewModel.jobs)
        assertNotNull(viewModel.chatChannels)
        
        // Test notification trigger
        viewModel.triggerNotification("اختبار إشعار تجريبي")
        runBlocking {
            val list = viewModel.notifications.first()
            assertTrue(list.any { it.title.contains("اختبار إشعار تجريبي") })
        }
    }
}
