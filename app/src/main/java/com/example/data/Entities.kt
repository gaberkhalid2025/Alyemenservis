package com.example.data

import androidx.annotation.Keep

@Keep
data class CategoryEntity(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val order: Int = 0
)

@Keep
data class ProviderEntity(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val categoryId: String = "",
    val area: String = "",
    val isVip: Boolean = false,
    val subscriptionStatus: String = "PENDING", // e.g., "APPROVED"
    val isAvailable: Boolean = true,
    val cityId: String = "",
    val localNeighborhood: String = "",
    val rating: Float = 5.0f,
    val points: Int = 0
)

@Keep
data class PendingProviderEntity(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val categoryId: String = "",
    val area: String = "",
    val localNeighborhood: String = "",
    val status: String = "PENDING",
    val reason: String = ""
)

@Keep
data class BannerEntity(
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val redirectCategory: String = "",
    val type: String = "",
    val size: String = "",
    val duration: Int = 5
)

@Keep
data class AdminSettingsEntity(
    val id: String = "main_settings",
    val appName: String = "اليمن الخدمات",
    val welcomeMessage: String = "مرحباً بكم في منصة الخدمات اليمنية الشاملة",
    val footerMessage: String = "منصة اليمن للخدمات - تسهيل الوصول لمقدمي الخدمة",
    val activeThemeId: String = "EMERALD_YEMEN",
    val customPrimaryHex: String = "#059669",
    val customSecondaryHex: String = "#115E59",
    val isMaintenanceActive: Boolean = false,
    val hidePromoFooter: Boolean = false,
    val assistantHidden: Boolean = false,
    val assistantSize: Int = 56,
    val chatHidden: Boolean = false,
    val chatSize: Int = 56,
    val maxSearchRadiusKm: Int = 20,
    val appVersion: String = "1.0.0",
    val isSpeechSearchEnabled: Boolean = true
)

@Keep
data class ReportEntity(
    val id: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val reporterName: String = "",
    val content: String = ""
)

@Keep
data class ActivityLogEntity(
    val id: String = "",
    val action: String = "",
    val timestamp: Long = 0L
)

@Keep
data class CityEntity(
    val id: String = "",
    val nameAr: String = "",
    val nameEn: String = ""
)

@Keep
data class ChatMessageEntity(
    val id: String = "",
    val senderId: String = "guest",
    val message: String = "",
    val timestamp: Long = 0L
)
