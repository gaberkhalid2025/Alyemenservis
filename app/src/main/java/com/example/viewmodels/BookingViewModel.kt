package com.example.viewmodels

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BookingEntity
import com.example.data.repositories.BookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Keep
data class BookingNoteItem(
    val id: String = "",
    val bookingId: String = "",
    val note: String = "",
    val createdBy: String = "USER",
    val createdAt: Long = System.currentTimeMillis()
)

sealed class BookingUiState {
    object Idle : BookingUiState()
    object Loading : BookingUiState()
    data class Success(val message: String) : BookingUiState()
    data class Error(val message: String) : BookingUiState()
}

/**
 * 📅 BookingViewModel
 * إدارة كاملة لمنطق نظام الحجوزات الذكي، الحماية، الصلاحيات، التحديثات المباشرة، الإلغاء الآمن وقاعدة الـ 8 ساعات.
 */
@HiltViewModel
class BookingViewModel @Inject constructor(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BookingUiState>(BookingUiState.Idle)
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    private val _bookings = MutableStateFlow<List<BookingEntity>>(emptyList())
    val bookings: StateFlow<List<BookingEntity>> = _bookings.asStateFlow()

    private val _selectedBooking = MutableStateFlow<BookingEntity?>(null)
    val selectedBooking: StateFlow<BookingEntity?> = _selectedBooking.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        observeAllBookings()
    }

    fun observeAllBookings(userId: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            bookingRepository.getBookingsFlow(userId).collect { list ->
                _isLoading.value = false
                _bookings.value = list
            }
        }
    }

    fun observeUserBookings(phoneOrId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            bookingRepository.getUserBookings(phoneOrId).collect { list ->
                _isLoading.value = false
                _bookings.value = list
            }
        }
    }

    fun createBooking(booking: BookingEntity, rawPasswordPin: String = "", onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = BookingUiState.Loading
            bookingRepository.createBooking(
                booking = booking,
                rawPasswordPin = rawPasswordPin,
                onSuccess = { createdBooking ->
                    _isLoading.value = false
                    _uiState.value = BookingUiState.Success("تم إرسال الحجز بنجاح برقم #${createdBooking.bookingNumber}")
                    _successMessage.value = "تم إرسال الحجز بنجاح"
                    onResult(true, createdBooking.bookingNumber)
                },
                onError = { err ->
                    _isLoading.value = false
                    _uiState.value = BookingUiState.Error(err)
                    _errorMessage.value = err
                    onResult(false, err)
                }
            )
        }
    }

    fun updateBookingStatus(bookingId: String, status: String, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            bookingRepository.updateBookingStatus(
                bookingId = bookingId,
                newStatus = status,
                onSuccess = {
                    onResult?.invoke(true)
                },
                onError = { err ->
                    _errorMessage.value = err
                    onResult?.invoke(false)
                }
            )
        }
    }

    fun cancelBooking(booking: BookingEntity, passwordPin: String, reason: String = "طلب العميل", onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            bookingRepository.cancelBookingWithSecurity(
                booking = booking,
                inputPinOrPassword = passwordPin,
                cancellationReason = reason,
                cancelledBy = "CLIENT",
                onSuccess = {
                    _isLoading.value = false
                    _successMessage.value = "تم إلغاء الحجز بنجاح"
                    onResult(true, "تم إلغاء الحجز بنجاح")
                },
                onError = { err ->
                    _isLoading.value = false
                    _errorMessage.value = err
                    onResult(false, err)
                }
            )
        }
    }

    fun selectBooking(booking: BookingEntity?) {
        _selectedBooking.value = booking
    }

    override fun onCleared() {
        super.onCleared()
        bookingRepository.clearListeners()
    }
}
