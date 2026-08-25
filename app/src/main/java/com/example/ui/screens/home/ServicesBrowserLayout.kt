@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.home

import com.example.ui.*
import com.example.ui.utils.*


import android.content.Intent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import okhttp3.MediaType.Companion.toMediaType

import com.example.*

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.*
import com.example.utils.*
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.screens.home.*
import com.example.ui.screens.map.*
import com.example.ui.screens.bookings.*
import com.example.ui.screens.admin.*
import com.example.ui.screens.assistant.*
import com.example.ui.screens.register.*
import com.example.ui.screens.status.*
import com.example.ui.screens.about.*
import com.example.viewmodels.*
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
    val filterByCurrentCityOnly by viewModel.filterByCurrentCityOnly.collectAsState()
    val currentUserResidence by viewModel.currentUserResidence.collectAsState()

    val currentUserIdState by viewModel.currentUserId.collectAsState()
    val currentUserPhoneState by viewModel.currentUserPhone.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val myBookings = remember(bookings, currentUserPhoneState) {
        bookings.filter { it.customerPhone.trim() == currentUserPhoneState.trim() && currentUserPhoneState.isNotEmpty() }
    }
    var showGuestRegisterDialogForBooking by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Payment-related state collections and dialog states
    val paymentWallets by viewModel.paymentWallets.collectAsState()
    val paymentsList by viewModel.payments.collectAsState()

    var payingBookingObj by remember { mutableStateOf<BookingEntity?>(null) }
    var selectedUserWalletObj by remember { mutableStateOf<PaymentWalletEntity?>(null) }
    var userTransferIdInput by remember { mutableStateOf("") }
    var userTransferAccountNameInput by remember { mutableStateOf("") }
    var userTransferPhotoInput by remember { mutableStateOf("") }

    var showFiltersPanel by remember { mutableStateOf(false) }
    var sortByCheapestFirst by remember { mutableStateOf(false) }
    var selectedSortMode by remember { mutableStateOf("DEFAULT") } // DEFAULT, CHEAPEST, RATING, NEWEST
    var citySearchQuery by remember { mutableStateOf("") }
    var isCityDropdownOpen by remember { mutableStateOf(false) }

    // --- STORES & REAL ESTATE COMPONENT STATES ---
    val parsedSections = remember(settingsState.dynamicSectionsData) {
        com.example.data.DynamicSection.parseDynamicSections(settingsState.dynamicSectionsData)
            .filter { it.id != "services" && !it.name.contains("المهن والخدمات") }
    }
    val activeTabs = remember(parsedSections, settingsState.isStoresEnabled, settingsState.isPropertiesEnabled) {
        val list = mutableListOf("الرئيسية")
        parsedSections.filter { it.isEnabled }.forEach { sec ->
            if (sec.id == "services" || sec.name.contains("المهن والخدمات")) return@forEach
            if ((sec.id == "stores" || sec.type == "store") && !settingsState.isStoresEnabled) return@forEach
            if ((sec.id == "properties" || sec.type == "property") && !settingsState.isPropertiesEnabled) return@forEach
            list.add(sec.name)
        }
        list.add("المفضلة")
        list.toList()
    }
    var activeTabName by remember(activeTabs) { mutableStateOf(activeTabs.firstOrNull() ?: "الرئيسية") }
    var providersLimit by remember { mutableStateOf(10) }

    var selectedStoreForDetails by remember { mutableStateOf<com.example.data.StoreEntity?>(null) }
    var selectedPropertyForDetails by remember { mutableStateOf<com.example.data.PropertyEntity?>(null) }
    var showStoreCreateDialog by remember { mutableStateOf(false) }
    var showPropertyCreateDialog by remember { mutableStateOf(false) }
    var productToOrderElectronic by remember { mutableStateOf<com.example.data.ProductEntity?>(null) }

    val displayProviders = remember(filteredProviders, selectedSortMode, sortByCheapestFirst) {
        when {
            sortByCheapestFirst || selectedSortMode == "CHEAPEST" -> {
                filteredProviders.sortedBy { if (it.previewPrice <= 0) Double.MAX_VALUE else it.previewPrice }
            }
            selectedSortMode == "RATING" -> {
                filteredProviders.sortedByDescending { it.rating }
            }
            else -> filteredProviders
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Show Top Banners & Cross-Search ONLY on "الرئيسية" (Home Tab)
        // This ensures Stores, Restaurants, Medical Centers, Real Estate, etc. occupy the full screen
        if (activeTabName == "الرئيسية") {
            if (settingsState.bannerEnabled && settingsState.bannerLocation == "TOP") {
                item {
                    com.example.ui.components.AdminCustomBannerView(settingsState = settingsState, themeColors = themeColors)
                }
            }

            // Horizontal banners list
            if (bannersList.isNotEmpty()) {
                item {
                    com.example.ui.components.BannerSliderView(banners = bannersList, themeColors = themeColors) { catTarget ->
                        if (catTarget.isNotEmpty()) viewModel.selectCategory(catTarget)
                    }
                }
            }

            // Search Bar Block (Modern Sleek with subtle filter icon)
            item {
                val isFilterActive = phoneOrNameFilter.isNotEmpty() || neighborFilter.isNotEmpty() || isVipOnly || isAvailableOnly || radiusKm != 15 || filterByCurrentCityOnly
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    border = BorderStroke(0.8.dp, if (isFilterActive) themeColors.accent else themeColors.accent.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "بحث",
                            tint = themeColors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                singleLine = true,
                                maxLines = 1,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = themeColors.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_text_input"),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "ابحث عن فني، متجر، خدمة، عقار...",
                                            fontSize = 11.sp,
                                            color = themeColors.textSecondary.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.updateSearchQuery("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // Subtle interactive Filter Icon button with Active badge indicator
                        Box(contentAlignment = Alignment.TopEnd) {
                            Surface(
                                onClick = { showFiltersPanel = true },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isFilterActive) themeColors.accent else Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(0.7.dp, if (isFilterActive) themeColors.accent else Color.White.copy(alpha = 0.15f)),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "فلترة وبحث متقدم",
                                        tint = if (isFilterActive) Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (isFilterActive) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF10B981), CircleShape)
                                )
                            }
                        }

                        if (settingsState.isSpeechSearchEnabled) {
                            IconButton(
                                onClick = {
                                    VoiceManager.onHear?.invoke { spokenText ->
                                        viewModel.updateSearchQuery(spokenText)
                                        viewModel.triggerNotification("🎙️ تم سماع صوتك اليمني: $spokenText")
                                    }
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Text("🎙️", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Price Comparison & Quick Sorting Bar (Cross-Store Discovery)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filter 1: Cheapest (الأرخص سعراً)
                    val isCheapest = selectedSortMode == "CHEAPEST" || sortByCheapestFirst
                    Surface(
                        onClick = {
                            sortByCheapestFirst = !sortByCheapestFirst
                            selectedSortMode = if (sortByCheapestFirst) "CHEAPEST" else "DEFAULT"
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCheapest) Color(0xFF00C853) else Color(0xFF1A2128),
                        border = BorderStroke(1.dp, if (isCheapest) Color(0xFF00C853) else Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                        ) {
                            Text("🏷️", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isCheapest) "الأرخص سعراً 🟢" else "الأرخص سعراً",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCheapest) Color.Black else Color.White
                            )
                        }
                    }

                    // Filter 2: Top Rated (الأعلى تقييماً)
                    val isTopRated = selectedSortMode == "RATING"
                    Surface(
                        onClick = {
                            selectedSortMode = if (isTopRated) "DEFAULT" else "RATING"
                            if (selectedSortMode == "RATING") sortByCheapestFirst = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isTopRated) Color(0xFFFF9800) else Color(0xFF1A2128),
                        border = BorderStroke(1.dp, if (isTopRated) Color(0xFFFF9800) else Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                        ) {
                            Text("⭐", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "الأعلى تقييماً",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTopRated) Color.Black else Color.White
                            )
                        }
                    }

                    // Filter 3: Nearest (الأقرب لي)
                    val isNearest = selectedSortMode == "NEAREST"
                    Surface(
                        onClick = {
                            selectedSortMode = if (isNearest) "DEFAULT" else "NEAREST"
                            if (selectedSortMode == "NEAREST") sortByCheapestFirst = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isNearest) Color(0xFF3B82F6) else Color(0xFF1A2128),
                        border = BorderStroke(1.dp, if (isNearest) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                        ) {
                            Text("📍", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "الأقرب لي",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNearest) Color.White else Color.White
                            )
                        }
                    }
                }
            }
        }

        // ----------------- TAB CHIPS BAR -----------------
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                activeTabs.forEach { tabName ->
                    val isSelected = activeTabName == tabName
                    val icon = when (tabName) {
                        "الرئيسية" -> "🏠"
                        settingsState.storesTabName -> "🏪"
                        settingsState.propertiesTabName -> "🏠"
                        "المفضلة" -> "⭐"
                        else -> "✨"
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) themeColors.accent else themeColors.surface,
                                RoundedCornerShape(24.dp)
                            )
                            .clickable { activeTabName = tabName }
                            .border(
                                1.dp,
                                if (isSelected) themeColors.accent else themeColors.accent.copy(alpha = 0.2f),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(icon, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(5.dp))
                            val currentLangState by viewModel.currentLanguage.collectAsState()
                            val displayTabName = if (currentLangState == "en") {
                                when (tabName) {
                                    "الرئيسية" -> "Home"
                                    "المفضلة" -> "Favorites"
                                    settingsState.storesTabName -> "Stores"
                                    settingsState.propertiesTabName -> "Properties"
                                    else -> tabName
                                }
                            } else {
                                tabName
                            }
                            Text(
                                text = displayTabName,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- SMART RECOMMENDATIONS ---
        if (activeTabName == "الرئيسية" && (settingsState.isStoresEnabled || settingsState.isPropertiesEnabled)) {
            item {
                SmartRecommendationsSection(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onStoreClick = { selectedStoreForDetails = it },
                    onPropertyClick = { selectedPropertyForDetails = it }
                )
            }
        }

        // --- CONDITIONAL TABS IN LAZYCOLUMN ---
        val selectedSection = parsedSections.find { it.name == activeTabName && it.isEnabled }
        val isSecTypeEnabled = selectedSection?.let { sec ->
            if (sec.type == "store" && !settingsState.isStoresEnabled) false
            else if (sec.type == "property" && !settingsState.isPropertiesEnabled) false
            else true
        } ?: false
        if (selectedSection != null && isSecTypeEnabled) {
            if (selectedSection.type == "store") {
                item {
                    StoresTabContent(
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onStoreClick = { selectedStoreForDetails = it },
                        onAddStoreClick = {
                            onActiveSectionIdForCreationChange(selectedSection.id)
                            onPreselectedRegistrationTypeChange("STORE")
                            viewModel.navigateTo("REGISTER_FORM")
                        },
                        sectionId = selectedSection.id
                    )
                }
            } else {
                item {
                    PropertiesTabContent(
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onPropertyClick = { selectedPropertyForDetails = it },
                        onAddPropertyClick = {
                            onActiveSectionIdForCreationChange(selectedSection.id)
                            onPreselectedRegistrationTypeChange("PROPERTY")
                            viewModel.navigateTo("REGISTER_FORM")
                        },
                        sectionId = selectedSection.id
                    )
                }
            }
        } else if (activeTabName == "المفضلة") {
            item {
                WishlistTabContent(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onChatOpen = onChatOpen,
                    onSelectStore = { selectedStoreForDetails = it },
                    onSelectProperty = { selectedPropertyForDetails = it },
                    onSwitchToHome = { activeTabName = "الرئيسية" }
                )
            }
        } else {
            // "الرئيسية" / Standard flow

            // Yemen Cities Tabs / Scroll Selection
            item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "🌍 اختر المدينة لعرض الخدمات المحلية:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // All Cities chip
                    val isAllSelected = activeCityId == null
                    Surface(
                        modifier = Modifier
                            .clickable { viewModel.setCityFilter(null) }
                            .testTag("city_tab_all"),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isAllSelected) themeColors.accent else themeColors.surface,
                        border = BorderStroke(1.dp, if (isAllSelected) Color.Transparent else themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "🌍 كل المدن",
                            color = if (isAllSelected) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }

                    citiesList.forEach { city ->
                        val isSelected = activeCityId == city.id
                        Surface(
                            modifier = Modifier
                                .clickable { viewModel.setCityFilter(city.id) }
                                .testTag("city_tab_${city.id}"),
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) themeColors.accent else themeColors.surface,
                            border = BorderStroke(1.dp, if (isSelected) Color.Transparent else themeColors.accent.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = city.nameAr,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Clean Main Categories & Subcategories Selector
        item {
            val mainCats = remember(categories) {
                categories.filter { it.isMainCategory || it.parentId.isNullOrEmpty() }
            }
            val displayCats = if (mainCats.isNotEmpty()) mainCats else categories

            val activeSubCategories = remember(selectedCategory, categories) {
                if (selectedCategory != null) {
                    categories.filter { it.parentId == selectedCategory }
                } else {
                    emptyList()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📂 الأقسام والتخصصات الرئيسية:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (selectedCategory != null) {
                        Text(
                            text = "عرض الكل 🔄",
                            fontSize = 10.sp,
                            color = themeColors.accent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { viewModel.selectCategory(null) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Main Categories Horizontal Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "All" chip
                    val isAllSelected = selectedCategory == null
                    Surface(
                        modifier = Modifier
                            .clickable { viewModel.selectCategory(null) }
                            .testTag("category_chip_all"),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isAllSelected) themeColors.accent else themeColors.surface,
                        border = BorderStroke(1.dp, if (isAllSelected) Color.Transparent else themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "✨ الكل",
                            color = if (isAllSelected) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }

                    displayCats.forEach { cat ->
                        val isSelected = selectedCategory == cat.id
                        val hasSub = categories.any { it.parentId == cat.id }
                        Surface(
                            modifier = Modifier
                                .clickable {
                                    if (cat.id == "stores" || cat.parentId == "stores" || cat.id == "restaurants") {
                                        activeTabName = settingsState.storesTabName
                                        viewModel.selectCategory(cat.id)
                                    } else if (cat.id == "realestate" || cat.parentId == "realestate" || cat.id == "jobs") {
                                        activeTabName = settingsState.propertiesTabName
                                        viewModel.selectCategory(cat.id)
                                    } else {
                                        activeTabName = "الرئيسية"
                                        if (isSelected) viewModel.selectCategory(null)
                                        else viewModel.selectCategory(cat.id)
                                    }
                                }
                                .testTag("category_chip_${cat.id}"),
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) themeColors.accent else themeColors.surface,
                            border = BorderStroke(1.dp, if (isSelected) Color.Transparent else themeColors.accent.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(cat.icon, fontSize = 12.sp)
                                Text(
                                    text = cat.name,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (hasSub) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.Black else Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Subcategories smooth expandable row if selected category has subcategories
                if (activeSubCategories.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.7f),
                        border = BorderStroke(0.6.dp, themeColors.accent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "الخدمات الفرعية والتخصصات الدقيقة:",
                                fontSize = 10.sp,
                                color = themeColors.accent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                activeSubCategories.forEach { subCat ->
                                    Surface(
                                        modifier = Modifier.clickable {
                                            viewModel.selectCategory(subCat.id)
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (selectedCategory == subCat.id) themeColors.accent else Color.White.copy(alpha = 0.08f),
                                        border = BorderStroke(0.6.dp, if (selectedCategory == subCat.id) Color.Transparent else Color.White.copy(alpha = 0.15f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(subCat.icon, fontSize = 10.sp)
                                            Text(
                                                text = subCat.name,
                                                color = if (selectedCategory == subCat.id) Color.Black else Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Services Providers Headers
        item {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💼 مقدمو الخدمات المتوفرون (${displayProviders.size}):",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (selectedCategory != null) {
                        Text(
                            text = "إلغاء الفلترة",
                            fontSize = 11.sp,
                            color = themeColors.accent,
                            modifier = Modifier.clickable { viewModel.selectCategory(null) }
                        )
                    }
                }
                if (sortByCheapestFirst) {
                    Text(
                        text = "🏷️ تم ترتيب مقدمي الخدمات من الأرخص سعراً للأعلى لمقارنة الأسعار بسهولة",
                        fontSize = 10.5.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // List of Service providers
        if (isProvidersLoading) {
            item {
                com.example.ui.components.ProviderListSkeleton()
            }
        } else if (displayProviders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = themeColors.textSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("عذراً، لم يتم العثور على مقدمي خدمة يطابقون هذه الفلاتر", fontSize = 13.sp, color = themeColors.textSecondary, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(displayProviders.take(providersLimit), key = { it.id }) { provider ->
                ProviderCard(
                    provider = provider,
                    themeColors = themeColors,
                    viewModel = viewModel,
                    onChatOpen = onChatOpen
                )
            }
            if (displayProviders.size > providersLimit) {
                item {
                    Button(
                        onClick = { providersLimit += 10 },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text("عرض المزيد من مقدمي الخدمات والفنيين اليمنيين ⏬", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }



        if (settingsState.bannerEnabled && settingsState.bannerLocation == "BOTTOM") {
            item {
                com.example.ui.components.AdminCustomBannerView(settingsState = settingsState, themeColors = themeColors)
            }
        }
        } // Closing activeTabName conditional branch
    }

    // --- STORES & PROPERTIES DETAILS AND REGISTRATION DIALOGS ---
    selectedStoreForDetails?.let { store ->
        StoreDetailsDialog(
            store = store,
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { selectedStoreForDetails = null },
            onOrderProductClick = { productToOrderElectronic = it }
        )
    }

    selectedPropertyForDetails?.let { prop ->
        PropertyDetailsDialog(
            property = prop,
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { selectedPropertyForDetails = null }
        )
    }

    if (showStoreCreateDialog) {
        StoreCreateEditDialog(
            store = null,
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { showStoreCreateDialog = false },
            sectionId = activeSectionIdForCreation.ifEmpty { "stores" }
        )
    }

    if (showPropertyCreateDialog) {
        PropertyCreateEditDialog(
            property = null,
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { showPropertyCreateDialog = false },
            sectionId = activeSectionIdForCreation.ifEmpty { "properties" }
        )
    }

    productToOrderElectronic?.let { product ->
        StoreProductOrderDialog(
            product = product,
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { productToOrderElectronic = null }
        )
    }

    // 5. User Submit Transfer Proof Dialog (Placed cleanly at screen level)
    payingBookingObj?.let { booking ->
        Dialog(onDismissRequest = { payingBookingObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("💳 سداد رسوم الحجز والخدمة بالمنصة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("يرجى اختيار أحد الحسابات / المحافظ التالية والتحويل إليها بقيمة تكلفة المعاينة والصيانة:", fontSize = 11.sp, color = Color.LightGray)

                    if (paymentWallets.isEmpty()) {
                        Text("⚠️ عذراً، لا توجد محافظ دفع مفعلة حالياً بالمنصة للتسديد. يرجى مراجعة المشرفين.", fontSize = 11.sp, color = Color.Red)
                    } else {
                        Text("المحافظ والحسابات المتاحة للتحويل:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            paymentWallets.filter { it.status == "active" }.forEach { wallet ->
                                val isSel = selectedUserWalletObj?.id == wallet.id
                                val name = when (wallet.provider) {
                                    "jeeb" -> "جيب 📱"
                                    "alKarimi" -> "الكريمي 🏦"
                                    "jawaly" -> "جوالي 📲"
                                    "yemenMobile" -> "يمن كاش 🇾🇪"
                                    else -> wallet.accountNameAr.take(8)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSel) themeColors.accent else Color.DarkGray,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { selectedUserWalletObj = wallet }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(name, color = if (isSel) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    selectedUserWalletObj?.let { wallet ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("رقم الحساب/المحفظة للتحويل: ${wallet.walletNumber}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                Text("اسم صاحب الحساب المستلم: ${wallet.accountNameAr}", fontSize = 11.sp, color = Color.White)
                                if (wallet.description.isNotEmpty()) {
                                    Text("تعليمات: ${wallet.description}", fontSize = 10.sp, color = Color.LightGray)
                                }
                            }
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.05f))
                    Text("يرجى تعبئة بيانات التحويل بعد إرسال المبلغ المالي:", fontSize = 11.sp, color = Color.LightGray)

                    OutlinedTextField(
                        value = userTransferIdInput,
                        onValueChange = { userTransferIdInput = it },
                        label = { Text("رقم الحوالة المرجعي / رقم العملية (الـ ID)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = userTransferAccountNameInput,
                        onValueChange = { userTransferAccountNameInput = it },
                        label = { Text("اسم المرسل الكامل (صاحب المحفظة المحوِلة)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = userTransferPhotoInput,
                        onValueChange = { userTransferPhotoInput = it },
                        label = { Text("رابط صورة الإثبات أو لقطة الشاشة", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    val proofPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.PickVisualMedia()
                    ) { uri ->
                        if (uri != null) {
                            userTransferPhotoInput = uri.toString()
                            viewModel.triggerNotification("📸 تم اختيار صورة الإثبات من المعرض بنجاح!")
                        }
                    }

                    Button(
                        onClick = {
                            proofPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (userTransferPhotoInput.isNotEmpty()) Color(0xFF10B981) else themeColors.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text(
                            if (userTransferPhotoInput.isNotEmpty()) "✅ تم اختيار صورة الإثبات (اضغط لتغييرها)" else "📷 رفع صورة الإثبات أو لقطة الشاشة من الهاتف",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (userTransferIdInput.isBlank() || userTransferAccountNameInput.isBlank()) {
                                    viewModel.triggerNotification("❌ يرجى ملء رقم الحوالة واسم مرسل الحوالة كاملاً")
                                    return@Button
                                }
                                if (settingsState.requirePaymentProofImage && userTransferPhotoInput.isBlank()) {
                                    viewModel.triggerNotification("❌ الإدارة تتطلب إرفاق صورة الإثبات أو لقطة الشاشة للتحقق!")
                                    return@Button
                                }
                                val wallet = selectedUserWalletObj ?: return@Button
                                
                                val docRef = viewModel.db.collection("payments").document()
                                val payment = com.example.data.PaymentEntity(
                                    id = docRef.id,
                                    userId = booking.customerPhone,
                                    providerId = booking.providerId,
                                    bookingId = booking.id,
                                    type = "service",
                                    method = "mobileWallet",
                                    status = "PROCESSING",
                                    amount = 1000.0,
                                    advanceAmount = 0.0,
                                    remainingAmount = 1000.0,
                                    commission = 0.0,
                                    providerShare = 1000.0,
                                    currency = "YER",
                                    isLinkedToBooking = true,
                                    transferId = userTransferIdInput,
                                    transferPhoto = userTransferPhotoInput,
                                    walletProvider = wallet.provider,
                                    walletNumber = wallet.walletNumber,
                                    walletAccountName = userTransferAccountNameInput,
                                    updatedAt = System.currentTimeMillis()
                                )
                                viewModel.db.collection("payments").document(docRef.id).set(payment).addOnSuccessListener {
                                    viewModel.triggerNotification("✅ تم إرسال إثبات السداد بنجاح! بانتظار تأكيد الإدارة.")
                                }.addOnFailureListener {
                                    viewModel.triggerNotification("❌ فشل إرسال الإثبات: ${it.message}")
                                }
                                
                                payingBookingObj = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إرسال الإثبات 📤", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { payingBookingObj = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // Advanced Search & Filter Modal Dialog
    if (showFiltersPanel) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showFiltersPanel = false }
        ) {
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, themeColors.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 20.dp)
            ) {
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(18.dp))
                            Text("معايير التصفية والبحث المتقدم", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        IconButton(onClick = { showFiltersPanel = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "إغلاق", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.15f))

                    OutlinedTextField(
                        value = phoneOrNameFilter,
                        onValueChange = { viewModel.setPhoneOrNameFilter(it) },
                        placeholder = { Text("البحث بالاسم أو رقم الهاتف...", fontSize = 11.sp, color = themeColors.textSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("المدينة اليمنية:", fontSize = 10.sp, color = themeColors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth()) {
                                val selectedCityName = citiesList.firstOrNull { it.id == activeCityId }?.nameAr ?: (if (activeCityId.isNullOrEmpty()) "كل المدن" else activeCityId ?: "كل المدن")
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E293B))
                                        .border(1.dp, themeColors.accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .clickable { isCityDropdownOpen = true }
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = selectedCityName,
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(Icons.Default.ArrowDropDown, null, tint = themeColors.accent, modifier = Modifier.size(18.dp))
                                    }
                                }

                                DropdownMenu(
                                    expanded = isCityDropdownOpen,
                                    onDismissRequest = { isCityDropdownOpen = false },
                                    modifier = Modifier
                                        .background(Color(0xFF0F172A))
                                        .border(1.dp, themeColors.accent, RoundedCornerShape(8.dp))
                                        .width(220.dp)
                                        .heightIn(max = 280.dp)
                                ) {
                                    // Search / Custom Input
                                    OutlinedTextField(
                                        value = citySearchQuery,
                                        onValueChange = { 
                                            citySearchQuery = it
                                            if (it.isNotBlank()) {
                                                viewModel.setCityFilter(it)
                                            }
                                        },
                                        placeholder = { Text("اكتب اسم المدينة...", fontSize = 10.sp, color = Color.Gray) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = Color(0xFF1E293B),
                                            unfocusedContainerColor = Color(0xFF1E293B)
                                        )
                                    )

                                    DropdownMenuItem(
                                        text = { Text("🌍 كل المدن", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            viewModel.setCityFilter(null)
                                            citySearchQuery = ""
                                            isCityDropdownOpen = false
                                        }
                                    )

                                    val baseCities = listOf(
                                        "صنعاء", "عدن", "تعز", "الحديدة", "إب", "حضرموت", "المكلا", "سيئون",
                                        "مأرب", "ذمار", "حجة", "صعدة", "أبين", "لحج", "شبوة", "المهرة",
                                        "عمران", "البيضاء", "الضالع", "ريمة", "المحويت", "سقطرى"
                                    )
                                    val dynamicCityNames = (citiesList.map { it.nameAr } + baseCities).distinct()
                                    val filteredCityNames = if (citySearchQuery.isBlank()) dynamicCityNames else dynamicCityNames.filter { it.contains(citySearchQuery, ignoreCase = true) }

                                    filteredCityNames.forEach { cityName ->
                                        DropdownMenuItem(
                                            text = { Text("📍 $cityName", color = Color.White, fontSize = 11.sp) },
                                            onClick = {
                                                val matchingId = citiesList.firstOrNull { it.nameAr == cityName }?.id ?: cityName
                                                viewModel.setCityFilter(matchingId)
                                                citySearchQuery = ""
                                                isCityDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("المنطقة / الحي:", fontSize = 10.sp, color = themeColors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = neighborFilter,
                                onValueChange = { viewModel.setNeighborhoodFilter(it) },
                                placeholder = { Text("مثال: حدة، الحصبة...", fontSize = 11.sp, color = themeColors.textSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = themeColors.accent,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedContainerColor = Color(0xFF1E293B),
                                    unfocusedContainerColor = Color(0xFF1E293B)
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("نطاق البحث الجغرافي:", fontSize = 11.sp, color = Color.White)
                            Text("${radiusKm} كم", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = radiusKm.toFloat(),
                            onValueChange = { viewModel.setRadiusKm(it.toInt().coerceAtMost(settingsState.maxSearchRadiusKm)) },
                            valueRange = 5f..50f,
                            steps = 5,
                            colors = SliderDefaults.colors(thumbColor = themeColors.accent, activeTrackColor = themeColors.accent)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isVipOnly,
                                onCheckedChange = { viewModel.toggleVipFilter() },
                                colors = CheckboxDefaults.colors(checkedColor = themeColors.accent)
                            )
                            Text("الذهبيون VIP", fontSize = 11.sp, color = Color.White)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isAvailableOnly,
                                onCheckedChange = { viewModel.toggleAvailableFilter() },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981))
                            )
                            Text("المتاحون الآن ⚡", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = filterByCurrentCityOnly,
                            onCheckedChange = { viewModel.toggleFilterByCurrentCityOnly(it) },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981))
                        )
                        Text(
                            text = if (currentUserResidence.isNotBlank()) "تصفية حسب مدينتي ($currentUserResidence)" else "تصفية عروض مدينتي القريبة",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.setPhoneOrNameFilter("")
                                viewModel.setNeighborhoodFilter("")
                                viewModel.setCityFilter(null)
                                viewModel.setRadiusKm(15)
                                if (isVipOnly) viewModel.toggleVipFilter()
                                if (isAvailableOnly) viewModel.toggleAvailableFilter()
                                if (filterByCurrentCityOnly) viewModel.toggleFilterByCurrentCityOnly(false)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إعادة ضبط 🔄", color = Color.White, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { showFiltersPanel = false },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تطبيق الفلاتر ✅", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

data class Picture2CategoryDetail(
    val title: String,
    val subtitle: String,
    val vectorIcon: ImageVector,
    val badge: String? = null
)

fun getPicture2CategoryDetail(cat: CategoryEntity): Picture2CategoryDetail {
    val id = cat.id.lowercase()
    val name = cat.name.lowercase()
    return when {
        id == "1" || id == "crafts" || id == "services" || name.contains("صيانة") || name.contains("مهن") || name.contains("فني") ->
            Picture2CategoryDetail(
                title = if (cat.name.isNotEmpty() && cat.name != "صيانة وخدمات مهنية") cat.name else "مزودو الخدمات والمهنيين",
                subtitle = "كهرباء، سباكة، بناء، عمال...",
                vectorIcon = Icons.Default.Build,
                badge = "خدمات"
            )
        id == "stores" || name.contains("محل") || name.contains("تجاري") || name.contains("معارض") ->
            Picture2CategoryDetail(
                title = if (cat.name.isNotEmpty() && cat.name != "محلات ومعارض تجارية") cat.name else "المحلات والمراكز التجارية",
                subtitle = "تسوق، معروضات، أجهزة تجارية...",
                vectorIcon = Icons.Default.ShoppingCart,
                badge = null
            )
        id == "restaurants" || name.contains("مطعم") || name.contains("وجب") || name.contains("كافيه") ->
            Picture2CategoryDetail(
                title = if (cat.name.isNotEmpty() && cat.name != "مطاعم وكافيهات") cat.name else "المطاعم والوجبات السريعة",
                subtitle = "أقرب المطاعم، كافيهات، توصيل",
                vectorIcon = Icons.Default.Place,
                badge = null
            )
        id == "2" || id == "medical" || name.contains("طب") || name.contains("صحي") || name.contains("عياد") ->
            Picture2CategoryDetail(
                title = if (cat.name.isNotEmpty() && cat.name != "طب ورعاية صحية") cat.name else "المراكز والعيادات الطبية",
                subtitle = "صيدليات، أطباء، رعاية صحية",
                vectorIcon = Icons.Default.Add,
                badge = "طبي"
            )
        id == "jobs" || name.contains("وظا") || name.contains("فرص") || name.contains("عمل") ->
            Picture2CategoryDetail(
                title = if (cat.name.isNotEmpty()) cat.name else "الفرص والوظائف والمهن",
                subtitle = "وظائف شاغرة، تقديم طلبات توظيف",
                vectorIcon = Icons.Default.AccountBox,
                badge = "وظائف"
            )
        id == "realestate" || name.contains("عقار") || name.contains("أرض") ->
            Picture2CategoryDetail(
                title = cat.name,
                subtitle = "شقق، فلل، أراضي، مكاتب...",
                vectorIcon = Icons.Default.Home,
                badge = "عقارات"
            )
        else ->
            Picture2CategoryDetail(
                title = cat.name,
                subtitle = if (cat.id.isEmpty()) "عرض جميع الخدمات والقطاعات" else "تصفح القائمة في ${cat.name}",
                vectorIcon = Icons.Default.List,
                badge = null
            )
    }
}

@Composable
fun WishlistTabContent(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onChatOpen: (String) -> Unit,
    onSelectStore: (StoreEntity) -> Unit,
    onSelectProperty: (PropertyEntity) -> Unit,
    onSwitchToHome: () -> Unit
) {
    val context = LocalContext.current
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()

    var wishlistFilterIndex by remember { mutableStateOf(0) }

    val favProviders = remember(providers, favoriteIds) {
        providers.filter { favoriteIds.contains(it.id) }
    }
    val favStores = remember(stores, favoriteIds) {
        stores.filter { favoriteIds.contains(it.id) }
    }
    val favProperties = remember(properties, favoriteIds) {
        properties.filter { favoriteIds.contains(it.id) }
    }

    val favOffers = remember(favStores, favProviders) {
        val list = mutableListOf<Triple<String, String, SpecialOfferEntity>>()
        favStores.forEach { store ->
            SpecialOfferEntity.parseList(store.specialOffersJson).forEach { offer ->
                list.add(Triple(store.name, store.phone, offer))
            }
        }
        favProviders.forEach { provider ->
            SpecialOfferEntity.parseList(provider.specialOffersJson).forEach { offer ->
                list.add(Triple(provider.name, provider.phone, offer))
            }
        }
        list
    }

    val totalCount = favProviders.size + favStores.size + favProperties.size + favOffers.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- WISHLIST HEADER BANNER ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(
                            "❤️ قائمة المفضلة والعروض الحصرية",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "إدارة المفضلة وتلقي إشعارات التخفيضات داخل التطبيق فورياً",
                            fontSize = 10.5.sp,
                            color = Color.LightGray
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFEF4444)
                ) {
                    Text(
                        "$totalCount عنصر",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // --- FILTER CHIPS ROW ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("الكل ($totalCount)", "المتاجر والمطاعم (${favStores.size}) 🛍️", "الفنيون والخدمات (${favProviders.size}) 🔧", "العروض والتخفيضات (${favOffers.size}) 🔥", "العقارات (${favProperties.size}) 🏠").forEachIndexed { idx, filterTitle ->
                val isSel = wishlistFilterIndex == idx
                Surface(
                    onClick = { wishlistFilterIndex = idx },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSel) themeColors.accent else Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(0.5.dp, if (isSel) themeColors.accent else Color.White.copy(alpha = 0.2f))
                ) {
                    Text(
                        filterTitle,
                        fontSize = 10.5.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSel) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (favoriteIds.isEmpty() || totalCount == 0) {
            // --- EMPTY WISHLIST STATE ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        "قائمة المفضلة فارغة حالياً 💔",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "تصفح المتاجر والمطاعم والخدمات واضغط على زر القلب ❤️ في أي صفحة للوصول إليها بسرعة من هنا، وسنرسل لك إشعارات داخل التطبيق فور إضافة عروض جديدة!",
                        fontSize = 11.5.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onSwitchToHome,
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تصفح المتاجر والخدمات الآن 🚀", color = Color.Black, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // --- WISHLIST CONTENT SECTIONS ---

            // 1. Special Offers Section
            if ((wishlistFilterIndex == 0 || wishlistFilterIndex == 3) && favOffers.isNotEmpty()) {
                Text("🔥 العروض والتخفيضات المتاحة للمفضلين لديك:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                favOffers.forEach { (merchantName, phone, offer) ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = Color.Red) {
                                        Text("%${offer.discountPercent} خصم 🔥", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Text(merchantName, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                                }
                                Text("⌛ ${offer.expiryDate}", fontSize = 9.5.sp, color = Color(0xFFFF9800))
                            }

                            Text(offer.title, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            if (offer.description.isNotEmpty()) {
                                Text(offer.description, fontSize = 10.5.sp, color = Color.LightGray)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${offer.offerPrice.toInt()} ر.ي", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                    if (offer.originalPrice > offer.offerPrice) {
                                        Text("${offer.originalPrice.toInt()} ر.ي", fontSize = 11.sp, color = Color.Gray, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                                    }
                                }

                                if (phone.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("📞 طلب العرض", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Favorite Stores & Restaurants Section
            if ((wishlistFilterIndex == 0 || wishlistFilterIndex == 1) && favStores.isNotEmpty()) {
                Text("🛍️ المتاجر والمطاعم المفضلة (${favStores.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                favStores.forEach { store ->
                    FavoriteStoreCard(
                        store = store,
                        themeColors = themeColors,
                        onClick = { onSelectStore(store) },
                        onRemoveFavorite = { viewModel.toggleFavorite(store.id) }
                    )
                }
            }

            // 3. Favorite Technicians & Service Providers
            if ((wishlistFilterIndex == 0 || wishlistFilterIndex == 2) && favProviders.isNotEmpty()) {
                Text("🔧 الفنيون والخدمات المفضلة (${favProviders.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF60A5FA))
                favProviders.forEach { provider ->
                    FavoriteProviderCard(
                        provider = provider,
                        themeColors = themeColors,
                        onClick = {
                            viewModel.selectedProvider = provider
                            viewModel.navigateTo("DYNAMIC_PROFILE")
                        },
                        onRemoveFavorite = { viewModel.toggleFavorite(provider.id) },
                        onCall = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // 4. Favorite Real Estate & Properties
            if ((wishlistFilterIndex == 0 || wishlistFilterIndex == 4) && favProperties.isNotEmpty()) {
                Text("🏢 العقارات والاستثمارات المحفوظة (${favProperties.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA78BFA))
                favProperties.forEach { property ->
                    FavoritePropertyCard(
                        property = property,
                        themeColors = themeColors,
                        onClick = { onSelectProperty(property) },
                        onRemoveFavorite = { viewModel.toggleFavorite(property.id) }
                    )
                }
            }
        }
    }
}
