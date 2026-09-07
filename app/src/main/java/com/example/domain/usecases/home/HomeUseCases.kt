package com.example.domain.usecases.home

import com.example.data.CategoryEntity
import com.example.data.CityEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.data.PropertyEntity
import com.example.utils.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// Mocks to allow compilation since these shouldn't be created as separate files yet
class CategoryRepository {
    fun observeCategories(): Flow<List<CategoryEntity>> = emptyFlow()
    fun getCities(): Flow<List<CityEntity>> = emptyFlow()
}

class ProviderRepository {
    fun getProviders(limit: Int): Flow<List<ProviderEntity>> = emptyFlow()
    suspend fun getProviderById(id: String): ProviderEntity? = null
}

class StoreRepository {
    fun getStores(limit: Int): Flow<List<StoreEntity>> = emptyFlow()
}

class PropertyRepository {
    fun getProperties(limit: Int): Flow<List<PropertyEntity>> = emptyFlow()
}

/**
 * 🏠 HomeUseCases
 * UseCases موحدة للصفحة الرئيسية والبحث والتصفية
 */
class HomeUseCases(
    private val providerRepository: ProviderRepository,
    private val categoryRepository: CategoryRepository,
    private val storeRepository: StoreRepository,
    private val propertyRepository: PropertyRepository
) {
    
    fun getCategories(): Flow<List<CategoryEntity>> {
        return categoryRepository.observeCategories()
    }
    
    fun getProviders(limit: Int = 250): Flow<List<ProviderEntity>> {
        return providerRepository.getProviders(limit)
    }
    
    fun getStores(limit: Int = 250): Flow<List<StoreEntity>> {
        return storeRepository.getStores(limit)
    }
    
    fun getProperties(limit: Int = 250): Flow<List<PropertyEntity>> {
        return propertyRepository.getProperties(limit)
    }
    
    fun getCities(): Flow<List<CityEntity>> {
        return categoryRepository.getCities()
    }
    
    fun applyFilters(
        providers: List<ProviderEntity>,
        cityId: String = "",
        searchQuery: String = "",
        filterVipOnly: Boolean = false,
        filterAvailableOnly: Boolean = false,
        neighborhood: String = "",
        phoneOrName: String = "",
        radiusKm: Int = 10,
        currentUserResidence: String = ""
    ): List<ProviderEntity> {
        var filtered = providers
        
        if (cityId.isNotBlank()) {
            filtered = filtered.filter { it.cityId == cityId }
        }
        
        if (neighborhood.isNotBlank()) {
            filtered = filtered.filter { 
                it.localNeighborhood.contains(neighborhood, ignoreCase = true) ||
                it.area.contains(neighborhood, ignoreCase = true)
            }
        }
        
        if (filterVipOnly) {
            filtered = filtered.filter { it.isVip }
        }
        
        if (filterAvailableOnly) {
            filtered = filtered.filter { it.isAvailable }
        }
        
        if (phoneOrName.isNotBlank()) {
            val query = phoneOrName.trim().lowercase()
            filtered = filtered.filter {
                it.name.lowercase().contains(query) ||
                it.phone.contains(query) ||
                it.profession.lowercase().contains(query)
            }
        }
        
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.trim().lowercase()
            filtered = filtered.filter {
                it.name.lowercase().contains(query) ||
                it.profession.lowercase().contains(query) ||
                it.specialization.lowercase().contains(query) ||
                it.area.lowercase().contains(query) ||
                it.localNeighborhood.lowercase().contains(query)
            }
        }
        
        filtered = filtered.sortedWith(
            compareByDescending<ProviderEntity> { it.isVip }
                .thenByDescending { it.rating }
                .thenBy { it.name }
        )
        
        return filtered
    }
    
    fun filterProvidersByCategory(
        providers: List<ProviderEntity>,
        categoryId: String?,
        currentUserResidence: String = ""
    ): List<ProviderEntity> {
        if (categoryId == null || categoryId == "ALL") {
            return providers
        }
        
        return providers.filter { 
            it.categoryId == categoryId ||
            it.customCategoryName == categoryId
        }
    }
    
    fun findNearbyProviders(
        providers: List<ProviderEntity>,
        latitude: Double,
        longitude: Double,
        radiusKm: Int = 10
    ): List<ProviderEntity> {
        return providers.filter { provider ->
            val distance = calculateDistance(
                latitude, longitude,
                provider.latitude, provider.longitude
            )
            distance <= radiusKm
        }
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return 6371.0 * c 
    }
}
