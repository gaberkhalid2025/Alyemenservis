package com.example.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repositories.IDashboardRepository
import com.example.data.repositories.IFavoritesRepository
import com.example.data.repositories.IGalleryRepository
import com.example.data.repositories.IProductsRepository
import com.example.data.repositories.IRatingsRepository
import com.example.domain.entities.DashboardStatsEntity
import com.example.domain.entities.FavoriteItemEntity
import com.example.domain.entities.GalleryAlbumEntity
import com.example.domain.entities.ProductItemEntity
import com.example.domain.entities.RatingReviewEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 🎨 Common UI States for Dashboards and Features
 */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val stats: DashboardStatsEntity = DashboardStatsEntity(),
    val products: List<ProductItemEntity> = emptyList(),
    val reviews: List<RatingReviewEntity> = emptyList(),
    val galleryAlbums: List<GalleryAlbumEntity> = emptyList(),
    val activeTab: Int = 0, // 0: Overview, 1: Products/Services, 2: Bookings/Orders, 3: Gallery, 4: Reviews
    val errorMessage: String? = null
)

sealed class DashboardEvent {
    data class ShowToast(val message: String) : DashboardEvent()
    data class NavigateToDetail(val id: String) : DashboardEvent()
}

/**
 * 🧠 TechnicianDashboardViewModel
 */
class TechnicianDashboardViewModel(
    private val ownerId: String,
    private val dashboardRepository: IDashboardRepository,
    private val productsRepository: IProductsRepository,
    private val ratingsRepository: IRatingsRepository,
    private val galleryRepository: IGalleryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<DashboardEvent>()
    val eventFlow: SharedFlow<DashboardEvent> = _eventFlow.asSharedFlow()

    init {
        loadDashboardData()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(activeTab = tabIndex)
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            launch {
                dashboardRepository.getDashboardStats(ownerId, "PROVIDER").collect { stats ->
                    _uiState.value = _uiState.value.copy(stats = stats, isLoading = false)
                }
            }

            launch {
                productsRepository.getOwnerProducts(ownerId).collect { prods ->
                    _uiState.value = _uiState.value.copy(products = prods)
                }
            }

            launch {
                ratingsRepository.getTargetRatings(ownerId).collect { revs ->
                    _uiState.value = _uiState.value.copy(reviews = revs)
                }
            }

            launch {
                galleryRepository.getOwnerGallery(ownerId).collect { albums ->
                    _uiState.value = _uiState.value.copy(galleryAlbums = albums)
                }
            }
        }
    }

    fun addNewProductService(title: String, priceYer: Double, category: String) {
        viewModelScope.launch {
            if (title.isBlank()) {
                _eventFlow.emit(DashboardEvent.ShowToast("يرجى كتابة عنوان الخدمة أو المنتج"))
                return@launch
            }
            val newProduct = ProductItemEntity(
                ownerId = ownerId,
                title = title,
                priceYer = priceYer,
                category = category
            )
            val res = productsRepository.addProduct(newProduct)
            res.onSuccess {
                _eventFlow.emit(DashboardEvent.ShowToast("تمت إضافة الخدمة بنجاح"))
            }.onFailure {
                _eventFlow.emit(DashboardEvent.ShowToast("حدث خطأ أثناء الإضافة"))
            }
        }
    }

    fun deleteProduct(id: String) {
        viewModelScope.launch {
            productsRepository.deleteProduct(id).onSuccess {
                _eventFlow.emit(DashboardEvent.ShowToast("تم الحذف بنجاح"))
            }
        }
    }
}

/**
 * 🧠 StoreDashboardViewModel
 */
class StoreDashboardViewModel(
    private val storeId: String,
    private val dashboardRepository: IDashboardRepository,
    private val productsRepository: IProductsRepository,
    private val ratingsRepository: IRatingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<DashboardEvent>()
    val eventFlow: SharedFlow<DashboardEvent> = _eventFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            launch {
                dashboardRepository.getDashboardStats(storeId, "STORE").collect { stats ->
                    _uiState.value = _uiState.value.copy(stats = stats, isLoading = false)
                }
            }
            launch {
                productsRepository.getOwnerProducts(storeId).collect { prods ->
                    _uiState.value = _uiState.value.copy(products = prods)
                }
            }
        }
    }
}

/**
 * 🧠 RestaurantDashboardViewModel
 */
class RestaurantDashboardViewModel(
    private val restaurantId: String,
    private val dashboardRepository: IDashboardRepository,
    private val productsRepository: IProductsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            launch {
                dashboardRepository.getDashboardStats(restaurantId, "RESTAURANT").collect { stats ->
                    _uiState.value = _uiState.value.copy(stats = stats, isLoading = false)
                }
            }
            launch {
                productsRepository.getOwnerProducts(restaurantId).collect { menu ->
                    _uiState.value = _uiState.value.copy(products = menu)
                }
            }
        }
    }
}

/**
 * 🧠 MedicalDashboardViewModel
 */
class MedicalDashboardViewModel(
    private val centerId: String,
    private val dashboardRepository: IDashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dashboardRepository.getDashboardStats(centerId, "MEDICAL").collect { stats ->
                _uiState.value = _uiState.value.copy(stats = stats, isLoading = false)
            }
        }
    }
}

/**
 * 🧠 PropertyDashboardViewModel
 */
class PropertyDashboardViewModel(
    private val ownerId: String,
    private val dashboardRepository: IDashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dashboardRepository.getDashboardStats(ownerId, "PROPERTY").collect { stats ->
                _uiState.value = _uiState.value.copy(stats = stats, isLoading = false)
            }
        }
    }
}

/**
 * 🧠 JobPosterDashboardViewModel
 */
class JobPosterDashboardViewModel(
    private val posterId: String,
    private val dashboardRepository: IDashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dashboardRepository.getDashboardStats(posterId, "JOB").collect { stats ->
                _uiState.value = _uiState.value.copy(stats = stats, isLoading = false)
            }
        }
    }
}

/**
 * 🧠 FavoritesViewModel
 */
data class FavoritesUiState(
    val isLoading: Boolean = true,
    val favorites: List<FavoriteItemEntity> = emptyList(),
    val errorMessage: String? = null
)

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
            val isFav = favoritesRepository.isFavorite(userId, item.targetId)
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

/**
 * 🧠 ServicesBrowserViewModel
 */
data class ServicesBrowserUiState(
    val isLoading: Boolean = true,
    val products: List<ProductItemEntity> = emptyList(),
    val filteredProducts: List<ProductItemEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCity: String = "الكل",
    val selectedCategory: String = "الكل"
)

class ServicesBrowserViewModel(
    private val productsRepository: IProductsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServicesBrowserUiState())
    val uiState: StateFlow<ServicesBrowserUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            productsRepository.getAllAvailableProducts().collect { list ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    products = list,
                    filteredProducts = filterList(list, _uiState.value.searchQuery, _uiState.value.selectedCategory)
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        val state = _uiState.value
        _uiState.value = state.copy(
            searchQuery = query,
            filteredProducts = filterList(state.products, query, state.selectedCategory)
        )
    }

    fun selectCategory(category: String) {
        val state = _uiState.value
        _uiState.value = state.copy(
            selectedCategory = category,
            filteredProducts = filterList(state.products, state.searchQuery, category)
        )
    }

    private fun filterList(
        list: List<ProductItemEntity>,
        query: String,
        category: String
    ): List<ProductItemEntity> {
        return list.filter { item ->
            val matchesQuery = query.isBlank() || item.title.contains(query, ignoreCase = true) || item.description.contains(query, ignoreCase = true)
            val matchesCategory = category == "الكل" || item.category == category
            matchesQuery && matchesCategory
        }
    }
}
