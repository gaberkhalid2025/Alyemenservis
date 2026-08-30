package com.example.ui.screens.bookings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BookingItem(
    val id: String,
    val serviceName: String,
    val date: String,
    val status: String
)

class BookingListViewModel : ViewModel() {
    private val _bookings = MutableStateFlow<List<BookingItem>>(emptyList())
    val bookings: StateFlow<List<BookingItem>> = _bookings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadBookings() {
        _isLoading.value = true
        // Load bookings logic
        _isLoading.value = false
    }
}
