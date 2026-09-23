package com.example.utils

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

/**
 * 📍 LocationServiceUnitTest
 * Unit tests for LocationService using MockK and Robolectric to verify coordinate fetching,
 * distance calculation, and behavior under all GPS permission scenarios.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocationServiceUnitTest {

    private lateinit var context: Context
    private lateinit var mockFusedClient: FusedLocationProviderClient
    private lateinit var mockLocationManager: LocationManager
    private lateinit var locationService: LocationService
    private lateinit var shadowApp: ShadowApplication

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowApp = shadowOf(context as Application)
        mockFusedClient = mockk(relaxed = true)
        mockLocationManager = mockk(relaxed = true)

        locationService = LocationService(
            context = context,
            fusedClient = mockFusedClient,
            customLocationManager = mockLocationManager
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // ==========================================
    // 1. PERMISSION SCENARIO TESTS
    // ==========================================

    @Test
    fun `test permission denied when neither fine nor coarse permission is granted`() = runBlocking {
        shadowApp.denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

        every { mockLocationManager.isProviderEnabled(any()) } returns true

        val result = locationService.getCurrentCoordinates()
        assertTrue(result is LocationResult.PermissionDenied)
    }

    @Test
    fun `test gps disabled scenario when location providers are off`() = runBlocking {
        shadowApp.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        every { mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns false
        every { mockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } returns false

        val result = locationService.getCurrentCoordinates()
        assertTrue(result is LocationResult.GpsDisabled)
    }

    @Test
    fun `test fine location granted successfully fetches exact coordinates`() = runBlocking {
        shadowApp.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        every { mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns true

        val mockLocation = mockk<Location>(relaxed = true)
        every { mockLocation.latitude } returns 15.3694
        every { mockLocation.longitude } returns 44.1910
        every { mockLocation.accuracy } returns 5.0f
        every { mockLocation.time } returns 1700000000L

        val task: Task<Location> = Tasks.forResult(mockLocation)
        every { mockFusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, any<CancellationToken>()) } returns task

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

        every { mockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } returns true

        val mockLocation = mockk<Location>(relaxed = true)
        every { mockLocation.latitude } returns 12.7855
        every { mockLocation.longitude } returns 45.0187
        every { mockLocation.accuracy } returns 100.0f

        val task: Task<Location> = Tasks.forResult(mockLocation)
        every { mockFusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, any<CancellationToken>()) } returns task

        val result = locationService.getCurrentCoordinates()
        assertTrue(result is LocationResult.Success)

        val coords = (result as LocationResult.Success).coordinates
        assertEquals(12.7855, coords.latitude, 0.0001)
        assertEquals(45.0187, coords.longitude, 0.0001)
    }

    @Test
    fun `test fallback to default coordinates when fused client and manager return null`() = runBlocking {
        shadowApp.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        every { mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns true
        every { mockLocationManager.getLastKnownLocation(any()) } returns null

        val task: Task<Location?> = Tasks.forResult(null)
        every { mockFusedClient.getCurrentLocation(any<Int>(), any<CancellationToken>()) } returns task

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
