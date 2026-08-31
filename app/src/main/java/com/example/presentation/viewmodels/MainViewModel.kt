package com.example.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repositories.contracts.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val authRepository: IAuthRepository,
    private val userRepository: IUserRepository,
    private val providerRepository: IProviderRepository,
    private val storeRepository: IStoreRepository,
    private val propertyRepository: IPropertyRepository,
    private val jobRepository: IJobRepository,
    private val bookingRepository: IBookingRepository,
    private val notificationRepository: INotificationRepository,
    private val adminRepository: IAdminRepository,
    private val chatRepository: IChatRepository,
    private val categoryRepository: ICategoryRepository,
    private val filterRepository: IFilterRepository,
    private val bannerRepository: IBannerRepository,
    private val couponRepository: ICouponRepository,
    private val settingsRepository: ISettingsRepository,
    private val analyticsRepository: IAnalyticsRepository,
    private val colorThemeRepository: IColorThemeRepository,
    private val storageRepository: IStorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        observeNotifications()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            bannerRepository.observeBanners().collect { result ->
                result.onSuccess { banners ->
                    _uiState.update { it.copy(banners = banners, isLoading = false) }
                }.onFailure { error ->
                    _uiState.update { it.copy(error = error.localizedMessage, isLoading = false) }
                }
            }
        }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            notificationRepository.observeAdminNotifications().collect { list ->
                _uiState.update { it.copy(notifications = list) }
            }
        }
    }

    fun approveEntityRequest(entityId: String, entityType: String) {
        viewModelScope.launch {
            adminRepository.approveJoinRequest(entityId, entityType).fold(
                onSuccess = { analyticsRepository.logEvent("admin_approve_success", mapOf("id" to entityId)) },
                onFailure = { err -> _uiState.update { it.copy(error = err.localizedMessage) } }
            )
        }
    }

    fun applyCouponCode(code: String, amount: Double) {
        viewModelScope.launch {
            couponRepository.applyCoupon(code, amount).fold(
                onSuccess = { discounted -> _uiState.update { it.copy(discountedAmount = discounted) } },
                onFailure = { err -> _uiState.update { it.copy(error = err.localizedMessage) } }
            )
        }
    }
}

data class MainUiState(
    val isLoading: Boolean = false,
    val banners: List<Any> = emptyList(),
    val notifications: List<Any> = emptyList(),
    val discountedAmount: Double? = null,
    val error: String? = null
)
