package com.example.data

import androidx.annotation.Keep

@Keep
enum class AttachmentType {
    EXCEL, CSV, PDF, IMAGE, JSON
}

@Keep
data class ProductAttachment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val userId: String = "",
    val type: String = "PDF", // "EXCEL", "CSV", "PDF", "IMAGE", "JSON"
    val url: String = "",
    val fileName: String = "",
    val size: Long = 0,
    val mimeType: String = "",
    val uploadedAt: Long = System.currentTimeMillis(),
    val isPublic: Boolean = true
) {
    companion object {
        fun parseList(jsonStr: String): List<ProductAttachment> {
            if (jsonStr.isBlank()) return emptyList()
            return try {
                jsonStr.split(";;").filter { it.isNotBlank() }.map { chunk ->
                    val parts = chunk.split("||")
                    ProductAttachment(
                        id = parts.getOrElse(0) { java.util.UUID.randomUUID().toString() },
                        userId = parts.getOrElse(1) { "" },
                        type = parts.getOrElse(2) { "PDF" },
                        url = parts.getOrElse(3) { "" },
                        fileName = parts.getOrElse(4) { "" },
                        size = parts.getOrElse(5) { "0" }.toLongOrNull() ?: 0L,
                        mimeType = parts.getOrElse(6) { "" },
                        uploadedAt = parts.getOrElse(7) { "0" }.toLongOrNull() ?: System.currentTimeMillis(),
                        isPublic = parts.getOrElse(8) { "true" }.toBoolean()
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun serializeList(list: List<ProductAttachment>): String {
            return list.joinToString(";;") { item ->
                listOf(
                    item.id,
                    item.userId,
                    item.type,
                    item.url,
                    item.fileName,
                    item.size.toString(),
                    item.mimeType,
                    item.uploadedAt.toString(),
                    item.isPublic.toString()
                ).joinToString("||")
            }
        }
    }
}

@Keep
data class CategoryEntity(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val order: Int = 0,
    val isPinned: Boolean = false,
    val parentId: String = "",
    val isMainCategory: Boolean = true
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
    val numReviews: Int = 0,
    val coverImage: String = "",
    val profileImage: String = "",
    val previewPrice: Double = 1500.0,
    val latitude: Double = 15.3694,
    val longitude: Double = 44.1910,
    val subscriptionExpiry: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
    val workPhotosBase64: List<String> = emptyList(),
    val productAttachmentsJson: String = "",
    val specialOffersJson: String = "",
    val customCategoryName: String = "",
    val profession: String = "",
    val specialization: String = "",
    val chatRecipientId: String = "",
    val isBlocked: Boolean = false,
    val isChatDisabled: Boolean = false,
    val isNotificationsDisabled: Boolean = false,
    val isPaymentRequired: Boolean = false,
    val password: String = "",
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
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
    val reason: String = "",
    val idPhotoBase64: String = "",
    val selfiePhotoBase64: String = "",
    val workPhotosBase64: List<String> = emptyList(),
    val productAttachmentsJson: String = "",
    val customCategoryName: String = "",
    val profession: String = "",
    val specialization: String = "",
    val chatRecipientId: String = "",
    val password: String = ""
)

@Keep
data class BannerEntity(
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val redirectCategory: String = "",
    val type: String = "",
    val size: String = "",
    val duration: Int = 5,
    val displayTime: String = "طوال اليوم",
    val order: Int = 0,
    val targetSection: String = "ALL" // ALL, HOME, STORES, RESTAURANTS, MEDICAL, PROPERTIES, JOBS
)

@Keep
data class AdminSettingsEntity(
    val id: String = "main_settings",
    val appName: String = "دليل خدمات اليمن",
    val welcomeMessage: String = "مرحباً بكم في منصة الخدمات اليمنية الشاملة",
    val footerMessage: String = "777644",
    val footerBgColorHex: String = "#115E59",
    val footerItemsOrder: String = "INFO,BOOKINGS,TEXT,LANG,ADMIN",
    val showInfoIcon: Boolean = true,
    val showBookingsIcon: Boolean = true,
    val showLangIcon: Boolean = true,
    val showAdminIcon: Boolean = true,
    val showFooterText: Boolean = true,
    val infoIconType: String = "INFO",
    val adminIconType: String = "LOCK",
    val langIconType: String = "GLOBE",
    val activeThemeId: String = "EMERALD_YEMEN",
    val customPrimaryHex: String = "#059669",
    val customSecondaryHex: String = "#115E59",
    val customBackgroundHex: String = "#0A0F0D",
    val customSurfaceHex: String = "#121D18",
    val isMaintenanceActive: Boolean = false,
    val hidePromoFooter: Boolean = false,
    val assistantHidden: Boolean = false,
    val showLoyaltyBanner: Boolean = false,
    val assistantSize: Int = 56,
    val chatHidden: Boolean = false,
    val chatSize: Int = 56,
    val maxSearchRadiusKm: Int = 20,
    val appVersion: String = "v2.2026",
    val isSpeechSearchEnabled: Boolean = true,
    val supportPhone: String = "777644",
    val supportWhatsapp: String = "777644",
    val supportEmail: String = "mah73646@gmail.com",
    val adminUsername: String = "meh777644@gmail.com",
    val adminPassword: String = "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918",
    val ownerEmail: String = "mah73646@gmail.com",
    val ownerPassword: String = "a11cd656ca89547ea8e05b5f899ca82fa9c873fc4bb8a81f6f2ab448a918",
    
    // Notifications control
    val isNotificationsEnabled: Boolean = true,

    // Chat service states and controls
    val disableChatAll: Boolean = false,
    val disableChatProviders: Boolean = false,
    val disableChatUsers: Boolean = false,
    val disableChatSupervisors: Boolean = false,
    val chatDisabledAnnouncement: String = "خدمة الدردشة متوقفة حالياً للصيانة، نعتذر عن الإزعاج",
    val allowChatUserToProvider: Boolean = true,
    val allowChatProviderToAdmin: Boolean = true,
    val allowChatUserToAdmin: Boolean = true,
    val chatFontSizeSp: Int = 14,
    val chatBackgroundHex: String = "#1E293B",
    val allowVoiceInput: Boolean = true,
    val allowTextToSpeech: Boolean = true,
    val allowVoiceInputJoinForm: Boolean = true,
    val allowTextToSpeechJoinForm: Boolean = true,
    val allowVoiceInputAssistant: Boolean = true,
    val allowTextToSpeechAssistant: Boolean = true,
    val allowVoiceInputBookingForm: Boolean = true,
    val allowTextToSpeechBookingForm: Boolean = true,
    val approveChatsBeforeProvider: Boolean = false,
    val bookingRouting: String = "BOTH", // "BOTH", "ADMIN", "PROVIDER"

    // Section 10 layout and details customization
    val activeFontFamily: String = "CAIRO", // "CAIRO", "DEFAULT", "TAHOMA", "AMIRI"
    val registrationRequirements: String = "الاسم الثلاثي للفني|Mandatory,رقم الهاتف|Mandatory,قسم الصيانة|Mandatory,المدينة والمحافظة|Mandatory,الحي أو الشارع|Optional,صورة سيلفي شخصية|Optional,صورة الهوية المهنية|Optional,نماذج من أعمالك السابقة|Optional",
    val maxWorkPhotos: Int = 5,
    val showWorkPhotos: Boolean = true,
    val bypassVisitorRegistration: Boolean = true,
    val disableChatFirewall: Boolean = false,
    val disableBookingFirewall: Boolean = true,
    
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
    val mapProvider: String = "MAPLIBRE", // "MAPLIBRE", "GOOGLE", "MAPBOX"
    val mapDefaultZoom: Float = 14f,
    val mapMaxDistanceKm: Int = 20,
    val mapPrecisionDigits: Int = 1, // 1 or 2 digits after decimal

    // Assistant controls
    val isAssistantEnabled: Boolean = true,
    val isAssistantIconVisible: Boolean = true,

    // Card dimensions styles
    val coverHeight: Int = 0,
    val avatarSize: Int = 50,
    val avatarShape: String = "CIRCLE", // "CIRCLE" or "ROUNDED"
    val cardBackgroundHex: String = "#1E293B",
    val providerNameColorHex: String = "#FFFFFF",
    val ratingColorHex: String = "#F59E0B",
    val locationColorHex: String = "#94A3B8",
    val previewPriceColorHex: String = "#10B981",

    // Badges switches
    val showUserIdInsteadOfNameInChat: Boolean = false,
    val requirePaymentProofImage: Boolean = true,
    val enableBookingPaymentStep: Boolean = true,
    val showVipBadge: Boolean = true,
    val showVerifiedBadge: Boolean = true,
    val showRecommendedBadge: Boolean = true,
    val vipBadgeColorHex: String = "#D97706",
    val verifiedBadgeColorHex: String = "#3B82F6",
    val recommendedBadgeColorHex: String = "#10B981",

    // Buttons switches and colors
    val showCallButton: Boolean = true,
    val showVoiceCallButton: Boolean = false,
    val showInstantChatButton: Boolean = true,
    val showReviewButton: Boolean = true,
    val showReportButton: Boolean = true,
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
    val cardMarginHorizontal: Int = 0,
    val cardMarginVertical: Int = 4,

    // Click press effect scale
    val enableScaleAnimation: Boolean = true,
    val clickScaleRatio: Float = 0.95f,

    // Admin customizable booking labels and terms
    val bookingTerms: String = "الشروط: لضمان جدية الحجز يرجى التواجد بالمنزل واستقبال الفني وتأكيد العنوان بدقة عبر الهاتف. الدليل لا يتحمل مسؤولية الاتفاقات الخاصة.",
    val bookingLabelName: String = "الاسم الثنائي/الثلاثي للعميل كامل",
    val bookingLabelPhone: String = "رقم الهاتف اليمني (أرقام فقط)",
    val bookingLabelArea: String = "منطقة السكن والحي بالتفصيل (مثل: صنعاء-شارع الستين)",
    val bookingLabelService: String = "نوع الخدمة المطلوبة بالتفصيل",

    // About App customization fields
    val aboutCoverType: String = "IMAGE", // IMAGE, VIDEO, TEXT
    val aboutCoverContent: String = "https://images.unsplash.com/photo-1542435503-956c469947f6?auto=format&fit=crop&w=600&q=80",
    val aboutCoverBase64: String = "",
    val aboutCustomInfo: String = "تطبيق دليل خدمات اليمن الذكي هو منصة متكاملة مخصصة لربط المستخدمين والعملاء بأمهر الفنيين، المهندسين، ومقدمي الخدمات في شتى المجالات والصيانة في مختلف محافظات الجمهورية اليمنية بسهولة وسرعة فائقة.",

    // Main screen Banner configuration
    val bannerEnabled: Boolean = true,
    val bannerType: String = "TEXT", // "TEXT", "IMAGE", "VIDEO"
    val bannerContent: String = "أهلاً بكم في دليل خدمات اليمن! المنصة الأولى لربط مقدمي الخدمات والمهنيين والمراكز التجارية مع المستخدمين، وانتظروا الإضافات القادمة! ✨",
    val bannerBase64: String = "",
    val bannerLocation: String = "TOP", // "TOP", "BOTTOM"
    val bannerDurationSeconds: Int = 10, // 0 for persistent, otherwise duration in seconds
    val bannerDisplayStyle: String = "SLIDE", // "SLIDE", "FADE", "BLINK", "SCROLL"
    val appDownloadUrl: String = "https://example.com/download_app",
    val allowSendImages: Boolean = true,
    val allowSendVideos: Boolean = true,
    val maxImagesPerChat: Int = 5,
    val maxVideosPerChat: Int = 2,
    val showRefreshIcon: Boolean = false,
    val showSettingsIcon: Boolean = false,
    val headerIconsOrder: String = "MENU,NOTIF,CHAT",
    val headerIconsVisible: String = "MENU,NOTIF,CHAT",
    val categoriesLayoutType: String = "GRID_HORIZONTAL",
    val isFavoritesEnabled: Boolean = true,
    val encryptionType: String = "تشفير آمن سحابي",
    val splashWelcomeMessage: String = "التطبيق الأول في اليمن والوطن العربي الذي يربط مقدمي الخدمات وأصحاب المهن بالمستخدمين فورياً",
    val websiteUrl: String = "https://www.yamandelil.com",
    val telegramUrl: String = "https://t.me/yamandelil",
    val facebookUrl: String = "https://facebook.com/yamandelil",
    val twitterUrl: String = "https://twitter.com/yamandelil",
    val instagramUrl: String = "https://instagram.com/yamandelil",
    val youtubeUrl: String = "https://youtube.com/yamandelil",
    val aboutLayoutOrder: String = "COVER,LOGO,TITLE,ANNOUNCEMENT,ABOUT_CARD,DOWNLOAD_BTN,CONTACTS,SOCIALS",
    val hideTwitter: Boolean = false,
    val hideInstagram: Boolean = false,
    val hideYoutube: Boolean = false,
    val hideFacebook: Boolean = false,
    val hideTelegram: Boolean = false,
    val hideWebsite: Boolean = false,
    
    // Integrated payment configurations
    val isPaymentEnabled: Boolean = true,
    val isBookingPaymentRequired: Boolean = false,
    val requireAdvancePayment: Boolean = false,
    val advancePaymentPercent: Float = 0.30f,
    val minAdvanceAmount: Double = 500.0,
    val maxAdvanceAmount: Double = 50000.0,
    val isCommissionEnabled: Boolean = true,
    val paymentCommissionRate: Float = 0.10f,
    
    // Dynamic controls for Stores & Real Estate sections
    val isStoresEnabled: Boolean = true,
    val isPropertiesEnabled: Boolean = true,
    val dynamicTabsList: String = "الرئيسية,المحلات,العقارات,المفضلة",
    val maxStorePhotos: Int = 5,
    val maxPropertyPhotos: Int = 5,
    val storesRequiredFields: String = "الاسم,الوصف,الهاتف,الموقع",
    val propertiesRequiredFields: String = "العنوان,الوصف,السعر,الهاتف",
    val recommendationsLayout: String = "GRID_HORIZONTAL",
    val storesTabName: String = "المحلات",
    val propertiesTabName: String = "العقارات",
    val storesRegistrationTerms: String = "شروط تسجيل المحل: يرجى إدخال بيانات صحيحة ومطابقة للواقع التجاري وصور واضحة.",
    val propertiesRegistrationTerms: String = "شروط إضافة العقار: يرجى تأكيد ملكية العقار وصحة الصور والأسعار المعروضة.",
    val showStoresPhotosOption: Boolean = true,
    val showPropertiesPhotosOption: Boolean = true,
    val dynamicSectionsData: String = "",
    val isUserPasswordRequired: Boolean = false,
    val isBookingsIconVisible: Boolean = true,
    val isMapsIconVisible: Boolean = true,
    val bookingsIconType: String = "DateRange", // "DateRange", "Star", "Favorite", "Home"
    val bookingsAccessControl: String = "ALL", // "ALL", "REGISTERED_ONLY", "DISABLED"
    val blockedUsersForBookings: String = "",
    
    // 3D Golden Icons, Font Scaling, & Icon Customization Settings
    val topNavIconStyle: String = "GOLDEN_3D", // "GOLDEN_3D", "METALLIC", "MINIMAL"
    val navIconSizeDp: Int = 26,
    val globalFontScale: Float = 1.0f,
    val topHomeIcon: String = "🏠",
    val topMapsIcon: String = "🗺️",
    val topJoinIcon: String = "👤",
    val topNotifIcon: String = "🔔",
    val topChatsIcon: String = "✉️",
    val bottomInfoIcon: String = "ℹ️",
    val bottomBookingsIcon: String = "📅",
    val bottomLangIcon: String = "🌐",
    val bottomAdminIcon: String = "🔒",

    // Advanced Instant Chat System Controls & Routing
    val chatDisplayIdentityMode: String = "NAME_AND_PHONE", // "NAME_ONLY", "NAME_AND_PHONE", "NAME_AND_ID", "PHONE_ONLY"
    val isChatTextEnabled: Boolean = true,
    val isChatAudioEnabled: Boolean = true,
    val isChatImageEnabled: Boolean = true,
    val isChatVideoEnabled: Boolean = true,
    val isChatCallEnabled: Boolean = true,
    val chatRoutingMode: String = "DEFAULT", // "ADMIN_ONLY", "ADMIN_PROVIDERS", "ADMIN_CATEGORIES", "CATEGORY_ONLY", "ADMIN_SUPERVISORS", "SUPERVISORS_ONLY", "DEFAULT"
    val chatBlockedIds: String = "", // Comma-separated IDs/phones blocked from chat
    val chatDisabledCategories: String = "", // Comma-separated category IDs with disabled chat

    // Registration Forms Controls (Default: Providers ON, all others OFF)
    val enableProvidersRegistration: Boolean = true,
    val enableStoresRegistration: Boolean = false,
    val enableRestaurantsRegistration: Boolean = false,
    val enablePropertiesRegistration: Boolean = false,
    val enableMedicalRegistration: Boolean = false,
    val enableJobsRegistration: Boolean = false,

    // Admin Customizable Booking Settings
    val isBookingEnabled: Boolean = true,
    val routingMode: String = "auto_nearest",  // auto_nearest, admin_select, supervisor, specialty, admin_dispatch
    val maxCancellationHours: Int = 8,          // 8 ساعات قبل الموعد
    val maxCancellationAttempts: Int = 3,       // عدد محاولات الإلغاء المسموح بها
    val enableBookingPassword: Boolean = true,
    val bookingPasswordLength: Int = 4,
    val enableUniqueBookingNumber: Boolean = true,
    val bookingNumberPrefix: String = "BK",
    val advancePaymentPercentage: Int = 30,
    val enableProgressTracking: Boolean = true,

    // Advanced Payment, Instant Booking & Deposit Rule Controls
    val linkedCategoriesForInstantBooking: String = "",
    val linkedProvidersForInstantBooking: String = "",
    val linkedProvidersForDeposit: String = "",
    val exemptUsersFromDeposit: String = "",
    val showWalletInProfile: Boolean = true,
    val allowBookingWithoutDeposit: Boolean = true,

    // Voice Call In-App System Controls
    val voiceCallsEnabled: Boolean = true,
    val voiceCallsAllowedCategories: String = "",
    val voiceCallsAllowedProviders: String = "",
    val voiceCallsAllowedUsers: String = "",
    val disableVoiceCalls: Boolean = true,
    val voiceCallsDisabledAnnouncement: String = "",
    val appLanguage: String = "ar",
    val hideTopHeaderBar: Boolean = false,
    val customAppName: String = ""
)

@Keep
data class InternalWalletEntity(
    val id: String = "", // owner id or phone
    val ownerType: String = "PROVIDER", // PROVIDER, STORE, RESTAURANT, CENTER, USER
    val ownerName: String = "",
    val ownerPhone: String = "",
    val balance: Double = 0.0,
    val currency: String = "YER",
    val isBlocked: Boolean = false,
    val defaultWalletNumber: String = "",
    val defaultWalletType: String = "الكريمي",
    val updatedAt: Long = System.currentTimeMillis()
)

@Keep
data class WalletTransactionEntity(
    val id: String = "",
    val walletId: String = "",
    val type: String = "DEPOSIT", // DEPOSIT, WITHDRAWAL, TRANSFER, PAYMENT, REFUND
    val amount: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val note: String = "",
    val performByAdmin: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
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
    val nameEn: String = "",
    val icon: String = "📍",
    val photoUrl: String = "",
    val sortOrder: Int = 0
)

@Keep
data class ChatMessageEntity(
    val id: String = "",
    val senderId: String = "guest",
    val senderName: String = "",
    val senderPhone: String = "",
    val recipientId: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val mediaType: String = "TEXT", // "TEXT", "AUDIO", "IMAGE", "VIDEO", "CALL"
    val mediaUrl: String = "",
    val audioDurationSec: Int = 0,
    val status: String = "SENT", // "SENT", "DELIVERED", "READ"
    val statusTime: Long = 0L,
    val imageUrl: String = ""
)

@Keep
data class BookingEntity(
    val id: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerArea: String = "",
    val serviceType: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val dateString: String = "",
    val timeString: String = "",
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val rejectionReason: String = "",
    val pinCode: String = "",
    
    // New Fields requested for the enhanced booking system
    val bookingNumber: String = "",      // BK-YYMMDDHHMMSS-XXXX
    val bookingPassword: String = "",    // 4-digit code (e.g. "8372")
    val clientId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val clientAddress: String = "",
    val providerPhone: String = "",
    val category: String = "",
    val subCategory: String = "",
    val serviceDetails: String = "",
    val date: String = "",
    val time: String = "",
    val advancePayment: Double = 0.0,
    val paymentStatus: String = "unpaid",
    val totalAmount: Double = 0.0,
    val progress: Int = 0,
    
    val cancellationReason: String? = null,
    val cancelledAt: Long? = null,
    val cancelledBy: String? = null,
    
    val requiresPasswordForCancellation: Boolean = true,
    val cancellationAttempts: Int = 0,
    val maxCancellationAttempts: Int = 3,
    val isLocked: Boolean = false,
    val lockedUntil: Long? = null,
    
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val completedAt: Long? = null
)

@Keep
data class NotificationEntity(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val targetType: String = "ALL", // "ALL", "REGION", "CATEGORY", "USER", "PROVIDER", "SUPERVISOR"
    val targetValue: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val expiryTimestamp: Long = 0L,
    val scheduledTime: Long = 0L,
    val customerPhone: String = "",
    val customerName: String = "",
    val notificationType: String = "NORMAL", // "IMPORTANT", "NORMAL", "REGISTRATION_APPROVED", "BOOKING_CONFIRMED", etc.
    val channel: String = "IN_APP", // "IN_APP", "FCM", "WHATSAPP", "SMS", "TELEGRAM"
    val isRead: Boolean = false
)

@Keep
data class ChatChannelEntity(
    val id: String = "",
    val channelType: String = "PROVIDER", // "PROVIDER", "STORE", "PROPERTY", "RESTAURANT", "ADMIN", "SUPERVISOR", "CATEGORY"
    val targetId: String = "",
    val targetName: String = "",
    val targetPhone: String = "",
    val targetCategory: String = "",
    val userName: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerId: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val isBlocked: Boolean = false,
    val isProvider: Boolean = false,
    val timestamp: Long = 0L,
    val unreadCountUser: Int = 0,
    val unreadCountTarget: Int = 0,
    val messages: List<ChatMessageEntity> = emptyList()
)

@Keep
data class SupervisorEntity(
    val id: String = "",
    val name: String = "",
    val role: String = "", // "ADMIN", "AUDITOR", "SUPPORT", "OPERATIONS"
    val passcode: String = "",
    val permissions: List<String> = emptyList()
)

@Keep
data class ColorPaletteEntity(
    val id: String = "",
    val name: String = "",
    val primaryHex: String = "#059669",
    val secondaryHex: String = "#115E59",
    val backgroundHex: String = "#0A0F0D",
    val surfaceHex: String = "#121D18"
)

@Keep
data class CallEntity(
    val id: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val callerName: String = "",
    val timestamp: Long = 0L
)

@Keep
data class CouponEntity(
    val id: String = "",
    val code: String = "",
    val pointsValue: Int = 0,
    val expiryTimestamp: Long = 0L,
    val status: String = "ACTIVE",
    val discountPercentage: Int = 0, // percentage discount (e.g. 15 for 15%)
    val maxUsageCount: Int = 100,
    val usedCount: Int = 0
)

@Keep
data class PaymentWalletEntity(
    val id: String = "",
    val provider: String = "other", // jeeb, alKarimi, jawaly, floosi, cashExchange, foreignCurrency, yemenMobile, mtc, sabafon, youssef, other
    val walletNumber: String = "",
    val accountName: String = "",
    val accountNameAr: String = "",
    val logoUrl: String = "",
    val bankName: String = "",
    val bankAccountNumber: String = "",
    val bankAccountName: String = "",
    val description: String = "",
    val status: String = "active", // active, inactive, suspended, deleted
    val walletType: String = "BOTH", // DEPOSIT, WITHDRAWAL, BOTH
    val currency: String = "YER", // YER, USD, SAR
    val isVisibleToUsers: Boolean = true,
    val qrCodePhoto: String = "",
    val isDefault: Boolean = false,
    val displayOrder: Int = 0,
    val minTransferAmount: Double = 0.0,
    val maxTransferAmount: Double = 1000000.0,
    val transferFee: Double = 0.0,
    val commissionRate: Double = 0.0,
    val instructions: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = ""
)

@Keep
data class PaymentEntity(
    val id: String = "",
    val userId: String = "",
    val providerId: String = "",
    val bookingId: String = "",
    val type: String = "service",
    val method: String = "cash", // cash, bankTransfer, mobileWallet, wallet
    val status: String = "PENDING", // PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED, CANCELLED, DISPUTED
    val amount: Double = 0.0,
    val advanceAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val commission: Double = 0.0,
    val providerShare: Double = 0.0,
    val currency: String = "YER",
    val walletProvider: String = "",
    val walletNumber: String = "",
    val walletAccountName: String = "",
    val transferId: String = "",
    val transferPhoto: String = "", // base64 / URL
    val bankName: String = "",
    val accountNumber: String = "",
    val accountHolderName: String = "",
    val isLinkedToBooking: Boolean = false,
    val bookingDate: Long? = null,
    val bookingServiceType: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val paidAt: Long? = null,
    val verifiedAt: Long? = null,
    val verifiedBy: String = "",
    val verificationStatus: String = "PENDING", // PENDING, VERIFIED, REJECTED, DISPUTED
    val verificationNote: String = "",
    val adminNote: String = "",
    val isDeleted: Boolean = false
)

@Keep
data class StoreEntity(
    val id: String = "",
    val sectionId: String = "stores",
    val name: String = "",
    val description: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val categoryId: String = "",
    val cityId: String = "",
    val localNeighborhood: String = "",
    val coverImage: String = "",
    val logoImage: String = "",
    val rating: Float = 5.0f,
    val numReviews: Int = 0,
    val isActive: Boolean = true,
    val isPinned: Boolean = false,
    val displayOrder: Int = 0,
    val maxImages: Int = 5,
    val workingHours: String = "9:00 AM - 10:00 PM",
    val latitude: Double = 15.3694,
    val longitude: Double = 44.1910,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val paymentEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val password: String = "",
    val pdfFileUri: String = "",
    val pdfFileBase64: String = "",
    val pdfStatus: String = "",
    val images: List<String> = emptyList(),
    val isApproved: Boolean = false,
    val isVip: Boolean = false,
    val isVerified: Boolean = false,
    val isRecommended: Boolean = false,
    val isChatDisabled: Boolean = false,
    val isNotificationsDisabled: Boolean = false,
    val productAttachmentsJson: String = "",
    val specialOffersJson: String = "",
    val isBlocked: Boolean = false,
    val blockReason: String = ""
)

@Keep
data class ProductEntity(
    val id: String = "",
    val storeId: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val currency: String = "YER",
    val imageUrl: String = "",
    val isAvailable: Boolean = true,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Keep
data class PropertyEntity(
    val id: String = "",
    val sectionId: String = "properties",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val currency: String = "YER",
    val type: String = "rent", // rent, sale
    val propertyType: String = "apartment", // apartment, house, land, shop
    val ownerId: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val cityId: String = "",
    val localNeighborhood: String = "",
    val rating: Float = 5.0f,
    val numReviews: Int = 0,
    val isActive: Boolean = true,
    val isPinned: Boolean = false,
    val displayOrder: Int = 0,
    val latitude: Double = 15.3694,
    val longitude: Double = 44.1910,
    val images: List<String> = emptyList(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val paymentEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val password: String = "",
    val pdfFileUri: String = "",
    val pdfFileBase64: String = "",
    val pdfStatus: String = "",
    val isApproved: Boolean = false,
    val maxImages: Int = 5,
    val isVip: Boolean = false,
    val isVerified: Boolean = false,
    val isRecommended: Boolean = false,
    val isChatDisabled: Boolean = false,
    val isNotificationsDisabled: Boolean = false,
    val productAttachmentsJson: String = ""
)

@Keep
data class RatingEntity(
    val id: String = "",
    val targetId: String = "", // storeId or propertyId
    val targetType: String = "STORE", // STORE, PROPERTY
    val userId: String = "",
    val userName: String = "",
    val rating: Float = 5.0f,
    val comment: String = "",
    val isApproved: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val reply: String = "",
    val replyTimestamp: Long? = null
)

@Keep
data class OrderEntity(
    val id: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val productId: String = "",
    val productName: String = "",
    val customerPhone: String = "",
    val customerName: String = "",
    val customerArea: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val totalAmount: Double = 0.0,
    val paymentId: String = "",
    val paymentStatus: String = "PENDING", // PENDING, PROCESSING, COMPLETED, FAILED
    val status: String = "PENDING", // PENDING, PROCESSING, COMPLETED, CANCELLED
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Keep
data class DynamicSection(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val isEnabled: Boolean = true,
    val type: String = "store", // "store" or "property"
    val order: Int = 0,
    val terms: String = "",
    val maxPhotos: Int = 5,
    val showPhotos: Boolean = true,
    val allowPdf: Boolean = true,
    val requiredFields: String = "الاسم,الوصف,الهاتف,الموقع"
) {
    companion object {
        fun parseDynamicSections(serialized: String): List<DynamicSection> {
            if (serialized.isEmpty()) {
                return listOf(
                    DynamicSection("stores", "المحلات والمراكز", "🏪", true, "store", 1, "شروط تسجيل المحل: يرجى إدخال بيانات صحيحة ومطابقة للواقع التجاري وصور واضحة.", 10, true, true, "الاسم,الوصف,الهاتف,الموقع"),
                    DynamicSection("restaurants", "المطاعم والكافيهات", "🍔", true, "store", 2, "شروط تسجيل المطعم: توضيح نوع الوجبات، الأسعار، وساعات العمل وصور الوجبات.", 10, true, true, "الاسم,الوصف,الهاتف,الموقع,المنيو"),
                    DynamicSection("medical", "المراكز الطبية والعيادات", "🏥", true, "store", 3, "شروط تسجيل المركز الطبي: إضافة اسم الدكتور/المركز والتخصص وأوقات الدوام.", 10, true, true, "الاسم,التخصص,الهاتف,الموقع"),
                    DynamicSection("properties", "العقارات والأراضي", "🏠", true, "property", 4, "شروط إضافة العقار: يرجى تأكيد ملكية العقار وصحة الصور والأسعار المعروضة.", 10, true, true, "العنوان,الوصف,السعر,الهاتف"),
                    DynamicSection("jobs", "إعلانات الوظائف", "💼", true, "store", 5, "شروط إعلان الوظيفة: توضيح المسمى الوظيفي والراتب والشروط المطلوبة ورقم التواصل.", 5, true, true, "المسمى,الجهة,الهاتف,الراتب"),
                    DynamicSection("services", "المهن والخدمات", "🛠️", true, "store", 6, "شروط تسجيل الفني: تحديد المهنة/التخصص والأسعار التقديرية ونطاق العمل.", 10, true, true, "الاسم,المهنة,الهاتف,المنطقة")
                )
            }
            return try {
                serialized.split(";;").filter { it.isNotEmpty() }.map { sectionStr ->
                    val parts = sectionStr.split("||")
                    DynamicSection(
                        id = parts.getOrElse(0) { "" },
                        name = parts.getOrElse(1) { "" },
                        icon = parts.getOrElse(2) { "" },
                        isEnabled = parts.getOrElse(3) { "true" }.toBoolean(),
                        type = parts.getOrElse(4) { "store" },
                        order = parts.getOrElse(5) { "0" }.toIntOrNull() ?: 0,
                        terms = parts.getOrElse(6) { "" },
                        maxPhotos = parts.getOrElse(7) { "5" }.toIntOrNull() ?: 5,
                        showPhotos = parts.getOrElse(8) { "true" }.toBoolean(),
                        allowPdf = parts.getOrElse(9) { "true" }.toBoolean(),
                        requiredFields = parts.getOrElse(10) { "الاسم,الهاتف,الوصف" }
                    )
                }.sortedBy { it.order }
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun serializeDynamicSections(list: List<DynamicSection>): String {
            return list.joinToString(";;") { sec ->
                listOf(
                    sec.id,
                    sec.name,
                    sec.icon,
                    sec.isEnabled.toString(),
                    sec.type,
                    sec.order.toString(),
                    sec.terms,
                    sec.maxPhotos.toString(),
                    sec.showPhotos.toString(),
                    sec.allowPdf.toString(),
                    sec.requiredFields
                ).joinToString("||")
            }
        }
    }
}

@Keep
data class JobEntity(
    val id: String = "",
    val sectionId: String = "jobs",
    val title: String = "", // المسمى الوظيفي
    val companyName: String = "", // اسم الشركة أو الجهة
    val managerName: String = "", // اسم المسؤول
    val phone: String = "", // رقم الهاتف والواتساب
    val cityId: String = "", // المحافظة / المدينة
    val address: String = "", // الحي والشارع
    val jobType: String = "دوام كامل", // دوام كامل، دوام جزئي، عن بعد، بالساعة
    val salary: String = "", // الراتب المتوقع
    val description: String = "", // التفاصيل
    val requirements: String = "", // الشروط والمؤهلات
    val isApproved: Boolean = false,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isVip: Boolean = false,
    val isChatDisabled: Boolean = false
)

@Keep
data class JobApplicationEntity(
    val id: String = "",
    val jobId: String = "",
    val jobTitle: String = "",
    val companyName: String = "",
    val applicantName: String = "",
    val applicantPhone: String = "",
    val applicantQuals: String = "",
    val cvBase64: String = "",
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis()
)

@Keep
data class SpecialOfferEntity(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val discountPercent: Int = 0,
    val originalPrice: Double = 0.0,
    val offerPrice: Double = 0.0,
    val expiryDate: String = "",
    val isEnabled: Boolean = true
) {
    companion object {
        fun parseList(jsonStr: String): List<SpecialOfferEntity> {
            if (jsonStr.isBlank()) return emptyList()
            return try {
                jsonStr.split(";;;").filter { it.isNotBlank() }.map { chunk ->
                    val p = chunk.split("|||")
                    SpecialOfferEntity(
                        id = p.getOrElse(0) { "" },
                        title = p.getOrElse(1) { "" },
                        description = p.getOrElse(2) { "" },
                        discountPercent = p.getOrElse(3) { "0" }.toIntOrNull() ?: 0,
                        originalPrice = p.getOrElse(4) { "0" }.toDoubleOrNull() ?: 0.0,
                        offerPrice = p.getOrElse(5) { "0" }.toDoubleOrNull() ?: 0.0,
                        expiryDate = p.getOrElse(6) { "" },
                        isEnabled = p.getOrElse(7) { "true" }.toBoolean()
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun serializeList(list: List<SpecialOfferEntity>): String {
            return list.joinToString(";;;") { offer ->
                listOf(
                    offer.id,
                    offer.title,
                    offer.description,
                    offer.discountPercent.toString(),
                    offer.originalPrice.toString(),
                    offer.offerPrice.toString(),
                    offer.expiryDate,
                    offer.isEnabled.toString()
                ).joinToString("|||")
            }
        }
    }
}

@Keep
data class CustomProfileTabEntity(
    val id: String = "",
    val title: String = "",
    val icon: String = "📑",
    val targetType: String = "ALL", // "ALL", "PROVIDERS", "STORES", "PROPERTIES"
    val contentHtmlOrText: String = "",
    val isEnabled: Boolean = true,
    val displayOrder: Int = 0
)

@Keep
data class AppSettings(
    val disableChatAll: Boolean = false,
    val disableChatUsers: Boolean = false,
    val disableChatProviders: Boolean = false,
    val allowChatUserToProvider: Boolean = true,
    val chatDisabledAnnouncement: String = "",
    val showUserIdInsteadOfNameInChat: Boolean = false,
    val disableVoiceCalls: Boolean = false,
    val voiceCallsDisabledAnnouncement: String = "",
    val appLanguage: String = "ar"
)



