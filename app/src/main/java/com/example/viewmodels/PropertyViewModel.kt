package com.example.viewmodels

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PropertyEntity
import com.example.data.repositories.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PropertyUiState {
    object Idle : PropertyUiState()
    object Loading : PropertyUiState()
    data class Success(val message: String) : PropertyUiState()
    data class Error(val message: String) : PropertyUiState()
}

/**
 * 🏠 PropertyViewModel
 * إدارة كاملة لمنطق قسم العقارات، الإعلانات، والعروض.
 */
@HiltViewModel
class PropertyViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PropertyUiState>(PropertyUiState.Idle)
    val uiState: StateFlow<PropertyUiState> = _uiState.asStateFlow()

    private val _properties = MutableStateFlow<List<PropertyEntity>>(emptyList())
    val properties: StateFlow<List<PropertyEntity>> = _properties.asStateFlow()

    private val _selectedProperty = MutableStateFlow<PropertyEntity?>(null)
    val selectedProperty: StateFlow<PropertyEntity?> = _selectedProperty.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeAllProperties()
    }

    fun observeAllProperties() {
        viewModelScope.launch {
            _isLoading.value = true
            propertyRepository.observeAllProperties().collect { list ->
                _isLoading.value = false
                _properties.value = list
            }
        }
    }

    fun observePropertiesByType(type: String) {
        viewModelScope.launch {
            _isLoading.value = true
            propertyRepository.observePropertiesByType(type).collect { list ->
                _isLoading.value = false
                _properties.value = list
            }
        }
    }

    fun saveOrUpdateProperty(property: PropertyEntity, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val res = propertyRepository.saveOrUpdateProperty(property)
            onResult?.invoke(res.isSuccess)
        }
    }

    fun selectProperty(property: PropertyEntity?) {
        _selectedProperty.value = property
    }

    override fun onCleared() {
        super.onCleared()
        propertyRepository.clearListeners()
    }
}
