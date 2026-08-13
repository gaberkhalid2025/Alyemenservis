package com.example.util

enum class UserRole {
    CUSTOMER,
    TECHNICIAN,
    STORE,
    RESTAURANT,
    MEDICAL,
    PROPERTY,
    JOB_POSTER,
    APPLICANT,
    SUPERVISOR,
    ADMIN,
    OWNER
}

object RoleManager {
    fun getArabicRoleName(role: UserRole): String {
        return when (role) {
            UserRole.CUSTOMER -> "عميل"
            UserRole.TECHNICIAN -> "فني"
            UserRole.STORE -> "محل تجاري"
            UserRole.RESTAURANT -> "مطعم / كافيه"
            UserRole.MEDICAL -> "مركز طبي"
            UserRole.PROPERTY -> "عقاري"
            UserRole.JOB_POSTER -> "معلن وظائف"
            UserRole.APPLICANT -> "متقدم لوظيفة"
            UserRole.SUPERVISOR -> "مشرف رقابي"
            UserRole.ADMIN -> "مسؤول إدارة"
            UserRole.OWNER -> "المالك العام"
        }
    }

    fun canAccessAdminFeatures(role: UserRole): Boolean {
        return role == UserRole.ADMIN || role == UserRole.OWNER || role == UserRole.SUPERVISOR
    }

    fun fromRoleString(roleStr: String): UserRole {
        return when (roleStr.uppercase().trim()) {
            "OWNER" -> UserRole.OWNER
            "ADMIN" -> UserRole.ADMIN
            "SUPERVISOR" -> UserRole.SUPERVISOR
            "TECHNICIAN" -> UserRole.TECHNICIAN
            "STORE" -> UserRole.STORE
            "RESTAURANT" -> UserRole.RESTAURANT
            "MEDICAL" -> UserRole.MEDICAL
            "PROPERTY" -> UserRole.PROPERTY
            "JOB_POSTER" -> UserRole.JOB_POSTER
            "APPLICANT" -> UserRole.APPLICANT
            else -> UserRole.CUSTOMER
        }
    }
}
