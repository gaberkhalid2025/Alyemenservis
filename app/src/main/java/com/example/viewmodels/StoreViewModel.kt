package com.example.viewmodels

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.StoreEntity
import com.example.data.repositories.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StoreUiState {
    object Idle : StoreUiState()
    object Loading : StoreUiState()
    data class Success(val message: String) : StoreUiState()
    data class Error(val message: String) : StoreUiState()
}

/**
 * 🏬 StoreViewModel
 * إدارة كاملة لمنطق قسم المتاجر والأنشطة التجارية والمطاعم والخدمات الطبية.
 */
@HiltViewModel
class StoreViewModel @Inject constructor(
    private val storeRepository: StoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StoreUiState>(StoreUiState.Idle)
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    private val _stores = MutableStateFlow<List<StoreEntity>>(emptyList())
    val stores: StateFlow<List<StoreEntity>> = _stores.asStateFlow()

    private val _selectedStore = MutableStateFlow<StoreEntity?>(null)
    val selectedStore: StateFlow<StoreEntity?> = _selectedStore.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeAllStores()
    }

    fun observeAllStores() {
        viewModelScope.launch {
            _isLoading.value = true
            storeRepository.observeAllStores().collect { list ->
                _isLoading.value = false
                _stores.value = list
            }
        }
    }

    fun observeStoresBySection(sectionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            storeRepository.observeStoresBySection(sectionId).collect { list ->
                _isLoading.value = false
                _stores.value = list
            }
        }
    }

    fun saveOrUpdateStore(store: StoreEntity, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val res = storeRepository.saveOrUpdateStore(store)
            onResult?.invoke(res.isSuccess)
        }
    }

    fun selectStore(store: StoreEntity?) {
        _selectedStore.value = store
    }

    override fun onCleared() {
        super.onCleared()
        storeRepository.clearListeners()
    }
}
