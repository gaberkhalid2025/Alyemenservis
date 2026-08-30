package com.example.ui.screens.bookings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BookingDetailsViewModel : ViewModel() {
    private val _bookingStatus = MutableStateFlow("قيد الانتظار")
    val bookingStatus: StateFlow<String> = _bookingStatus.asStateFlow()

    fun updateStatus(newStatus: String) {
        _bookingStatus.value = newStatus
    }
}
