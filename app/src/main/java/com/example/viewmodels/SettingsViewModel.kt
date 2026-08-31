package com.example.viewmodels

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminSettingsEntity
import com.example.data.repositories.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SettingsUiState {
    object Idle : SettingsUiState()
    object Loading : SettingsUiState()
    data class Success(val message: String) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}

/**
 * ⚙️ SettingsViewModel
 * إدارة كاملة لمنطق إعدادات المنصة المباشرة والتخصيص المظهر الشامل.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _settings = MutableStateFlow(AdminSettingsEntity())
    val settings: StateFlow<AdminSettingsEntity> = _settings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeSettings()
    }

    fun observeSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            settingsRepository.observeSettings().collect { data ->
                _isLoading.value = false
                _settings.value = data
            }
        }
    }

    fun saveSettings(newSettings: AdminSettingsEntity, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = settingsRepository.saveSettings(newSettings)
            _isLoading.value = false
            onResult?.invoke(res.isSuccess)
        }
    }

    fun updatePartialSettings(updates: Map<String, Any>, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val res = settingsRepository.updatePartialSettings(updates)
            onResult?.invoke(res.isSuccess)
        }
    }

    override fun onCleared() {
        super.onCleared()
        settingsRepository.clearListeners()
    }
}
