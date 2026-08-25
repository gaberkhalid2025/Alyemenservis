package com.example.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.ChatChannel
import com.example.data.repositories.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _channels = MutableStateFlow<List<ChatChannel>>(emptyList())
    val channels: StateFlow<List<ChatChannel>> = _channels.asStateFlow()

    private val _selectedFilter = MutableStateFlow("ALL") // ALL, UNREAD, TECHNICIANS, STORES, RESTAURANTS, SUPPORT
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var channelsJob: Job? = null

    fun loadUserChannels(currentUserId: String) {
        channelsJob?.cancel()
        _isLoading.value = true
        channelsJob = viewModelScope.launch {
            repository.getUserChannels(currentUserId).collect { list ->
                _channels.value = list
                _isLoading.value = false
            }
        }
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setUserPresence(userId: String, isOnline: Boolean) {
        viewModelScope.launch {
            repository.setUserPresence(userId, isOnline)
        }
    }

    override fun onCleared() {
        super.onCleared()
        channelsJob?.cancel()
    }
}
