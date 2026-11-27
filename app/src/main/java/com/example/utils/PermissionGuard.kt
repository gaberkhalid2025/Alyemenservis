package com.example.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.AdminPermissionsRegistry
import com.example.data.models.AdminRole

object PermissionGuard {
    const val PERMISSION_BOOKINGS = "MANAGE_BOOKINGS"
    const val PERMISSION_RESTAURANTS = "MANAGE_RESTAURANTS"
    const val PERMISSION_NOTIFICATIONS = "MANAGE_NOTIFICATIONS"
    const val PERMISSION_BANNERS = "MANAGE_BANNERS"
    const val PERMISSION_REG_FORMS = "MANAGE_REG_FORMS"
    const val PERMISSION_QUICK_SERVICE = "MANAGE_QUICK_SERVICE"
    const val PERMISSION_CHAT = "MANAGE_ADVANCED_CHAT"
    const val PERMISSION_THEMES = "MANAGE_THEMES"
    const val PERMISSION_SECTIONS = "MANAGE_NEW_SECTIONS"
    const val PERMISSION_MAP = "MANAGE_MAP"
    const val PERMISSION_STORES = "MANAGE_STORES"
    const val PERMISSION_MEDICAL = "MANAGE_MEDICAL"
    const val PERMISSION_PROPERTIES = "MANAGE_PROPERTIES"
    const val PERMISSION_JOBS = "MANAGE_JOBS"
    const val PERMISSION_CUSTOM_TABS = "MANAGE_CUSTOM_TABS"

    private val legacyManageAliases: Map<String, Set<String>> = mapOf(
        "MANAGE_REQUESTS" to setOf("REG_REQ"),
        "MANAGE_PROVIDERS" to setOf("PROVIDERS", "MANUAL_ADD"),
        "MANAGE_BOOKINGS" to setOf("BOOKINGS"),
        "MANAGE_RESTAURANTS" to setOf("RESTAURANTS"),
        "MANAGE_NOTIFICATIONS" to setOf("NOTIFICATIONS"),
        "MANAGE_BANNERS" to setOf("BANNERS"),
        "MANAGE_REG_FORMS" to setOf("REG_FORMS_MANAGER"),
        "MANAGE_QUICK_SERVICE" to setOf("QUICK_SERVICE"),
        "MANAGE_ADVANCED_CHAT" to setOf("ADVANCED_CHAT", "CHATS"),
        "MANAGE_THEMES" to setOf("COLORS"),
        "MANAGE_NEW_SECTIONS" to setOf("NEW_SECTION_CREATOR"),
        "MANAGE_MAP" to setOf("MAP_CONTROLS"),
        "MANAGE_STORES" to setOf("STORES"),
        "MANAGE_MALLS" to setOf("STORES", "STORE_MALL_MANAGE", "STORE_MALL_TENANTS"),
        "MANAGE_MEDICAL" to setOf("MEDICAL"),
        "MANAGE_PROPERTIES" to setOf("PROPERTIES"),
        "MANAGE_JOBS" to setOf("JOBS", "APPLICANTS"),
        "MANAGE_CUSTOM_TABS" to setOf("CUSTOM_TABS")
    )

    /**
     * Verifies if the role and assigned permission set has access to a specific permission key.
     * Owner (Main Admin) has ALL permissions automatically and cannot be downgraded.
     */
    fun hasPermission(
        role: AdminRole,
        permission: String,
        supervisorGrantedPermissions: List<String> = emptyList()
    ): Boolean {
        if (role == AdminRole.GUEST) return false
        if (role == AdminRole.OWNER || role == AdminRole.SUPER_ADMIN || role == AdminRole.MAIN_ADMIN) return true

        if (com.example.security.AdminPermissionResolver.hasAccess(supervisorGrantedPermissions, permission)) {
            return true
        }

        // Support legacy MANAGE_* grants mapping to tabKey or category permissions
        val permItem = AdminPermissionsRegistry.allPermissions.find { it.key == permission || it.id == permission }
        val targetTab = permItem?.category?.tabKey ?: permission

        val grantedViaLegacyAlias = supervisorGrantedPermissions.any { granted ->
            legacyManageAliases[granted]?.let { targets ->
                targetTab in targets || permission in targets
            } == true
        }
        if (grantedViaLegacyAlias) return true

        // Category-level check (e.g. if supervisor has "BOOKINGS", they can access all booking permissions)
        if (permItem != null && supervisorGrantedPermissions.contains(permItem.category.tabKey)) {
            return true
        }

        return false
    }

    fun hasPermission(role: String, permission: String, grantedPermissions: List<String> = emptyList()): Boolean {
        return hasPermission(RoleManager.fromRoleString(role), permission, grantedPermissions)
    }

    /**
     * 🛡️ التحقق المتقدم والدقيق من صلاحيات الإجراءات (الإضافة، الحذف، التوثيق، التوصية)
     * لمختلف أقسام المنظومة (المحلات، المطاعم، المراكز الطبية، الفنيين، العقارات، الوظائف).
     */
    fun canPerformAction(
        role: AdminRole,
        categoryPrefix: String,
        actionSuffix: String, // مثل: ADD, DELETE, VERIFY, RECOMMEND, EDIT, VIEW
        grantedPermissions: List<String>
    ): Boolean {
        if (role == AdminRole.GUEST) return false
        if (role == AdminRole.OWNER || role == AdminRole.SUPER_ADMIN || role == AdminRole.MAIN_ADMIN) return true

        val directKey = "${categoryPrefix}_${actionSuffix}".uppercase()
        if (hasPermission(role, directKey, grantedPermissions)) return true

        // التحقق من الصلاحية العامة للقسم أو التبويب
        if (hasPermission(role, categoryPrefix, grantedPermissions)) return true

        // التحقق عبر الرموز البديلة (Legacy / Aliases)
        val legacyKey = "MANAGE_$categoryPrefix"
        if (hasPermission(role, legacyKey, grantedPermissions)) return true

        return false
    }

    fun canPerformAction(
        roleString: String,
        categoryPrefix: String,
        actionSuffix: String,
        grantedPermissions: List<String>
    ): Boolean {
        return canPerformAction(RoleManager.fromRoleString(roleString), categoryPrefix, actionSuffix, grantedPermissions)
    }

    @Composable
    fun UnauthorizedView(
        customMessage: String = "🔒 ليس لديك صلاحية للوصول إلى هذه الميزة",
        subMessage: String = "يرجى التواصل مع المالك أو المدير الرئيسي لمنحك الصلاحية اللازمة."
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "غير مصرح",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "عذراً - وصول غير مصرح!",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = customMessage,
                        color = Color(0xFFFCA5A5),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = subMessage,
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
