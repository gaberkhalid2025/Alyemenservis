package com.example.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.JoinRequestEntity
import com.example.data.repositories.RegistrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RegistrationState {
    object Idle : RegistrationState()
    object Loading : RegistrationState()
    data class Success(val message: String) : RegistrationState()
    data class Error(val message: String) : RegistrationState()
}

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val repository: RegistrationRepository = RegistrationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val uiState: StateFlow<RegistrationState> = _uiState.asStateFlow()

    private val _joinStatus = MutableStateFlow<JoinRequestEntity?>(null)
    val joinStatus: StateFlow<JoinRequestEntity?> = _joinStatus.asStateFlow()

    fun submitJoinRequest(request: JoinRequestEntity) {
        viewModelScope.launch {
            _uiState.value = RegistrationState.Loading
            val result = repository.submitJoinRequest(request)
            result.onSuccess {
                _uiState.value = RegistrationState.Success("تم إرسال طلب الانضمام بنجاح")
            }.onFailure { e ->
                _uiState.value = RegistrationState.Error(e.message ?: "حدث خطأ غير متوقع")
            }
        }
    }

    fun observeJoinStatus(phone: String) {
        viewModelScope.launch {
            repository.observeJoinStatusByPhone(phone).collect { status ->
                _joinStatus.value = status
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.clearListeners()
    }
}
