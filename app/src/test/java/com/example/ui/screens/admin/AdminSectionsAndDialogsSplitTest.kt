package com.example.ui.screens.admin

import android.app.Application
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.data.models.InstantRequestEntity
import com.example.data.models.RequestOfferEntity
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodels.AdminViewModel
import com.example.ui.viewmodels.AuthViewModel
import com.example.ui.viewmodels.BookingViewModel
import com.example.ui.viewmodels.SettingsViewModel
import com.example.utils.VisualThemePalette
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class AdminSectionsAndDialogsSplitTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var application: Application
    private lateinit var mockViewModel: MainViewModel
    private lateinit var mockAdminViewModel: AdminViewModel
    private lateinit var mockAuthViewModel: AuthViewModel
    private lateinit var mockBookingViewModel: BookingViewModel
    private lateinit var mockSettingsViewModel: SettingsViewModel
    private lateinit var testTheme: VisualThemePalette

    private val adminRoleFlow = MutableStateFlow("OWNER")
    private val supervisorPermissionsFlow = MutableStateFlow<List<String>>(emptyList())
    private val pendingProvidersFlow = MutableStateFlow<List<PendingProviderEntity>>(emptyList())
    private val reportsFlow = MutableStateFlow<List<ReportEntity>>(emptyList())
    private val categoriesFlow = MutableStateFlow<List<CategoryEntity>>(emptyList())
    private val providersFlow = MutableStateFlow<List<ProviderEntity>>(emptyList())
    private val bookingsFlow = MutableStateFlow<List<BookingEntity>>(emptyList())
    private val notificationsFlow = MutableStateFlow<List<NotificationEntity>>(emptyList())
    private val chatChannelsFlow = MutableStateFlow<List<ChatChannelEntity>>(emptyList())
    private val settingsFlow = MutableStateFlow(AdminSettingsEntity())
    private val bannersFlow = MutableStateFlow<List<BannerEntity>>(emptyList())
    private val supervisorsFlow = MutableStateFlow<List<SupervisorEntity>>(emptyList())
    private val colorPalettesFlow = MutableStateFlow<List<ColorPaletteEntity>>(emptyList())
    private val citiesFlow = MutableStateFlow<List<CityEntity>>(emptyList())
    private val storesFlow = MutableStateFlow<List<StoreEntity>>(emptyList())
    private val propertiesFlow = MutableStateFlow<List<PropertyEntity>>(emptyList())
    private val jobsFlow = MutableStateFlow<List<JobEntity>>(emptyList())
    private val jobApplicationsFlow = MutableStateFlow<List<JobApplicationEntity>>(emptyList())
    private val registeredUsersFlow = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    private val instantRequestsFlow = MutableStateFlow<List<InstantRequestEntity>>(emptyList())
    private val requestOffersFlow = MutableStateFlow<List<RequestOfferEntity>>(emptyList())
    private val passwordRecoveryFlow = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    private val callsLogFlow = MutableStateFlow<List<CallEntity>>(emptyList())
    private val couponsFlow = MutableStateFlow<List<CouponEntity>>(emptyList())
    private val internalWalletsFlow = MutableStateFlow<List<InternalWalletEntity>>(emptyList())
    private val walletTxFlow = MutableStateFlow<List<WalletTransactionEntity>>(emptyList())
    private val paymentWalletsFlow = MutableStateFlow<List<PaymentWalletEntity>>(emptyList())
    private val paymentsFlow = MutableStateFlow<List<PaymentEntity>>(emptyList())
    private val deletedProvidersFlow = MutableStateFlow<List<ProviderEntity>>(emptyList())
    private val customTabsFlow = MutableStateFlow<List<CustomProfileTabEntity>>(emptyList())

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        testTheme = VisualThemePalette(
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

        mockAdminViewModel = mockk(relaxed = true)
        mockAuthViewModel = mockk(relaxed = true)
        mockBookingViewModel = mockk(relaxed = true)
        mockSettingsViewModel = mockk(relaxed = true)

        mockViewModel = mockk(relaxed = true)
        every { mockViewModel.adminViewModel } returns mockAdminViewModel
        every { mockViewModel.authViewModel } returns mockAuthViewModel
        every { mockViewModel.bookingViewModel } returns mockBookingViewModel
        every { mockViewModel.settingsViewModel } returns mockSettingsViewModel

        every { mockViewModel.adminRole } returns adminRoleFlow
        every { mockViewModel.supervisorPermissions } returns supervisorPermissionsFlow
        every { mockViewModel.pendingProviders } returns pendingProvidersFlow
        every { mockViewModel.reports } returns reportsFlow
        every { mockViewModel.categories } returns categoriesFlow
        every { mockViewModel.providers } returns providersFlow
        every { mockViewModel.bookings } returns bookingsFlow
        every { mockViewModel.notifications } returns notificationsFlow
        every { mockViewModel.chatChannels } returns chatChannelsFlow
        every { mockViewModel.settings } returns settingsFlow
        every { mockViewModel.banners } returns bannersFlow
        every { mockViewModel.supervisors } returns supervisorsFlow
        every { mockViewModel.colorPalettes } returns colorPalettesFlow
        every { mockViewModel.cities } returns citiesFlow
        every { mockViewModel.stores } returns storesFlow
        every { mockViewModel.properties } returns propertiesFlow
        every { mockViewModel.jobs } returns jobsFlow
        every { mockViewModel.jobApplications } returns jobApplicationsFlow
        every { mockViewModel.registeredUsersList } returns registeredUsersFlow
        every { mockViewModel.instantRequests } returns instantRequestsFlow
        every { mockViewModel.requestOffers } returns requestOffersFlow
        every { mockViewModel.passwordRecoveryRequests } returns passwordRecoveryFlow
        every { mockViewModel.callsLog } returns callsLogFlow
        every { mockViewModel.coupons } returns couponsFlow
        every { mockViewModel.internalWallets } returns internalWalletsFlow
        every { mockViewModel.walletTransactions } returns walletTxFlow
        every { mockViewModel.paymentWallets } returns paymentWalletsFlow
        every { mockViewModel.payments } returns paymentsFlow
        every { mockViewModel.deletedProviders } returns deletedProvidersFlow
        every { mockViewModel.customProfileTabs } returns customTabsFlow
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // =========================================================================
    // 1. Role & Tab Routing Integrity
    // =========================================================================

    @Test
    fun `role tab permissions and security section routes remain intact after split`() {
        val ownerTabs = buildAdminTabsForRole("OWNER", emptyList()).map { it.first }
        assertTrue(ownerTabs.contains("BACKDOOR"))
        listOf(
            "PASSWORDS_RESET", "SUPERVISORS", "ROLES_PERMISSIONS", "BACKUP",
            "CLEAN", "BLOCKED", "DELETED", "MAP_CONTROLS", "FINANCIAL_REPORTS",
            "STORAGE_QUOTA", "SECURITY_AUDIT", "SYSTEM_SETTINGS", "AUTO_DISPATCH",
            "API_MANAGEMENT", "AI_ASSISTANT_PANEL", "ADMIN_PAYMENT_PANEL",
            "VOICE_CALLS_PANEL", "MAINTENANCE_MODE", "DATA_MANAGEMENT"
        ).forEach { expectedTab ->
            assertTrue("Missing tab $expectedTab for OWNER", expectedTab in ownerTabs)
        }

        val adminTabs = buildAdminTabsForRole("ADMIN", emptyList()).map { it.first }
        assertFalse("ADMIN must not see BACKDOOR", "BACKDOOR" in adminTabs)
        assertTrue("PASSWORDS_RESET" in adminTabs)

        assertFalse(hasAdminTabAccess("PASSWORDS_RESET", "GUEST", listOf("ALL")))
        assertTrue(hasAdminTabAccess("PASSWORDS_RESET", "SUPERVISOR", listOf("PASS_VIEW")))
        assertTrue(hasAdminTabAccess("MAP_CONTROLS", "SUPERVISOR", listOf("MANAGE_MAP")))
        assertFalse(hasAdminTabAccess("BACKDOOR", "SUPERVISOR", listOf("ALL")))
        assertFalse(hasAdminTabAccess("CLEAN", "SUPERVISOR", listOf("PASS_VIEW")))
    }

    // =========================================================================
    // 2. Security, Password Reset & Supervisors Sections Tests
    // =========================================================================

    @Test
    fun `password reset section renders requests and service provider password cards`() {
        passwordRecoveryFlow.value = listOf(
            mapOf(
                "id" to "rec_1",
                "phone" to "771234567",
                "name" to "صالح العامري",
                "accountType" to "فني",
                "status" to "PENDING"
            )
        )
        providersFlow.value = listOf(
            ProviderEntity(id = "prov_1", name = "خالد الكهربائي", phone = "770001122")
        )

        lateinit var capturedState: AdminPanelState
        composeTestRule.setContent {
            MyApplicationTheme {
                val state = rememberAdminPanelState(mockViewModel)
                capturedState = state
                LazyColumn {
                    adminSectionSecurityAndSystem("PASSWORDS_RESET", state, mockViewModel, testTheme)
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("👤 صالح العامري", useUnmergedTree = true).assertIsDisplayed()

        composeTestRule.runOnUiThread {
            capturedState.adminPasswordSubTab = "SERVICES"
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("خالد الكهربائي", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("رقم الهاتف: 770001122", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `supervisors section renders and triggers addSupervisor on authViewModel`() {
        supervisorsFlow.value = listOf(
            SupervisorEntity(id = "sup_1", name = "علي المشرف", role = "SUPPORT", passcodeHash = "9988")
        )

        lateinit var capturedState: AdminPanelState
        composeTestRule.setContent {
            MyApplicationTheme {
                val state = rememberAdminPanelState(mockViewModel)
                capturedState = state
                LazyColumn(modifier = Modifier.testTag("test_admin_lazy_column")) {
                    adminSectionSecurityAndSystem("SUPERVISORS", state, mockViewModel, testTheme)
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("test_admin_lazy_column")
            .performScrollToNode(hasText("علي المشرف"))
        composeTestRule.onNodeWithText("علي المشرف", useUnmergedTree = true).assertIsDisplayed()

        composeTestRule.runOnUiThread {
            capturedState.supervisorInputName = "مشرف جديد"
            capturedState.supervisorInputPasscode = "123456"
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("test_admin_lazy_column")
            .performScrollToNode(hasText("إضافة المشرف وتثبيت الصلاحيات 🛡️"))
        composeTestRule.onNodeWithText("إضافة المشرف وتثبيت الصلاحيات 🛡️", useUnmergedTree = true).performClick()

        verify {
            mockAuthViewModel.addSupervisor(
                name = "مشرف جديد",
                role = "SUPPORT",
                passcode = "123456",
                permissions = emptyList()
            )
        }
    }

    // =========================================================================
    // 3. Backup, Clean, Blocked, Deleted, Map & Financial Reports Tests
    // =========================================================================

    @Test
    fun `clean section opens wipe dialog and moderation tabs unblock or restore providers`() {
        providersFlow.value = listOf(
            ProviderEntity(id = "blk_1", name = "فني محظور", phone = "771111111", profession = "سباك", isBlocked = true)
        )
        deletedProvidersFlow.value = listOf(
            ProviderEntity(id = "del_1", name = "فني محذوف", phone = "772222222", profession = "نجار", isDeleted = true)
        )

        lateinit var capturedState: AdminPanelState

        composeTestRule.setContent {
            MyApplicationTheme {
                val state = rememberAdminPanelState(mockViewModel)
                capturedState = state
                LazyColumn(modifier = Modifier.testTag("test_admin_lazy_column")) {
                    adminSectionSecurityAndSystem(state.activeSubTab, state, mockViewModel, testTheme)
                }
                AdminPanelDialogs(state = state, viewModel = mockViewModel, themeColors = testTheme)
            }
        }

        composeTestRule.runOnUiThread {
            capturedState.activeSubTab = "CLEAN"
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("test_admin_lazy_column")
            .performScrollToNode(hasText("مسح كامل البيانات وإعادة بناء الدليل العظيم"))
        composeTestRule.onNodeWithText("مسح كامل البيانات وإعادة بناء الدليل العظيم", useUnmergedTree = true)
            .performClick()
        composeTestRule.waitForIdle()
        assertTrue(capturedState.showWipeConfirmDialog)
        composeTestRule.onNodeWithText("🚨 تأكيد الهوية الأمنية للمصفي", useUnmergedTree = true).assertIsDisplayed()

        composeTestRule.runOnUiThread {
            capturedState.showWipeConfirmDialog = false
            capturedState.activeSubTab = "BLOCKED"
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("إلغاء الحظر ✅", useUnmergedTree = true).performClick()
        verify { mockAdminViewModel.toggleProviderBlock("blk_1") }

        composeTestRule.runOnUiThread {
            capturedState.activeSubTab = "DELETED"
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("استعادة حساب الفني 🟢", useUnmergedTree = true).performClick()
        verify { mockAdminViewModel.restoreProvider("del_1") }

        composeTestRule.runOnUiThread {
            capturedState.activeSubTab = "FINANCIAL_REPORTS"
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("📈 تقارير الأرباح المتقدمة", useUnmergedTree = true).assertIsDisplayed()
    }

    // =========================================================================
    // 4. All 4 Split Dialog Modules End-to-End State Triggers
    // =========================================================================

    @Test
    fun `all split dialog categories render and invoke viewModel actions properly`() {
        lateinit var capturedState: AdminPanelState
        every { mockAuthViewModel.verifyAdminOrOwnerPassword("0000", any(), any()) } returns false
        every { mockAuthViewModel.verifyAdminOrOwnerPassword("9999", any(), any()) } returns true
        every { mockSettingsViewModel.wipeSelectedDatabaseData("9999", any()) } returns true

        composeTestRule.setContent {
            MyApplicationTheme {
                val state = rememberAdminPanelState(mockViewModel)
                capturedState = state
                AdminPanelDialogs(state = state, viewModel = mockViewModel, themeColors = testTheme)
            }
        }

        // 1) Catalog/Request dialog: reject pending provider
        composeTestRule.runOnUiThread {
            capturedState.rejectingProviderRequest = PendingProviderEntity(id = "pend_1", name = "فهد")
            capturedState.providerRejectionReasonText = "صور غير واضحة"
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("تأكيد الرفض", useUnmergedTree = true).performClick()
        verify { mockAdminViewModel.rejectTechnician("pend_1", "صور غير واضحة") }
        assertNull(capturedState.rejectingProviderRequest)

        // 2) Operations/Booking dialog: delete booking
        composeTestRule.runOnUiThread {
            capturedState.showDeleteBookingConfirmId = "book_99"
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("نعم، امسح تماماً", useUnmergedTree = true).performClick()
        verify { mockBookingViewModel.deleteBooking("book_99") }
        assertNull(capturedState.showDeleteBookingConfirmId)

        // 3) Payment/Wallet dialog: verify payment
        composeTestRule.runOnUiThread {
            capturedState.verifyingPaymentObj = PaymentEntity(id = "pay_77", amount = 5000.0, walletProvider = "جيب")
            capturedState.adminVerifyPaymentNote = "تم التحقق"
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("اعتماد وتأكيد ✅", useUnmergedTree = true).performClick()
        verify { mockAdminViewModel.verifyPayment("pay_77", true, "تم التحقق", any()) }
        assertNull(capturedState.verifyingPaymentObj)

        // 4) Security/Supervisor dialog: wipe password validation & recovery notif reset
        composeTestRule.runOnUiThread {
            capturedState.showWipeConfirmDialog = true
            capturedState.wipeInputPassword = "9999"
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("تأكيد مسح وتطهير النظام العظيم", useUnmergedTree = true).performClick()
        verify { mockAuthViewModel.verifyAdminOrOwnerPassword("9999", any(), any()) }
        verify { mockSettingsViewModel.wipeSelectedDatabaseData("9999", any()) }
        assertFalse(capturedState.showWipeConfirmDialog)

        composeTestRule.runOnUiThread {
            capturedState.selectedRecoveryNotif = NotificationEntity(
                id = "notif_rec_1",
                message = "طلب استعادة كلمة مرور الحساب: متجر النجاح (هاتف: 775554433)"
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("تنفيذ الإجراء وإرسال 🚀", useUnmergedTree = true).performClick()
        verify {
            mockSettingsViewModel.adminResetAccountPassword(
                phone = "775554433",
                newPassword = "123456",
                notifyAction = "DIRECT_PASSWORD",
                customerName = "متجر النجاح"
            )
        }
        assertNull(capturedState.selectedRecoveryNotif)
    }
}
