package com.example.viewmodels

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Looper
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
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
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Keep
data class MapCluster(
    val id: String,
    val centerLatitude: Double,
    val centerLongitude: Double,
    val title: String,
    val itemCount: Int,
    val items: List<ProviderEntity>
)

@Keep
data class HeatmapPoint(
    val latitude: Double,
    val longitude: Double,
    val intensity: Float = 1.0f
)

/**
 * 🗺️ MapViewModel
 * إدارة الخريطة التفاعلية، تحديد الموقع الحي GPS، تجميع النقاط (Clustering)، الخريطة الحرارية (Heatmap)، وتوجيه الملاحة.
 */
class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val mapRepo = MapRepository(application)
    private val syncRepo = RealtimeSyncRepository(application)
    val connectionManager = ConnectionManager(application)

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application)
    private var locationCallback: LocationCallback? = null
    val isGpsTrackingActive = MutableStateFlow(false)

    val isOnline: StateFlow<Boolean> = connectionManager.isOnline

    // Position State - Default Sana'a (15.3694, 44.1910)
    val userLatitude = MutableStateFlow(15.3694)
    val userLongitude = MutableStateFlow(44.1910)
    val selectedCityName = MutableStateFlow("صنعاء")
    val isManualLocationMode = MutableStateFlow(false)

    // Filter States
    val searchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow("ALL") // ALL, PROVIDERS, STORES, RESTAURANTS, MEDICAL, PROPERTIES
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

    // Clusters & Heatmap
    private val _clusters = MutableStateFlow<List<MapCluster>>(emptyList())
    val clusters: StateFlow<List<MapCluster>> = _clusters.asStateFlow()

    private val _heatmapPoints = MutableStateFlow<List<HeatmapPoint>>(emptyList())
    val heatmapPoints: StateFlow<List<HeatmapPoint>> = _heatmapPoints.asStateFlow()

    // Notification toast event when new items arrive in real-time
    private val _newServiceBanner = MutableStateFlow<String?>(null)
    val newServiceBanner: StateFlow<String?> = _newServiceBanner.asStateFlow()

    init {
        startRealtimeListeners()
        startLocationUpdates()
    }

    /**
     * 🛰️ تفعيل نظام الـ GPS عالي الدقة وتحديث الموقع كل 30 ثانية
     */
    fun startLocationUpdates() {
        val hasFine = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            isGpsTrackingActive.value = false
            return
        }

        try {
            // First get last known location immediately
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && !isManualLocationMode.value) {
                    updateUserLocation(loc.latitude, loc.longitude)
                }
            }

            // Setup high accuracy periodic updates every 30 seconds
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30_000L)
                .setMinUpdateIntervalMillis(15_000L)
                .setWaitForAccurateLocation(false)
                .build()

            locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    if (!isManualLocationMode.value) {
                        updateUserLocation(loc.latitude, loc.longitude)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            isGpsTrackingActive.value = true
        } catch (e: SecurityException) {
            e.printStackTrace()
            isGpsTrackingActive.value = false
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
        isGpsTrackingActive.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }

    fun requestSingleGpsFix(onComplete: (Boolean) -> Unit = {}) {
        val hasFine = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            onComplete(false)
            return
        }

        try {
            isManualLocationMode.value = false
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        updateUserLocation(loc.latitude, loc.longitude)
                        onComplete(true)
                    } else {
                        onComplete(false)
                    }
                }
                .addOnFailureListener {
                    onComplete(false)
                }
        } catch (e: SecurityException) {
            onComplete(false)
        }
    }

    fun setManualLocation(lat: Double, lng: Double, cityName: String = "موقع مخصص") {
        isManualLocationMode.value = true
        userLatitude.value = lat
        userLongitude.value = lng
        selectedCityName.value = cityName
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

    private fun startRealtimeListeners() {
        viewModelScope.launch {
            syncRepo.observeProvidersRealtime { message ->
                _newServiceBanner.value = "🆕 $message"
            }.collect { list ->
                _providers.value = list
                computeClustersAndHeatmap(list)
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

    /**
     * حساب التجميع والتوزيع الحراري
     */
    private fun computeClustersAndHeatmap(providers: List<ProviderEntity>) {
        val validProviders = providers.filter { it.latitude != 0.0 && it.longitude != 0.0 }
        
        // Heatmap points
        _heatmapPoints.value = validProviders.map {
            HeatmapPoint(
                latitude = it.latitude,
                longitude = it.longitude,
                intensity = (it.rating.coerceAtLeast(1.0f) / 5.0f)
            )
        }

        // Simple grid-based clustering (~0.05 deg ~ 5km)
        val clusterMap = mutableMapOf<String, MutableList<ProviderEntity>>()
        validProviders.forEach { p ->
            val gridKey = "${(p.latitude * 20).toInt()}_${(p.longitude * 20).toInt()}"
            val group = clusterMap.getOrPut(gridKey) { mutableListOf() }
            group.add(p)
        }

        _clusters.value = clusterMap.map { (key, group) ->
            val avgLat = group.map { it.latitude }.average()
            val avgLng = group.map { it.longitude }.average()
            MapCluster(
                id = key,
                centerLatitude = avgLat,
                centerLongitude = avgLng,
                title = "${group.first().area} (${group.size})",
                itemCount = group.size,
                items = group
            )
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
