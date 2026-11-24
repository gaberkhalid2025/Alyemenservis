package com.example.ui.screens.dashboard

import android.widget.Toast
import androidx.activity.compose.BackHandler
import com.example.ui.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UnifiedBusinessAccount
import com.example.data.repositories.*
import com.example.domain.entities.DashboardStatsEntity
import com.example.ui.MainViewModel
import com.example.ui.helpers.AppPreferenceHelper
import com.example.ui.screens.dashboard.components.InventoryManager
import com.example.ui.screens.dashboard.components.LoyaltyManager
import com.example.ui.screens.dashboard.components.StaffManager
import com.example.ui.screens.dashboard.components.UnifiedLoadingIndicator
import com.example.ui.screens.dashboard.viewmodels.StoreDashboardViewModel
import com.example.utils.BookingStatus
import com.example.utils.VisualThemePalette
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDashboard(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit,
    initialTab: Int = 0,
    storeViewModel: StoreDashboardViewModel = hiltViewModel()
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 5)) }
    var activeMgmtSection by remember { mutableIntStateOf(0) }

    val stores by viewModel.stores.collectAsState()
    val liveAccount = remember(stores, account) {
        stores.find { it.id == account.id }
            ?.let { UnifiedBusinessAccount.fromStore(it, "stores") }
            ?: account
    }

    LaunchedEffect(liveAccount.id) {
        storeViewModel.initialize(liveAccount.id)
    }

    val uiState by storeViewModel.uiState.collectAsState()
    val allBookings by viewModel.bookings.collectAsState()

    val cleanAccPhone = remember(liveAccount.phone) {
        AppPreferenceHelper.normalizePhoneNumber(liveAccount.phone)
    }
    val storeOrders = remember(allBookings, liveAccount.id, cleanAccPhone) {
        allBookings.filter { b ->
            b.providerId == liveAccount.id ||
                (cleanAccPhone.isNotBlank() &&
                    AppPreferenceHelper.normalizePhoneNumber(b.providerPhone) == cleanAccPhone)
        }
    }

    val openClientChat: (String) -> Unit = { clientIdOrPhone ->
        val matchedBooking = storeOrders.find {
            it.clientId == clientIdOrPhone ||
                it.customerPhone == clientIdOrPhone ||
                it.clientPhone == clientIdOrPhone
        }
        val targetName = matchedBooking?.customerName?.ifBlank { matchedBooking.clientName }
            ?.ifBlank { "العميل" } ?: "العميل"
        val targetPhone = matchedBooking?.customerPhone?.ifBlank { matchedBooking.clientPhone }
            ?.ifBlank { clientIdOrPhone } ?: clientIdOrPhone
        viewModel.openOrCreateChatChannel(
            targetId = clientIdOrPhone.ifBlank { targetPhone },
            targetType = "CLIENT",
            targetName = targetName,
            targetPhone = targetPhone,
            relatedEntityId = liveAccount.id,
            relatedEntityType = "STORE",
            onCreated = { channel ->
                if (channel != null) {
                    viewModel.targetChatChannelId = channel.id
                    viewModel.targetChatUserId = clientIdOrPhone.ifBlank { targetPhone }
                    viewModel.targetChatUserName = targetName
                    viewModel.targetChatRelatedEntityId = liveAccount.id
                    viewModel.targetChatRelatedEntityType = "STORE"
                    viewModel.navigateToScreen(AppScreens.CHAT_DIRECT)
                }
            }
        )
    }

    val computedStats = remember(uiState.stats, storeOrders, uiState.reviews, uiState.products, liveAccount) {
        val activeCount = storeOrders.count {
            it.status.uppercase() in listOf(
                BookingStatus.PENDING.code,
                BookingStatus.ACCEPTED.code,
                BookingStatus.IN_PROGRESS.code,
                "APPROVED",
                "IN_PREPARATION",
                "READY"
            )
        }
        val completedCount = storeOrders.count {
            it.status.uppercase() in listOf(BookingStatus.COMPLETED.code, BookingStatus.PAID.code, BookingStatus.CLOSED.code, "DELIVERED")
        }
        val revenue = storeOrders.filter {
            it.status.uppercase() in listOf(BookingStatus.COMPLETED.code, BookingStatus.PAID.code, BookingStatus.CLOSED.code, "DELIVERED")
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
            totalProductsCount = maxOf(uiState.stats.totalProductsCount, uiState.products.size),
            pendingRequestsCount = storeOrders.count { it.status.equals(BookingStatus.PENDING.code, ignoreCase = true) }
        )
    }

    val tabs = listOf(
        "المنتجات والمخزون 📦",
        "طلبات العملاء 🛍️",
        "العروض والكوبونات 🏷️",
        "التقييمات والآراء ⭐",
        "النمو والإدارة 📊",
        "الملف التجاري 📝"
    )

    LaunchedEffect(storeViewModel) {
        storeViewModel.eventFlow.collect { event ->
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
                        Text(text = "لوحة تحكم المتجر 🛍️", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
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
                        TabProductsServices(
                            products = uiState.products,
                            titleLabel = "كتالوج المنتجات والمخزون",
                            addButtonLabel = "إضافة منتج جديد 📦",
                            themeColors = themeColors,
                            onAddProduct = { title, price, desc, img ->
                                storeViewModel.addProduct(title, price, desc, img)
                            },
                            onAddProductRich = { title, price, desc, img, category, isAvail, isOffer, discount ->
                                storeViewModel.addProduct(
                                    title = title,
                                    priceYer = price,
                                    description = desc,
                                    imageUrl = img,
                                    category = category.ifBlank { "STORE" },
                                    isAvailable = isAvail,
                                    isOffer = isOffer,
                                    discountPercent = discount
                                )
                            },
                            onDeleteProduct = { id ->
                                storeViewModel.deleteProduct(id)
                            },
                            onUpdateProduct = { updated ->
                                storeViewModel.updateProduct(updated)
                            }
                        )
                    }
                    1 -> {
                        TabBookingsOrders(
                            bookings = storeOrders,
                            themeColors = themeColors,
                            onAcceptBooking = { bId -> viewModel.updateBookingStatus(bId, "IN_PREPARATION") },
                            onRejectBooking = { bId, reason -> viewModel.updateBookingStatus(bId, BookingStatus.REJECTED.code, reason) },
                            onStartProgress = { bId -> viewModel.updateBookingStatus(bId, "READY") },
                            onCompleteBooking = { bId -> viewModel.updateBookingStatus(bId, "DELIVERED") },
                            onChatWithClient = openClientChat,
                            onUpdateOrderStatus = { bId, status ->
                                viewModel.updateBookingStatus(bId, status)
                                Toast.makeText(context, "تم تحديث حالة الطلب ✅", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    2 -> {
                        TabOffersCoupons(
                            storeId = liveAccount.id,
                            themeColors = themeColors
                        )
                    }
                    3 -> {
                        TabReviewsFeedback(
                            reviews = uiState.reviews,
                            themeColors = themeColors,
                            onReplySubmit = { revId, reply ->
                                storeViewModel.replyToReview(revId, reply)
                            }
                        )
                    }
                    4 -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            val subSections = listOf("الإحصائيات 📊", "المخزون 📦", "نقاط الولاء 💎", "طاقم العمل 👥")
                            ScrollableTabRow(
                                selectedTabIndex = activeMgmtSection,
                                containerColor = themeColors.surface.copy(alpha = 0.7f),
                                contentColor = themeColors.accent,
                                edgePadding = 12.dp
                            ) {
                                subSections.forEachIndexed { idx, title ->
                                    Tab(
                                        selected = activeMgmtSection == idx,
                                        onClick = { activeMgmtSection = idx },
                                        text = { Text(title, fontSize = 11.5.sp) }
                                    )
                                }
                            }
                            when (activeMgmtSection) {
                                0 -> TabStatisticsGrowth(stats = computedStats, themeColors = themeColors)
                                1 -> Box(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)) {
                                    InventoryManager(ownerId = liveAccount.id, themeColors = themeColors)
                                }
                                2 -> Box(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)) {
                                    LoyaltyManager(ownerId = liveAccount.id, themeColors = themeColors)
                                }
                                3 -> Box(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)) {
                                    StaffManager(ownerId = liveAccount.id, themeColors = themeColors)
                                }
                            }
                        }
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
                            latitude = liveAccount.latitude,
                            longitude = liveAccount.longitude,
                            themeColors = themeColors,
                            onChangePhoto = { url ->
                                storeViewModel.updateProfile(
                                    name = liveAccount.name,
                                    cityArea = liveAccount.neighborhood.ifBlank { liveAccount.cityId },
                                    description = liveAccount.description,
                                    workingHours = liveAccount.workingHours,
                                    isAvailable = liveAccount.rawStore?.isActive ?: liveAccount.isActive,
                                    photoUrl = url
                                )
                            },
                            onChangeCover = { url ->
                                storeViewModel.updateProfile(
                                    name = liveAccount.name,
                                    cityArea = liveAccount.neighborhood.ifBlank { liveAccount.cityId },
                                    description = liveAccount.description,
                                    workingHours = liveAccount.workingHours,
                                    isAvailable = liveAccount.rawStore?.isActive ?: liveAccount.isActive,
                                    coverUrl = url
                                )
                            },
                            onSaveLocation = { lat, lng ->
                                storeViewModel.updateProfile(
                                    name = liveAccount.name,
                                    cityArea = liveAccount.neighborhood.ifBlank { liveAccount.cityId },
                                    description = liveAccount.description,
                                    workingHours = liveAccount.workingHours,
                                    isAvailable = liveAccount.rawStore?.isActive ?: liveAccount.isActive,
                                    latitude = lat,
                                    longitude = lng
                                )
                            },
                            onSaveProfileWithLocation = { name, editedPhone, city, desc, hours, available, lat, lng ->
                                storeViewModel.updateProfile(
                                    name = name,
                                    cityArea = city,
                                    description = desc,
                                    workingHours = hours,
                                    isAvailable = available,
                                    phone = editedPhone,
                                    latitude = lat,
                                    longitude = lng
                                )
                            },
                            onSaveProfile = { name, editedPhone, city, desc, hours, available ->
                                storeViewModel.updateProfile(name, city, desc, hours, available, phone = editedPhone)
                            },
                            onChangePassword = { oldP, newP ->
                                storeViewModel.changePassword(oldP, newP)
                            }
                        )
                    }
                }
            }
        }
    }
}
