package com.example.viewmodels

import androidx.lifecycle.ViewModel
import com.example.data.repositories.JobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class JobViewModel @Inject constructor(
    private val repository: JobRepository = JobRepository()
) : ViewModel() {
    override fun onCleared() {
        super.onCleared()
        repository.clearListeners()
    }
}
