package com.example.ui.screens.map

import com.example.data.PropertyEntity
import com.example.ui.*
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.map.components.MarkerRenderer
import com.example.utils.getPropertyCoords
import com.example.utils.getProviderCoords
import com.example.utils.getStoreCoords
import com.example.ui.screens.map.utils.CityAliases
import com.example.ui.screens.map.utils.MapDistanceCalculator
import kotlin.math.cos
import kotlin.math.sin

/**
 * 🗺️ MapScreenFilters
 * Resilient filtering engine for Yemeni cities, categories, search queries, and Radar coordinates conversion.
 */
object MapScreenFilters {

    private fun norm(s: String): String = s.trim().lowercase(java.util.Locale.ROOT)
        .replace(Regex("[\\u064B-\\u0652\\u0640]"), "")
        .replace(Regex("[إأآٱ]"), "ا")
        .replace('ى', 'ي').replace('ئ', 'ي').replace('ؤ', 'و')
        .replace('ة', 'ه')

    fun matchesCityFilter(
        cityFilter: String,
        entityCityId: String,
        entityArea: String,
        entityNeighborhood: String
    ): Boolean {
        if (cityFilter.isEmpty() || cityFilter == "الكل" || cityFilter == "جميع المدن والمحافظات") return true
        val normFilter = norm(cityFilter)

        val matchingKey = CityAliases.ALIASES.keys.firstOrNull { norm(it) == normFilter }
            ?: CityAliases.ALIASES.keys.firstOrNull { val k = norm(it); k.length >= 3 && normFilter.contains(k) }
        val aliases = if (matchingKey != null) CityAliases.ALIASES[matchingKey] ?: listOf(cityFilter) else listOf(cityFilter)

        val text = norm("$entityCityId $entityArea $entityNeighborhood")
        val tokens = text.split(Regex("[^\\p{L}\\p{N}_]+")).filter { it.isNotEmpty() }.toSet()

        return aliases.any { alias ->
            val na = norm(alias)
            when {
                na.isEmpty() -> false
                na.all { it.isDigit() } || na.startsWith("ye_") || na.length < 3 -> tokens.contains(na) // Exact token matching for short/numeric/system codes
                else -> text.contains(na)
            }
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
                    pr.localNeighborhood.lowercase().contains(cleanQuery) ||
                    pr.propertyType.lowercase().contains(cleanQuery)
            matchesCat && matchesCity && matchesQuery
        }
    }

    fun buildRadarPoints(
        filteredProviders: List<ProviderEntity>,
        filteredStores: List<StoreEntity>,
        filteredProperties: List<PropertyEntity>,
        safeUserLat: Double,
        safeUserLng: Double,
        maxRangeKm: Float = 25f,
        hasUser: Boolean = true
    ): List<MarkerRenderer.MapItemPoint> {
        if (!hasUser) return emptyList()                       // لا رادار بلا موقع حقيقي
        val rangeM = maxRangeKm.coerceAtLeast(0.5f) * 1000.0
        val list = mutableListOf<MarkerRenderer.MapItemPoint>()

        fun addPoints(items: List<Any>, typeName: String) {
            items.forEach { item ->
                val lat: Double; val lng: Double; val title: String
                val rating: Double; val isAvailable: Boolean; val id: String
                when (item) {
                    is ProviderEntity -> { id = item.id; lat = item.latitude; lng = item.longitude; title = item.name; rating = item.rating.toDouble(); isAvailable = item.isAvailable }
                    is StoreEntity -> { id = item.id; lat = item.latitude; lng = item.longitude; title = item.name; rating = item.rating.toDouble(); isAvailable = item.isActive }
                    is PropertyEntity -> { id = item.id; lat = item.latitude; lng = item.longitude; title = item.title; rating = 0.0; isAvailable = item.isActive }
                    else -> return@forEach
                }
                if (lat.isNaN() || lng.isNaN() || lat == 0.0 || lng == 0.0) return@forEach     // لا نقطة بلا موقع حقيقي

                val d = MapDistanceCalculator.calculateDistanceMeters(safeUserLat, safeUserLng, lat, lng)
                if (d > rangeM) return@forEach                                                   // خارج المدى المختار
                val fraction = (d / rangeM).toFloat().coerceIn(0.03f, 1f)

                val dLng = Math.toRadians(lng - safeUserLng)
                val lat1 = Math.toRadians(safeUserLat)
                val lat2 = Math.toRadians(lat)
                val y = sin(dLng) * cos(lat2)
                val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
                val azimuth = Math.atan2(y, x)

                val derivedType = when (item) {
                    is StoreEntity -> {
                        val isMedical = item.sectionId.contains("medical") || item.categoryId.contains("medical") || item.name.contains("طبي") || item.name.contains("صيدلية")
                        val isRestaurant = !isMedical && (item.sectionId.contains("restaurant") || item.categoryId.contains("restaurant") || item.name.contains("مطعم") || item.name.contains("كافيه"))
                        if (isMedical) "MEDICAL" else if (isRestaurant) "RESTAURANT" else "STORE"
                    }
                    else -> typeName
                }
                list.add(MarkerRenderer.MapItemPoint(
                    id = id, title = title, type = derivedType,
                    x = (fraction * sin(azimuth)).toFloat(),
                    y = (-fraction * cos(azimuth)).toFloat(),
                    rating = rating, isAvailable = isAvailable, originalItem = item
                ))
            }
        }
        addPoints(filteredProviders, "PROVIDER")
        addPoints(filteredStores, "STORE")
        addPoints(filteredProperties, "PROPERTY")
        return list
    }
}

