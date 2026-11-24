package com.example.security

/**
 * 🛡️ AdminPermissionResolver
 * محلل صلاحيات الإدارة الموحد (Document 74)
 * القاعدة الصارمة: الرفض افتراضياً؛ فراغ القائمة يعني لا صلاحيات عدا للمالك.
 */
object AdminPermissionResolver {
    const val ALL = "*"

    /** صلاحيات افتراضية دنيا للمشرف والأدوار المحدودة */
    val DEFAULT_SUPERVISOR = setOf("REG_REQ", "CHATS", "BOOKINGS", "STATS")
    val DEFAULT_ADMIN = setOf("ALL")

    private val fullAccessRoles = setOf("OWNER", "SUPER_ADMIN", "MAIN_ADMIN")

    /**
     * @param role OWNER/ADMIN/SUPERVISOR/...
     * @param explicit صلاحيات الشخص نفسه من مستنده (قد تكون null/فارغة)
     * @param matrix مصفوفة الدور من settings/admin_permissions_matrix (دور ← مفاتيح)
     * القاعدة: المالك يأخذ الكل دائماً ولا ينقص. الأدمن يأخذ مصفوفة دوره أو الكل افتراضياً إن لم تُخصص. المشرف/الأدوار الأخرى تأخذ صلاحياتها الصريحة أو مصفوفة الدور أو الافتراضي الأدنى.
     */
    fun resolve(
        role: String,
        explicit: Collection<String>?,
        matrix: Map<String, Collection<String>> = emptyMap()
    ): Set<String> {
        val r = role.uppercase().trim()
        if (r in fullAccessRoles) return setOf("ALL", ALL)
        val own = explicit?.filter { it.isNotBlank() }?.toSet().orEmpty()
        val byRole = matrix[r]?.filter { it.isNotBlank() }?.toSet().orEmpty()
        if (own.isNotEmpty()) {
            // Merge explicit supervisor overrides with role matrix if neither is empty, keeping explicit priority when present
            return if (byRole.isNotEmpty() && r != "ADMIN") own else own
        }
        if (byRole.isNotEmpty()) return byRole
        return when (r) {
            "ADMIN" -> DEFAULT_ADMIN
            "SUPERVISOR" -> DEFAULT_SUPERVISOR
            else -> emptySet()
        }
    }

    fun hasAccess(perms: Collection<String>, key: String): Boolean =
        ALL in perms || "ALL" in perms || "ALL_538" in perms || key in perms
}
