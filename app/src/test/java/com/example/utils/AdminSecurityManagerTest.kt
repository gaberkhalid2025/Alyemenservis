package com.example.utils

import com.example.data.AdminSettingsEntity
import com.example.data.SupervisorEntity
import com.example.data.models.AdminRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * 🧪 اختبارات وحدة لـ AdminSecurityManager
 * تغطي التحقق من الصلاحيات، الأدوار، ومنطق مطابقة بيانات الدخول عبر الإعدادات والمشرفين
 */
class AdminSecurityManagerTest {

    @Test
    fun `verifyCredentials - blank username or password returns null`() = runBlocking {
        val res1 = AdminSecurityManager.verifyCredentials("", "pass123")
        val res2 = AdminSecurityManager.verifyCredentials("user", "")
        val res3 = AdminSecurityManager.verifyCredentials("   ", "   ")
        assertNull(res1)
        assertNull(res2)
        assertNull(res3)
    }

    @Test
    fun `fromRoleString - maps roles correctly to AdminRole enum`() {
        assertEquals(AdminRole.OWNER, AdminSecurityManager.fromRoleString("OWNER"))
        assertEquals(AdminRole.OWNER, AdminSecurityManager.fromRoleString("MAIN_ADMIN"))
        assertEquals(AdminRole.SUPER_ADMIN, AdminSecurityManager.fromRoleString("SUPER_ADMIN"))
        assertEquals(AdminRole.ADMIN, AdminSecurityManager.fromRoleString("ADMIN"))
        assertEquals(AdminRole.SUPERVISOR, AdminSecurityManager.fromRoleString("SUPERVISOR"))
        assertEquals(AdminRole.SUPERVISOR, AdminSecurityManager.fromRoleString("SUPPORT"))
        assertEquals(AdminRole.SUPERVISOR, AdminSecurityManager.fromRoleString("AUDITOR"))
        assertEquals(AdminRole.GUEST, AdminSecurityManager.fromRoleString("GUEST"))
        assertEquals(AdminRole.GUEST, AdminSecurityManager.fromRoleString("UNKNOWN_ROLE"))
    }

    @Test
    fun `permissions check functions - enforce hierarchy`() {
        // hasOwnerPermission
        assertTrue(AdminSecurityManager.hasOwnerPermission("OWNER"))
        assertFalse(AdminSecurityManager.hasOwnerPermission("ADMIN"))
        assertFalse(AdminSecurityManager.hasOwnerPermission("SUPERVISOR"))
        assertFalse(AdminSecurityManager.hasOwnerPermission("GUEST"))

        // hasAdminPermission
        assertTrue(AdminSecurityManager.hasAdminPermission("OWNER"))
        assertTrue(AdminSecurityManager.hasAdminPermission("ADMIN"))
        assertFalse(AdminSecurityManager.hasAdminPermission("SUPERVISOR"))
        assertFalse(AdminSecurityManager.hasAdminPermission("GUEST"))

        // hasSupervisorPermission
        assertTrue(AdminSecurityManager.hasSupervisorPermission("OWNER"))
        assertTrue(AdminSecurityManager.hasSupervisorPermission("ADMIN"))
        assertTrue(AdminSecurityManager.hasSupervisorPermission("SUPERVISOR"))
        assertFalse(AdminSecurityManager.hasSupervisorPermission("GUEST"))
    }

    @Test
    fun `verifyCredentials with settings - valid owner email and password returns OWNER`() = runBlocking {
        val pass = "OwnerPass@2026"
        val hashedPass = SecureHasher.hashPassword(pass)
        val settings = AdminSettingsEntity(
            ownerEmail = "custom_owner@yemen.services",
            ownerPassword = hashedPass
        )

        // Matching credentials
        val role = AdminSecurityManager.verifyCredentials(
            username = "custom_owner@yemen.services",
            passwordAttempt = pass,
            settings = settings
        )
        assertEquals("OWNER", role)

        // Wrong password
        val wrongPassRole = AdminSecurityManager.verifyCredentials(
            username = "custom_owner@yemen.services",
            passwordAttempt = "WrongPassword",
            settings = settings
        )
        assertNull(wrongPassRole)

        // Wrong username (does not match settings.ownerEmail)
        val wrongUserRole = AdminSecurityManager.verifyCredentials(
            username = "other_user@yemen.services",
            passwordAttempt = pass,
            settings = settings
        )
        assertNull(wrongUserRole)
    }

    @Test
    fun `verifyCredentials with supervisors list - matching supervisor returns correct role`() = runBlocking {
        val pass = "SupervisorPass@123"
        val hashedPass = SecureHasher.hashPassword(pass)
        val sup = SupervisorEntity(
            id = "sup_001",
            name = "Supervisor One",
            passcodeHash = hashedPass,
            role = "SUPERVISOR"
        )

        val role = AdminSecurityManager.verifyCredentials(
            username = "sup_001",
            passwordAttempt = pass,
            supervisors = listOf(sup)
        )
        assertEquals("SUPERVISOR", role)

        val wrongPass = AdminSecurityManager.verifyCredentials(
            username = "sup_001",
            passwordAttempt = "wrong",
            supervisors = listOf(sup)
        )
        assertNull(wrongPass)
    }
}
