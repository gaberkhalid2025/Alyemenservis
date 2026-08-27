package com.example.util

/**
 * 👑 AdminRole - مستويات وأدوار الإدارة والتحكم في النظام
 */
enum class AdminRole(val level: Int, val roleNameAr: String) {
    OWNER(100, "المالك / المدير العام"),
    SUPER_ADMIN(90, "مدير عام النظام"),
    ADMIN(70, "مدير العمليات"),
    SUPERVISOR(50, "مشرف إداري"),
    SUPPORT(40, "الدعم الفني وخدمة العملاء"),
    AUDITOR(30, "مدقق الحسابات والأمان"),
    OPERATIONS(20, "إدارة الميدان والطلبات"),
    GUEST(0, "زائر / غير مصرح");

    /**
     * التحقق مما إذا كان الدور يملك صلاحيات مساوية أو أعلى من دور آخر
     */
    fun hasEqualOrHigherLevelThan(other: AdminRole): Boolean {
        return this.level >= other.level
    }
}

/**
 * 👥 RoleManager - مدير تعيين وتحليل وتدقيق أدوار المستخدمين والمشرفين
 */
object RoleManager {

    /**
     * تحويل النص إلى كائن AdminRole مع دعم مختلف الصيغ والمصطلحات
     * @param roleStr النص المعبر عن الدور
     * @return الدور المقابل
     */
    fun fromRoleString(roleStr: String?): AdminRole {
        if (roleStr.isNullOrBlank()) return AdminRole.GUEST
        val normalized = roleStr.uppercase().trim()
        return when {
            normalized in listOf("OWNER", "MAIN_ADMIN", "ROOT", "المالك", "المدير العام") -> AdminRole.OWNER
            normalized in listOf("SUPER_ADMIN", "DIRECTOR", "مدير_عام", "مدير عام") -> AdminRole.SUPER_ADMIN
            normalized in listOf("ADMIN", "MANAGER", "مدير", "إدارة") -> AdminRole.ADMIN
            normalized in listOf("SUPERVISOR", "مشرف") -> AdminRole.SUPERVISOR
            normalized in listOf("SUPPORT", "CUSTOMER_SERVICE", "دعم", "خدمة عملاء") -> AdminRole.SUPPORT
            normalized in listOf("AUDITOR", "FINANCE", "مدقق", "حسابات") -> AdminRole.AUDITOR
            normalized in listOf("OPERATIONS", "FIELD", "عمليات") -> AdminRole.OPERATIONS
            else -> AdminRole.GUEST
        }
    }

    /**
     * هل المستخدم يمتلك صلاحيات إدارة النظام الفعلية؟
     */
    fun isAdmin(role: AdminRole): Boolean {
        return role.level >= AdminRole.ADMIN.level
    }

    /**
     * هل المستخدم يمتلك صلاحيات إشراف أو دعم؟
     */
    fun isSupervisorOrAbove(role: AdminRole): Boolean {
        return role.level >= AdminRole.SUPERVISOR.level
    }
}
