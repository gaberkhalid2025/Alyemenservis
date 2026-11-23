package com.example.ui.screens.dashboard.viewmodels

import androidx.lifecycle.ViewModel
import com.example.ui.*
import androidx.lifecycle.viewModelScope
import com.example.data.repositories.IDashboardRepository
import com.example.data.repositories.IGalleryRepository
import com.example.data.repositories.IProductsRepository
import com.example.data.repositories.IRatingsRepository
import com.example.domain.entities.ProductItemEntity
import com.example.ui.screens.dashboard.DashboardEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.flow.catch

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

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            supervisorScope {
                launch {
                    dashboardRepository.getDashboardStats(ownerId, "PROVIDER")
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
                launch {
                    galleryRepository.getOwnerGallery(ownerId)
                        .catch { e -> _eventFlow.emit(DashboardEvent.ShowToast(e.message ?: "خطأ في المعرض")) }
                        .collect { albums ->
                            _uiState.value = _uiState.value.copy(galleryAlbums = albums)
                        }
                }
            }
        }
    }

    fun addNewProductService(
        title: String,
        priceYer: Double,
        description: String = "",
        imageUrl: String = "",
        category: String = "TECHNICIAN",
        isAvailable: Boolean = true,
        isOffer: Boolean = false,
        discountPercent: Int = 0,
        oldPriceYer: Double = 0.0,
        unit: String = ""
    ) {
        viewModelScope.launch {
            if (title.isBlank()) {
                _eventFlow.emit(DashboardEvent.ShowToast("يرجى إدخال اسم الخدمة"))
                return@launch
            }
            val newProduct = ProductItemEntity(
                ownerId = ownerId,
                title = title,
                priceYer = priceYer,
                description = description,
                imageUrl = imageUrl,
                category = category.ifBlank { "TECHNICIAN" },
                isAvailable = isAvailable,
                isOffer = isOffer || discountPercent > 0,
                discountPercent = discountPercent.coerceIn(0, 99),
                oldPriceYer = oldPriceYer.coerceAtLeast(0.0),
                unit = unit
            )
            val res = productsRepository.addProduct(newProduct)
            res.onSuccess {
                _eventFlow.emit(DashboardEvent.ShowToast("تمت إضافة الخدمة بنجاح ✅"))
            }.onFailure {
                _eventFlow.emit(DashboardEvent.ShowToast("حدث خطأ أثناء الإضافة"))
            }
        }
    }

    fun deleteProduct(id: String) {
        viewModelScope.launch {
            productsRepository.deleteProduct(id).onSuccess {
                _eventFlow.emit(DashboardEvent.ShowToast("تم حذف الخدمة بنجاح 🗑️"))
            }.onFailure { e ->
                _eventFlow.emit(DashboardEvent.ShowToast(e.localizedMessage ?: "فشل حذف الخدمة"))
            }
        }
    }

    fun updateProduct(product: ProductItemEntity) {
        viewModelScope.launch {
            productsRepository.updateProduct(product).onSuccess {
                _eventFlow.emit(DashboardEvent.ShowToast("تم تحديث الخدمة والسعر بنجاح ✅"))
            }.onFailure { e ->
                _eventFlow.emit(DashboardEvent.ShowToast(e.localizedMessage ?: "فشل تحديث الخدمة"))
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
                role = "PROVIDER",
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
                _eventFlow.emit(DashboardEvent.ShowToast("تم تحديث بيانات ملفك الشخصي ✅"))
            }.onFailure { e ->
                _eventFlow.emit(DashboardEvent.ShowToast(e.localizedMessage ?: "فشل تحديث البيانات"))
            }
        }
    }

    fun changePassword(oldPass: String, newPass: String) {
        viewModelScope.launch {
            dashboardRepository.changeBusinessPassword(ownerId, "PROVIDER", oldPass, newPass)
                .onSuccess { _eventFlow.emit(DashboardEvent.ShowToast("تم تغيير كلمة المرور بنجاح 🔑")) }
                .onFailure { e -> _eventFlow.emit(DashboardEvent.ShowToast(e.localizedMessage ?: "فشل تغيير كلمة المرور")) }
        }
    }

    fun saveGalleryAlbum(context: android.content.Context, localUriStr: String) {
        val appContext = context.applicationContext
        val cleanUri = localUriStr.trim()
        if (cleanUri.isBlank()) return
        viewModelScope.launch {
            if (cleanUri.startsWith("http://") || cleanUri.startsWith("https://")) {
                val album = com.example.domain.entities.GalleryAlbumEntity(
                    ownerId = ownerId,
                    title = "أعمال سابقة",
                    imageUrls = listOf(cleanUri)
                )
                galleryRepository.saveGalleryAlbum(album).onSuccess {
                    _eventFlow.emit(DashboardEvent.ShowToast("تم حفظ الصورة في المعرض بنجاح ✅"))
                }.onFailure {
                    _eventFlow.emit(DashboardEvent.ShowToast("حدث خطأ أثناء حفظ الصورة"))
                }
            } else {
                _eventFlow.emit(DashboardEvent.ShowToast("جاري رفع الصورة... ⏳"))
                val uri = android.net.Uri.parse(cleanUri)
                val uploadResult = com.example.utils.FirebaseStorageUploader.uploadImageToStorage(appContext, uri, "galleries/${ownerId}")
                
                uploadResult.onSuccess { remoteUrl ->
                    val album = com.example.domain.entities.GalleryAlbumEntity(
                        ownerId = ownerId,
                        title = "أعمال سابقة",
                        imageUrls = listOf(remoteUrl)
                    )
                    galleryRepository.saveGalleryAlbum(album).onSuccess {
                        _eventFlow.emit(DashboardEvent.ShowToast("تم حفظ الصورة في المعرض بنجاح ✅"))
                    }.onFailure {
                        _eventFlow.emit(DashboardEvent.ShowToast("حدث خطأ أثناء حفظ الصورة"))
                    }
                }.onFailure {
                    _eventFlow.emit(DashboardEvent.ShowToast("فشل رفع الصورة ❌"))
                }
            }
        }
    }

    fun deleteGalleryAlbum(albumId: String) {
        viewModelScope.launch {
            galleryRepository.deleteGalleryAlbum(albumId).onSuccess {
                _eventFlow.emit(DashboardEvent.ShowToast("تم حذف الصورة بنجاح 🗑️"))
            }.onFailure {
                _eventFlow.emit(DashboardEvent.ShowToast("حدث خطأ أثناء الحذف"))
            }
        }
    }

    fun dispose() {
        onCleared()
    }
}
