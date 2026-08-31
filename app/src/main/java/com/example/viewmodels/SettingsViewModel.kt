package com.example.viewmodels

import androidx.lifecycle.ViewModel
import com.example.data.repositories.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository = SettingsRepository()
) : ViewModel() {
    override fun onCleared() {
        super.onCleared()
        repository.clearListeners()
    }
}
