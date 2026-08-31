package com.example.viewmodels

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.InstantRequestEntity
import com.example.data.models.RequestOfferEntity
import com.example.data.repositories.InstantRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class InstantRequestUiState {
    object Idle : InstantRequestUiState()
    object Loading : InstantRequestUiState()
    data class Success(val message: String) : InstantRequestUiState()
    data class Error(val message: String) : InstantRequestUiState()
}

/**
 * ⚡ InstantRequestViewModel
 * إدارة كاملة لمنطق الطلبات الفورية، التدفق المباشر والعروض التنافسية.
 */
@HiltViewModel
class InstantRequestViewModel @Inject constructor(
    private val instantRequestRepository: InstantRequestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<InstantRequestUiState>(InstantRequestUiState.Idle)
    val uiState: StateFlow<InstantRequestUiState> = _uiState.asStateFlow()

    private val _instantRequests = MutableStateFlow<List<InstantRequestEntity>>(emptyList())
    val instantRequests: StateFlow<List<InstantRequestEntity>> = _instantRequests.asStateFlow()

    private val _offers = MutableStateFlow<List<RequestOfferEntity>>(emptyList())
    val offers: StateFlow<List<RequestOfferEntity>> = _offers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun observeUserRequests(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            instantRequestRepository.getUserInstantRequests(userId).collect { list ->
                _isLoading.value = false
                _instantRequests.value = list
            }
        }
    }

    fun observeAvailableRequests(category: String = "", city: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            instantRequestRepository.getAvailableInstantRequests(category, city).collect { list ->
                _isLoading.value = false
                _instantRequests.value = list
            }
        }
    }

    fun createInstantRequest(request: InstantRequestEntity, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = InstantRequestUiState.Loading
            instantRequestRepository.createInstantRequest(
                request = request,
                onSuccess = { created ->
                    _isLoading.value = false
                    _uiState.value = InstantRequestUiState.Success("تم إنشاء الطلب الفوري بنجاح")
                    onResult(true, created.requestCode)
                },
                onError = { err ->
                    _isLoading.value = false
                    _uiState.value = InstantRequestUiState.Error(err)
                    _errorMessage.value = err
                    onResult(false, err)
                }
            )
        }
    }

    fun submitOffer(offer: RequestOfferEntity, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            instantRequestRepository.submitOffer(
                offer = offer,
                onSuccess = {
                    onResult(true, "تم تقديم العرض بنجاح")
                },
                onError = { err ->
                    _errorMessage.value = err
                    onResult(false, err)
                }
            )
        }
    }

    fun acceptOffer(
        requestId: String,
        offerId: String,
        providerId: String,
        providerName: String,
        providerPhone: String,
        price: Double,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            instantRequestRepository.acceptOffer(
                requestId = requestId,
                offerId = offerId,
                providerId = providerId,
                providerName = providerName,
                providerPhone = providerPhone,
                acceptedPrice = price,
                onSuccess = {
                    onResult(true, "تم قبول العرض بنجاح")
                },
                onError = { err ->
                    _errorMessage.value = err
                    onResult(false, err)
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        instantRequestRepository.clearListeners()
    }
}
