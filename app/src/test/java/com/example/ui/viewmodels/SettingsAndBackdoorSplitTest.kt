package com.example.ui.viewmodels

import android.app.Application
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.helpers.AppState
import com.example.ui.screens.admin.OwnerBackdoorPanelLayout
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.VisualThemePalette
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SettingsAndBackdoorSplitTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var application: Application
    private lateinit var appState: AppState
    private lateinit var mockDb: FirebaseFirestore
    private lateinit var mockCollection: CollectionReference
    private lateinit var mockDocRef: DocumentReference
    private lateinit var mockBatch: WriteBatch
    private lateinit var mockVoidTask: Task<Void>
    private lateinit var mockQuery: Query
    private lateinit var mockQueryTask: Task<QuerySnapshot>

    private lateinit var mockAuthViewModel: AuthViewModel
    private lateinit var mockHomeViewModel: HomeViewModel
    private lateinit var mockBookingViewModel: BookingViewModel
    private lateinit var mockAdminViewModel: AdminViewModel

    private val triggeredNotifications = mutableListOf<String>()
    private var waitingPhoneCaptured = ""
    private var passwordShouldVerify = true

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        appState = AppState()
        triggeredNotifications.clear()
        waitingPhoneCaptured = ""
        passwordShouldVerify = true

        mockDb = mockk(relaxed = true)
        mockCollection = mockk(relaxed = true)
        mockDocRef = mockk(relaxed = true)
        mockBatch = mockk(relaxed = true)
        mockVoidTask = mockk(relaxed = true)
        mockQuery = mockk(relaxed = true)
        mockQueryTask = mockk(relaxed = true)

        every { mockVoidTask.addOnSuccessListener(any()) } answers {
            @Suppress("UNCHECKED_CAST")
            (firstArg<OnSuccessListener<Void>>()).onSuccess(null)
            mockVoidTask
        }
        every { mockVoidTask.addOnFailureListener(any()) } returns mockVoidTask

        every { mockDb.collection(any()) } returns mockCollection
        every { mockDb.batch() } returns mockBatch
        every { mockBatch.commit() } returns mockVoidTask
        every { mockCollection.document() } returns mockDocRef
        every { mockCollection.document(any()) } returns mockDocRef
        every { mockDocRef.id } returns "doc_settings_test"
        every { mockDocRef.set(any()) } returns mockVoidTask
        every { mockDocRef.set(any(), any<SetOptions>()) } returns mockVoidTask
        every { mockDocRef.update(any<Map<String, Any?>>()) } returns mockVoidTask
        every { mockDocRef.delete() } returns mockVoidTask
        every { mockCollection.whereEqualTo(any<String>(), any()) } returns mockQuery
        every { mockQuery.get() } returns mockQueryTask

        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns mockDb

        mockAuthViewModel = mockk(relaxed = true)
        every { mockAuthViewModel._currentUserId } returns MutableStateFlow("user_1")

        mockHomeViewModel = mockk(relaxed = true)
        every { mockHomeViewModel._providers } returns appState._providers
        every { mockHomeViewModel._categories } returns appState._categories

        mockBookingViewModel = mockk(relaxed = true)
        every { mockBookingViewModel._bookings } returns appState._bookings

        mockAdminViewModel = mockk(relaxed = true)
        every { mockAdminViewModel._stores } returns appState._stores
        every { mockAdminViewModel._properties } returns appState._properties
        every { mockAdminViewModel.adminRole } returns MutableStateFlow("OWNER")
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    private fun createSettingsViewModel(): SettingsViewModel {
        return SettingsViewModel(appState).apply {
            appContext = application
            getAuthViewModel = { mockAuthViewModel }
            getHomeViewModel = { mockHomeViewModel }
            getBookingViewModel = { mockBookingViewModel }
            getAdminViewModel = { mockAdminViewModel }
            getProviders = { appState._providers }
            getBookings = { appState._bookings }
            getCategories = { appState._categories }
            getStores = { appState._stores }
            getProperties = { appState._properties }
            getPasswordRecoveryWaitingPhone = { MutableStateFlow(waitingPhoneCaptured) }
            setPasswordRecoveryWaitingPhone = { waitingPhoneCaptured = it }
            verifyAdminOrOwnerPassword = { passwordShouldVerify && it == "secret123" }
            triggerNotification = { triggeredNotifications.add(it) }
        }
    }

    // =========================================================================
    // 1. SettingsThemeAndColorSyncDelegate Tests
    // =========================================================================

    @Test
    fun `theme language card settings and backdoor settings update state and Firestore`() {
        val vm = createSettingsViewModel()

        vm.updateAppLanguage("en")
        assertEquals("en", vm.settings.value.appLanguage)

        val customCard = SettingsViewModel.CardSettings(cardHeight = 220, showVipBadge = false)
        vm.updateCardSettings(customCard)
        assertEquals(220, vm.cardSettings.value.cardHeight)
        assertFalse(vm.cardSettings.value.showVipBadge)

        vm.updatePaymentEnabled(true)
        assertTrue(vm.settings.value.isPaymentEnabled)
        assertTrue(triggeredNotifications.any { it.contains("حفظ ومزامنة") })

        vm.updateBackdoorSettings(
            appName = "دليل صنعاء الموحد",
            welcomeMsg = "أهلاً بكم",
            footerMsg = "sanaa_footer",
            themeId = "LUXURY_GOLD",
            supportPhone = "777111222",
            supportEmail = "support@test.ye",
            supportWhatsapp = "777111222",
            isMaintenance = true,
            hiddenFooter = true,
            botHidden = true,
            botSize = 64,
            chatHidden = false,
            chatSize = 48,
            radiusKm = 75,
            isSpeech = true,
            isDataSaver = false,
            imgQuality = 85,
            adminUsername = "owner_admin",
            adminPassword = "NewStrongPassword!"
        )
        assertEquals("دليل صنعاء الموحد", vm.settings.value.appName)
        assertEquals("LUXURY_GOLD", vm.settings.value.activeThemeId)
        assertTrue(vm.settings.value.isMaintenanceActive)
        assertEquals(75, vm.settings.value.maxSearchRadiusKm)
        assertEquals("", vm.settings.value.ownerPassword)
        verify { mockDb.collection("admin_secrets") }

        vm.addColorPalette("زمردي", "#059669", "#115E59")
        vm.updateColorPalette("pal_1", "ذهبي", "#FFD700", "#AA8800")
        vm.deleteColorPalette("pal_1")
        vm.saveCustomPermissionsMatrixToFirestore(listOf("MANAGE_STORES", "VIEW_REPORTS"), "ADMIN")
        assertTrue(triggeredNotifications.any { it.contains("538") })
    }

    // =========================================================================
    // 2. SettingsCommsAndSyncUtilsDelegate Tests
    // =========================================================================

    @Test
    fun `chat participant blocking and voice call state transitions work accurately`() {
        val vm = createSettingsViewModel()

        assertTrue(vm.canParticipateInChat(SettingsViewModel.ChatParticipantType.VISITOR))
        vm.toggleChatParticipant(SettingsViewModel.ChatParticipantType.VISITOR)
        assertTrue(vm.isChatBlockedFor(SettingsViewModel.ChatParticipantType.VISITOR))
        assertFalse(vm.canParticipateInChat(SettingsViewModel.ChatParticipantType.VISITOR))
        assertTrue(vm.canParticipateInChat(SettingsViewModel.ChatParticipantType.PROVIDER))

        // Toggling ALL blocks every participant type
        vm.toggleChatParticipant(SettingsViewModel.ChatParticipantType.ALL)
        assertFalse(vm.canParticipateInChat(SettingsViewModel.ChatParticipantType.PROVIDER))
        assertFalse(vm.canParticipateInChat(SettingsViewModel.ChatParticipantType.ADMIN))

        vm.startVoiceCall("أحمد علي", "PROVIDER")
        assertEquals(Pair("أحمد علي", "PROVIDER"), vm.activeVoiceCall.value)
        vm.endVoiceCall()
        assertNull(vm.activeVoiceCall.value)
    }

    // =========================================================================
    // 3. SettingsBackupAndReportsDelegate Tests
    // =========================================================================

    @Test
    fun `system backup JSON serialization and restore round trip succeeds`() {
        appState._providers.value = listOf(ProviderEntity(id = "p1", name = "فني كهرباء", phone = "777000111", customCategoryName = "كهرباء"))
        appState._bookings.value = listOf(BookingEntity(id = "b1", customerName = "سالم", customerPhone = "770123456", providerId = "p1", status = "PENDING"))
        appState._categories.value = listOf(CategoryEntity(id = "c1", name = "صيانة", icon = "🔧", isMainCategory = true))
        appState._stores.value = listOf(StoreEntity(id = "s1", name = "متجر النور", phone = "773334445", isActive = true))
        appState._properties.value = listOf(PropertyEntity(id = "pr1", title = "شقة للإيجار", price = 50000.0, phone = "771223344", isActive = true))

        val vm = createSettingsViewModel()
        var backupSuccess = false
        var backupJson = ""
        vm.createSystemBackup { ok, json ->
            backupSuccess = ok
            backupJson = json
        }

        assertTrue(backupSuccess)
        assertTrue(backupJson.contains("فني كهرباء"))
        assertTrue(backupJson.contains("متجر النور"))
        assertTrue(backupJson.contains("شقة للإيجار"))

        var restoreSuccess = false
        var restoreMessage = ""
        vm.restoreSystemFromBackup(backupJson) { ok, msg ->
            restoreSuccess = ok
            restoreMessage = msg
        }
        assertTrue(restoreSuccess)
        assertEquals("Success", restoreMessage)
        verify { mockDb.collection("providers") }
        verify { mockDb.collection("bookings") }
        verify { mockDb.collection("stores") }
        verify { mockDb.collection("properties") }

        val savedPath = vm.saveBackupToLocalStorage(application, backupJson, "test_backup")
        assertTrue(savedPath.isNotBlank())

        vm.setSecondaryFirebaseConfig("sec-proj", "key", "app-id", "bucket", true)
        assertTrue(triggeredNotifications.any { it.contains("Firebase الثانوي") })
    }

    // =========================================================================
    // 4. SettingsPasswordRecoveryAndCleanupDelegate Tests
    // =========================================================================

    @Test
    fun `password recovery and wipe guards enforce password checks and update state`() {
        appState._providers.value = listOf(ProviderEntity(id = "p1", name = "علي", phone = "777555666", passwordHash = "old"))
        appState._stores.value = listOf(StoreEntity(id = "s1", name = "متجر", phone = "777999000", passwordHash = "old"))

        val vm = createSettingsViewModel()

        // Invalid phone rejected
        var reqOk = true
        var reqMsg = ""
        vm.requestPasswordReset("123") { ok, msg ->
            reqOk = ok
            reqMsg = msg
        }
        assertFalse(reqOk)
        assertTrue(reqMsg.contains("غير صالح"))

        // Admin reset updates matching provider & store password hashes
        vm.adminResetAccountPassword("777555666", "NewPass99", "DIRECT_PASSWORD", "علي")
        assertNotEquals("old", appState._providers.value.first().passwordHash)

        vm.resetAccountPassword("STORE", "777999000", "StorePass77")
        assertNotEquals("old", appState._stores.value.first().passwordHash)

        vm.requestPasswordRecoveryForStore("متجر الخير", "+967 771 112 233", "ignored")
        assertEquals("771112233", waitingPhoneCaptured)

        // Wipe guards fail on bad password and succeed on valid password
        assertFalse(vm.wipeAllDatabaseData("wrong"))
        assertFalse(vm.wipeSelectedDatabaseData("wrong", listOf("providers")))
        assertTrue(vm.wipeAllDatabaseData("secret123"))
        assertTrue(vm.wipeSelectedDatabaseData("secret123", listOf("reports", "bookings")))

        vm.wipeAllMockAndTemporaryData()
        assertTrue(triggeredNotifications.any { it.contains("التجريبية والوهمية") })
    }

    // =========================================================================
    // 5. OwnerBackdoorPanelLayout Split UI & Save Wiring Tests
    // =========================================================================

    @Test
    fun `OwnerBackdoorPanelLayout renders all sections saves edits and enforces wipe dialog security`() {
        val initialSettings = AdminSettingsEntity(
            appName = "دليل اليمن",
            welcomeMessage = "أهلاً",
            activeThemeId = "EMERALD_YEMEN",
            headerIconsOrder = "MENU,NOTIF,CHAT",
            categoriesLayoutType = "GRID_HORIZONTAL",
            enableProvidersRegistration = true,
            bannerEnabled = false
        )
        val settingsFlow = MutableStateFlow(initialSettings)
        val mockSettingsVm = mockk<SettingsViewModel>(relaxed = true)
        val mockViewModel = mockk<MainViewModel>(relaxed = true)
        val mockAuthVm = mockk<AuthViewModel>(relaxed = true)

        every { mockViewModel.settingsViewModel } returns mockSettingsVm
        every { mockViewModel.authViewModel } returns mockAuthVm
        every { mockViewModel.settings } returns settingsFlow
        every { mockAuthVm.verifyAdminOrOwnerPassword("secret123") } returns true
        every { mockAuthVm.verifyAdminOrOwnerPassword("wrong") } returns false

        val testTheme = VisualThemePalette(
            activeId = "TEST",
            primary = Color(0xFF0D9488),
            secondary = Color(0xFF115E59),
            background = Color(0xFF0F172A),
            surface = Color(0xFF1E293B),
            textPrimary = Color.White,
            textSecondary = Color(0xFF94A3B8),
            accent = Color(0xFFF59E0B),
            gradientBrush = Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B))),
            scheme = darkColorScheme()
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OwnerBackdoorPanelLayout(viewModel = mockViewModel, themeColors = testTheme)
                }
            }
        }

        // Core header & sections are rendered across split files
        composeTestRule.onNodeWithText("🔓 بوابة المالك والتحكم الخلفي الديناميكي").assertExists()
        composeTestRule.onNodeWithTag("owner_logs_btn").assertExists()
        composeTestRule.onNodeWithText("📋 التحكم بإظهار/إخفاء استمارات التسجيل للأقسام").assertExists()
        composeTestRule.onNodeWithText("📢 إعداد وتخصيص البنر الإعلاني بالأعلى (الرئيسي):").assertExists()

        // Interact with layout preset & category layout
        composeTestRule.onNodeWithText("تفعيل وترتيب الكل").performScrollTo().performClick()
        composeTestRule.onNodeWithText("شبكة عمودية (تمرير طولي) ↕️").performScrollTo().performClick()

        // Click Save button and verify full settings entity passed to SettingsViewModel
        composeTestRule.onNodeWithTag("owner_save_settings_btn").performScrollTo().performClick()
        verify(atLeast = 1) {
            mockSettingsVm.saveCustomSettingsState(match {
                it.headerIconsOrder == "MENU,CHAT,NOTIF,REFRESH,SETTINGS" &&
                    it.showRefreshIcon &&
                    it.showSettingsIcon &&
                    it.categoriesLayoutType == "GRID_VERTICAL"
            })
        }

        // Open wipe mock data dialog and verify checkbox + password validation guard
        composeTestRule.onNodeWithTag("owner_wipe_mock_btn").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("owner_wipe_confirm_submit_btn").performClick()
        composeTestRule.onNodeWithText("⚠️ يرجى تأكيد خانة الموافقة أولاً.").assertExists()

        composeTestRule.onNodeWithTag("owner_wipe_confirm_checkbox").performClick()
        composeTestRule.onNodeWithTag("owner_wipe_password_input").performTextInput("wrong")
        composeTestRule.onNodeWithTag("owner_wipe_confirm_submit_btn").performClick()
        composeTestRule.onNodeWithText("❌ كلمة المرور غير صحيحة! تم منع التطهير والحذف.").assertExists()
        verify(exactly = 0) { mockSettingsVm.wipeAllMockAndTemporaryData() }

        composeTestRule.onNodeWithTag("owner_wipe_password_input").performTextReplacement("secret123")
        composeTestRule.onNodeWithTag("owner_wipe_confirm_submit_btn").performClick()
        verify(exactly = 1) { mockSettingsVm.wipeAllMockAndTemporaryData() }

        // Verify close panel triggers logout
        composeTestRule.onNodeWithTag("owner_close_panel_btn").performScrollTo().performClick()
        verify(atLeast = 1) { mockAuthVm.logout(any()) }
    }
}
