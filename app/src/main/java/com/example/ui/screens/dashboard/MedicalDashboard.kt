package com.example.ui.screens.dashboard

import android.widget.Toast
import androidx.activity.compose.BackHandler
import com.example.ui.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UnifiedBusinessAccount
import com.example.data.repositories.*
import com.example.domain.entities.DashboardStatsEntity
import com.example.ui.MainViewModel
import com.example.ui.helpers.AppPreferenceHelper
import com.example.ui.screens.dashboard.components.UnifiedEmptyState
import com.example.ui.screens.dashboard.components.UnifiedLoadingIndicator
import com.example.ui.screens.dashboard.viewmodels.MedicalDashboardViewModel
import com.example.utils.BookingStatus
import com.example.utils.VisualThemePalette
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalDashboard(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit,
    initialTab: Int = 0,
    medicalViewModel: MedicalDashboardViewModel = hiltViewModel()
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 5)) }

    val stores by viewModel.stores.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val liveAccount = remember(stores, providers, account) {
        stores.find { it.id == account.id }
            ?.let { UnifiedBusinessAccount.fromStore(it, "stores") }
            ?: providers.find { it.id == account.id }
                ?.let { UnifiedBusinessAccount.fromProvider(it) }
            ?: account
    }

    LaunchedEffect(liveAccount.id) {
        medicalViewModel.initialize(liveAccount.id)
    }

    val uiState by medicalViewModel.uiState.collectAsState()
    val doctors by medicalViewModel.doctors.collectAsState()
    val allBookings by viewModel.bookings.collectAsState()

    val cleanAccPhone = remember(liveAccount.phone) {
        AppPreferenceHelper.normalizePhoneNumber(liveAccount.phone)
    }
    val medicalBookings = remember(allBookings, liveAccount.id, cleanAccPhone) {
        allBookings.filter { b ->
            b.providerId == liveAccount.id ||
                (cleanAccPhone.isNotBlank() &&
                    AppPreferenceHelper.normalizePhoneNumber(b.providerPhone) == cleanAccPhone)
        }
    }

    val openClientChat: (String) -> Unit = { clientIdOrPhone ->
        val matchedBooking = medicalBookings.find {
            it.clientId == clientIdOrPhone ||
                it.customerPhone == clientIdOrPhone ||
                it.clientPhone == clientIdOrPhone
        }
        val targetName = matchedBooking?.customerName?.ifBlank { matchedBooking.clientName }
            ?.ifBlank { "المريض" } ?: "المريض"
        val targetPhone = matchedBooking?.customerPhone?.ifBlank { matchedBooking.clientPhone }
            ?.ifBlank { clientIdOrPhone } ?: clientIdOrPhone
        viewModel.openOrCreateChatChannel(
            targetId = clientIdOrPhone.ifBlank { targetPhone },
            targetType = "CLIENT",
            targetName = targetName,
            targetPhone = targetPhone,
            relatedEntityId = liveAccount.id,
            relatedEntityType = "MEDICAL",
            onCreated = { channel ->
                if (channel != null) {
                    viewModel.targetChatChannelId = channel.id
                    viewModel.targetChatUserId = clientIdOrPhone.ifBlank { targetPhone }
                    viewModel.targetChatUserName = targetName
                    viewModel.targetChatRelatedEntityId = liveAccount.id
                    viewModel.targetChatRelatedEntityType = "MEDICAL"
                    viewModel.navigateToScreen(AppScreens.CHAT_DIRECT)
                }
            }
        )
    }

    val computedStats = remember(uiState.stats, medicalBookings, uiState.reviews, uiState.products, liveAccount, doctors) {
        val activeCount = medicalBookings.count {
            it.status.uppercase() in listOf(
                BookingStatus.PENDING.code,
                BookingStatus.ACCEPTED.code,
                BookingStatus.IN_PROGRESS.code,
                "APPROVED"
            )
        }
        val completedCount = medicalBookings.count {
            it.status.uppercase() in listOf(BookingStatus.COMPLETED.code, BookingStatus.PAID.code, BookingStatus.CLOSED.code)
        }
        val revenue = medicalBookings.filter {
            it.status.uppercase() in listOf(BookingStatus.COMPLETED.code, BookingStatus.PAID.code, BookingStatus.CLOSED.code)
        }.sumOf { if (it.totalAmount > 0.0) it.totalAmount else it.price }
        val avgRating = if (uiState.reviews.isNotEmpty()) {
            uiState.reviews.map { it.rating }.average()
        } else {
            liveAccount.rating.toDouble()
        }
        DashboardStatsEntity(
            totalViews = uiState.stats.totalViews,
            activeBookingsCount = maxOf(uiState.stats.activeBookingsCount, activeCount),
            completedBookingsCount = maxOf(uiState.stats.completedBookingsCount, completedCount),
            averageRating = avgRating,
            totalReviewsCount = maxOf(uiState.stats.totalReviewsCount, maxOf(uiState.reviews.size, liveAccount.numReviews)),
            totalRevenueYer = maxOf(uiState.stats.totalRevenueYer, revenue),
            totalProductsCount = maxOf(uiState.stats.totalProductsCount, uiState.products.size + doctors.size),
            pendingRequestsCount = medicalBookings.count { it.status.equals(BookingStatus.PENDING.code, ignoreCase = true) }
        )
    }

    var showAddDoctorDialog by remember { mutableStateOf(false) }
    var docNameInput by remember { mutableStateOf("") }
    var docSpecInput by remember { mutableStateOf("") }
    var docHoursInput by remember { mutableStateOf("") }

    val tabs = listOf(
        "العيادات والأطباء 🩺",
        "الحجوزات الطبية 📅",
        "الخدمات الطبية 💊",
        "تقييمات المرضى ⭐",
        "الإحصائيات 📊",
        "الملف التعريفي 📝"
    )

    LaunchedEffect(medicalViewModel) {
        medicalViewModel.eventFlow.collect { event ->
            when (event) {
                is DashboardEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "لوحة تحكم المركز الطبي 🩺", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
                        Text(text = liveAccount.name, fontSize = 11.sp, color = themeColors.textSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = themeColors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = themeColors.surface)
            )
        },
        containerColor = themeColors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = themeColors.surface,
                contentColor = themeColors.accent,
                edgePadding = 12.dp
            ) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == index) themeColors.accent else themeColors.textSecondary
                            )
                        }
                    )
                }
            }

            if (uiState.isLoading) {
                UnifiedLoadingIndicator(themeColors = themeColors)
            } else {
                when (activeTab) {
                    0 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🩺 أطباء المركز والعيادات (${doctors.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
                                Button(
                                    onClick = { showAddDoctorDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("إضافة طبيب 🩺", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (doctors.isEmpty()) {
                                UnifiedEmptyState(
                                    title = "لا يوجد أطباء مضافين حالياً",
                                    description = "أضف أطباء المركز وتخصصاتهم لتمكين المرضى من حجز المواعيد.",
                                    iconText = "🩺",
                                    actionLabel = "إضافة طبيب 🩺",
                                    onActionClick = { showAddDoctorDialog = true },
                                    themeColors = themeColors
                                )
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(doctors, key = { it.id }) { doc ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text(text = "د. ${doc.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
                                                    Text(text = "التخصص: ${doc.specialty}", fontSize = 12.sp, color = themeColors.accent)
                                                    if (doc.workingHours.isNotBlank()) {
                                                        Text(text = "أوقات الدوام: ${doc.workingHours}", fontSize = 11.sp, color = themeColors.textSecondary)
                                                    }
                                                }
                                                IconButton(onClick = { medicalViewModel.deleteDoctor(doc.id) }) {
                                                    Text("🗑️", fontSize = 16.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        TabBookingsOrders(
                            bookings = medicalBookings,
                            themeColors = themeColors,
                            onAcceptBooking = { bId -> viewModel.updateBookingStatus(bId, BookingStatus.ACCEPTED.code) },
                            onRejectBooking = { bId, reason -> viewModel.updateBookingStatus(bId, BookingStatus.REJECTED.code, reason) },
                            onStartProgress = { bId -> viewModel.updateBookingStatus(bId, BookingStatus.IN_PROGRESS.code) },
                            onCompleteBooking = { bId -> viewModel.updateBookingStatus(bId, BookingStatus.COMPLETED.code) },
                            onChatWithClient = openClientChat
                        )
                    }
                    2 -> {
                        TabProductsServices(
                            products = uiState.products,
                            titleLabel = "الفحوصات والخدمات الطبية",
                            addButtonLabel = "إضافة خدمة طبية 💊",
                            themeColors = themeColors,
                            onAddProduct = { title, price, desc, img ->
                                medicalViewModel.addMedicalService(title, price, desc, img)
                            },
                            onAddProductRich = { title, price, desc, img, category, isAvail, isOffer, discount ->
                                medicalViewModel.addMedicalService(
                                    title = title,
                                    priceYer = price,
                                    description = desc,
                                    imageUrl = img,
                                    category = category.ifBlank { "MEDICAL" },
                                    isAvailable = isAvail,
                                    isOffer = isOffer,
                                    discountPercent = discount
                                )
                            },
                            onDeleteProduct = { id ->
                                medicalViewModel.deleteMedicalService(id)
                            },
                            onUpdateProduct = { updated ->
                                medicalViewModel.updateMedicalService(updated)
                            }
                        )
                    }
                    3 -> {
                        TabReviewsFeedback(
                            reviews = uiState.reviews,
                            themeColors = themeColors,
                            onReplySubmit = { revId, reply ->
                                medicalViewModel.replyToReview(revId, reply)
                            }
                        )
                    }
                    4 -> {
                        TabStatisticsGrowth(
                            stats = computedStats,
                            themeColors = themeColors
                        )
                    }
                    5 -> {
                        TabProfileEdit(
                            name = liveAccount.name,
                            phone = liveAccount.phone,
                            cityArea = liveAccount.neighborhood.ifBlank { liveAccount.cityId },
                            description = liveAccount.description,
                            workingHours = liveAccount.workingHours,
                            photoUrl = liveAccount.logoImage,
                            coverUrl = liveAccount.coverImage,
                            isAvailable = liveAccount.rawStore?.isActive ?: liveAccount.isActive,
                            rating = liveAccount.rating.toDouble(),
                            reviewCount = liveAccount.numReviews,
                            themeColors = themeColors,
                            onChangePhoto = { url ->
                                medicalViewModel.updateProfile(
                                    name = liveAccount.name,
                                    cityArea = liveAccount.neighborhood.ifBlank { liveAccount.cityId },
                                    description = liveAccount.description,
                                    workingHours = liveAccount.workingHours,
                                    isAvailable = liveAccount.rawStore?.isActive ?: liveAccount.isActive,
                                    photoUrl = url
                                )
                            },
                            onChangeCover = { url ->
                                medicalViewModel.updateProfile(
                                    name = liveAccount.name,
                                    cityArea = liveAccount.neighborhood.ifBlank { liveAccount.cityId },
                                    description = liveAccount.description,
                                    workingHours = liveAccount.workingHours,
                                    isAvailable = liveAccount.rawStore?.isActive ?: liveAccount.isActive,
                                    coverUrl = url
                                )
                            },
                            onSaveProfile = { name, _, city, desc, hours, available ->
                                medicalViewModel.updateProfile(name, city, desc, hours, available)
                            },
                            onChangePassword = { oldP, newP ->
                                medicalViewModel.changePassword(oldP, newP)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDoctorDialog) {
        AlertDialog(
            onDismissRequest = { showAddDoctorDialog = false },
            containerColor = themeColors.surface,
            titleContentColor = themeColors.textPrimary,
            textContentColor = themeColors.textPrimary,
            title = { Text("إضافة طبيب جديد للمركز", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = docNameInput,
                        onValueChange = { docNameInput = it },
                        label = { Text("اسم الطبيب", color = themeColors.textSecondary) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = docSpecInput,
                        onValueChange = { docSpecInput = it },
                        label = { Text("التخصص الطبي", color = themeColors.textSecondary) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = docHoursInput,
                        onValueChange = { docHoursInput = it },
                        label = { Text("ساعات الدوام (مثال: 4 م - 8 م)", color = themeColors.textSecondary) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (docNameInput.isNotBlank()) {
                            medicalViewModel.addDoctor(docNameInput, docSpecInput, docHoursInput)
                            docNameInput = ""
                            docSpecInput = ""
                            docHoursInput = ""
                            showAddDoctorDialog = false
                        } else {
                            Toast.makeText(context, "يرجى إدخال اسم الطبيب", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ الطبيب", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDoctorDialog = false }) {
                    Text("إلغاء", color = themeColors.textSecondary)
                }
            }
        )
    }
}

