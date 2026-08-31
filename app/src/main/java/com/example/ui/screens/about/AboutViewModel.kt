package com.example.ui.screens.about
import com.example.ui.MainViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminSettingsEntity

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Sealed class representing the different UI States for the About App screen.
 */
sealed class AboutUiState {
    object Loading : AboutUiState()
    data class Success(val settings: AdminSettingsEntity, val isAdmin: Boolean) : AboutUiState()
    data class Editing(val settings: AdminSettingsEntity) : AboutUiState()
    data class Error(val message: String) : AboutUiState()
}

/**
 * ViewModel for managing the "About App" screen settings and layouts.
 */
class AboutViewModel(
    private val mainViewModel: MainViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow<AboutUiState>(AboutUiState.Loading)
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    private var isEditingMode = false

    init {
        viewModelScope.launch {
            mainViewModel.settings.collectLatest { settings ->
                val adminRole = mainViewModel.adminRole.value
                val isAdmin = adminRole != "GUEST"
                if (isEditingMode) {
                    _uiState.value = AboutUiState.Editing(settings)
                } else {
                    _uiState.value = AboutUiState.Success(settings, isAdmin)
                }
            }
        }
    }

    /**
     * Toggles between standard view and editing view.
     */
    fun toggleEditingMode() {
        setEditingMode(!isEditingMode)
    }

    /**
     * Explicitly sets whether the editor is shown.
     */
    fun setEditingMode(editing: Boolean) {
        isEditingMode = editing
        val settings = mainViewModel.settings.value
        if (editing) {
            _uiState.value = AboutUiState.Editing(settings)
        } else {
            val adminRole = mainViewModel.adminRole.value
            val isAdmin = adminRole != "GUEST"
            _uiState.value = AboutUiState.Success(settings, isAdmin)
        }
    }

    /**
     * Moves an item up or down in the rendering order.
     */
    fun moveItem(index: Int, moveUp: Boolean) {
        val settings = mainViewModel.settings.value
        val list = settings.aboutLayoutOrder
            .split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }
            .toMutableList()

        if (moveUp && index > 0) {
            val temp = list[index]
            list[index] = list[index - 1]
            list[index - 1] = temp
        } else if (!moveUp && index < list.size - 1) {
            val temp = list[index]
            list[index] = list[index + 1]
            list[index + 1] = temp
        }
        val newOrder = list.joinToString(",")
        mainViewModel.saveCustomSettingsState(settings.copy(aboutLayoutOrder = newOrder))
    }

    /**
     * Updates the custom description text.
     */
    fun updateCustomInfo(newInfo: String) {
        val settings = mainViewModel.settings.value
        mainViewModel.saveCustomSettingsState(settings.copy(aboutCustomInfo = newInfo))
        mainViewModel.triggerNotification("💾 تم تحديث وحفظ نص شاشة عن التطبيق!")
    }
}
