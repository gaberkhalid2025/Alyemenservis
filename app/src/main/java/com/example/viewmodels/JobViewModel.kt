package com.example.viewmodels

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.JobEntity
import com.example.data.repositories.JobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class JobUiState {
    object Idle : JobUiState()
    object Loading : JobUiState()
    data class Success(val message: String) : JobUiState()
    data class Error(val message: String) : JobUiState()
}

/**
 * 💼 JobViewModel
 * إدارة كاملة لمنطق قسم الوظائف، الإعلانات، والتقديم على الوظائف.
 */
@HiltViewModel
class JobViewModel @Inject constructor(
    private val jobRepository: JobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<JobUiState>(JobUiState.Idle)
    val uiState: StateFlow<JobUiState> = _uiState.asStateFlow()

    private val _jobs = MutableStateFlow<List<JobEntity>>(emptyList())
    val jobs: StateFlow<List<JobEntity>> = _jobs.asStateFlow()

    private val _selectedJob = MutableStateFlow<JobEntity?>(null)
    val selectedJob: StateFlow<JobEntity?> = _selectedJob.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeAllJobs()
    }

    fun observeAllJobs() {
        viewModelScope.launch {
            _isLoading.value = true
            jobRepository.observeAllJobs().collect { list ->
                _isLoading.value = false
                _jobs.value = list
            }
        }
    }

    fun observeJobsByCity(cityId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            jobRepository.observeJobsByCity(cityId).collect { list ->
                _isLoading.value = false
                _jobs.value = list
            }
        }
    }

    fun applyForJob(jobId: String, applicantName: String, applicantPhone: String, notes: String = "", onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val res = jobRepository.applyForJob(jobId, applicantName, applicantPhone, notes)
            onResult?.invoke(res.isSuccess)
        }
    }

    fun saveOrUpdateJob(job: JobEntity, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val res = jobRepository.saveOrUpdateJob(job)
            onResult?.invoke(res.isSuccess)
        }
    }

    fun deleteJob(jobId: String, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val res = jobRepository.deleteJob(jobId)
            onResult?.invoke(res.isSuccess)
        }
    }

    fun selectJob(job: JobEntity?) {
        _selectedJob.value = job
    }

    override fun onCleared() {
        super.onCleared()
        jobRepository.clearListeners()
    }
}
