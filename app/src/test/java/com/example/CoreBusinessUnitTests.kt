package com.example

import com.example.domain.entities.RegistrationEntity
import com.example.domain.usecases.*
import com.example.util.PermissionManager
import com.example.utils.BookingUtils
import com.example.util.SecurityCryptoUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CoreBusinessUnitTests {

    @Test
    fun testBookingNumberGeneration() {
        val bookingNum = BookingUtils.generateBookingNumber("YEMEN")
        assertTrue(bookingNum.startsWith("YEMEN-"))
        assertEquals(3, bookingNum.split("-").size)
    }

    @Test
    fun testBookingPasswordGeneration() {
        val pass = BookingUtils.generateBookingPassword(6)
        assertEquals(6, pass.length)
        assertTrue(pass.all { it.isDigit() })
    }

    @Test
    fun testPasswordPolicyValidation() {
        val (shortValid, shortError) = SecurityCryptoUtils.validatePasswordPolicy("123")
        assertFalse(shortValid)
        assertNotNull(shortError)

        val (valid, error) = SecurityCryptoUtils.validatePasswordPolicy("secret123")
        assertTrue(valid)
        assertNull(error)
    }

    @Test
    fun testCategoryMapSerialization() {
        val cat = Category(1, "صيانة كهرباء", "⚡", 2)
        val map = cat.toMap()
        assertEquals(1, map["id"])
        assertEquals("صيانة كهرباء", map["name"])
        assertEquals("⚡", map["icon"])

        val reconstructed = Category.fromMap(map)
        assertEquals(cat.id, reconstructed.id)
        assertEquals(cat.name, reconstructed.name)
        assertEquals(cat.icon, reconstructed.icon)
    }

    @Test
    fun testPermissionManagerRationaleStrings() {
        val cameraRationale = PermissionManager.getRationaleForPermission(PermissionManager.PERMISSION_CAMERA)
        assertTrue(cameraRationale.contains("الكاميرا"))

        val audioRationale = PermissionManager.getRationaleForPermission(PermissionManager.PERMISSION_RECORD_AUDIO)
        assertTrue(audioRationale.contains("الميكروفون"))

        val locationRationale = PermissionManager.getRationaleForPermission(PermissionManager.PERMISSION_FINE_LOCATION)
        assertTrue(locationRationale.contains("موقعك الجغرافي"))
    }

    @Test
    fun testRegisterStoreUseCaseValidation() = runBlocking {
        val fakeRepo = com.example.data.repositories.FakeRegistrationRepository()
        val useCase = RegisterStoreUseCase(fakeRepo)

        val invalidStore = RegistrationEntity.Store(
            storeName = "",
            ownerName = "علي",
            phone = "771234567",
            storeCategory = "إلكترونيات",
            city = "صنعاء",
            addressDetails = "شارع حدة",
            passwordHash = "Pass1234"
        )
        val result = useCase(invalidStore)
        assertTrue(result.isFailure)
        assertEquals("يرجى إدخال اسم المتجر/المحل التجاري", result.exceptionOrNull()?.message)
    }

    @Test
    fun testRegisterRestaurantUseCaseValidation() = runBlocking {
        val fakeRepo = com.example.data.repositories.FakeRegistrationRepository()
        val useCase = RegisterRestaurantUseCase(fakeRepo)

        val invalidRest = RegistrationEntity.Restaurant(
            restaurantName = "مطعم الشيباني",
            ownerName = "أحمد",
            phone = "123", // invalid phone
            cuisineType = "شبيات",
            city = "عدن",
            addressDetails = "كريتر",
            passwordHash = "Pass1234"
        )
        val result = useCase(invalidRest)
        assertTrue(result.isFailure)
    }
}

