package com.example.ui.screens.bookings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

class BookingCalendarViewModel : ViewModel() {
    private val _selectedDate = MutableStateFlow(Date())
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()

    private val _availableSlots = MutableStateFlow(listOf("09:00 AM", "10:30 AM", "01:00 PM", "03:30 PM", "06:00 PM"))
    val availableSlots: StateFlow<List<String>> = _availableSlots.asStateFlow()

    fun selectDate(date: Date) {
        _selectedDate.value = date
    }
}
