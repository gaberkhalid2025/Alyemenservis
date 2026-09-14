package com.example.ui.screens.dashboard.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SpecialOfferEntity
import com.example.data.repositories.OffersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OffersViewModel @Inject constructor(
    private val repository: OffersRepository
) : ViewModel() {

    private val _offers = MutableStateFlow<List<SpecialOfferEntity>>(emptyList())
    val offers: StateFlow<List<SpecialOfferEntity>> = _offers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadOffers()
    }

    fun loadOffers() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getOffers().onSuccess {
                _offers.value = it
            }.onFailure {
                _error.value = it.message
            }
            _isLoading.value = false
        }
    }

    fun addOffer(offer: SpecialOfferEntity) {
        viewModelScope.launch {
            repository.addOffer(offer).onSuccess {
                loadOffers()
            }
        }
    }

    fun updateOffer(offer: SpecialOfferEntity) {
        viewModelScope.launch {
            repository.updateOffer(offer).onSuccess {
                loadOffers()
            }
        }
    }

    fun deleteOffer(offerId: String) {
        viewModelScope.launch {
            repository.deleteOffer(offerId).onSuccess {
                loadOffers()
            }
        }
    }
}
