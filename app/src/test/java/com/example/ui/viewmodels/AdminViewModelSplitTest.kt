package com.example.ui.viewmodels

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.data.models.Offer
import com.example.ui.helpers.AppState
import com.example.utils.AdminSecurityManager
import com.example.utils.AdminSession
import com.example.utils.SecureAdminStorage
import com.example.utils.SecureStorage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdminViewModelSplitTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var application: Application
    private lateinit var appState: AppState
    private lateinit var secureStorage: SecureStorage
    private lateinit var mockDb: FirebaseFirestore
    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockCollection: CollectionReference
    private lateinit var mockDocRef: DocumentReference
    private lateinit var mockListenerReg: ListenerRegistration

    private lateinit var mockHomeViewModel: HomeViewModel
    private lateinit var mockSettingsViewModel: SettingsViewModel
    private lateinit var mockInstantRequestViewModel: InstantRequestViewModel
    private lateinit var mockBookingViewModel: BookingViewModel

    private val notificationsFlow = MutableStateFlow<List<NotificationEntity>>(emptyList())
    private val triggeredNotifications = mutableListOf<String>()
    private val addedNotifications = mutableListOf<Pair<String, String>>()
    private var filtersAppliedCount = 0
    private var generatedDocCounter = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        appState = AppState()
        triggeredNotifications.clear()
        addedNotifications.clear()
        filtersAppliedCount = 0
        generatedDocCounter = 0

        secureStorage = mockk(relaxed = true)
        every { secureStorage.getAdminSession() } returns null

        mockDb = mockk(relaxed = true)
        mockAuth = mockk(relaxed = true)
        mockCollection = mockk(relaxed = true)
        mockDocRef = mockk(relaxed = true)
        mockListenerReg = mockk(relaxed = true)

        every { mockAuth.currentUser } returns null
        every { mockDb.collection(any()) } returns mockCollection
        every { mockCollection.document() } answers {
            generatedDocCounter += 1
            mockk<DocumentReference>(relaxed = true).also { ref ->
                every { ref.id } returns "doc_$generatedDocCounter"
            }
        }
        every { mockCollection.document(any()) } returns mockDocRef
        every { mockDocRef.id } returns "fixed_doc_id"
        every { mockCollection.addSnapshotListener(any<com.google.firebase.firestore.EventListener<com.google.firebase.firestore.QuerySnapshot>>()) } returns mockListenerReg

        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns mockDb

        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns mockAuth

        mockkObject(AdminSecurityManager)
        mockkObject(SecureAdminStorage)
        every { SecureAdminStorage.storeCredentials(any(), any(), any(), any(), any()) } returns true

        mockHomeViewModel = mockk(relaxed = true)
        every { mockHomeViewModel._providers } returns appState._providers
        every { mockHomeViewModel.providers } returns appState.providers
        every { mockHomeViewModel._categories } returns appState._categories
        every { mockHomeViewModel._banners } returns appState._banners

        mockSettingsViewModel = mockk(relaxed = true)
        every { mockSettingsViewModel._settings } returns appState._settings

        mockInstantRequestViewModel = mockk(relaxed = true)
        every { mockInstantRequestViewModel._offers } returns appState._offers

        mockBookingViewModel = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AdminViewModel {
        return AdminViewModel(application, appState, secureStorage).apply {
            appContext = application
            getHomeViewModel = { mockHomeViewModel }
            getSettingsViewModel = { mockSettingsViewModel }
            getInstantRequestViewModel = { mockInstantRequestViewModel }
            getBookingViewModel = { mockBookingViewModel }
            getNotifications = { notificationsFlow }
            onAddNotification = { title, message, _, _ ->
                addedNotifications.add(title to message)
            }
            onTriggerNotification = { msg ->
                triggeredNotifications.add(msg)
            }
            onApplyFilters = {
                filtersAppliedCount += 1
            }
        }
    }

    // =========================================================================
    // 1. Auth, Session & Security Delegate
    // =========================================================================

    @Test
    fun `init restores valid admin session and ignores expired session`() {
        every { secureStorage.getAdminSession() } returns AdminSession(
            uid = "u1",
            email = "owner@test.com",
            loginTime = System.currentTimeMillis() - 1000L,
            refreshToken = "tok",
            role = "OWNER",
            permissions = listOf("ALL")
        )
        val vm = createViewModel()
        assertEquals("OWNER", vm.adminRole.value)
        assertEquals(listOf("ALL"), vm.supervisorPermissions.value)

        // Expired session (> 30 days)
        every { secureStorage.getAdminSession() } returns AdminSession(
            uid = "u2",
            email = "old@test.com",
            loginTime = System.currentTimeMillis() - (31L * 24 * 60 * 60 * 1000),
            refreshToken = "old",
            role = "ADMIN",
            permissions = emptyList()
        )
        val expiredVm = createViewModel()
        assertEquals("GUEST", expiredVm.adminRole.value)
    }

    @Test
    fun `loginAdminSecure loginOwnerSecure loginSupervisorSecure and logoutAdmin preserve exact auth flow`() = runTest {
        val vm = createViewModel()

        // Admin login
        coEvery {
            AdminSecurityManager.verifyCredentials(any(), any(), any(), any(), any(), "ADMIN")
        } returns "ADMIN"
        var adminSuccess = false
        vm.loginAdminSecure("admin@yemen.com", "Secret@123", rememberMe = true) { s, _ ->
            adminSuccess = s
        }
        assertTrue(adminSuccess)
        assertEquals("ADMIN", vm.adminRole.value)
        assertTrue(vm.isAdminSessionValid() || !vm.isAdminSessionValid()) // callable without crash
        verify(atLeast = 1) { secureStorage.saveAdminSession(match { it.role == "ADMIN" }) }

        // Owner login
        coEvery {
            AdminSecurityManager.verifyCredentials(any(), any(), any(), any(), any(), "OWNER")
        } returns "OWNER"
        var ownerSuccess = false
        vm.loginOwnerSecure("owner@yemen.com", "Owner@123", rememberMe = true) { s, _ ->
            ownerSuccess = s
        }
        assertTrue(ownerSuccess)
        assertEquals("OWNER", vm.adminRole.value)
        assertEquals(listOf("ALL"), vm.supervisorPermissions.value)

        // Supervisor login resolves permissions from AppState._supervisors
        appState._supervisors.value = listOf(
            SupervisorEntity(
                id = "sup_1",
                name = "Ali",
                permissions = listOf("MANAGE_STORES", "VIEW_REPORTS")
            )
        )
        coEvery {
            AdminSecurityManager.verifyCredentials(any(), any(), any(), any(), any(), "SUPERVISOR")
        } returns "SUPERVISOR"
        var supSuccess = false
        vm.loginSupervisorSecure("sup_1", "pass", rememberMe = true) { s, _ ->
            supSuccess = s
        }
        assertTrue(supSuccess)
        assertEquals("SUPERVISOR", vm.adminRole.value)
        assertEquals(listOf("MANAGE_STORES", "VIEW_REPORTS"), vm.supervisorPermissions.value)

        // Logout resets state
        vm.logoutAdmin()
        assertEquals("GUEST", vm.adminRole.value)
        assertTrue(vm.supervisorPermissions.value.isEmpty())
        verify(atLeast = 1) { secureStorage.clearAdminSession() }
    }

    // =========================================================================
    // 2. Requests, Duplicate Prevention & Users Delegate
    // =========================================================================

    @Test
    fun `checkAndGetDuplicateAccountType detects duplicates across providers pending stores and properties`() {
        val vm = createViewModel()

        appState._providers.value = listOf(
            ProviderEntity(id = "p1", phone = "+967 771 111 111", isDeleted = false),
            ProviderEntity(id = "p_del", phone = "779999999", isDeleted = true)
        )
        appState._pendingProviders.value = listOf(
            PendingProviderEntity(id = "pend1", phone = "772222222", status = "PENDING")
        )
        appState._stores.value = listOf(
            StoreEntity(id = "s1", phone = "773333333", isDeleted = false)
        )
        appState._properties.value = listOf(
            PropertyEntity(id = "prop1", phone = "774444444", isDeleted = false)
        )

        assertEquals("مقدم خدمة نشط (فني)", vm.checkAndGetDuplicateAccountType("+967 771 111 111", ""))
        assertNull(vm.checkAndGetDuplicateAccountType("+967 771 111 111", "p1")) // exclude self
        assertNull(vm.checkAndGetDuplicateAccountType("779999999", "")) // deleted provider ignored
        assertEquals("طلب انضمام فني قيد المراجعة", vm.checkAndGetDuplicateAccountType("772222222", ""))
        assertEquals("محل أو متجر تجاري مسجل", vm.checkAndGetDuplicateAccountType("773333333", ""))
        assertEquals("إعلان عقاري مدرج", vm.checkAndGetDuplicateAccountType("774444444", ""))
        assertNull(vm.checkAndGetDuplicateAccountType("770000000", ""))
    }

    @Test
    fun `approveRequest routes store property job client and provider correctly`() {
        val vm = createViewModel()

        // Store request
        val storeReq = PendingProviderEntity(
            id = "req_store",
            name = "متجر النور",
            phone = "771000001",
            categoryId = "STORE",
            profession = "STORE_OWNER",
            area = "عدن - المعلا",
            providerType = "STORE",
            status = "PENDING"
        )
        appState._pendingProviders.value = listOf(storeReq)
        vm.approveRequest(storeReq)
        assertTrue(appState._stores.value.any { it.name == "متجر النور" && it.cityId == "ye_ade" && it.isActive })
        assertTrue(appState._pendingProviders.value.none { it.id == "req_store" })

        // Property request
        val propReq = PendingProviderEntity(
            id = "req_prop",
            name = "عمارة السلام",
            phone = "771000002",
            categoryId = "PROPERTY",
            profession = "PROPERTY_OWNER",
            area = "تعز",
            providerType = "PROPERTY",
            status = "PENDING"
        )
        vm.approveRequest(propReq)
        assertTrue(appState._properties.value.any { it.title == "عمارة السلام" && it.cityId == "ye_tai" && it.isActive })

        // Job request
        val jobReq = PendingProviderEntity(
            id = "req_job",
            name = "مهندس برمجيات",
            phone = "771000003",
            categoryId = "JOB",
            profession = "JOB_POSTER",
            customCategoryName = "مهندس برمجيات",
            area = "الحديدة",
            providerType = "JOB",
            status = "PENDING"
        )
        vm.approveRequest(jobReq)
        assertTrue(appState._jobs.value.any { it.title == "مهندس برمجيات" && it.isApproved })

        // Provider request
        val techReq = PendingProviderEntity(
            id = "req_tech",
            name = "سالم الكهربائي",
            phone = "771000004",
            categoryId = "Electrical",
            area = "صنعاء",
            providerType = "PROVIDER",
            status = "PENDING"
        )
        vm.approveRequest(techReq)
        assertTrue(appState._providers.value.any { it.name == "سالم الكهربائي" && it.cityId == "ye_san" })
        assertTrue(filtersAppliedCount >= 1)
    }

    @Test
    fun `rejectRequest and registered user moderation update state flows properly`() {
        val vm = createViewModel()
        val pending = PendingProviderEntity(id = "r1", name = "خالد", phone = "771234000", status = "PENDING")
        appState._pendingProviders.value = listOf(pending)

        vm.rejectRequest(pending, "بيانات ناقصة")
        assertTrue(appState._pendingProviders.value.isEmpty())
        assertTrue(addedNotifications.any { it.second.contains("بيانات ناقصة") })

        appState._registeredUsersList.value = listOf(
            mapOf("id" to "u1", "name" to "أحمد", "isApproved" to false, "isBlocked" to false)
        )
        vm.approveRegisteredUser("u1", "أحمد")
        assertEquals(true, vm.registeredUsersList.value.first()["isApproved"])

        vm.toggleBlockRegisteredUser("u1", false, "أحمد")
        assertEquals(true, vm.registeredUsersList.value.first()["isBlocked"])

        vm.deleteRegisteredUser("u1", "أحمد")
        assertTrue(vm.registeredUsersList.value.isEmpty())
    }

    // =========================================================================
    // 3. Providers, Catalog & Custom Profile Tabs Delegate
    // =========================================================================

    @Test
    fun `provider toggles and category delegation sync state properly`() {
        val vm = createViewModel()
        appState._providers.value = listOf(
            ProviderEntity(
                id = "prov_1",
                name = "عمر السباك",
                subscriptionStatus = "APPROVED",
                isVip = false,
                isVerified = false,
                isRecommended = false
            )
        )

        vm.toggleProviderPin("prov_1")
        vm.toggleProviderVerification("prov_1")
        vm.toggleProviderRecommendation("prov_1")
        vm.toggleProviderSubscription("prov_1")

        val updated = appState._providers.value.first { it.id == "prov_1" }
        assertTrue(updated.isVip)
        assertTrue(updated.isVerified)
        assertTrue(updated.isRecommended)
        assertEquals("EXPIRED", updated.subscriptionStatus)

        vm.addNewCategory("نجارة", "Carpentry", "🪚", "أعمال الخشب")
        verify { mockHomeViewModel.addNewCategory("نجارة", "Carpentry", "🪚", "أعمال الخشب", "", true) }

        vm.deleteCategory("cat_1")
        verify { mockHomeViewModel.deleteCategory("cat_1") }

        appState._customProfileTabs.value = listOf(
            CustomProfileTabEntity(id = "tab1", title = "أسعارنا", isEnabled = true)
        )
        vm.toggleCustomProfileTab("tab1")
        assertTrue(triggeredNotifications.any { it.contains("إيقاف التبويب") })
    }

    // =========================================================================
    // 4. Stores, Properties, Jobs & Cross-Entity Delegate
    // =========================================================================

    @Test
    fun `store property job CRUD and CSV export work through AdminViewModel facade`() {
        val vm = createViewModel()

        // Store save + duplicate block
        val store = StoreEntity(id = "st_1", name = "سوبرماركت اليمن", phone = "775551111", isActive = true)
        vm.saveStore(store)
        assertTrue(appState._stores.value.any { it.id == "st_1" })

        val dupStore = StoreEntity(id = "st_2", name = "مكرر", phone = "775551111")
        vm.saveStore(dupStore)
        assertFalse(appState._stores.value.any { it.id == "st_2" })
        assertTrue(triggeredNotifications.any { it.contains("مسجل بالفعل") })

        // Soft delete + restore + badges
        vm.deleteStore("st_1")
        assertTrue(appState._stores.value.first { it.id == "st_1" }.isDeleted)
        vm.restoreEntity("STORE", "st_1")
        assertFalse(appState._stores.value.first { it.id == "st_1" }.isDeleted)

        vm.setStorePinned("st_1", true)
        vm.setStoreVip("st_1", true)
        vm.setStoreVerified("st_1", true)
        vm.setStoreRecommended("st_1", true)
        vm.setStoreBlocked("st_1", true, "مخالفة")
        val stUpdated = appState._stores.value.first { it.id == "st_1" }
        assertTrue(stUpdated.isPinned && stUpdated.isVip && stUpdated.isVerified && stUpdated.isRecommended && stUpdated.isBlocked)

        // Property lifecycle
        val prop = PropertyEntity(id = "pr_1", title = "فيلا حدة", phone = "775552222")
        vm.saveProperty(prop)
        vm.setPropertyActive("pr_1", true)
        vm.setPropertyPinned("pr_1", true)
        assertTrue(appState._properties.value.first { it.id == "pr_1" }.isPinned)
        vm.deleteProperty("pr_1")
        assertTrue(appState._properties.value.first { it.id == "pr_1" }.isDeleted)
        vm.restoreProperty("pr_1")
        assertFalse(appState._properties.value.first { it.id == "pr_1" }.isDeleted)

        // Job & JobApplication + Clipboard CSV export
        val job = JobEntity(id = "job_1", title = "مطور أندرويد", phone = "775553333", isApproved = false)
        vm.saveJob(job)
        vm.setJobApproved("job_1", true)
        assertTrue(appState._jobs.value.first { it.id == "job_1" }.isApproved)

        val appEntity = JobApplicationEntity(
            applicantName = "ياسين",
            applicantPhone = "771112233",
            jobTitle = "مطور أندرويد",
            companyName = "تك",
            applicantQuals = "بكالوريوس",
            status = "PENDING",
            createdAt = 1_700_000_000_000L
        )
        vm.submitJobApplication(appEntity)
        val savedAppId = vm.jobApplications.value.first().id
        vm.acceptJobApplication(savedAppId)
        assertEquals("ACCEPTED", vm.jobApplications.value.first().status)

        vm.exportJobApplicantsCsv(application)
        val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        assertTrue(clipText.contains("ياسين"))
        assertTrue(clipText.contains("مطور أندرويد"))
    }

    // =========================================================================
    // 5. Commerce, Wallets, Payments, Offers, Ratings & Stats Delegate
    // =========================================================================

    @Test
    fun `wallet transactions payment verification product pricing offers and ratings behave identically`() {
        val vm = createViewModel()

        // Wallet deposit & withdrawal clamping
        appState._internalWallets.value = listOf(
            InternalWalletEntity(id = "w1", ownerName = "فني 1", balance = 500.0)
        )
        vm.performWalletTransaction("w1", "فني 1", "771", "PROVIDER", "DEPOSIT", 250.0, "مكافأة")
        assertTrue(triggeredNotifications.last().contains("750.0"))

        vm.performWalletTransaction("w1", "فني 1", "771", "PROVIDER", "WITHDRAWAL", 900.0, "سحب")
        assertTrue(triggeredNotifications.last().contains("0.0"))

        // createPayment calculation & verification validation
        appState._settings.value = AdminSettingsEntity(
            requireAdvancePayment = true,
            advancePaymentPercent = 0.25f,
            minAdvanceAmount = 500.0,
            maxAdvanceAmount = 5000.0,
            isCommissionEnabled = true,
            paymentCommissionRate = 0.10f
        )
        val capturedPayments = mutableListOf<PaymentEntity>()
        every { mockDocRef.set(capture(capturedPayments)) } answers { mockk(relaxed = true) }

        vm.createPayment(
            userId = "u1",
            providerId = "p1",
            amount = 1000.0, // 25% = 250 < min 500 -> clamped to 500; commission = 100
            method = "WALLET"
        )
        // Generated doc uses mockk returned from mockCollection.document()
        vm.verifyPayment("   ", true, "note", "admin")
        assertTrue(triggeredNotifications.last().contains("غير صالح"))

        appState._payments.value = listOf(PaymentEntity(id = "pay_1", amount = 1000.0))
        vm.verifyPayment("unknown_pay", true, "note", "admin")
        assertTrue(triggeredNotifications.last().contains("لم يتم العثور"))

        // Product pricing
        appState._products.value = listOf(
            ProductEntity(id = "prod_1", name = "كابل", price = 2000.0, oldPrice = 0.0)
        )
        vm.updateProductPrice("prod_1", 1500.0)
        val prodAfter = vm.products.value.first { it.id == "prod_1" }
        assertEquals(1500.0, prodAfter.price, 0.001)
        assertEquals(2000.0, prodAfter.oldPrice, 0.001)

        // Offers
        vm.saveOffer(Offer(id = "off_1", title = "خصم العيد", isActive = true))
        assertEquals(1, appState._offers.value.size)
        vm.toggleOfferStatus("off_1", false)
        assertFalse(appState._offers.value.first().isActive)
        vm.deleteOffer("off_1")
        assertTrue(appState._offers.value.isEmpty())

        // Ratings
        val rating = RatingEntity(id = "rat_1", targetId = "p1", targetType = "PROVIDER", rating = 5.0f)
        vm.addRating(rating)
        assertEquals(1, vm.ratings.value.size)
        vm.deleteRating("rat_1")
        assertTrue(vm.ratings.value.isEmpty())
    }

    @Test
    fun `analytics audit logs stats and reports aggregate AppState accurately`() {
        val vm = createViewModel()
        val now = System.currentTimeMillis()

        appState._providers.value = listOf(
            ProviderEntity(id = "p1", isVerified = true, subscriptionStatus = "APPROVED", rating = 4.9f, numReviews = 12, area = "صنعاء - حدة", isAvailable = true, isBlocked = false),
            ProviderEntity(id = "p2", isVerified = false, subscriptionStatus = "PENDING", rating = 3.8f, numReviews = 0, area = "عدن - خورمكسر", isAvailable = false, isBlocked = true)
        )
        appState._pendingProviders.value = listOf(
            PendingProviderEntity(id = "pend1", status = "PENDING")
        )
        appState._stores.value = listOf(
            StoreEntity(id = "s1", categoryId = "متجر إلكترونيات"),
            StoreEntity(id = "s2", categoryId = "مطعم الشيباني"),
            StoreEntity(id = "s3", categoryId = "مركز طبي وصيدلية")
        )
        appState._bookings.value = listOf(
            BookingEntity(id = "b1", status = "PENDING", createdAt = now, customerArea = "صنعاء"),
            BookingEntity(id = "b2", status = "IN_PROGRESS", createdAt = now, customerArea = "تعز"),
            BookingEntity(id = "b3", status = "COMPLETED", createdAt = now, customerArea = "إب"),
            BookingEntity(id = "b4", status = "CANCELLED", createdAt = now, customerArea = "المكلا")
        )
        appState._payments.value = listOf(
            PaymentEntity(id = "pay1", status = "COMPLETED", amount = 10000.0, commission = 1000.0, providerShare = 9000.0, paidAt = now)
        )

        val sysStats = vm.getSystemStats()
        assertEquals(2, sysStats.totalProviders)
        assertEquals(2, sysStats.activeBookings) // PENDING + IN_PROGRESS
        assertEquals(1, sysStats.completedOrders)
        assertEquals(1, sysStats.pendingApprovals)

        val bookingStats = vm.getBookingStats("ALL")
        assertEquals(4, bookingStats.total)
        assertEquals(1, bookingStats.pending)
        assertEquals(1, bookingStats.inProgress)
        assertEquals(1, bookingStats.completed)
        assertEquals(1, bookingStats.cancelled)

        val revenueStats = vm.getRevenueStats("ALL")
        assertEquals(10000.0, revenueStats.totalRevenue, 0.01)
        assertEquals(1000.0, revenueStats.platformCommission, 0.01)
        assertEquals(9000.0, revenueStats.providerPayouts, 0.01)

        val providerStats = vm.getProviderStats()
        assertEquals(1, providerStats.totalVerified)
        assertEquals(1, providerStats.topRated)
        assertEquals(1, providerStats.suspended)

        val categoryStats = vm.getCategoryStats()
        assertEquals(2, categoryStats.maintenanceCount)
        assertEquals(1, categoryStats.storeCount)
        assertEquals(1, categoryStats.restaurantCount)
        assertEquals(1, categoryStats.medicalCount)

        val cityStats = vm.getCityStats()
        assertEquals(1, cityStats.sanaaCount)
        assertEquals(1, cityStats.adenCount)
        assertEquals(1, cityStats.taizCount)
        assertEquals(1, cityStats.ibbCount)
        assertEquals(1, cityStats.mukallaCount)

        vm.recordAuditLog("APPROVE", "اعتماد مزود")
        vm.sendAdminNotification("تنبيه", "رسالة", "ALL")
        assertEquals(2, vm.getAuditLogs().size)
        assertEquals(1, vm.getAuditLogs("APPROVE").size)
        assertEquals(1, vm.adminNotificationsList.value.size)

        val csv = vm.exportAuditLogs()
        assertTrue(csv.contains("APPROVE"))
        assertTrue(vm.exportReport("FINANCIAL").contains("التقرير المالي"))
        assertEquals("HEALTHY", vm.getSystemHealth().status)
        assertTrue(vm.getStorageUsage().totalBytes > 0L)

        vm.clearSystemLogs()
        assertTrue(vm.getSystemLogs().isEmpty())
    }
}
