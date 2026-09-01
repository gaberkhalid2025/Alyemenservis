package com.example.ui.screens.dashboard.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repositories.IFavoritesRepository
import com.example.domain.entities.FavoriteItemEntity
import com.example.ui.screens.dashboard.DashboardEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val isLoading: Boolean = true,
    val favorites: List<FavoriteItemEntity> = emptyList(),
    val errorMessage: String? = null
)

/**
 * 🧠 FavoritesViewModel - إدارة تفضيلات المستخدم والمفضلة
 */
class FavoritesViewModel(
    private val userId: String,
    private val favoritesRepository: IFavoritesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<DashboardEvent>()
    val eventFlow: SharedFlow<DashboardEvent> = _eventFlow.asSharedFlow()

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            favoritesRepository.getUserFavorites(userId).collect { list ->
                _uiState.value = FavoritesUiState(isLoading = false, favorites = list)
            }
        }
    }

    fun toggleFavorite(item: FavoriteItemEntity) {
        viewModelScope.launch {
            val isFav = favoritesRepository.isFavorite(userId, item.targetId).first()
            if (isFav) {
                favoritesRepository.removeFavorite(userId, item.targetId).onSuccess {
                    _eventFlow.emit(DashboardEvent.ShowToast("تمت الإزالة من المفضلة"))
                }
            } else {
                favoritesRepository.addFavorite(item.copy(userId = userId)).onSuccess {
                    _eventFlow.emit(DashboardEvent.ShowToast("تمت الإضافة للمفضلة"))
                }
            }
        }
    }
}
