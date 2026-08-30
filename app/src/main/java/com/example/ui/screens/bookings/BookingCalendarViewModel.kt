package com.example.ui.screens.bookings

import androidx.lifecycle.ViewModel
import com.example.data.BookingEntity
import com.example.data.ProviderEntity
import com.example.util.HolidayManager
import com.example.util.ScheduleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

data class BookingCalendarUiState(
    val calendarMonthOffset: Int = 0,
    val selectedDateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val selectedTimeSlot: ScheduleManager.TimeSlot? = null,
    val recurrenceOption: String = "NONE", // "NONE", "WEEKLY", "MONTHLY"
    val clientNotes: String = "",
    val clientAddress: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

class BookingCalendarViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BookingCalendarUiState())
    val uiState: StateFlow<BookingCalendarUiState> = _uiState.asStateFlow()

    fun nextMonth() {
        _uiState.value = _uiState.value.copy(calendarMonthOffset = _uiState.value.calendarMonthOffset + 1)
    }

    fun previousMonth() {
        if (_uiState.value.calendarMonthOffset > 0) {
            _uiState.value = _uiState.value.copy(calendarMonthOffset = _uiState.value.calendarMonthOffset - 1)
        }
    }

    fun selectDate(dateString: String) {
        _uiState.value = _uiState.value.copy(
            selectedDateString = dateString,
            selectedTimeSlot = null
        )
    }

    fun selectTimeSlot(slot: ScheduleManager.TimeSlot?) {
        _uiState.value = _uiState.value.copy(selectedTimeSlot = slot)
    }

    fun setRecurrenceOption(option: String) {
        _uiState.value = _uiState.value.copy(recurrenceOption = option)
    }

    fun setClientNotes(notes: String) {
        _uiState.value = _uiState.value.copy(clientNotes = notes)
    }

    fun setClientAddress(address: String) {
        _uiState.value = _uiState.value.copy(clientAddress = address)
    }

    fun setSubmitting(isSubmitting: Boolean) {
        _uiState.value = _uiState.value.copy(isSubmitting = isSubmitting)
    }

    fun setErrorMessage(message: String?) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }
}
