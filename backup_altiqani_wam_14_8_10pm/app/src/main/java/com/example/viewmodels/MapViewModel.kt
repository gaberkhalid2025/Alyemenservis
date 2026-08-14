package com.example.viewmodels

import com.example.utils.*

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.data.repositories.MapRepository
import com.example.data.repositories.RealtimeSyncRepository
import com.example.utils.ConnectionManager
import com.example.utils.calculateDistanceInMeters
import com.example.utils.formatDistance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val mapRepo = MapRepository(application)
    private val syncRepo = RealtimeSyncRepository(application)
    val connectionManager = ConnectionManager(application)

    val isOnline: StateFlow<Boolean> = connectionManager.isOnline

    // Position State - Default Sana'a (15.3694, 44.1910)
    val userLatitude = MutableStateFlow(15.3694)
    val userLongitude = MutableStateFlow(44.1910)

    // Filter States
    val searchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow("ALL") // ALL, PROVIDERS, STORES, RESTAURANTS, PROPERTIES
    val maxDistanceKm = MutableStateFlow(20.0f)
    val minRatingFilter = MutableStateFlow(0.0f)
    val onlyAvailableFilter = MutableStateFlow(false)

    // Real-time Lists
    private val _providers = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val providers: StateFlow<List<ProviderEntity>> = _providers.asStateFlow()

    private val _stores = MutableStateFlow<List<StoreEntity>>(emptyList())
    val stores: StateFlow<List<StoreEntity>> = _stores.asStateFlow()

    private val _properties = MutableStateFlow<List<PropertyEntity>>(emptyList())
    val properties: StateFlow<List<PropertyEntity>> = _properties.asStateFlow()

    // Notification toast event when new items arrive in real-time
    private val _newServiceBanner = MutableStateFlow<String?>(null)
    val newServiceBanner: StateFlow<String?> = _newServiceBanner.asStateFlow()

    init {
        startRealtimeListeners()
    }

    private fun startRealtimeListeners() {
        viewModelScope.launch {
            syncRepo.observeProvidersRealtime { message ->
                _newServiceBanner.value = "🆕 $message"
            }.collect { list ->
                _providers.value = list
            }
        }

        viewModelScope.launch {
            syncRepo.observeStoresRealtime { message ->
                _newServiceBanner.value = "🆕 $message"
            }.collect { list ->
                _stores.value = list
            }
        }

        viewModelScope.launch {
            syncRepo.observePropertiesRealtime { message ->
                _newServiceBanner.value = "🆕 $message"
            }.collect { list ->
                _properties.value = list
            }
        }
    }

    fun dismissNewServiceBanner() {
        _newServiceBanner.value = null
    }

    fun updateUserLocation(lat: Double, lng: Double) {
        if (lat != 0.0 && lng != 0.0) {
            userLatitude.value = lat
            userLongitude.value = lng
        }
    }

    // Navigation & Helpers
    fun calculateFormattedDistance(destLat: Double, destLng: Double): String {
        val meters = calculateDistanceInMeters(userLatitude.value, userLongitude.value, destLat, destLng)
        return formatDistance(meters)
    }

    fun calculateEtaText(destLat: Double, destLng: Double): String {
        val meters = calculateDistanceInMeters(userLatitude.value, userLongitude.value, destLat, destLng)
        val km = meters / 1000.0
        return if (km < 1.0) {
            val mins = (km / 5.0 * 60.0).roundToInt().coerceAtLeast(1)
            "⏱️ ~ $mins دقيقة سيراً"
        } else {
            val mins = (km / 40.0 * 60.0).roundToInt().coerceAtLeast(2)
            "🚘 ~ $mins دقيقة بالسيارة"
        }
    }

    fun openExternalDirections(context: Context, destLat: Double, destLng: Double, label: String = "الوجهة") {
        try {
            val gmapsUri = Uri.parse("google.navigation:q=$destLat,$destLng")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmapsUri).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destLat,$destLng")
                context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
            }
        } catch (e: Exception) {
            val fallbackUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$destLat,$destLng")
            context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
        }
    }
}
