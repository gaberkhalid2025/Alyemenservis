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
            // Polymorphic Action Bar at the bottom
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chat Button
                    Button(
                        onClick = {
                            val channelId = when {
                                provider != null -> "chat_p_${provider.id}"
                                store != null -> "chat_store_${store.id}"
                                property != null -> "chat_prop_${property.id}"
                                else -> "chat_general_$entityId"
                            }
                            onOpenChat(channelId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.MailOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("محادثة 💬", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Dynamic Primary Action
                    when (entityType) {
                        ProfileEntityType.TECHNICIAN -> {
                            Button(
                                onClick = onRequestBooking,
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.3f).height(44.dp)
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طلب حجز صيانة 🔧", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                        ProfileEntityType.STORE -> {
                            Button(
                                onClick = onOrderProduct,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.3f).height(44.dp)
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طلب شراء بضاعة 🛍️", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        ProfileEntityType.RESTAURANT -> {
                            Button(
                                onClick = onOrderProduct,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.3f).height(44.dp)
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حجز طاولة / طلب 🍽️", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                        ProfileEntityType.MEDICAL -> {
                            Button(
                                onClick = onRequestBooking,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.3f).height(44.dp)
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حجز موعد طبي 🩺", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        ProfileEntityType.REAL_ESTATE -> {
                            Button(
                                onClick = onRequestBooking,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.3f).height(44.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طلب معاينة العقار 🏡", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        ProfileEntityType.JOB -> {
                            Button(
                                onClick = {
                                    if (entityPhone.isNotBlank()) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/967$entityPhone?text=${Uri.encode("مرحباً، أود التقديم على فرصة العمل: $entityName")}"))
                                        context.startActivity(intent)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.3f).height(44.dp)
                            ) {
                                Icon(Icons.Default.AccountBox, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تقديم السيرة الذاتية 📄", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                        ProfileEntityType.GENERAL -> {
                            Button(
                                onClick = onRequestBooking,
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.3f).height(44.dp)
                            ) {
                                Text("طلب الخدمة 🚀", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }
            }
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
            // 🌟 1. HERO BANNER & AVATAR
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.25f))
                ) {
                    Column {
                        // Cover & Hero image
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(themeColors.primary, themeColors.secondary)
                                    )
                                )
                        ) {
                            if (profileCover.isNotBlank()) {
                                SmartAsyncImage(
                                    model = profileCover,
                                    contentDescription = entityName,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            // Type Tag
                            Box(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .align(Alignment.TopEnd)
                                    .background(entityType.badgeColor, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = entityType.labelAr,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Info Body
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entityName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "📍 $entityAddress",
                                        fontSize = 12.sp,
                                        color = Color.LightGray
                                    )
                                }

                                // Rating Badge
                                Surface(
                                    color = Color(0xFFFFA000).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color(0xFFFFA000)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(String.format("%.1f", ratingValue), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(" ($reviewsCount)", color = Color.LightGray, fontSize = 10.sp)
                                    }
                                }
                            }

                            Text(
                                text = entityDescription,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // 🌟 2. POLYMORPHIC STRUCTURED DATA (Tailored Fields per category)
            item {
                when (entityType) {
                    ProfileEntityType.TECHNICIAN -> TechnicianSpecificSpecsView(provider, themeColors)
                    ProfileEntityType.STORE -> StoreSpecificSpecsView(store, products, themeColors)
                    ProfileEntityType.RESTAURANT -> RestaurantSpecificSpecsView(store, products, themeColors)
                    ProfileEntityType.MEDICAL -> MedicalSpecificSpecsView(provider, store, themeColors)
                    ProfileEntityType.REAL_ESTATE -> RealEstateSpecificSpecsView(property, themeColors)
                    ProfileEntityType.JOB -> JobSpecificSpecsView(job, themeColors)
                    ProfileEntityType.GENERAL -> GeneralSpecificSpecsView(provider, store, themeColors)
                }
            }

            // 🌟 3. TABS (معرض الأعمال / المنتجات / التقييمات / تفاصيل إضافية)
            item {
                val tabTitles = when (entityType) {
                    ProfileEntityType.TECHNICIAN -> listOf("سابقة الأعمال 📸", "التقييمات والآراء ⭐", "شروط الضمان 🛡️")
                    ProfileEntityType.STORE -> listOf("الكتالوج والبضائع 📦", "العروض والخصومات 🏷️", "تقييمات المتجر ⭐")
                    ProfileEntityType.RESTAURANT -> listOf("منيو الأكلات 🍔", "العروض الخاصة 🎁", "آراء الذواقة ⭐")
                    ProfileEntityType.MEDICAL -> listOf("العيادات والتخصصات 🩺", "أوقات الدوام والطوارئ ⏰", "آراء المراجعين ⭐")
                    ProfileEntityType.REAL_ESTATE -> listOf("صور العقار والمخطط 📐", "المواصفات والخدمات 🏢", "معاينة وخريطة 🗺️")
                    ProfileEntityType.JOB -> listOf("شروط ومؤهلات الوظيفة 📋", "المزايا والحوافز 💰", "عن جهة العمل 🏢")
                    ProfileEntityType.GENERAL -> listOf("الخدمات 🛠️", "التقييمات ⭐", "معلومات التواصل 📞")
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = themeColors.surface,
                    contentColor = themeColors.accent,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) themeColors.accent else Color.LightGray
                                )
                            }
                        )
                    }
                }
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
                            ProfileEntityType.STORE, ProfileEntityType.RESTAURANT -> {
                                val storeProducts = products.filter { it.storeId == (store?.id ?: "") }
                                if (storeProducts.isEmpty()) {
                                    EmptyStateBox("لا توجد أصناف مدرجة في القائمة حالياً.", themeColors)
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        storeProducts.forEach { prod ->
                                            ProductItemCard(prod, themeColors)
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
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(review.userName.ifEmpty { "عميل معتمد" }, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                                Row {
                                                    val ratingCount = review.rating.toInt().coerceIn(1, 5)
                                                    repeat(ratingCount) {
                                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }
                                            Text(review.comment, color = Color.LightGray, fontSize = 11.sp)
                                            if (review.reply.isNotBlank()) {
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(6.dp)) {
                                                        Text("رد المنشأة / المزود 💬:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                                        Text(review.reply, fontSize = 10.5.sp, color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }
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

// ----------------------------------------------------
// POLYMORPHIC SPECIFIC CARDS (الحقول المخصصة لكل فئة)
// ----------------------------------------------------

@Composable
fun TechnicianSpecificSpecsView(provider: ProviderEntity?, themeColors: VisualThemePalette) {
    Card(
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🔧 بيانات المهنة والاعتماد الفني", fontWeight = FontWeight.Bold, color = Color(0xFF60A5FA), fontSize = 13.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SpecBadge("سعر المعاينة:", "${provider?.previewPrice?.toInt() ?: 1500} ر.ي", Icons.Default.CheckCircle)
                SpecBadge("الحالة:", if (provider?.isAvailable == true) "متاح للعمل الآن 🟢" else "مشغول 🔴", Icons.Default.Info)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SpecBadge("التوثيق:", if (provider?.isVerified == true) "موثق بالهوية ✅" else "قيد التدقيق", Icons.Default.Star)
                SpecBadge("المدينة:", if (!provider?.cityId.isNullOrEmpty()) provider?.cityId!! else "صنعاء", Icons.Default.LocationOn)
            }
        }
    }
}

@Composable
fun StoreSpecificSpecsView(store: StoreEntity?, products: List<ProductEntity>, themeColors: VisualThemePalette) {
    Card(
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🛍️ تفاصيل المتجر وسياسة التوصيل", fontWeight = FontWeight.Bold, color = Color(0xFF34D399), fontSize = 13.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SpecBadge("أوقات الدوام:", store?.workingHours ?: "9:00 ص - 10:00 م", Icons.Default.AccountBox)
                SpecBadge("عدد المنتجات:", "${products.count { it.storeId == store?.id }} صنف", Icons.Default.ShoppingCart)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SpecBadge("طرق الدفع:", "محافظ إلكترونية + نقد", Icons.Default.Star)
                SpecBadge("السجل التجاري:", if (!store?.commercialRegisterNo.isNullOrEmpty()) store?.commercialRegisterNo!! else "معتمد بالمنصة", Icons.Default.CheckCircle)
            }
        }
    }
}

@Composable
fun RestaurantSpecificSpecsView(store: StoreEntity?, products: List<ProductEntity>, themeColors: VisualThemePalette) {
    Card(
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🍽️ خدمات المطعم والضيافة", fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24), fontSize = 13.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SpecBadge("أوقات الوجبات:", "فطور - غداء - عشاء", Icons.Default.Favorite)
                SpecBadge("خدمة التوصيل:", "سريع لجميع الأحياء 🛵", Icons.Default.Send)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SpecBadge("جلسات عائلية:", "متوفرة وقسم خاص 👨‍👩‍👧", Icons.Default.Home)
                SpecBadge("حجز مسبق:", "متاح عبر التطبيق 📱", Icons.Default.DateRange)
            }
        }
    }
}

@Composable
fun MedicalSpecificSpecsView(provider: ProviderEntity?, store: StoreEntity?, themeColors: VisualThemePalette) {
    Card(
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🩺 بيانات الاعتماد الطبي والعيادات", fontWeight = FontWeight.Bold, color = Color(0xFFF87171), fontSize = 13.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SpecBadge("قسم الطوارئ:", "متاح 24 ساعة 🚨", Icons.Default.Warning)
                SpecBadge("الترخيص الطبي:", store?.medicalLicenseNo?.ifEmpty { "مرخص رسمياً 📄" } ?: "مرخص رسمياً 📄", Icons.Default.CheckCircle)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SpecBadge("حجز الكشوفات:", "مسبق لتجنب الانتظار ⏱️", Icons.Default.DateRange)
                SpecBadge("المختبر والأشعة:", "فحوصات متكاملة 🔬", Icons.Default.Star)
            }
        }
    }
}

@Composable
fun RealEstateSpecificSpecsView(property: PropertyEntity?, themeColors: VisualThemePalette) {
    Card(
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🏢 مواصفات العقار والاستثمار", fontWeight = FontWeight.Bold, color = Color(0xFFA78BFA), fontSize = 13.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SpecBadge("نوع العرض:", if (property?.type == "rent") "إيجار شهري/سنوي 🔑" else "للبيع والشراء 📜", Icons.Default.Home)
                SpecBadge("السعر:", "${property?.price?.toInt() ?: 0} ${property?.currency ?: "YER"}", Icons.Default.Star)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SpecBadge("تصنيف العقار:", if (!property?.propertyType.isNullOrEmpty()) property?.propertyType!! else "شقة سكنية", Icons.Default.LocationOn)
                SpecBadge("المعاينة:", "متاحة بالتنسيق المباشر 🚶‍♂️", Icons.Default.CheckCircle)
            }
        }
    }
}

@Composable
fun JobSpecificSpecsView(job: JobEntity?, themeColors: VisualThemePalette) {
    Card(
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("💼 تفاصيل فرصة العمل", fontWeight = FontWeight.Bold, color = Color(0xFF22D3EE), fontSize = 13.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SpecBadge("نوع الدوام:", if (!job?.jobType.isNullOrEmpty()) job?.jobType!! else "دوام كامل", Icons.Default.AccountBox)
                SpecBadge("الراتب المتوقع:", if (!job?.salary.isNullOrEmpty()) job?.salary!! else "حسب الاتفاق", Icons.Default.Star)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SpecBadge("اسم الشركة:", if (!job?.companyName.isNullOrEmpty()) job?.companyName!! else "جهة معتمدة", Icons.Default.CheckCircle)
                SpecBadge("المدينة:", if (!job?.cityId.isNullOrEmpty()) job?.cityId!! else "صنعاء", Icons.Default.LocationOn)
            }
        }
    }
}

@Composable
fun GeneralSpecificSpecsView(provider: ProviderEntity?, store: StoreEntity?, themeColors: VisualThemePalette) {
    Card(
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("📋 معلومات النشاط", fontWeight = FontWeight.Bold, color = themeColors.accent, fontSize = 13.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SpecBadge("الخدمات:", "متنوعة ومعتمدة", Icons.Default.Build)
                SpecBadge("الحالة:", "نشط على المنصة ✅", Icons.Default.CheckCircle)
            }
        }
    }
}

@Composable
fun SpecBadge(title: String, value: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(title, fontSize = 9.sp, color = Color.Gray)
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun ProductItemCard(product: ProductEntity, themeColors: VisualThemePalette) {
    Card(
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (product.imageUrl.isNotEmpty()) {
                SmartAsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.DarkGray, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.LightGray)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Text(product.description, color = Color.LightGray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${product.price.toInt()} ${product.currency}", fontWeight = FontWeight.Bold, color = themeColors.accent, fontSize = 12.sp)
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
