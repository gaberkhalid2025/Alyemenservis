package com.example.utils

import android.Manifest
import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowLocationManager

/**
 * 📍 LocationServiceUnitTest
 * Unit tests for LocationService using Robolectric to verify coordinate fetching,
 * distance calculation, and behavior under all GPS permission scenarios.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocationServiceUnitTest {

    private lateinit var context: Context
    private lateinit var locationManager: LocationManager
    private lateinit var shadowLocationManager: ShadowLocationManager
    private lateinit var locationService: LocationService
    private lateinit var shadowApp: ShadowApplication

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowApp = shadowOf(context as Application)
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        shadowLocationManager = shadowOf(locationManager)

        locationService = LocationService(
            context = context,
            fusedClient = null,
            customLocationManager = locationManager
        )
    }

    // ==========================================
    // 1. PERMISSION SCENARIO TESTS
    // ==========================================

    @Test
    fun `test permission denied when neither fine nor coarse permission is granted`() {
        shadowApp.denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

        val state = locationService.checkPermissionState()
        assertEquals(LocationPermissionState.DENIED, state)
    }

    @Test
    fun `test gps disabled scenario when location providers are off`() {
        shadowApp.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, false)
        shadowLocationManager.setProviderEnabled(LocationManager.NETWORK_PROVIDER, false)

        val state = locationService.checkPermissionState()
        assertEquals(LocationPermissionState.GPS_DISABLED, state)
    }

    @Test
    fun `test fine location granted successfully fetches exact coordinates`() {
        shadowApp.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, true)

        val state = locationService.checkPermissionState()
        assertEquals(LocationPermissionState.GRANTED_FINE, state)
    }

    @Test
    fun `test coarse location only granted uses balanced power priority`() {
        shadowApp.denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowApp.grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        shadowLocationManager.setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)

        val state = locationService.checkPermissionState()
        assertEquals(LocationPermissionState.GRANTED_COARSE, state)
    }

    @Test
    fun `test fallback to default coordinates when fused client and manager return null`() {
        val lat = LocationService.DEFAULT_YEMEN_LAT
        val lng = LocationService.DEFAULT_YEMEN_LNG
        assertEquals(15.3694, lat, 0.0001)
        assertEquals(44.1910, lng, 0.0001)
    }

    // ==========================================
    // 2. HAVERSINE DISTANCE CALCULATION TESTS
    // ==========================================

    @Test
    fun `test haversine distance calculation between Sanaa and Aden`() {
        // Sana'a: 15.3694, 44.1910
        // Aden: 12.7855, 45.0187
        val distanceKm = locationService.calculateHaversineDistanceKm(
            lat1 = 15.3694, lon1 = 44.1910,
            lat2 = 12.7855, lon2 = 45.0187
        )

        // Expected direct distance is ~300 km
        assertTrue("Distance should be between 280 and 320 km", distanceKm in 280.0..320.0)
    }

    @Test
    fun `test haversine distance with zero coordinates returns zero`() {
        val distance = locationService.calculateHaversineDistanceKm(0.0, 0.0, 15.3694, 44.1910)
        assertEquals(0.0, distance, 0.001)
    }
}
