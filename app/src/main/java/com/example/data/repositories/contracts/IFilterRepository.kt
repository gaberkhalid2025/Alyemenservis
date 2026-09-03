package com.example.data.repositories.contracts

import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.data.PropertyEntity

interface IFilterRepository {
    fun filterProvidersByDistance(
        providers: List<ProviderEntity>,
        userLat: Double,
        userLng: Double,
        maxDistanceKm: Double
    ): List<ProviderEntity>

    fun filterStoresByDistance(
        stores: List<StoreEntity>,
        userLat: Double,
        userLng: Double,
        maxDistanceKm: Double
    ): List<StoreEntity>

    fun filterPropertiesByDistance(
        properties: List<PropertyEntity>,
        userLat: Double,
        userLng: Double,
        maxDistanceKm: Double
    ): List<PropertyEntity>

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double
}
