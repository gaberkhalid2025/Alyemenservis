package com.example.ui.screens.map.components

import androidx.compose.ui.graphics.Color
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.map.utils.OfflineMapManager
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 📐 OfflineMapPointMapper
 * الإسقاط الجغرافي الحقيقي (Equirectangular) وحساب الإزاحة الحتمية (Deterministic Hash Offset)
 * لمزودي الخدمات والمتاجر والمطاعم والمراكز الطبية والعقارات
 */
internal fun buildInteractiveMapPoints(
    nearbyProviders: List<ProviderEntity>,
    nearbyStores: List<StoreEntity>,
    nearbyProperties: List<PropertyEntity>,
    originLat: Double,
    originLng: Double,
    metersPerDegreeLat: Double,
    metersPerDegreeLng: Double,
    selectedCity: String
): List<InteractiveMapPoint> {
    val points = mutableListOf<InteractiveMapPoint>()

    // 1. Providers
    nearbyProviders.forEach { p ->
        val hasValidCoords = p.latitude != 0.0 && p.longitude != 0.0 &&
            !p.latitude.isNaN() && !p.longitude.isNaN() &&
            p.latitude in -90.0..90.0 && p.longitude in -180.0..180.0

        val (xMeters, yMeters) = projectOrFallbackOffset(
            hasValidCoords = hasValidCoords,
            lat = p.latitude,
            lng = p.longitude,
            entityId = p.id,
            cityId = p.cityId,
            selectedCity = selectedCity,
            originLat = originLat,
            originLng = originLng,
            metersPerDegreeLat = metersPerDegreeLat,
            metersPerDegreeLng = metersPerDegreeLng
        )

        points.add(
            InteractiveMapPoint(
                id = p.id,
                name = p.name.ifBlank { "فني متخصص" },
                category = p.profession.ifBlank { "خدمات صيانة" },
                type = "PROVIDER",
                emoji = "👷",
                color = Color(0xFF00E5FF), // Neon Cyan
                xMeters = xMeters,
                yMeters = yMeters,
                rating = p.rating.toDouble().coerceAtLeast(1.0),
                phone = p.phone,
                originalEntity = p,
                isApprox = !hasValidCoords
            )
        )
    }

    // 2. Stores & Restaurants & Medical Centers
    nearbyStores.forEach { s ->
        val hasValidCoords = s.latitude != 0.0 && s.longitude != 0.0 &&
            !s.latitude.isNaN() && !s.longitude.isNaN() &&
            s.latitude in -90.0..90.0 && s.longitude in -180.0..180.0

        val (xMeters, yMeters) = projectOrFallbackOffset(
            hasValidCoords = hasValidCoords,
            lat = s.latitude,
            lng = s.longitude,
            entityId = s.id,
            cityId = s.cityId,
            selectedCity = selectedCity,
            originLat = originLat,
            originLng = originLng,
            metersPerDegreeLat = metersPerDegreeLat,
            metersPerDegreeLng = metersPerDegreeLng
        )

        val isMedical = s.sectionId.contains("medical") || s.categoryId.contains("medical") || s.name.contains("طبي") || s.name.contains("صيدلية")
        val isRestaurant = !isMedical && (s.sectionId.contains("restaurant") || s.categoryId.contains("restaurant") || s.name.contains("مطعم") || s.name.contains("كافيه"))

        val pinColor = when {
            isMedical -> Color(0xFFEC4899)     // Vivid Pink
            isRestaurant -> Color(0xFFF59E0B)  // Vibrant Amber/Orange
            else -> Color(0xFF10B981)          // Emerald Green
        }

        val pinEmoji = when {
            isMedical -> "🏥"
            isRestaurant -> "🍔"
            else -> "🏪"
        }

        points.add(
            InteractiveMapPoint(
                id = s.id,
                name = s.name.ifBlank { "متجر" },
                category = s.description.ifBlank { if (isMedical) "مركز طبي" else if (isRestaurant) "مطعم" else "متجر" },
                type = if (isMedical) "MEDICAL" else if (isRestaurant) "RESTAURANT" else "STORE",
                emoji = pinEmoji,
                color = pinColor,
                xMeters = xMeters,
                yMeters = yMeters,
                rating = s.rating.toDouble().coerceAtLeast(1.0),
                phone = s.phone,
                originalEntity = s,
                isApprox = !hasValidCoords
            )
        )
    }

    // 3. Properties
    nearbyProperties.forEach { prop ->
        val hasValidCoords = prop.latitude != 0.0 && prop.longitude != 0.0 &&
            !prop.latitude.isNaN() && !prop.longitude.isNaN() &&
            prop.latitude in -90.0..90.0 && prop.longitude in -180.0..180.0

        val (xMeters, yMeters) = projectOrFallbackOffset(
            hasValidCoords = hasValidCoords,
            lat = prop.latitude,
            lng = prop.longitude,
            entityId = prop.id,
            cityId = prop.cityId,
            selectedCity = selectedCity,
            originLat = originLat,
            originLng = originLng,
            metersPerDegreeLat = metersPerDegreeLat,
            metersPerDegreeLng = metersPerDegreeLng
        )

        points.add(
            InteractiveMapPoint(
                id = prop.id,
                name = prop.title.ifBlank { "عقار معروض" },
                category = "عقار",
                type = "PROPERTY",
                emoji = "🏠",
                color = Color(0xFF8B5CF6), // Purple / Violet
                xMeters = xMeters,
                yMeters = yMeters,
                rating = 5.0,
                phone = prop.phone,
                originalEntity = prop,
                isApprox = !hasValidCoords
            )
        )
    }

    return points
}

private fun projectOrFallbackOffset(
    hasValidCoords: Boolean,
    lat: Double,
    lng: Double,
    entityId: String,
    cityId: String,
    selectedCity: String,
    originLat: Double,
    originLng: Double,
    metersPerDegreeLat: Double,
    metersPerDegreeLng: Double
): Pair<Float, Float> {
    if (hasValidCoords) {
        val dLng = lng - originLng
        val dLat = lat - originLat
        return Pair((dLng * metersPerDegreeLng).toFloat(), (-dLat * metersPerDegreeLat).toFloat())
    }

    val targetCity = if (cityId.isNotBlank()) cityId else selectedCity
    val cityCoords = OfflineMapManager.getCityCoordinates(targetCity)
    val dLng = cityCoords.longitude - originLng
    val dLat = cityCoords.latitude - originLat
    val baseXMeters = (dLng * metersPerDegreeLng).toFloat()
    val baseYMeters = (-dLat * metersPerDegreeLat).toFloat()

    val idHash = entityId.hashCode().toLong()
    val hashAngle = (abs(idHash) % 360).toDouble() * (Math.PI / 180.0)
    val hashOffsetMeters = 40.0 + (abs(idHash ushr 16) % 120) // بين 40 و 160 متر
    val offsetX = (hashOffsetMeters * cos(hashAngle)).toFloat()
    val offsetY = (hashOffsetMeters * sin(hashAngle)).toFloat()
    return Pair(baseXMeters + offsetX, baseYMeters + offsetY)
}
