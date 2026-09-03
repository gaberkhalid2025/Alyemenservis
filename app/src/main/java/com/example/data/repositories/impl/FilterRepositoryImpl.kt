package com.example.data.repositories.impl

import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.data.PropertyEntity
import com.example.data.repositories.contracts.IFilterRepository
import kotlin.math.*

class FilterRepositoryImpl : IFilterRepository {

    override fun filterProvidersByDistance(
        providers: List<ProviderEntity>,
        userLat: Double,
        userLng: Double,
        maxDistanceKm: Double
    ): List<ProviderEntity> {
        if (maxDistanceKm <= 0) return providers
        return providers.filter { provider ->
            calculateDistance(userLat, userLng, provider.latitude, provider.longitude) <= maxDistanceKm
        }
    }

    override fun filterStoresByDistance(
        stores: List<StoreEntity>,
        userLat: Double,
        userLng: Double,
        maxDistanceKm: Double
    ): List<StoreEntity> {
        if (maxDistanceKm <= 0) return stores
        return stores.filter { store ->
            calculateDistance(userLat, userLng, store.latitude, store.longitude) <= maxDistanceKm
        }
    }

    override fun filterPropertiesByDistance(
        properties: List<PropertyEntity>,
        userLat: Double,
        userLng: Double,
        maxDistanceKm: Double
    ): List<PropertyEntity> {
        if (maxDistanceKm <= 0) return properties
        return properties.filter { property ->
            calculateDistance(userLat, userLng, property.latitude, property.longitude) <= maxDistanceKm
        }
    }

    /**
     * Calculates the distance in kilometers between two points on the Earth's surface using Haversine formula.
     */
    override fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val originLatRad = Math.toRadians(lat1)
        val targetLatRad = Math.toRadians(lat2)

        val a = sin(dLat / 2).pow(2.0) +
                sin(dLon / 2).pow(2.0) *
                cos(originLatRad) *
                cos(targetLatRad)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusKm * c
    }
}
