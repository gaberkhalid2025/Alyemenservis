package com.example.ui.screens.notifications

import com.example.data.NotificationEntity
import org.junit.Assert.*
import org.junit.Test

class NotificationAudienceFilterTest {

    @Test
    fun testAdminAlwaysReceivesValidNotifications() {
        val notif = NotificationEntity(
            id = "n1",
            title = "تنبيه إداري",
            message = "يرجى مراجعة الطلبات",
            targetAudience = "ADMIN_ONLY"
        )
        val matched = NotificationAudienceFilter.isAudienceMatched(
            notif = notif,
            cleanPhone = "777000000",
            cleanUserId = "u1",
            isAdmin = true,
            isRegistered = true,
            isProvider = false
        )
        assertTrue(matched)
    }

    @Test
    fun testNonAdminCannotReceiveAdminOnlyNotification() {
        val notif = NotificationEntity(
            id = "n1",
            title = "تنبيه إداري",
            message = "تقرير مالي",
            targetAudience = "ADMIN_ONLY"
        )
        val matched = NotificationAudienceFilter.isAudienceMatched(
            notif = notif,
            cleanPhone = "777000000",
            cleanUserId = "u1",
            isAdmin = false,
            isRegistered = true,
            isProvider = false
        )
        assertFalse(matched)
    }

    @Test
    fun testAllRegisteredUsersAudience() {
        val notif = NotificationEntity(
            id = "n2",
            title = "عرض جديد",
            message = "خصم 20%",
            targetAudience = "ALL_REGISTERED_USERS"
        )
        // Registered user
        assertTrue(
            NotificationAudienceFilter.isAudienceMatched(
                notif = notif,
                cleanPhone = "777123456",
                cleanUserId = "user_123",
                isAdmin = false,
                isRegistered = true,
                isProvider = false
            )
        )
        // Guest/unregistered user
        assertFalse(
            NotificationAudienceFilter.isAudienceMatched(
                notif = notif,
                cleanPhone = "",
                cleanUserId = "",
                isAdmin = false,
                isRegistered = false,
                isProvider = false
            )
        )
    }

    @Test
    fun testSpecificRolesAudience() {
        val notif = NotificationEntity(
            id = "n3",
            title = "تحديث الفنيين",
            message = "شروط فنية جديدة",
            targetAudience = "SPECIFIC_ROLES",
            targetRoles = listOf("PROVIDER", "TECHNICIAN")
        )

        // For provider
        assertTrue(
            NotificationAudienceFilter.isAudienceMatched(
                notif = notif,
                cleanPhone = "777123456",
                cleanUserId = "prov_1",
                isAdmin = false,
                isRegistered = true,
                isProvider = true
            )
        )

        // For non-provider user
        assertFalse(
            NotificationAudienceFilter.isAudienceMatched(
                notif = notif,
                cleanPhone = "777123456",
                cleanUserId = "user_1",
                isAdmin = false,
                isRegistered = true,
                isProvider = false
            )
        )
    }

    @Test
    fun testSensitiveNotificationSecurity() {
        val notif = NotificationEntity(
            id = "n4",
            title = "رمز التحقق لاستعادة كلمة المرور",
            message = "رمزك هو 1234",
            targetAudience = "SPECIFIC_USERS",
            targetValue = "777111222"
        )

        // Target user can see
        assertTrue(
            NotificationAudienceFilter.isAudienceMatched(
                notif = notif,
                cleanPhone = "777111222",
                cleanUserId = "u_target",
                isAdmin = false,
                isRegistered = true,
                isProvider = false
            )
        )

        // Another user cannot see sensitive notification
        assertFalse(
            NotificationAudienceFilter.isAudienceMatched(
                notif = notif,
                cleanPhone = "777999888",
                cleanUserId = "u_other",
                isAdmin = false,
                isRegistered = true,
                isProvider = false
            )
        )
    }

    @Test
    fun testPhoneSubstringDoesNotMatchAndNormalizedPhoneMatches() {
        val notif = NotificationEntity(
            id = "n5",
            title = "تنبيه خاص",
            message = "مرحباً بك",
            targetAudience = "SPECIFIC_USERS",
            targetValue = "00967771234567"
        )

        // Exact normalized 9-digit Yemeni number should match
        assertTrue(
            NotificationAudienceFilter.isAudienceMatched(
                notif = notif,
                cleanPhone = "771234567",
                cleanUserId = "u_exact",
                isAdmin = false,
                isRegistered = true,
                isProvider = false
            )
        )

        // Partial substring must NOT match
        assertFalse(
            NotificationAudienceFilter.isAudienceMatched(
                notif = notif,
                cleanPhone = "77123456",
                cleanUserId = "u_partial",
                isAdmin = false,
                isRegistered = true,
                isProvider = false
            )
        )
    }

    @Test
    fun testStrictRegionAndCategoryMatchingAndExpiration() {
        val regionNotif = NotificationEntity(
            id = "n6",
            title = "تنبيه منطقة",
            message = "صيانة مجدولة",
            targetAudience = "REGION",
            targetValue = "صنعاء"
        )
        assertTrue(
            NotificationAudienceFilter.isAudienceMatched(
                notif = regionNotif,
                cleanPhone = "777000111",
                cleanUserId = "u1",
                isAdmin = false,
                isRegistered = true,
                isProvider = false,
                currentResidence = "صنعاء - حدة"
            )
        )
        assertFalse(
            NotificationAudienceFilter.isAudienceMatched(
                notif = regionNotif,
                cleanPhone = "777000111",
                cleanUserId = "u1",
                isAdmin = false,
                isRegistered = true,
                isProvider = false,
                currentResidence = "تعز"
            )
        )

        val categoryNotif = NotificationEntity(
            id = "n7",
            title = "تحديث قسم",
            message = "تنبيه لقطاع الكهرباء",
            targetAudience = "CATEGORY",
            targetValue = "كهرباء"
        )
        assertTrue(
            NotificationAudienceFilter.isAudienceMatched(
                notif = categoryNotif,
                cleanPhone = "777000111",
                cleanUserId = "u1",
                isAdmin = false,
                isRegistered = true,
                isProvider = true,
                userCategory = "كهرباء"
            )
        )
        assertFalse(
            NotificationAudienceFilter.isAudienceMatched(
                notif = categoryNotif,
                cleanPhone = "777000111",
                cleanUserId = "u1",
                isAdmin = false,
                isRegistered = true,
                isProvider = true,
                userCategory = "سباكة"
            )
        )

        val expiredNotif = NotificationEntity(
            id = "n8",
            title = "عرض منتهي",
            message = "انتهى العرض",
            targetAudience = "ALL",
            expiryTimestamp = System.currentTimeMillis() - 60_000L
        )
        assertFalse(
            NotificationAudienceFilter.isAudienceMatched(
                notif = expiredNotif,
                cleanPhone = "777000111",
                cleanUserId = "u1",
                isAdmin = false,
                isRegistered = true,
                isProvider = false
            )
        )
    }

    @Test
    fun testStructuredSensitiveNotificationTypeBlocksUnmatchedUser() {
        val otpNotif = NotificationEntity(
            id = "n9",
            title = "تنبيه أمني",
            message = "تم إصدار رمز دخول",
            targetAudience = "SPECIFIC_USERS",
            targetValue = "777111222",
            notificationType = "OTP"
        )
        assertFalse(
            NotificationAudienceFilter.isAudienceMatched(
                notif = otpNotif,
                cleanPhone = "77711122",
                cleanUserId = "u_other",
                isAdmin = false,
                isRegistered = true,
                isProvider = false
            )
        )
        assertTrue(
            NotificationAudienceFilter.isAudienceMatched(
                notif = otpNotif,
                cleanPhone = "777111222",
                cleanUserId = "u_target",
                isAdmin = false,
                isRegistered = true,
                isProvider = false
            )
        )
    }
}
