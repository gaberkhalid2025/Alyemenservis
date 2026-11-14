package com.example.ui.screens.dashboard

import android.widget.Toast
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
import com.example.domain.entities.GalleryAlbumEntity
import com.example.ui.MainViewModel
import com.example.ui.screens.dashboard.components.UnifiedLoadingIndicator
import com.example.ui.screens.dashboard.viewmodels.TechnicianDashboardViewModel
import com.example.utils.VisualThemePalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicianDashboard(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    var activeTab by remember { mutableIntStateOf(0) }

    val techViewModel = remember(account.id, appContext) {
        TechnicianDashboardViewModel(
            ownerId = account.id,
            dashboardRepository = DashboardRepositoryImpl(appContext),
            productsRepository = ProductsRepositoryImpl(appContext),
            ratingsRepository = RatingsRepositoryImpl(appContext),
            galleryRepository = GalleryRepositoryImpl(appContext)
        )
    }

    val uiState by techViewModel.uiState.collectAsState()
    val allBookings by viewModel.bookings.collectAsState()

    val providerBookings = remember(allBookings, account) {
        allBookings.filter { b ->
            b.providerId == account.id || b.providerPhone == account.phone
        }
    }

    val tabs = listOf(
        "طلبات الحجز 📅",
        "حجوزاتي الحالية 📋",
        "خدماتي وأسعاري 🛠️",
        "معرض الأعمال 📸",
        "الملف الشخصي 👤"
    )

    LaunchedEffect(techViewModel) {
        techViewModel.eventFlow.collect { event ->
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
                        Text(text = "لوحة تحكم الفني 🛠️", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
                        Text(text = account.name, fontSize = 11.sp, color = themeColors.textSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = themeColors.textPrimary)
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
                        TabBookingsOrders(
                            bookings = providerBookings.filter { it.status == com.example.utils.BookingStatus.PENDING.code },
                            themeColors = themeColors,
                            onAcceptBooking = { bId ->
                                viewModel.updateBookingStatus(bId, com.example.utils.BookingStatus.ACCEPTED.code)
                                Toast.makeText(context, "تم قبول طلب الحجز وإشعار العميل ✅", Toast.LENGTH_SHORT).show()
                            },
                            onRejectBooking = { bId, reason ->
                                viewModel.updateBookingStatus(bId, com.example.utils.BookingStatus.REJECTED.code)
                                Toast.makeText(context, "تم رفض الطلب وإرسال السبب للعميل ❌", Toast.LENGTH_SHORT).show()
                            },
                            onStartProgress = { bId -> viewModel.updateBookingStatus(bId, com.example.utils.BookingStatus.IN_PROGRESS.code) },
                            onCompleteBooking = { bId -> viewModel.updateBookingStatus(bId, com.example.utils.BookingStatus.COMPLETED.code) },
                            onChatWithClient = { phone ->
                                viewModel.openOrCreateChatChannel(
                                    targetId = account.id,
                                    targetType = "PROVIDER",
                                    targetName = account.name,
                                    targetPhone = phone,
                                    onCreated = {}
                                )
                            }
                        )
                    }
                    1 -> {
                        TabBookingsOrders(
                            bookings = providerBookings.filter {
                                it.status in listOf(
                                    com.example.utils.BookingStatus.ACCEPTED.code,
                                    "APPROVED",
                                    com.example.utils.BookingStatus.IN_PROGRESS.code
                                )
                            },
                            themeColors = themeColors,
                            onAcceptBooking = { bId -> viewModel.updateBookingStatus(bId, com.example.utils.BookingStatus.ACCEPTED.code) },
                            onRejectBooking = { bId, _ -> viewModel.updateBookingStatus(bId, com.example.utils.BookingStatus.REJECTED.code) },
                            onStartProgress = { bId ->
                                viewModel.updateBookingStatus(bId, com.example.utils.BookingStatus.IN_PROGRESS.code)
                                Toast.makeText(context, "تم بدء تنفيذ الخدمة ⚙️", Toast.LENGTH_SHORT).show()
                            },
                            onCompleteBooking = { bId ->
                                viewModel.updateBookingStatus(bId, com.example.utils.BookingStatus.COMPLETED.code)
                                Toast.makeText(context, "تم إكمال الخدمة بنجاح 🏁", Toast.LENGTH_SHORT).show()
                            },
                            onChatWithClient = { phone ->
                                viewModel.openOrCreateChatChannel(
                                    targetId = account.id,
                                    targetType = "PROVIDER",
                                    targetName = account.name,
                                    targetPhone = phone,
                                    onCreated = {}
                                )
                            }
                        )
                    }
                    2 -> {
                        TabProductsServices(
                            products = uiState.products,
                            titleLabel = "قائمة الخدمات والأسعار",
                            addButtonLabel = "إضافة خدمة جديدة 🛠️",
                            themeColors = themeColors,
                            onAddProduct = { title, price, desc, img ->
                                techViewModel.addNewProductService(title, price, desc, img)
                            },
                            onDeleteProduct = { id ->
                                techViewModel.deleteProduct(id)
                            },
                            onUpdateProduct = { updated ->
                                techViewModel.updateProduct(updated)
                            }
                        )
                    }
                    3 -> {
                        TabGalleryAlbums(
                            albums = uiState.galleryAlbums,
                            themeColors = themeColors,
                            onAddPhoto = { img ->
                                techViewModel.saveGalleryAlbum(context, img)
                            },
                            onDeletePhoto = { id -> 
                                techViewModel.deleteGalleryAlbum(id)
                            }
                        )
                    }
                    4 -> {
                        TabProfileEdit(
                            name = account.name,
                            phone = account.phone,
                            cityArea = account.neighborhood.ifBlank { account.cityId },
                            description = account.description,
                            workingHours = account.workingHours,
                            photoUrl = account.logoImage,
                            coverUrl = account.coverImage,
                            isAvailable = account.rawProvider?.isAvailable ?: true,
                            rating = account.rating.toDouble(),
                            reviewCount = account.numReviews,
                            themeColors = themeColors,
                            onChangePhoto = { url ->
                                techViewModel.updateProfile(
                                    name = account.name,
                                    cityArea = account.neighborhood.ifBlank { account.cityId },
                                    description = account.description,
                                    workingHours = account.workingHours,
                                    isAvailable = account.rawProvider?.isAvailable ?: true,
                                    photoUrl = url
                                )
                            },
                            onChangeCover = { url ->
                                techViewModel.updateProfile(
                                    name = account.name,
                                    cityArea = account.neighborhood.ifBlank { account.cityId },
                                    description = account.description,
                                    workingHours = account.workingHours,
                                    isAvailable = account.rawProvider?.isAvailable ?: true,
                                    coverUrl = url
                                )
                            },
                            onSaveProfile = { name, _, city, desc, hours, available ->
                                techViewModel.updateProfile(name, city, desc, hours, available)
                            },
                            onChangePassword = { oldP, newP ->
                                techViewModel.changePassword(oldP, newP)
                            }
                        )
                    }
                }
            }
        }
    }
}
