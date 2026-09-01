package com.example.ui.navigation

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.CreateBookingScreen
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.screens.admin.AdminPanelLayout
import com.example.ui.screens.assistant.SmartAssistantDialogView
import com.example.ui.screens.bookings.BookingsScreenLayout
import com.example.ui.screens.bookings.InstantRequestsScreen
import com.example.ui.screens.bookings.OrdersScreenLayout
import com.example.ui.screens.chat.ChatListScreen
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.entities.*
import com.example.ui.screens.home.FavoritesScreenLayout
import com.example.ui.screens.map.MapScreenLayout
import com.example.ui.screens.notifications.UserNotificationsDialogView
import com.example.ui.screens.owner.OwnerDashboardScreen
import com.example.ui.screens.status.StatusScreen
import com.example.ui.screens.urgent.UrgentOffersList
import com.example.ui.screens.urgent.UrgentRequestDetailsScreen
import com.example.utils.VisualThemePalette

@Composable
fun AppNavigator(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    permissionLauncher: ActivityResultLauncher<Array<String>>? = null,
    locationPermissions: Array<String> = emptyArray()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()

    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showAssistantDialog by remember { mutableStateOf(false) }
    var showRequestServiceModal by remember { mutableStateOf(false) }
    var showRestoreAccountDialog by remember { mutableStateOf(false) }

    val providers by viewModel.providers.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategoryId.collectAsState()

    if (settingsState.isMaintenanceActive && viewModel.adminRole.collectAsState().value == "GUEST") {
        MaintenanceSplashView(settingsState = settingsState, themeColors = themeColors, viewModel = viewModel)
        return
    }

    Scaffold(
        topBar = {
            if (ScreenRoutes.showTopBar(currentScreen)) {
                AppHeaderBar(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onNotificationsClick = { showNotificationsDialog = true },
                    onChatsClick = { viewModel.navigateToScreen(AppScreens.CHAT_LIST) },
                    onMenuClick = { showRestoreAccountDialog = true }
                )
            }
        },
        bottomBar = {
            if (ScreenRoutes.showBottomBar(currentScreen)) {
                AppFooterBar(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onInfoClick = { viewModel.navigateToScreen(AppScreens.ABOUT_APP) }
                )
            }
        },
        containerColor = themeColors.background
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreens.USER_BROWSE, AppScreens.HOME -> {
                    val banners by viewModel.banners.collectAsState()
                    val selectedCatName = selectedCategory ?: ""
                    val filteredProviders = if (selectedCatName.isNotEmpty() && selectedCatName != "الكل") {
                        providers.filter { it.profession.contains(selectedCatName, ignoreCase = true) || it.specialization.contains(selectedCatName, ignoreCase = true) }
                    } else providers

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            if (banners.isNotEmpty()) {
                                BannerSliderView(banners = banners, themeColors = themeColors) { cat ->
                                    if (cat.isNotEmpty()) viewModel.selectCategory(cat)
                                }
                            }
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(categories) { cat ->
                                    CategoryChip(
                                        name = cat.name,
                                        icon = cat.icon,
                                        isSelected = selectedCategory == cat.name,
                                        themeColors = themeColors
                                    ) { viewModel.selectCategory(cat.name) }
                                }
                            }
                        }
                        items(filteredProviders) { provider ->
                            ProviderCard(
                                provider = provider,
                                themeColors = themeColors,
                                viewModel = viewModel,
                                onChatOpen = { viewModel.navigateToScreen(AppScreens.CHAT_DIRECT) }
                            )
                        }
                    }
                }
                AppScreens.ADMIN_PANEL -> AdminPanelLayout(viewModel = viewModel, themeColors = themeColors)
                AppScreens.OWNER_PANEL, AppScreens.PRODUCTS_MGMT_VIEW, AppScreens.PRICE_MGMT_VIEW, AppScreens.OFFERS_MGMT_VIEW, AppScreens.GALLERY_MGMT_VIEW -> OwnerDashboardScreen(
                    account = UnifiedBusinessAccount(id = "owner_1", name = "المالك", description = "منصة اليمن"),
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onNavigateTab = {},
                    onBackClick = { viewModel.navigateToScreen(AppScreens.USER_BROWSE) }
                )
                AppScreens.STATUS_VIEW -> StatusScreen(viewModel = viewModel, themeColors = themeColors)
                AppScreens.CATEGORIES_VIEW -> CategoriesScreen(viewModel = viewModel, themeColors = themeColors, onCategoryClick = { cat -> viewModel.selectCategory(cat); viewModel.navigateToScreen(AppScreens.USER_BROWSE) })
                AppScreens.STORES_VIEW -> StoresScreen(viewModel = viewModel, themeColors = themeColors, onStoreClick = {}, onChatClick = { viewModel.navigateToScreen(AppScreens.CHAT_DIRECT) }, onRequestServiceClick = { showRequestServiceModal = true })
                AppScreens.MEDICAL_VIEW -> MedicalCentersScreen(viewModel = viewModel, themeColors = themeColors, onMedicalCenterClick = {}, onChatClick = { viewModel.navigateToScreen(AppScreens.CHAT_DIRECT) }, onBookAppointmentClick = { showRequestServiceModal = true })
                AppScreens.RESTAURANTS_VIEW -> RestaurantsScreen(viewModel = viewModel, themeColors = themeColors, onRestaurantClick = {}, onChatClick = { viewModel.navigateToScreen(AppScreens.CHAT_DIRECT) }, onOrderMealClick = { showRequestServiceModal = true })
                AppScreens.PROPERTIES_VIEW -> PropertiesScreen(viewModel = viewModel, themeColors = themeColors, onPropertyClick = {}, onChatClick = { viewModel.navigateToScreen(AppScreens.CHAT_DIRECT) }, onRequestInspectionClick = { showRequestServiceModal = true })
                AppScreens.CHAT_LIST -> ChatListScreen(currentUserId = currentUserId, currentUserName = currentUserName, themeColors = themeColors, onChannelClick = { viewModel.navigateToScreen(AppScreens.CHAT_DIRECT) }, onBackClick = { viewModel.navigateToScreen(AppScreens.USER_BROWSE) })
                AppScreens.CHAT_DIRECT -> ChatScreen(currentUserId = currentUserId, currentUserName = currentUserName, themeColors = themeColors, onBackClick = { viewModel.navigateToScreen(AppScreens.USER_BROWSE) })
                AppScreens.CREATE_BOOKING -> CreateBookingScreen(onBack = { viewModel.navigateToScreen(AppScreens.USER_BROWSE) }, onBookingCreated = { viewModel.navigateToScreen(AppScreens.USER_BROWSE) })
                AppScreens.DYNAMIC_PROFILE, AppScreens.OWNER_PROFILE_VIEW, AppScreens.PROVIDER_DETAILS, AppScreens.STORE_DETAILS, AppScreens.PROPERTY_DETAILS -> DynamicPolymorphicProfileScreen(viewModel = viewModel, themeColors = themeColors, onBackClick = { viewModel.navigateToScreen(AppScreens.USER_BROWSE) })
                AppScreens.BOOKINGS_VIEW -> BookingsScreenLayout(viewModel = viewModel, themeColors = themeColors)
                AppScreens.INSTANT_REQUESTS_VIEW -> InstantRequestsScreen(viewModel = viewModel, themeColors = themeColors)
                AppScreens.ORDERS_VIEW -> OrdersScreenLayout(viewModel = viewModel, themeColors = themeColors, onRequestQuickService = { showRequestServiceModal = true })
                AppScreens.MAP_VIEW -> MapScreenLayout(
                    viewModel = viewModel,
                    onBackClick = { viewModel.navigateToScreen(AppScreens.USER_BROWSE) }
                )
                AppScreens.FAVORITES_VIEW -> FavoritesScreenLayout(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onBackClick = { viewModel.navigateToScreen(AppScreens.USER_BROWSE) }
                )
                AppScreens.URGENT_REQUEST_DETAILS -> UrgentRequestDetailsScreen(
                    requestId = viewModel.selectedRequestId,
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onNavigateBack = { viewModel.goBack() }
                )
                AppScreens.OFFERS_LIST -> UrgentOffersList(
                    offers = viewModel.requestOffers.collectAsState().value,
                    isOwner = viewModel.isProviderUser,
                    onAcceptOffer = { _ -> },
                    onContactProvider = { _, _ -> viewModel.navigateToScreen(AppScreens.CHAT_DIRECT) }
                )
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("الشاشة المعروضة: $currentScreen", color = Color.White, fontSize = 14.sp)
                    }
                }
            }

            FloatingIconsOverlay(
                settings = settingsState,
                themeColors = themeColors,
                onAssistantClick = { showAssistantDialog = true },
                onRequestServiceClick = { showRequestServiceModal = true }
            )

            if (showRestoreAccountDialog) {
                RestoreAccountDialog(viewModel = viewModel, themeColors = themeColors, onDismiss = { showRestoreAccountDialog = false })
            }
            if (showNotificationsDialog) {
                UserNotificationsDialogView(viewModel = viewModel, themeColors = themeColors, onDismiss = { showNotificationsDialog = false })
            }
            if (showAssistantDialog) {
                SmartAssistantDialogView(
                    viewModel = viewModel,
                    settings = settingsState,
                    themeColors = themeColors,
                    onDismiss = { showAssistantDialog = false },
                    onChatOpen = { viewModel.navigateToScreen(AppScreens.CHAT_DIRECT) }
                )
            }
            if (showRequestServiceModal) {
                QuickServiceRequestDialog(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onDismiss = { showRequestServiceModal = false },
                    onRequestCreated = { showRequestServiceModal = false }
                )
            }
        }
    }
}
