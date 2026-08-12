package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FavoritesManager {
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    fun toggleFavorite(id: String) {
        val current = _favoriteIds.value
        _favoriteIds.value = if (current.contains(id)) {
            current - id
        } else {
            current + id
        }
    }

    fun isFavorite(id: String): Boolean {
        return _favoriteIds.value.contains(id)
    }

    fun clearAllFavorites() {
        _favoriteIds.value = emptySet()
    }
}
