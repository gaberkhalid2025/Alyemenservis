package com.example

import com.example.ui.ProductAttachmentsSection
import com.example.ui.SpecialOffersSection
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import com.example.data.*
import com.example.ui.MainViewModel

@Composable
fun rememberBase64Bitmap(base64Str: String): ImageBitmap? {
    return remember(base64Str) {
        if (base64Str.isNotEmpty() && !base64Str.startsWith("http") && !base64Str.startsWith("content")) {
            try {
                val cleanBase64 = if (base64Str.contains(",")) base64Str.substringAfter(",") else base64Str
                val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                bitmap?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
}

// --------------------------------------------------------
// 1. SMART RECOMMENDATIONS SECTION
// --------------------------------------------------------
@Composable
fun SmartRecommendationsSection(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onStoreClick: (StoreEntity) -> Unit,
    onPropertyClick: (PropertyEntity) -> Unit
) {
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val settingsState by viewModel.settings.collectAsState()

    // Filter active items
    val activeStores = remember(stores) { stores.filter { it.isActive && !it.isDeleted } }
    val activeProps = remember(properties) { properties.filter { it.isActive && !it.isDeleted } }

    // Logic: Pinned by admin only
    val recommendedStores = remember(activeStores, settingsState.isStoresEnabled) {
        if (!settingsState.isStoresEnabled) emptyList()
        else activeStores.filter { it.isPinned }
            .sortedWith(compareByDescending<StoreEntity> { it.rating }
                .thenByDescending { it.createdAt })
            .take(5)
    }

    val recommendedProps = remember(activeProps, settingsState.isPropertiesEnabled) {
        if (!settingsState.isPropertiesEnabled) emptyList()
        else activeProps.filter { it.isPinned }
            .sortedWith(compareByDescending<PropertyEntity> { it.rating }
                .thenByDescending { it.createdAt })
            .take(5)
    }

    if (recommendedStores.isNotEmpty() || recommendedProps.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(themeColors.surface, RoundedCornerShape(16.dp))
                .padding(14.dp)
                .border(1.dp, themeColors.accent.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💡", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "توصيات ذكية منتقاة لك",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                    Text(
                        text = "محلات وعقارات متميزة بناءً على التقييمات وتفضيلات المستخدمين",
                        fontSize = 10.sp,
                        color = themeColors.textSecondary
                    )
                }
            }

            if (settingsState.recommendationsLayout == "GRID_HORIZONTAL") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(recommendedStores) { store ->
                        RecommendationItemCard(
                            title = store.name,
                            subtitle = store.description,
                            image = store.logoImage.ifEmpty { store.coverImage },
                            rating = store.rating,
                            badge = "متجر مميز",
                            themeColors = themeColors,
                            onClick = { onStoreClick(store) }
                        )
                    }
                    items(recommendedProps) { prop ->
                        val label = if (prop.type == "rent") "للإيجار" else "للبيع"
                        RecommendationItemCard(
                            title = prop.title,
                            subtitle = "${prop.price} YER - $label",
                            image = prop.images.firstOrNull() ?: "",
                            rating = prop.rating,
                            badge = "عقار متميز",
                            themeColors = themeColors,
                            onClick = { onPropertyClick(prop) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendationItemCard(
    title: String,
    subtitle: String,
    image: String,
    rating: Float,
    badge: String,
    themeColors: VisualThemePalette,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
            .border(1.dp, themeColors.accent.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray)
            ) {
                if (image.isNotEmpty()) {
                    // Simulating high-quality local image load placeholder
                    Box(modifier = Modifier.fillMaxSize().background(themeColors.primary.copy(alpha = 0.2f))) {
                        Text(
                            text = "📸",
                            modifier = Modifier.align(Alignment.Center),
                            fontSize = 24.sp
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = Color.Gray)
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(themeColors.accent, RoundedCornerShape(bottomEnd = 8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(badge, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = themeColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⭐", fontSize = 10.sp)
                Spacer(modifier = Modifier.width(3.dp))
                Text(String.format("%.1f", rating), fontSize = 10.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --------------------------------------------------------
// 2. STORES TAB CONTENT
// --------------------------------------------------------
@Composable
fun StoresTabContent(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onStoreClick: (StoreEntity) -> Unit,
    onAddStoreClick: () -> Unit,
    sectionId: String = "stores"
) {
    val stores by viewModel.stores.collectAsState()
    val cities by viewModel.cities.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val currentPhone by viewModel.currentUserPhone.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()
    val isEn = currentLang == "en"

    val sectionCategories = remember(categories, sectionId) {
        val filtered = categories.filter { it.parentId == sectionId && !it.isMainCategory }
        if (filtered.isNotEmpty()) filtered
        else {
            when (sectionId) {
                "restaurants" -> listOf(
                    CategoryEntity("sub_rest_1", "مطاعم يمنية وشرقية", "🍲", 1, parentId = "restaurants"),
                    CategoryEntity("sub_rest_2", "وجبات سريعة وبرجر", "🍔", 2, parentId = "restaurants"),
                    CategoryEntity("sub_rest_3", "كافيهات ومشروبات", "☕", 3, parentId = "restaurants"),
                    CategoryEntity("sub_rest_4", "حلويات ومخابز", "🍰", 4, parentId = "restaurants"),
                    CategoryEntity("sub_rest_5", "مشويات وأسماك", "🥩", 5, parentId = "restaurants")
                )
                "medical", "2" -> listOf(
                    CategoryEntity("sub_med_1", "عيادات وأطباء", "🩺", 1, parentId = "medical"),
                    CategoryEntity("sub_med_2", "صيدليات ومستلزمات", "💊", 2, parentId = "medical"),
                    CategoryEntity("sub_med_3", "مختبرات تحاليل", "🔬", 3, parentId = "medical"),
                    CategoryEntity("sub_med_4", "مراكز علاج طبيعي", "🧘", 4, parentId = "medical"),
                    CategoryEntity("sub_med_5", "مستشفيات ومراكز تخصصية", "🏥", 5, parentId = "medical")
                )
                "centers" -> listOf(
                    CategoryEntity("sub_center_1", "مراكز تجميل وصالونات", "✂️", 1, parentId = "centers"),
                    CategoryEntity("sub_center_2", "مراكز طبية وتخصصية", "🏥", 2, parentId = "centers"),
                    CategoryEntity("sub_center_3", "مراكز تعليم وتدريب", "🎓", 3, parentId = "centers"),
                    CategoryEntity("sub_center_4", "أندية وصالات رياضية", "🏋️", 4, parentId = "centers")
                )
                "realestate" -> listOf(
                    CategoryEntity("sub_prop_1", "شقق للإيجار والبيع", "🏢", 1, parentId = "realestate"),
                    CategoryEntity("sub_prop_2", "فلل وقصور", "🏰", 2, parentId = "realestate"),
                    CategoryEntity("sub_prop_3", "أراضي ومخططات", "🏞️", 3, parentId = "realestate"),
                    CategoryEntity("sub_prop_4", "مكاتب ومحلات تجارية", "🏪", 4, parentId = "realestate"),
                    CategoryEntity("sub_prop_5", "شاليهات واستراحات", "🏊", 5, parentId = "realestate")
                )
                "jobs" -> listOf(
                    CategoryEntity("sub_job_1", "وظائف هندسية وتقنية", "💻", 1, parentId = "jobs"),
                    CategoryEntity("sub_job_2", "وظائف طبية وصحية", "🩺", 2, parentId = "jobs"),
                    CategoryEntity("sub_job_3", "مبيعات وتسويق", "📈", 3, parentId = "jobs"),
                    CategoryEntity("sub_job_4", "محاسبة وإدارة", "📑", 4, parentId = "jobs"),
                    CategoryEntity("sub_job_5", "حرف وخدمات مهنية", "🔧", 5, parentId = "jobs")
                )
                else -> listOf( // "stores"
                    CategoryEntity("sub_store_1", "ملابس وأزياء", "👔", 1, parentId = "stores"),
                    CategoryEntity("sub_store_2", "إلكترونيات وهواتف", "📱", 2, parentId = "stores"),
                    CategoryEntity("sub_store_3", "أجهزة منزلية وكهربائية", "📺", 3, parentId = "stores"),
                    CategoryEntity("sub_store_4", "سوبرماركت ومواد غذائية", "🛒", 4, parentId = "stores"),
                    CategoryEntity("sub_store_5", "عطور ومستحضرات تجميل", "💄", 5, parentId = "stores")
                )
            }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCityId by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf("") }
    var itemsToShowLimit by remember { mutableStateOf(10) }

    val activeStores = remember(stores, searchQuery, selectedCityId, selectedCatId, sectionId) {
        stores.filter {
            !it.isDeleted &&
            (it.sectionId == sectionId || (sectionId == "medical" && it.sectionId == "centers") || (sectionId == "centers" && it.sectionId == "medical")) &&
            (it.isActive || adminRole != "GUEST" || it.ownerId == currentPhone) &&
            (searchQuery.isEmpty() || it.name.contains(searchQuery, true) || it.description.contains(searchQuery, true)) &&
            (selectedCityId.isEmpty() || it.cityId == selectedCityId) &&
            (selectedCatId.isEmpty() || it.categoryId == selectedCatId)
        }.sortedWith(compareByDescending<StoreEntity> { it.isPinned }.thenBy { it.displayOrder })
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Real-time join request status banner for shop owners (Transitions immediately to full profile view after approval)
        val myStore = stores.find { !it.isDeleted && it.ownerId == currentPhone }
        myStore?.let { store ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (store.isActive) Color(0xFF0F291E) else Color(0xFF2C2414)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (store.isActive) Color(0xFF22C55E).copy(alpha = 0.5f) else Color(0xFFEAB308).copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (store.isActive) {
                                if (isEn) "✅ Your business profile was successfully activated!" else "✅ تم تفعيل ملفك التعريفي بنجاح!"
                            } else {
                                if (isEn) "⏳ Your shop registration request is under review" else "⏳ طلب انضمام محلك قيد المراجعة"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (store.isActive) Color(0xFF4ADE80) else Color(0xFFFACC15)
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    if (store.isActive) Color(0xFF22C55E).copy(alpha = 0.2f) else Color(0xFFEAB308).copy(alpha = 0.2f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (store.isActive) {
                                    if (isEn) "Active" else "نشط ومفعل"
                                } else {
                                    if (isEn) "Pending" else "قيد التدقيق الإداري"
                                },
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (store.isActive) Color(0xFF4ADE80) else Color(0xFFFACC15)
                            )
                        }
                    }
                    Text(
                        text = if (store.isActive) {
                            if (isEn) "Welcome! Your shop (${store.name}) has been accepted and published. You can now receive orders and customer reviews." else "أهلاً بك يا غالي! تم قبول محلك (${store.name}) ونشره رسمياً للجمهور. يمكنك الآن استقبال طلبات الشراء والتقييمات من الزبائن."
                        } else {
                            if (isEn) "Your shop registration request (${store.name}) was successfully submitted. It is currently under administrative verification." else "تم تقديم طلب تسجيل محلك (${store.name}) بنجاح. ملفك ومستنداتك قيد التدقيق والفحص الإداري الآن من قبل الإدارة وسنرسل لك إشعاراً فور تفعيله."
                        },
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Button(
                        onClick = { onStoreClick(store) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (store.isActive) Color(0xFF22C55E) else Color(0xFFEAB308)
                        ),
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (store.isActive) {
                                if (isEn) "📂 View & Manage Completed Profile" else "📂 عرض وإدارة ملفك الشخصي المكتمل"
                            } else {
                                if (isEn) "👁️ Preview Submitted Details" else "👁️ عرض ومعاينة تفاصيل ملفك المقدم"
                            },
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Search & Filters Header
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isEn) "Search for store name or service..." else "ابحث عن اسم المتجر أو الخدمة المعروضة...", fontSize = 12.sp, color = themeColors.textSecondary) },
                    modifier = Modifier.fillMaxWidth().testTag("store_search_input"),
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = themeColors.textSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // City selector
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        val cityLabel = if (selectedCityId.isEmpty()) {
                            if (isEn) "All Cities" else "كل المحافظات"
                        } else {
                            val city = cities.find { it.id == selectedCityId }
                            if (isEn) (city?.nameEn ?: city?.nameAr ?: "") else (city?.nameAr ?: "")
                        }
                        Button(
                            onClick = { expanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(cityLabel, fontSize = 11.sp, color = Color.White)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(
                                text = { Text(if (isEn) "All Cities" else "كل المحافظات") },
                                onClick = { selectedCityId = ""; expanded = false }
                            )
                            cities.forEach { city ->
                                DropdownMenuItem(
                                    text = { Text(if (isEn) city.nameEn else city.nameAr) },
                                    onClick = { selectedCityId = city.id; expanded = false }
                                )
                            }
                        }
                    }

                    // Category selector
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        val catLabel = sectionCategories.find { it.id == selectedCatId }?.let { cat ->
                            if (isEn) {
                                when (cat.name) {
                                    "المحلات والمراكز" -> "Malls & Stores"
                                    "المطاعم والكافيهات" -> "Restaurants & Cafes"
                                    "عقارات وأراضي" -> "Real Estate"
                                    else -> cat.name
                                }
                            } else cat.name
                        } ?: (if (isEn) "All Categories" else "كل الفئات")
                        Button(
                            onClick = { expanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(catLabel, fontSize = 11.sp, color = Color.White)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(
                                text = { Text(if (isEn) "All Categories" else "كل الفئات") },
                                onClick = { selectedCatId = ""; expanded = false }
                            )
                            sectionCategories.forEach { cat ->
                                val catName = if (isEn) {
                                    when (cat.name) {
                                        "المحلات والمراكز" -> "Malls & Stores"
                                        "المطاعم والكافيهات" -> "Restaurants & Cafes"
                                        "عقارات وأراضي" -> "Real Estate"
                                        else -> cat.name
                                    }
                                } else cat.name
                                DropdownMenuItem(
                                    text = { Text("${cat.icon} $catName") },
                                    onClick = { selectedCatId = cat.id; expanded = false }
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEn) "🏪 Registered Yemeni Stores (${activeStores.size}):" else "🏪 المتاجر اليمنية المسجلة (${activeStores.size}):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { viewModel.triggerRestoreAccountDialog.value = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(if (isEn) "Restore 🔓" else "استرجاع 🔓", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onAddStoreClick,
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(if (isEn) "Add Store" else "إضافة متجر", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (activeStores.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isEn) "No stores match the current search filters." else "لم يتم العثور على أي متجر يطابق فلاتر البحث الحالية.", color = themeColors.textSecondary, fontSize = 12.sp)
            }
        } else {
            activeStores.take(itemsToShowLimit).forEach { store ->
                StoreListItemCard(store = store, themeColors = themeColors, onClick = { onStoreClick(store) }, viewModel = viewModel)
            }
            if (activeStores.size > itemsToShowLimit) {
                Button(
                    onClick = { itemsToShowLimit += 10 },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(if (isEn) "Load More Stores ⏬" else "عرض المزيد من المتاجر اليمنية ⏬", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StoreListItemCard(
    store: StoreEntity,
    themeColors: VisualThemePalette,
    onClick: () -> Unit,
    viewModel: MainViewModel? = null
) {
    com.example.ui.CompactStoreCard(
        store = store,
        onOpenDetails = onClick,
        onOpenReviews = onClick,
        onAddReview = onClick,
        onRequestBooking = {
            if (viewModel != null) {
                viewModel.selectedStore = store
                viewModel.navigateTo("CREATE_BOOKING")
            } else {
                onClick()
            }
        },
        onDirectChat = {
            if (viewModel != null) {
                viewModel.openDirectChatWithEntity(store.id, store.name, store.phone, "STORE")
            } else {
                onClick()
            }
        },
        onAgoraCall = {
            viewModel?.startVoiceCall(store.name, "متجر / مركز تجاري HD")
        }
    )
}

// --------------------------------------------------------
// 3. PROPERTIES TAB CONTENT
// --------------------------------------------------------
@Composable
fun PropertiesTabContent(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onPropertyClick: (PropertyEntity) -> Unit,
    onAddPropertyClick: () -> Unit,
    sectionId: String = "properties"
) {
    val properties by viewModel.properties.collectAsState()
    val cities by viewModel.cities.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val currentPhone by viewModel.currentUserPhone.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()
    val isEn = currentLang == "en"

    var searchQuery by remember { mutableStateOf("") }
    var selectedCityId by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("") } // rent, sale, empty
    var filterPropType by remember { mutableStateOf("") } // apartment, house, land, shop, empty
    var itemsToShowLimit by remember { mutableStateOf(10) }

    val activeProps = remember(properties, searchQuery, selectedCityId, filterType, filterPropType, sectionId) {
        properties.filter {
            !it.isDeleted &&
            (it.sectionId == sectionId || (sectionId == "properties" && (it.sectionId == "properties" || it.sectionId == "realestate" || it.sectionId.isEmpty()))) &&
            (it.isActive || adminRole != "GUEST" || it.ownerId == currentPhone) &&
            (searchQuery.isEmpty() || it.title.contains(searchQuery, true) || it.description.contains(searchQuery, true)) &&
            (selectedCityId.isEmpty() || it.cityId == selectedCityId) &&
            (filterType.isEmpty() || it.type == filterType) &&
            (filterPropType.isEmpty() || it.propertyType == filterPropType)
        }.sortedByDescending { it.isPinned }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Real-time join request status banner for property advertisers (Transitions immediately to full profile view after approval)
        val myProp = properties.find { !it.isDeleted && it.ownerId == currentPhone }
        myProp?.let { prop ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (prop.isActive) Color(0xFF0F291E) else Color(0xFF2C2414)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (prop.isActive) Color(0xFF22C55E).copy(alpha = 0.5f) else Color(0xFFEAB308).copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (prop.isActive) {
                                if (isEn) "✅ Property advertisement successfully activated!" else "✅ تم تفعيل إعلان العقار بنجاح!"
                            } else {
                                if (isEn) "⏳ Property listing request under review" else "⏳ طلب إدراج العقار قيد المراجعة"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (prop.isActive) Color(0xFF4ADE80) else Color(0xFFFACC15)
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    if (prop.isActive) Color(0xFF22C55E).copy(alpha = 0.2f) else Color(0xFFEAB308).copy(alpha = 0.2f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (prop.isActive) {
                                    if (isEn) "Published" else "منشور وعامل"
                                } else {
                                    if (isEn) "Pending" else "قيد التدقيق الإداري"
                                },
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (prop.isActive) Color(0xFF4ADE80) else Color(0xFFFACC15)
                            )
                        }
                    }
                    Text(
                        text = if (prop.isActive) {
                            if (isEn) "Welcome! Your property advertisement (${prop.title}) was approved and published. You can now receive calls and inquiries from buyers." else "أهلاً بك يا غالي! تم الموافقة على إعلان عقارك (${prop.title}) ونشره رسمياً للجمهور. يمكنك الآن استقبال الاتصالات والاستفسارات من المشترين المهتمين."
                        } else {
                            if (isEn) "Your property listing request (${prop.title}) was submitted. Data and documents are under administrative review, and you will be notified once published." else "تم تقديم طلب إدراج عقارك (${prop.title}) بنجاح. بياناتك والمستندات المرفقة قيد التدقيق والفحص الإداري حالياً من قبل الإدارة لضمان المصداقية وسننبهك فور نشره."
                        },
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Button(
                        onClick = { onPropertyClick(prop) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (prop.isActive) Color(0xFF22C55E) else Color(0xFFEAB308)
                        ),
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (prop.isActive) {
                                if (isEn) "📂 View & Manage Published Property" else "📂 عرض وإدارة ملف العقار المنشور"
                            } else {
                                if (isEn) "👁️ Preview Listing Details" else "👁️ عرض ومعاينة تفاصيل طلبك وعقد الملكية"
                            },
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Search & Filters Header
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isEn) "Search for apartment, building, land..." else "ابحث عن شقة، عمارة، أرض، دكان...", fontSize = 12.sp, color = themeColors.textSecondary) },
                    modifier = Modifier.fillMaxWidth().testTag("property_search_input"),
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = themeColors.textSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Type selector (Rent/Sale)
                    val types = listOf(
                        Pair("", if (isEn) "All" else "الكل"),
                        Pair("rent", if (isEn) "For Rent" else "للإيجار"),
                        Pair("sale", if (isEn) "For Sale" else "للبيع")
                    )
                    types.forEach { t ->
                        val isSel = filterType == t.first
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSel) themeColors.accent else Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable { filterType = t.first }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(t.second, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // City selector
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        val cityLabel = if (selectedCityId.isEmpty()) {
                            if (isEn) "All Cities" else "كل المحافظات"
                        } else {
                            val city = cities.find { it.id == selectedCityId }
                            if (isEn) (city?.nameEn ?: city?.nameAr ?: "") else (city?.nameAr ?: "")
                        }
                        Button(
                            onClick = { expanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(cityLabel, fontSize = 10.sp, color = Color.White)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(
                                text = { Text(if (isEn) "All Cities" else "كل المحافظات") },
                                onClick = { selectedCityId = ""; expanded = false }
                            )
                            cities.forEach { city ->
                                DropdownMenuItem(
                                    text = { Text(if (isEn) city.nameEn else city.nameAr) },
                                    onClick = { selectedCityId = city.id; expanded = false }
                                )
                            }
                        }
                    }

                    // Property Type Selector
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        val pTypes = mapOf(
                            "" to (if (isEn) "All Properties" else "جميع أنواع العقارات"),
                            "apartment" to (if (isEn) "Apartment" else "شقة سكينة"),
                            "house" to (if (isEn) "House / Villa" else "بيت مستقل/فيلا"),
                            "land" to (if (isEn) "Land" else "أرض عقارية"),
                            "shop" to (if (isEn) "Shop / Store" else "محل تجاري/دكان")
                        )
                        val buttonLabel = pTypes[filterPropType] ?: (if (isEn) "Property Type" else "نوع العقار")
                        Button(
                            onClick = { expanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(buttonLabel, fontSize = 10.sp, color = Color.White)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            pTypes.forEach { (key, value) ->
                                DropdownMenuItem(
                                    text = { Text(value) },
                                    onClick = { filterPropType = key; expanded = false }
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEn) "🏠 Available Properties (${activeProps.size}):" else "🏠 العقارات والمنشآت المتاحة (${activeProps.size}):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { viewModel.triggerRestoreAccountDialog.value = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(if (isEn) "Restore 🔓" else "استرجاع 🔓", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onAddPropertyClick,
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(if (isEn) "Add Property" else "أضف عقار", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (activeProps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isEn) "No properties match the current search filters." else "لم يتم العثور على أي عقار يطابق فلاتر البحث الحالية.", color = themeColors.textSecondary, fontSize = 12.sp)
            }
        } else {
            activeProps.take(itemsToShowLimit).forEach { prop ->
                PropertyListItemCard(prop = prop, themeColors = themeColors, onClick = { onPropertyClick(prop) })
            }
            if (activeProps.size > itemsToShowLimit) {
                Button(
                    onClick = { itemsToShowLimit += 10 },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(if (isEn) "Load More Properties ⏬" else "عرض المزيد من العقارات والمنشآت ⏬", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PropertyListItemCard(
    prop: PropertyEntity,
    themeColors: VisualThemePalette,
    onClick: () -> Unit,
    viewModel: MainViewModel? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                1.dp,
                if (prop.isPinned) themeColors.accent else themeColors.accent.copy(alpha = 0.08f),
                RoundedCornerShape(12.dp)
            )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color.DarkGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(themeColors.primary.copy(alpha = 0.1f))
                ) {
                    Text("📸 صور العقار", modifier = Modifier.align(Alignment.Center), color = Color.Gray, fontSize = 12.sp)
                }

                // Type Badge (Rent/Sale)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(if (prop.type == "rent") Color(0xFF0284C7) else Color(0xFF16A34A), RoundedCornerShape(bottomStart = 8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (prop.type == "rent") "للإيجار" else "للبيع",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (prop.isPinned) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(themeColors.accent, RoundedCornerShape(bottomEnd = 8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("📌 متميز", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }

                if (!prop.isActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(Color.Red)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("قيد التحقق", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(prop.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    if (prop.isVip) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFD97706), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("🏆 VIP", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    if (prop.isVerified) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF3B82F6), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("✅ موثق", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    if (prop.isRecommended) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEC4899), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("💖 موصى به", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(prop.description, fontSize = 10.sp, color = themeColors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("سعر الطلب:", fontSize = 8.sp, color = themeColors.textSecondary)
                        Text("${prop.price} ${prop.currency}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("📍 الحي: ${prop.localNeighborhood}", fontSize = 9.sp, color = Color.White)
                        }
                    }
                }

                Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(top = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Button(
                        onClick = {
                            val u = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${prop.phone}"))
                            context.startActivity(u)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1f).height(30.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("📞 اتصال مباشر", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            if (viewModel != null) {
                                viewModel.startVoiceCall(prop.title, "صاحب العقار HD")
                            } else {
                                val u = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${prop.phone}"))
                                context.startActivity(u)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.weight(1f).height(30.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("🎙️ مكالمة صوتية", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------
// 4. STORE DETAILS DIALOG
// --------------------------------------------------------
@Composable
fun StoreDetailsDialog(
    store: StoreEntity,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit,
    onOrderProductClick: (ProductEntity) -> Unit
) {
    val products by viewModel.products.collectAsState()
    val ratings by viewModel.ratings.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val context = LocalContext.current

    val storeProducts = remember(products) { products.filter { it.storeId == store.id && !it.isDeleted && it.isAvailable } }
    val storeReviews = remember(ratings) { ratings.filter { it.targetId == store.id && it.targetType == "STORE" && it.isApproved } }

    var userRatingInput by remember { mutableStateOf(5f) }
    var userCommentInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.background),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(2.dp, themeColors.accent, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Image Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(themeColors.primary.copy(alpha = 0.15f))
                ) {
                    Text("📸 غلاف المتجر الرئيسي", modifier = Modifier.align(Alignment.Center), color = Color.Gray, fontSize = 12.sp)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.Red)
                    }
                }

                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val adminRole by viewModel.adminRole.collectAsState()
                    var showEditDialog by remember { mutableStateOf(false) }

                    if (adminRole != "GUEST") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D).copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, Color.Red),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🛠️ لوحة تحكم الإدارة الفورية لهذا العنصر:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { showEditDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("📝 تعديل البيانات", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.deleteStore(store.id)
                                            onDismiss()
                                            android.widget.Toast.makeText(context, "🗑️ تم حذف وحظر المتجر من النظام بنجاح!", android.widget.Toast.LENGTH_LONG).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("🗑️ حذف وحظر", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (showEditDialog) {
                        StoreCreateEditDialog(
                            store = store,
                            viewModel = viewModel,
                            themeColors = themeColors,
                            onDismiss = { showEditDialog = false }
                        )
                    }

                    // Logo + Name
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(themeColors.accent.copy(alpha = 0.2f))
                                .border(2.dp, themeColors.accent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏪", fontSize = 28.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(store.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("المالك: ${store.ownerName}", fontSize = 10.sp, color = themeColors.textSecondary)
                        }
                    }

                    Divider(color = themeColors.accent.copy(alpha = 0.15f))

                    Text("📝 نبذة و وصف المتجر:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text(store.description, fontSize = 11.sp, color = Color.White)

                    // Show image gallery if available
                    // Product & Services Attachments Section (VISITOR_VIEW)
                    val storeAtts = remember(store.productAttachmentsJson) {
                        com.example.data.ProductAttachment.parseList(store.productAttachmentsJson)
                    }
                    ProductAttachmentsSection(
                        attachments = storeAtts,
                        onAttachmentsChanged = {},
                        mode = "VISITOR_VIEW",
                        themeColors = themeColors
                    )

                    // Special Offers & Discounts Section (VISITOR_VIEW)
                    SpecialOffersSection(
                        offersJson = store.specialOffersJson,
                        onOffersChanged = {},
                        isEditable = false,
                        themeColors = themeColors
                    )

                    if (store.images.isNotEmpty()) {
                        Text("📸 معرض الصور والمنتجات (${store.images.size}):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().height(120.dp)
                        ) {
                            items(store.images.size) { index ->
                                val bMap = rememberBase64Bitmap(store.images[index])
                                if (bMap != null) {
                                    Image(
                                        bitmap = bMap,
                                        contentDescription = null,
                                        modifier = Modifier.width(160.dp).fillMaxHeight().clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    // Show PDF link if available and approved
                    if (store.pdfFileBase64.isNotEmpty() && store.pdfStatus == "APPROVED") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            modifier = Modifier.fillMaxWidth().clickable {
                                android.widget.Toast.makeText(context, "📄 تم تنزيل قائمة الخدمات والأسعار بنجاح لقرائتها خارج التطبيق!", android.widget.Toast.LENGTH_LONG).show()
                            }
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("📄", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("عرض قائمة أسعار السلع والخدمات (PDF)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                    Text("مستند رسمي معتمد وموثق من الإدارة للجمهور", fontSize = 9.sp, color = Color.LightGray)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📍 الموقع والحي السكني", fontSize = 9.sp, color = themeColors.textSecondary)
                                Text(store.localNeighborhood, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🕒 ساعات العمل اليومي", fontSize = 9.sp, color = themeColors.textSecondary)
                                Text(store.workingHours, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // Contact row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val u = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${store.phone}"))
                                context.startActivity(u)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Call, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اتصال مباشر", color = Color.White, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val u = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${store.phone.replace("+", "")}"))
                                context.startActivity(u)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💬 واتساب", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    Divider(color = themeColors.accent.copy(alpha = 0.15f))

                    // Products/Menu Section
                    Text("🛍️ السلع والمنتجات المتاحة (${storeProducts.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    if (storeProducts.isEmpty()) {
                        Text("لا يوجد منتجات معروضة حالياً لهذا المتجر.", color = themeColors.textSecondary, fontSize = 10.sp)
                    } else {
                        storeProducts.forEach { product ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth().border(1.dp, themeColors.accent.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.DarkGray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("📦", fontSize = 24.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(product.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(product.description, fontSize = 9.sp, color = themeColors.textSecondary, maxLines = 1)
                                        Text("${product.price} YER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                    }
                                    Button(
                                        onClick = { onOrderProductClick(product) },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Text("شراء فوراً", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = themeColors.accent.copy(alpha = 0.15f))

                    // Reviews List
                    Text("⭐ التقييمات وآراء العملاء (${storeReviews.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    storeReviews.forEach { rev ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(rev.userName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Row {
                                        repeat(rev.rating.toInt()) { Text("⭐", fontSize = 8.sp) }
                                    }
                                }
                                Text(rev.comment, fontSize = 10.sp, color = themeColors.textSecondary)
                            }
                        }
                    }

                    // Add Review Form
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("➕ أضف تقييمك ورأيك في المتجر:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("النجوم: ", fontSize = 10.sp, color = themeColors.textSecondary)
                                (1..5).forEach { i ->
                                    val isSelected = userRatingInput >= i
                                    Text(
                                        text = if (isSelected) "★" else "☆",
                                        fontSize = 18.sp,
                                        color = if (isSelected) themeColors.accent else Color.Gray,
                                        modifier = Modifier.clickable { userRatingInput = i.toFloat() }.padding(horizontal = 2.dp)
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = userCommentInput,
                                onValueChange = { userCommentInput = it },
                                placeholder = { Text("اكتب تعليقك هنا...", fontSize = 11.sp, color = themeColors.textSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Button(
                                onClick = {
                                    if (userCommentInput.isNotEmpty()) {
                                        viewModel.addRating(
                                            RatingEntity(
                                                targetId = store.id,
                                                targetType = "STORE",
                                                userId = currentUserId,
                                                userName = currentUserName.ifEmpty { "عميل التطبيق" },
                                                rating = userRatingInput,
                                                comment = userCommentInput
                                            )
                                        )
                                        userCommentInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("إرسال التقييم", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------
// 5. PROPERTY DETAILS DIALOG
// --------------------------------------------------------
@Composable
fun PropertyDetailsDialog(
    property: PropertyEntity,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    val ratings by viewModel.ratings.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val context = LocalContext.current

    val propReviews = remember(ratings) { ratings.filter { it.targetId == property.id && it.targetType == "PROPERTY" && it.isApproved } }

    var userRatingInput by remember { mutableStateOf(5f) }
    var userCommentInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.background),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(2.dp, themeColors.accent, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Photo Gallery/Cover
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(themeColors.primary.copy(alpha = 0.15f))
                ) {
                    Text("📸 معرض صور العقار التفصيلية", modifier = Modifier.align(Alignment.Center), color = Color.Gray, fontSize = 12.sp)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.Red)
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(if (property.type == "rent") Color(0xFF0284C7) else Color(0xFF16A34A), RoundedCornerShape(bottomEnd = 8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (property.type == "rent") "للإيجار" else "للبيع",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val adminRole by viewModel.adminRole.collectAsState()
                    var showEditDialog by remember { mutableStateOf(false) }

                    if (adminRole != "GUEST") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D).copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, Color.Red),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🛠️ لوحة تحكم الإدارة الفورية لهذا العقار:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { showEditDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("📝 تعديل البيانات", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.deleteProperty(property.id)
                                            onDismiss()
                                            android.widget.Toast.makeText(context, "🗑️ تم حذف وحظر العقار من النظام بنجاح!", android.widget.Toast.LENGTH_LONG).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("🗑️ حذف وحظر", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (showEditDialog) {
                        PropertyCreateEditDialog(
                            property = property,
                            viewModel = viewModel,
                            themeColors = themeColors,
                            onDismiss = { showEditDialog = false }
                        )
                    }

                    Text(property.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("السعر المطلوب المالي:", fontSize = 9.sp, color = themeColors.textSecondary)
                            Text("${property.price} ${property.currency}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⭐", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(String.format("%.1f", property.rating), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                        }
                    }

                    Divider(color = themeColors.accent.copy(alpha = 0.15f))

                    Text("📝 تفاصيل و مواصفات العقار:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text(property.description, fontSize = 11.sp, color = Color.White)

                    // Show image gallery if available
                    if (property.images.isNotEmpty()) {
                        Text("📸 معرض صور العقار التفصيلية (${property.images.size}):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().height(120.dp)
                        ) {
                            items(property.images.size) { index ->
                                val bMap = rememberBase64Bitmap(property.images[index])
                                if (bMap != null) {
                                    Image(
                                        bitmap = bMap,
                                        contentDescription = null,
                                        modifier = Modifier.width(160.dp).fillMaxHeight().clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    // Show PDF link if available and approved
                    if (property.pdfFileBase64.isNotEmpty() && property.pdfStatus == "APPROVED") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            modifier = Modifier.fillMaxWidth().clickable {
                                android.widget.Toast.makeText(context, "📄 تم تحميل مستندات وخرائط وثيقة الملكية المعتمدة للعقار بنجاح!", android.widget.Toast.LENGTH_LONG).show()
                            }
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("📄", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("عرض وثائق وتفاصيل المخططات الهندسية والملكية (PDF)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                    Text("مستند رسمي معتمد وموثق ومفحوص من الإدارة", fontSize = 9.sp, color = Color.LightGray)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("نوع العقار", fontSize = 8.sp, color = themeColors.textSecondary)
                                Text(property.propertyType, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📍 الحي والمربع السكني", fontSize = 8.sp, color = themeColors.textSecondary)
                                Text(property.localNeighborhood, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // Contacts panel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val u = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${property.phone}"))
                                context.startActivity(u)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Call, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اتصال بالمالك", color = Color.White, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val u = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${property.phone.replace("+", "")}"))
                                context.startActivity(u)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💬 واتساب مالي", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    Divider(color = themeColors.accent.copy(alpha = 0.15f))

                    // Reviews/Comments list
                    Text("💬 الاستفسارات والتعليقات (${propReviews.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    propReviews.forEach { rev ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(rev.userName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Row {
                                        repeat(rev.rating.toInt()) { Text("★", fontSize = 8.sp, color = themeColors.accent) }
                                    }
                                }
                                Text(rev.comment, fontSize = 10.sp, color = themeColors.textSecondary)
                            }
                        }
                    }

                    // Add comment form
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("➕ أضف استفسارك أو تقييمك للعقار:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            OutlinedTextField(
                                value = userCommentInput,
                                onValueChange = { userCommentInput = it },
                                placeholder = { Text("اكتب سؤالك أو تعليقك...", fontSize = 11.sp, color = themeColors.textSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Button(
                                onClick = {
                                    if (userCommentInput.isNotEmpty()) {
                                        viewModel.addRating(
                                            RatingEntity(
                                                targetId = property.id,
                                                targetType = "PROPERTY",
                                                userId = currentUserId,
                                                userName = currentUserName.ifEmpty { "عميل عقارات" },
                                                rating = userRatingInput,
                                                comment = userCommentInput
                                            )
                                        )
                                        userCommentInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("إرسال", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------
// 6. STORE CREATE/EDIT DIALOG
// --------------------------------------------------------
@Composable
fun StoreCreateEditDialog(
    store: StoreEntity?, // Null if creating
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit,
    sectionId: String = "stores"
) {
    val cities by viewModel.cities.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val context = LocalContext.current

    var name by remember { mutableStateOf(store?.name ?: "") }
    var description by remember { mutableStateOf(store?.description ?: "") }
    var phone by remember { mutableStateOf(store?.phone ?: currentUserPhone) }
    var neighborhood by remember { mutableStateOf(store?.localNeighborhood ?: "") }
    var selectedCityId by remember { mutableStateOf(store?.cityId ?: cities.firstOrNull()?.id ?: "") }
    val availableCategories = remember(categories, sectionId) {
        val filtered = categories.filter { it.parentId == sectionId && !it.isMainCategory }
        if (filtered.isNotEmpty()) filtered
        else {
            when (sectionId) {
                "restaurants" -> listOf(
                    CategoryEntity("sub_rest_1", "مطاعم يمنية وشرقية", "🍲", 1, parentId = "restaurants"),
                    CategoryEntity("sub_rest_2", "وجبات سريعة وبرجر", "🍔", 2, parentId = "restaurants"),
                    CategoryEntity("sub_rest_3", "كافيهات ومشروبات", "☕", 3, parentId = "restaurants"),
                    CategoryEntity("sub_rest_4", "حلويات ومخابز", "🍰", 4, parentId = "restaurants"),
                    CategoryEntity("sub_rest_5", "مشويات وأسماك", "🥩", 5, parentId = "restaurants")
                )
                "medical", "2" -> listOf(
                    CategoryEntity("sub_med_1", "عيادات وأطباء", "🩺", 1, parentId = "medical"),
                    CategoryEntity("sub_med_2", "صيدليات ومستلزمات", "💊", 2, parentId = "medical"),
                    CategoryEntity("sub_med_3", "مختبرات تحاليل", "🔬", 3, parentId = "medical"),
                    CategoryEntity("sub_med_4", "مراكز علاج طبيعي", "🧘", 4, parentId = "medical"),
                    CategoryEntity("sub_med_5", "مستشفيات ومراكز تخصصية", "🏥", 5, parentId = "medical")
                )
                "centers" -> listOf(
                    CategoryEntity("sub_center_1", "مراكز تجميل وصالونات", "✂️", 1, parentId = "centers"),
                    CategoryEntity("sub_center_2", "مراكز طبية وتخصصية", "🏥", 2, parentId = "centers"),
                    CategoryEntity("sub_center_3", "مراكز تعليم وتدريب", "🎓", 3, parentId = "centers"),
                    CategoryEntity("sub_center_4", "أندية وصالات رياضية", "🏋️", 4, parentId = "centers")
                )
                "realestate" -> listOf(
                    CategoryEntity("sub_prop_1", "شقق للإيجار والبيع", "🏢", 1, parentId = "realestate"),
                    CategoryEntity("sub_prop_2", "فلل وقصور", "🏰", 2, parentId = "realestate"),
                    CategoryEntity("sub_prop_3", "أراضي ومخططات", "🏞️", 3, parentId = "realestate"),
                    CategoryEntity("sub_prop_4", "مكاتب ومحلات تجارية", "🏪", 4, parentId = "realestate"),
                    CategoryEntity("sub_prop_5", "شاليهات واستراحات", "🏊", 5, parentId = "realestate")
                )
                "jobs" -> listOf(
                    CategoryEntity("sub_job_1", "وظائف هندسية وتقنية", "💻", 1, parentId = "jobs"),
                    CategoryEntity("sub_job_2", "وظائف طبية وصحية", "🩺", 2, parentId = "jobs"),
                    CategoryEntity("sub_job_3", "مبيعات وتسويق", "📈", 3, parentId = "jobs"),
                    CategoryEntity("sub_job_4", "محاسبة وإدارة", "📑", 4, parentId = "jobs"),
                    CategoryEntity("sub_job_5", "حرف وخدمات مهنية", "🔧", 5, parentId = "jobs")
                )
                else -> listOf( // "stores"
                    CategoryEntity("sub_store_1", "ملابس وأزياء", "👔", 1, parentId = "stores"),
                    CategoryEntity("sub_store_2", "إلكترونيات وهواتف", "📱", 2, parentId = "stores"),
                    CategoryEntity("sub_store_3", "أجهزة منزلية وكهربائية", "📺", 3, parentId = "stores"),
                    CategoryEntity("sub_store_4", "سوبرماركت ومواد غذائية", "🛒", 4, parentId = "stores"),
                    CategoryEntity("sub_store_5", "عطور ومستحضرات تجميل", "💄", 5, parentId = "stores")
                )
            }
        }
    }

    var selectedCatId by remember { mutableStateOf(store?.categoryId ?: availableCategories.firstOrNull()?.id ?: "") }
    var workingHours by remember { mutableStateOf(store?.workingHours ?: "9:00 AM - 10:00 PM") }
    
    // Passwords
    var password by remember { mutableStateOf(store?.password ?: "") }
    var confirmPassword by remember { mutableStateOf(store?.password ?: "") }

    // Admin limit
    var customMaxPhotosInput by remember { mutableStateOf(if (store != null) store.maxImages.toString() else "5") }

    // PDF files
    var pdfUriText by remember { mutableStateOf(store?.pdfFileUri ?: "") }
    var pdfBase64Text by remember { mutableStateOf(store?.pdfFileBase64 ?: "") }
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pdfUriText = uri.toString()
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                if (bytes != null) {
                    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                    pdfBase64Text = base64
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Images
    var storePhotosList by remember { mutableStateOf<List<String>>(store?.images ?: emptyList()) }
    
    val currentMaxPhotos = if (store != null && store.maxImages > 0) store.maxImages else settingsState.maxStorePhotos
    val isUnlimited = currentMaxPhotos >= 999

    val storePhotosUriPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val converted = uris.map { uri ->
            val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
            "data:image/jpeg;base64,$base64"
        }
        val combined = if (isUnlimited) {
            storePhotosList + converted
        } else {
            (storePhotosList + converted).take(currentMaxPhotos)
        }
        storePhotosList = combined
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.background),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(2.dp, themeColors.accent, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (store == null) "🏪 تسجيل وإضافة متجر جديد" else "🏪 تعديل بيانات المتجر",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color.Red)
                    }
                }

                // Show Admin Terms
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = settingsState.storesRegistrationTerms,
                        fontSize = 10.sp,
                        color = themeColors.accent,
                        modifier = Modifier.padding(8.dp),
                        lineHeight = 14.sp
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المتجر التجاري", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("store_name_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف السلع والخدمات المعروضة بالتفصيل", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم هاتف المالك للتواصل (مثال: 777123456)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = neighborhood,
                    onValueChange = { neighborhood = it },
                    label = { Text("الحي السكني أو الشارع الرئيسي", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = workingHours,
                    onValueChange = { workingHours = it },
                    label = { Text("ساعات العمل اليومية (مثال: 8:00 ص - 10:00 م)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Passwords with confirmation
                Text("🔑 كلمة المرور لحساب المالك والمشرف:", fontSize = 11.sp, color = themeColors.textSecondary)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة المرور الخاصة بك", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("تأكيد كلمة المرور الخاصة بك", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Forgot Password link
                if (store != null || phone.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            if (password.isNotEmpty()) {
                                viewModel.requestPasswordRecoveryForStore(name.ifEmpty { "المتجر" }, phone, password)
                            } else {
                                android.widget.Toast.makeText(context, "الرجاء إدخال كلمة المرور أولاً ليتم حفظها وطلب استعادتها", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text("🔑 نسيت كلمة المرور؟ أرسل طلب للأدمن لاسترجاعها فوراً", fontSize = 9.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }

                // Dropdowns
                Text("المحافظة اليمنية:", fontSize = 11.sp, color = themeColors.textSecondary)
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    val currentCityName = cities.find { it.id == selectedCityId }?.nameAr ?: "اختر المحافظة"
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(currentCityName, color = Color.White)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        cities.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city.nameAr) },
                                onClick = { selectedCityId = city.id; expanded = false }
                            )
                        }
                    }
                }

                Text("فئة الأنشطة التجارية:", fontSize = 11.sp, color = themeColors.textSecondary)
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    val currentCatName = availableCategories.find { it.id == selectedCatId }?.name ?: "اختر الفئة"
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(currentCatName, color = Color.White)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        availableCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.icon} ${cat.name}") },
                                onClick = { selectedCatId = cat.id; expanded = false }
                            )
                        }
                    }
                }

                // Multiple Images Upload
                if (settingsState.showStoresPhotosOption) {
                    val limitText = if (isUnlimited) "عدد غير محدود" else "$currentMaxPhotos"
                    Text("📸 معرض صور المتجر والمنتجات (${storePhotosList.size}/$limitText):", fontSize = 11.sp, color = themeColors.textSecondary)
                    if (storePhotosList.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().height(75.dp)
                        ) {
                            items(storePhotosList.size) { index ->
                                val photo = storePhotosList[index]
                                val bMap = rememberBase64Bitmap(photo)
                                Box(modifier = Modifier.size(70.dp)) {
                                    if (bMap != null) {
                                        Image(
                                            bitmap = bMap,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize().background(Color.Gray), contentAlignment = Alignment.Center) {
                                            Text("📸", fontSize = 20.sp)
                                        }
                                    }
                                    IconButton(
                                        onClick = { storePhotosList = storePhotosList.filterIndexed { idx, _ -> idx != index } },
                                        modifier = Modifier.align(Alignment.TopEnd).size(18.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                        }
                    }
                    if (isUnlimited || storePhotosList.size < currentMaxPhotos) {
                        Button(
                            onClick = { storePhotosUriPicker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📷 إضافة صور إضافية للمعرض", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }

                if (adminRole != "GUEST") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("⚙️ لوحة تحكم الإدارة (خاص بالأدمن):", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = customMaxPhotosInput,
                        onValueChange = { customMaxPhotosInput = it },
                        label = { Text("الحد الأقصى للصور (اكتب 999 لصور غير محدودة)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // PDF Upload Section
                Text("📄 ملفات PDF المرفقة (قوائم الأسعار، عروض السلع والمطعم):", fontSize = 11.sp, color = themeColors.textSecondary)
                Button(
                    onClick = { pdfPickerLauncher.launch("application/pdf") },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (pdfUriText.isEmpty()) "📄 إرفاق ملف PDF للسلع والخدمات من الهاتف" else "✅ تم إرفاق ملف الـ PDF بنجاح",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (name.trim().isEmpty()) {
                            android.widget.Toast.makeText(context, "❌ يرجى إدخال اسم المتجر/المحل التجاري أولاً!", android.widget.Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (phone.trim().isEmpty()) {
                            android.widget.Toast.makeText(context, "❌ يرجى إدخال رقم هاتف للتواصل!", android.widget.Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
                        val isValidYemeniPhone = cleanPhone.length == 9 && (
                            cleanPhone.startsWith("77") || 
                            cleanPhone.startsWith("73") || 
                            cleanPhone.startsWith("71") || 
                            cleanPhone.startsWith("70") || 
                            cleanPhone.startsWith("78")
                        )
                        if (!isValidYemeniPhone) {
                            android.widget.Toast.makeText(context, "❌ رقم الهاتف المدخل غير صحيح! يجب أن يبدأ بـ (77، 73، 71، 70، 78) ويتكون من 9 أرقام.", android.widget.Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (adminRole == "GUEST" && neighborhood.trim().isEmpty()) {
                            android.widget.Toast.makeText(context, "❌ يرجى تحديد الحي أو الحارة السكنية لتسهيل الوصول!", android.widget.Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (password.trim().isEmpty()) {
                            android.widget.Toast.makeText(context, "❌ يرجى تعيين كلمة مرور لحماية متجرك وإمكانية تعديله مستقبلاً!", android.widget.Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (password != confirmPassword) {
                            android.widget.Toast.makeText(context, "❌ كلمتا المرور غير متطابقتين!", android.widget.Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (true) {
                            val newStore = StoreEntity(
                                id = store?.id ?: "",
                                sectionId = store?.sectionId ?: sectionId,
                                name = name,
                                description = description,
                                ownerId = currentUserId,
                                ownerName = currentUserName.ifEmpty { "تاجر يمني" },
                                phone = phone,
                                categoryId = selectedCatId,
                                cityId = selectedCityId,
                                localNeighborhood = neighborhood.ifEmpty { "إدارة التطبيق" },
                                workingHours = workingHours,
                                isActive = if (adminRole != "GUEST") true else (store?.isActive ?: false), // Auto-approve if added by admin
                                isApproved = if (adminRole != "GUEST") true else (store?.isApproved ?: false),
                                password = password,
                                maxImages = customMaxPhotosInput.toIntOrNull() ?: (store?.maxImages ?: 5),
                                pdfFileUri = pdfUriText,
                                pdfFileBase64 = pdfBase64Text,
                                pdfStatus = if (pdfBase64Text.isNotEmpty()) (store?.pdfStatus ?: "PENDING") else "",
                                images = storePhotosList
                            )
                            viewModel.saveStore(newStore)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إرسال الطلب للمراجعة والتحقق", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --------------------------------------------------------
// 7. PROPERTY CREATE/EDIT DIALOG
// --------------------------------------------------------
@Composable
fun PropertyCreateEditDialog(
    property: PropertyEntity?,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit,
    sectionId: String = "properties"
) {
    val cities by viewModel.cities.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val context = LocalContext.current

    var title by remember { mutableStateOf(property?.title ?: "") }
    var description by remember { mutableStateOf(property?.description ?: "") }
    var priceInput by remember { mutableStateOf(property?.price?.toString() ?: "0") }
    var phone by remember { mutableStateOf(property?.phone ?: currentUserPhone) }
    var neighborhood by remember { mutableStateOf(property?.localNeighborhood ?: "") }
    var type by remember { mutableStateOf(property?.type ?: "rent") } // rent, sale
    var propertyType by remember { mutableStateOf(property?.propertyType ?: "apartment") } // apartment, house, land, shop
    var selectedCityId by remember { mutableStateOf(property?.cityId ?: cities.firstOrNull()?.id ?: "") }
    
    // Passwords
    var password by remember { mutableStateOf(property?.password ?: "") }
    var confirmPassword by remember { mutableStateOf(property?.password ?: "") }

    // Admin limit
    var customMaxPhotosInput by remember { mutableStateOf(if (property != null) property.maxImages.toString() else "5") }

    // PDF files
    var pdfUriText by remember { mutableStateOf(property?.pdfFileUri ?: "") }
    var pdfBase64Text by remember { mutableStateOf(property?.pdfFileBase64 ?: "") }
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pdfUriText = uri.toString()
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                if (bytes != null) {
                    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                    pdfBase64Text = base64
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Images
    var propPhotosList by remember { mutableStateOf<List<String>>(property?.images ?: emptyList()) }

    val currentMaxPhotos = if (property != null && property.maxImages > 0) property.maxImages else settingsState.maxPropertyPhotos
    val isUnlimited = currentMaxPhotos >= 999

    val propPhotosUriPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val converted = uris.map { uri ->
            val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
            "data:image/jpeg;base64,$base64"
        }
        val combined = if (isUnlimited) {
            propPhotosList + converted
        } else {
            (propPhotosList + converted).take(currentMaxPhotos)
        }
        propPhotosList = combined
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.background),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(2.dp, themeColors.accent, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (property == null) "🏠 إضافة عقار جديد" else "🏠 تعديل بيانات العقار",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color.Red)
                    }
                }

                // Show Admin Terms
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = settingsState.propertiesRegistrationTerms,
                        fontSize = 10.sp,
                        color = themeColors.accent,
                        modifier = Modifier.padding(8.dp),
                        lineHeight = 14.sp
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الإعلان (مثال: شقة متميزة للإيجار في حدة)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("prop_title_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("المواصفات الكاملة (عدد الغرف، المرافق، الخدمات)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("القيمة السعرية", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text("نوع المعاملة:", fontSize = 9.sp, color = themeColors.textSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(Pair("rent", "إيجار"), Pair("sale", "بيع")).forEach { (key, name) ->
                                val isSel = type == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSel) themeColors.accent else themeColors.surface, RoundedCornerShape(6.dp))
                                        .clickable { type = key }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(name, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم هاتف المالك / المعلن", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = neighborhood,
                    onValueChange = { neighborhood = it },
                    label = { Text("المنطقة / المربع السكني", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Passwords with confirmation
                Text("🔑 كلمة المرور لحساب المالك والمشرف:", fontSize = 11.sp, color = themeColors.textSecondary)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة المرور الخاصة بك", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("تأكيد كلمة المرور الخاصة بك", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Forgot Password link
                if (property != null || phone.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            if (password.isNotEmpty()) {
                                viewModel.requestPasswordRecoveryForProperty(title.ifEmpty { "العقار" }, phone, password)
                            } else {
                                android.widget.Toast.makeText(context, "الرجاء إدخال كلمة المرور أولاً ليتم حفظها وطلب استعادتها", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text("🔑 نسيت كلمة المرور؟ أرسل طلب للأدمن لاسترجاعها فوراً", fontSize = 9.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }

                Text("نوع المنشأة العقارية:", fontSize = 11.sp, color = themeColors.textSecondary)
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    val pTypes = mapOf(
                        "apartment" to "شقة سكينة",
                        "house" to "بيت مستقل/فيلا",
                        "land" to "أرض عقارية",
                        "shop" to "محل تجاري/دكان"
                    )
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(pTypes[propertyType] ?: "شقة سكينة", color = Color.White)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        pTypes.forEach { (key, value) ->
                            DropdownMenuItem(
                                text = { Text(value) },
                                onClick = { propertyType = key; expanded = false }
                            )
                        }
                    }
                }

                Text("المحافظة اليمنية:", fontSize = 11.sp, color = themeColors.textSecondary)
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    val currentCityName = cities.find { it.id == selectedCityId }?.nameAr ?: "اختر المحافظة"
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(currentCityName, color = Color.White)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        cities.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city.nameAr) },
                                onClick = { selectedCityId = city.id; expanded = false }
                            )
                        }
                    }
                }

                // Multiple Images Upload
                if (settingsState.showPropertiesPhotosOption) {
                    val limitText = if (isUnlimited) "عدد غير محدود" else "$currentMaxPhotos"
                    Text("📸 معرض صور العقار الملحق (${propPhotosList.size}/$limitText):", fontSize = 11.sp, color = themeColors.textSecondary)
                    if (propPhotosList.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().height(75.dp)
                        ) {
                            items(propPhotosList.size) { index ->
                                val photo = propPhotosList[index]
                                val bMap = rememberBase64Bitmap(photo)
                                Box(modifier = Modifier.size(70.dp)) {
                                    if (bMap != null) {
                                        Image(
                                            bitmap = bMap,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize().background(Color.Gray), contentAlignment = Alignment.Center) {
                                            Text("📸", fontSize = 20.sp)
                                        }
                                    }
                                    IconButton(
                                        onClick = { propPhotosList = propPhotosList.filterIndexed { idx, _ -> idx != index } },
                                        modifier = Modifier.align(Alignment.TopEnd).size(18.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                        }
                    }
                    if (isUnlimited || propPhotosList.size < currentMaxPhotos) {
                        Button(
                            onClick = { propPhotosUriPicker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📷 إضافة صور عقارية إضافية للمجموعة", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }

                if (adminRole != "GUEST") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("⚙️ لوحة تحكم الإدارة (خاص بالأدمن):", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = customMaxPhotosInput,
                        onValueChange = { customMaxPhotosInput = it },
                        label = { Text("الحد الأقصى للصور (اكتب 999 لصور غير محدودة)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // PDF Upload Section
                Text("📄 الوثائق والتفاصيل الإضافية المرفقة بصيغة PDF:", fontSize = 11.sp, color = themeColors.textSecondary)
                Button(
                    onClick = { pdfPickerLauncher.launch("application/pdf") },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (pdfUriText.isEmpty()) "📄 إرفاق وثائق العقار PDF من الهاتف" else "✅ تم إرفاق ملف الـ PDF العقاري بنجاح",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (title.trim().isEmpty()) {
                            android.widget.Toast.makeText(context, "❌ يرجى إدخال عنوان الإعلان الرئيسي للعقار!", android.widget.Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (phone.trim().isEmpty()) {
                            android.widget.Toast.makeText(context, "❌ يرجى إدخال رقم الهاتف للتواصل بخصوص العقار!", android.widget.Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
                        val isValidYemeniPhone = cleanPhone.length == 9 && (
                            cleanPhone.startsWith("77") || 
                            cleanPhone.startsWith("73") || 
                            cleanPhone.startsWith("71") || 
                            cleanPhone.startsWith("70") || 
                            cleanPhone.startsWith("78")
                        )
                        if (!isValidYemeniPhone) {
                            android.widget.Toast.makeText(context, "❌ رقم الهاتف المدخل غير صحيح! يجب أن يبدأ بـ (77، 73، 71، 70، 78) ويتكون من 9 أرقام.", android.widget.Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (adminRole == "GUEST" && neighborhood.trim().isEmpty()) {
                            android.widget.Toast.makeText(context, "❌ يرجى تحديد موقع الحي أو الشارع السكني للعقار!", android.widget.Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (password.trim().isEmpty()) {
                            android.widget.Toast.makeText(context, "❌ يرجى إدخال كلمة مرور لحماية الإعلان العقاري وتعديله مستقبلاً!", android.widget.Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (password != confirmPassword) {
                            android.widget.Toast.makeText(context, "❌ كلمتا المرور غير متطابقتين!", android.widget.Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (true) {
                            val newProp = PropertyEntity(
                                id = property?.id ?: "",
                                sectionId = property?.sectionId ?: sectionId,
                                title = title,
                                description = description,
                                price = priceInput.toDoubleOrNull() ?: 0.0,
                                phone = phone,
                                ownerId = currentUserId,
                                ownerName = currentUserName.ifEmpty { "معلن عقاري" },
                                cityId = selectedCityId,
                                localNeighborhood = neighborhood.ifEmpty { "إدارة التطبيق" },
                                type = type,
                                propertyType = propertyType,
                                isActive = if (adminRole != "GUEST") true else (property?.isActive ?: false), // Auto-approve if added by admin
                                isApproved = if (adminRole != "GUEST") true else (property?.isApproved ?: false),
                                password = password,
                                maxImages = customMaxPhotosInput.toIntOrNull() ?: (property?.maxImages ?: 5),
                                pdfFileUri = pdfUriText,
                                pdfFileBase64 = pdfBase64Text,
                                pdfStatus = if (pdfBase64Text.isNotEmpty()) (property?.pdfStatus ?: "PENDING") else "",
                                images = propPhotosList
                            )
                            viewModel.saveProperty(newProp)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إرسال طلب الإضافة للتحقق والتنشيط", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --------------------------------------------------------
// 8. STORE PRODUCT ELECTRONIC ORDER & PAYMENT DIALOG (Yemeni Methods Only)
// --------------------------------------------------------
@Composable
fun StoreProductOrderDialog(
    product: ProductEntity,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val paymentWallets by viewModel.paymentWallets.collectAsState()

    var customerName by remember { mutableStateOf(currentUserName) }
    var customerPhone by remember { mutableStateOf(currentUserPhone) }
    var customerArea by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf(1) }
    var notes by remember { mutableStateOf("") }

    val activeWallets = remember(paymentWallets) { paymentWallets.filter { it.status == "active" } }
    var selectedWallet by remember { mutableStateOf<PaymentWalletEntity?>(null) }
    var transferIdInput by remember { mutableStateOf("") }
    var transferPhotoInput by remember { mutableStateOf("") }

    val totalAmount = product.price * quantity

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.background),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(2.dp, themeColors.accent, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🛒 طلب شراء السلعة ودفعها إلكترونياً", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color.Red)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("📦", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(product.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("السعر الفردي: ${product.price} YER", fontSize = 11.sp, color = themeColors.accent)
                        }
                    }
                }

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("الاسم الكامل للمشتري", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("order_name_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("رقم هاتف المشتري للتواصل والتأكيد", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = customerArea,
                    onValueChange = { customerArea = it },
                    label = { Text("حي التوصيل أو المنطقة بالكامل بالتفصيل", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الكمية المطلوبة للطلب:", fontSize = 11.sp, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { if (quantity > 1) quantity-- },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(36.dp)
                        ) { Text("-", color = Color.White) }
                        Text("$quantity", modifier = Modifier.padding(horizontal = 14.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Button(
                            onClick = { quantity++ },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(36.dp)
                        ) { Text("+", color = Color.White) }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إجمالي المبلغ المستحق:", fontSize = 11.sp, color = Color.White)
                        Text("$totalAmount YER", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    }
                }

                // Payment Wallets Selection (Yemeni Methods)
                Text("🏦 اختر وسيلة الدفع الإلكترونية اليمنية المتاحة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                if (activeWallets.isEmpty()) {
                    Text("لا يوجد محافظ دفع مفعلة حالياً من قبل الأدمن.", color = Color.Red, fontSize = 10.sp)
                } else {
                    activeWallets.forEach { wallet ->
                        val isSelected = selectedWallet?.id == wallet.id
                        val walletIcon = when (wallet.provider) {
                            "jeep" -> "📱 محفظة جيب"
                            "jawali" -> "📲 محفظة جوالي"
                            "kuraimi" -> "🏦 الكريمي موني"
                            else -> "💳 تحويل مباشر"
                        }
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) themeColors.accent.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedWallet = wallet }
                                .border(
                                    1.dp,
                                    if (isSelected) themeColors.accent else Color.Gray.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(walletIcon, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("اسم الحساب المالي: ${wallet.accountName}", fontSize = 10.sp, color = themeColors.textSecondary)
                                Text("رقم الحساب / رقم المحفظة: ${wallet.walletNumber}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            }
                        }
                    }
                }

                if (selectedWallet != null) {
                    OutlinedTextField(
                        value = transferIdInput,
                        onValueChange = { transferIdInput = it },
                        label = { Text("رقم مرجع الحوالة / العملية المالية الموثق", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("transfer_id_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = transferPhotoInput,
                        onValueChange = { transferPhotoInput = it },
                        label = { Text("أدخل رابط إثبات التحويل أو السند المالي", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Button(
                    onClick = {
                        if (customerName.isNotEmpty() && customerPhone.isNotEmpty() && customerArea.isNotEmpty()) {
                            val newOrder = OrderEntity(
                                id = "",
                                storeId = product.storeId,
                                productId = product.id,
                                productName = product.name,
                                customerPhone = customerPhone,
                                customerName = customerName,
                                customerArea = customerArea,
                                price = product.price,
                                quantity = quantity,
                                totalAmount = totalAmount,
                                paymentId = transferIdInput,
                                paymentStatus = if (selectedWallet != null) "PROCESSING" else "PENDING",
                                status = "PENDING"
                            )
                            viewModel.placeOrder(newOrder)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تأكيد وحفظ طلب الشراء الفوري", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --------------------------------------------------------
// 9. ADMIN STORES & PROPERTIES MANAGEMENT PANEL
// --------------------------------------------------------
@Composable
fun AdminStoresPropertiesPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val ratings by viewModel.ratings.collectAsState()
    val settingsState by viewModel.settings.collectAsState()

    val activeStores = remember(stores) { stores.filter { !it.isDeleted } }
    val archivedStores = remember(stores) { stores.filter { it.isDeleted } }
    val activeProperties = remember(properties) { properties.filter { !it.isDeleted } }
    val archivedProperties = remember(properties) { properties.filter { it.isDeleted } }

    var storeToEdit by remember { mutableStateOf<StoreEntity?>(null) }
    var propertyToEdit by remember { mutableStateOf<PropertyEntity?>(null) }

    var isStoresEnabled by remember(settingsState.isStoresEnabled) { mutableStateOf(settingsState.isStoresEnabled) }
    var isPropertiesEnabled by remember(settingsState.isPropertiesEnabled) { mutableStateOf(settingsState.isPropertiesEnabled) }
    var dynamicTabs by remember(settingsState.dynamicTabsList) { mutableStateOf(settingsState.dynamicTabsList) }

    // Terms and Conditions inputs
    var storesTerms by remember(settingsState.storesRegistrationTerms) { mutableStateOf(settingsState.storesRegistrationTerms) }
    var propertiesTerms by remember(settingsState.propertiesRegistrationTerms) { mutableStateOf(settingsState.propertiesRegistrationTerms) }
    var maxStorePhotos by remember(settingsState.maxStorePhotos) { mutableStateOf(settingsState.maxStorePhotos.toString()) }
    var maxPropertyPhotos by remember(settingsState.maxPropertyPhotos) { mutableStateOf(settingsState.maxPropertyPhotos.toString()) }
    var showStoresPhotos by remember(settingsState.showStoresPhotosOption) { mutableStateOf(settingsState.showStoresPhotosOption) }
    var showPropertiesPhotos by remember(settingsState.showPropertiesPhotosOption) { mutableStateOf(settingsState.showPropertiesPhotosOption) }

    // Dynamic Sections state (parsed from database)
    var localSectionsList by remember(settingsState.dynamicSectionsData) {
        mutableStateOf(com.example.data.DynamicSection.parseDynamicSections(settingsState.dynamicSectionsData))
    }

    var confirmActionType by remember { mutableStateOf<String?>(null) }
    var confirmTargetId by remember { mutableStateOf("") }
    var confirmTargetName by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Creating section state
    var newSecId by remember { mutableStateOf("") }
    var newSecName by remember { mutableStateOf("") }
    var newSecIcon by remember { mutableStateOf("🏪") }
    var newSecType by remember { mutableStateOf("store") } // "store" or "property"
    var newSecTerms by remember { mutableStateOf("شروط تسجيل القسم الجديد: يرجى التحقق من صحة ومصداقية كافة البيانات.") }
    var newSecMaxPhotos by remember { mutableStateOf("5") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "⚙️ إعدادات التحكم بالأقسام والميزات الحية",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = themeColors.accent
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تفعيل تبويب وقسم المحلات التجارية بالكامل", fontSize = 11.sp, color = Color.White)
                    Switch(
                        checked = isStoresEnabled,
                        onCheckedChange = { isStoresEnabled = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تفعيل تبويب وقسم العقارات بالكامل", fontSize = 11.sp, color = Color.White)
                    Switch(
                        checked = isPropertiesEnabled,
                        onCheckedChange = { isPropertiesEnabled = it }
                    )
                }

                OutlinedTextField(
                    value = dynamicTabs,
                    onValueChange = { dynamicTabs = it },
                    label = { Text("قائمة التبويبات الفعالة (مفصولة بفواصل)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text("📝 شروط وسياسات تسجيل المتاجر:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                OutlinedTextField(
                    value = storesTerms,
                    onValueChange = { storesTerms = it },
                    placeholder = { Text("أدخل الشروط والتعليمات لتسجيل المحلات والمطاعم...", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Text("📝 شروط وسياسات إدراج العقارات:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                OutlinedTextField(
                    value = propertiesTerms,
                    onValueChange = { propertiesTerms = it },
                    placeholder = { Text("أدخل شروط ووثائق إدراج العقارات والأراضي الحاكمة للتفعيل...", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("خيار رفع صور متعددة للمحلات", fontSize = 11.sp, color = Color.White)
                    Switch(
                        checked = showStoresPhotos,
                        onCheckedChange = { showStoresPhotos = it }
                    )
                }

                OutlinedTextField(
                    value = maxStorePhotos,
                    onValueChange = { maxStorePhotos = it },
                    label = { Text("أقصى عدد صور مسموح به للمتجر (من 5 إلى 10)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("خيار رفع صور متعددة للعقارات", fontSize = 11.sp, color = Color.White)
                    Switch(
                        checked = showPropertiesPhotos,
                        onCheckedChange = { showPropertiesPhotos = it }
                    )
                }

                OutlinedTextField(
                    value = maxPropertyPhotos,
                    onValueChange = { maxPropertyPhotos = it },
                    label = { Text("أقصى عدد صور مسموح به للعقار (من 5 إلى 10)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        }

        // --- DYNAMIC SECTIONS CRUD PANEL ---
        Text(
            text = "🛠️ إدارة وتعديل الأقسام والتبويبات الحية",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = themeColors.accent
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("إضافة قسم ديناميكي مخصص جديد:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = newSecId,
                        onValueChange = { newSecId = it },
                        label = { Text("معرف القسم (ID)", fontSize = 9.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = newSecName,
                        onValueChange = { newSecName = it },
                        label = { Text("اسم القسم (التبويب)", fontSize = 9.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = newSecIcon,
                        onValueChange = { newSecIcon = it },
                        label = { Text("أيقونة/إيموجي", fontSize = 9.sp) },
                        modifier = Modifier.weight(0.6f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("نوع طبيعة القسم:", fontSize = 10.sp, color = Color.White)
                    listOf(Pair("store", "محل تجاري 🏪"), Pair("property", "إعلان عقاري 🏠")).forEach { (key, name) ->
                        val isSel = newSecType == key
                        Button(
                            onClick = { newSecType = key },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSel) themeColors.accent else Color.DarkGray),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text(name, fontSize = 9.sp, color = if (isSel) Color.Black else Color.White)
                        }
                    }
                }

                Button(
                    onClick = {
                        if (newSecId.isNotEmpty() && newSecName.isNotEmpty()) {
                            val newSection = com.example.data.DynamicSection(
                                id = newSecId.trim().lowercase(),
                                name = newSecName.trim(),
                                icon = newSecIcon.trim(),
                                type = newSecType,
                                isEnabled = true,
                                order = localSectionsList.size + 1,
                                terms = newSecTerms,
                                maxPhotos = newSecMaxPhotos.toIntOrNull() ?: 5
                            )
                            localSectionsList = localSectionsList + newSection
                            newSecId = ""
                            newSecName = ""
                            newSecIcon = "🏪"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إضافة هذا القسم الجديد للمسودة ➕", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

                Text("أقسام المسودة الحالية (${localSectionsList.size}):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                localSectionsList.forEachIndexed { index, section ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(section.icon, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${section.name} (${if (section.type == "store") "محل" else "عقار"})", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Order controls
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val list = localSectionsList.toMutableList()
                                                val temp = list[index]
                                                list[index] = list[index - 1]
                                                list[index - 1] = temp
                                                localSectionsList = list
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("🔼", fontSize = 10.sp)
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < localSectionsList.size - 1) {
                                                val list = localSectionsList.toMutableList()
                                                val temp = list[index]
                                                list[index] = list[index + 1]
                                                list[index + 1] = temp
                                                localSectionsList = list
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("🔽", fontSize = 10.sp)
                                    }
                                    Button(
                                        onClick = {
                                            localSectionsList = localSectionsList.filter { it.id != section.id }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.height(20.dp).widthIn(max = 50.dp)
                                    ) {
                                        Text("حذف", fontSize = 8.sp, color = Color.White)
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("تفعيل هذا التبويب للمواطنين", fontSize = 9.sp, color = Color.White)
                                Switch(
                                    checked = section.isEnabled,
                                    onCheckedChange = { checked ->
                                        localSectionsList = localSectionsList.map {
                                            if (it.id == section.id) it.copy(isEnabled = checked) else it
                                        }
                                    }
                                )
                            }

                            OutlinedTextField(
                                value = section.name,
                                onValueChange = { newVal ->
                                    localSectionsList = localSectionsList.map {
                                        if (it.id == section.id) it.copy(name = newVal) else it
                                    }
                                },
                                label = { Text("تعديل اسم القسم", fontSize = 9.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }
                    }
                }
            }
        }

        // --- ATOMIC MANUAL SAVE BUTTON ---
        Button(
            onClick = {
                val updatedSettings = settingsState.copy(
                    isStoresEnabled = isStoresEnabled,
                    isPropertiesEnabled = isPropertiesEnabled,
                    dynamicTabsList = dynamicTabs,
                    storesRegistrationTerms = storesTerms,
                    propertiesRegistrationTerms = propertiesTerms,
                    maxStorePhotos = maxStorePhotos.toIntOrNull() ?: 5,
                    maxPropertyPhotos = maxPropertyPhotos.toIntOrNull() ?: 5,
                    showStoresPhotosOption = showStoresPhotos,
                    showPropertiesPhotosOption = showPropertiesPhotos,
                    dynamicSectionsData = com.example.data.DynamicSection.serializeDynamicSections(localSectionsList)
                )
                viewModel.saveCustomSettingsState(updatedSettings)
                viewModel.triggerNotification("💾 تم حفظ وتطبيق كافة التعديلات والإعدادات بنجاح فورياً!")
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("💾 حفظ وتطبيق جميع الإعدادات فورياً", fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Bold)
        }

        // Stores Management List
        Text("🏢 مراجعة وإدارة المتاجر المسجلة (${activeStores.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        activeStores.forEach { store ->
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(store.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = if (store.isActive) "● نشط" else "○ قيد المراجعة",
                            fontSize = 10.sp,
                            color = if (store.isActive) Color.Green else Color.Yellow,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("الهاتف: ${store.phone} | الحي: ${store.localNeighborhood}", fontSize = 9.sp, color = themeColors.textSecondary)
                    
                    if (store.password.isNotEmpty()) {
                        Text("🔑 كلمة مرور المالك المشرف: ${store.password}", fontSize = 9.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                    }

                    // PDF Document verification block
                    if (store.pdfFileBase64.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📄 وثيقة ملف الـ PDF للمحل مرفقة:", fontSize = 10.sp, color = Color.White)
                                    Text(
                                        text = when (store.pdfStatus) {
                                            "APPROVED" -> "✅ معتمد وموافق عليه"
                                            "REJECTED" -> "❌ مرفوض ومستبعد"
                                            else -> "⏳ قيد التدقيق والمراجعة"
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (store.pdfStatus) {
                                            "APPROVED" -> Color.Green
                                            "REJECTED" -> Color.Red
                                            else -> Color.Yellow
                                        }
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { viewModel.approveStorePdf(store.id, true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("قبول وتفعيل الـ PDF", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { viewModel.approveStorePdf(store.id, false) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("رفض المستند", fontSize = 9.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // Premium Checkboxes Grid
                    Column(
                        modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Checkbox(
                                    checked = store.isVip,
                                    onCheckedChange = { viewModel.setStoreVip(store.id, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706)),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("VIP ذهبي", fontSize = 8.sp, color = Color.White)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Checkbox(
                                    checked = store.isVerified,
                                    onCheckedChange = { viewModel.setStoreVerified(store.id, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF3B82F6)),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("موثق حساب", fontSize = 8.sp, color = Color.White)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Checkbox(
                                    checked = store.isRecommended,
                                    onCheckedChange = { viewModel.setStoreRecommended(store.id, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEC4899)),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("موصى به", fontSize = 8.sp, color = Color.White)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Checkbox(
                                    checked = store.isChatDisabled,
                                    onCheckedChange = { viewModel.setStoreChatDisabled(store.id, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444)),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("إيقاف الدردشة 🔇", fontSize = 8.sp, color = Color.LightGray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.2f)) {
                                Checkbox(
                                    checked = store.isNotificationsDisabled,
                                    onCheckedChange = { viewModel.setStoreNotificationsDisabled(store.id, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444)),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("إيقاف الإشعارات 🔕", fontSize = 8.sp, color = Color.LightGray)
                            }
                        }
                    }

                    if (store.isBlocked) {
                        Surface(
                            color = Color(0xFF7F1D1D),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "🚫 هذا المحل محظور حالياً من قبل الأدمن ${if (store.blockReason.isNotEmpty()) "(${store.blockReason})" else ""}",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { storeToEdit = store },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            Text("تعديل", fontSize = 10.sp, color = Color.Black)
                        }

                        Button(
                            onClick = { viewModel.toggleBlockStore(store.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (store.isBlocked) Color(0xFF16A34A) else Color(0xFFDC2626)),
                            modifier = Modifier.weight(1.2f),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            Text(
                                text = if (store.isBlocked) "إلغاء الحظر 🟢" else "حظر المحل 🚫",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                confirmActionType = if (store.isActive) "DEACTIVATE_STORE" else "ACTIVATE_STORE"
                                confirmTargetId = store.id
                                confirmTargetName = store.name
                                showConfirmDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (store.isActive) Color.Gray else Color.Green),
                            modifier = Modifier.weight(1.2f),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            Text(if (store.isActive) "إلغاء التفعيل" else "تفعيل / موافقة", fontSize = 9.sp, color = Color.Black)
                        }

                        Button(
                            onClick = { viewModel.setStorePinned(store.id, !store.isPinned) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (store.isPinned) Color.Yellow else Color.DarkGray),
                            modifier = Modifier.weight(1.2f),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            Text(if (store.isPinned) "إلغاء التثبيت" else "تثبيت في الصدارة", fontSize = 9.sp, color = Color.Black)
                        }

                        Button(
                            onClick = {
                                confirmActionType = "DELETE_STORE"
                                confirmTargetId = store.id
                                confirmTargetName = store.name
                                showConfirmDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            Text("حذف", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Properties Management List
        Text("🏠 مراجعة وإدارة عقارات البيع والإيجار (${activeProperties.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        activeProperties.forEach { prop ->
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(prop.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = if (prop.isActive) "● نشط" else "○ قيد المراجعة",
                            fontSize = 10.sp,
                            color = if (prop.isActive) Color.Green else Color.Yellow,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("السعر: ${prop.price} YER | هاتف: ${prop.phone}", fontSize = 9.sp, color = themeColors.textSecondary)
                    
                    if (prop.password.isNotEmpty()) {
                        Text("🔑 كلمة مرور المعلن المشرف: ${prop.password}", fontSize = 9.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                    }

                    // PDF Document verification block
                    if (prop.pdfFileBase64.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📄 وثائق ملكية/تفاصيل PDF مرفقة:", fontSize = 10.sp, color = Color.White)
                                    Text(
                                        text = when (prop.pdfStatus) {
                                            "APPROVED" -> "✅ معتمد وموافق عليه"
                                            "REJECTED" -> "❌ مرفوض ومستبعد"
                                            else -> "⏳ قيد التدقيق والمراجعة"
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (prop.pdfStatus) {
                                            "APPROVED" -> Color.Green
                                            "REJECTED" -> Color.Red
                                            else -> Color.Yellow
                                        }
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { viewModel.approvePropertyPdf(prop.id, true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("قبول وتفعيل الـ PDF", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { viewModel.approvePropertyPdf(prop.id, false) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("رفض المستند", fontSize = 9.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // Premium Checkboxes Grid
                    Column(
                        modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Checkbox(
                                    checked = prop.isVip,
                                    onCheckedChange = { viewModel.setPropertyVip(prop.id, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706)),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("VIP ذهبي", fontSize = 8.sp, color = Color.White)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Checkbox(
                                    checked = prop.isVerified,
                                    onCheckedChange = { viewModel.setPropertyVerified(prop.id, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF3B82F6)),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("موثق حساب", fontSize = 8.sp, color = Color.White)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Checkbox(
                                    checked = prop.isRecommended,
                                    onCheckedChange = { viewModel.setPropertyRecommended(prop.id, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEC4899)),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("موصى به", fontSize = 8.sp, color = Color.White)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Checkbox(
                                    checked = prop.isChatDisabled,
                                    onCheckedChange = { viewModel.setPropertyChatDisabled(prop.id, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444)),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("إيقاف الدردشة 🔇", fontSize = 8.sp, color = Color.LightGray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.2f)) {
                                Checkbox(
                                    checked = prop.isNotificationsDisabled,
                                    onCheckedChange = { viewModel.setPropertyNotificationsDisabled(prop.id, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444)),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("إيقاف الإشعارات 🔕", fontSize = 8.sp, color = Color.LightGray)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { propertyToEdit = prop },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            Text("تعديل", fontSize = 10.sp, color = Color.Black)
                        }

                        Button(
                            onClick = {
                                confirmActionType = if (prop.isActive) "DEACTIVATE_PROP" else "ACTIVATE_PROP"
                                confirmTargetId = prop.id
                                confirmTargetName = prop.title
                                showConfirmDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (prop.isActive) Color.Gray else Color.Green),
                            modifier = Modifier.weight(1.2f),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            Text(if (prop.isActive) "إلغاء تفعيل" else "تفعيل ونشر", fontSize = 9.sp, color = Color.Black)
                        }

                        Button(
                            onClick = { viewModel.setPropertyPinned(prop.id, !prop.isPinned) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (prop.isPinned) Color.Yellow else Color.DarkGray),
                            modifier = Modifier.weight(1.2f),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            Text(if (prop.isPinned) "إلغاء التثبيت" else "تمييز وتثبيت", fontSize = 9.sp, color = Color.Black)
                        }

                        Button(
                            onClick = {
                                confirmActionType = "DELETE_PROP"
                                confirmTargetId = prop.id
                                confirmTargetName = prop.title
                                showConfirmDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            Text("حذف", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // 📦 ARCHIVE / DELETED ITEMS SECTION
        Text("📦 الأرشيف والمحذوفات (الحذف الناعم) - محلات (${archivedStores.size}) | عقارات (${archivedProperties.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Red)
        if (archivedStores.isNotEmpty() || archivedProperties.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (archivedStores.isNotEmpty()) {
                        Text("🏢 المحلات المحذوفة في الأرشيف:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        archivedStores.forEach { store ->
                            Row(
                                modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.2f)).padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(store.name, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("الهاتف: ${store.phone}", fontSize = 9.sp, color = Color.LightGray)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(
                                        onClick = { viewModel.restoreStore(store.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text("استعادة", fontSize = 9.sp, color = Color.Black)
                                    }
                                    Button(
                                        onClick = {
                                            confirmActionType = "HARD_DELETE_STORE"
                                            confirmTargetId = store.id
                                            confirmTargetName = store.name
                                            showConfirmDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text("حذف نهائي", fontSize = 9.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                    if (archivedProperties.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("🏠 العقارات المحذوفة في الأرشيف:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        archivedProperties.forEach { prop ->
                            Row(
                                modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.2f)).padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prop.title, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("السعر: ${prop.price} YER", fontSize = 9.sp, color = Color.LightGray)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(
                                        onClick = { viewModel.restoreProperty(prop.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text("استعادة", fontSize = 9.sp, color = Color.Black)
                                    }
                                    Button(
                                        onClick = {
                                            confirmActionType = "HARD_DELETE_PROP"
                                            confirmTargetId = prop.id
                                            confirmTargetName = prop.title
                                            showConfirmDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text("حذف نهائي", fontSize = 9.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Text("لا توجد عناصر في الأرشيف حالياً", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
        }

        // Orders Management List
        Text("🛍️ الطلبات والمعاملات المالية الحية (${orders.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        orders.forEach { ord ->
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("السلعة: ${ord.productName} (الكمية: ${ord.quantity})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("إجمالي السعر: ${ord.totalAmount} YER", fontSize = 11.sp, color = themeColors.accent)
                    Text("المشتري: ${ord.customerName} (${ord.customerPhone})", fontSize = 9.sp, color = themeColors.textSecondary)
                    Text("الموقع: ${ord.customerArea}", fontSize = 9.sp, color = themeColors.textSecondary)
                    Text("رقم العملية المالية: ${ord.paymentId}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.updateOrderStatus(ord.id, "COMPLETED") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تأكيد استلام الدفعة والطلب", fontSize = 9.sp, color = Color.Black)
                        }
                        Button(
                            onClick = { viewModel.updateOrderStatus(ord.id, "CANCELLED") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("رفض / إلغاء المعاملة", fontSize = 9.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        storeToEdit?.let { store ->
            StoreCreateEditDialog(
                store = store,
                viewModel = viewModel,
                themeColors = themeColors,
                onDismiss = { storeToEdit = null },
                sectionId = store.sectionId
            )
        }

        propertyToEdit?.let { prop ->
            PropertyCreateEditDialog(
                property = prop,
                viewModel = viewModel,
                themeColors = themeColors,
                onDismiss = { propertyToEdit = null },
                sectionId = prop.sectionId
            )
        }

        if (showConfirmDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("⚠️ تأكيد العملية", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Text(
                        text = when (confirmActionType) {
                            "ACTIVATE_STORE" -> "هل أنت متأكد من تفعيل وموافقة المتجر \"$confirmTargetName\" ونشره للجمهور؟"
                            "DEACTIVATE_STORE" -> "هل أنت متأكد من إلغاء تفعيل المتجر \"$confirmTargetName\" وإخفائه عن الجمهور؟"
                            "DELETE_STORE" -> "هل أنت متأكد من نقل المتجر \"$confirmTargetName\" إلى الأرشيف (حذف ناعم)؟"
                            "HARD_DELETE_STORE" -> "⚠️ تحذير: هل أنت متأكد من الحذف الجذري والنهائي للمتجر \"$confirmTargetName\" من Firestore؟ لا يمكن التراجع عن هذه الخطوة!"
                            "ACTIVATE_PROP" -> "هل أنت متأكد من تفعيل وموافقة العقار \"$confirmTargetName\" ونشره للجمهور؟"
                            "DEACTIVATE_PROP" -> "هل أنت متأكد من إلغاء تفعيل العقار \"$confirmTargetName\" وإخفائه عن الجمهور؟"
                            "DELETE_PROP" -> "هل أنت متأكد من نقل العقار \"$confirmTargetName\" إلى الأرشيف (حذف ناعم)؟"
                            "HARD_DELETE_PROP" -> "⚠️ تحذير: هل أنت متأكد من الحذف الجذري والنهائي للعقار \"$confirmTargetName\" من Firestore؟ لا يمكن التراجع عن هذه الخطوة!"
                            else -> "هل تريد الاستمرار في هذه العملية؟"
                        },
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmDialog = false
                            when (confirmActionType) {
                                "ACTIVATE_STORE" -> viewModel.setStoreActive(confirmTargetId, true)
                                "DEACTIVATE_STORE" -> viewModel.setStoreActive(confirmTargetId, false)
                                "DELETE_STORE" -> viewModel.deleteStore(confirmTargetId)
                                "HARD_DELETE_STORE" -> viewModel.deleteStorePermanently(confirmTargetId)
                                "ACTIVATE_PROP" -> viewModel.setPropertyActive(confirmTargetId, true)
                                "DEACTIVATE_PROP" -> viewModel.setPropertyActive(confirmTargetId, false)
                                "DELETE_PROP" -> viewModel.deleteProperty(confirmTargetId)
                                "HARD_DELETE_PROP" -> viewModel.deletePropertyPermanently(confirmTargetId)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (confirmActionType?.contains("HARD_DELETE") == true) Color.Red else themeColors.accent)
                    ) {
                        Text("نعم، استمر", color = if (confirmActionType?.contains("HARD_DELETE") == true) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("إلغاء", color = Color.White)
                    }
                },
                containerColor = themeColors.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun AdminJobsPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val activatedProviders by viewModel.providers.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var activeJobsSearchQuery by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("💼 إدارة شواغر التوظيف والوظائف الشاملة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        Text("البحث والتحكم في شارات وتفاصيل إعلانات الوظائف وإمكانيات التواصل والاتصال والحذف:", fontSize = 11.sp, color = themeColors.textSecondary)
        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = activeJobsSearchQuery,
            onValueChange = { activeJobsSearchQuery = it },
            label = { Text("البحث في شواغر الوظائف والمهن...") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            trailingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = themeColors.accent) }
        )

        val filteredJobs = activatedProviders.filter {
            val isJob = it.categoryId.contains("job", ignoreCase = true) || 
                        it.profession.contains("job", ignoreCase = true) || 
                        it.profession.contains("وظيفة", ignoreCase = true) || 
                        it.profession.contains("توظيف", ignoreCase = true) || 
                        it.profession.contains("شاغر", ignoreCase = true) || 
                        it.profession.contains("وظائف", ignoreCase = true)
            isJob && (
                it.name.contains(activeJobsSearchQuery, ignoreCase = true) ||
                it.phone.contains(activeJobsSearchQuery, ignoreCase = true) ||
                it.area.contains(activeJobsSearchQuery, ignoreCase = true)
            )
        }

        if (filteredJobs.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("لا توجد إعلانات وظائف مطابقة للبحث حالياً.", fontSize = 11.sp, color = Color.LightGray)
                }
            }
        } else {
            filteredJobs.forEach { p ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                val catName = if (p.categoryId == "other" && p.customCategoryName.isNotEmpty()) p.customCategoryName else (categories.find { it.id == p.categoryId }?.name ?: "إعلان توظيف")
                                Text("المسمى الوظيفي: ${p.profession.ifEmpty { catName }} | المنطقة: ${p.area}", fontSize = 11.sp, color = themeColors.textSecondary)
                            }
                            IconButton(
                                onClick = { viewModel.removeProvider(p.id) }
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف الإعلان نهائياً", tint = Color.Red, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = p.isVip,
                                    onCheckedChange = { viewModel.pinProvider(p.id, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706)),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("VIP ذهبي", fontSize = 10.sp, color = Color.White, maxLines = 1)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = p.isVerified,
                                    onCheckedChange = { viewModel.verifyProviderBadge(p.id, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF3B82F6)),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("موثق حساب", fontSize = 10.sp, color = Color.White, maxLines = 1)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = p.isRecommended,
                                    onCheckedChange = { viewModel.recommendProvider(p.id, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEC4899)),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("موصى به", fontSize = 10.sp, color = Color.White, maxLines = 1)
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = p.isChatDisabled,
                                    onCheckedChange = { viewModel.setProviderChatDisabled(p.id, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444)),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("إيقاف الدردشة 🔇", fontSize = 9.sp, color = Color.LightGray, maxLines = 1)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = p.isNotificationsDisabled,
                                    onCheckedChange = { viewModel.setProviderNotificationsDisabled(p.id, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444)),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("إيقاف الإشعارات 🔕", fontSize = 9.sp, color = Color.LightGray, maxLines = 1)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = p.isPaymentRequired,
                                    onCheckedChange = { viewModel.setProviderPaymentRequired(p.id, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981)),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("ربط بالدفع 💳", fontSize = 9.sp, color = Color.LightGray, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
