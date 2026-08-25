package com.example.ui.screens.entities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.components.SmartAsyncImage
import com.example.utils.VisualThemePalette

enum class ProfileEntityType(val labelAr: String, val badgeColor: Color) {
    TECHNICIAN("فني / مهني معتمد", Color(0xFF3B82F6)),
    STORE("متجر / تسوق تجاري", Color(0xFF10B981)),
    RESTAURANT("مطعم / كافيه", Color(0xFFF59E0B)),
    MEDICAL("مركز طبي / عيادة", Color(0xFFEF4444)),
    REAL_ESTATE("مكتب عقاري / استثمار", Color(0xFF8B5CF6)),
    JOB("شواغر وفرص عمل", Color(0xFF06B6D4)),
    GENERAL("نشاط تجاري معتمد", Color(0xFF6B7280))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicPolymorphicProfileScreen(
    provider: ProviderEntity? = null,
    store: StoreEntity? = null,
    property: PropertyEntity? = null,
    job: JobEntity? = null,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit,
    onOpenChat: (channelId: String) -> Unit = {},
    onRequestBooking: () -> Unit = {},
    onOrderProduct: () -> Unit = {}
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.products.collectAsState()
    val ratings by viewModel.ratings.collectAsState()
    val bookings by viewModel.bookings.collectAsState()

    // Current user authentication states
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()

    // Determine Entity Type dynamically
    val entityType = remember(provider, store, property, job) {
        when {
            provider != null -> {
                val catName = categories.find { it.id == provider.categoryId }?.name?.lowercase() ?: ""
                when {
                    provider.categoryId == "medical" || catName.contains("طب") || catName.contains("عياد") -> ProfileEntityType.MEDICAL
                    provider.categoryId == "restaurants" || catName.contains("مطعم") || catName.contains("كافيه") -> ProfileEntityType.RESTAURANT
                    provider.categoryId == "stores" || catName.contains("متجر") || catName.contains("سوق") -> ProfileEntityType.STORE
                    else -> ProfileEntityType.TECHNICIAN
                }
            }
            store != null -> {
                when (store.sectionId) {
                    "restaurants" -> ProfileEntityType.RESTAURANT
                    "medical" -> ProfileEntityType.MEDICAL
                    else -> ProfileEntityType.STORE
                }
            }
            property != null -> ProfileEntityType.REAL_ESTATE
            job != null -> ProfileEntityType.JOB
            else -> ProfileEntityType.GENERAL
        }
    }

    // Unify primary fields
    val entityName = provider?.name ?: store?.name ?: property?.title ?: job?.title ?: "ملف النشاط"
    val entityPhone = provider?.phone ?: store?.phone ?: property?.phone ?: job?.phone ?: ""
    val entityAddress = provider?.localNeighborhood?.ifEmpty { provider.area }
        ?: store?.localNeighborhood?.ifEmpty { store.cityId }
        ?: property?.localNeighborhood?.ifEmpty { property.cityId }
        ?: job?.address?.ifEmpty { job.cityId } ?: "اليمن"
    val ratingValue = provider?.rating ?: store?.rating ?: property?.rating ?: 5.0f
    val reviewsCount = provider?.numReviews ?: store?.numReviews ?: property?.numReviews ?: 0
    val profileCover = provider?.coverImage?.ifEmpty { provider.profileImage }
        ?: store?.coverImage?.ifEmpty { store.logoImage }
        ?: property?.images?.firstOrNull() ?: ""
    val entityDescription = provider?.profession?.ifEmpty { provider.specialization }
        ?: store?.description
        ?: property?.description
        ?: job?.description ?: "لا يوجد وصف متاح."

    var selectedTab by remember { mutableStateOf(0) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    val entityId = provider?.id ?: store?.id ?: property?.id ?: job?.id ?: ""
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val isFav = remember(favoriteIds, entityId) { favoriteIds.contains(entityId) }
    val entityReviews = remember(ratings, entityId) {
        ratings.filter { it.targetId == entityId }
    }

    // Ownership logic: check if logged-in user is the provider/owner
    val isOwner = remember(currentUserId, currentUserPhone, provider, store, property) {
        val phoneClean = currentUserPhone.trim()
        val uidClean = currentUserId.trim()
        val provPhone = provider?.phone?.trim() ?: ""
        val storePhone = store?.phone?.trim() ?: ""
        val storeOwner = store?.ownerId?.trim() ?: ""
        val propPhone = property?.phone?.trim() ?: ""
        val propOwner = property?.ownerId?.trim() ?: ""

        (uidClean.isNotEmpty() && (uidClean == storeOwner || uidClean == propOwner)) ||
        (phoneClean.isNotEmpty() && (phoneClean == provPhone || phoneClean == storePhone || phoneClean == propPhone))
    }

    // Dynamic stats calculations
    val bookingsCount = remember(bookings, entityId) {
        bookings.count { it.providerId == entityId }
    }
    val completedRevenue = remember(bookings, entityId) {
        bookings.filter { it.providerId == entityId && (it.status == "COMPLETED" || it.status == "APPROVED") }
            .sumOf { it.totalAmount }
    }

    if (showRatingDialog) {
        com.example.ui.dialogs.MultiDimensionRatingDialog(
            targetId = entityId,
            targetName = entityName,
            targetType = entityType.name,
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { showRatingDialog = false }
        )
    }

    if (showReportDialog) {
        com.example.ui.dialogs.SubmitReportDialog(
            targetId = entityId,
            targetName = entityName,
            targetType = entityType.name,
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { showReportDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = entityName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = entityType.labelAr,
                            fontSize = 11.sp,
                            color = entityType.badgeColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite(entityId) }) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "المفضلة",
                            tint = if (isFav) Color(0xFFEF4444) else Color.White
                        )
                    }
                    IconButton(onClick = { showReportDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "إبلاغ",
                            tint = Color(0xFFEF4444)
                        )
                    }
                    if (entityPhone.isNotBlank()) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$entityPhone"))
                            context.startActivity(intent)
                        }) {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = "اتصال", tint = Color(0xFF10B981))
                        }
                    }
                    IconButton(onClick = {
                        val shareText = "دليل خدمات اليمن | $entityName\n${entityType.labelAr}\nالعنوان: $entityAddress\nالهاتف: $entityPhone"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "مشاركة الملف"))
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "مشاركة", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColors.surface
                )
            )
        },
        bottomBar = {
            ProfileActions(
                entityId = entityId,
                entityName = entityName,
                entityPhone = entityPhone,
                entityType = entityType,
                isOwner = isOwner,
                themeColors = themeColors,
                onOpenChat = onOpenChat,
                onRequestBooking = onRequestBooking,
                onOrderProduct = onOrderProduct,
                onEditProfile = { viewModel.navigateTo("OWNER_PROFILE_VIEW") },
                onEditProducts = { viewModel.navigateTo("PRODUCTS_MGMT_VIEW") },
                onEditGallery = { viewModel.navigateTo("GALLERY_MGMT_VIEW") }
            )
        },
        containerColor = themeColors.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 🌟 1. HERO BANNER, AVATAR & BUSINESS METRICS
            item {
                ProfileHeader(
                    entityName = entityName,
                    entityType = entityType,
                    entityAddress = entityAddress,
                    ratingValue = ratingValue,
                    reviewsCount = reviewsCount,
                    profileCover = profileCover,
                    entityDescription = entityDescription,
                    isVerified = provider?.isVerified ?: store?.isVerified ?: property?.isVerified ?: true,
                    isVip = provider?.isVip ?: store?.isVip ?: property?.isVip ?: false,
                    isOwner = isOwner,
                    bookingsCount = bookingsCount,
                    completedRevenue = completedRevenue,
                    themeColors = themeColors
                )
            }

            // 🌟 2. POLYMORPHIC STRUCTURED DATA (Tailored Fields per category)
            item {
                ProfileSpecs(
                    entityType = entityType,
                    provider = provider,
                    store = store,
                    property = property,
                    job = job,
                    products = products,
                    themeColors = themeColors
                )
            }

            // 🌟 3. TABS (معرض الأعمال / المنتجات / التقييمات / تفاصيل إضافية)
            item {
                ProfileTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    entityType = entityType,
                    themeColors = themeColors
                )
            }

            // 🌟 4. TAB CONTENTS
            item {
                when (selectedTab) {
                    0 -> {
                        // First Tab (Photos / Products / Menu / Listings)
                        when (entityType) {
                            ProfileEntityType.TECHNICIAN -> {
                                val photos = provider?.workPhotosBase64 ?: emptyList()
                                if (photos.isEmpty()) {
                                    EmptyStateBox("لا توجد صور سابقة أعمال مرفوعة حالياً.", themeColors)
                                } else {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(photos) { photo ->
                                            Card(
                                                modifier = Modifier.size(120.dp),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                SmartAsyncImage(model = photo, contentDescription = "عمل سابق", modifier = Modifier.fillMaxSize())
                                            }
                                        }
                                    }
                                }
                            }
                            ProfileEntityType.STORE, ProfileEntityType.RESTAURANT, ProfileEntityType.MEDICAL -> {
                                val storeProducts = products.filter { it.storeId == (store?.id ?: "") }
                                if (storeProducts.isEmpty()) {
                                    EmptyStateBox("لا توجد أصناف مدرجة في القائمة حالياً.", themeColors)
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        storeProducts.forEach { prod ->
                                            ProfileProductCard(product = prod, themeColors = themeColors)
                                        }
                                    }
                                }
                            }
                            ProfileEntityType.REAL_ESTATE -> {
                                val images = property?.images ?: emptyList()
                                if (images.isEmpty()) {
                                    EmptyStateBox("لا توجد صور إضافية للعقار.", themeColors)
                                } else {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(images) { img ->
                                            Card(
                                                modifier = Modifier.size(140.dp),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                SmartAsyncImage(model = img, contentDescription = "صورة العقار", modifier = Modifier.fillMaxSize())
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                Text(
                                    text = entityDescription,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                    1 -> {
                        // Reviews / Secondary info
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("التقييمات وآراء العملاء ⭐", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                Button(
                                    onClick = { showRatingDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("أضف تقييمك ⭐", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (entityReviews.isEmpty()) {
                                EmptyStateBox("لا توجد تقييمات مسجلة بعد. كن أول من يكتب تقييماً!", themeColors)
                            } else {
                                entityReviews.forEach { review ->
                                    ProfileReviewCard(review = review, themeColors = themeColors)
                                }
                            }
                        }
                    }
                    2 -> {
                        // Warranty / Working Hours / Policy
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📜 الشروط والضمان المعتمد بالمنصة", fontWeight = FontWeight.Bold, color = themeColors.accent, fontSize = 13.sp)
                                Text("• جميع التعاملات تخضع لميثاق الجودة والحماية في دليل خدمات اليمن.", color = Color.White, fontSize = 11.sp)
                                Text("• إمكانية استرجاع الرسوم أو رفع شكوى مباشرة لإدارة المنصة في حال الإخلال بالمواصفات.", color = Color.LightGray, fontSize = 11.sp)
                                Text("• الدفع المباشر عبر المحافظ الإلكترونية المعتمدة (الكريمي، جيب، جوالي، ون كاش).", color = Color.LightGray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateBox(msg: String, themeColors: VisualThemePalette) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = msg, color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}
