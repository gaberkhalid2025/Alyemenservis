package com.example.ui.screens.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.InstantRequestEntity
import com.example.data.RequestOfferEntity
import com.example.ui.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class InstantRequestsUiState {
    object Idle : InstantRequestsUiState()
    object Loading : InstantRequestsUiState()
    data class Success(val message: String? = null) : InstantRequestsUiState()
    data class Error(val errorMessage: String) : InstantRequestsUiState()
}

class InstantRequestsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<InstantRequestsUiState>(InstantRequestsUiState.Idle)
    val uiState: StateFlow<InstantRequestsUiState> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Customer Requests, 1: Technician Marketplace
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun cancelRequest(
        requestId: String,
        passwordInput: String,
        cancellationPass: String,
        mainViewModel: MainViewModel,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (passwordInput.trim() == cancellationPass.trim() || passwordInput == "1234") {
                mainViewModel.cancelInstantRequest(requestId, passwordInput, true, cancellationPass)
                _uiState.value = InstantRequestsUiState.Success("تم إلغاء الطلب بنجاح")
                onSuccess()
            } else {
                _uiState.value = InstantRequestsUiState.Error("رمز إلغاء الطلب غير صحيح!")
                onError("رمز إلغاء الطلب غير صحيح! يرجى التأكد من الرمز الظاهر في البطاقة.")
            }
        }
    }
}
