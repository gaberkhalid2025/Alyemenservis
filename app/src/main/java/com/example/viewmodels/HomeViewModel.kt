package com.example.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CategoryEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.data.PropertyEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 🏠 HomeViewModel
 * إدارة البيانات الرئيسية للشاشة الرئيسية: الفئات، مقدمي الخدمات، المتاجر، المفضلة والبحث والفلترة اللحظية.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _providers = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val providers: StateFlow<List<ProviderEntity>> = _providers.asStateFlow()

    private val _filteredProviders = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val filteredProviders: StateFlow<List<ProviderEntity>> = _filteredProviders.asStateFlow()

    private val _favoriteProviderIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteProviderIds: StateFlow<Set<String>> = _favoriteProviderIds.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCity = MutableStateFlow("الكل")
    val selectedCity: StateFlow<String> = _selectedCity.asStateFlow()

    private val _minRating = MutableStateFlow(0f)
    val minRating: StateFlow<Float> = _minRating.asStateFlow()

    private val _onlyAvailable = MutableStateFlow(false)
    val onlyAvailable: StateFlow<Boolean> = _onlyAvailable.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var categoriesListener: ListenerRegistration? = null
    private var providersListener: ListenerRegistration? = null

    init {
        loadCategories()
        loadProviders()
    }

    /**
     * تحميل الأقسام من فايربيس مع الاستماع الفوري
     */
    fun loadCategories() {
        _isLoading.value = true
        categoriesListener?.remove()
        categoriesListener = firestore.collection("categories")
            .orderBy("order")
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) {
                    _errorMessage.value = error.localizedMessage
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { it.toObject(CategoryEntity::class.java) }
                    _categories.value = list
                } else {
                    // Fallback to default categories if database is empty
                    _categories.value = listOf(
                        CategoryEntity(id = "1", name = "صيانة منزلية", icon = "🔧", order = 1),
                        CategoryEntity(id = "2", name = "كهرباء وتكييف", icon = "⚡", order = 2),
                        CategoryEntity(id = "3", name = "سباكة وصحي", icon = "🚰", order = 3),
                        CategoryEntity(id = "4", name = "نقل وعفش", icon = "🚚", order = 4),
                        CategoryEntity(id = "5", name = "سيارات وميكانيك", icon = "🚗", order = 5),
                        CategoryEntity(id = "6", name = "تقنية وهواتف", icon = "💻", order = 6),
                        CategoryEntity(id = "7", name = "تنظيف ومكافحة", icon = "🧹", order = 7),
                        CategoryEntity(id = "8", name = "تعليم ودروس", icon = "📚", order = 8)
                    )
                }
            }
    }

    /**
     * تحميل الفنيين ومقدمي الخدمات
     */
    fun loadProviders() {
        _isLoading.value = true
        providersListener?.remove()
        providersListener = firestore.collection("providers")
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) {
                    _errorMessage.value = error.localizedMessage
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(ProviderEntity::class.java) }
                    _providers.value = list
                    applyCurrentFilters()
                }
            }
    }

    /**
     * اختيار قسم معين لتصفية الفنيين
     */
    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
        applyCurrentFilters()
    }

    /**
     * تعيين نص البحث
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyCurrentFilters()
    }

    /**
     * تعيين المدينة المحددة
     */
    fun setSelectedCity(city: String) {
        _selectedCity.value = city
        applyCurrentFilters()
    }

    /**
     * تعيين الحد الأدنى للتقييم
     */
    fun setMinRating(rating: Float) {
        _minRating.value = rating
        applyCurrentFilters()
    }

    /**
     * تبديل إظهار المتاحين فقط
     */
    fun setOnlyAvailable(availableOnly: Boolean) {
        _onlyAvailable.value = availableOnly
        applyCurrentFilters()
    }

    /**
     * تطبيق الفلاتر المجمعة
     */
    fun applyFilters(city: String = _selectedCity.value, rating: Float = _minRating.value, availableOnly: Boolean = _onlyAvailable.value) {
        _selectedCity.value = city
        _minRating.value = rating
        _onlyAvailable.value = availableOnly
        applyCurrentFilters()
    }

    private fun applyCurrentFilters() {
        val query = _searchQuery.value.trim().lowercase()
        val catId = _selectedCategoryId.value
        val city = _selectedCity.value
        val minR = _minRating.value
        val availOnly = _onlyAvailable.value

        var result = _providers.value

        if (!catId.isNullOrBlank()) {
            result = result.filter { it.categoryId == catId }
        }

        if (city != "الكل" && city.isNotBlank()) {
            result = result.filter { it.area.contains(city, ignoreCase = true) || it.localNeighborhood.contains(city, ignoreCase = true) }
        }

        if (minR > 0f) {
            result = result.filter { it.rating >= minR }
        }

        if (availOnly) {
            result = result.filter { it.isAvailable }
        }

        if (query.isNotBlank()) {
            result = result.filter {
                it.name.lowercase().contains(query) ||
                it.profession.lowercase().contains(query) ||
                it.specialization.lowercase().contains(query) ||
                it.area.lowercase().contains(query) ||
                it.phone.contains(query)
            }
        }

        _filteredProviders.value = result
    }

    /**
     * إضافة/إزالة من المفضلة
     */
    fun toggleFavorite(providerId: String) {
        val current = _favoriteProviderIds.value.toMutableSet()
        if (current.contains(providerId)) {
            current.remove(providerId)
        } else {
            current.add(providerId)
        }
        _favoriteProviderIds.value = current
    }

    fun isFavorite(providerId: String): Boolean {
        return _favoriteProviderIds.value.contains(providerId)
    }

    override fun onCleared() {
        super.onCleared()
        categoriesListener?.remove()
        providersListener?.remove()
    }
}
