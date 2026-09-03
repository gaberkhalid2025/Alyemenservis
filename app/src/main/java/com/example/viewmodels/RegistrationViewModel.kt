package com.example.viewmodels

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.JoinRequestEntity
import com.example.data.repositories.RegistrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RegistrationUiState {
    object Idle : RegistrationUiState()
    object Loading : RegistrationUiState()
    data class Success(val message: String) : RegistrationUiState()
    data class Error(val message: String) : RegistrationUiState()
}

/**
 * 📝 RegistrationViewModel
 * إدارة كاملة لمنطق طلبات انضمام المزودين والمحلات، المراجعة، والتأكيد/الرفض.
 */
@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val registrationRepository: RegistrationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Idle)
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<JoinRequestEntity>>(emptyList())
    val pendingRequests: StateFlow<List<JoinRequestEntity>> = _pendingRequests.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun submitJoinRequest(request: JoinRequestEntity, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = RegistrationUiState.Loading
            val result = registrationRepository.submitJoinRequest(request)
            _isLoading.value = false
            if (result.isSuccess) {
                _uiState.value = RegistrationUiState.Success("تم تقديم طلب الانضمام بنجاح")
                onResult(true, "تم تقديم طلب الانضمام بنجاح")
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "فشل تقديم طلب الانضمام"
                _uiState.value = RegistrationUiState.Error(err)
                _errorMessage.value = err
                onResult(false, err)
            }
        }
    }

    fun observeAllJoinRequests() {
        viewModelScope.launch {
            _isLoading.value = true
            registrationRepository.observeAllJoinRequests().collect { list ->
                _isLoading.value = false
                _pendingRequests.value = list
            }
        }
    }

    fun approveRequest(requestId: String, adminId: String = "admin", onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val result = registrationRepository.approveJoinRequest(requestId, adminId)
            onResult?.invoke(result.isSuccess)
        }
    }

    fun rejectRequest(requestId: String, reason: String, adminId: String = "admin", onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val result = registrationRepository.rejectJoinRequest(requestId, reason, adminId)
            onResult?.invoke(result.isSuccess)
        }
    }

    override fun onCleared() {
        super.onCleared()
        registrationRepository.clearListeners()
    }
}
