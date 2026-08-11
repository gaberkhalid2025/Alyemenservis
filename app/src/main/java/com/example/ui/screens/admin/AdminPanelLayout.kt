@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.admin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState

import com.example.ui.*
import com.example.ui.utils.*


import android.content.Intent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import okhttp3.MediaType.Companion.toMediaType

import com.example.*

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.*
import com.example.utils.*
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.screens.home.*
import com.example.ui.screens.map.*
import com.example.ui.screens.bookings.*
import com.example.ui.screens.admin.*
import com.example.ui.screens.assistant.*
import com.example.ui.screens.register.*
import com.example.ui.screens.status.*
import com.example.ui.screens.about.*
import com.example.viewmodels.*
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun AdminPanelLayout(viewModel: MainViewModel, themeColors: VisualThemePalette) {
    val pendingProviders by viewModel.pendingProviders.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val activatedProviders by viewModel.providers.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val chatChannels by viewModel.chatChannels.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val bannersList by viewModel.banners.collectAsState()
    val supervisorsList by viewModel.supervisors.collectAsState()
    val colorPalettesList by viewModel.colorPalettes.collectAsState()
    val citiesList by viewModel.cities.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val jobs by viewModel.jobs.collectAsState()
    val jobApplications by viewModel.jobApplications.collectAsState()

    val inputPasscodeState = remember { mutableStateOf<String>("") }
    var inputPasscode by inputPasscodeState
    val isAuthorizedState = remember(adminRole) { mutableStateOf<Boolean>(adminRole != "GUEST") }
    var isAuthorized by isAuthorizedState
    val activeSubTabState = remember(adminRole) { mutableStateOf<String>(if (adminRole == "OWNER") "BACKDOOR" else "REG_REQ") }
    var activeSubTab by activeSubTabState
    val adminReqSubTabState = remember { mutableStateOf<String>("SERVICES") } // SERVICES, PROPERTIES, STORES, MEDICAL, RESTAURANTS, JOBS
    var adminReqSubTab by adminReqSubTabState
    val adminBookingSubTabState = remember { mutableStateOf<String>("SERVICES") }
    var adminBookingSubTab by adminBookingSubTabState
    val adminChatSubTabState = remember { mutableStateOf<String>("SERVICES") }
    var adminChatSubTab by adminChatSubTabState
    val adminAddSubTabState = remember { mutableStateOf<String>("SERVICES") } // SERVICES, PROPERTIES, STORES, MEDICAL, RESTAURANTS, JOBS
    var adminAddSubTab by adminAddSubTabState
    val adminReviewSubTabState = remember { mutableStateOf<String>("SERVICES") }
    var adminReviewSubTab by adminReviewSubTabState
    val adminNotifSubTabState = remember { mutableStateOf<String>("SERVICES") }
    var adminNotifSubTab by adminNotifSubTabState
    val adminVipSubTabState = remember { mutableStateOf<String>("SERVICES") }
    var adminVipSubTab by adminVipSubTabState
    val adminBannerSubTabState = remember { mutableStateOf<String>("SERVICES") }
    var adminBannerSubTab by adminBannerSubTabState
    val adminPasswordSubTabState = remember { mutableStateOf<String>("SERVICES") }
    var adminPasswordSubTab by adminPasswordSubTabState
    val showDeleteCategoryConfirmIdState = remember { mutableStateOf<String?>(null) }
    var showDeleteCategoryConfirmId by showDeleteCategoryConfirmIdState
    val showEditCategoryObjState = remember { mutableStateOf<CategoryEntity?>(null) }
    var showEditCategoryObj by showEditCategoryObjState
    val showEditCityObjState = remember { mutableStateOf<com.example.data.CityEntity?>(null) }
    var showEditCityObj by showEditCityObjState
    val rejectingProviderRequestState = remember { mutableStateOf<com.example.data.PendingProviderEntity?>(null) }
    var rejectingProviderRequest by rejectingProviderRequestState
    val providerRejectionReasonTextState = remember { mutableStateOf<String>("") }
    var providerRejectionReasonText by providerRejectionReasonTextState
    val editCatNameState = remember { mutableStateOf<String>("") }
    var editCatName by editCatNameState
    val editCatIconState = remember { mutableStateOf<String>("") }
    var editCatIcon by editCatIconState
    val newCatNameState = remember { mutableStateOf<String>("") }
    var newCatName by newCatNameState
    val newCatIconState = remember { mutableStateOf<String>("") }
    var newCatIcon by newCatIconState
    val showDeleteBookingConfirmIdState = remember { mutableStateOf<String?>(null) }
    var showDeleteBookingConfirmId by showDeleteBookingConfirmIdState
    val showRejectionReasonDialogIdState = remember { mutableStateOf<String?>(null) }
    var showRejectionReasonDialogId by showRejectionReasonDialogIdState
    val bookingRejectionReasonInputState = remember { mutableStateOf<String>("") }
    var bookingRejectionReasonInput by bookingRejectionReasonInputState
    val editingBookingObjState = remember { mutableStateOf<BookingEntity?>(null) }
    var editingBookingObj by editingBookingObjState
    val redirectingBookingObjState = remember { mutableStateOf<BookingEntity?>(null) }
    var redirectingBookingObj by redirectingBookingObjState
    val editingSupervisorObjState = remember { mutableStateOf<SupervisorEntity?>(null) }
    var editingSupervisorObj by editingSupervisorObjState
    val showDeleteNotifConfirmIdState = remember { mutableStateOf<String?>(null) }
    var showDeleteNotifConfirmId by showDeleteNotifConfirmIdState
    val selectedRecoveryNotifState = remember { mutableStateOf<NotificationEntity?>(null) }
    var selectedRecoveryNotif by selectedRecoveryNotifState
    val showActiveChatChannelObjState = remember { mutableStateOf<ChatChannelEntity?>(null) }
    var showActiveChatChannelObj by showActiveChatChannelObjState
    val adminChatReplyInputState = remember { mutableStateOf<String>("") }
    var adminChatReplyInput by adminChatReplyInputState
    val showDeleteChatConfirmIdState = remember { mutableStateOf<String?>(null) }
    var showDeleteChatConfirmId by showDeleteChatConfirmIdState
    val backupJsonStringStateState = remember { mutableStateOf<String>("") }
    var backupJsonStringState by backupJsonStringStateState
    val restoreJsonInputStateState = remember { mutableStateOf<String>("") }
    var restoreJsonInputState by restoreJsonInputStateState
    val notifTitleInputState = remember { mutableStateOf<String>("") }
    var notifTitleInput by notifTitleInputState
    val notifMsgInputState = remember { mutableStateOf<String>("") }
    var notifMsgInput by notifMsgInputState
    val notifTargetTypeState = remember { mutableStateOf<String>("ALL") } // ALL, USER, PROVIDER, SUPERVISOR, AREA
    var notifTargetType by notifTargetTypeState
    val notifTargetValueState = remember { mutableStateOf<String>("") }
    var notifTargetValue by notifTargetValueState
    val notifDelayHoursState = remember { mutableStateOf<String>("") }
    var notifDelayHours by notifDelayHoursState
    val notifValidityHoursState = remember { mutableStateOf<String>("") }
    var notifValidityHours by notifValidityHoursState
    val editPrimaryHexState = remember { mutableStateOf<String>(settingsState.customPrimaryHex) }
    var editPrimaryHex by editPrimaryHexState
    val editSecondaryHexState = remember { mutableStateOf<String>(settingsState.customSecondaryHex) }
    var editSecondaryHex by editSecondaryHexState
    val editCardBgHexState = remember { mutableStateOf<String>(settingsState.cardBackgroundHex) }
    var editCardBgHex by editCardBgHexState
    val editProviderNameHexState = remember { mutableStateOf<String>(settingsState.providerNameColorHex) }
    var editProviderNameHex by editProviderNameHexState
    val editLocationHexState = remember { mutableStateOf<String>(settingsState.locationColorHex) }
    var editLocationHex by editLocationHexState
    val editRatingHexState = remember { mutableStateOf<String>(settingsState.ratingColorHex) }
    var editRatingHex by editRatingHexState
    val editVipBadgeHexState = remember { mutableStateOf<String>(settingsState.vipBadgeColorHex) }
    var editVipBadgeHex by editVipBadgeHexState
    val editVerifiedHexState = remember { mutableStateOf<String>(settingsState.verifiedBadgeColorHex) }
    var editVerifiedHex by editVerifiedHexState
    val editRecommendedHexState = remember { mutableStateOf<String>(settingsState.recommendedBadgeColorHex) }
    var editRecommendedHex by editRecommendedHexState
    val editFontSelectedState = remember { mutableStateOf<String>(settingsState.activeFontFamily) }
    var editFontSelected by editFontSelectedState
    val editChatIconSizeState = remember(settingsState.chatSize) { mutableStateOf<Float>(settingsState.chatSize.toFloat()) }
    var editChatIconSize by editChatIconSizeState
    val editChatIconXState = remember(settingsState.chatXOffset) { mutableStateOf<Float>(settingsState.chatXOffset.toFloat()) }
    var editChatIconX by editChatIconXState
    val editChatIconYState = remember(settingsState.chatYOffset) { mutableStateOf<Float>(settingsState.chatYOffset.toFloat()) }
    var editChatIconY by editChatIconYState
    val editAssistantIconSizeState = remember(settingsState.assistantSize) { mutableStateOf<Float>(settingsState.assistantSize.toFloat()) }
    var editAssistantIconSize by editAssistantIconSizeState
    val editAssistantIconXState = remember(settingsState.assistantXOffset) { mutableStateOf<Float>(settingsState.assistantXOffset.toFloat()) }
    var editAssistantIconX by editAssistantIconXState
    val editAssistantIconYState = remember(settingsState.assistantYOffset) { mutableStateOf<Float>(settingsState.assistantYOffset.toFloat()) }
    var editAssistantIconY by editAssistantIconYState
    val requirementItemInputState = remember { mutableStateOf<String>("") }
    var requirementItemInput by requirementItemInputState
    val isNewRequirementMandatoryState = remember { mutableStateOf<Boolean>(true) }
    var isNewRequirementMandatory by isNewRequirementMandatoryState
    val requirementsListStateState = remember { mutableStateOf<List<String>>(settingsState.registrationRequirements.split(",").filter { it.isNotBlank() }) }
    var requirementsListState by requirementsListStateState
    val editCoverHeightState = remember(settingsState.coverHeight) { mutableStateOf<Float>(settingsState.coverHeight.toFloat()) }
    var editCoverHeight by editCoverHeightState
    val editAvatarSizeState = remember(settingsState.avatarSize) { mutableStateOf<Float>(settingsState.avatarSize.toFloat()) }
    var editAvatarSize by editAvatarSizeState
    val editElementSpacingState = remember(settingsState.elementSpacing) { mutableStateOf<Float>(settingsState.elementSpacing.toFloat()) }
    var editElementSpacing by editElementSpacingState
    val editCardPaddingState = remember(settingsState.cardPadding) { mutableStateOf<Float>(settingsState.cardPadding.toFloat()) }
    var editCardPadding by editCardPaddingState
    val editShowVipBadgeState = remember(settingsState.showVipBadge) { mutableStateOf<Boolean>(settingsState.showVipBadge) }
    var editShowVipBadge by editShowVipBadgeState
    val editShowVerifiedBadgeState = remember(settingsState.showVerifiedBadge) { mutableStateOf<Boolean>(settingsState.showVerifiedBadge) }
    var editShowVerifiedBadge by editShowVerifiedBadgeState
    val editShowRecommendedBadgeState = remember(settingsState.showRecommendedBadge) { mutableStateOf<Boolean>(settingsState.showRecommendedBadge) }
    var editShowRecommendedBadge by editShowRecommendedBadgeState
    val editShowCallButtonState = remember(settingsState.showCallButton) { mutableStateOf<Boolean>(settingsState.showCallButton) }
    var editShowCallButton by editShowCallButtonState
    val editShowWhatsappButtonState = remember(settingsState.showWhatsappButton) { mutableStateOf<Boolean>(settingsState.showWhatsappButton) }
    var editShowWhatsappButton by editShowWhatsappButtonState
    val editShowDetailsButtonState = remember(settingsState.showDetailsButton) { mutableStateOf<Boolean>(settingsState.showDetailsButton) }
    var editShowDetailsButton by editShowDetailsButtonState
    val editShowBookButtonState = remember(settingsState.showBookButton) { mutableStateOf<Boolean>(settingsState.showBookButton) }
    var editShowBookButton by editShowBookButtonState
    val categoryManagementModeState = remember { mutableStateOf<String>("MAINTENANCE") } // MAINTENANCE or PLATFORM_SECTIONS
    var categoryManagementMode by categoryManagementModeState
    val editCallButtonColorHexState = remember(settingsState.callButtonColorHex) { mutableStateOf<String>(settingsState.callButtonColorHex) }
    var editCallButtonColorHex by editCallButtonColorHexState
    val editWhatsappButtonColorHexState = remember(settingsState.whatsappButtonColorHex) { mutableStateOf<String>(settingsState.whatsappButtonColorHex) }
    var editWhatsappButtonColorHex by editWhatsappButtonColorHexState
    val editDetailsButtonColorHexState = remember(settingsState.detailsButtonColorHex) { mutableStateOf<String>(settingsState.detailsButtonColorHex) }
    var editDetailsButtonColorHex by editDetailsButtonColorHexState
    val editBookButtonColorHexState = remember(settingsState.bookButtonColorHex) { mutableStateOf<String>(settingsState.bookButtonColorHex) }
    var editBookButtonColorHex by editBookButtonColorHexState
    val editShowLoyaltyBannerState = remember(settingsState.showLoyaltyBanner) { mutableStateOf<Boolean>(settingsState.showLoyaltyBanner) }
    var editShowLoyaltyBanner by editShowLoyaltyBannerState
    val editMaxWorkPhotosState = remember(settingsState.maxWorkPhotos) { mutableStateOf<Float>(settingsState.maxWorkPhotos.toFloat()) }
    var editMaxWorkPhotos by editMaxWorkPhotosState
    val showWipeConfirmDialogState = remember { mutableStateOf<Boolean>(false) }
    var showWipeConfirmDialog by showWipeConfirmDialogState
    val wipeInputPasswordState = remember { mutableStateOf<String>("") }
    var wipeInputPassword by wipeInputPasswordState
    val wipeProvidersCheckedState = remember { mutableStateOf<Boolean>(true) }
    var wipeProvidersChecked by wipeProvidersCheckedState
    val wipeBookingsCheckedState = remember { mutableStateOf<Boolean>(true) }
    var wipeBookingsChecked by wipeBookingsCheckedState
    val wipeChatsCheckedState = remember { mutableStateOf<Boolean>(true) }
    var wipeChatsChecked by wipeChatsCheckedState
    val wipeNotifsCheckedState = remember { mutableStateOf<Boolean>(true) }
    var wipeNotifsChecked by wipeNotifsCheckedState
    val wipeReportsCheckedState = remember { mutableStateOf<Boolean>(true) }
    var wipeReportsChecked by wipeReportsCheckedState
    val wipeCategoriesCheckedState = remember { mutableStateOf<Boolean>(false) }
    var wipeCategoriesChecked by wipeCategoriesCheckedState
    val wipePendingCheckedState = remember { mutableStateOf<Boolean>(true) }
    var wipePendingChecked by wipePendingCheckedState
    val wipeBannersCheckedState = remember { mutableStateOf<Boolean>(true) }
    var wipeBannersChecked by wipeBannersCheckedState
    val wipeSupervisorsCheckedState = remember { mutableStateOf<Boolean>(false) }
    var wipeSupervisorsChecked by wipeSupervisorsCheckedState
    val wipeCitiesCheckedState = remember { mutableStateOf<Boolean>(false) }
    var wipeCitiesChecked by wipeCitiesCheckedState
    val wipeThemesCheckedState = remember { mutableStateOf<Boolean>(false) }
    var wipeThemesChecked by wipeThemesCheckedState
    val manualNameState = remember { mutableStateOf<String>("") }
    var manualName by manualNameState
    val manualPhoneState = remember { mutableStateOf<String>("") }
    var manualPhone by manualPhoneState
    val manualCategoryIdState = remember { mutableStateOf<String>("") }
    var manualCategoryId by manualCategoryIdState
    val manualStreetState = remember { mutableStateOf<String>("") }
    var manualStreet by manualStreetState
    val manualCityIdState = remember { mutableStateOf<String>("") }
    var manualCityId by manualCityIdState
    val manualPhotoUrlState = remember { mutableStateOf<String>("") }
    var manualPhotoUrl by manualPhotoUrlState
    val manualIdCardUrlState = remember { mutableStateOf<String>("") }
    var manualIdCardUrl by manualIdCardUrlState
    val manualForensicUrlState = remember { mutableStateOf<String>("") }
    var manualForensicUrl by manualForensicUrlState
    val manualPriceValueState = remember { mutableStateOf<String>("1500") }
    var manualPriceValue by manualPriceValueState
    val manualIsVipGoldenState = remember { mutableStateOf<Boolean>(false) }
    var manualIsVipGolden by manualIsVipGoldenState
    val newCityArNameState = remember { mutableStateOf<String>("") }
    var newCityArName by newCityArNameState
    val newCityEnNameState = remember { mutableStateOf<String>("") }
    var newCityEnName by newCityEnNameState
    val newCityIconState = remember { mutableStateOf<String>("📍") }
    var newCityIcon by newCityIconState
    val complaintsSearchQueryState = remember { mutableStateOf<String>("") }
    var complaintsSearchQuery by complaintsSearchQueryState
    val activeProvidersSearchQueryState = remember { mutableStateOf<String>("") }
    var activeProvidersSearchQuery by activeProvidersSearchQueryState
    val activeJobsSearchQueryState = remember { mutableStateOf<String>("") }
    var activeJobsSearchQuery by activeJobsSearchQueryState
    val storesSearchQueryState = remember { mutableStateOf<String>("") }
    var storesSearchQuery by storesSearchQueryState
    val restaurantsSearchQueryState = remember { mutableStateOf<String>("") }
    var restaurantsSearchQuery by restaurantsSearchQueryState
    val medicalSearchQueryState = remember { mutableStateOf<String>("") }
    var medicalSearchQuery by medicalSearchQueryState
    val propertiesSearchQueryState = remember { mutableStateOf<String>("") }
    var propertiesSearchQuery by propertiesSearchQueryState
    val applicantsSearchQueryState = remember { mutableStateOf<String>("") }
    var applicantsSearchQuery by applicantsSearchQueryState
    val showEditProviderMetadataObjState = remember { mutableStateOf<ProviderEntity?>(null) }
    var showEditProviderMetadataObj by showEditProviderMetadataObjState
    val editProviderPhoneState = remember { mutableStateOf<String>("") }
    var editProviderPhone by editProviderPhoneState
    val editProviderCategoryIdState = remember { mutableStateOf<String>("") }
    var editProviderCategoryId by editProviderCategoryIdState
    val supervisorInputNameState = remember { mutableStateOf<String>("") }
    var supervisorInputName by supervisorInputNameState
    val supervisorInputRoleState = remember { mutableStateOf<String>("SUPPORT") }
    var supervisorInputRole by supervisorInputRoleState
    val supervisorInputPasscodeState = remember { mutableStateOf<String>("") }
    var supervisorInputPasscode by supervisorInputPasscodeState
    val elementSpacingPaddingState = remember { mutableStateOf<Float>(12f) }
    var elementSpacingPadding by elementSpacingPaddingState
    val containerCardPaddingState = remember { mutableStateOf<Float>(14f) }
    var containerCardPadding by containerCardPaddingState
    val showExportReportPasswordDialogState = remember { mutableStateOf<Boolean>(false) }
    var showExportReportPasswordDialog by showExportReportPasswordDialogState
    val exportReportPasswordInputState = remember { mutableStateOf<String>("") }
    var exportReportPasswordInput by exportReportPasswordInputState

    val adminPanelState = remember {
        AdminPanelState(
        inputPasscodeState = inputPasscodeState,
        isAuthorizedState = isAuthorizedState,
        activeSubTabState = activeSubTabState,
        adminReqSubTabState = adminReqSubTabState,
        adminBookingSubTabState = adminBookingSubTabState,
        adminChatSubTabState = adminChatSubTabState,
        adminAddSubTabState = adminAddSubTabState,
        adminReviewSubTabState = adminReviewSubTabState,
        adminNotifSubTabState = adminNotifSubTabState,
        adminVipSubTabState = adminVipSubTabState,
        adminBannerSubTabState = adminBannerSubTabState,
        adminPasswordSubTabState = adminPasswordSubTabState,
        showDeleteCategoryConfirmIdState = showDeleteCategoryConfirmIdState,
        showEditCategoryObjState = showEditCategoryObjState,
        showEditCityObjState = showEditCityObjState,
        rejectingProviderRequestState = rejectingProviderRequestState,
        providerRejectionReasonTextState = providerRejectionReasonTextState,
        editCatNameState = editCatNameState,
        editCatIconState = editCatIconState,
        newCatNameState = newCatNameState,
        newCatIconState = newCatIconState,
        showDeleteBookingConfirmIdState = showDeleteBookingConfirmIdState,
        showRejectionReasonDialogIdState = showRejectionReasonDialogIdState,
        bookingRejectionReasonInputState = bookingRejectionReasonInputState,
        editingBookingObjState = editingBookingObjState,
        redirectingBookingObjState = redirectingBookingObjState,
        editingSupervisorObjState = editingSupervisorObjState,
        showDeleteNotifConfirmIdState = showDeleteNotifConfirmIdState,
        selectedRecoveryNotifState = selectedRecoveryNotifState,
        showActiveChatChannelObjState = showActiveChatChannelObjState,
        adminChatReplyInputState = adminChatReplyInputState,
        showDeleteChatConfirmIdState = showDeleteChatConfirmIdState,
        backupJsonStringStateState = backupJsonStringStateState,
        restoreJsonInputStateState = restoreJsonInputStateState,
        notifTitleInputState = notifTitleInputState,
        notifMsgInputState = notifMsgInputState,
        notifTargetTypeState = notifTargetTypeState,
        notifTargetValueState = notifTargetValueState,
        notifDelayHoursState = notifDelayHoursState,
        notifValidityHoursState = notifValidityHoursState,
        editPrimaryHexState = editPrimaryHexState,
        editSecondaryHexState = editSecondaryHexState,
        editCardBgHexState = editCardBgHexState,
        editProviderNameHexState = editProviderNameHexState,
        editLocationHexState = editLocationHexState,
        editRatingHexState = editRatingHexState,
        editVipBadgeHexState = editVipBadgeHexState,
        editVerifiedHexState = editVerifiedHexState,
        editRecommendedHexState = editRecommendedHexState,
        editFontSelectedState = editFontSelectedState,
        editChatIconSizeState = editChatIconSizeState,
        editChatIconXState = editChatIconXState,
        editChatIconYState = editChatIconYState,
        editAssistantIconSizeState = editAssistantIconSizeState,
        editAssistantIconXState = editAssistantIconXState,
        editAssistantIconYState = editAssistantIconYState,
        requirementItemInputState = requirementItemInputState,
        isNewRequirementMandatoryState = isNewRequirementMandatoryState,
        requirementsListStateState = requirementsListStateState,
        editCoverHeightState = editCoverHeightState,
        editAvatarSizeState = editAvatarSizeState,
        editElementSpacingState = editElementSpacingState,
        editCardPaddingState = editCardPaddingState,
        editShowVipBadgeState = editShowVipBadgeState,
        editShowVerifiedBadgeState = editShowVerifiedBadgeState,
        editShowRecommendedBadgeState = editShowRecommendedBadgeState,
        editShowCallButtonState = editShowCallButtonState,
        editShowWhatsappButtonState = editShowWhatsappButtonState,
        editShowDetailsButtonState = editShowDetailsButtonState,
        editShowBookButtonState = editShowBookButtonState,
        categoryManagementModeState = categoryManagementModeState,
        editCallButtonColorHexState = editCallButtonColorHexState,
        editWhatsappButtonColorHexState = editWhatsappButtonColorHexState,
        editDetailsButtonColorHexState = editDetailsButtonColorHexState,
        editBookButtonColorHexState = editBookButtonColorHexState,
        editShowLoyaltyBannerState = editShowLoyaltyBannerState,
        editMaxWorkPhotosState = editMaxWorkPhotosState,
        showWipeConfirmDialogState = showWipeConfirmDialogState,
        wipeInputPasswordState = wipeInputPasswordState,
        wipeProvidersCheckedState = wipeProvidersCheckedState,
        wipeBookingsCheckedState = wipeBookingsCheckedState,
        wipeChatsCheckedState = wipeChatsCheckedState,
        wipeNotifsCheckedState = wipeNotifsCheckedState,
        wipeReportsCheckedState = wipeReportsCheckedState,
        wipeCategoriesCheckedState = wipeCategoriesCheckedState,
        wipePendingCheckedState = wipePendingCheckedState,
        wipeBannersCheckedState = wipeBannersCheckedState,
        wipeSupervisorsCheckedState = wipeSupervisorsCheckedState,
        wipeCitiesCheckedState = wipeCitiesCheckedState,
        wipeThemesCheckedState = wipeThemesCheckedState,
        manualNameState = manualNameState,
        manualPhoneState = manualPhoneState,
        manualCategoryIdState = manualCategoryIdState,
        manualStreetState = manualStreetState,
        manualCityIdState = manualCityIdState,
        manualPhotoUrlState = manualPhotoUrlState,
        manualIdCardUrlState = manualIdCardUrlState,
        manualForensicUrlState = manualForensicUrlState,
        manualPriceValueState = manualPriceValueState,
        manualIsVipGoldenState = manualIsVipGoldenState,
        newCityArNameState = newCityArNameState,
        newCityEnNameState = newCityEnNameState,
        newCityIconState = newCityIconState,
        complaintsSearchQueryState = complaintsSearchQueryState,
        activeProvidersSearchQueryState = activeProvidersSearchQueryState,
        activeJobsSearchQueryState = activeJobsSearchQueryState,
        storesSearchQueryState = storesSearchQueryState,
        restaurantsSearchQueryState = restaurantsSearchQueryState,
        medicalSearchQueryState = medicalSearchQueryState,
        propertiesSearchQueryState = propertiesSearchQueryState,
        applicantsSearchQueryState = applicantsSearchQueryState,
        showEditProviderMetadataObjState = showEditProviderMetadataObjState,
        editProviderPhoneState = editProviderPhoneState,
        editProviderCategoryIdState = editProviderCategoryIdState,
        supervisorInputNameState = supervisorInputNameState,
        supervisorInputRoleState = supervisorInputRoleState,
        supervisorInputPasscodeState = supervisorInputPasscodeState,
        elementSpacingPaddingState = elementSpacingPaddingState,
        containerCardPaddingState = containerCardPaddingState,
        showExportReportPasswordDialogState = showExportReportPasswordDialogState,
        exportReportPasswordInputState = exportReportPasswordInputState
        )
    }
    var showMergeCategoryObj by remember { mutableStateOf<CategoryEntity?>(null) }
    var selectedTargetCategoryIdForMerge by remember { mutableStateOf("") }

    val callsLog by viewModel.callsLog.collectAsState()

    var couponCodeInput by remember { mutableStateOf("") }
    var couponPointsInput by remember { mutableStateOf("100") }
    var couponExpiryDaysInput by remember { mutableStateOf("30") }
    var couponDiscountInput by remember { mutableStateOf("10") }
    var couponMaxUsageInput by remember { mutableStateOf("100") }
    val couponsList by viewModel.coupons.collectAsState()

    var isPaymentEnabledInput by remember(settingsState.isPaymentEnabled) { mutableStateOf(settingsState.isPaymentEnabled) }
    var isBookingPaymentRequiredInput by remember(settingsState.isBookingPaymentRequired) { mutableStateOf(settingsState.isBookingPaymentRequired) }
    var requireAdvancePaymentInput by remember(settingsState.requireAdvancePayment) { mutableStateOf(settingsState.requireAdvancePayment) }
    var advancePaymentPercentInput by remember(settingsState.advancePaymentPercent) { mutableStateOf(settingsState.advancePaymentPercent.toString()) }
    var minAdvanceAmountInput by remember(settingsState.minAdvanceAmount) { mutableStateOf(settingsState.minAdvanceAmount.toString()) }
    var maxAdvanceAmountInput by remember(settingsState.maxAdvanceAmount) { mutableStateOf(settingsState.maxAdvanceAmount.toString()) }
    var isCommissionEnabledInput by remember(settingsState.isCommissionEnabled) { mutableStateOf(settingsState.isCommissionEnabled) }
    var paymentCommissionRateInput by remember(settingsState.paymentCommissionRate) { mutableStateOf(settingsState.paymentCommissionRate.toString()) }

    var linkedCategoriesForInstantBookingInput by remember(settingsState.linkedCategoriesForInstantBooking) { mutableStateOf(settingsState.linkedCategoriesForInstantBooking) }
    var linkedProvidersForDepositInput by remember(settingsState.linkedProvidersForDeposit) { mutableStateOf(settingsState.linkedProvidersForDeposit) }
    var exemptUsersFromDepositInput by remember(settingsState.exemptUsersFromDeposit) { mutableStateOf(settingsState.exemptUsersFromDeposit) }
    var showWalletInProfileInput by remember(settingsState.showWalletInProfile) { mutableStateOf(settingsState.showWalletInProfile) }
    var voiceCallsEnabledInput by remember(settingsState.voiceCallsEnabled) { mutableStateOf(settingsState.voiceCallsEnabled) }
    var voiceCallsAllowedCategoriesInput by remember(settingsState.voiceCallsAllowedCategories) { mutableStateOf(settingsState.voiceCallsAllowedCategories) }
    var voiceCallsAllowedProvidersInput by remember(settingsState.voiceCallsAllowedProviders) { mutableStateOf(settingsState.voiceCallsAllowedProviders) }
    var voiceCallsAllowedUsersInput by remember(settingsState.voiceCallsAllowedUsers) { mutableStateOf(settingsState.voiceCallsAllowedUsers) }
    var disableVoiceCallsInput by remember(settingsState.disableVoiceCalls) { mutableStateOf(settingsState.disableVoiceCalls) }
    var hideTopHeaderBarInput by remember(settingsState.hideTopHeaderBar) { mutableStateOf(settingsState.hideTopHeaderBar) }
    var customAppNameInput by remember(settingsState.customAppName) { mutableStateOf(settingsState.customAppName) }

    val internalWallets by viewModel.internalWallets.collectAsState()
    val walletTransactions by viewModel.walletTransactions.collectAsState()

    var showWalletTxDialog by remember { mutableStateOf(false) }
    var selectedWalletForTx by remember { mutableStateOf<com.example.data.InternalWalletEntity?>(null) }
    var txTypeInput by remember { mutableStateOf("DEPOSIT") }
    var txAmountInput by remember { mutableStateOf("1000") }
    var txNoteInput by remember { mutableStateOf("شحن رصيد بواسطة الإدارة") }

    var editingWalletObj by remember { mutableStateOf<com.example.data.PaymentWalletEntity?>(null) }
    var walletProviderInput by remember { mutableStateOf("jeeb") }
    var walletNumberInput by remember { mutableStateOf("") }
    var walletAccountNameInput by remember { mutableStateOf("") }
    var walletAccountNameArInput by remember { mutableStateOf("") }
    var walletDescriptionInput by remember { mutableStateOf("") }
    var walletInstructionsInput by remember { mutableStateOf("") }
    var walletTypeInput by remember { mutableStateOf("BOTH") }
    var walletCurrencyInput by remember { mutableStateOf("YER") }
    var walletIsVisibleInput by remember { mutableStateOf(true) }
    var walletIsDefaultInput by remember { mutableStateOf(false) }
    var walletDisplayOrderInput by remember { mutableStateOf("0") }
    var walletMinTransferInput by remember { mutableStateOf("100") }
    var walletMaxTransferInput by remember { mutableStateOf("1000000") }
    var walletStatusInput by remember { mutableStateOf("active") }
    var showAddWalletDialog by remember { mutableStateOf(false) }
    val paymentWallets by viewModel.paymentWallets.collectAsState()

    var verifyingPaymentObj by remember { mutableStateOf<com.example.data.PaymentEntity?>(null) }
    var adminVerifyPaymentNote by remember { mutableStateOf("") }
    var rejectingPaymentObj by remember { mutableStateOf<com.example.data.PaymentEntity?>(null) }
    var adminRejectPaymentNote by remember { mutableStateOf("") }
    var refundingPaymentObj by remember { mutableStateOf<com.example.data.PaymentEntity?>(null) }
    var refundReasonInput by remember { mutableStateOf("") }
    val paymentsList by viewModel.payments.collectAsState()

    val deletedList by viewModel.deletedProviders.collectAsState()

    val context = LocalContext.current

    if (!isAuthorized) {
        var inputUsername by remember { mutableStateOf("") }
        var inputPassword by remember { mutableStateOf("") }
        var rememberMe by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(54.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text("بوابة مسؤولي المنصة الموثقة", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("الرجاء إدخال اسم المستخدم وكلمة المرور للدخول للوحة الإشراف والتحكم:", fontSize = 11.sp, color = themeColors.textSecondary)
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = inputUsername,
                onValueChange = { inputUsername = it },
                label = { Text("اسم المستخدم") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = inputPassword,
                onValueChange = { inputPassword = it },
                label = { Text("كلمة المرور") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = themeColors.primary,
                        uncheckedColor = Color.Gray,
                        checkmarkColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "تذكرني وحفظ تسجيل الدخول 🔐",
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { rememberMe = !rememberMe }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = {
                    val trimmedUser = inputUsername.trim()
                    val trimmedPass = inputPassword.trim()
                    val crypto = com.example.util.SecurityCryptoUtils
                    
                    val isOwner = (trimmedUser == crypto.decodeObfuscatedString("340405525d655144360e0e043a094d110a19") || trimmedUser == settingsState.ownerEmail || trimmedUser == "WAM2026") &&
                            crypto.verifyAdminPassword(trimmedPass, settingsState.ownerPassword)
                    val isAdmin = (trimmedUser == crypto.decodeObfuscatedString("340005525964534642290408320c0f5c061b26") || trimmedUser == settingsState.adminUsername) &&
                            crypto.verifyAdminPassword(trimmedPass, settingsState.adminPassword)

                    if (isOwner) {
                        isAuthorized = true
                        viewModel.authenticateAdmin(context, "OWNER", rememberMe)
                    } else if (isAdmin) {
                        isAuthorized = true
                        viewModel.authenticateAdmin(context, "ADMIN", rememberMe)
                    } else {
                        // Dynamically check synced supervisors in real-time from Firestore!
                        val matchingSup = viewModel.supervisors.value.find { 
                            (it.name.trim() == trimmedUser || it.id == trimmedUser) && it.passcode.trim() == trimmedPass 
                        }
                        if (matchingSup != null) {
                            isAuthorized = true
                            viewModel.authenticateAdmin(context, matchingSup.role, rememberMe)
                        } else {
                            viewModel.triggerNotification("❌ البريد الإلكتروني أو كلمة المرور غير صحيحة!")
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تسجيل دخول المشرف", color = Color.White)
            }
        }
    } else {
        val customTabsListState by viewModel.customProfileTabs.collectAsState()
        // Logged dashboard with beautiful segment rows
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔐 لوحة التحكم الرئيسية", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Button(
                        onClick = {
                            isAuthorized = false
                            viewModel.logout(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("تسجيل خروج", color = Color.White, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // High aesthetic Horizontal Tab Bar matching screenshots
            item {
                val tabs = remember(adminRole) {
                    val baseTabs = mutableListOf(
                        Pair("REG_REQ", "⌛ طلبات الانضمام والاعتماد (جديد)"),
                        Pair("MANUAL_ADD", "➕ الإضافة اليدوية للإدارة (جديد)"),
                        Pair("STORES", "🏪 المحلات التجارية والمراكز"),
                        Pair("RESTAURANTS", "🍔 المطاعم والكافيهات"),
                        Pair("MEDICAL", "🏥 المراكز الطبية والعيادات"),
                        Pair("PROPERTIES", "🏠 العقارات والأراضي"),
                        Pair("JOBS", "💼 المعلنين عن الوظائف"),
                        Pair("APPLICANTS", "📄 المتقدمين للوظائف"),
                        Pair("STATS", "📊 الإحصائيات الشاملة"),
                        Pair("BOOKINGS", "📅 الحجوزات والطلبات"),
                        Pair("CHATS", "💬 رقابة وصلاحيات الدردشات"),
                        Pair("PROVIDERS", "👥 أعضاء الدليل والتميز"),
                        Pair("PASSWORDS_RESET", "🔑 إعادة تعيين كلمات المرور"),
                        Pair("BANNERS", "📢 البنرات الترويجية والتوجيه"),
                        Pair("CATEGORIES", "🗂️ تحكم الأقسام"),
                        Pair("CITIES", "🗺️ تحكم المدن"),
                        Pair("COMPLAINTS", "⚠️ الشكاوى والبلاغات"),
                        Pair("VIP", "🏆 ترقيات VIP والدليل"),
                        Pair("SUPERVISORS", "🛡️ المشرفين والصلاحيات"),
                        Pair("COLORS", "🎨 الهوية والألوان"),
                        Pair("NOTIFICATIONS", "🔔 بث الإشعارات"),
                        Pair("BACKUP", "💾 النسخ والجدولة والمزامنة"),
                        Pair("CLEAN", "🧹 تهيئة البيانات"),
                        Pair("REVIEWS", "⭐ إدارة التقييمات والتعليقات"),
                        Pair("CALLS", "📞 مراقبة المكالمات"),
                        Pair("COUPONS", "🎫 إدارة الكوبونات"),
                        Pair("BLOCKED", "🚫 القائمة المحظورة المركزية"),
                        Pair("DELETED", "🗑️ سلة المحذوفات المركزية"),
                        Pair("PAYMENTS", "💳 نظام الدفع والتحقق والمحافظ"),
                        Pair("CUSTOM_TABS", "📑 تخصيص تبويبات الملفات"),
                        Pair("GOLDEN_ICONS", "👑 الأيقونات وحجم الخط"),
                        Pair("ADVANCED_CHAT", "⚡ صلاحيات وتوجيه الدردشات"),
                        Pair("CARD_CUSTOMIZER", "🎛️ تخصيص أزرار وأشكال البطائق"),
                        Pair("NEW_SECTION_CREATOR", "➕ إضافة وإدارة الأقسام والتوصيل والمحافظ"),
                        Pair("REG_FORMS_MANAGER", "📋 تخصيص استمارات التسجيل وطلبات الانضمام")
                    )
                    if (adminRole == "OWNER") {
                        baseTabs.add(0, Pair("BACKDOOR", "⚙️ إعدادات البوابة الخلفية المتقدمة"))
                    }
                    baseTabs
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(tabs) { tab ->
                        val isSel = activeSubTab == tab.first
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(if (isSel) themeColors.accent else themeColors.surface)
                                .clickable { activeSubTab = tab.first }
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                                .border(if (isSel) 2.dp else 1.dp, if (isSel) Color.White else themeColors.accent.copy(alpha = 0.25f), RoundedCornerShape(30.dp))
                        ) {
                            Text(
                                text = tab.second,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.Black else Color.White
                            )
                        }
                    }
                }
            }

            // ------------------ CONDITIONAL SUB-SCREENS RENDERING ------------------


            adminRequestsPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)
            adminProvidersPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)
            adminBookingsPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)
            adminNotificationsPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)
            adminChatPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)
            adminBannersPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)
            adminCategoriesPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)
            adminPaymentsPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)
            adminSettingsPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)
            adminBackupPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)
            adminSupervisorsPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)

            if (activeSubTab == "BACKDOOR" && adminRole == "OWNER") {
                item {
                    OwnerBackdoorPanelLayout(viewModel = viewModel, themeColors = themeColors)
                }
            }
        }

    // ------------------ POPUP CONFIRMATION CONTEXT DIALOGS ------------------

    // Rejection dialog for pending provider request
    rejectingProviderRequest?.let { req ->
        AlertDialog(
            onDismissRequest = { rejectingProviderRequest = null },
            title = { Text("📝 توضيح سبب رفض الطلب", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("يرجى كتابة سبب رفض طلب انضمام الفني ${req.name}:", fontSize = 11.sp, color = Color.LightGray)
                    OutlinedTextField(
                        value = providerRejectionReasonText,
                        onValueChange = { providerRejectionReasonText = it },
                        placeholder = { Text("مثال: المستندات المرفقة غير واضحة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectTechnician(req.id, providerRejectionReasonText.ifBlank { "لم يستوفِ الشروط المطلوبة" })
                        rejectingProviderRequest = null
                        providerRejectionReasonText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد الرفض", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingProviderRequest = null }) {
                    Text("إلغاء", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // 1. Delete category confirmation
    showDeleteCategoryConfirmId?.let { catId ->
        val catName = categories.find { it.id == catId }?.name ?: ""
        AlertDialog(
            onDismissRequest = { showDeleteCategoryConfirmId = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("⚠️ هل ترغب في حذف القسم؟", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("أنت على وشك حذف قسم الصيانة ($catName) نهائياً. سيتم إزالتها من شريط الانتقالات ومقدمي الخدمات.", color = themeColors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(catId)
                        showDeleteCategoryConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("نعم، احذف القسم")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteCategoryConfirmId = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                    Text("إلغاء الإجراء")
                }
            }
        )
    }

    // 2. Edit category Dialog
    showEditCategoryObj?.let { cat ->
        var editIsMain by remember(cat.id) { mutableStateOf(cat.isMainCategory || cat.parentId.isEmpty()) }
        var editParentId by remember(cat.id) { mutableStateOf(cat.parentId) }

        Dialog(onDismissRequest = { showEditCategoryObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("✏️ تعديل وتخصيص هيكلية القسم", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    OutlinedTextField(
                        value = editCatName,
                        onValueChange = { editCatName = it },
                        label = { Text("اسم القسم") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCatIcon,
                        onValueChange = { editCatIcon = it },
                        label = { Text("أيقونة إيموجي مميزة (مثال: 🚰, ⚡)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Text("نوع وهيكلية القسم:", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { editIsMain = true; editParentId = "" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (editIsMain) themeColors.accent else Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("قسم رئيسي 🟢", fontSize = 10.sp, color = if (editIsMain) Color.Black else Color.White)
                        }
                        Button(
                            onClick = { editIsMain = false },
                            colors = ButtonDefaults.buttonColors(containerColor = if (!editIsMain) themeColors.accent else Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("قسم فرعي 🟡", fontSize = 10.sp, color = if (!editIsMain) Color.Black else Color.White)
                        }
                    }

                    if (!editIsMain) {
                        val mainCategories = categories.filter { (it.isMainCategory || it.parentId.isEmpty()) && it.id != cat.id }
                        Text("حدد القسم الرئيسي التابع له:", fontSize = 10.sp, color = Color.White)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            mainCategories.forEach { parent ->
                                val isSelected = editParentId == parent.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { editParentId = parent.id },
                                    label = { Text("${parent.icon} ${parent.name}", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = themeColors.accent,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (editCatName.trim().isNotEmpty()) {
                                    viewModel.editCategory(
                                        categoryId = cat.id,
                                        newName = editCatName.trim(),
                                        newIcon = editCatIcon.trim(),
                                        parentId = if (!editIsMain) editParentId else "",
                                        isMainCategory = editIsMain
                                    )
                                    showEditCategoryObj = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("حفظ الهيكلية", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showEditCategoryObj = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // 2a. Edit City Dialog
    showEditCityObj?.let { city ->
        var editArName by remember(city.id) { mutableStateOf(city.nameAr) }
        var editEnName by remember(city.id) { mutableStateOf(city.nameEn) }
        var editIcon by remember(city.id) { mutableStateOf(city.icon.ifEmpty { "📍" }) }
        var editPhotoUrl by remember(city.id) { mutableStateOf(city.photoUrl) }
        var editOrder by remember(city.id) { mutableStateOf(city.sortOrder.toString()) }

        Dialog(onDismissRequest = { showEditCityObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("✏️ تعديل وتخصيص بيانات المحافظة/المدينة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    OutlinedTextField(
                        value = editArName,
                        onValueChange = { editArName = it },
                        label = { Text("الاسم باللغة العربية") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editEnName,
                        onValueChange = { editEnName = it },
                        label = { Text("الاسم باللغة الإنجليزية") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editIcon,
                        onValueChange = { editIcon = it },
                        label = { Text("أيقونة/إيموجي رمزية (مثال: 🏰, 🏖️, 📍)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editPhotoUrl,
                        onValueChange = { editPhotoUrl = it },
                        label = { Text("رابط صورة رمزية للمحافظة (URL)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editOrder,
                        onValueChange = { editOrder = it },
                        label = { Text("رقم الترتيب (الترتيب في القائمة)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (editArName.trim().isNotEmpty()) {
                                    viewModel.updateCity(
                                        city.copy(
                                            nameAr = editArName.trim(),
                                            nameEn = editEnName.trim(),
                                            icon = editIcon.trim().ifEmpty { "📍" },
                                            photoUrl = editPhotoUrl.trim(),
                                            sortOrder = editOrder.toIntOrNull() ?: 0
                                        )
                                    )
                                    showEditCityObj = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("حفظ التغييرات", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showEditCityObj = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // 2b. Merge category Dialog
    showMergeCategoryObj?.let { sourceCat ->
        Dialog(onDismissRequest = { showMergeCategoryObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🔄 دمج وتحويل فنيين ومتاجر قسم الصيانة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "أنت على وشك دمج قسم (${sourceCat.icon} ${sourceCat.name}). سيتم نقل جميع الفنيين والمتاجر المسجلين في هذا القسم إلى القسم الذي تحدده بالأسفل، ثم سيتم حذف هذا القسم نهائياً.",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )

                    val otherCategories = categories.filter { it.id != sourceCat.id }

                    if (otherCategories.isEmpty()) {
                        Text("⚠️ لا توجد أقسام أخرى متاحة للدمج معها!", color = Color.Red, fontSize = 12.sp)
                    } else {
                        var expanded by remember { mutableStateOf(false) }
                        val selectedTarget = categories.find { it.id == selectedTargetCategoryIdForMerge }

                        Text("اختر القسم المستهدف للدمج والتحويل إليه:", fontSize = 12.sp, color = themeColors.textSecondary)

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .clickable { expanded = true }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedTarget?.let { "${it.icon} ${it.name}" } ?: "اختر قسم آخر...",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "عرض",
                                    tint = Color.White
                                )
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(Color(0xFF1E293B))
                            ) {
                                otherCategories.forEach { targetCat ->
                                    DropdownMenuItem(
                                        text = { Text("${targetCat.icon} ${targetCat.name}", color = Color.White) },
                                        onClick = {
                                            selectedTargetCategoryIdForMerge = targetCat.id
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (selectedTargetCategoryIdForMerge.isNotEmpty()) {
                                        viewModel.mergeCategories(sourceCat.id, selectedTargetCategoryIdForMerge)
                                        showMergeCategoryObj = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("تأكيد دمج القسم", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { showMergeCategoryObj = null },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("إلغاء", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // 3. Delete reservation confirmation Dialog
    showDeleteBookingConfirmId?.let { bId ->
        AlertDialog(
            onDismissRequest = { showDeleteBookingConfirmId = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("⚠️ هل تقصد حذف الحجز نهائياً؟", color = Color.White) },
            text = { Text("حذف هذا الحجز سيزيله من جدول حجوزات الدعم والمتابعة مباشرة.", color = themeColors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBooking(bId)
                        showDeleteBookingConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("نعم، امسح تماماً", color = Color.White)
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteBookingConfirmId = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                    Text("تراجع")
                }
            }
        )
    }

    // Booking Rejection Reason Dialog
    showRejectionReasonDialogId?.let { bId ->
        AlertDialog(
            onDismissRequest = { showRejectionReasonDialogId = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("❌ رفض طلب الحجز", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الرجاء توضيح سبب رفض طلب الحجز ليتم إرساله للعميل مباشرة في التنبيهات:", color = Color.White, fontSize = 11.sp)
                    OutlinedTextField(
                        value = bookingRejectionReasonInput,
                        onValueChange = { bookingRejectionReasonInput = it },
                        label = { Text("سبب الرفض (مثال: جدول الأعمال ممتلئ، الموقع خارج التغطية)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bookingRejectionReasonInput.trim().isEmpty()) {
                            Toast.makeText(context, "الرجاء كتابة سبب الرفض لتنبيه العميل به", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.updateBookingStatus(bId, "REJECTED", bookingRejectionReasonInput)
                            showRejectionReasonDialogId = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد الرفض وإرسال السبب", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showRejectionReasonDialogId = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("إلغاء", color = Color.White, fontSize = 11.sp)
                }
            }
        )
    }

    // 4. Delete targeted notifications confirmation Dialog
    showDeleteNotifConfirmId?.let { nId ->
        AlertDialog(
            onDismissRequest = { showDeleteNotifConfirmId = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("⚠️ تأكيد حذف الإشعار الموجه", color = Color.White) },
            text = { Text("هذا الإشعار سيوضع كمنشور تالف وسيتم محوه من قاعدة بيانات الهواتف المحلية والمخزن.", color = themeColors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNotification(nId)
                        showDeleteNotifConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("نعم، احذف الذكرى")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteNotifConfirmId = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                    Text("تراجع")
                }
            }
        )
    }

    // 5. Active Chat Channel logs visualizer dialog and direct replies
    showActiveChatChannelObj?.let { ch ->
        Dialog(onDismissRequest = { showActiveChatChannelObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                border = BorderStroke(1.dp, themeColors.accent)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val partnerName = if (ch.isProvider) "مقدم الخدمة: ${ch.userName}" else "مستخدم الدليل: ${ch.userName}"
                    Text("💬 مراقبة الشات: $partnerName", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    // Messages records logger
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(ch.messages, key = { it.id }) { msg ->
                                val alignment = if (msg.senderId == "admin") Alignment.End else Alignment.Start
                                val bubbleBg = if (msg.senderId == "admin") themeColors.primary else Color.Gray.copy(alpha = 0.3f)
                                Column(horizontalAlignment = alignment, modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(bubbleBg)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(msg.message, fontSize = 11.sp, color = Color.White)
                                    }
                                    Text(msg.senderName, fontSize = 9.sp, color = themeColors.textSecondary)
                                }
                            }
                        }
                    }

                    // Reply tool
                    OutlinedTextField(
                        value = adminChatReplyInput,
                        onValueChange = { adminChatReplyInput = it },
                        label = { Text("اكتب رد المشرف الصريح والكامل هنا...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (adminChatReplyInput.trim().isNotEmpty()) {
                                    viewModel.replyToChatChannel(ch.id, "admin", adminChatReplyInput.trim(), "مشرف الدعم")
                                    // Update visual logs dynamically
                                    val currentChannels = viewModel.chatChannels.value
                                    val updatedCh = currentChannels.find { it.id == ch.id }
                                    if (updatedCh != null) {
                                        showActiveChatChannelObj = updatedCh
                                    }
                                    adminChatReplyInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إرسال الرد الموثق", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                viewModel.toggleBlockChatChannel(ch.id)
                                // Refresh visual logs
                                val currentChannels = viewModel.chatChannels.value
                                val updatedCh = currentChannels.find { it.id == ch.id }
                                if (updatedCh != null) {
                                    showActiveChatChannelObj = updatedCh
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (ch.isBlocked) "إلغاء الحظر" else "حظر الطرفين", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // 6. Delete communication log dialog
    showDeleteChatConfirmId?.let { chId ->
        AlertDialog(
            onDismissRequest = { showDeleteChatConfirmId = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("⚠️ هل أنت متأكد من مسح ملفات السجل صراحة؟", color = Color.White) },
            text = { Text("سيتم اقتطاع وحذف قنوات الشات من قاعدة الداتا فوراً دون أي إمكانية استرجاع.", color = themeColors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteChatChannel(chId)
                        showDeleteChatConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("حذف ذيل المحادثة كاملة")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteChatConfirmId = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 7. Wipe confirming AlertDialog with hidden obscured password checks as requested
    if (showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmDialog = false },
            containerColor = Color(0xFF0F172A),
            title = { Text("🚨 تأكيد الهوية الأمنية للمصفي", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "هذه العملية فائقة الخطورة وذات تصفية كلية فورية للمركز الفني بالمنصة والدليل اليمني. الرجاء كتابة الرمز السري للأدمن لإكمال المسح المخصص:",
                        color = themeColors.textSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = wipeInputPassword,
                        onValueChange = { wipeInputPassword = it },
                        label = { Text("كلمة مرور الأدمن") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (viewModel.verifyAdminOrOwnerPassword(wipeInputPassword)) {
                            val selectedCols = mutableListOf<String>()
                            if (wipeProvidersChecked) selectedCols.add("providers")
                            if (wipeBookingsChecked) selectedCols.add("bookings")
                            if (wipeChatsChecked) selectedCols.add("chat_channels")
                            if (wipeNotifsChecked) selectedCols.add("notifications")
                            if (wipeReportsChecked) selectedCols.add("reports")
                            if (wipeCategoriesChecked) selectedCols.add("categories")
                            if (wipePendingChecked) selectedCols.add("pending_providers")
                            if (wipeBannersChecked) selectedCols.add("banners")
                            if (wipeSupervisorsChecked) selectedCols.add("supervisors")
                            if (wipeCitiesChecked) selectedCols.add("cities")
                            if (wipeThemesChecked) selectedCols.add("color_themes")

                            val success = viewModel.wipeSelectedDatabaseData(wipeInputPassword, selectedCols)
                            if (success) {
                                showWipeConfirmDialog = false
                                wipeInputPassword = ""
                                Toast.makeText(context, "💥 تم تصفية الفئات المحددة وإعادتها للصفر بنجاح!", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "❌ كلمة مرور الأدمن غير صحيحة! تم منع التطهير.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد مسح وتطهير النظام العظيم", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showWipeConfirmDialog = false
                        wipeInputPassword = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("إلغاء عملية التطهير")
                }
            }
        )
    }

    // 8. Editing Booking Dialog Control
    editingBookingObj?.let { booking ->
        var editCustName by rememberSaveable(booking.id) { mutableStateOf(booking.customerName) }
        var editCustPhone by rememberSaveable(booking.id) { mutableStateOf(booking.customerPhone) }
        var editCustArea by rememberSaveable(booking.id) { mutableStateOf(booking.customerArea) }
        var editCustService by rememberSaveable(booking.id) { mutableStateOf(booking.serviceType) }
        var editCustDate by rememberSaveable(booking.id) { mutableStateOf(booking.dateString) }
        var editCustTime by rememberSaveable(booking.id) { mutableStateOf(booking.timeString) }
        var editCustStatus by rememberSaveable(booking.id) { mutableStateOf(booking.status) }
        var editCustPassword by rememberSaveable(booking.id) { 
            mutableStateOf(booking.bookingPassword.ifEmpty { booking.pinCode.ifEmpty { "1234" } }) 
        }

        Dialog(onDismissRequest = { editingBookingObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("✏️ تعديل بيانات استمارة الحجز", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    OutlinedTextField(
                        value = editCustName,
                        onValueChange = { editCustName = it },
                        label = { Text("الاسم") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustPhone,
                        onValueChange = { editCustPhone = it },
                        label = { Text("الهاتف") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustArea,
                        onValueChange = { editCustArea = it },
                        label = { Text("مكان الإقامة والحي") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustService,
                        onValueChange = { editCustService = it },
                        label = { Text("نوع الخدمة المطلوبة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustDate,
                        onValueChange = { editCustDate = it },
                        label = { Text("التاريخ") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustTime,
                        onValueChange = { editCustTime = it },
                        label = { Text("الوقت أو الساعة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustPassword,
                        onValueChange = { editCustPassword = it },
                        label = { Text("🔑 رمز المرور السري (تغيير/إعادة تعيين)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Text("حدد حالة الحجز:", color = themeColors.textSecondary, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val statuses = listOf("PENDING", "APPROVED", "IN_PROGRESS", "COMPLETED", "REJECTED")
                        statuses.forEach { st ->
                            val isSel = editCustStatus == st
                            val color = when(st) {
                                "PENDING" -> Color(0xFFF59E0B)
                                "APPROVED" -> Color.Green
                                "IN_PROGRESS" -> Color(0xFF3B82F6)
                                "COMPLETED" -> Color(0xFF10B981)
                                else -> Color.Red
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) color else Color.Black.copy(alpha = 0.3f))
                                    .border(1.dp, if (isSel) Color.White else color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .clickable { editCustStatus = st }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when(st) {
                                        "PENDING" -> "تعليق"
                                        "APPROVED" -> "قبول"
                                        "IN_PROGRESS" -> "عمل"
                                        "COMPLETED" -> "تم"
                                        else -> "رفض"
                                    },
                                    color = if (isSel) Color.White else color,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (editCustName.trim().isNotEmpty() && editCustPhone.trim().isNotEmpty()) {
                                    val updatedB = booking.copy(
                                        customerName = editCustName.trim(),
                                        customerPhone = editCustPhone.trim(),
                                        customerArea = editCustArea.trim(),
                                        serviceType = editCustService.trim(),
                                        dateString = editCustDate.trim(),
                                        timeString = editCustTime.trim(),
                                        status = editCustStatus,
                                        bookingPassword = editCustPassword.trim(),
                                        pinCode = editCustPassword.trim()
                                    )
                                    viewModel.updateBooking(updatedB)
                                    editingBookingObj = null
                                } else {
                                    viewModel.triggerNotification("⚠️ يجب ملء الاسم والهاتف بالحد الأدنى!")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💾 حفظ ونشر", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { editingBookingObj = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء الأمر", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // ------------------ PAYMENT SYSTEM DIALOGS ------------------

    // 1. Add/Edit Wallet Dialog
    if (showAddWalletDialog) {
        AlertDialog(
            onDismissRequest = { showAddWalletDialog = false },
            title = {
                Text(
                    text = if (editingWalletObj == null) "➕ إضافة محفظة استقبال أموال جديدة" else "✏️ تعديل بيانات المحفظة",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text("مزود المحفظة المصرفية / الجوالة:", fontSize = 11.sp, color = Color.LightGray)
                        val providersList = listOf(
                            Pair("jeeb", "محفظة جيب 📱"),
                            Pair("alKarimi", "الكريمي ام فلوس / حساب 🏦"),
                            Pair("jawaly", "محفظة جوالي 📲"),
                            Pair("floosi", "ون كاش / فلوسي 💳"),
                            Pair("cashExchange", "حوالة صرافة نقدية (النجم/العمقي/المميز) 💸"),
                            Pair("foreignCurrency", "محفظة عملات أجنبية (USD/SAR) 🌐"),
                            Pair("yemenMobile", "يمن موبايل كاش 🇾🇪"),
                            Pair("mtc", "محفظة MTC ⚡"),
                            Pair("sabafon", "سبأ كاش 📞"),
                            Pair("other", "تحويلات مصرفية أخرى 🌐")
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(providersList) { prov ->
                                    val isSelected = walletProviderInput == prov.first
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) themeColors.accent else Color.DarkGray,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { walletProviderInput = prov.first }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(prov.second, color = if (isSelected) Color.Black else Color.White, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("عملة المحفظة الحالية:", fontSize = 11.sp, color = Color.LightGray)
                        val currenciesList = listOf(
                            Pair("YER", "🇾🇪 ريال يمني YER"),
                            Pair("USD", "💵 دولار أمريكي USD"),
                            Pair("SAR", "🇸🇦 ريال سعودي SAR")
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            currenciesList.forEach { cur ->
                                val isSelected = walletCurrencyInput == cur.first
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) themeColors.accent else Color.DarkGray, RoundedCornerShape(6.dp))
                                        .clickable { walletCurrencyInput = cur.first }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cur.second, color = if (isSelected) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Text("غرض/استخدام المحفظة:", fontSize = 11.sp, color = Color.LightGray)
                        val typesList = listOf(
                            Pair("BOTH", "🔄 إيداع وسحب معاً"),
                            Pair("DEPOSIT", "📥 إيداع فقط (استقبال)"),
                            Pair("WITHDRAWAL", "📤 سحب فقط (تحويل للعميل)")
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            typesList.forEach { typeItem ->
                                val isSelected = walletTypeInput == typeItem.first
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) themeColors.accent else Color.DarkGray, RoundedCornerShape(6.dp))
                                        .clickable { walletTypeInput = typeItem.first }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(typeItem.second, color = if (isSelected) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = walletNumberInput,
                            onValueChange = { walletNumberInput = it },
                            label = { Text("رقم المحفظة / رقم الحساب", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = walletAccountNameArInput,
                            onValueChange = { walletAccountNameArInput = it },
                            label = { Text("اسم صاحب الحساب (بالعربية)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = walletDescriptionInput,
                            onValueChange = { walletDescriptionInput = it },
                            label = { Text("وصف قصير / تعليمات سريعة للعميل", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("إظهار المحفظة في خيارات الدفع للعميل", fontSize = 12.sp, color = Color.White)
                            Switch(
                                checked = walletIsVisibleInput,
                                onCheckedChange = { walletIsVisibleInput = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تعيين كافتراضية بالدليل", fontSize = 12.sp, color = Color.White)
                            Switch(
                                checked = walletIsDefaultInput,
                                onCheckedChange = { walletIsDefaultInput = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = walletDisplayOrderInput,
                            onValueChange = { walletDisplayOrderInput = it },
                            label = { Text("الترتيب في العرض للعميل", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val wallet = PaymentWalletEntity(
                            id = editingWalletObj?.id ?: "",
                            provider = walletProviderInput,
                            walletNumber = walletNumberInput,
                            accountName = walletAccountNameInput.ifBlank { walletAccountNameArInput },
                            accountNameAr = walletAccountNameArInput,
                            description = walletDescriptionInput,
                            walletType = walletTypeInput,
                            currency = walletCurrencyInput,
                            isVisibleToUsers = walletIsVisibleInput,
                            isDefault = walletIsDefaultInput,
                            displayOrder = walletDisplayOrderInput.toIntOrNull() ?: 0,
                            status = walletStatusInput
                        )
                        if (editingWalletObj == null) {
                            viewModel.addPaymentWallet(wallet)
                        } else {
                            viewModel.updatePaymentWallet(wallet)
                        }
                        showAddWalletDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ المحفظة 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWalletDialog = false }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }

    // 1b. Internal Wallet Transaction Dialog
    if (showWalletTxDialog && selectedWalletForTx != null) {
        val wallet = selectedWalletForTx!!
        AlertDialog(
            onDismissRequest = { showWalletTxDialog = false },
            title = {
                Text("💸 معاملة محفظة داخلية (${wallet.ownerName})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("نوع الحساب: ${wallet.ownerType} | الرقم: ${wallet.ownerPhone}", fontSize = 11.sp, color = Color.LightGray)
                    Text("الرصيد الحالي: ${wallet.balance} ريال يمني 🇾🇪", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    Divider(color = Color.White.copy(alpha = 0.1f))

                    Text("اختر نوع العملية المالية:", fontSize = 11.sp, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { txTypeInput = "DEPOSIT" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (txTypeInput == "DEPOSIT") Color(0xFF10B981) else Color.DarkGray)
                        ) {
                            Text("إيداع ➕", fontSize = 10.sp)
                        }
                        Button(
                            onClick = { txTypeInput = "WITHDRAWAL" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (txTypeInput == "WITHDRAWAL") Color.Red else Color.DarkGray)
                        ) {
                            Text("سحب ➖", fontSize = 10.sp)
                        }
                        Button(
                            onClick = { txTypeInput = "TRANSFER" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (txTypeInput == "TRANSFER") themeColors.accent else Color.DarkGray)
                        ) {
                            Text("تحويل 🔄", fontSize = 10.sp, color = if (txTypeInput == "TRANSFER") Color.Black else Color.White)
                        }
                    }

                    OutlinedTextField(
                        value = txAmountInput,
                        onValueChange = { txAmountInput = it },
                        label = { Text("المبلغ بالريال اليمني", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = txNoteInput,
                        onValueChange = { txNoteInput = it },
                        label = { Text("ملاحظات/سبب العملية", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = txAmountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.performWalletTransaction(
                                walletId = wallet.id,
                                ownerName = wallet.ownerName,
                                ownerPhone = wallet.ownerPhone,
                                ownerType = wallet.ownerType,
                                type = txTypeInput,
                                amount = amt,
                                note = txNoteInput
                            )
                            showWalletTxDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("تنفيذ العملية ⚡", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWalletTxDialog = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }

    // 2. Verify Payment Dialog
    verifyingPaymentObj?.let { payment ->
        AlertDialog(
            onDismissRequest = { verifyingPaymentObj = null },
            title = { Text("✅ تأكيد واعتماد الدفع الوارد", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أنت على وشك اعتماد وتأكيد دفعة بقيمة ${payment.amount} ريال يمني والمحولة عبر ${payment.walletProvider}.", fontSize = 11.sp, color = Color.LightGray)
                    Text("سيعمل هذا التفعيل على تفعيل/اعتماد طلب الحجز المرتبط وتنشيط حساب المعاملة تلقائياً.", fontSize = 11.sp, color = themeColors.accent)
                    
                    OutlinedTextField(
                        value = adminVerifyPaymentNote,
                        onValueChange = { adminVerifyPaymentNote = it },
                        label = { Text("ملاحظة وتوضيح (اختياري)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.verifyPayment(payment.id, true, adminVerifyPaymentNote, "مدير الدليل 🛡️")
                        verifyingPaymentObj = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("اعتماد وتأكيد ✅", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { verifyingPaymentObj = null }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }

    // 3. Reject Payment Dialog
    rejectingPaymentObj?.let { payment ->
        AlertDialog(
            onDismissRequest = { rejectingPaymentObj = null },
            title = { Text("❌ رفض المعاملة وإثبات الدفع", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("يرجى توضيح سبب رفض إثبات التحويل للعميل:", fontSize = 11.sp, color = Color.LightGray)
                    
                    OutlinedTextField(
                        value = adminRejectPaymentNote,
                        onValueChange = { adminRejectPaymentNote = it },
                        label = { Text("سبب الرفض المالي", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.verifyPayment(payment.id, false, adminRejectPaymentNote, "مدير الدليل 🛡️")
                        rejectingPaymentObj = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد الرفض ❌", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingPaymentObj = null }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }

    // 4. Refund Payment Dialog
    refundingPaymentObj?.let { payment ->
        AlertDialog(
            onDismissRequest = { refundingPaymentObj = null },
            title = { Text("🔄 إرجاع واسترداد المبلغ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تنبيه: سيتم تسجيل هذه العملية كـ 'تم الاسترداد' وإلغاء فاعليتها المالية في إحصائيات الدليل.", fontSize = 11.sp, color = Color.LightGray)
                    Text("يرجى كتابة سبب الاسترداد للتسجيل والمتابعة:", fontSize = 11.sp, color = Color.LightGray)
                    
                    OutlinedTextField(
                        value = refundReasonInput,
                        onValueChange = { refundReasonInput = it },
                        label = { Text("سبب إرجاع المبلغ", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.refundPayment(payment.id, refundReasonInput.ifBlank { "استرداد بناء على طلب الأطراف" })
                        refundingPaymentObj = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                ) {
                    Text("تأكيد الاسترداد 🔄", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { refundingPaymentObj = null }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }

    /*
    // 5. User Submit Transfer Proof Dialog
    payingBookingObj?.let { booking ->
        Dialog(onDismissRequest = { payingBookingObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text("💳 سداد رسوم الحجز والخدمة بالمنصة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                        Text("يرجى اختيار أحد الحسابات / المحافظ التالية والتحويل إليها بقيمة تكلفة المعاينة والصيانة:", fontSize = 11.sp, color = Color.LightGray)
                    }

                    if (paymentWallets.isEmpty()) {
                        item {
                            Text("⚠️ عذراً، لا توجد محافظ دفع مفعلة حالياً بالمنصة للتسديد. يرجى مراجعة المشرفين.", fontSize = 11.sp, color = Color.Red)
                        }
                    } else {
                        item {
                            Text("المحافظ والحسابات المتاحة للتحويل:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(paymentWallets.filter { it.status == "active" }) { wallet ->
                                    val isSel = selectedUserWalletObj?.id == wallet.id
                                    val name = when (wallet.provider) {
                                        "jeeb" -> "جيب 📱"
                                        "alKarimi" -> "الكريمي 🏦"
                                        "jawaly" -> "جوالي 📲"
                                        "yemenMobile" -> "يمن كاش 🇾🇪"
                                        else -> wallet.accountNameAr.take(8)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSel) themeColors.accent else Color.DarkGray,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { selectedUserWalletObj = wallet }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(name, color = if (isSel) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        selectedUserWalletObj?.let { wallet ->
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("رقم الحساب/المحفظة للتحويل: ${wallet.walletNumber}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                        Text("اسم صاحب الحساب المستلم: ${wallet.accountNameAr}", fontSize = 11.sp, color = Color.White)
                                        if (wallet.description.isNotEmpty()) {
                                            Text("تعليمات: ${wallet.description}", fontSize = 10.sp, color = Color.LightGray)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Divider(color = Color.White.copy(alpha = 0.05f))
                            Text("يرجى تعبئة بيانات التحويل بعد إرسال المبلغ المالي:", fontSize = 11.sp, color = Color.LightGray)
                        }

                        item {
                            OutlinedTextField(
                                value = userTransferIdInput,
                                onValueChange = { userTransferIdInput = it },
                                label = { Text("رقم الحوالة المرجعي / رقم العملية (الـ ID)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = userTransferAccountNameInput,
                                onValueChange = { userTransferAccountNameInput = it },
                                label = { Text("اسم المرسل الكامل (صاحب المحفظة المحوِلة)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = userTransferPhotoInput,
                                onValueChange = { userTransferPhotoInput = it },
                                label = { Text("رابط صورة الإثبات أو لقطة الشاشة (اختياري)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (userTransferIdInput.isBlank() || userTransferAccountNameInput.isBlank()) {
                                            viewModel.triggerNotification("❌ يرجى ملء رقم الحوالة واسم مرسل الحوالة كاملاً")
                                            return@Button
                                        }
                                        val wallet = selectedUserWalletObj ?: return@Button
                                        
                                        val docRef = viewModel.db.collection("payments").document()
                                        val payment = PaymentEntity(
                                            id = docRef.id,
                                            userId = booking.customerPhone,
                                            providerId = booking.providerId,
                                            bookingId = booking.id,
                                            type = "service",
                                            method = "mobileWallet",
                                            status = "PROCESSING",
                                            amount = 1000.0,
                                            advanceAmount = 0.0,
                                            remainingAmount = 1000.0,
                                            commission = 0.0,
                                            providerShare = 1000.0,
                                            currency = "YER",
                                            isLinkedToBooking = true,
                                            bookingDate = System.currentTimeMillis(),
                                            bookingServiceType = booking.serviceType,
                                            createdAt = System.currentTimeMillis(),
                                            transferId = userTransferIdInput,
                                            transferPhoto = userTransferPhotoInput,
                                            walletProvider = wallet.provider,
                                            walletNumber = wallet.walletNumber,
                                            walletAccountName = userTransferAccountNameInput,
                                            updatedAt = System.currentTimeMillis()
                                        )
                                        viewModel.db.collection("payments").document(docRef.id).set(payment).addOnSuccessListener {
                                            viewModel.triggerNotification("✅ تم إرسال إثبات السداد بنجاح! بانتظار تأكيد الإدارة.")
                                        }.addOnFailureListener {
                                            viewModel.triggerNotification("❌ فشل إرسال الإثبات: ${it.message}")
                                        }
                                        
                                        payingBookingObj = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("إرسال الإثبات 📤", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { payingBookingObj = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("إلغاء", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    */

    // 8.5 Redirect Booking Dialog Control
    redirectingBookingObj?.let { booking ->
        val providersList by viewModel.providers.collectAsState()
        
        // Find the nearest provider dynamically
        val nearestProvider = remember(booking, providersList) {
            val userCoords = getAreaCoords(booking.customerArea)
            providersList.minByOrNull { tech ->
                val techCoords = getProviderCoords(tech)
                val dist = getDistance(userCoords.first, userCoords.second, techCoords.first, techCoords.second)
                dist
            }
        }

        var selectedTargetType by remember { mutableStateOf("SPECIFIC") } // SPECIFIC, ADMIN, NEAREST, OTHER
        var selectedOtherProviderId by remember { mutableStateOf("") }
        var dropdownExpanded by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { redirectingBookingObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.5.dp, themeColors.accent),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🔄 توجيه الحجز إلى مسؤول جديد", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    Text("العميل: ${booking.customerName} - الحي: ${booking.customerArea}", fontSize = 11.sp, color = themeColors.textSecondary)
                    Text("الفني الحالي: ${booking.providerName}", fontSize = 11.sp, color = themeColors.accent)
                    
                    Divider(color = Color.Gray.copy(alpha = 0.3f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 1. To Admin
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedTargetType = "ADMIN" }
                        ) {
                            RadioButton(selected = (selectedTargetType == "ADMIN"), onClick = { selectedTargetType = "ADMIN" })
                            Text("المدير (الأدمن) 👑", fontSize = 12.sp, color = Color.White)
                        }

                        // 2. To Current Provider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedTargetType = "SPECIFIC" }
                        ) {
                            RadioButton(selected = (selectedTargetType == "SPECIFIC"), onClick = { selectedTargetType = "SPECIFIC" })
                            Text("الفني المذكور: ${booking.providerName} 👷", fontSize = 12.sp, color = Color.White)
                        }

                        // 3. To Nearest Provider
                        nearestProvider?.let { np ->
                            val userCoords = getAreaCoords(booking.customerArea)
                            val techCoords = getProviderCoords(np)
                            val distance = getDistance(userCoords.first, userCoords.second, techCoords.first, techCoords.second)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { selectedTargetType = "NEAREST" }
                            ) {
                                RadioButton(selected = (selectedTargetType == "NEAREST"), onClick = { selectedTargetType = "NEAREST" })
                                Text("الفني الأقرب للمستخدم: ${np.name} (يبعد ${String.format("%.1f", distance)} كم) 📍", fontSize = 12.sp, color = Color.White)
                            }
                        }

                        // 4. To another technician
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedTargetType = "OTHER" }
                        ) {
                            RadioButton(selected = (selectedTargetType == "OTHER"), onClick = { selectedTargetType = "OTHER" })
                            Text("توجيه لفني آخر من الدليل 🔎", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    if (selectedTargetType == "OTHER") {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val selectedProvName = providersList.find { it.id == selectedOtherProviderId }?.name ?: "اختر الفني الآخر..."
                            Button(
                                onClick = { dropdownExpanded = true },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(selectedProvName, color = Color.White, fontSize = 11.sp)
                            }

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.background(Color(0xFF1E293B)).fillMaxWidth(0.8f)
                            ) {
                                providersList.forEach { prov ->
                                    DropdownMenuItem(
                                        text = { Text(prov.name + " (${prov.customCategoryName.ifBlank { "فني" }})", color = Color.White, fontSize = 11.sp) },
                                        onClick = {
                                            selectedOtherProviderId = prov.id
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val targetProvider = when (selectedTargetType) {
                                    "ADMIN" -> Pair("admin", "إدارة الدليل")
                                    "SPECIFIC" -> Pair(booking.providerId, booking.providerName)
                                    "NEAREST" -> Pair(nearestProvider?.id ?: "admin", nearestProvider?.name ?: "إدارة الدليل")
                                    "OTHER" -> {
                                        val op = providersList.find { it.id == selectedOtherProviderId }
                                        Pair(op?.id ?: "admin", op?.name ?: "إدارة الدليل")
                                    }
                                    else -> Pair(booking.providerId, booking.providerName)
                                }

                                val updatedB = booking.copy(
                                    providerId = targetProvider.first,
                                    providerName = targetProvider.second
                                )
                                viewModel.updateBooking(updatedB)

                                // Send notifications to user about stages
                                viewModel.addNotification(
                                    title = "🔄 تم تعديل وتوجيه حجزك",
                                    message = "عزيزي العميل، تم توجيه حجزك رقم ${booking.id} بنجاح ومسؤولية تنفيذ الخدمة انتقلت إلى: ${targetProvider.second}.",
                                    targetType = "USER",
                                    targetValue = booking.customerPhone
                                )

                                // Send notifications to the target provider as well
                                if (targetProvider.first != "admin") {
                                    val targetPhone = providersList.find { it.id == targetProvider.first }?.phone ?: ""
                                    viewModel.addNotification(
                                        title = "📅 تم توجيه حجز جديد لك",
                                        message = "تم توجيه حجز جديد لك من الإدارة.\nالعميل: ${booking.customerName}\nالهاتف: ${booking.customerPhone}\nالمنطقة: ${booking.customerArea}\nالخدمة: ${booking.serviceType}\nيرجى التواصل معه مباشرة للتنسيق.",
                                        targetType = "PROVIDER",
                                        targetValue = targetPhone,
                                        customerPhone = booking.customerPhone,
                                        customerName = booking.customerName
                                    )
                                }

                                redirectingBookingObj = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("توجيه 🔄", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { redirectingBookingObj = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // 9. Editing Supervisor Dialog Control
    editingSupervisorObj?.let { supervisor ->
        var editSupName by rememberSaveable(supervisor.id) { mutableStateOf(supervisor.name) }
        var editSupRole by rememberSaveable(supervisor.id) { mutableStateOf(supervisor.role) }
        var editSupPasscode by rememberSaveable(supervisor.id) { mutableStateOf(supervisor.passcode) }

        Dialog(onDismissRequest = { editingSupervisorObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("✏️ تعديل صلاحيات وبيانات المشرف", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    OutlinedTextField(
                        value = editSupName,
                        onValueChange = { editSupName = it },
                        label = { Text("اسم المشرف الثلاثي") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editSupPasscode,
                        onValueChange = { editSupPasscode = it },
                        label = { Text("رمز المرور والدخول (Passcode)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Text("اختر الأدوار والصلاحيات الأمنية للمشرف (يمكنك اختيار صلاحية واحدة أو أكثر):", color = themeColors.textSecondary, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val roles = listOf("ADMIN", "AUDITOR", "SUPPORT", "OPERATIONS")
                        roles.forEach { rl ->
                            val isSel = editSupRole.split(",").contains(rl)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) themeColors.accent else Color.Black.copy(alpha = 0.3f))
                                    .clickable {
                                        val currentSelected = editSupRole.split(",").filter { it.isNotEmpty() }.toMutableList()
                                        if (currentSelected.contains(rl)) {
                                            currentSelected.remove(rl)
                                        } else {
                                            currentSelected.add(rl)
                                        }
                                        if (currentSelected.isEmpty()) {
                                            currentSelected.add("SUPPORT")
                                        }
                                        editSupRole = currentSelected.joinToString(",")
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (rl) {
                                        "ADMIN" -> "مدير 👑"
                                        "AUDITOR" -> "مدقق 🔍"
                                        "OPERATIONS" -> "ميداني 🚗"
                                        else -> "دعم 📞"
                                    },
                                    color = if (isSel) Color.Black else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (editSupName.trim().isNotEmpty() && editSupPasscode.trim().isNotEmpty()) {
                                    viewModel.editSupervisor(supervisor.id, editSupName.trim(), editSupRole, editSupPasscode.trim())
                                    editingSupervisorObj = null
                                } else {
                                    viewModel.triggerNotification("⚠️ يرجى كتابة الاسم والرمز بالكامل")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💾 حفظ التعديل", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { editingSupervisorObj = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    selectedRecoveryNotif?.let { notif ->
        var phoneExtract = ""
        var nameExtract = "المستخدم"
        try {
            val phoneRegex = Regex("(?:هاتف:|الرقم:|الهاتف:)?\\s*([0-9]{9,10})")
            val match = phoneRegex.find(notif.message)
            if (match != null) {
                phoneExtract = match.groupValues[1]
            }
            if (notif.message.contains("المتجر")) {
                val split = notif.message.split("المتجر")
                if (split.size > 1) {
                    nameExtract = split[1].substringBefore("(").trim()
                }
            } else if (notif.message.contains("الحساب:")) {
                val split = notif.message.split("الحساب:")
                if (split.size > 1) {
                    nameExtract = split[1].substringBefore("(").trim()
                }
            }
        } catch (e: Exception) {}

        var newPasswordInput by remember { mutableStateOf("123456") }
        var notifyActionChoice by remember { mutableStateOf("DIRECT_PASSWORD") }

        AlertDialog(
            onDismissRequest = { selectedRecoveryNotif = null },
            containerColor = Color(0xFF0F172A),
            title = { Text("🔓 إدارة استعادة كلمة المرور للحساب", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("تفاصيل الطلب: ${notif.message}", fontSize = 11.sp, color = Color.White)
                    
                    OutlinedTextField(
                        value = phoneExtract,
                        onValueChange = { phoneExtract = it },
                        label = { Text("رقم الهاتف المستهدف") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("كلمة المرور الجديدة (أو الحالية)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Text("اختر الإجراء الإداري للرد والاشعار:", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { notifyActionChoice = "DIRECT_PASSWORD" }) {
                        RadioButton(selected = notifyActionChoice == "DIRECT_PASSWORD", onClick = { notifyActionChoice = "DIRECT_PASSWORD" })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إعادة تعيين كلمة المرور وإرسالها بإشعار مباشر للمستخدم", fontSize = 10.sp, color = Color.White)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { notifyActionChoice = "VERIFICATION_WHATSAPP" }) {
                        RadioButton(selected = notifyActionChoice == "VERIFICATION_WHATSAPP", onClick = { notifyActionChoice = "VERIFICATION_WHATSAPP" })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("طلب التحقق عبر الواتس / التليجرام أو المحادثة الفورية", fontSize = 10.sp, color = Color.White)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { notifyActionChoice = "INSTANT_CHAT" }) {
                        RadioButton(selected = notifyActionChoice == "INSTANT_CHAT", onClick = { notifyActionChoice = "INSTANT_CHAT" })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فتح محادثة فورية مباشرة مع المستخدم لإثبات الهوية", fontSize = 10.sp, color = Color.White)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (phoneExtract.isNotEmpty() && newPasswordInput.isNotEmpty()) {
                            viewModel.adminResetAccountPassword(
                                phone = phoneExtract,
                                newPassword = newPasswordInput.trim(),
                                notifyAction = notifyActionChoice,
                                customerName = nameExtract
                            )
                            if (notifyActionChoice == "INSTANT_CHAT") {
                                viewModel.getOrCreateChatChannel(
                                    providerId = "admin",
                                    providerName = "الإدارة والدعم",
                                    customerId = phoneExtract,
                                    customerName = nameExtract
                                )
                                Toast.makeText(context, "💬 تم فتح محادثة الدعم وإرسال الإجراء بنجاح!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "✅ تم إعادة تعيين كلمة المرور وإرسال الإشعار بنجاح!", Toast.LENGTH_LONG).show()
                            }
                            selectedRecoveryNotif = null
                        } else {
                            Toast.makeText(context, "الرجاء التأكد من رقم الهاتف وكلمة المرور", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("تنفيذ الإجراء وإرسال 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                Button(
                    onClick = { selectedRecoveryNotif = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("إلغاء", color = Color.White, fontSize = 11.sp)
                }
            }
        )
    }
}
}
}

@Composable
fun PasswordEntityCard(
    name: String,
    phone: String,
    category: String,
    password: String?,
    onResetPassword: (String) -> Unit,
    context: android.content.Context
) {
    var editPass by remember { mutableStateOf(password ?: "") }
    var showPass by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("القسم: $category", fontSize = 10.sp, color = Color(0xFF3B82F6))
            }
            Text("رقم الهاتف: $phone", fontSize = 11.sp, color = Color.LightGray)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("🔑 كلمة المرور الحالية: ${if (showPass) (password ?: "غير متوفرة") else "••••••••"}", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                TextButton(onClick = { showPass = !showPass }) {
                    Text(if (showPass) "إخفاء" else "إظهار", fontSize = 10.sp, color = Color.Yellow)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = editPass,
                    onValueChange = { editPass = it },
                    label = { Text("كلمة مرور جديدة", fontSize = 9.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Button(
                    onClick = {
                        if (editPass.isNotBlank()) {
                            onResetPassword(editPass)
                        } else {
                            Toast.makeText(context, "الرجاء إدخال كلمة المرور الجديدة", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("تحديث 🔒", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = {
                        val whatsappText = "مرحباً يا غالي، كلمة المرور الخاصة بحسابك في دليل خدمات اليمن هي: ${password ?: "غير متوفرة"}"
                        val whatsappUrl = "https://wa.me/967${phone.trim().removePrefix("0").removePrefix("+967")}?text=${android.net.Uri.encode(whatsappText)}"
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(whatsappUrl))
                            context.startActivity(intent)
                        } catch(e: Exception) {
                            Toast.makeText(context, "فشل فتح واتساب", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Text("🟢 واتساب", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val smsText = "كلمة المرور الخاصة بحسابك في دليل خدمات اليمن هي: ${password ?: "غير متوفرة"}"
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:$phone")).apply {
                                putExtra("sms_body", smsText)
                            }
                            context.startActivity(intent)
                        } catch(e: Exception) {
                            Toast.makeText(context, "فشل فتح SMS", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Text("💬 رسالة SMS", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
