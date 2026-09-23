package com.example

import com.example.data.*
import com.example.data.models.InstantRequestEntity
import com.example.data.models.RequestOfferEntity
import com.example.utils.BookingStateMachine
import com.example.utils.PasswordHasher
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class FlowsIntegrationUnitTest {

    @Test
    fun testPasswordHashingAndVerification() {
        val plainPassword = "AdminSecurePassword123!"
        val hash = PasswordHasher.hash(plainPassword)
        assertNotNull(hash)
        assertTrue(hash.isNotEmpty())
        assertNotEquals(plainPassword, hash)
        assertTrue(hash.contains(":")) // Formatted as saltBase64:hashBase64

        // Verify salted password hashing using verifyPassword
        assertTrue(PasswordHasher.verifyPassword(plainPassword, hash))
        assertFalse(PasswordHasher.verifyPassword("WrongPassword123!", hash))

        // Blank password or empty stored hash returns false safely
        assertFalse(PasswordHasher.verifyPassword("", hash))
        assertFalse(PasswordHasher.verifyPassword(plainPassword, ""))
    }

    @Test
    fun testBookingStateMachineTransitions() {
        // Direct booking starts at PENDING
        assertTrue(BookingStateMachine.canTransition("PENDING", "ACCEPTED"))
        assertTrue(BookingStateMachine.canTransition("PENDING", "REJECTED"))
        assertTrue(BookingStateMachine.canTransition("PENDING", "CANCELLED"))

        // ACCEPTED to IN_PROGRESS or CANCELLED
        assertTrue(BookingStateMachine.canTransition("ACCEPTED", "IN_PROGRESS"))
        assertTrue(BookingStateMachine.canTransition("ACCEPTED", "CANCELLED"))
        assertFalse(BookingStateMachine.canTransition("ACCEPTED", "COMPLETED")) // Must pass through IN_PROGRESS

        // IN_PROGRESS to COMPLETED or CANCELLED
        assertTrue(BookingStateMachine.canTransition("IN_PROGRESS", "COMPLETED"))
        assertTrue(BookingStateMachine.canTransition("IN_PROGRESS", "CANCELLED"))

        // Terminal states should not transition to PENDING
        assertFalse(BookingStateMachine.canTransition("COMPLETED", "PENDING"))
        assertFalse(BookingStateMachine.canTransition("CANCELLED", "ACCEPTED"))
    }

    @Test
    fun testDirectBookingEntityIntegrity() {
        val booking = BookingEntity(
            id = UUID.randomUUID().toString(),
            customerName = "أحمد محمد",
            customerPhone = "771234567",
            serviceType = "كهرباء منزلية",
            category = "صيانة",
            providerId = "provider_123",
            providerName = "مهندس علي",
            bookingNumber = "BK-2026-001",
            bookingPassword = "654321",
            status = "PENDING"
        )

        assertEquals("PENDING", booking.status)
        assertEquals("771234567", booking.customerPhone)
        assertEquals("BK-2026-001", booking.bookingNumber)
        assertTrue(booking.bookingPassword.length >= 4)
    }

    @Test
    fun testUrgentRequestFlowIntegrity() {
        val now = System.currentTimeMillis()
        val instantReq = InstantRequestEntity(
            id = "req_${UUID.randomUUID()}",
            userId = "user_771234567",
            userName = "سالم",
            userPhone = "771234567",
            serviceTitle = "طوارئ سباكة - تسريب مياه",
            categoryName = "سباكة",
            userCity = "صنعاء",
            status = "WAITING_FOR_OFFERS",
            expiresAt = now + 3600_000L,
            createdAt = now
        )

        assertEquals("WAITING_FOR_OFFERS", instantReq.status)
        assertTrue(instantReq.expiresAt > instantReq.createdAt)

        // Offer response flow
        val offer = RequestOfferEntity(
            id = "offer_${UUID.randomUUID()}",
            requestId = instantReq.id,
            technicianId = "prov_999",
            technicianName = "فني السباكة الماهر",
            technicianPhone = "770000000",
            price = 5000.0,
            status = "PENDING"
        )
        assertEquals(instantReq.id, offer.requestId)
        assertEquals(5000.0, offer.price, 0.01)
        assertEquals("PENDING", offer.status)
    }

    @Test
    fun testUserAndProviderBlockingIntegrity() {
        val activeProvider = ProviderEntity(
            id = "p_1",
            name = "فهد",
            phone = "772223334",
            isBlocked = false,
            isDeleted = false
        )
        val blockedProvider = ProviderEntity(
            id = "p_2",
            name = "سعيد",
            phone = "773334445",
            isBlocked = true,
            isDeleted = false
        )
        val deletedProvider = ProviderEntity(
            id = "p_3",
            name = "طارق",
            phone = "774445556",
            isBlocked = false,
            isDeleted = true
        )

        val allProviders = listOf(activeProvider, blockedProvider, deletedProvider)

        val visibleForClient = allProviders.filter { !it.isDeleted && !it.isBlocked }
        assertEquals(1, visibleForClient.size)
        assertEquals("p_1", visibleForClient.first().id)

        val adminManageable = allProviders.filter { !it.isDeleted }
        assertEquals(2, adminManageable.size)
    }

    @Test
    fun testBroadcastNotificationAndDedupKey() {
        val notif1 = NotificationEntity(
            id = "notif_1",
            title = "تنبيه هام",
            message = "تم تحديث النظام",
            targetType = "ALL",
            dedupKey = "UPDATE_V2",
            timestamp = 1000L
        )

        val notif2 = NotificationEntity(
            id = "notif_2",
            title = "تنبيه هام مكرر",
            message = "تم تحديث النظام مجدداً",
            targetType = "ALL",
            dedupKey = "UPDATE_V2",
            timestamp = 2000L
        )

        val notif3 = NotificationEntity(
            id = "notif_3",
            title = "إشعار مختلف",
            message = "عرض خاص",
            targetType = "ALL",
            dedupKey = "OFFER_SPRING",
            timestamp = 3000L
        )

        val notifications = listOf(notif1, notif2, notif3)

        val deduplicated = notifications.distinctBy { it.dedupKey.ifBlank { it.id } }
        assertEquals(2, deduplicated.size)
        assertTrue(deduplicated.any { it.dedupKey == "UPDATE_V2" })
        assertTrue(deduplicated.any { it.dedupKey == "OFFER_SPRING" })
    }
}
