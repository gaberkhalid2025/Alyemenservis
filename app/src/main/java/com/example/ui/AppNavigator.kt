package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultLauncher
import com.example.ui.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.CreateBookingScreen
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.helpers.AppPreferenceHelper
import com.example.ui.screens.admin.AdminPanelLayout
import com.example.ui.screens.assistant.SmartAssistantDialogView
import com.example.ui.screens.bookings.BookingsScreenLayout
import com.example.ui.screens.bookings.InstantRequestsScreen
import com.example.ui.screens.bookings.OrdersScreenLayout
import com.example.ui.screens.chat.ChatListScreen
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.entities.*
import com.example.ui.screens.home.FavoritesScreenLayout
import com.example.ui.screens.home.ServicesBrowserLayout
import com.example.ui.screens.home.UnifiedGlobalSearchScreen
import com.example.ui.screens.map.MapScreen
import com.example.ui.screens.notifications.UserNotificationsDialogView
import com.example.ui.screens.owner.*
import com.example.ui.screens.register.RegisterScreen
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
    val adminRole by viewModel.adminRole.collectAsState()

    val showNotificationsDialog by viewModel.showNotificationsDialog.collectAsState()
    val showRestoreAccountDialog by viewModel.showRestoreAccountDialog.collectAsState()

    val providers by viewModel.providers.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategoryId.collectAsState()
    val requestOffers by viewModel.requestOffers.collectAsState()
    val joinRequestPhone by viewModel.joinRequestPhone.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()

    if (settingsState.isMaintenanceActive && adminRole == "GUEST") {
        MaintenanceSplashView(settingsState = settingsState, themeColors = themeColors, viewModel = viewModel)
        return
    }

    Scaffold(
        topBar = {
            if (ScreenRoutes.showTopBar(currentScreen, adminRole)) {
                AppHeaderBar(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onNotificationsClick = { viewModel.navigateToScreen(AppScreens.NOTIFICATIONS_VIEW) },
                    onChatsClick = { viewModel.navigateToScreen(AppScreens.CHAT_LIST) },
                    onMenuClick = { viewModel.toggleRestoreAccountDialog(true) }
                )
            }
        },
        bottomBar = {
            if (ScreenRoutes.showBottomBar(currentScreen, adminRole)) {
                AppFooterBar(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onInfoClick = { viewModel.navigateToScreen(AppScreens.ABOUT_APP) }
                )
            }
        },
        contentWindowInsets = if (currentScreen == AppScreens.CHAT_DIRECT || currentScreen == AppScreens.MAP_VIEW || currentScreen == AppScreens.SMART_ASSISTANT) {
            WindowInsets(0, 0, 0, 0)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        containerColor = themeColors.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (currentScreen == AppScreens.CHAT_DIRECT || currentScreen == AppScreens.MAP_VIEW || currentScreen == AppScreens.SMART_ASSISTANT) PaddingValues(0.dp) else innerPadding)
        ) {
            when (currentScreen) {
                AppScreens.USER_BROWSE, AppScreens.HOME -> {
                    var activeSectionIdForCreation by remember { mutableStateOf("") }
                    var preselectedRegistrationType by remember { mutableStateOf("") }

                    ServicesBrowserLayout(
                        viewModel = viewModel,
                        themeColors = themeColors,
                        activeSectionIdForCreation = activeSectionIdForCreation,
                        onActiveSectionIdForCreationChange = { activeSectionIdForCreation = it },
                        preselectedRegistrationType = preselectedRegistrationType,
                        onPreselectedRegistrationTypeChange = { preselectedRegistrationType = it },
                        onChatOpen = { targetId ->
                            val foundProvider = viewModel.providers.value.find { it.id == targetId || it.phone == targetId }
                            if (foundProvider != null) {
                                viewModel.openDirectChat(
                                    targetUserId = foundProvider.id.ifBlank { foundProvider.phone },
                                    targetUserName = foundProvider.name,
                                    targetUserPhoto = foundProvider.profileImage,
                                    relatedEntityId = foundProvider.id,
                                    relatedEntityType = "PROVIDER"
                                )
                            } else {
                                val foundStore = viewModel.stores.value.find { it.id == targetId }
                                if (foundStore != null) {
                                    val isMedical = foundStore.sectionId.contains("medical") || foundStore.categoryId.contains("medical") || foundStore.name.contains("طبي") || foundStore.name.contains("صيدلية")
                                    val isRestaurant = !isMedical && (foundStore.sectionId.contains("restaurant") || foundStore.categoryId.contains("restaurant") || foundStore.name.contains("مطعم") || foundStore.name.contains("كافيه"))
                                    val entityType = when {
                                        isMedical -> "MEDICAL"
                                        isRestaurant -> "RESTAURANT"
                                        else -> "STORE"
                                    }
                                    viewModel.openDirectChat(
                                        targetUserId = foundStore.id,
                                        targetUserName = foundStore.name,
                                        targetUserPhoto = foundStore.logoImage.ifBlank { foundStore.coverImage },
                                        relatedEntityId = foundStore.id,
                                        relatedEntityType = entityType
                                    )
                                } else {
                                    val foundProp = viewModel.properties.value.find { it.id == targetId }
                                    if (foundProp != null) {
                                        viewModel.openDirectChat(
                                            targetUserId = foundProp.id.ifBlank { foundProp.phone },
                                            targetUserName = foundProp.title.ifBlank { foundProp.ownerName },
                                            targetUserPhoto = foundProp.images.firstOrNull() ?: "",
                                            relatedEntityId = foundProp.id,
                                            relatedEntityType = "PROPERTY"
                                        )
                                    } else if (targetId == "support_channel_id" || targetId == "admin_support") {
                                        viewModel.openSupportChat()
                                    } else {
                                        viewModel.openDirectChat(
                                            targetUserId = targetId,
                                            targetUserName = "مقدم الخدمة",
                                            relatedEntityId = targetId,
                                            relatedEntityType = "PROVIDER"
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
                AppScreens.ADMIN_PANEL -> AdminPanelLayout(viewModel = viewModel, themeColors = themeColors)
                AppScreens.OWNER_PANEL, AppScreens.PRODUCTS_MGMT_VIEW, AppScreens.PRICE_MGMT_VIEW, AppScreens.OFFERS_MGMT_VIEW, AppScreens.GALLERY_MGMT_VIEW -> {
                    val resolvedOwnerAccount = remember(
                        viewModel.selectedStore,
                        viewModel.selectedProvider,
                        viewModel.selectedProperty,
                        stores,
                        providers,
                        properties,
                        joinRequestPhone,
                        currentUserPhone,
                        currentUserId,
                        currentUserName
                    ) {
                        resolveActiveOwnerAccount(
                            viewModel = viewModel,
                            stores = stores,
                            providers = providers,
                            properties = properties,
                            joinPhone = joinRequestPhone,
                            currentUserPhone = currentUserPhone,
                            currentUserId = currentUserId,
                            currentUserName = currentUserName
                        )
                    }
                    BackHandler { viewModel.goBack() }
                    when (currentScreen) {
                        AppScreens.PRODUCTS_MGMT_VIEW -> {
                            OwnerSubScreenScaffold(
                                title = "إدارة المنتجات والخدمات",
                                themeColors = themeColors,
                                onBack = { viewModel.goBack() }
                            ) {
                                ProductServiceManagementScreen(
                                    account = resolvedOwnerAccount,
                                    viewModel = viewModel,
                                    themeColors = themeColors
                                )
                            }
                        }
                        AppScreens.PRICE_MGMT_VIEW -> {
                            OwnerSubScreenScaffold(
                                title = "إدارة وتحديث الأسعار",
                                themeColors = themeColors,
                                onBack = { viewModel.goBack() }
                            ) {
                                PriceManagementScreen(
                                    account = resolvedOwnerAccount,
                                    viewModel = viewModel,
                                    themeColors = themeColors
                                )
                            }
                        }
                        AppScreens.OFFERS_MGMT_VIEW -> {
                            OwnerSubScreenScaffold(
                                title = "إدارة العروض والكوبونات",
                                themeColors = themeColors,
                                onBack = { viewModel.goBack() }
                            ) {
                                OffersManagementScreen(
                                    account = resolvedOwnerAccount,
                                    viewModel = viewModel,
                                    themeColors = themeColors
                                )
                            }
                        }
                        AppScreens.GALLERY_MGMT_VIEW -> {
                            OwnerSubScreenScaffold(
                                title = "إدارة معرض الصور",
                                themeColors = themeColors,
                                onBack = { viewModel.goBack() }
                            ) {
                                GalleryManagementScreen(
                                    account = resolvedOwnerAccount,
                                    viewModel = viewModel,
                                    themeColors = themeColors
                                )
                            }
                        }
                        else -> {
                            OwnerDashboardScreen(
                                account = resolvedOwnerAccount,
                                viewModel = viewModel,
                                themeColors = themeColors,
                                onNavigateTab = { tab ->
                                    when (tab) {
                                        1 -> viewModel.navigateToScreen(AppScreens.PRODUCTS_MGMT_VIEW)
                                        2 -> viewModel.navigateToScreen(AppScreens.OFFERS_MGMT_VIEW)
                                        3 -> viewModel.navigateToScreen(AppScreens.PRICE_MGMT_VIEW)
                                        5 -> viewModel.navigateToScreen(AppScreens.GALLERY_MGMT_VIEW)
                                        4, 6 -> viewModel.navigateToScreen(AppScreens.OWNER_PROFILE_VIEW)
                                    }
                                },
                                onBackClick = { viewModel.goBack() }
                            )
                        }
                    }
                }
                AppScreens.STATUS_VIEW -> StatusScreen(viewModel = viewModel, themeColors = themeColors)
                AppScreens.CATEGORIES_VIEW -> CategoriesScreen(viewModel = viewModel, themeColors = themeColors, onCategoryClick = { cat -> viewModel.selectCategory(cat); viewModel.navigateToScreen(AppScreens.USER_BROWSE) })
                AppScreens.STORES_VIEW -> StoresScreen(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onStoreClick = { store ->
                        viewModel.selectedStore = store
                        viewModel.selectedProvider = null
                        viewModel.selectedProperty = null
                        viewModel.selectedJob = null
                        viewModel.navigateToScreen(AppScreens.STORE_DETAILS)
                    },
                    onChatClick = { store ->
                        viewModel.openDirectChat(
                            targetUserId = store.id,
                            targetUserName = store.name,
                            targetUserPhoto = store.logoImage.ifBlank { store.coverImage },
                            relatedEntityId = store.id,
                            relatedEntityType = "STORE"
                        )
                    },
                    onRequestServiceClick = { store ->
                        viewModel.selectedStore = store
                        viewModel.selectedProvider = null
                        viewModel.selectedProperty = null
                        viewModel.selectedJob = null
                        viewModel.navigateToScreen(AppScreens.STORE_DETAILS)
                    }
                )
                AppScreens.MEDICAL_VIEW -> MedicalCentersScreen(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onMedicalCenterClick = { med ->
                        viewModel.selectedProvider = med
                        viewModel.selectedStore = null
                        viewModel.selectedProperty = null
                        viewModel.selectedJob = null
                        viewModel.navigateToScreen(AppScreens.PROVIDER_DETAILS)
                    },
                    onChatClick = { med ->
                        viewModel.openDirectChat(
                            targetUserId = med.id.ifBlank { med.phone },
                            targetUserName = med.name,
                            targetUserPhoto = med.profileImage,
                            relatedEntityId = med.id,
                            relatedEntityType = "MEDICAL"
                        )
                    },
                    onBookAppointmentClick = { med ->
                        viewModel.selectedProvider = med
                        viewModel.selectedStore = null
                        viewModel.selectedProperty = null
                        viewModel.selectedJob = null
                        viewModel.navigateToScreen(AppScreens.CREATE_BOOKING)
                    },
                    onMedicalStoreClick = { medStore ->
                        viewModel.selectedStore = medStore
                        viewModel.selectedProvider = null
                        viewModel.selectedProperty = null
                        viewModel.selectedJob = null
                        viewModel.navigateToScreen(AppScreens.STORE_DETAILS)
                    },
                    onMedicalStoreChatClick = { medStore ->
                        viewModel.openDirectChat(
                            targetUserId = medStore.id,
                            targetUserName = medStore.name,
                            targetUserPhoto = medStore.logoImage.ifBlank { medStore.coverImage },
                            relatedEntityId = medStore.id,
                            relatedEntityType = "MEDICAL"
                        )
                    }
                )
                AppScreens.RESTAURANTS_VIEW -> RestaurantsScreen(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onRestaurantClick = { rest ->
                        viewModel.selectedStore = rest
                        viewModel.selectedProvider = null
                        viewModel.selectedProperty = null
                        viewModel.selectedJob = null
                        viewModel.navigateToScreen(AppScreens.STORE_DETAILS)
                    },
                    onChatClick = { rest ->
                        viewModel.openDirectChat(
                            targetUserId = rest.id,
                            targetUserName = rest.name,
                            targetUserPhoto = rest.logoImage.ifBlank { rest.coverImage },
                            relatedEntityId = rest.id,
                            relatedEntityType = "RESTAURANT"
                        )
                    },
                    onOrderMealClick = { rest ->
                        viewModel.selectedStore = rest
                        viewModel.selectedProvider = null
                        viewModel.selectedProperty = null
                        viewModel.selectedJob = null
                        viewModel.navigateToScreen(AppScreens.STORE_DETAILS)
                    }
                )
                AppScreens.PROPERTIES_VIEW -> PropertiesScreen(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onPropertyClick = { prop ->
                        viewModel.selectedProperty = prop
                        viewModel.selectedProvider = null
                        viewModel.selectedStore = null
                        viewModel.selectedJob = null
                        viewModel.navigateToScreen(AppScreens.PROPERTY_DETAILS)
                    },
                    onChatClick = { prop ->
                        viewModel.openDirectChat(
                            targetUserId = prop.id.ifBlank { prop.phone },
                            targetUserName = prop.title.ifBlank { prop.ownerName },
                            targetUserPhoto = prop.images.firstOrNull() ?: "",
                            relatedEntityId = prop.id,
                            relatedEntityType = "PROPERTY"
                        )
                    },
                    onRequestInspectionClick = { prop ->
                        viewModel.selectedProperty = prop
                        viewModel.selectedProvider = null
                        viewModel.selectedStore = null
                        viewModel.selectedJob = null
                        viewModel.navigateToScreen(AppScreens.CREATE_BOOKING)
                    }
                )
                AppScreens.CHAT_LIST -> ChatListScreen(
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    themeColors = themeColors,
                    onChannelClick = { ch ->
                        viewModel.targetChatChannelId = ch.id
                        val otherId = ch.participants.firstOrNull { it != currentUserId } ?: ch.targetPhone
                        viewModel.targetChatUserId = otherId
                        viewModel.targetChatUserName = ch.participantNames[otherId] ?: ch.title.ifBlank { "محادثة" }
                        viewModel.targetChatUserPhoto = ch.participantPhotos[otherId] ?: ch.groupAvatarUrl
                        viewModel.targetChatRelatedEntityId = ch.relatedEntityId
                        viewModel.targetChatRelatedEntityType = ch.relatedEntityType
                        viewModel.navigateToScreen(AppScreens.CHAT_DIRECT)
                    },
                    onBackClick = { viewModel.goBack() },
                    onStartSupportChat = { viewModel.openSupportChat() }
                )
                AppScreens.CHAT_DIRECT -> ChatScreen(
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    themeColors = themeColors,
                    channelId = viewModel.targetChatChannelId,
                    targetUserId = viewModel.targetChatUserId,
                    targetUserName = viewModel.targetChatUserName,
                    targetUserPhoto = viewModel.targetChatUserPhoto,
                    relatedEntityId = viewModel.targetChatRelatedEntityId,
                    relatedEntityType = viewModel.targetChatRelatedEntityType,
                    showClientIdentityToProviders = viewModel.settings.value.showClientIdentityToProviders,
                    onBackClick = {
                        viewModel.targetChatChannelId = null
                        viewModel.targetChatUserId = null
                        viewModel.targetChatUserName = null
                        viewModel.targetChatUserPhoto = null
                        viewModel.targetChatRelatedEntityId = null
                        viewModel.targetChatRelatedEntityType = null
                        viewModel.goBack()
                    }
                )
                AppScreens.CREATE_BOOKING -> {
                    val pId = viewModel.selectedProvider?.id
                        ?: viewModel.selectedStore?.id
                        ?: viewModel.selectedProperty?.id
                        ?: ""
                    val pName = viewModel.selectedProvider?.name
                        ?: viewModel.selectedStore?.name
                        ?: viewModel.selectedProperty?.title
                        ?: ""
                    val pService = viewModel.selectedProvider?.specialization?.ifBlank { viewModel.selectedProvider?.profession }
                        ?: viewModel.selectedStore?.categoryId
                        ?: viewModel.selectedProperty?.title
                        ?: ""
                    BackHandler { viewModel.goBack() }
                    CreateBookingScreen(
                        providerId = pId,
                        providerName = pName,
                        preselectedService = pService,
                        onBack = { viewModel.goBack() },
                        onBookingCreated = { booking ->
                            viewModel.createBooking(booking)
                            viewModel.navigateToScreen(AppScreens.BOOKINGS_VIEW)
                        }
                    )
                }
                AppScreens.DYNAMIC_PROFILE, AppScreens.OWNER_PROFILE_VIEW, AppScreens.PROVIDER_DETAILS, AppScreens.STORE_DETAILS, AppScreens.PROPERTY_DETAILS -> DynamicPolymorphicProfileScreen(
                    provider = viewModel.selectedProvider,
                    store = viewModel.selectedStore,
                    property = viewModel.selectedProperty,
                    job = viewModel.selectedJob,
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onBackClick = { viewModel.goBack() },
                    onOpenChat = { channelId ->
                        viewModel.targetChatChannelId = channelId
                        viewModel.navigateToScreen(AppScreens.CHAT_DIRECT)
                    },
                    onRequestBooking = {
                        viewModel.navigateToScreen(AppScreens.CREATE_BOOKING)
                    },
                    onOrderProduct = {
                        viewModel.navigateToScreen(AppScreens.CREATE_BOOKING)
                    }
                )
                AppScreens.BOOKINGS_VIEW -> BookingsScreenLayout(viewModel = viewModel, themeColors = themeColors)
                AppScreens.INSTANT_REQUESTS_VIEW -> InstantRequestsScreen(viewModel = viewModel, themeColors = themeColors)
                AppScreens.ORDERS_VIEW -> OrdersScreenLayout(viewModel = viewModel, themeColors = themeColors, onRequestQuickService = { viewModel.navigateToScreen(AppScreens.QUICK_SERVICE_REQUEST) })
                AppScreens.MAP_VIEW -> MapScreen(
                    viewModel = viewModel,
                    onBackClick = { viewModel.goBack() },
                    onOpenProviderDetails = { provider ->
                        viewModel.selectedProvider = provider
                        viewModel.selectedStore = null
                        viewModel.selectedProperty = null
                        viewModel.selectedJob = null
                        viewModel.navigateToScreen(AppScreens.PROVIDER_DETAILS)
                    },
                    onOpenStoreDetails = { store ->
                        viewModel.selectedStore = store
                        viewModel.selectedProvider = null
                        viewModel.selectedProperty = null
                        viewModel.selectedJob = null
                        viewModel.navigateToScreen(AppScreens.STORE_DETAILS)
                    },
                    onOpenPropertyDetails = { prop ->
                        viewModel.selectedProperty = prop
                        viewModel.selectedProvider = null
                        viewModel.selectedStore = null
                        viewModel.selectedJob = null
                        viewModel.navigateToScreen(AppScreens.PROPERTY_DETAILS)
                    },
                    onRequestBooking = { entity ->
                        when (entity) {
                            is ProviderEntity -> {
                                viewModel.selectedProvider = entity
                                viewModel.selectedStore = null
                                viewModel.selectedProperty = null
                                viewModel.selectedJob = null
                                viewModel.navigateToScreen(AppScreens.CREATE_BOOKING)
                            }
                            is StoreEntity -> {
                                viewModel.selectedStore = entity
                                viewModel.selectedProvider = null
                                viewModel.selectedProperty = null
                                viewModel.selectedJob = null
                                viewModel.navigateToScreen(AppScreens.CREATE_BOOKING)
                            }
                            is PropertyEntity -> {
                                viewModel.selectedProperty = entity
                                viewModel.selectedProvider = null
                                viewModel.selectedStore = null
                                viewModel.selectedJob = null
                                viewModel.navigateToScreen(AppScreens.CREATE_BOOKING)
                            }
                        }
                    }
                )
                AppScreens.FAVORITES_VIEW -> FavoritesScreenLayout(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onBackClick = { viewModel.goBack() },
                    onOpenProviderDetails = { provider ->
                        viewModel.selectedProvider = provider
                        viewModel.selectedStore = null
                        viewModel.selectedProperty = null
                        viewModel.selectedJob = null
                        viewModel.navigateToScreen(AppScreens.PROVIDER_DETAILS)
                    },
                    onOpenStoreDetails = { store ->
                        viewModel.selectedStore = store
                        viewModel.selectedProvider = null
                        viewModel.selectedProperty = null
                        viewModel.selectedJob = null
                        viewModel.navigateToScreen(AppScreens.STORE_DETAILS)
                    },
                    onOpenPropertyDetails = { prop ->
                        viewModel.selectedProperty = prop
                        viewModel.selectedProvider = null
                        viewModel.selectedStore = null
                        viewModel.selectedJob = null
                        viewModel.navigateToScreen(AppScreens.PROPERTY_DETAILS)
                    }
                )
                AppScreens.URGENT_REQUEST_DETAILS -> UrgentRequestDetailsScreen(
                    requestId = viewModel.selectedRequestId,
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onNavigateBack = { viewModel.goBack() },
                    onNavigateToChat = { phoneOrId, name ->
                        viewModel.openDirectChat(
                            targetUserId = phoneOrId,
                            targetUserName = name,
                            relatedEntityId = viewModel.selectedRequestId,
                            relatedEntityType = "URGENT_PROVIDER"
                        )
                    }
                )
                AppScreens.OFFERS_LIST -> UrgentOffersList(
                    offers = requestOffers,
                    isOwner = viewModel.isProviderUser,
                    onAcceptOffer = { _ -> },
                    onContactProvider = { offer ->
                        viewModel.openOrCreateChatChannel(
                            targetId = offer.technicianId.ifBlank { offer.technicianPhone },
                            targetType = "URGENT_PROVIDER",
                            targetName = offer.technicianName,
                            targetPhone = offer.technicianPhone,
                            relatedEntityId = offer.requestId,
                            relatedEntityType = "URGENT_PROVIDER"
                        ) {
                            viewModel.navigateToScreen(AppScreens.CHAT_DIRECT)
                        }
                    }
                )
                AppScreens.REGISTER_FORM, AppScreens.JOIN_REQUEST_STATUS, "TECHNICIAN_DASHBOARD", "STORE_DASHBOARD", "RESTAURANT_DASHBOARD", "MEDICAL_DASHBOARD", "PROPERTY_DASHBOARD", "JOB_POSTER_DASHBOARD", "CLIENT_DASHBOARD", "DASHBOARD" -> {
                    val roleFromRoute = if (currentScreen.endsWith("_DASHBOARD")) currentScreen else null
                    RegisterScreen(
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onBackClick = { viewModel.goBack() },
                        preferredRole = roleFromRoute
                    )
                }
                AppScreens.PASSWORD_RESET_WAITING -> {
                    com.example.ui.screens.register.PasswordResetWaitingScreen(
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onBackClick = { viewModel.navigateToScreen(AppScreens.USER_BROWSE) }
                    )
                }
                AppScreens.ABOUT_APP -> {
                    com.example.ui.screens.about.AboutAppScreenContent(
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onBackClick = { viewModel.goBack() }
                    )
                }
                AppScreens.NOTIFICATIONS_VIEW -> {
                    com.example.ui.screens.notifications.UserNotificationsScreen(
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onBack = { viewModel.goBack() }
                    )
                }
                AppScreens.SMART_ASSISTANT -> {
                    SmartAssistantDialogView(
                        viewModel = viewModel,
                        settings = settingsState,
                        themeColors = themeColors,
                        onDismiss = { viewModel.goBack() },
                        onChatOpen = { viewModel.openSupportChat() },
                        onRequestQuickService = {
                            viewModel.navigateToScreen(AppScreens.QUICK_SERVICE_REQUEST)
                        },
                        onNavigateToMap = {
                            viewModel.navigateToScreen(AppScreens.MAP_VIEW)
                        }
                    )
                }
                AppScreens.QUICK_SERVICE_REQUEST -> {
                    QuickServiceRequestDialog(
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onDismiss = { viewModel.goBack() },
                        onRequestCreated = { viewModel.navigateToScreen(AppScreens.ORDERS_VIEW) }
                    )
                }
                AppScreens.UNIFIED_SEARCH -> {
                    UnifiedGlobalSearchScreen(
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onBackClick = { viewModel.goBack() }
                    )
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("الشاشة المعروضة: $currentScreen", color = Color.White, fontSize = 14.sp)
                    }
                }
            }

            // ⚠️ SECURITY NOTICE: Backdoor dialog for debugging only.
            // Must be disabled in production builds via BuildConfig.DEBUG check.
            val showBackdoorDialog by viewModel.showBackdoorDialog.collectAsState()
            if (showBackdoorDialog) {
                BackdoorLoginDialog(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onDismiss = { viewModel.dismissBackdoorDialog() }
                )
            }

            if (currentScreen == AppScreens.USER_BROWSE || currentScreen == AppScreens.HOME || currentScreen == AppScreens.FAVORITES_VIEW) {
                FloatingIconsOverlay(
                    settings = settingsState,
                    themeColors = themeColors,
                    onAssistantClick = { viewModel.navigateToScreen(AppScreens.SMART_ASSISTANT) },
                    onRequestServiceClick = { viewModel.navigateToScreen(AppScreens.QUICK_SERVICE_REQUEST) }
                )
            }

            if (showRestoreAccountDialog) {
                RestoreAccountDialog(viewModel = viewModel, themeColors = themeColors, onDismiss = { viewModel.toggleRestoreAccountDialog(false) })
            }
            if (showNotificationsDialog) {
                UserNotificationsDialogView(viewModel = viewModel, themeColors = themeColors, onDismiss = { viewModel.toggleNotificationsDialog(false) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnerSubScreenScaffold(
    title: String,
    themeColors: VisualThemePalette,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = themeColors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = themeColors.surface)
            )
        },
        containerColor = themeColors.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(themeColors.background)
        ) {
            content()
        }
    }
}

private fun resolveActiveOwnerAccount(
    viewModel: MainViewModel,
    stores: List<StoreEntity>,
    providers: List<ProviderEntity>,
    properties: List<PropertyEntity>,
    joinPhone: String,
    currentUserPhone: String,
    currentUserId: String,
    currentUserName: String
): UnifiedBusinessAccount {
    val cleanPhone = AppPreferenceHelper.normalizePhoneNumber(joinPhone.ifBlank { currentUserPhone })

    val selStore = viewModel.selectedStore ?: stores.find {
        !it.isDeleted && (
            (cleanPhone.isNotBlank() && (AppPreferenceHelper.normalizePhoneNumber(it.phone) == cleanPhone || AppPreferenceHelper.normalizePhoneNumber(it.ownerId) == cleanPhone)) ||
                (currentUserId.isNotBlank() && (it.ownerId == currentUserId || it.id == currentUserId))
            )
    }
    if (selStore != null) {
        val sec = selStore.sectionId.lowercase()
        val bType = when {
            sec.contains("restaurant") || selStore.name.contains("مطعم") -> BusinessType.RESTAURANT
            sec.contains("medical") || selStore.medicalLicenseNo.isNotBlank() || selStore.name.contains("طبي") || selStore.name.contains("عيادة") -> BusinessType.MEDICAL
            else -> BusinessType.STORE
        }
        return UnifiedBusinessAccount(
            id = selStore.id,
            businessType = bType,
            name = selStore.name,
            description = selStore.description,
            phone = selStore.phone,
            ownerName = selStore.ownerName.ifBlank { currentUserName },
            categoryId = selStore.categoryId,
            cityId = selStore.cityId,
            neighborhood = selStore.localNeighborhood,
            coverImage = selStore.coverImage,
            logoImage = selStore.logoImage,
            rating = selStore.rating,
            numReviews = selStore.numReviews,
            isVerified = selStore.isApproved || selStore.isActive
        )
    }

    val selProvider = viewModel.selectedProvider ?: providers.find {
        !it.isDeleted && (
            (cleanPhone.isNotBlank() && AppPreferenceHelper.normalizePhoneNumber(it.phone) == cleanPhone) ||
                (currentUserId.isNotBlank() && it.id == currentUserId)
            )
    }
    if (selProvider != null) {
        val isMed = selProvider.providerType.contains("medical", ignoreCase = true) ||
            selProvider.categoryId.contains("medical", ignoreCase = true) ||
            selProvider.profession.contains("طبي", ignoreCase = true) ||
            selProvider.profession.contains("عياد", ignoreCase = true) ||
            selProvider.profession.contains("مستشفى", ignoreCase = true)
        return UnifiedBusinessAccount(
            id = selProvider.id,
            businessType = if (isMed) BusinessType.MEDICAL else BusinessType.TECHNICIAN,
            name = selProvider.name,
            description = selProvider.profession,
            phone = selProvider.phone,
            ownerName = selProvider.name,
            categoryId = selProvider.categoryId,
            cityId = selProvider.cityId,
            neighborhood = selProvider.localNeighborhood.ifBlank { selProvider.area },
            coverImage = selProvider.coverImage,
            logoImage = selProvider.profileImage,
            rating = selProvider.rating,
            numReviews = selProvider.numReviews,
            isVerified = selProvider.isVerified,
            isVip = selProvider.isVip
        )
    }

    val selProperty = viewModel.selectedProperty ?: properties.find {
        !it.isDeleted && (
            (cleanPhone.isNotBlank() && (AppPreferenceHelper.normalizePhoneNumber(it.phone) == cleanPhone || AppPreferenceHelper.normalizePhoneNumber(it.ownerId) == cleanPhone)) ||
                (currentUserId.isNotBlank() && (it.ownerId == currentUserId || it.id == currentUserId))
            )
    }
    if (selProperty != null) {
        return UnifiedBusinessAccount(
            id = selProperty.id,
            businessType = BusinessType.REAL_ESTATE,
            name = selProperty.title,
            description = selProperty.description,
            phone = selProperty.phone,
            ownerName = selProperty.ownerName.ifBlank { currentUserName },
            cityId = selProperty.cityId,
            neighborhood = selProperty.localNeighborhood,
            coverImage = selProperty.images.firstOrNull() ?: "",
            logoImage = selProperty.images.firstOrNull() ?: "",
            rating = selProperty.rating,
            numReviews = selProperty.numReviews,
            isVerified = selProperty.isActive
        )
    }

    return UnifiedBusinessAccount(
        id = currentUserId.ifBlank { cleanPhone.ifBlank { "owner_guest" } },
        name = currentUserName.ifBlank { "حساب الأعمال" },
        phone = joinPhone.ifBlank { currentUserPhone },
        description = "لوحة تحكم الأعمال"
    )
}
