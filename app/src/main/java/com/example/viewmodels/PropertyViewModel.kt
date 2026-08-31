package com.example.viewmodels

import androidx.lifecycle.ViewModel
import com.example.data.repositories.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PropertyViewModel @Inject constructor(
    private val repository: PropertyRepository = PropertyRepository()
) : ViewModel() {
    override fun onCleared() {
        super.onCleared()
        repository.clearListeners()
    }
}
