package com.example.viewmodels

import androidx.lifecycle.ViewModel
import com.example.data.repositories.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProviderViewModel @Inject constructor(
    private val repository: ProviderRepository = ProviderRepository()
) : ViewModel() {
    override fun onCleared() {
        super.onCleared()
        repository.clearListeners()
    }
}
