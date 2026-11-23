package com.example.ui.screens.dashboard.viewmodels

import androidx.lifecycle.ViewModel
import com.example.ui.*
import androidx.lifecycle.viewModelScope
import com.example.data.repositories.IDashboardRepository
import com.example.data.repositories.IProductsRepository
import com.example.data.repositories.IRatingsRepository
import com.example.domain.entities.ProductItemEntity
import com.example.ui.screens.dashboard.DashboardEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.flow.catch

class StoreDashboardViewModel(
    private val ownerId: String,
    private val dashboardRepository: IDashboardRepository,
    private val productsRepository: IProductsRepository,
    private val ratingsRepository: IRatingsRepository
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

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            supervisorScope {
                launch {
                    dashboardRepository.getDashboardStats(ownerId, "STORE")
                        .catch { e -> 
                            _eventFlow.emit(DashboardEvent.ShowToast(e.message ?: "خطأ في الإحصائيات")) 
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }
                        .collect { stats ->
                            _uiState.value = _uiState.value.copy(stats = stats, isLoading = false)
                        }
                }
                launch {
                    productsRepository.getOwnerProducts(ownerId)
                        .catch { e -> _eventFlow.emit(DashboardEvent.ShowToast(e.message ?: "خطأ في المنتجات")) }
                        .collect { prods ->
                            _uiState.value = _uiState.value.copy(products = prods)
                        }
                }
                launch {
                    ratingsRepository.getTargetRatings(ownerId)
                        .catch { e -> _eventFlow.emit(DashboardEvent.ShowToast(e.message ?: "خطأ في التقييمات")) }
                        .collect { revs ->
                            _uiState.value = _uiState.value.copy(reviews = revs)
                        }
                }
            }
        }
    }

    fun addProduct(
        title: String,
        priceYer: Double,
        description: String = "",
        imageUrl: String = "",
        category: String = "STORE",
        isAvailable: Boolean = true,
        isOffer: Boolean = false,
        discountPercent: Int = 0,
        oldPriceYer: Double = 0.0,
        unit: String = ""
    ) {
        viewModelScope.launch {
            if (title.isBlank()) {
                _eventFlow.emit(DashboardEvent.ShowToast("يرجى إدخال اسم المنتج"))
                return@launch
            }
            val newProduct = ProductItemEntity(
                ownerId = ownerId,
                title = title,
                priceYer = priceYer,
                description = description,
                imageUrl = imageUrl,
                category = category.ifBlank { "STORE" },
                isAvailable = isAvailable,
                isOffer = isOffer || discountPercent > 0,
                discountPercent = discountPercent.coerceIn(0, 99),
                oldPriceYer = oldPriceYer.coerceAtLeast(0.0),
                unit = unit
            )
            val res = productsRepository.addProduct(newProduct)
            res.onSuccess {
                _eventFlow.emit(DashboardEvent.ShowToast("تمت إضافة المنتج بنجاح 📦"))
            }.onFailure {
                _eventFlow.emit(DashboardEvent.ShowToast("حدث خطأ أثناء حفظ المنتج"))
            }
        }
    }

    fun deleteProduct(id: String) {
        viewModelScope.launch {
            productsRepository.deleteProduct(id).onSuccess {
                _eventFlow.emit(DashboardEvent.ShowToast("تم حذف المنتج 🗑️"))
            }.onFailure { e ->
                _eventFlow.emit(DashboardEvent.ShowToast(e.localizedMessage ?: "فشل حذف المنتج"))
            }
        }
    }

    fun updateProduct(product: ProductItemEntity) {
        viewModelScope.launch {
            productsRepository.updateProduct(product).onSuccess {
                _eventFlow.emit(DashboardEvent.ShowToast("تم تحديث المنتج بنجاح ✅"))
            }.onFailure { e ->
                _eventFlow.emit(DashboardEvent.ShowToast(e.localizedMessage ?: "فشل تحديث المنتج"))
            }
        }
    }

    fun replyToReview(reviewId: String, replyText: String) {
        viewModelScope.launch {
            ratingsRepository.replyToRating(reviewId, replyText).onSuccess {
                _eventFlow.emit(DashboardEvent.ShowToast("تم حفظ وإرسال الرد للعميل بنجاح ✅"))
            }.onFailure { e ->
                _eventFlow.emit(DashboardEvent.ShowToast(e.localizedMessage ?: "تعذر حفظ الرد"))
            }
        }
    }

    fun updateProfile(
        name: String,
        cityArea: String,
        description: String,
        workingHours: String,
        isAvailable: Boolean,
        photoUrl: String? = null,
        coverUrl: String? = null,
        phone: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        viewModelScope.launch {
            dashboardRepository.updateBusinessProfile(
                ownerId = ownerId,
                role = "STORE",
                name = name,
                cityArea = cityArea,
                description = description,
                workingHours = workingHours,
                isAvailable = isAvailable,
                photoUrl = photoUrl,
                coverUrl = coverUrl,
                phone = phone,
                latitude = latitude,
                longitude = longitude
            ).onSuccess {
                _eventFlow.emit(DashboardEvent.ShowToast("تم حفظ بيانات المتجر بنجاح ✅"))
            }.onFailure { e ->
                _eventFlow.emit(DashboardEvent.ShowToast(e.localizedMessage ?: "فشل حفظ البيانات"))
            }
        }
    }

    fun changePassword(oldPass: String, newPass: String) {
        viewModelScope.launch {
            dashboardRepository.changeBusinessPassword(ownerId, "STORE", oldPass, newPass)
                .onSuccess { _eventFlow.emit(DashboardEvent.ShowToast("تم تحديث كلمة المرور بنجاح 🔑")) }
                .onFailure { e -> _eventFlow.emit(DashboardEvent.ShowToast(e.localizedMessage ?: "فشل تحديث كلمة المرور")) }
        }
    }

    fun dispose() {
        onCleared()
    }
}
