package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.example.utils.OfflineRequest
import com.example.utils.CoverageLog
import com.example.utils.LandmarkAddress
import com.example.utils.MapPowerDataPrefs
import com.example.utils.NavWaypoint
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal fun MainViewModel.markFixHelper() {
    _lastFixAtMs.value = System.currentTimeMillis()
    _isUserLocationReal.value = true
}

internal fun MainViewModel.startFreshnessWatchdogHelper() {
    if (freshnessJob?.isActive == true) return
    freshnessJob = viewModelScope.launch {
        while (true) {
            delay(5_000L)
            val limit = if (navTrackingJob?.isActive == true) 15_000L else 90_000L
            if (!_isManualLocationSet.value && _isUserLocationReal.value && System.currentTimeMillis() - _lastFixAtMs.value > limit) {
                _isUserLocationReal.value = false
                _userLocationAccuracyM.value = Float.MAX_VALUE
            }
        }
    }
}

internal fun MainViewModel.openInAppMapHelper(
    latitude: Double,
    longitude: Double,
    label: String = "موقع مشارك",
    startNavigation: Boolean = false
) {
    if (!LandmarkAddress.isValid(latitude, longitude)) {
        triggerToast("⚠️ تعذر قراءة إحداثيات الموقع")
        return
    }
    pendingMapFocusPoint = NavWaypoint(
        name = label.ifBlank { "موقع مشارك" },
        latitude = latitude,
        longitude = longitude
    )
    pendingMapAutoNavigate = startNavigation
    selectedProvider = null
    selectedStore = null
    selectedProperty = null
    selectedJob = null
    navigateToScreen(AppScreens.MAP_VIEW)
}

internal fun MainViewModel.updateUserLocationHelper(lat: Double, lng: Double) {
    _userLatitude.value = lat
    _userLongitude.value = lng
    if (!_isManualLocationSet.value && LandmarkAddress.isValid(lat, lng)) {
        appContext?.let { ctx ->
            try {
                CoverageLog.sample(ctx, lat, lng, com.example.NetworkUtils.hasInternetTransport(ctx))
            } catch (_: Throwable) {}
        }
    }
}

internal fun MainViewModel.setManualUserLocationHelper(lat: Double, lng: Double) {
    if (!LandmarkAddress.isValid(lat, lng)) return
    _isManualLocationSet.value = true
    _userLatitude.value = lat
    _userLongitude.value = lng
    _userLocationAccuracyM.value = 5f
    markFixHelper()
}

internal fun MainViewModel.clearManualUserLocationHelper() {
    _isManualLocationSet.value = false
}

internal fun MainViewModel.startLocationUpdatesHelper() {
    val ctx = appContext ?: return
    try {
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return

        val eco = MapPowerDataPrefs.isEcoMode(ctx)
        val fusedClient = LocationServices.getFusedLocationProviderClient(ctx)

        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc == null || _isManualLocationSet.value) return@addOnSuccessListener
            val isMock = if (Build.VERSION.SDK_INT >= 31) loc.isMock else @Suppress("DEPRECATION") loc.isFromMockProvider
            if (!isMock && LandmarkAddress.isValid(loc.latitude, loc.longitude)) {
                updateUserLocation(loc.latitude, loc.longitude)
                _userLocationAccuracyM.value = if (loc.hasAccuracy()) loc.accuracy else 25f
                markFixHelper()
            }
        }

        val intervalMs = if (eco) 12_000L else 6_000L
        val minDistM = if (eco) 15f else 8f
        val priority = if (eco) Priority.PRIORITY_BALANCED_POWER_ACCURACY else Priority.PRIORITY_HIGH_ACCURACY

        val request = LocationRequest.Builder(priority, intervalMs)
            .setMinUpdateIntervalMillis(if (eco) 6_000L else 3_000L)
            .setMinUpdateDistanceMeters(minDistM)
            .build()

        val cb = locationCallback ?: object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (navTrackingJob?.isActive == true || _isManualLocationSet.value) return
                val loc = result.lastLocation ?: return
                val isMock = if (Build.VERSION.SDK_INT >= 31) loc.isMock else @Suppress("DEPRECATION") loc.isFromMockProvider
                if (!isMock && LandmarkAddress.isValid(loc.latitude, loc.longitude)) {
                    updateUserLocation(loc.latitude, loc.longitude)
                    _userLocationAccuracyM.value = if (loc.hasAccuracy()) loc.accuracy else 20f
                    markFixHelper()
                }
            }
        }.also { locationCallback = it }
        fusedClient.requestLocationUpdates(request, cb, Looper.getMainLooper())
            .addOnSuccessListener { _isGpsTrackingActive.value = true }
            .addOnFailureListener { _isGpsTrackingActive.value = false }
        startFreshnessWatchdogHelper()
    } catch (e: Exception) {
        _isGpsTrackingActive.value = false
        android.util.Log.e("MainViewModel", "Location updates error: ", e)
    }
}

internal fun MainViewModel.stopLocationUpdatesHelper() {
    val ctx = appContext
    val cb = locationCallback
    if (ctx != null && cb != null) {
        try {
            LocationServices.getFusedLocationProviderClient(ctx).removeLocationUpdates(cb)
        } catch (_: Exception) {}
    }
    _isGpsTrackingActive.value = false
}

internal fun MainViewModel.startNavigationTrackingHelper() {
    if (navTrackingJob?.isActive == true) return
    startFreshnessWatchdogHelper()
    val eco = appContext?.let { MapPowerDataPrefs.isEcoMode(it) } == true
    navTrackingJob = viewModelScope.launch {
        try {
            locationService.locationUpdates(
                intervalMs = if (eco) 2_500L else 1_500L,
                maxAccuracyM = 50f,
                minDistanceMeters = if (eco) 5f else 3f
            ).collect { c ->
                _isManualLocationSet.value = false
                updateUserLocation(c.latitude, c.longitude)
                _userLocationAccuracyM.value = c.accuracyMeters
                markFixHelper()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Navigation tracking error: ", e)
        }
    }
}

internal fun MainViewModel.stopNavigationTrackingHelper() {
    navTrackingJob?.cancel()
    navTrackingJob = null
}

internal fun MainViewModel.enqueueSosHelper(
    lat: Double,
    lng: Double,
    accuracyM: Float?,
    message: String
): Boolean {
    if (lat.isNaN() || lng.isNaN() || (lat == 0.0 && lng == 0.0)) return false
    return try {
        val data = mutableMapOf<String, Any>(
            "lat" to lat,
            "lng" to lng,
            "message" to message,
            "createdAt" to System.currentTimeMillis(),
            "status" to "OPEN",
            "uid" to (auth.currentUser?.uid ?: ""),
            "phone" to (auth.currentUser?.phoneNumber ?: currentUserPhone.value)
        )
        if (accuracyM != null && accuracyM < Float.MAX_VALUE) {
            data["accuracyM"] = accuracyM.toDouble()
        }
        offlineQueueManager.addToQueue(
            OfflineRequest(
                type = "SOS",
                priority = 1,
                data = data
            )
        )
        true
    } catch (e: Exception) {
        android.util.Log.e("MainViewModel", "Failed to enqueue SOS: ", e)
        false
    }
}
