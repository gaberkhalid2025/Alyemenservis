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
    val points: Int = 0,
    val isVerified: Boolean = true,
    val isRecommended: Boolean = true,
    val numReviews: Int = 24,
    val coverImage: String = "",
    val profileImage: String = "",
    val previewPrice: Double = 1500.0,
    val latitude: Double = 15.3694,
    val longitude: Double = 44.1910,
    val subscriptionExpiry: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
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
    val appName: String = "دليل خدمات اليمن",
    val welcomeMessage: String = "مرحباً بكم في منصة الخدمات اليمنية الشاملة",
    val footerMessage: String = "wam777644670",
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
    val appVersion: String = "V2.6.2026",
    val isSpeechSearchEnabled: Boolean = true,
    
    // Notifications control
    val isNotificationsEnabled: Boolean = true,

    // Chat service states and controls
    val disableChatAll: Boolean = false,
    val disableChatProviders: Boolean = false,
    val disableChatUsers: Boolean = false,
    val chatDisabledAnnouncement: String = "خدمة الدردشة متوقفة حالياً للصيانة، نعتذر عن الإزعاج",

    // Section 10 layout and details customization
    val activeFontFamily: String = "CAIRO", // "CAIRO", "DEFAULT", "TAHOMA", "AMIRI"
    val registrationRequirements: String = "الاسم الثلاثي للعميل,رقم الهاتف,منطقة السكن",
    
    // X and Y scaling positions (as percentages of screen width/height, 0f..1f)
    val assistantPositionX: Float = 0.85f,
    val assistantPositionY: Float = 0.70f,
    val chatPositionX: Float = 0.85f,
    val chatPositionY: Float = 0.82f,
    val chatXOffset: Int = 20,
    val chatYOffset: Int = 80,
    val assistantXOffset: Int = 20,
    val assistantYOffset: Int = 140,

    // Map feature switches
    val isMapFeatureEnabled: Boolean = true,
    val mapPrecisionDigits: Int = 1, // 1 or 2 digits after decimal

    // Card dimensions styles
    val coverHeight: Int = 120,
    val avatarSize: Int = 50,
    val avatarShape: String = "CIRCLE", // "CIRCLE" or "ROUNDED"
    val cardBackgroundHex: String = "#1E293B",
    val providerNameColorHex: String = "#FFFFFF",
    val ratingColorHex: String = "#F59E0B",
    val locationColorHex: String = "#94A3B8",
    val previewPriceColorHex: String = "#10B981",

    // Badges switches
    val showVipBadge: Boolean = true,
    val showVerifiedBadge: Boolean = true,
    val showRecommendedBadge: Boolean = true,
    val vipBadgeColorHex: String = "#D97706",
    val verifiedBadgeColorHex: String = "#3B82F6",
    val recommendedBadgeColorHex: String = "#10B981",

    // Buttons switches and colors
    val showCallButton: Boolean = true,
    val showWhatsappButton: Boolean = true,
    val showDetailsButton: Boolean = true,
    val showBookButton: Boolean = true,
    val callButtonColorHex: String = "#10B981",
    val whatsappButtonColorHex: String = "#25D366",
    val detailsButtonColorHex: String = "#3B82F6",
    val bookButtonColorHex: String = "#F59E0B",
    val buttonsOrder: String = "CALL,WHATSAPP,DETAILS,BOOK",

    // Information rows switches and order
    val showDistance: Boolean = true,
    val showPreviewPrice: Boolean = true,
    val showAvailability: Boolean = true,
    val showReviewsCount: Boolean = true,
    val infoRowsOrder: String = "NAME,RATING,DISTANCE,LOCATION,PRICE,AVAILABILITY",

    // Padding & Spacing
    val elementSpacing: Int = 8,
    val cardPadding: Int = 12,

    // Click press effect scale
    val enableScaleAnimation: Boolean = true,
    val clickScaleRatio: Float = 0.95f
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
    val timestamp: Long = 0L,
    val senderName: String = "" // e.g. "مشرف" or user name
)

@Keep
data class BookingEntity(
    val id: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerArea: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val dateString: String = "",
    val timeString: String = "",
    val status: String = "PENDING" // "PENDING", "APPROVED", "REJECTED"
)

@Keep
data class NotificationEntity(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val targetType: String = "ALL", // "ALL", "REGION", "CATEGORY", "USER"
    val targetValue: String = "",
    val timestamp: Long = 0L
)

@Keep
data class ChatChannelEntity(
    val id: String = "",
    val userName: String = "",
    val lastMessage: String = "",
    val isBlocked: Boolean = false,
    val isProvider: Boolean = false,
    val timestamp: Long = 0L,
    val messages: List<ChatMessageEntity> = emptyList()
)

@Keep
data class SupervisorEntity(
    val id: String = "",
    val name: String = "",
    val role: String = "", // "ADMIN", "AUDITOR", "SUPPORT", "OPERATIONS"
    val passcode: String = ""
)

