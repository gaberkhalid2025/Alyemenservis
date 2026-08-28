package com.example.ui.screens.map

import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.map.components.MarkerRenderer
import com.example.utils.getPropertyCoords
import com.example.utils.getProviderCoords
import com.example.utils.getStoreCoords
import com.example.ui.screens.map.utils.MapDistanceCalculator
import kotlin.math.cos
import kotlin.math.sin

/**
 * 🗺️ MapScreenFilters
 * Resilient filtering engine for Yemeni cities, categories, search queries, and Radar coordinates conversion.
 */
object MapScreenFilters {

    fun matchesCityFilter(
        cityFilter: String,
        entityCityId: String,
        entityArea: String,
        entityNeighborhood: String
    ): Boolean {
        if (cityFilter.isEmpty() || cityFilter == "الكل" || cityFilter == "جميع المدن والمحافظات") return true
        val normFilter = cityFilter.trim().lowercase()
        val combined = "$entityCityId $entityArea $entityNeighborhood".lowercase()

        return when {
            normFilter.contains("صنعاء") -> combined.contains("صنعاء") || combined.contains("sanaa") || combined.contains("ye_san") || entityCityId == "1" || combined.contains("حدة") || combined.contains("السبعين") || combined.contains("التحرير")
            normFilter.contains("عدن") -> combined.contains("عدن") || combined.contains("aden") || combined.contains("ye_ade") || entityCityId == "2" || combined.contains("كريتر") || combined.contains("المعلا") || combined.contains("المنصورة") || combined.contains("الشيخ عثمان")
            normFilter.contains("تعز") -> combined.contains("تعز") || combined.contains("taiz") || combined.contains("ye_tai") || entityCityId == "3" || combined.contains("الحوبان") || combined.contains("صالة")
            normFilter.contains("الحديدة") -> combined.contains("الحديدة") || combined.contains("hodeidah") || combined.contains("ye_hud") || combined.contains("الحالي") || combined.contains("الميناء")
            normFilter.contains("إب") -> combined.contains("إب") || combined.contains("ibb") || combined.contains("ye_ibb") || combined.contains("يريم") || combined.contains("العدين")
            normFilter.contains("حضرموت") || normFilter.contains("المكلا") -> combined.contains("حضرموت") || combined.contains("المكلا") || combined.contains("mukalla") || combined.contains("سيئون") || combined.contains("ye_had")
            normFilter.contains("مأرب") -> combined.contains("مأرب") || combined.contains("marib") || combined.contains("ye_mar")
            normFilter.contains("ذمار") -> combined.contains("ذمار") || combined.contains("dhamar") || combined.contains("ye_dha")
            else -> combined.contains(normFilter)
        }
    }

    fun filterProviders(
        providers: List<ProviderEntity>,
        selectedCategory: String,
        selectedCity: String,
        searchQuery: String
    ): List<ProviderEntity> {
        val cleanQuery = searchQuery.trim().lowercase()
        return providers.filter { p ->
            val matchesCat = selectedCategory == "ALL" || selectedCategory == "PROVIDERS"
            val matchesCity = matchesCityFilter(selectedCity, p.cityId, p.area, p.localNeighborhood)
            val matchesQuery = cleanQuery.isEmpty() ||
                    p.name.lowercase().contains(cleanQuery) ||
                    p.profession.lowercase().contains(cleanQuery) ||
                    p.customCategoryName.lowercase().contains(cleanQuery) ||
                    p.specialization.lowercase().contains(cleanQuery) ||
                    p.area.lowercase().contains(cleanQuery) ||
                    p.localNeighborhood.lowercase().contains(cleanQuery)
            matchesCat && matchesCity && matchesQuery
        }
    }

    fun filterStores(
        stores: List<StoreEntity>,
        selectedCategory: String,
        selectedCity: String,
        searchQuery: String
    ): List<StoreEntity> {
        val cleanQuery = searchQuery.trim().lowercase()
        return stores.filter { s ->
            val isMedical = s.sectionId.contains("medical") || s.categoryId.contains("medical") || s.name.contains("طبي") || s.name.contains("صيدلية") || s.name.contains("مستشفى") || s.name.contains("عيادة")
            val isRestaurant = !isMedical && (s.sectionId.contains("restaurant") || s.categoryId.contains("restaurant") || s.name.contains("مطعم") || s.name.contains("كافيه") || s.name.contains("مأكولات") || s.name.contains("شاورما"))

            val matchesCat = when (selectedCategory) {
                "ALL" -> true
                "STORES" -> !isMedical && !isRestaurant
                "RESTAURANTS" -> isRestaurant
                "MEDICAL" -> isMedical
                else -> false
            }
            val matchesCity = matchesCityFilter(selectedCity, s.cityId, "", s.localNeighborhood)
            val matchesQuery = cleanQuery.isEmpty() ||
                    s.name.lowercase().contains(cleanQuery) ||
                    s.description.lowercase().contains(cleanQuery) ||
                    s.localNeighborhood.lowercase().contains(cleanQuery) ||
                    s.sectionId.lowercase().contains(cleanQuery) ||
                    s.categoryId.lowercase().contains(cleanQuery)
            matchesCat && matchesCity && matchesQuery
        }
    }

    fun filterProperties(
        properties: List<PropertyEntity>,
        selectedCategory: String,
        selectedCity: String,
        searchQuery: String
    ): List<PropertyEntity> {
        val cleanQuery = searchQuery.trim().lowercase()
        return properties.filter { pr ->
            val matchesCat = selectedCategory == "ALL" || selectedCategory == "PROPERTIES"
            val matchesCity = matchesCityFilter(selectedCity, pr.cityId, "", pr.localNeighborhood)
            val matchesQuery = cleanQuery.isEmpty() ||
                    pr.title.lowercase().contains(cleanQuery) ||
                    pr.description.lowercase().contains(cleanQuery) ||
                    pr.localNeighborhood.lowercase().contains(cleanQuery)
            matchesCat && matchesCity && matchesQuery
        }
    }

    fun buildRadarPoints(
        filteredProviders: List<ProviderEntity>,
        filteredStores: List<StoreEntity>,
        filteredProperties: List<PropertyEntity>,
        safeUserLat: Double,
        safeUserLng: Double
    ): List<MarkerRenderer.MapItemPoint> {
        val list = mutableListOf<MarkerRenderer.MapItemPoint>()
        var angle = 0.0

        filteredProviders.forEach { p ->
            val coords = getProviderCoords(p)
            val d = MapDistanceCalculator.calculateDistanceMeters(safeUserLat, safeUserLng, coords.first, coords.second)
            val r = (d / 1000.0).coerceIn(1.0, 30.0).toFloat() * 10f
            val rad = Math.toRadians(angle)
            list.add(
                MarkerRenderer.MapItemPoint(
                    id = p.id,
                    title = p.name,
                    type = "PROVIDER",
                    x = (r * cos(rad)).toFloat(),
                    y = (r * sin(rad)).toFloat(),
                    rating = p.rating.toDouble(),
                    isAvailable = p.isAvailable,
                    originalItem = p
                )
            )
            angle += 40.0
        }

        filteredStores.forEach { s ->
            val coords = getStoreCoords(s)
            val isMedical = s.sectionId.contains("medical") || s.categoryId.contains("medical") || s.name.contains("طبي") || s.name.contains("صيدلية")
            val isRestaurant = !isMedical && (s.sectionId.contains("restaurant") || s.categoryId.contains("restaurant") || s.name.contains("مطعم") || s.name.contains("كافيه"))
            val typeStr = if (isMedical) "MEDICAL" else if (isRestaurant) "RESTAURANT" else "STORE"
            val d = MapDistanceCalculator.calculateDistanceMeters(safeUserLat, safeUserLng, coords.first, coords.second)
            val r = (d / 1000.0).coerceIn(1.0, 30.0).toFloat() * 10f
            val rad = Math.toRadians(angle)
            list.add(
                MarkerRenderer.MapItemPoint(
                    id = s.id,
                    title = s.name,
                    type = typeStr,
                    x = (r * cos(rad)).toFloat(),
                    y = (r * sin(rad)).toFloat(),
                    rating = s.rating.toDouble(),
                    isAvailable = s.isActive,
                    originalItem = s
                )
            )
            angle += 35.0
        }

        filteredProperties.forEach { pr ->
            val coords = getPropertyCoords(pr)
            val d = MapDistanceCalculator.calculateDistanceMeters(safeUserLat, safeUserLng, coords.first, coords.second)
            val r = (d / 1000.0).coerceIn(1.0, 30.0).toFloat() * 10f
            val rad = Math.toRadians(angle)
            list.add(
                MarkerRenderer.MapItemPoint(
                    id = pr.id,
                    title = pr.title,
                    type = "PROPERTY",
                    x = (r * cos(rad)).toFloat(),
                    y = (r * sin(rad)).toFloat(),
                    rating = 5.0,
                    isAvailable = pr.isActive,
                    originalItem = pr
                )
            )
            angle += 45.0
        }
        return list
    }
}
