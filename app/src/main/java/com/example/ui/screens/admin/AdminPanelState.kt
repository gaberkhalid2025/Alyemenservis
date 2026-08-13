package com.example.ui.screens.admin

import androidx.compose.runtime.MutableState
import com.example.data.*
import com.example.data.models.*

/**
 * كائن الحالة الأساسي (AdminPanelState) الموحد لإدارة جميع حالات لوحة التحكم الإدارية.
 */
data class AdminPanelState(
    // ==========================================
    // الفئة الأولى: متغيرات التحكم العامة (5 متغيرات)
    // ==========================================
    val activeSubTabState: MutableState<String>,
    val inputPasscodeState: MutableState<String>,
    val isAuthorizedState: MutableState<Boolean>,
    val adminReqSubTabState: MutableState<String>,
    val adminBookingSubTabState: MutableState<String>,

    // ==========================================
    // الفئة الثانية: متغيرات طلبات الانضمام والفنيين (8 متغيرات)
    // ==========================================
    val rejectingProviderRequestState: MutableState<PendingProviderEntity?>,
    val providerRejectionReasonTextState: MutableState<String>,
    val showDeleteCategoryConfirmIdState: MutableState<String?>,
    val showEditCategoryObjState: MutableState<CategoryEntity?>,
    val showEditCityObjState: MutableState<CityEntity?>,
    val newCatNameState: MutableState<String>,
    val newCatIconState: MutableState<String>,
    val categoryManagementModeState: MutableState<String>,

    // ==========================================
    // الفئة الثالثة: متغيرات الحجوزات (5 متغيرات - مع دمج المكرر)
    // ==========================================
    val showDeleteBookingConfirmIdState: MutableState<String?>,
    val showRejectionReasonDialogIdState: MutableState<String?>,
    val bookingRejectionReasonInputState: MutableState<String>,
    val editingBookingObjState: MutableState<BookingEntity?>,
    val redirectingBookingObjState: MutableState<BookingEntity?>,

    // ==========================================
    // الفئة الرابعة: متغيرات المحادثات (4 متغيرات - مع دمج المكرر)
    // ==========================================
    val showActiveChatChannelObjState: MutableState<ChatChannelEntity?>,
    val adminChatReplyInputState: MutableState<String>,
    val showDeleteChatConfirmIdState: MutableState<String?>,
    val adminChatSubTabState: MutableState<String>,

    // ==========================================
    // الفئة الخامسة: متغيرات الإشعارات (8 متغيرات)
    // ==========================================
    val showDeleteNotifConfirmIdState: MutableState<String?>,
    val selectedRecoveryNotifState: MutableState<NotificationEntity?>,
    val notifTitleInputState: MutableState<String>,
    val notifMsgInputState: MutableState<String>,
    val notifTargetTypeState: MutableState<String>,
    val notifTargetValueState: MutableState<String>,
    val notifDelayHoursState: MutableState<String>,
    val notifValidityHoursState: MutableState<String>,

    // ==========================================
    // الفئة السادسة: متغيرات الألوان والإعدادات (10 متغيرات - مع دمج المكررات)
    // ==========================================
    val editPrimaryHexState: MutableState<String>,
    val editSecondaryHexState: MutableState<String>,
    val editCardBgHexState: MutableState<String>,
    val editProviderNameHexState: MutableState<String>,
    val editLocationHexState: MutableState<String>,
    val editRatingHexState: MutableState<String>,
    val editVipBadgeHexState: MutableState<String>,
    val editVerifiedHexState: MutableState<String>,
    val editRecommendedHexState: MutableState<String>,
    val editFontSelectedState: MutableState<String>,

    // ==========================================
    // الفئة السابعة: متغيرات الحذف والمسح (13 متغيراً)
    // ==========================================
    val showWipeConfirmDialogState: MutableState<Boolean>,
    val wipeInputPasswordState: MutableState<String>,
    val wipeProvidersCheckedState: MutableState<Boolean>,
    val wipeBookingsCheckedState: MutableState<Boolean>,
    val wipeChatsCheckedState: MutableState<Boolean>,
    val wipeNotifsCheckedState: MutableState<Boolean>,
    val wipeReportsCheckedState: MutableState<Boolean>,
    val wipeCategoriesCheckedState: MutableState<Boolean>,
    val wipePendingCheckedState: MutableState<Boolean>,
    val wipeBannersCheckedState: MutableState<Boolean>,
    val wipeSupervisorsCheckedState: MutableState<Boolean>,
    val wipeCitiesCheckedState: MutableState<Boolean>,
    val wipeThemesCheckedState: MutableState<Boolean>,

    // ==========================================
    // الفئة الثامنة: متغيرات الإضافة اليدوية (11 متغيراً)
    // ==========================================
    val manualNameState: MutableState<String>,
    val manualPhoneState: MutableState<String>,
    val manualCategoryIdState: MutableState<String>,
    val manualStreetState: MutableState<String>,
    val manualCityIdState: MutableState<String>,
    val manualPhotoUrlState: MutableState<String>,
    val manualIdCardUrlState: MutableState<String>,
    val manualForensicUrlState: MutableState<String>,
    val manualPriceValueState: MutableState<String>,
    val manualIsVipGoldenState: MutableState<Boolean>,
    val adminAddSubTabState: MutableState<String>,

    // ==========================================
    // الفئة التاسعة: متغيرات المدن (3 متغيرات)
    // ==========================================
    val newCityArNameState: MutableState<String>,
    val newCityEnNameState: MutableState<String>,
    val newCityIconState: MutableState<String>,

    // ==========================================
    // الفئة العاشرة: متغيرات البحث والفلترة (9 متغيرات)
    // ==========================================
    val complaintsSearchQueryState: MutableState<String>,
    val activeProvidersSearchQueryState: MutableState<String>,
    val activeJobsSearchQueryState: MutableState<String>,
    val storesSearchQueryState: MutableState<String>,
    val restaurantsSearchQueryState: MutableState<String>,
    val medicalSearchQueryState: MutableState<String>,
    val propertiesSearchQueryState: MutableState<String>,
    val applicantsSearchQueryState: MutableState<String>,
    val adminReviewSubTabState: MutableState<String>,

    // ==========================================
    // الفئة الحادية عشرة: متغيرات المشرفين (4 متغيرات)
    // ==========================================
    val editingSupervisorObjState: MutableState<SupervisorEntity?>,
    val supervisorInputNameState: MutableState<String>,
    val supervisorInputRoleState: MutableState<String>,
    val supervisorInputPasscodeState: MutableState<String>,

    // ==========================================
    // الفئة الثانية عشرة: متغيرات التصدير والنسخ الاحتياطي (4 متغيرات)
    // ==========================================
    val backupJsonStringState: MutableState<String>,
    val restoreJsonInputState: MutableState<String>,
    val showExportReportPasswordDialogState: MutableState<Boolean>,
    val exportReportPasswordInputState: MutableState<String>,

    // ==========================================
    // الفئة الثالثة عشرة: متغيرات أبعاد وتصميم البطاقات (12 متغيراً)
    // ==========================================
    val editCoverHeightState: MutableState<Float>,
    val editAvatarSizeState: MutableState<Float>,
    val editElementSpacingState: MutableState<Float>,
    val editCardPaddingState: MutableState<Float>,
    val editChatIconSizeState: MutableState<Float>,
    val editChatIconXState: MutableState<Float>,
    val editChatIconYState: MutableState<Float>,
    val editAssistantIconSizeState: MutableState<Float>,
    val editAssistantIconXState: MutableState<Float>,
    val editAssistantIconYState: MutableState<Float>,
    val elementSpacingPaddingState: MutableState<Float>,
    val containerCardPaddingState: MutableState<Float>,

    // ==========================================
    // الفئة الرابعة عشرة: متغيرات الأزرار والشارات (12 متغيراً)
    // ==========================================
    val editShowVipBadgeState: MutableState<Boolean>,
    val editShowVerifiedBadgeState: MutableState<Boolean>,
    val editShowRecommendedBadgeState: MutableState<Boolean>,
    val editShowCallButtonState: MutableState<Boolean>,
    val editShowWhatsappButtonState: MutableState<Boolean>,
    val editShowDetailsButtonState: MutableState<Boolean>,
    val editShowBookButtonState: MutableState<Boolean>,
    val editCallButtonColorHexState: MutableState<String>,
    val editWhatsappButtonColorHexState: MutableState<String>,
    val editDetailsButtonColorHexState: MutableState<String>,
    val editBookButtonColorHexState: MutableState<String>,
    val editShowLoyaltyBannerState: MutableState<Boolean>,

    // ==========================================
    // الفئة الخامسة عشرة: متغيرات متنوعة (6 متغيرات)
    // ==========================================
    val requirementItemInputState: MutableState<String>,
    val isNewRequirementMandatoryState: MutableState<Boolean>,
    val requirementsListStateState: MutableState<List<String>>,
    val editMaxWorkPhotosState: MutableState<Float>,
    val adminVipSubTabState: MutableState<String>,
    val adminPasswordSubTabState: MutableState<String>,

    // ==========================================
    // متغيرات إضافية للتوافق والتشغيل السلس
    // ==========================================
    val adminNotifSubTabState: MutableState<String>,
    val adminBannerSubTabState: MutableState<String>,
    val editCatNameState: MutableState<String>,
    val editCatIconState: MutableState<String>,
    val showEditProviderMetadataObjState: MutableState<ProviderEntity?>,
    val editProviderPhoneState: MutableState<String>,
    val editProviderCategoryIdState: MutableState<String>
)
