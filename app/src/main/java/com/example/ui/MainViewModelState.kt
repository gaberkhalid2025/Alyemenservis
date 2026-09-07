package com.example.ui

import androidx.compose.runtime.Stable
import com.example.data.JobEntity
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity

@Stable
data class MainViewModelState(
    // Navigation
    val currentScreen: String = "USER_BROWSE",
    val screenBackStack: List<String> = listOf("USER_BROWSE"),
    
    // User & System State
    val isInitialized: Boolean = false,
    val isOnline: Boolean = true,
    val isRefreshing: Boolean = false,
    val isProvidersLoading: Boolean = true,
    val isChatChannelsLoading: Boolean = true,
    
    // Location
    val userLatitude: Double = 15.3694,
    val userLongitude: Double = 44.1910,
    val isGpsTrackingActive: Boolean = false,
    val maxKmRadius: Int = 10,
    
    // UI
    val toastMessage: String? = null,
    val uiErrorMessage: String? = null,
    
    // Favorites
    val favoriteIds: Set<String> = emptySet(),
    
    // Language
    val currentLanguage: String = "ar",
    
    // Selected Items
    val selectedProvider: ProviderEntity? = null,
    val selectedStore: StoreEntity? = null,
    val selectedProperty: PropertyEntity? = null,
    val selectedJob: JobEntity? = null,
    val selectedOfferId: String = "",
    val selectedRequestId: String = "",
    
    // Dialogs
    val showQuickServiceDialog: Boolean = false,
    val showBackdoorDialog: Boolean = false,
    val triggerRestoreAccountDialog: Boolean = false,
    
    // User Points
    val currentUserPoints: Int = 0,
    
    // Notifications Read Status
    val readNotificationIds: Set<String> = emptySet()
)

sealed class MainViewModelAction {
    data class NavigateTo(val screen: String) : MainViewModelAction()
    object GoBack : MainViewModelAction()
    data class ShowToast(val message: String) : MainViewModelAction()
    data class ShowError(val message: String) : MainViewModelAction()
    data class UpdateLocation(val lat: Double, val lng: Double) : MainViewModelAction()
    data class ToggleFavorite(val id: String) : MainViewModelAction()
    data class SetLanguage(val lang: String) : MainViewModelAction()
    object RefreshData : MainViewModelAction()
    data class SelectProvider(val provider: ProviderEntity?) : MainViewModelAction()
    data class SelectStore(val store: StoreEntity?) : MainViewModelAction()
    data class SelectProperty(val property: PropertyEntity?) : MainViewModelAction()
    data class SelectJob(val job: JobEntity?) : MainViewModelAction()
    data class SetShowQuickServiceDialog(val show: Boolean) : MainViewModelAction()
    data class UpdateOnlineStatus(val isOnline: Boolean) : MainViewModelAction()
    data class UpdateUserPoints(val points: Int) : MainViewModelAction()
    data class MarkNotificationRead(val notificationId: String) : MainViewModelAction()
    data class MarkAllNotificationsRead(val ids: Set<String>) : MainViewModelAction()
}
