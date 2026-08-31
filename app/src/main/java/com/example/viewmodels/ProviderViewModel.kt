package com.example.viewmodels

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProviderEntity
import com.example.data.repositories.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProviderUiState {
    object Idle : ProviderUiState()
    object Loading : ProviderUiState()
    data class Success(val message: String) : ProviderUiState()
    data class Error(val message: String) : ProviderUiState()
}

/**
 * 🛠️ ProviderViewModel
 * إدارة كاملة لمنطق الفنيين والمهنيين ومزودي الخدمات.
 */
@HiltViewModel
class ProviderViewModel @Inject constructor(
    private val providerRepository: ProviderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProviderUiState>(ProviderUiState.Idle)
    val uiState: StateFlow<ProviderUiState> = _uiState.asStateFlow()

    private val _providers = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val providers: StateFlow<List<ProviderEntity>> = _providers.asStateFlow()

    private val _selectedProvider = MutableStateFlow<ProviderEntity?>(null)
    val selectedProvider: StateFlow<ProviderEntity?> = _selectedProvider.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeApprovedProviders()
    }

    fun observeApprovedProviders() {
        viewModelScope.launch {
            _isLoading.value = true
            providerRepository.observeApprovedProviders().collect { list ->
                _isLoading.value = false
                _providers.value = list
            }
        }
    }

    fun selectProvider(provider: ProviderEntity?) {
        _selectedProvider.value = provider
    }

    override fun onCleared() {
        super.onCleared()
        providerRepository.clearListeners()
    }
}
