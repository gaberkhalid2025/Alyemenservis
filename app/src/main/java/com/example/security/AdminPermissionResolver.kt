package com.example.security

/**
 * 🛡️ AdminPermissionResolver
 * محلل صلاحيات الإدارة الموحد (Document 74)
 * القاعدة الصارمة: الرفض افتراضياً؛ فراغ القائمة يعني لا صلاحيات عدا للمالك.
 */
object AdminPermissionResolver {
    const val ALL = "*"

    /** صلاحيات افتراضية دنيا للمشرف */
    val DEFAULT_SUPERVISOR = setOf("DASHBOARD", "REQUESTS", "SUPPORT_CHAT", "REPORTS_VIEW")
    val DEFAULT_ADMIN = DEFAULT_SUPERVISOR + setOf("USERS", "PROVIDERS", "STORES", "NOTIFICATIONS")

    /**
     * @param role OWNER/ADMIN/SUPERVISOR/...
     * @param explicit صلاحيات الشخص نفسه من مستنده (قد تكون null/فارغة)
     * @param matrix مصفوفة الدور من settings/admin_permissions_matrix (دور ← مفاتيح)
     * القاعدة: المالك فقط يأخذ الكل. غيره: صلاحياته الصريحة ثم مصفوفة دوره ثم الافتراضي الأدنى. لا «الكل عند الفراغ».
     */
    fun resolve(
        role: String,
        explicit: Collection<String>?,
        matrix: Map<String, Collection<String>> = emptyMap()
    ): Set<String> {
        val r = role.uppercase().trim()
        if (r == "OWNER") return setOf(ALL)
        val own = explicit?.filter { it.isNotBlank() }?.toSet().orEmpty()
        if (own.isNotEmpty()) return own
        val byRole = matrix[r]?.filter { it.isNotBlank() }?.toSet().orEmpty()
        if (byRole.isNotEmpty()) return byRole
        return when (r) {
            "ADMIN" -> DEFAULT_ADMIN
            "SUPERVISOR" -> DEFAULT_SUPERVISOR
            else -> emptySet()
        }
    }

    fun hasAccess(perms: Set<String>, key: String): Boolean = ALL in perms || key in perms
}
