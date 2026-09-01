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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.CreateBookingScreen
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.screens.admin.AdminPanelLayout
import com.example.ui.screens.assistant.SmartAssistantDialogView
import com.example.ui.screens.chat.ChatListScreen
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.entities.*
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
    val context = LocalContext.current
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
            AppHeaderBar(
                viewModel = viewModel,
                themeColors = themeColors,
                onNotificationsClick = { showNotificationsDialog = true },
                onChatsClick = { viewModel.navigateTo(AppScreens.CHAT_LIST) },
                onMenuClick = { showRestoreAccountDialog = true }
            )
        },
        bottomBar = {
            if (!ScreenRoutes.isFullScreen(currentScreen)) {
                AppFooterBar(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onInfoClick = { viewModel.navigateTo(AppScreens.ABOUT_APP) }
                )
            }
        },
        containerColor = themeColors.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreens.USER_BROWSE -> {
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
                                onChatOpen = { phone -> viewModel.navigateTo(AppScreens.CHAT_DIRECT) }
                            )
                        }
                    }
                }
                AppScreens.ADMIN_PANEL -> AdminPanelLayout(viewModel = viewModel, themeColors = themeColors)
                AppScreens.OWNER_PANEL -> OwnerDashboardScreen(
                    account = UnifiedBusinessAccount(id = "owner_1", name = "المالك", description = "منصة اليمن"),
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onNavigateTab = {},
                    onBackClick = { viewModel.navigateTo(AppScreens.USER_BROWSE) }
                )
                AppScreens.STATUS_VIEW -> StatusScreen(viewModel = viewModel, themeColors = themeColors)
                AppScreens.CATEGORIES_VIEW -> CategoriesScreen(viewModel = viewModel, themeColors = themeColors, onCategoryClick = { cat -> viewModel.selectCategory(cat); viewModel.navigateTo(AppScreens.USER_BROWSE) })
                AppScreens.STORES_VIEW -> StoresScreen(viewModel = viewModel, themeColors = themeColors, onStoreClick = {}, onChatClick = { viewModel.navigateTo(AppScreens.CHAT_DIRECT) }, onRequestServiceClick = { showRequestServiceModal = true })
                AppScreens.MEDICAL_VIEW -> MedicalCentersScreen(viewModel = viewModel, themeColors = themeColors, onMedicalCenterClick = {}, onChatClick = { viewModel.navigateTo(AppScreens.CHAT_DIRECT) }, onBookAppointmentClick = { showRequestServiceModal = true })
                AppScreens.RESTAURANTS_VIEW -> RestaurantsScreen(viewModel = viewModel, themeColors = themeColors, onRestaurantClick = {}, onChatClick = { viewModel.navigateTo(AppScreens.CHAT_DIRECT) }, onOrderMealClick = { showRequestServiceModal = true })
                AppScreens.PROPERTIES_VIEW -> PropertiesScreen(viewModel = viewModel, themeColors = themeColors, onPropertyClick = {}, onChatClick = { viewModel.navigateTo(AppScreens.CHAT_DIRECT) }, onRequestInspectionClick = { showRequestServiceModal = true })
                AppScreens.CHAT_LIST -> ChatListScreen(currentUserId = currentUserId, currentUserName = currentUserName, themeColors = themeColors, onChannelClick = { viewModel.navigateTo(AppScreens.CHAT_DIRECT) }, onBackClick = { viewModel.navigateTo(AppScreens.USER_BROWSE) })
                AppScreens.CHAT_DIRECT -> ChatScreen(currentUserId = currentUserId, currentUserName = currentUserName, themeColors = themeColors, onBackClick = { viewModel.navigateTo(AppScreens.USER_BROWSE) })
                AppScreens.CREATE_BOOKING -> CreateBookingScreen(onBack = { viewModel.navigateTo(AppScreens.USER_BROWSE) }, onBookingCreated = { viewModel.navigateTo(AppScreens.USER_BROWSE) })
                AppScreens.DYNAMIC_PROFILE -> DynamicPolymorphicProfileScreen(viewModel = viewModel, themeColors = themeColors, onBackClick = { viewModel.navigateTo(AppScreens.USER_BROWSE) })
                AppScreens.URGENT_REQUEST_DETAILS -> UrgentRequestDetailsScreen(requestId = "", viewModel = viewModel)
                AppScreens.OFFERS_LIST -> UrgentOffersList(offers = emptyList(), isOwner = true, onAcceptOffer = {}, onContactProvider = { _, _ -> })
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
                    onChatOpen = { viewModel.navigateTo(AppScreens.CHAT_DIRECT) }
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
