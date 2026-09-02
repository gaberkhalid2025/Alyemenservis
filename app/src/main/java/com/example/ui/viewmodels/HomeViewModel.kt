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

    // ==========================================
    // 🗺️ Map & Location Utilities (Transferred from MapViewModel)
    // ==========================================
    internal val _userLatitude = MutableStateFlow(15.3694) // Default Sana'a
    val userLatitude: StateFlow<Double> = _userLatitude.asStateFlow()

    internal val _userLongitude = MutableStateFlow(44.1910)
    val userLongitude: StateFlow<Double> = _userLongitude.asStateFlow()

    internal val _selectedCityName = MutableStateFlow("صنعاء")
    val selectedCityName: StateFlow<String> = _selectedCityName.asStateFlow()

    internal val _isManualLocationMode = MutableStateFlow(false)
    val isManualLocationMode: StateFlow<Boolean> = _isManualLocationMode.asStateFlow()

    fun updateUserLocation(lat: Double, lng: Double) {
        if (lat != 0.0 && lng != 0.0) {
            _userLatitude.value = lat
            _userLongitude.value = lng
        }
    }

    fun setManualLocation(lat: Double, lng: Double, cityName: String = "موقع مخصص") {
        _isManualLocationMode.value = true
        _userLatitude.value = lat
        _userLongitude.value = lng
        _selectedCityName.value = cityName
    }

    fun selectYemeniCity(cityName: String) {
        val coords = when (cityName) {
            "عدن" -> Pair(12.7855, 45.0187)
            "تعز" -> Pair(13.5795, 44.0209)
            "إب" -> Pair(13.9667, 44.1833)
            "الحديدة" -> Pair(14.7978, 42.9545)
            "المكلا" -> Pair(14.5425, 49.1242)
            "مأرب" -> Pair(15.4633, 45.3267)
            "ذمار" -> Pair(14.5427, 44.4051)
            else -> Pair(15.3694, 44.1910) // صنعاء
        }
        setManualLocation(coords.first, coords.second, cityName)
    }

    fun calculateFormattedDistance(destLat: Double, destLng: Double): String {
        val meters = com.example.utils.calculateDistanceInMeters(_userLatitude.value, _userLongitude.value, destLat, destLng)
        return com.example.utils.formatDistance(meters)
    }

    fun calculateEtaText(destLat: Double, destLng: Double): String {
        val meters = com.example.utils.calculateDistanceInMeters(_userLatitude.value, _userLongitude.value, destLat, destLng)
        val km = meters / 1000.0
        return if (km < 1.0) {
            val mins = (km / 5.0 * 60.0).toInt().coerceAtLeast(1)
            "⏱️ ~ $mins دقيقة سيراً"
        } else {
            val mins = (km / 40.0 * 60.0).toInt().coerceAtLeast(2)
            "🚘 ~ $mins دقيقة بالسيارة"
        }
    }

    fun openExternalDirections(context: android.content.Context, destLat: Double, destLng: Double, label: String = "الوجهة") {
        try {
            val gmapsUri = android.net.Uri.parse("google.navigation:q=$destLat,$destLng")
            val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmapsUri).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val browserUri = android.net.Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destLat,$destLng")
                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, browserUri))
            }
        } catch (e: Exception) {
            val fallbackUri = android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=$destLat,$destLng")
            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, fallbackUri))
        }
    }
}
