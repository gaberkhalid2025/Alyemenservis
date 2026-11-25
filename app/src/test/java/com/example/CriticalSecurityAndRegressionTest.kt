package com.example

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.example.data.AdminSettingsEntity
import com.example.ui.helpers.AppPreferenceHelper
import com.example.ui.screens.map.utils.OfflineTileInterceptor
import com.example.ui.theme.BookingStatusColors
import com.example.utils.DateFormatter
import com.example.utils.PermissionManager
import com.example.utils.SecurityCryptoUtils
import com.google.firebase.firestore.Exclude
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CriticalSecurityAndRegressionTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `generateStrongTempPassword produces policy-compliant strong password`() {
        val passwords = (1..10).map { SecurityCryptoUtils.generateStrongTempPassword(10) }
        passwords.forEach { pw ->
            assertTrue("Length must be >= 10", pw.length >= 10)
            assertNotEquals("1234", pw)
            assertTrue("Must contain uppercase", pw.any { it.isUpperCase() })
            assertTrue("Must contain lowercase", pw.any { it.isLowerCase() })
            assertTrue("Must contain digit", pw.any { it.isDigit() })
            assertTrue("Must pass validatePasswordPolicy", SecurityCryptoUtils.validatePasswordPolicy(pw).first)
        }
        assertTrue("Generated passwords should be randomized", passwords.distinct().size > 5)
    }

    @Test
    fun `SecurityCryptoUtils encrypt and decrypt cross-device round-trip succeeds`() {
        val plain = "AlYemen_Secret_2026_صنعاء"
        val enc = SecurityCryptoUtils.encrypt(plain)
        assertNotEquals(plain, enc)
        assertEquals(plain, SecurityCryptoUtils.decrypt(enc))

        val crossEnc = SecurityCryptoUtils.encryptCrossDevice(plain)
        assertNotEquals(plain, crossEnc)
        assertEquals(plain, SecurityCryptoUtils.decryptCrossDevice(crossEnc))
    }

    @Test
    fun `AppPreferenceHelper normalizePhoneNumber normalizes all Yemeni formats consistently`() {
        val expected = "777123456"
        assertEquals(expected, AppPreferenceHelper.normalizePhoneNumber("777123456"))
        assertEquals(expected, AppPreferenceHelper.normalizePhoneNumber("+967777123456"))
        assertEquals(expected, AppPreferenceHelper.normalizePhoneNumber("00967777123456"))
        assertEquals(expected, AppPreferenceHelper.normalizePhoneNumber("967777123456"))
        assertEquals(expected, AppPreferenceHelper.normalizePhoneNumber("0777123456"))
        assertEquals(expected, AppPreferenceHelper.normalizePhoneNumber(" +967 777-123-456 "))
    }

    @Test
    fun `OfflineTileInterceptor blocks legacy Carto API-key tile URLs and map html has zero Carto refs`() {
        val blockedBasemaps = OfflineTileInterceptor.intercept(
            context,
            "https://a.basemaps.cartocdn.com/light_all/15/19200/14100.png"
        )
        assertEquals(404, blockedBasemaps?.statusCode)

        val blockedCartoApi = OfflineTileInterceptor.intercept(
            context,
            "https://carto.com/basemaps/rastertiles/voyager/10/100/100.png"
        )
        assertEquals(404, blockedCartoApi?.statusCode)

        val mapHtml = context.assets.open("map.html").bufferedReader().use { it.readText() }
        assertFalse("map.html must not contain cartocdn URLs", mapHtml.contains("cartocdn.com", ignoreCase = true))
        assertFalse("map.html must not contain carto basemap URLs", mapHtml.contains("carto.com/basemaps", ignoreCase = true))
    }

    @Test
    fun `PermissionManager and manifest comply with Play Policy photo picker without broad storage perms`() {
        val forbidden = setOf(
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.READ_EXTERNAL_STORAGE"
        )
        val hasStorageField = PermissionManager::class.java.declaredFields.any { field ->
            field.name.contains("STORAGE", ignoreCase = true) || field.name.contains("READ_MEDIA", ignoreCase = true)
        }
        assertFalse("PermissionManager must not expose broad storage permissions", hasStorageField)

        val pkgInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val requested = pkgInfo.requestedPermissions?.toSet() ?: emptySet()
        assertTrue(
            "AndroidManifest must not request broad storage media permissions: ${requested.intersect(forbidden)}",
            requested.intersect(forbidden).isEmpty()
        )
    }

    @Test
    fun `DateFormatter US and UTC formatters output expected canonical strings`() {
        val fixedEpochMs = 1767225600000L // 2026-01-01T00:00:00Z
        assertEquals("2026-01-01T00:00:00Z", DateFormatter.formatIsoUtc(fixedEpochMs))
        assertTrue(DateFormatter.formatDateDashUs(fixedEpochMs).matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        assertTrue(DateFormatter.formatDateKeyUs(fixedEpochMs).matches(Regex("\\d{8}")))
    }

    @Test
    fun `AdminSettingsEntity excludes ownerPassword and omits adminPassword`() {
        val hasAdminPasswordGetter = try {
            AdminSettingsEntity::class.java.getDeclaredMethod("getAdminPassword")
            true
        } catch (_: NoSuchMethodException) {
            false
        }
        assertFalse(hasAdminPasswordGetter)

        val ownerPassMethod = AdminSettingsEntity::class.java.getDeclaredMethod("getOwnerPassword")
        assertTrue(ownerPassMethod.isAnnotationPresent(Exclude::class.java))
    }

    @Test
    fun `BookingStatusColors unifies ACCEPTED and APPROVED status styling and labels`() {
        assertEquals(
            BookingStatusColors.getBookingStatusLabel("ACCEPTED"),
            BookingStatusColors.getBookingStatusLabel("APPROVED")
        )
        assertEquals(
            BookingStatusColors.getBookingStatusColor("ACCEPTED"),
            BookingStatusColors.getBookingStatusColor("APPROVED")
        )
    }
}
