package com.example.ui.viewmodels

import android.content.Context
import com.example.data.*
import com.example.ui.helpers.AppState
import com.example.utils.Conflict
import com.example.utils.Resolution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ⚙️ SettingsViewModel (Facade)
 * المنسّق الرئيسي لإعدادات التطبيق والمظهر والنسخ الاحتياطي واستعادة كلمات المرور:
 * 1. [SettingsThemeAndColorSyncDelegate] إعدادات البطاقات، السمات، مزامنة الألوان السحابية، ومصفوفة الصلاحيات.
 * 2. [SettingsBackupAndReportsDelegate] تصدير CSV/PDF والنسخ الاحتياطي الشامل/الانتقائي وإعداد Firebase الثانوي.
 * 3. [SettingsPasswordRecoveryAndCleanupDelegate] إعادة تعيين واستعادة كلمات المرور، التطهير التام والانتقائي، والتنظيف التلقائي.
 * 4. [SettingsCommsAndSyncUtilsDelegate] صلاحيات المشاركين بالمحادثة، المكالمات الصوتية، ومزامنة الطابور المحلي.
 */
class SettingsViewModel @Inject constructor(
    val appState: AppState
) : BaseViewModel() {

    // --- Callback/Lambda Properties for decoupling ---
    var getAuthViewModel: (() -> AuthViewModel)? = null
    var getHomeViewModel: (() -> HomeViewModel)? = null
    var getBookingViewModel: (() -> BookingViewModel)? = null
    var getAdminViewModel: (() -> AdminViewModel)? = null

    var getProviders: (() -> MutableStateFlow<List<ProviderEntity>>)? = null
    var getBookings: (() -> MutableStateFlow<List<BookingEntity>>)? = null
    var getCategories: (() -> MutableStateFlow<List<CategoryEntity>>)? = null
    var getStores: (() -> MutableStateFlow<List<StoreEntity>>)? = null
    var getProperties: (() -> MutableStateFlow<List<PropertyEntity>>)? = null

    var getPasswordRecoveryWaitingPhone: (() -> MutableStateFlow<String>)? = null
    var setPasswordRecoveryWaitingPhone: ((String) -> Unit)? = null
    var verifyAdminOrOwnerPassword: ((String) -> Boolean)? = null
    var triggerNotification: ((String) -> Unit)? = null

    inner class MainViewModelDelegate {
        val authViewModel get() = getAuthViewModel?.invoke() ?: throw IllegalStateException("authViewModel not provided")
        val homeViewModel get() = getHomeViewModel?.invoke() ?: throw IllegalStateException("homeViewModel not provided")
        val bookingViewModel get() = getBookingViewModel?.invoke() ?: throw IllegalStateException("bookingViewModel not provided")
        val adminViewModel get() = getAdminViewModel?.invoke() ?: throw IllegalStateException("adminViewModel not provided")

        val _providers get() = getProviders?.invoke() ?: throw IllegalStateException("_providers not provided")
        val _bookings get() = getBookings?.invoke() ?: throw IllegalStateException("_bookings not provided")
        val _categories get() = getCategories?.invoke() ?: throw IllegalStateException("_categories not provided")
        val _stores get() = getStores?.invoke() ?: throw IllegalStateException("_stores not provided")
        val _properties get() = getProperties?.invoke() ?: throw IllegalStateException("_properties not provided")

        val _passwordRecoveryWaitingPhone get() = getPasswordRecoveryWaitingPhone?.invoke()
            ?: throw IllegalStateException("_passwordRecoveryWaitingPhone not provided")

        fun setPasswordRecoveryWaitingPhone(phone: String) {
            this@SettingsViewModel.setPasswordRecoveryWaitingPhone?.invoke(phone)
        }

        fun verifyAdminOrOwnerPassword(password: String): Boolean {
            return this@SettingsViewModel.verifyAdminOrOwnerPassword?.invoke(password) ?: false
        }

        fun triggerNotification(msg: String) {
            this@SettingsViewModel.triggerNotification?.invoke(msg)
        }

        fun requestPasswordRecovery(phone: String, name: String, accountType: String) {
            getAuthViewModel?.invoke()?.requestPasswordRecovery(phone, name, accountType)
        }
    }

    val mainViewModel = MainViewModelDelegate()

    data class CardSettings(
        val cardHeight: Int = 180,
        val cardWidth: Int = 360,
        val cornerRadius: Int = 12,
        val backgroundColor: String = "#162A2D",
        val nameColor: String = "#FFFFFF",
        val ratingColor: String = "#FFD700",
        val locationColor: String = "#A0B2B5",
        val priceColor: String = "#4CAF50",
        val showVipBadge: Boolean = true,
        val showVerifiedBadge: Boolean = true,
        val showRecommendedBadge: Boolean = true,
        val vipColor: String = "#FFD700",
        val verifiedColor: String = "#2196F3",
        val recommendedColor: String = "#FF6B6B",
        val showCallButton: Boolean = true,
        val showWhatsAppButton: Boolean = true,
        val showDetailsButton: Boolean = true,
        val showBookingButton: Boolean = true,
        val callButtonColor: String = "#CE1126",
        val whatsappColor: String = "#25D366",
        val detailsColor: String = "#0D47A1",
        val bookingColor: String = "#E65100",
        val showDistance: Boolean = true,
        val showPrice: Boolean = true,
        val showAvailability: Boolean = true,
        val showRatingCount: Boolean = true,
        val imageShape: String = "circle",
        val spacing: Int = 8,
        val padding: Int = 12,
        val scaleAnimation: Boolean = true,
        val scaleFactor: Float = 0.95f
    )

    enum class ChatParticipantType {
        VISITOR,    // زائر
        PROVIDER,   // مقدم خدمة
        ADMIN,      // مشرف
        ALL         // الجميع
    }

    // --- StateFlows ---
    internal val _cardSettings = MutableStateFlow(CardSettings())
    val cardSettings: StateFlow<CardSettings> = _cardSettings.asStateFlow()

    internal val _settings get() = appState._settings
    val settings: StateFlow<AdminSettingsEntity> = _settings.asStateFlow()
    internal val _colorScheme = MutableStateFlow(ColorSchemeEntity())
    val colorScheme: StateFlow<ColorSchemeEntity> = _colorScheme.asStateFlow()
    internal val _personalColors = MutableStateFlow(UserColorsEntity())
    val personalColors: StateFlow<UserColorsEntity> = _personalColors.asStateFlow()
    internal val _colorSyncStatus = MutableStateFlow(ColorSyncStatus.SYNCED)
    val colorSyncStatus: StateFlow<ColorSyncStatus> = _colorSyncStatus.asStateFlow()
    internal val _colorSyncLogs = MutableStateFlow<List<SyncLogEntity>>(emptyList())
    val colorSyncLogs: StateFlow<List<SyncLogEntity>> = _colorSyncLogs.asStateFlow()
    internal val _pendingConflictScheme = MutableStateFlow<ColorSchemeEntity?>(null)
    val pendingConflictScheme: StateFlow<ColorSchemeEntity?> = _pendingConflictScheme.asStateFlow()
    internal val _blockedChatParticipants = MutableStateFlow<Set<ChatParticipantType>>(emptySet())
    val blockedChatParticipants: StateFlow<Set<ChatParticipantType>> = _blockedChatParticipants.asStateFlow()
    internal val _activeVoiceCall = MutableStateFlow<Pair<String, String>?>(null)
    val activeVoiceCall: StateFlow<Pair<String, String>?> = _activeVoiceCall.asStateFlow()

    internal fun runSafeFirestoreCallback(
        operation: (onSuccess: () -> Unit, onFailure: (Exception) -> Unit) -> Unit,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {},
        errorMessage: String = "حدث خطأ أثناء تنفيذ العملية"
    ) {
        safeFirestoreCallWithCallback(operation, onSuccess, onError, errorMessage)
    }

    // --- Cohesive Delegates ---
    private val themeSyncDelegate = SettingsThemeAndColorSyncDelegate(this)
    private val backupAndReportsDelegate = SettingsBackupAndReportsDelegate(this)
    private val recoveryAndCleanupDelegate = SettingsPasswordRecoveryAndCleanupDelegate(this)
    private val commsAndSyncDelegate = SettingsCommsAndSyncUtilsDelegate(this)

    // ==========================================
    // 🎨 الإعدادات والسمات ومزامنة الألوان
    // ==========================================

    fun updateAppLanguage(lang: String) = themeSyncDelegate.updateAppLanguage(lang)

    fun updateCardSettings(settings: CardSettings) = themeSyncDelegate.updateCardSettings(settings)

    fun loadCardSettings() = themeSyncDelegate.loadCardSettings()

    fun updateTheme(themeId: String) = themeSyncDelegate.updateTheme(themeId)

    fun saveCustomSettingsState(newSettings: AdminSettingsEntity) =
        themeSyncDelegate.saveCustomSettingsState(newSettings)

    fun updatePaymentEnabled(enabled: Boolean) = themeSyncDelegate.updatePaymentEnabled(enabled)

    fun updateBackdoorSettings(
        appName: String, welcomeMsg: String, footerMsg: String, themeId: String,
        supportPhone: String, supportEmail: String, supportWhatsapp: String,
        isMaintenance: Boolean, hiddenFooter: Boolean, botHidden: Boolean, botSize: Int,
        chatHidden: Boolean, chatSize: Int, radiusKm: Int, isSpeech: Boolean,
        isDataSaver: Boolean, imgQuality: Int,
        bookingTerms: String = "يرجى الالتزام التام بالمواعيد المحددة والتسعيرة المتفق عليها مع الفني.",
        bookingLabelName: String = "الاسم الكامل للعميل",
        bookingLabelPhone: String = "رقم هاتف العميل للتواصل (مثال: 777000111)",
        bookingLabelArea: String = "المنطقة والحي السكني",
        bookingLabelService: String = "تفاصيل ونوع الخدمة المطلوبة",
        adminUsername: String = "",
        adminPassword: String = "",
        customPrimaryHex: String = "#059669",
        customSecondaryHex: String = "#115E59",
        customBackgroundHex: String = "#0A0F0D",
        customSurfaceHex: String = "#121D18"
    ) = themeSyncDelegate.updateBackdoorSettings(
        appName, welcomeMsg, footerMsg, themeId, supportPhone, supportEmail, supportWhatsapp,
        isMaintenance, hiddenFooter, botHidden, botSize, chatHidden, chatSize, radiusKm, isSpeech,
        isDataSaver, imgQuality, bookingTerms, bookingLabelName, bookingLabelPhone, bookingLabelArea,
        bookingLabelService, adminUsername, adminPassword, customPrimaryHex, customSecondaryHex,
        customBackgroundHex, customSurfaceHex
    )

    fun updateAdminSettings(newSettings: AdminSettingsEntity) =
        themeSyncDelegate.updateAdminSettings(newSettings)

    fun initColorSync(context: Context) = themeSyncDelegate.initColorSync(context)

    fun updateCloudColorScheme(context: Context, newScheme: ColorSchemeEntity) =
        themeSyncDelegate.updateCloudColorScheme(context, newScheme)

    fun updatePersonalColors(context: Context, personal: PersonalColors) =
        themeSyncDelegate.updatePersonalColors(context, personal)

    fun triggerManualSync(context: Context) = themeSyncDelegate.triggerManualSync(context)

    fun resolveConflict(context: Context, useCloud: Boolean) =
        themeSyncDelegate.resolveConflict(context, useCloud)

    fun getCurrentTimestampString(): String = themeSyncDelegate.getCurrentTimestampString()

    fun addNewSyncLog(
        context: Context,
        type: String,
        status: String,
        changes: List<String>,
        versionFrom: Int,
        versionTo: Int
    ) = themeSyncDelegate.addNewSyncLog(context, type, status, changes, versionFrom, versionTo)

    fun saveCustomPermissionsMatrixToFirestore(permissions: List<String>, role: String = "ADMIN") =
        themeSyncDelegate.saveCustomPermissionsMatrixToFirestore(permissions, role)

    fun addColorPalette(
        name: String,
        primaryHex: String,
        secondaryHex: String,
        backgroundHex: String = "#0A0F0D",
        surfaceHex: String = "#121D18"
    ) = themeSyncDelegate.addColorPalette(name, primaryHex, secondaryHex, backgroundHex, surfaceHex)

    fun updateColorPalette(
        id: String,
        name: String,
        primaryHex: String,
        secondaryHex: String,
        backgroundHex: String = "#0A0F0D",
        surfaceHex: String = "#121D18"
    ) = themeSyncDelegate.updateColorPalette(id, name, primaryHex, secondaryHex, backgroundHex, surfaceHex)

    fun deleteColorPalette(id: String) = themeSyncDelegate.deleteColorPalette(id)

    // ==========================================
    // 💬 التواصل والصلاحيات والمزامنة العامة
    // ==========================================

    fun toggleChatParticipant(participantType: ChatParticipantType) =
        commsAndSyncDelegate.toggleChatParticipant(participantType)

    fun isChatBlockedFor(participantType: ChatParticipantType): Boolean =
        commsAndSyncDelegate.isChatBlockedFor(participantType)

    fun canParticipateInChat(participantType: ChatParticipantType): Boolean =
        commsAndSyncDelegate.canParticipateInChat(participantType)

    fun startVoiceCall(name: String, role: String) = commsAndSyncDelegate.startVoiceCall(name, role)

    fun endVoiceCall() = commsAndSyncDelegate.endVoiceCall()

    fun triggerManualSync(context: Context, onComplete: ((Boolean) -> Unit)? = null) =
        commsAndSyncDelegate.triggerManualSync(context, onComplete)

    fun resolveConflict(context: Context, conflict: Conflict, resolution: Resolution) =
        commsAndSyncDelegate.resolveConflict(context, conflict, resolution)

    fun retryOfflineQueue(context: Context) = commsAndSyncDelegate.retryOfflineQueue(context)

    // ==========================================
    // 📊 التقارير والنسخ الاحتياطي والسحابة
    // ==========================================

    fun exportComplaintsToCSV() = backupAndReportsDelegate.exportComplaintsToCSV()

    fun exportComplaintsToPDF() = backupAndReportsDelegate.exportComplaintsToPDF()

    fun exportPerformanceReportToPDF() = backupAndReportsDelegate.exportPerformanceReportToPDF()

    fun createSystemBackup(onComplete: (Boolean, String) -> Unit) =
        backupAndReportsDelegate.createSystemBackup(onComplete)

    fun restoreSystemFromBackup(jsonStr: String, onComplete: (Boolean, String) -> Unit) =
        backupAndReportsDelegate.restoreSystemFromBackup(jsonStr, onComplete)

    fun exportSelectedCollectionsAsJson(selectedCollections: List<String>, onResult: (String) -> Unit) =
        backupAndReportsDelegate.exportSelectedCollectionsAsJson(selectedCollections, onResult)

    fun saveBackupToLocalStorage(context: Context, jsonStr: String, fileName: String): String =
        backupAndReportsDelegate.saveBackupToLocalStorage(context, jsonStr, fileName)

    fun setSecondaryFirebaseConfig(
        projectId: String,
        apiKey: String,
        appId: String,
        storageBucket: String,
        isEnabled: Boolean
    ) = backupAndReportsDelegate.setSecondaryFirebaseConfig(projectId, apiKey, appId, storageBucket, isEnabled)

    // ==========================================
    // 🔑 كلمات المرور والتطهير والتنظيف التلقائي
    // ==========================================

    fun resetAccountPassword(entityType: String, phoneOrId: String, newPass: String) =
        recoveryAndCleanupDelegate.resetAccountPassword(entityType, phoneOrId, newPass)

    fun requestAdminPasswordReset(phone: String) =
        recoveryAndCleanupDelegate.requestAdminPasswordReset(phone)

    fun requestPasswordReset(phone: String, onResult: (Boolean, String) -> Unit) =
        recoveryAndCleanupDelegate.requestPasswordReset(phone, onResult)

    fun approvePasswordReset(phone: String, onResult: (Boolean, String) -> Unit) =
        recoveryAndCleanupDelegate.approvePasswordReset(phone, onResult)

    fun adminResetAccountPassword(phone: String, newPassword: String, notifyAction: String, customerName: String) =
        recoveryAndCleanupDelegate.adminResetAccountPassword(phone, newPassword, notifyAction, customerName)

    fun requestPasswordRecoveryForStore(name: String, phone: String, password: String) =
        recoveryAndCleanupDelegate.requestPasswordRecoveryForStore(name, phone, password)

    fun requestPasswordRecoveryForProperty(title: String, phone: String, password: String) =
        recoveryAndCleanupDelegate.requestPasswordRecoveryForProperty(title, phone, password)

    fun requestPasswordRecoveryGeneral(accountName: String, phone: String, accountType: String, currentPassword: String) =
        recoveryAndCleanupDelegate.requestPasswordRecoveryGeneral(accountName, phone, accountType, currentPassword)

    fun wipeAllDatabaseData(password: String): Boolean =
        recoveryAndCleanupDelegate.wipeAllDatabaseData(password)

    fun wipeSelectedDatabaseData(password: String, selectedCollections: List<String>): Boolean =
        recoveryAndCleanupDelegate.wipeSelectedDatabaseData(password, selectedCollections)

    fun wipeAllMockAndTemporaryData() = recoveryAndCleanupDelegate.wipeAllMockAndTemporaryData()

    fun autoCleanupData(daysToKeep: Int = 30) = recoveryAndCleanupDelegate.autoCleanupData(daysToKeep)

    fun scheduleAutoCleanup(days: Int = 30) = recoveryAndCleanupDelegate.scheduleAutoCleanup(days)

    override fun onCleared() {
        super.onCleared()
        themeSyncDelegate.cleanup()
    }
}