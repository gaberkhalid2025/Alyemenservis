package com.example.ui.screens.notifications

import com.example.data.NotificationEntity
import com.example.ui.helpers.AppPreferenceHelper

/**
 * 🎯 NotificationAudienceFilter
 * محرك تصفية وفحص استحقاق الإشعارات حسب الجمهور المستهدف (Audience) والأدوار والمناطق والمستخدمين.
 */
object NotificationAudienceFilter {

    private fun isTargetUserMatched(
        notif: NotificationEntity,
        cleanPhone: String,
        cleanUserId: String
    ): Boolean {
        val targetTokens = notif.targetValue
            .split(",", "،", ";", "|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val phoneMatched = cleanPhone.isNotEmpty() && (
            targetTokens.any { token ->
                val normToken = AppPreferenceHelper.normalizePhoneNumber(token)
                normToken == cleanPhone || token == cleanPhone
            } ||
            notif.targetUserIds.any { id ->
                val normId = AppPreferenceHelper.normalizePhoneNumber(id)
                normId == cleanPhone || id.trim() == cleanPhone
            }
        )

        val uidMatched = cleanUserId.isNotEmpty() && (
            targetTokens.any { it == cleanUserId } ||
            notif.targetUserIds.any { it.trim() == cleanUserId }
        )

        return phoneMatched || uidMatched
    }

    fun isAudienceMatched(
        notif: NotificationEntity,
        cleanPhone: String,
        cleanUserId: String,
        isAdmin: Boolean,
        isRegistered: Boolean,
        isProvider: Boolean,
        currentResidence: String = "",
        userCategory: String = ""
    ): Boolean {
        if (!notif.isValid() || notif.isExpired()) return false

        val isSensitive = notif.notificationType.uppercase() in setOf(
            "PASSWORD_RESET", "RECOVERY", "OTP", "SECURITY", "ACCOUNT_RECOVERY"
        ) || notif.title.contains("كلمة مرور") ||
            notif.message.contains("كلمة المرور") ||
            notif.title.contains("استعادة") ||
            notif.title.contains("رمز التحقق")

        if (isSensitive) {
            val isMyTarget = isTargetUserMatched(notif, cleanPhone, cleanUserId)
            if (!isAdmin && !isMyTarget) return false
        }

        if (isAdmin) return true
        val audience = notif.targetAudience.trim().uppercase()
        if (!isRegistered && audience != "ALL") return false

        return when (audience) {
            "ADMIN_ONLY" -> false
            "ALL_REGISTERED_USERS" -> isRegistered
            "SPECIFIC_ROLES", "ROLE" -> {
                notif.targetRoles.any { r ->
                    when (r.trim().uppercase()) {
                        "TECHNICIAN", "PROVIDER" -> isProvider
                        "STORE", "RESTAURANT", "MEDICAL", "REAL_ESTATE", "PROPERTY" -> isProvider
                        "USER" -> isRegistered
                        else -> false
                    }
                }
            }
            "SPECIFIC_USERS", "SPECIFIC_USER" -> {
                isTargetUserMatched(notif, cleanPhone, cleanUserId)
            }
            "REGION" -> {
                val targetRegion = notif.targetValue.trim()
                val userRegion = currentResidence.trim()
                targetRegion.isEmpty() || (
                    userRegion.isNotEmpty() && (
                        userRegion.equals(targetRegion, ignoreCase = true) ||
                        userRegion.split(",", "،", "-", "/").any { part ->
                            part.trim().equals(targetRegion, ignoreCase = true)
                        }
                    )
                )
            }
            "CATEGORY" -> {
                val targetCategory = notif.targetValue.trim()
                targetCategory.isEmpty() || (
                    userCategory.isNotBlank() &&
                    userCategory.trim().equals(targetCategory, ignoreCase = true)
                )
            }
            "ALL" -> true
            else -> false
        }
    }
}

