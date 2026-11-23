package com.example.ui.screens.dashboard

import android.widget.Toast
import androidx.activity.compose.BackHandler
import com.example.ui.*
import androidx.compose.foundation.layout.*
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
import com.example.ui.screens.dashboard.components.UnifiedLoadingIndicator
import com.example.ui.screens.dashboard.viewmodels.PropertyDashboardViewModel
import com.example.utils.BookingStatus
import com.example.utils.VisualThemePalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDashboard(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit,
    initialTab: Int = 0
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    val appContext = context.applicationContext
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 4)) }

    val properties by viewModel.properties.collectAsState()
    val liveAccount = remember(properties, account) {
        properties.find { it.id == account.id }
            ?.let { UnifiedBusinessAccount.fromProperty(it) }
            ?: account
    }

    val propertyViewModel = remember(liveAccount.id, appContext) {
        PropertyDashboardViewModel(
            ownerId = liveAccount.id,
            dashboardRepository = DashboardRepositoryImpl(appContext),
            productsRepository = ProductsRepositoryImpl(appContext),
            ratingsRepository = RatingsRepositoryImpl(appContext)
        )
    }

    DisposableEffect(propertyViewModel) {
        onDispose { propertyViewModel.dispose() }
    }

    val uiState by propertyViewModel.uiState.collectAsState()
    val allBookings by viewModel.bookings.collectAsState()

    val cleanAccPhone = remember(liveAccount.phone) {
        AppPreferenceHelper.normalizePhoneNumber(liveAccount.phone)
    }
    val propertyRequests = remember(allBookings, liveAccount.id, cleanAccPhone) {
        allBookings.filter { b ->
            b.providerId == liveAccount.id ||
                (cleanAccPhone.isNotBlank() &&
                    AppPreferenceHelper.normalizePhoneNumber(b.providerPhone) == cleanAccPhone)
        }
    }

    val openClientChat: (String) -> Unit = { clientIdOrPhone ->
        val matchedBooking = propertyRequests.find {
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
            relatedEntityType = "REAL_ESTATE",
            onCreated = { channel ->
                if (channel != null) {
                    viewModel.targetChatChannelId = channel.id
                    viewModel.targetChatUserId = clientIdOrPhone.ifBlank { targetPhone }
                    viewModel.targetChatUserName = targetName
                    viewModel.targetChatRelatedEntityId = liveAccount.id
                    viewModel.targetChatRelatedEntityType = "REAL_ESTATE"
                    viewModel.navigateToScreen(AppScreens.CHAT_DIRECT)
                }
            }
        )
    }

    val computedStats = remember(uiState.stats, propertyRequests, uiState.reviews, uiState.products, liveAccount) {
        val activeCount = propertyRequests.count {
            it.status.uppercase() in listOf(
                BookingStatus.PENDING.code,
                BookingStatus.ACCEPTED.code,
                BookingStatus.IN_PROGRESS.code,
                "APPROVED"
            )
        }
        val completedCount = propertyRequests.count {
            it.status.uppercase() in listOf(BookingStatus.COMPLETED.code, BookingStatus.PAID.code, BookingStatus.CLOSED.code)
        }
        val revenue = propertyRequests.filter {
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
            totalProductsCount = maxOf(uiState.stats.totalProductsCount, uiState.products.size),
            pendingRequestsCount = propertyRequests.count { it.status.equals(BookingStatus.PENDING.code, ignoreCase = true) }
        )
    }

    val tabs = listOf(
        "العقارات المعروضة 🏠",
        "طلبات المعاينة 👁️",
        "تقييمات العملاء ⭐",
        "الإحصائيات 📊",
        "الملف التعريفي 📝"
    )

    LaunchedEffect(propertyViewModel) {
        propertyViewModel.eventFlow.collect { event ->
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
                        Text(text = "لوحة تحكم العقارات 🏠", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
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
                            titleLabel = "قائمة العقارات المعروضة",
                            addButtonLabel = "إضافة عقار جديد 🏠",
                            themeColors = themeColors,
                            onAddProduct = { title, price, desc, img ->
                                propertyViewModel.addProperty(title, price, desc, img)
                            },
                            onAddProductRich = { title, price, desc, img, category, isAvail, isOffer, discount ->
                                propertyViewModel.addProperty(
                                    title = title,
                                    priceYer = price,
                                    description = desc,
                                    imageUrl = img,
                                    category = category.ifBlank { "REAL_ESTATE" },
                                    isAvailable = isAvail,
                                    isOffer = isOffer,
                                    discountPercent = discount
                                )
                            },
                            onDeleteProduct = { id ->
                                propertyViewModel.deleteProperty(id)
                            },
                            onUpdateProduct = { updated ->
                                propertyViewModel.updateProperty(updated)
                            }
                        )
                    }
                    1 -> {
                        TabBookingsOrders(
                            bookings = propertyRequests,
                            themeColors = themeColors,
                            onAcceptBooking = { bId -> viewModel.updateBookingStatus(bId, BookingStatus.ACCEPTED.code) },
                            onRejectBooking = { bId, reason -> viewModel.updateBookingStatus(bId, BookingStatus.REJECTED.code, reason) },
                            onStartProgress = { bId -> viewModel.updateBookingStatus(bId, BookingStatus.IN_PROGRESS.code) },
                            onCompleteBooking = { bId -> viewModel.updateBookingStatus(bId, BookingStatus.COMPLETED.code) },
                            onChatWithClient = openClientChat
                        )
                    }
                    2 -> {
                        TabReviewsFeedback(
                            reviews = uiState.reviews,
                            themeColors = themeColors,
                            onReplySubmit = { revId, reply ->
                                propertyViewModel.replyToReview(revId, reply)
                            }
                        )
                    }
                    3 -> {
                        TabStatisticsGrowth(
                            stats = computedStats,
                            themeColors = themeColors
                        )
                    }
                    4 -> {
                        TabProfileEdit(
                            name = liveAccount.name,
                            phone = liveAccount.phone,
                            cityArea = liveAccount.neighborhood.ifBlank { liveAccount.cityId },
                            description = liveAccount.description,
                            workingHours = liveAccount.workingHours,
                            photoUrl = liveAccount.logoImage,
                            coverUrl = liveAccount.coverImage,
                            isAvailable = liveAccount.rawProperty?.isActive ?: liveAccount.isActive,
                            rating = liveAccount.rating.toDouble(),
                            reviewCount = liveAccount.numReviews,
                            latitude = liveAccount.latitude,
                            longitude = liveAccount.longitude,
                            themeColors = themeColors,
                            onChangePhoto = { url ->
                                propertyViewModel.updateProfile(
                                    name = liveAccount.name,
                                    cityArea = liveAccount.neighborhood.ifBlank { liveAccount.cityId },
                                    description = liveAccount.description,
                                    workingHours = liveAccount.workingHours,
                                    isAvailable = liveAccount.rawProperty?.isActive ?: liveAccount.isActive,
                                    photoUrl = url
                                )
                            },
                            onChangeCover = { url ->
                                propertyViewModel.updateProfile(
                                    name = liveAccount.name,
                                    cityArea = liveAccount.neighborhood.ifBlank { liveAccount.cityId },
                                    description = liveAccount.description,
                                    workingHours = liveAccount.workingHours,
                                    isAvailable = liveAccount.rawProperty?.isActive ?: liveAccount.isActive,
                                    coverUrl = url
                                )
                            },
                            onSaveLocation = { lat, lng ->
                                propertyViewModel.updateProfile(
                                    name = liveAccount.name,
                                    cityArea = liveAccount.neighborhood.ifBlank { liveAccount.cityId },
                                    description = liveAccount.description,
                                    workingHours = liveAccount.workingHours,
                                    isAvailable = liveAccount.rawProperty?.isActive ?: liveAccount.isActive,
                                    latitude = lat,
                                    longitude = lng
                                )
                            },
                            onSaveProfileWithLocation = { name, editedPhone, city, desc, hours, available, lat, lng ->
                                propertyViewModel.updateProfile(
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
                                propertyViewModel.updateProfile(name, city, desc, hours, available, phone = editedPhone)
                            },
                            onChangePassword = { oldP, newP ->
                                propertyViewModel.changePassword(oldP, newP)
                            }
                        )
                    }
                }
            }
        }
    }
}

