package com.example.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.ChannelType
import com.example.data.models.ChatChannel
import com.example.data.models.ChatFilterCategory
import com.example.data.repositories.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _channels = MutableStateFlow<List<ChatChannel>>(emptyList())
    val channels: StateFlow<List<ChatChannel>> = _channels.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ChatFilterCategory.ALL)
    val selectedFilter: StateFlow<ChatFilterCategory> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var channelsJob: Job? = null
    private var lastUserId: String = ""

    /**
     * Filtered channels flow based on search and selected category filter.
     */
    val filteredChannels: StateFlow<List<ChatChannel>> = combine(
        _channels,
        _selectedFilter,
        _searchQuery
    ) { rawChannels, filter, query ->
        val q = query.trim().lowercase()
        rawChannels.filter { channel ->
            // Text search match
            val matchesQuery = if (q.isBlank()) true else {
                val names = channel.participantNames.values.joinToString(" ").lowercase()
                val lastMsg = channel.lastMessage.lowercase()
                val entityId = channel.relatedEntityId?.lowercase() ?: ""
                names.contains(q) || lastMsg.contains(q) || entityId.contains(q)
            }

            // Category filter match
            val matchesFilter = when (filter) {
                ChatFilterCategory.ALL -> true
                ChatFilterCategory.UNREAD -> {
                    val unread = channel.unreadCount[lastUserId] ?: 0
                    unread > 0
                }
                ChatFilterCategory.TECHNICIANS -> {
                    val entityType = channel.relatedEntityType?.uppercase() ?: ""
                    entityType == "TECHNICIAN" || entityType == "BOOKING" ||
                            channel.participantRoles.values.any { it.contains("TECH", ignoreCase = true) || it.contains("فني", ignoreCase = true) }
                }
                ChatFilterCategory.STORES -> {
                    val entityType = channel.relatedEntityType?.uppercase() ?: ""
                    entityType == "STORE" ||
                            channel.participantRoles.values.any { it.contains("STORE", ignoreCase = true) || it.contains("متجر", ignoreCase = true) }
                }
                ChatFilterCategory.RESTAURANTS -> {
                    val entityType = channel.relatedEntityType?.uppercase() ?: ""
                    entityType == "RESTAURANT" ||
                            channel.participantRoles.values.any { it.contains("RESTAURANT", ignoreCase = true) || it.contains("مطعم", ignoreCase = true) }
                }
                ChatFilterCategory.SUPPORT -> {
                    channel.type == ChannelType.SUPPORT ||
                            channel.relatedEntityType?.uppercase() == "SUPPORT" ||
                            channel.participantNames.values.any { it.contains("الدعم", ignoreCase = true) || it.contains("Support", ignoreCase = true) }
                }
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadUserChannels(currentUserId: String) {
        if (currentUserId.isBlank()) return
        lastUserId = currentUserId
        channelsJob?.cancel()
        _isLoading.value = true
        channelsJob = viewModelScope.launch {
            repository.getUserChannels(currentUserId).collect { list ->
                _channels.value = list
                _isLoading.value = false
            }
        }
    }

    fun setFilter(filter: ChatFilterCategory) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteChannel(channelId: String, currentUserId: String, forEveryone: Boolean = false) {
        viewModelScope.launch {
            repository.deleteChannel(channelId, currentUserId, forEveryone)
        }
    }

    fun deleteAllChannels(currentUserId: String, forEveryone: Boolean = false) {
        viewModelScope.launch {
            repository.deleteAllChannels(currentUserId, forEveryone)
        }
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
