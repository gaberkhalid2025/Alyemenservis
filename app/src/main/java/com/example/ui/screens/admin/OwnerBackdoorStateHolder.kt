@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.admin

import android.content.Context
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.example.data.AdminSettingsEntity
import com.example.ui.*
import com.example.utils.AdminSession
import com.example.utils.SecureStorage
import com.example.utils.compressAndResizeImageUri

@Stable
internal class OwnerBackdoorState(
    val settingsState: AdminSettingsEntity,
    val context: Context,
    val secureStorage: SecureStorage
) {
    var appName by mutableStateOf(settingsState.appName)
    var welcomeMessage by mutableStateOf(settingsState.welcomeMessage)
    var footerMessage by mutableStateOf(settingsState.footerMessage)
    var footerBgColorHex by mutableStateOf(settingsState.footerBgColorHex)
    var footerItemsOrder by mutableStateOf(settingsState.footerItemsOrder)
    var showInfoIcon by mutableStateOf(settingsState.showInfoIcon)
    var showBookingsIcon by mutableStateOf(settingsState.showBookingsIcon)
    var showLangIcon by mutableStateOf(settingsState.showLangIcon)
    var showAdminIcon by mutableStateOf(settingsState.showAdminIcon)
    var showFooterText by mutableStateOf(settingsState.showFooterText)
    var infoIconType by mutableStateOf(settingsState.infoIconType)
    var adminIconType by mutableStateOf(settingsState.adminIconType)
    var langIconType by mutableStateOf(settingsState.langIconType)
    var bookingsIconType by mutableStateOf(settingsState.bookingsIconType)
    var appVersion by mutableStateOf(settingsState.appVersion)
    var supportPhone by mutableStateOf(settingsState.supportPhone)
    var supportEmail by mutableStateOf(settingsState.supportEmail)
    var supportWhatsapp by mutableStateOf(settingsState.supportWhatsapp)
    var appDownloadUrl by mutableStateOf(settingsState.appDownloadUrl)
    var activeThemeId by mutableStateOf(settingsState.activeThemeId)
    var isMaintenanceActive by mutableStateOf(settingsState.isMaintenanceActive)
    var hidePromoFooter by mutableStateOf(settingsState.hidePromoFooter)
    var assistantHidden by mutableStateOf(settingsState.assistantHidden)
    var assistantSize by mutableStateOf(56f)
    var chatHidden by mutableStateOf(settingsState.chatHidden)
    var chatSize by mutableStateOf(if (settingsState.chatSize > 0) settingsState.chatSize.toFloat() else 56f)
    var maxSearchRadiusKm by mutableStateOf(settingsState.maxSearchRadiusKm.toFloat())
    var isSpeechSearchEnabled by mutableStateOf(settingsState.isSpeechSearchEnabled)
    var isDataSaverEnabled by mutableStateOf(false)
    var appImageQuality by mutableStateOf(90f)
    var bypassVisitorRegistration by mutableStateOf(settingsState.bypassVisitorRegistration)
    var isUserPasswordRequired by mutableStateOf(settingsState.isUserPasswordRequired)
    var disableChatFirewall by mutableStateOf(settingsState.disableChatFirewall)
    var disableBookingFirewall by mutableStateOf(settingsState.disableBookingFirewall)
    var isMapFeatureEnabled by mutableStateOf(settingsState.isMapFeatureEnabled)
    var mapProviderState by mutableStateOf(settingsState.mapProvider)
    var isStoresEnabled by mutableStateOf(settingsState.isStoresEnabled)
    var isPropertiesEnabled by mutableStateOf(settingsState.isPropertiesEnabled)
    var enableProvidersRegistration by mutableStateOf(settingsState.enableProvidersRegistration)
    var enableStoresRegistration by mutableStateOf(settingsState.enableStoresRegistration)
    var enableRestaurantsRegistration by mutableStateOf(settingsState.enableRestaurantsRegistration)
    var enablePropertiesRegistration by mutableStateOf(settingsState.enablePropertiesRegistration)
    var enableMedicalRegistration by mutableStateOf(settingsState.enableMedicalRegistration)
    var enableJobsRegistration by mutableStateOf(settingsState.enableJobsRegistration)
    var showRefreshIcon by mutableStateOf(settingsState.showRefreshIcon)
    var showSettingsIcon by mutableStateOf(settingsState.showSettingsIcon)
    var headerIconsOrder by mutableStateOf(settingsState.headerIconsOrder)
    var categoriesLayoutTypeState by mutableStateOf(settingsState.categoriesLayoutType)
    var websiteUrl by mutableStateOf(settingsState.websiteUrl)
    var telegramUrl by mutableStateOf(settingsState.telegramUrl)
    var facebookUrl by mutableStateOf(settingsState.facebookUrl)
    var twitterUrl by mutableStateOf(settingsState.twitterUrl)
    var instagramUrl by mutableStateOf(settingsState.instagramUrl)
    var youtubeUrl by mutableStateOf(settingsState.youtubeUrl)
    var aboutLayoutOrder by mutableStateOf(settingsState.aboutLayoutOrder)
    var hideTwitter by mutableStateOf(settingsState.hideTwitter)
    var hideInstagram by mutableStateOf(settingsState.hideInstagram)
    var hideYoutube by mutableStateOf(settingsState.hideYoutube)
    var hideFacebook by mutableStateOf(settingsState.hideFacebook)
    var hideTelegram by mutableStateOf(settingsState.hideTelegram)
    var hideWebsite by mutableStateOf(settingsState.hideWebsite)

    // Booking form full control states
    var bookingTermsInput by mutableStateOf(settingsState.bookingTerms)
    var bookingLabelNameInput by mutableStateOf(settingsState.bookingLabelName)
    var bookingLabelPhoneInput by mutableStateOf(settingsState.bookingLabelPhone)
    var bookingLabelAreaInput by mutableStateOf(settingsState.bookingLabelArea)
    var bookingLabelServiceInput by mutableStateOf(settingsState.bookingLabelService)

    // About App customization states
    var aboutCoverType by mutableStateOf(settingsState.aboutCoverType)
    var aboutCoverContent by mutableStateOf(settingsState.aboutCoverContent)
    var aboutCoverBase64 by mutableStateOf(settingsState.aboutCoverBase64)
    var aboutCustomInfo by mutableStateOf(settingsState.aboutCustomInfo)
    var encryptionTypeState by mutableStateOf(settingsState.encryptionType)
    var splashWelcomeMessageState by mutableStateOf(settingsState.splashWelcomeMessage)

    // Banner control states
    var bannerEnabled by mutableStateOf(settingsState.bannerEnabled)
    var bannerType by mutableStateOf(settingsState.bannerType)
    var bannerContent by mutableStateOf(settingsState.bannerContent)
    var bannerBase64 by mutableStateOf(settingsState.bannerBase64)
    var bannerLocation by mutableStateOf(settingsState.bannerLocation)
    var bannerDurationSeconds by mutableStateOf(settingsState.bannerDurationSeconds.toString())
    var bannerDisplayStyle by mutableStateOf(settingsState.bannerDisplayStyle)
    var selectedSimulatedLogo by mutableStateOf("شعار احترافي 🔧")

    // Admin auth & diagnostics states
    var showLogsDialog by mutableStateOf(false)
    var showRegistrationTestScreen by mutableStateOf(false)
    var showAssistantTestScreen by mutableStateOf(false)
    var rememberLoginInput by mutableStateOf(secureStorage.getAdminSession()?.role == "OWNER")
    var adminUsernameInput by mutableStateOf(settingsState.adminUsername)

    // Wipe confirmation dialog states
    var showWipeConfirmDialog by mutableStateOf(false)
    var wipePasswordInput by mutableStateOf("")
    var wipeDoubleConfirmed by mutableStateOf(false)
    var wipeErrorMsg by mutableStateOf("")

    lateinit var galleryLauncherForBanner: ActivityResultLauncher<String>
    lateinit var galleryLauncherForCover: ActivityResultLauncher<String>

    fun saveAllSettings(viewModel: MainViewModel) {
        val currentSettings = settingsState.copy(
            appName = appName,
            welcomeMessage = welcomeMessage,
            footerMessage = footerMessage,
            footerBgColorHex = footerBgColorHex,
            footerItemsOrder = footerItemsOrder,
            appVersion = appVersion,
            activeThemeId = activeThemeId,
            isMaintenanceActive = isMaintenanceActive,
            hidePromoFooter = hidePromoFooter,
            assistantHidden = assistantHidden,
            assistantSize = assistantSize.toInt(),
            chatHidden = chatHidden,
            chatSize = chatSize.toInt(),
            maxSearchRadiusKm = maxSearchRadiusKm.toInt(),
            isSpeechSearchEnabled = isSpeechSearchEnabled,
            bypassVisitorRegistration = bypassVisitorRegistration,
            isUserPasswordRequired = isUserPasswordRequired,
            disableChatFirewall = disableChatFirewall,
            disableBookingFirewall = disableBookingFirewall,
            isMapFeatureEnabled = isMapFeatureEnabled,
            mapProvider = mapProviderState,
            enableProvidersRegistration = enableProvidersRegistration,
            enableStoresRegistration = enableStoresRegistration,
            enableRestaurantsRegistration = enableRestaurantsRegistration,
            enablePropertiesRegistration = enablePropertiesRegistration,
            enableMedicalRegistration = enableMedicalRegistration,
            enableJobsRegistration = enableJobsRegistration,
            bookingTerms = bookingTermsInput,
            bookingLabelName = bookingLabelNameInput,
            bookingLabelPhone = bookingLabelPhoneInput,
            bookingLabelArea = bookingLabelAreaInput,
            bookingLabelService = bookingLabelServiceInput,
            adminUsername = adminUsernameInput,
            aboutCoverType = aboutCoverType,
            aboutCoverContent = aboutCoverContent,
            aboutCoverBase64 = aboutCoverBase64,
            aboutCustomInfo = aboutCustomInfo,
            encryptionType = encryptionTypeState,
            splashWelcomeMessage = splashWelcomeMessageState,
            bannerEnabled = bannerEnabled,
            bannerType = bannerType,
            bannerContent = bannerContent,
            bannerBase64 = bannerBase64,
            bannerLocation = bannerLocation,
            bannerDurationSeconds = bannerDurationSeconds.toIntOrNull() ?: 0,
            bannerDisplayStyle = bannerDisplayStyle,
            appDownloadUrl = appDownloadUrl,
            showRefreshIcon = showRefreshIcon,
            showSettingsIcon = showSettingsIcon,
            headerIconsOrder = headerIconsOrder,
            categoriesLayoutType = categoriesLayoutTypeState,
            isStoresEnabled = isStoresEnabled,
            isPropertiesEnabled = isPropertiesEnabled,
            websiteUrl = websiteUrl,
            telegramUrl = telegramUrl,
            facebookUrl = facebookUrl,
            twitterUrl = twitterUrl,
            instagramUrl = instagramUrl,
            youtubeUrl = youtubeUrl,
            aboutLayoutOrder = aboutLayoutOrder,
            showInfoIcon = showInfoIcon,
            showBookingsIcon = showBookingsIcon,
            showLangIcon = showLangIcon,
            showAdminIcon = showAdminIcon,
            showFooterText = showFooterText,
            infoIconType = infoIconType,
            adminIconType = adminIconType,
            langIconType = langIconType,
            bookingsIconType = bookingsIconType,
            hideTwitter = hideTwitter,
            hideInstagram = hideInstagram,
            hideYoutube = hideYoutube,
            hideFacebook = hideFacebook,
            hideTelegram = hideTelegram,
            hideWebsite = hideWebsite
        )
        viewModel.saveCustomSettingsState(currentSettings)

        if (rememberLoginInput) {
            val currentAdminEmail = currentSettings.ownerEmail.ifBlank { "owner" }
            secureStorage.saveAdminSession(
                AdminSession(
                    uid = "owner_backdoor",
                    email = currentAdminEmail,
                    loginTime = System.currentTimeMillis(),
                    refreshToken = "",
                    role = "OWNER"
                )
            )
        } else {
            secureStorage.clearAdminSession()
        }
        Toast.makeText(context, "💾 تم حفظ كافة التخصيصات والتحققات بنجاح!", Toast.LENGTH_SHORT).show()
    }
}

@Composable
internal fun rememberOwnerBackdoorState(settingsState: AdminSettingsEntity): OwnerBackdoorState {
    val context = androidx.compose.ui.platform.LocalContext.current
    val secureStorage = remember { SecureStorage(context) }
    val state = remember { OwnerBackdoorState(settingsState, context, secureStorage) }

    state.galleryLauncherForBanner = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val mimeType = context.contentResolver.getType(it)
                if (mimeType != null && mimeType.startsWith("video/")) {
                    val bytes = context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() }
                    if (bytes != null) {
                        if (bytes.size > 2_000_000) {
                            Toast.makeText(context, "⚠️ حجم الفيديو كبير جداً (الحد الأقصى 2 ميجابايت)", Toast.LENGTH_LONG).show()
                        } else {
                            state.bannerBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                            state.bannerType = "VIDEO"
                            Toast.makeText(context, "📹 تم تحميل الفيديو القصير للبنر بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val base64Str = compressAndResizeImageUri(context, it, 800, 70)
                    if (base64Str.isNotEmpty()) {
                        state.bannerBase64 = base64Str
                        state.bannerType = "IMAGE"
                        Toast.makeText(context, "📸 تم تحميل صورة البنر الإعلاني من المعرض بنجاح!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    state.galleryLauncherForCover = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val base64Str = compressAndResizeImageUri(context, it, 800, 70)
                if (base64Str.isNotEmpty()) {
                    state.aboutCoverBase64 = base64Str
                    state.aboutCoverType = "IMAGE"
                    Toast.makeText(context, "📸 تم تحميل صورة الغلاف من المعرض بنجاح!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    return state
}
