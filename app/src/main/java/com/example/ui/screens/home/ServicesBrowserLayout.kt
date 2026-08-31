@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.home

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.components.AdminCustomBannerView
import com.example.ui.components.BannerSliderView
import com.example.utils.VisualThemePalette

import com.example.data.repositories.*
import com.example.StoreCreateEditDialog
import com.example.ui.screens.dashboard.ServicesBrowserViewModel
import com.example.ui.screens.home.sections.*

/**
 * 🏠 ServicesBrowserLayout - الشاشة الرئيسية لتصفح الخدمات والمتاجر باليمن
 * مفككة ومبنية وفق معمارية Clean Architecture و MVVM النظيفة (<250 سطر)
 */
@Composable
fun ServicesBrowserLayout(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    activeSectionIdForCreation: String,
    onActiveSectionIdForCreationChange: (String) -> Unit,
    preselectedRegistrationType: String,
    onPreselectedRegistrationTypeChange: (String) -> Unit,
    onChatOpen: (String) -> Unit
) {
    val context = LocalContext.current

    val browserViewModel = remember {
        ServicesBrowserViewModel(
            productsRepository = ProductsRepositoryImpl(context)
        )
    }

    val browserUiState by browserViewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val filteredProviders by viewModel.filteredProviders.collectAsState()
    val isProvidersLoading by viewModel.isProvidersLoading.collectAsState()
    val selectedCategory by viewModel.selectedCategoryId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isVipOnly by viewModel.filterVipOnly.collectAsState()
    val isAvailableOnly by viewModel.filterAvailableOnly.collectAsState()
    val citiesList by viewModel.cities.collectAsState()
    val activeCityId by viewModel.filterCityId.collectAsState()
    val radiusKm by viewModel.maxKmRadius.collectAsState()
    val neighborFilter by viewModel.filterNeighborhoodName.collectAsState()
    val phoneOrNameFilter by viewModel.phoneOrNameFilter.collectAsState()
    val bannersList by viewModel.banners.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val paymentWallets by viewModel.paymentWallets.collectAsState()

    var showFiltersPanel by remember { mutableStateOf(false) }
    var selectedStoreForDetails by remember { mutableStateOf<StoreEntity?>(null) }
    var selectedPropertyForDetails by remember { mutableStateOf<PropertyEntity?>(null) }
    var selectedJobForDetails by remember { mutableStateOf<JobEntity?>(null) }
    var payingBookingObj by remember { mutableStateOf<BookingEntity?>(null) }
    var providersLimit by remember { mutableStateOf(10) }
    var activeTabName by remember { mutableStateOf("الرئيسية") }
    var showCreateStoreModalSection by remember { mutableStateOf<String?>(null) }

    val activeTabs = remember(settingsState) {
        val list = mutableListOf("الرئيسية")
        if (settingsState.isStoresEnabled) list.add("المحلات والمتاجر")
        list.add("المطاعم والكافيهات")
        list.add("المراكز الطبية")
        if (settingsState.isPropertiesEnabled) list.add("العقارات")
        list.add("إعلانات الوظائف")
        list.add("المفضلة")
        list.toList()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (activeTabName == "الرئيسية") {
            // 1. Top Custom Banner
            if (settingsState.bannerEnabled && settingsState.bannerLocation == "TOP") {
                item {
                    AdminCustomBannerView(settingsState = settingsState, themeColors = themeColors)
                }
            }

            // 2. Banner Slider
            if (bannersList.isNotEmpty()) {
                item {
                    BannerSliderView(banners = bannersList, themeColors = themeColors) { catTarget ->
                        if (catTarget.isNotEmpty()) viewModel.selectCategory(catTarget)
                    }
                }
            }

            // 3. Search Bar
            item {
                val isFilterActive = phoneOrNameFilter.isNotEmpty() || neighborFilter.isNotEmpty() || isVipOnly || isAvailableOnly
                ServicesSearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    isFilterActive = isFilterActive,
                    onFilterClick = { showFiltersPanel = true },
                    isSpeechSearchEnabled = settingsState.isSpeechSearchEnabled,
                    onVoiceClick = { /* Voice click handled */ },
                    themeColors = themeColors
                )
            }
        }

        // 4. Tab Navigation Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                activeTabs.forEach { tabName ->
                    val isSelected = activeTabName == tabName
                    Surface(
                        onClick = { activeTabName = tabName },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) themeColors.accent else themeColors.surface,
                        border = BorderStroke(1.dp, if (isSelected) themeColors.accent else Color.White.copy(alpha = 0.1f))
                    ) {
                        Text(
                            text = tabName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 5. Main Tab Content
        item {
            when (activeTabName) {
                "المفضلة" -> {
                    FavoritesScreenLayout(
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onBackClick = { activeTabName = "الرئيسية" },
                        onOpenProviderDetails = {
                            viewModel.selectedProvider = it
                            viewModel.navigateTo("DYNAMIC_PROFILE")
                        },
                        onOpenStoreDetails = { selectedStoreForDetails = it },
                        onOpenPropertyDetails = { selectedPropertyForDetails = it },
                        onOpenChat = onChatOpen
                    )
                }
                "المحلات والمتاجر", "المحلات", settingsState.storesTabName -> {
                    StoresSectionView(
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onStoreClick = { 
                            viewModel.selectedStore = it
                            viewModel.navigateTo("STORE_DETAILS")
                        },
                        onCreateStoreClick = {
                            showCreateStoreModalSection = "stores"
                        }
                    )
                }
                "المطاعم والكافيهات", "المطاعم" -> {
                    RestaurantsSectionView(
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onStoreClick = { 
                            viewModel.selectedStore = it
                            viewModel.navigateTo("STORE_DETAILS")
                        },
                        onCreateRestaurantClick = {
                            showCreateStoreModalSection = "restaurants"
                        }
                    )
                }
                "المراكز الطبية", "المراكز" -> {
                    MedicalCentersSectionView(
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onStoreClick = { 
                            viewModel.selectedStore = it
                            viewModel.navigateTo("STORE_DETAILS")
                        },
                        onCreateMedicalClick = {
                            showCreateStoreModalSection = "medical"
                        }
                    )
                }
                "العقارات", settingsState.propertiesTabName -> {
                    PropertiesSectionView(
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onPropertyClick = { selectedPropertyForDetails = it },
                        onCreatePropertyClick = {
                            showCreateStoreModalSection = "realestate"
                        }
                    )
                }
                "إعلانات الوظائف", "الوظائف" -> {
                    JobsSectionView(
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onJobClick = { selectedJobForDetails = it },
                        onCreateJobClick = {
                            viewModel.navigateTo("REGISTER")
                        }
                    )
                }
                else -> {
                    ServicesBrowserMainContent(
                        viewModel = viewModel,
                        themeColors = themeColors,
                        displayProviders = filteredProviders,
                        isProvidersLoading = isProvidersLoading,
                        categories = categories,
                        selectedCategoryId = selectedCategory,
                        onCategorySelected = { viewModel.selectCategory(it ?: "") },
                        providersLimit = providersLimit,
                        onLoadMore = { providersLimit += 10 },
                        onStoreClick = { 
                            viewModel.selectedStore = it
                            viewModel.navigateTo("STORE_DETAILS")
                        },
                        onPropertyClick = { selectedPropertyForDetails = it },
                        onChatOpen = onChatOpen
                    )
                }
            }
        }
    }

    // Dialogs & Payment Sheets
    if (showFiltersPanel) {
        FilterPanelDialog(
            cities = citiesList,
            selectedCityId = activeCityId ?: "",
            onSelectCity = { viewModel.setCityFilter(it) },
            isVipOnly = isVipOnly,
            onToggleVip = { viewModel.toggleVipFilter() },
            isAvailableOnly = isAvailableOnly,
            onToggleAvailable = { viewModel.toggleAvailableFilter() },
            radiusKm = radiusKm,
            onRadiusChange = { viewModel.setRadiusKm(it) },
            neighborhood = neighborFilter,
            onNeighborhoodChange = { viewModel.setNeighborhoodFilter(it) },
            themeColors = themeColors,
            onDismiss = { showFiltersPanel = false }
        )
    }

    selectedStoreForDetails?.let { store ->
        StoreQuickDetailsDialog(
            store = store,
            context = context,
            themeColors = themeColors,
            onDismiss = { selectedStoreForDetails = null },
            onOpenDetails = {
                val storeToOpen = store
                selectedStoreForDetails = null
                viewModel.selectedStore = storeToOpen
                viewModel.navigateTo("STORE_DETAILS")
            }
        )
    }

    selectedPropertyForDetails?.let { prop ->
        PropertyQuickDetailsDialog(
            property = prop,
            context = context,
            themeColors = themeColors,
            onDismiss = { selectedPropertyForDetails = null }
        )
    }

    selectedJobForDetails?.let { job ->
        JobQuickDetailsDialog(
            job = job,
            context = context,
            themeColors = themeColors,
            onDismiss = { selectedJobForDetails = null }
        )
    }

    payingBookingObj?.let { booking ->
        ServicesBrowserPaymentDialog(
            booking = booking,
            wallets = paymentWallets,
            viewModel = viewModel,
            themeColors = themeColors,
            context = context,
            onDismiss = { payingBookingObj = null }
        )
    }

    showCreateStoreModalSection?.let { secId ->
        StoreCreateEditDialog(
            store = null as StoreEntity?,
            viewModel = viewModel,
            themeColors = themeColors,
            sectionId = secId,
            onDismiss = { showCreateStoreModalSection = null }
        )
    }
}
