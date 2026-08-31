package com.example.viewmodels

import androidx.lifecycle.ViewModel
import com.example.data.repositories.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val repository: StoreRepository = StoreRepository()
) : ViewModel() {
    override fun onCleared() {
        super.onCleared()
        repository.clearListeners()
    }
}
