package com.example.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.example.data.BannerEntity
import com.example.data.CategoryEntity
import com.example.data.ProviderEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

open class HomeViewModel : BaseViewModel() {

    internal val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    internal val _providers = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val providers: StateFlow<List<ProviderEntity>> = _providers.asStateFlow()

    internal val _filteredProviders = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val filteredProviders: StateFlow<List<ProviderEntity>> = _filteredProviders.asStateFlow()

    internal val _banners = MutableStateFlow<List<BannerEntity>>(emptyList())
    val banners: StateFlow<List<BannerEntity>> = _banners.asStateFlow()

    internal val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    internal val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    internal val _filterVipOnly = MutableStateFlow(false)
    val filterVipOnly: StateFlow<Boolean> = _filterVipOnly.asStateFlow()

    internal val _filterAvailableOnly = MutableStateFlow(false)
    val filterAvailableOnly: StateFlow<Boolean> = _filterAvailableOnly.asStateFlow()

    internal val _filterByCurrentCityOnly = MutableStateFlow(false)
    val filterByCurrentCityOnly: StateFlow<Boolean> = _filterByCurrentCityOnly.asStateFlow()

    internal val _filterCityId = MutableStateFlow<String?>(null)
    val filterCityId: StateFlow<String?> = _filterCityId.asStateFlow()

    internal val _filterNeighborhoodName = MutableStateFlow("")
    val filterNeighborhoodName: StateFlow<String> = _filterNeighborhoodName.asStateFlow()

    internal val _phoneOrNameFilter = MutableStateFlow("")
    val phoneOrNameFilter: StateFlow<String> = _phoneOrNameFilter.asStateFlow()

    internal val _maxKmRadius = MutableStateFlow(10)
    val maxKmRadius: StateFlow<Int> = _maxKmRadius.asStateFlow()

    fun applyFilters(userResidence: String = "") {
        val allProviders = _providers.value
        val selectedCat = _selectedCategoryId.value
        val query = _searchQuery.value.trim().lowercase()
        val vipOnly = _filterVipOnly.value
        val availOnly = _filterAvailableOnly.value
        val cityId = _filterCityId.value
        val neighborhood = _filterNeighborhoodName.value.trim().lowercase()
        val phoneName = _phoneOrNameFilter.value.trim().lowercase()

        var filtered = allProviders

        if (!selectedCat.isNullOrBlank() && selectedCat != "ALL" && selectedCat != "الكل") {
            filtered = filtered.filter { it.categoryId == selectedCat }
        }
        if (query.isNotEmpty()) {
            filtered = filtered.filter { 
                it.name.lowercase().contains(query) || 
                it.profession.lowercase().contains(query) ||
                it.specialization.lowercase().contains(query) ||
                it.customCategoryName.lowercase().contains(query) ||
                it.area.lowercase().contains(query) || 
                it.localNeighborhood.lowercase().contains(query) ||
                it.phone.contains(query)
            }
        }
        if (vipOnly) {
            filtered = filtered.filter { it.isVip || it.subscriptionStatus == "APPROVED" }
        }
        if (availOnly) {
            filtered = filtered.filter { it.isAvailable }
        }
        if (cityId != null) {
            filtered = filtered.filter { it.cityId == cityId }
        }
        val cleanResidence = userResidence.trim().lowercase()
        if (_filterByCurrentCityOnly.value && cleanResidence.isNotEmpty() && cleanResidence != "الكل" && cleanResidence != "اليمن") {
            filtered = filtered.filter { p ->
                p.area.lowercase().contains(cleanResidence) ||
                p.cityId.lowercase().contains(cleanResidence) ||
                p.localNeighborhood.lowercase().contains(cleanResidence) ||
                cleanResidence.contains(p.area.lowercase())
            }
        }
        if (neighborhood.isNotEmpty()) {
            filtered = filtered.filter { it.localNeighborhood.lowercase().contains(neighborhood) }
        }
        if (phoneName.isNotEmpty()) {
            filtered = filtered.filter { 
                it.name.lowercase().contains(phoneName) || 
                it.phone.contains(phoneName) 
            }
        }

        _filteredProviders.value = filtered
    }

    fun selectCategory(categoryId: String?, userResidence: String = "") {
        _selectedCategoryId.value = categoryId
        applyFilters(userResidence)
        
        try {
            val bundle = android.os.Bundle().apply {
                putString("category_id", categoryId ?: "all")
            }
            com.example.MyApplication.logFirebaseEvent("select_category", bundle)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateSearchQuery(query: String, userResidence: String = "") {
        _searchQuery.value = query
        applyFilters(userResidence)

        try {
            val bundle = android.os.Bundle().apply {
                putString("search_query", query)
            }
            com.example.MyApplication.logFirebaseEvent("search_query", bundle)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleVipFilter(userResidence: String = "") {
        _filterVipOnly.value = !_filterVipOnly.value
        applyFilters(userResidence)
    }

    fun toggleAvailableFilter(userResidence: String = "") {
        _filterAvailableOnly.value = !_filterAvailableOnly.value
        applyFilters(userResidence)
    }

    fun toggleFilterByCurrentCityOnly(enabled: Boolean? = null, userResidence: String = "") {
        _filterByCurrentCityOnly.value = enabled ?: !_filterByCurrentCityOnly.value
        applyFilters(userResidence)
    }

    fun setCityFilter(cityId: String?, userResidence: String = "") {
        _filterCityId.value = cityId
        applyFilters(userResidence)
    }

    fun setNeighborhoodFilter(neighborhood: String, userResidence: String = "") {
        _filterNeighborhoodName.value = neighborhood
        applyFilters(userResidence)
    }

    fun setPhoneOrNameFilter(text: String, userResidence: String = "") {
        _phoneOrNameFilter.value = text
        applyFilters(userResidence)
    }

    fun setRadiusKm(km: Int, userResidence: String = "") {
        _maxKmRadius.value = km
        applyFilters(userResidence)
    }

    fun addBanner(title: String, url: String, redirect: String, type: String, size: String, duration: Int, displayTime: String = "طوال اليوم") {
        val banner = BannerEntity(
            id = "banner_" + UUID.randomUUID().toString().take(6),
            title = title,
            url = url,
            redirectCategory = redirect,
            type = type,
            size = size,
            duration = duration,
            displayTime = displayTime
        )
        db.collection("banners").document(banner.id).set(banner)
        triggerToast("🖼️ تم إضافة إعلان جديد: $title")
    }

    fun deleteBanner(bannerId: String) {
        db.collection("banners").document(bannerId).delete()
        triggerToast("🗑️ تم حذف الإعلان")
    }

    fun addNewCategory(nameAr: String, nameEn: String, icon: String, description: String, parentId: String = "", isMainCategory: Boolean = true) {
        val nextId = UUID.randomUUID().toString().take(6)
        val extraCat = CategoryEntity(
            id = nextId,
            name = nameAr,
            icon = icon,
            order = _categories.value.size + 1,
            parentId = parentId,
            isMainCategory = isMainCategory
        )
        db.collection("categories").document(nextId).set(extraCat)
        triggerToast("📂 تم إضافة القسم الجديد $nameAr بنجاح!")
    }

    fun deleteCategory(categoryId: String) {
        db.collection("categories").document(categoryId).delete()
        triggerToast("🗑️ تم حذف القسم بنجاح")
    }
}
