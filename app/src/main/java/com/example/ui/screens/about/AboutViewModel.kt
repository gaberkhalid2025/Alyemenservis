package com.example.ui.screens.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminSettingsEntity
import com.example.ui.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Representing the various UI States for the About Screen.
 */
sealed class AboutUiState {
    object Loading : AboutUiState()
    data class Success(val settings: AdminSettingsEntity) : AboutUiState()
    data class Editing(val settings: AdminSettingsEntity, val tempCustomText: String) : AboutUiState()
    data class Error(val message: String) : AboutUiState()
}

/**
 * ViewModel for managing the "About App" screen state and editing actions.
 * Adheres to MVVM architecture principles.
 *
 * @property mainViewModel The shared MainViewModel containing global app settings state.
 */
class AboutViewModel(
    private val mainViewModel: MainViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow<AboutUiState>(AboutUiState.Loading)
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    /**
     * Observes the global settings from the MainViewModel and updates the local state.
     */
    private fun observeSettings() {
        viewModelScope.launch {
            try {
                mainViewModel.settings.collect { settings ->
                    val currentState = _uiState.value
                    if (currentState is AboutUiState.Editing) {
                        _uiState.value = AboutUiState.Editing(settings, currentState.tempCustomText)
                    } else {
                        _uiState.value = AboutUiState.Success(settings)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = AboutUiState.Error(e.localizedMessage ?: "حدث خطأ غير متوقع أثناء تحميل البيانات.")
            }
        }
    }

    /**
     * Toggles the editing state for administrators.
     */
    fun toggleEditing() {
        val currentState = _uiState.value
        if (currentState is AboutUiState.Success) {
            _uiState.value = AboutUiState.Editing(currentState.settings, currentState.settings.aboutCustomInfo)
        } else if (currentState is AboutUiState.Editing) {
            _uiState.value = AboutUiState.Success(currentState.settings)
        }
    }

    /**
     * Reorders an item in the about layout order list upwards.
     *
     * @param index The index of the item to move up.
     */
    fun moveItemUp(index: Int) {
        val settings = getSettingsFromCurrentState() ?: return
        val items = settings.aboutLayoutOrder.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toMutableList()
        if (index > 0 && index < items.size) {
            val tmp = items[index]
            items[index] = items[index - 1]
            items[index - 1] = tmp
            val updatedOrder = items.joinToString(",")
            saveSettings(settings.copy(aboutLayoutOrder = updatedOrder))
        }
    }

    /**
     * Reorders an item in the about layout order list downwards.
     *
     * @param index The index of the item to move down.
     */
    fun moveItemDown(index: Int) {
        val settings = getSettingsFromCurrentState() ?: return
        val items = settings.aboutLayoutOrder.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toMutableList()
        if (index >= 0 && index < items.size - 1) {
            val tmp = items[index]
            items[index] = items[index + 1]
            items[index + 1] = tmp
            val updatedOrder = items.joinToString(",")
            saveSettings(settings.copy(aboutLayoutOrder = updatedOrder))
        }
    }

    /**
     * Updates the temporary text for custom description field during editing.
     */
    fun updateCustomText(newText: String) {
        val currentState = _uiState.value
        if (currentState is AboutUiState.Editing) {
            _uiState.value = currentState.copy(tempCustomText = newText)
        }
    }

    /**
     * Persists the custom text changes to the settings repository.
     */
    fun saveCustomTextChanges() {
        val currentState = _uiState.value
        if (currentState is AboutUiState.Editing) {
            val updatedSettings = currentState.settings.copy(aboutCustomInfo = currentState.tempCustomText)
            saveSettings(updatedSettings)
            mainViewModel.triggerNotification("💾 تم تحديث وحفظ نص شاشة عن التطبيق!")
        }
    }

    /**
     * Helper to retrieve current settings from the state.
     */
    private fun getSettingsFromCurrentState(): AdminSettingsEntity? {
        return when (val state = _uiState.value) {
            is AboutUiState.Success -> state.settings
            is AboutUiState.Editing -> state.settings
            else -> null
        }
    }

    /**
     * Dispatches settings save call to the MainViewModel.
     */
    private fun saveSettings(updatedSettings: AdminSettingsEntity) {
        try {
            mainViewModel.saveCustomSettingsState(updatedSettings)
        } catch (e: Exception) {
            _uiState.value = AboutUiState.Error(e.localizedMessage ?: "فشل في حفظ التعديلات.")
        }
    }
}
