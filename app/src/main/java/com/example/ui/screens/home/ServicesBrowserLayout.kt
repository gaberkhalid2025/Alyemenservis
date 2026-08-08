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

    // --- STORES & REAL ESTATE COMPONENT STATES ---
    val parsedSections = remember(settingsState.dynamicSectionsData) {
        com.example.data.DynamicSection.parseDynamicSections(settingsState.dynamicSectionsData)
    }
    val activeTabs = remember(parsedSections, settingsState.isStoresEnabled, settingsState.isPropertiesEnabled) {
        val list = mutableListOf("الرئيسية")
        parsedSections.filter { it.isEnabled }.forEach { sec ->
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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

        // Smart Autocomplete Search Bar
        item {
            SmartAutocompleteSearchBox(
                searchQuery = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                viewModel = viewModel,
                themeColors = themeColors,
                onToggleFilters = { showFiltersPanel = !showFiltersPanel },
                showFilters = showFiltersPanel,
                onSelectSuggestion = { type, item ->
                    when (type) {
                        "PROVIDER" -> {
                            val p = item as? ProviderEntity
                            if (p != null) {
                                viewModel.selectCategory(p.categoryId)
                            }
                        }
                        "STORE" -> {
                            val s = item as? StoreEntity
                            if (s != null) {
                                selectedStoreForDetails = s
                            }
                        }
                        "PROPERTY" -> {
                            val pr = item as? PropertyEntity
                            if (pr != null) {
                                selectedPropertyForDetails = pr
                            }
                        }
                        "PRODUCT" -> {
                            val pair = item as? Pair<*, *>
                            val store = pair?.first as? StoreEntity
                            if (store != null) {
                                selectedStoreForDetails = store
                            }
                        }
                        "JOB" -> {
                            activeTabName = "الوظائف والتوظيف"
                        }
                    }
                }
            )
        }

        // ----------------- TAB CHIPS BAR -----------------
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activeTabs.forEach { tabName ->
                    val isSelected = activeTabName == tabName
                    val icon = when (tabName) {
                        "الرئيسية" -> "🏠"
                        "الفنيين والخدمات" -> "🛠️"
                        "المحلات والمراكز", settingsState.storesTabName -> "🏪"
                        "المطاعم والكافيهات" -> "🍔"
                        "المراكز الطبية" -> "🏥"
                        "المكاتب والعقارات", settingsState.propertiesTabName -> "🏢"
                        "الوظائف والتوظيف" -> "💼"
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
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(icon, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            val currentLangState by viewModel.currentLanguage.collectAsState()
                            val displayTabName = if (currentLangState == "en") {
                                when (tabName) {
                                    "الرئيسية" -> "Home"
                                    "المفضلة" -> "Favorites"
                                    "الفنيين والخدمات" -> "Technicians"
                                    "المحلات والمراكز", settingsState.storesTabName -> "Stores"
                                    "المطاعم والكافيهات" -> "Restaurants"
                                    "المراكز الطبية" -> "Medical"
                                    "المكاتب والعقارات", settingsState.propertiesTabName -> "Properties"
                                    "الوظائف والتوظيف" -> "Jobs"
                                    else -> tabName
                                }
                            } else {
                                tabName
                            }
                            Text(
                                text = displayTabName,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- 6 MAIN SECTIONS OVERVIEW GRID (HOME TAB) ---
        if (activeTabName == "الرئيسية") {
            item {
                MainCategoriesOverviewGrid(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    settingsState = settingsState,
                    onSelectCategory = { catId ->
                        when (catId) {
                            "1", "technicians" -> activeTabName = "الفنيين والخدمات"
                            "stores" -> activeTabName = "المحلات والمراكز"
                            "restaurants" -> activeTabName = "المطاعم والكافيهات"
                            "medical" -> activeTabName = "المراكز الطبية"
                            "realestate" -> activeTabName = "المكاتب والعقارات"
                            "jobs" -> activeTabName = "الوظائف والتوظيف"
                            else -> viewModel.selectCategory(catId)
                        }
                    }
                )
            }
        }

        // --- SMART RECOMMENDATIONS & PINNED ITEMS ---
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
                val currentLangState by viewModel.currentLanguage.collectAsState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val favTitle = if (currentLangState == "en") "⭐ Favorite Tabs & Stores" else "⭐ التبويبات والمحلات المفضلة"
                    val favDesc = if (currentLangState == "en") "Your personal favorites will be saved here for quick access later." else "سيتم حفظ مفضلاتك الشخصية هنا للوصول السريع إليها لاحقاً."
                    Text(favTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text(favDesc, fontSize = 11.sp, color = themeColors.textSecondary)
                }
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

        // Expanded Filter Panel drawer settings
        if (showFiltersPanel) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🔍 معايير البحث المتقدم والفلترة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = phoneOrNameFilter,
                            onValueChange = { viewModel.setPhoneOrNameFilter(it) },
                            placeholder = { Text("البحث بالاسم أو رقم الهاتف...", fontSize = 11.sp, color = themeColors.textSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("المدينة اليمنية:", fontSize = 10.sp, color = themeColors.textSecondary)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .clickable {
                                            val idx = citiesList.indexOfFirst { it.id == activeCityId }
                                            val nextIdx = if (idx == -1) 0 else if (idx == citiesList.size -1) -1 else idx + 1
                                            viewModel.setCityFilter(if (nextIdx == -1) null else citiesList[nextIdx].id)
                                        }
                                        .padding(10.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = citiesList.firstOrNull { it.id == activeCityId }?.nameAr ?: "كل المدن",
                                            fontSize = 11.sp,
                                            color = Color.White
                                        )
                                        Icon(Icons.Default.ArrowDropDown, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("المنطقة / الحي:", fontSize = 10.sp, color = themeColors.textSecondary)
                                OutlinedTextField(
                                    value = neighborFilter,
                                    onValueChange = { viewModel.setNeighborhoodFilter(it) },
                                    placeholder = { Text("مثال: حدة، الحصبة...", fontSize = 11.sp, color = themeColors.textSecondary) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    singleLine = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("البحث بنطاق جغرافي (دائرة):", fontSize = 11.sp, color = themeColors.textPrimary)
                                Text("${radiusKm} كم (الحد الأدنى)", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
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
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isVipOnly, onCheckedChange = { viewModel.toggleVipFilter() })
                                Text("العضوية الذهبية معتمدة", fontSize = 11.sp, color = Color.White)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isAvailableOnly, onCheckedChange = { viewModel.toggleAvailableFilter() })
                                Text("المتاحين الآن فقط", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Horizontal Grid list of categories (Responsive Grid / Custom Admin Display Layout)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "📁 تصفح حسب الأقسام (تصنيف تفاعلي):",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val allCats = listOf(
                    CategoryEntity(id = "", name = "الكل", icon = "🌐")
                ) + categories

                when (settingsState.categoriesLayoutType) {
                    "ROW_HORIZONTAL" -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            allCats.forEach { cat ->
                                val isSelected = if (cat.id.isEmpty()) selectedCategory == null else selectedCategory == cat.id
                                Card(
                                    modifier = Modifier
                                        .clickable {
                                            if (cat.id.isEmpty()) {
                                                viewModel.selectCategory(null)
                                            } else {
                                                viewModel.selectCategory(cat.id)
                                            }
                                        }
                                        .testTag("category_row_${cat.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) themeColors.accent else themeColors.surface
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) Color.Transparent else themeColors.accent.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = cat.icon,
                                            fontSize = 18.sp,
                                            modifier = Modifier
                                                .background(
                                                    color = (if (isSelected) Color.Black else themeColors.primary).copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(6.dp)
                                        )
                                        Text(
                                            text = cat.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "GRID_HORIZONTAL" -> {
                        val chunkSize = (allCats.size + 1) / 2
                        val row1 = allCats.take(chunkSize)
                        val row2 = allCats.drop(chunkSize)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row1.forEach { cat ->
                                    val isSelected = if (cat.id.isEmpty()) selectedCategory == null else selectedCategory == cat.id
                                    Card(
                                        modifier = Modifier
                                            .clickable {
                                                if (cat.id.isEmpty()) {
                                                    viewModel.selectCategory(null)
                                                } else {
                                                    viewModel.selectCategory(cat.id)
                                                }
                                            }
                                            .testTag("category_gridh1_${cat.id}"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) themeColors.accent else themeColors.surface
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) Color.Transparent else themeColors.accent.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = cat.icon,
                                                fontSize = 16.sp,
                                                modifier = Modifier
                                                    .background(
                                                        color = (if (isSelected) Color.Black else themeColors.primary).copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(4.dp)
                                            )
                                            Text(
                                                text = cat.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.Black else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row2.forEach { cat ->
                                    val isSelected = if (cat.id.isEmpty()) selectedCategory == null else selectedCategory == cat.id
                                    Card(
                                        modifier = Modifier
                                            .clickable {
                                                if (cat.id.isEmpty()) {
                                                    viewModel.selectCategory(null)
                                                } else {
                                                    viewModel.selectCategory(cat.id)
                                                }
                                            }
                                            .testTag("category_gridh2_${cat.id}"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) themeColors.accent else themeColors.surface
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) Color.Transparent else themeColors.accent.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = cat.icon,
                                                fontSize = 16.sp,
                                                modifier = Modifier
                                                    .background(
                                                        color = (if (isSelected) Color.Black else themeColors.primary).copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(4.dp)
                                            )
                                            Text(
                                                text = cat.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.Black else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "LIST_VERTICAL" -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allCats.forEach { cat ->
                                val isSelected = if (cat.id.isEmpty()) selectedCategory == null else selectedCategory == cat.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (cat.id.isEmpty()) {
                                                viewModel.selectCategory(null)
                                            } else {
                                                viewModel.selectCategory(cat.id)
                                            }
                                        }
                                        .testTag("category_list_${cat.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) themeColors.accent else themeColors.surface
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) Color.Transparent else themeColors.accent.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = cat.icon,
                                            fontSize = 22.sp,
                                            modifier = Modifier
                                                .background(
                                                    color = (if (isSelected) Color.Black else themeColors.primary).copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(6.dp)
                                        )
                                        Column {
                                            Text(
                                                text = cat.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.Black else Color.White
                                            )
                                            Text(
                                                text = if (cat.id.isEmpty()) "كل الخدمات اليمنية المتكاملة" else "عرض المختصين في $cat.name",
                                                fontSize = 10.sp,
                                                color = if (isSelected) Color.Black.copy(alpha = 0.7f) else themeColors.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> { // "GRID_VERTICAL"
                        val columns = 2
                        val rows = allCats.chunked(columns)
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { cat ->
                                    val isSelected = if (cat.id.isEmpty()) selectedCategory == null else selectedCategory == cat.id
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                if (cat.id.isEmpty()) {
                                                    viewModel.selectCategory(null)
                                                } else {
                                                    viewModel.selectCategory(cat.id)
                                                }
                                            }
                                            .testTag("category_grid_${cat.id}"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) themeColors.accent else themeColors.surface
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) Color.Transparent else themeColors.accent.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = cat.icon,
                                                fontSize = 18.sp,
                                                modifier = Modifier
                                                    .background(
                                                        color = (if (isSelected) Color.Black else themeColors.primary).copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(6.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = cat.name,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.Black else Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (cat.id.isEmpty()) "كل الخدمات اليمنية" else "عرض المختصين",
                                                    fontSize = 8.sp,
                                                    color = if (isSelected) Color.Black.copy(alpha = 0.7f) else themeColors.textSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                                if (rowItems.size < columns) {
                                    Spacer(modifier = Modifier.weight(columns - rowItems.size.toFloat()))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Services Providers Headers
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💼 مقدمو الخدمات المتوفرون (${filteredProviders.size}):",
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
        }

        // List of Service providers
        if (filteredProviders.isEmpty()) {
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
            items(filteredProviders.take(providersLimit), key = { it.id }) { provider ->
                ProviderCard(
                    provider = provider,
                    themeColors = themeColors,
                    viewModel = viewModel,
                    onChatOpen = onChatOpen
                )
            }
            if (filteredProviders.size > providersLimit) {
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
}

// -------------------------------------------------------------------------------------
// SMART AUTOCOMPLETE SEARCH BOX COMPONENT
// -------------------------------------------------------------------------------------
@Composable
fun SmartAutocompleteSearchBox(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onToggleFilters: () -> Unit,
    showFilters: Boolean,
    onSelectSuggestion: (type: String, item: Any) -> Unit
) {
    val providers by viewModel.providers.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val jobs by viewModel.jobs.collectAsState()
    val settingsState by viewModel.settings.collectAsState()

    var isDropdownExpanded by remember { mutableStateOf(false) }

    val cleanQuery = searchQuery.trim().lowercase()

    val suggestions = remember(cleanQuery, providers, stores, properties, jobs) {
        if (cleanQuery.isEmpty()) emptyList()
        else {
            val list = mutableListOf<SearchSuggestionItem>()

            // 1. Technicians & Providers
            providers.filter { p ->
                !p.isDeleted && (
                    p.name.lowercase().contains(cleanQuery) ||
                    p.profession.lowercase().contains(cleanQuery) ||
                    p.specialization.lowercase().contains(cleanQuery) ||
                    p.area.lowercase().contains(cleanQuery) ||
                    p.localNeighborhood.lowercase().contains(cleanQuery) ||
                    p.phone.contains(cleanQuery)
                )
            }.take(4).forEach { p ->
                list.add(SearchSuggestionItem(
                    type = "PROVIDER",
                    id = p.id,
                    title = p.name,
                    subtitle = "${p.profession} - ${p.area}",
                    badge = "🛠️ فني/خدمة",
                    rawItem = p
                ))
            }

            // 2. Stores & Shops & Restaurants & Medical Centers
            stores.filter { s ->
                !s.isDeleted && s.isActive && (
                    s.name.lowercase().contains(cleanQuery) ||
                    s.description.lowercase().contains(cleanQuery) ||
                    s.categoryId.lowercase().contains(cleanQuery) ||
                    s.phone.contains(cleanQuery) ||
                    s.localNeighborhood.lowercase().contains(cleanQuery)
                )
            }.take(5).forEach { s ->
                val badge = when {
                    s.sectionId == "restaurants" || s.categoryId.contains("rest") -> "🍔 مطعم/كافيه"
                    s.sectionId == "medical" || s.categoryId.contains("med") -> "🏥 مركز طبي"
                    else -> "🏪 محل/مركز تجاري"
                }
                list.add(SearchSuggestionItem(
                    type = "STORE",
                    id = s.id,
                    title = s.name,
                    subtitle = "${s.description.take(25)} - ${s.phone}",
                    badge = badge,
                    rawItem = s
                ))
            }

            // 3. Real Estate Properties
            properties.filter { pr ->
                !pr.isDeleted && pr.isActive && (
                    pr.title.lowercase().contains(cleanQuery) ||
                    pr.description.lowercase().contains(cleanQuery) ||
                    pr.localNeighborhood.lowercase().contains(cleanQuery) ||
                    pr.propertyType.lowercase().contains(cleanQuery)
                )
            }.take(3).forEach { pr ->
                list.add(SearchSuggestionItem(
                    type = "PROPERTY",
                    id = pr.id,
                    title = pr.title,
                    subtitle = "${pr.price} YER - ${pr.type}",
                    badge = "🏢 عقار/مكتب",
                    rawItem = pr
                ))
            }

            // 4. Jobs
            jobs.filter { j ->
                !j.isDeleted && (
                    j.title.lowercase().contains(cleanQuery) ||
                    j.companyName.lowercase().contains(cleanQuery) ||
                    j.address.lowercase().contains(cleanQuery)
                )
            }.take(3).forEach { j ->
                list.add(SearchSuggestionItem(
                    type = "JOB",
                    id = j.id,
                    title = j.title,
                    subtitle = "${j.companyName} - ${j.address}",
                    badge = "💼 وظيفة",
                    rawItem = j
                ))
            }

            list.take(8)
        }
    }

    LaunchedEffect(cleanQuery) {
        isDropdownExpanded = cleanQuery.isNotEmpty() && suggestions.isNotEmpty()
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(themeColors.surface, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Search, contentDescription = "أيقونة البحث الذكي", tint = themeColors.textSecondary)
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    onQueryChange(it)
                    isDropdownExpanded = it.isNotBlank()
                },
                placeholder = {
                    Text(
                        "ابحث بالاسم، فني، مطعم، وجبة، عصير، منتج، عقار، دواء، منطقة...",
                        fontSize = 11.sp,
                        color = themeColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_text_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onQueryChange(""); isDropdownExpanded = false }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "مسح", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }

            if (settingsState.isSpeechSearchEnabled) {
                IconButton(onClick = {
                    VoiceManager.onHear?.invoke { spokenText ->
                        onQueryChange(spokenText)
                        viewModel.triggerNotification("🎙️ تم سماع صوتك اليمني: $spokenText")
                    }
                }) {
                    Text("🎙️", fontSize = 18.sp)
                }
            }

            IconButton(onClick = onToggleFilters) {
                Icon(
                    imageVector = if (showFilters) Icons.Default.Settings else Icons.Default.List,
                    contentDescription = "الفلاتر",
                    tint = themeColors.accent
                )
            }
        }

        // Suggestions Dropdown Popup Overlay
        if (isDropdownExpanded && suggestions.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 54.dp)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Text(
                        "💡 نتائج البحث والاقتراحات التلقائية المطابقة (${suggestions.size}):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    HorizontalDivider(color = themeColors.accent.copy(alpha = 0.2f))

                    suggestions.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isDropdownExpanded = false
                                    onSelectSuggestion(item.type, item.rawItem)
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(item.subtitle, fontSize = 10.sp, color = themeColors.textSecondary)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = themeColors.accent.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    item.badge,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.accent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
                    }
                }
            }
        }
    }
}

data class SearchSuggestionItem(
    val type: String,
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val rawItem: Any
)

// -------------------------------------------------------------------------------------
// 6 MAIN SECTIONS OVERVIEW GRID COMPONENT
// -------------------------------------------------------------------------------------
@Composable
fun MainCategoriesOverviewGrid(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    settingsState: AdminSettingsEntity,
    onSelectCategory: (String) -> Unit
) {
    val parsedSections = remember(settingsState.dynamicSectionsData) {
        DynamicSection.parseDynamicSections(settingsState.dynamicSectionsData)
    }

    val mainCategories = listOf(
        MainCategoryItem("1", "قسم الفنيين والخدمات", "🛠️", "نخبة المهنيين والصيانة والمعاينة الفورية", "#1E3A8A"),
        MainCategoryItem("stores", "قسم المحلات والمراكز التجارية", "🏪", "تسوق من المحلات والمعارض والمولات", "#15803D"),
        MainCategoryItem("restaurants", "قسم المطاعم والكافيهات", "🍔", "أشهر الوجبات، المأكولات والعصائر", "#C2410C"),
        MainCategoryItem("medical", "قسم المراكز الطبية والمستشفيات", "🏥", "عيادات، صيدليات، أطباء ومستشفيات", "#047857"),
        MainCategoryItem("realestate", "قسم المكاتب والخدمات العقارية", "🏢", "شقق، فلل، أراضي ومحلات للإيجار والبيع", "#B45309"),
        MainCategoryItem("jobs", "قسم المعلنين عن الوظائف والتوظيف", "💼", "فرص عمل متجددة لكافة التخصصات", "#6B21A8")
    ).filter { cat ->
        when (cat.id) {
            "stores" -> settingsState.isStoresEnabled && parsedSections.any { it.id == "stores" && it.isEnabled }
            "realestate" -> settingsState.isPropertiesEnabled && parsedSections.any { it.id == "properties" && it.isEnabled }
            "restaurants" -> parsedSections.any { it.id == "restaurants" && it.isEnabled }
            "medical" -> parsedSections.any { it.id == "medical" && it.isEnabled }
            "jobs" -> parsedSections.any { it.id == "jobs" && it.isEnabled }
            else -> true
        }
    }

    if (mainCategories.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(
                text = "✨ الأقسام والخدمات الرئيسية بالتطبيق:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.accent,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val chunked = mainCategories.chunked(2)
            chunked.forEach { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { cat ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectCategory(cat.id) }
                                .shadow(4.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = themeColors.accent.copy(alpha = 0.15f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(cat.icon, fontSize = 20.sp)
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = themeColors.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = cat.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = cat.description,
                                    fontSize = 9.sp,
                                    color = themeColors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

data class MainCategoryItem(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val hexAccent: String
)

