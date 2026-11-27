package com.example.ui.screens.entities

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.*
import com.example.ui.components.SmartAsyncImage
import com.example.ui.helpers.AppPreferenceHelper
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
    BackHandler { onBackClick() }

    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.products.collectAsState()
    val ratings by viewModel.ratings.collectAsState()
    val bookings by viewModel.bookings.collectAsState()

    val allProviders by viewModel.providers.collectAsState()
    val allStores by viewModel.stores.collectAsState()
    val allProperties by viewModel.properties.collectAsState()
    val allJobs by viewModel.jobs.collectAsState()

    val liveProvider = remember(provider, allProviders) {
        provider?.let { p -> allProviders.find { it.id == p.id } ?: p }
    }
    val liveStore = remember(store, allStores) {
        store?.let { s -> allStores.find { it.id == s.id } ?: s }
    }
    val liveProperty = remember(property, allProperties) {
        property?.let { pr -> allProperties.find { it.id == pr.id } ?: pr }
    }
    val liveJob = remember(job, allJobs) {
        job?.let { j -> allJobs.find { it.id == j.id } ?: j }
    }

    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()

    // Determine Entity Type dynamically
    val entityType = remember(liveProvider, liveStore, liveProperty, liveJob, categories) {
        when {
            liveProvider != null -> {
                val catName = categories.find { it.id == liveProvider.categoryId }?.name?.lowercase() ?: ""
                when {
                    liveProvider.providerType.contains("medical", ignoreCase = true) || liveProvider.categoryId.contains("medical", ignoreCase = true) || catName.contains("طب") || catName.contains("عياد") || liveProvider.profession.contains("طبي") || liveProvider.profession.contains("عياد") -> ProfileEntityType.MEDICAL
                    liveProvider.categoryId == "restaurants" || catName.contains("مطعم") || catName.contains("كافيه") -> ProfileEntityType.RESTAURANT
                    liveProvider.categoryId == "stores" || catName.contains("متجر") || catName.contains("سوق") -> ProfileEntityType.STORE
                    else -> ProfileEntityType.TECHNICIAN
                }
            }
            liveStore != null -> {
                when {
                    liveStore.sectionId.contains("restaurant", ignoreCase = true) -> ProfileEntityType.RESTAURANT
                    liveStore.sectionId.contains("medical", ignoreCase = true) || liveStore.medicalLicenseNo.isNotBlank() -> ProfileEntityType.MEDICAL
                    else -> ProfileEntityType.STORE
                }
            }
            liveProperty != null -> ProfileEntityType.REAL_ESTATE
            liveJob != null -> ProfileEntityType.JOB
            else -> ProfileEntityType.GENERAL
        }
    }

    // Unify primary fields
    val entityName = liveProvider?.name ?: liveStore?.name ?: liveProperty?.title ?: liveJob?.title ?: "ملف النشاط"
    val entityPhone = liveProvider?.phone ?: liveStore?.phone ?: liveProperty?.phone ?: liveJob?.phone ?: ""
    val entityAddress = liveProvider?.localNeighborhood?.ifEmpty { liveProvider.area }
        ?: liveStore?.localNeighborhood?.ifEmpty { liveStore.cityId }
        ?: liveProperty?.localNeighborhood?.ifEmpty { liveProperty.cityId }
        ?: liveJob?.address?.ifEmpty { liveJob.cityId } ?: "اليمن"
    val ratingValue = liveProvider?.rating ?: liveStore?.rating ?: liveProperty?.rating ?: 5.0f
    val reviewsCount = liveProvider?.numReviews ?: liveStore?.numReviews ?: liveProperty?.numReviews ?: 0
    val profileCover = liveProvider?.coverImage?.ifEmpty { liveProvider.profileImage }
        ?: liveStore?.coverImage?.ifEmpty { liveStore.logoImage }
        ?: liveProperty?.images?.firstOrNull() ?: ""
    val entityDescription = liveProvider?.profession?.ifEmpty { liveProvider.specialization }
        ?: liveStore?.description
        ?: liveProperty?.description
        ?: liveJob?.description ?: "لا يوجد وصف متاح."

    var selectedTab by remember { mutableIntStateOf(0) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    val entityId = liveProvider?.id ?: liveStore?.id ?: liveProperty?.id ?: liveJob?.id ?: ""
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val isFav = remember(favoriteIds, entityId) { favoriteIds.contains(entityId) }
    val entityReviews = remember(ratings, entityId) {
        ratings.filter { it.targetId == entityId }
    }

    // Ownership logic: check if logged-in user is the owner
    val joinPhoneState by viewModel.joinRequestPhone.collectAsState()
    val isOwner = remember(currentUserId, currentUserPhone, liveProvider, liveStore, liveProperty, adminRole, joinPhoneState) {
        val phoneClean = AppPreferenceHelper.normalizePhoneNumber(currentUserPhone)
        val uidClean = currentUserId.trim()
        val provPhone = AppPreferenceHelper.normalizePhoneNumber(liveProvider?.phone ?: "")
        val storePhone = AppPreferenceHelper.normalizePhoneNumber(liveStore?.phone ?: "")
        val storeOwner = liveStore?.ownerId?.trim() ?: ""
        val propPhone = AppPreferenceHelper.normalizePhoneNumber(liveProperty?.phone ?: "")
        val propOwner = liveProperty?.ownerId?.trim() ?: ""
        val joinPhone = AppPreferenceHelper.normalizePhoneNumber(joinPhoneState)

        val provId = liveProvider?.id?.trim() ?: ""
        val storeId = liveStore?.id?.trim() ?: ""
        val propId = liveProperty?.id?.trim() ?: ""

        (uidClean.isNotEmpty() && (uidClean == provId || uidClean == storeId || uidClean == propId || uidClean == storeOwner || uidClean == propOwner)) ||
        (phoneClean.isNotEmpty() && (phoneClean == provPhone || phoneClean == storePhone || phoneClean == propPhone)) ||
        (joinPhone.isNotEmpty() && (joinPhone == provPhone || joinPhone == storePhone || joinPhone == propPhone))
    }

    val entityBookings = remember(bookings, entityId, entityPhone) {
        val cleanP = AppPreferenceHelper.normalizePhoneNumber(entityPhone)
        bookings.filter { b ->
            b.providerId == entityId ||
            (cleanP.isNotEmpty() && AppPreferenceHelper.normalizePhoneNumber(b.providerPhone) == cleanP)
        }
    }
    val calculatedBookingsCount = entityBookings.size
    val calculatedRevenue = entityBookings
        .filter { it.status.uppercase() in listOf("COMPLETED", "PAID", "CLOSED", "DELIVERED") }
        .sumOf { if (it.totalAmount > 0.0) it.totalAmount else it.price }

    var previewAsCustomer by remember { mutableStateOf(false) }
    val isAdminUser = remember(adminRole) {
        adminRole.isNotBlank() && !adminRole.equals("GUEST", ignoreCase = true)
    }

    if (liveStore != null && (!isOwner || previewAsCustomer)) {
        StoreCustomerCatalogView(
            store = liveStore,
            products = products,
            viewModel = viewModel,
            themeColors = themeColors,
            onBackClick = onBackClick,
            isAdmin = isAdminUser,
            isOwnerPreview = isOwner && previewAsCustomer,
            onExitOwnerPreview = if (isOwner) { { previewAsCustomer = false } } else null
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Column {
                        Text(
                            text = entityName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.textPrimary,
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
                            tint = themeColors.textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite(entityId) }) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "المفضلة",
                            tint = if (isFav) Color(0xFFEF4444) else themeColors.textPrimary
                        )
                    }
                    if (entityPhone.isNotBlank()) {
                        IconButton(onClick = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$entityPhone"))
                                context.startActivity(intent)
                            }.onFailure {
                                Toast.makeText(context, "تعذر فتح تطبيق الهاتف", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = "اتصال", tint = Color(0xFF10B981))
                        }
                    }
                    IconButton(onClick = {
                        runCatching {
                            val shareText = "دليل خدمات اليمن | $entityName\n${entityType.labelAr}\nالعنوان: $entityAddress\nالهاتف: $entityPhone"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "مشاركة الملف"))
                        }.onFailure {
                            Toast.makeText(context, "تعذر فتح قائمة المشاركة", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "مشاركة", tint = themeColors.textPrimary)
                    }
                    if (!isOwner) {
                        IconButton(
                            onClick = { showReportDialog = true },
                            modifier = Modifier.testTag("report_entity_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "إبلاغ عن مخالفة",
                                tint = Color(0xFFF59E0B)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = themeColors.surface)
            )
        },
        bottomBar = {
            ProfileActions(
                entityId = entityId,
                entityName = entityName,
                entityPhone = entityPhone,
                entityType = entityType,
                isOwner = isOwner,
                viewModel = viewModel,
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
            // 1. HERO BANNER & PROFILE HEADER
            item {
                ProfileHeader(
                    entityName = entityName,
                    entityType = entityType,
                    entityAddress = entityAddress,
                    ratingValue = ratingValue,
                    reviewsCount = reviewsCount,
                    profileCover = profileCover,
                    entityDescription = entityDescription,
                    isVerified = liveProvider?.isVerified ?: liveStore?.isVerified ?: liveProperty?.isVerified ?: true,
                    isVip = liveProvider?.isVip ?: liveStore?.isVip ?: liveProperty?.isVip ?: false,
                    isOwner = isOwner,
                    bookingsCount = calculatedBookingsCount,
                    completedRevenue = calculatedRevenue,
                    themeColors = themeColors
                )
            }

            // 2. PROMINENT DASHBOARD BUTTON FOR OWNER
            if (isOwner) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, themeColors.accent)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "👑 مرحباً بك يا صاحب المنشأة / الفني!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.accent
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val destination = when (entityType) {
                                            ProfileEntityType.TECHNICIAN -> "TECHNICIAN_DASHBOARD"
                                            ProfileEntityType.STORE -> "STORE_DASHBOARD"
                                            ProfileEntityType.RESTAURANT -> "RESTAURANT_DASHBOARD"
                                            ProfileEntityType.MEDICAL -> "MEDICAL_DASHBOARD"
                                            ProfileEntityType.REAL_ESTATE -> "PROPERTY_DASHBOARD"
                                            ProfileEntityType.JOB -> "JOB_POSTER_DASHBOARD"
                                            else -> "TECHNICIAN_DASHBOARD"
                                        }
                                        viewModel.navigateTo(destination)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Text(
                                        text = "لوحة التحكم ⚙️",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                                OutlinedButton(
                                    onClick = { previewAsCustomer = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, themeColors.accent),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "معاينة كعميل 👁️",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = themeColors.accent
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. OWNER CONTROL BAR
            if (isOwner || isAdminUser) {
                item {
                    ProfileOwnerAdminControlBar(
                        entityId = entityId,
                        entityType = entityType,
                        provider = liveProvider,
                        store = liveStore,
                        property = liveProperty,
                        job = liveJob,
                        isOwner = isOwner,
                        isAdmin = isAdminUser,
                        viewModel = viewModel,
                        themeColors = themeColors
                    )
                }
            }

            // 4. SPECS & DETAILS
            item {
                ProfileSpecs(
                    entityType = entityType,
                    provider = liveProvider,
                    store = liveStore,
                    property = liveProperty,
                    job = liveJob,
                    products = products,
                    themeColors = themeColors
                )
            }

            // 5. TABS & CONTENT
            item {
                ProfileTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    entityType = entityType,
                    themeColors = themeColors
                )
            }

            item {
                ProfileTabContent(
                    selectedTab = selectedTab,
                    entityType = entityType,
                    provider = liveProvider,
                    store = liveStore,
                    property = liveProperty,
                    job = liveJob,
                    products = products,
                    entityReviews = entityReviews,
                    entityDescription = entityDescription,
                    themeColors = themeColors,
                    isOwner = isOwner,
                    onAddReviewClick = { showRatingDialog = true },
                    onRequestBooking = onRequestBooking
                )
            }
        }
    }

    if (showRatingDialog) {
        var ratingStars by remember { mutableIntStateOf(5) }
        var reviewComment by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRatingDialog = false },
            containerColor = themeColors.surface,
            titleContentColor = themeColors.textPrimary,
            title = {
                Text("تقييم $entityName ⭐", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { ratingStars = star }) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "$star نجوم",
                                    tint = if (star <= ratingStars) Color(0xFFFFD700) else themeColors.textSecondary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        label = { Text("اكتب رأيك وتجربتك (اختياري)", color = themeColors.textSecondary) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ratingObj = RatingEntity(
                            id = com.example.utils.EntityIdGenerator.generate("REV"),
                            targetId = entityId,
                            targetType = entityType.name,
                            userId = currentUserId.ifBlank { "guest" },
                            userName = currentUserName.ifBlank { "عميل" },
                            rating = ratingStars.toFloat(),
                            comment = reviewComment.trim(),
                            timestamp = System.currentTimeMillis()
                        )
                        viewModel.adminViewModel.submitRating(ratingObj) {
                            Toast.makeText(context, "شكراً لك! تم حفظ تقييمك بنجاح ✅", Toast.LENGTH_SHORT).show()
                        }
                        showRatingDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("إرسال التقييم ⭐", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRatingDialog = false }) {
                    Text("إلغاء", color = themeColors.textSecondary)
                }
            }
        )
    }

    if (showReportDialog && entityId.isNotBlank()) {
        com.example.ui.dialogs.SubmitReportDialog(
            targetId = entityId,
            targetName = entityName,
            targetType = entityType.name,
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { showReportDialog = false }
        )
    }
}
