package com.example.utils

import android.Manifest
import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowLocationManager
import java.lang.reflect.Proxy

/**
 * 📍 LocationServiceUnitTest
 * Unit tests for LocationService using Robolectric and JDK Dynamic Proxy to verify coordinate fetching,
 * distance calculation, and behavior under all GPS permission scenarios.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocationServiceUnitTest {

    private lateinit var context: Context
    private lateinit var locationManager: LocationManager
    private lateinit var shadowLocationManager: ShadowLocationManager
    private lateinit var shadowApp: ShadowApplication
    private var nextLocationsByPriority = mutableMapOf<Int, Location?>()
    private var defaultNextLocation: Location? = null
    private lateinit var fakeFusedClient: FusedLocationProviderClient
    private lateinit var locationService: LocationService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowApp = shadowOf(context as Application)
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        shadowLocationManager = shadowOf(locationManager)
        nextLocationsByPriority.clear()
        defaultNextLocation = null

        fakeFusedClient = Proxy.newProxyInstance(
            FusedLocationProviderClient::class.java.classLoader,
            arrayOf(FusedLocationProviderClient::class.java)
        ) { _, method, args ->
            if (method.name == "getCurrentLocation" && args != null && args.isNotEmpty() && args[0] is Int) {
                val priority = args[0] as Int
                val loc = if (nextLocationsByPriority.containsKey(priority)) {
                    nextLocationsByPriority[priority]
                } else {
                    defaultNextLocation
                }
                Tasks.forResult(loc)
            } else {
                Tasks.forResult(null)
            }
        } as FusedLocationProviderClient

        locationService = LocationService(
            context = context,
            fusedClient = fakeFusedClient,
            customLocationManager = locationManager
        )
    }

    // ==========================================
    // 1. PERMISSION SCENARIO TESTS
    // ==========================================

    @Test
    fun `test permission denied when neither fine nor coarse permission is granted`() = runBlocking {
        shadowApp.denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, true)

        val result = locationService.getCurrentCoordinates()
        assertTrue(result is LocationResult.PermissionDenied)
    }

    @Test
    fun `test gps disabled scenario when location providers are off`() = runBlocking {
        shadowApp.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, false)
        shadowLocationManager.setProviderEnabled(LocationManager.NETWORK_PROVIDER, false)

        val result = locationService.getCurrentCoordinates()
        assertTrue(result is LocationResult.GpsDisabled)
    }

    @Test
    fun `test fine location granted successfully fetches exact coordinates`() = runBlocking {
        shadowApp.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, true)

        val gpsLocation = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = 15.3694
            longitude = 44.1910
            accuracy = 5.0f
            time = 1700000000L
        }
        nextLocationsByPriority[Priority.PRIORITY_HIGH_ACCURACY] = gpsLocation

        val result = locationService.getCurrentCoordinates(useHighAccuracy = true)
        assertTrue(result is LocationResult.Success)

        val coords = (result as LocationResult.Success).coordinates
        assertEquals(15.3694, coords.latitude, 0.0001)
        assertEquals(44.1910, coords.longitude, 0.0001)
        assertEquals(5.0f, coords.accuracyMeters, 0.01f)
    }

    @Test
    fun `test coarse location only granted uses balanced power priority`() = runBlocking {
        shadowApp.denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowApp.grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        shadowLocationManager.setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)

        val netLocation = Location(LocationManager.NETWORK_PROVIDER).apply {
            latitude = 12.7855
            longitude = 45.0187
            accuracy = 100.0f
        }
        nextLocationsByPriority[Priority.PRIORITY_BALANCED_POWER_ACCURACY] = netLocation

        val result = locationService.getCurrentCoordinates()
        assertTrue(result is LocationResult.Success)

        val coords = (result as LocationResult.Success).coordinates
        assertEquals(12.7855, coords.latitude, 0.0001)
        assertEquals(45.0187, coords.longitude, 0.0001)
    }

    @Test
    fun `test fallback to default coordinates when fused client and manager return null`() = runBlocking {
        shadowApp.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        shadowLocationManager.setLastKnownLocation(LocationManager.GPS_PROVIDER, null)
        shadowLocationManager.setLastKnownLocation(LocationManager.NETWORK_PROVIDER, null)
        defaultNextLocation = null

        val result = locationService.getCurrentCoordinates()
        assertTrue(result is LocationResult.Success)

        val coords = (result as LocationResult.Success).coordinates
        assertEquals(LocationService.DEFAULT_YEMEN_LAT, coords.latitude, 0.0001)
        assertEquals(LocationService.DEFAULT_YEMEN_LNG, coords.longitude, 0.0001)
        assertTrue(coords.isMock)
    }

    // ==========================================
    // 2. HAVERSINE DISTANCE CALCULATION TESTS
    // ==========================================

    @Test
    fun `test haversine distance calculation between Sanaa and Aden`() {
        val distanceKm = locationService.calculateHaversineDistanceKm(
            lat1 = 15.3694, lon1 = 44.1910,
            lat2 = 12.7855, lon2 = 45.0187
        )

        assertTrue("Distance should be between 280 and 320 km", distanceKm in 280.0..320.0)
    }

    @Test
    fun `test haversine distance with zero coordinates returns zero`() {
        val distance = locationService.calculateHaversineDistanceKm(0.0, 0.0, 15.3694, 44.1910)
        assertEquals(0.0, distance, 0.001)
    }
}
