package com.example.ui.screens.entities

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookingEntity
import com.example.data.ProductEntity
import com.example.data.RatingEntity
import com.example.data.SpecialOfferEntity
import com.example.data.StoreEntity
import com.example.data.repositories.DashboardExtensionsRepositoryImpl
import com.example.data.repositories.MedicalRepository
import com.example.data.repositories.ProductsRepositoryImpl
import com.example.domain.entities.DoctorItem
import com.example.domain.entities.ProductItemEntity
import com.example.data.LocalAppCacheManager
import com.example.ui.AppScreens
import com.example.ui.MainViewModel
import com.example.ui.components.SmartAsyncImage
import com.example.ui.navigateToScreen
import com.example.ui.toggleFavorite
import com.example.ui.helpers.AppPreferenceHelper
import com.example.utils.BookingStatus
import com.example.utils.EntityIdGenerator
import com.example.utils.VisualThemePalette
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

@Composable
fun StoreCustomerCatalogView(
    store: StoreEntity,
    products: List<ProductEntity>,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit,
    isAdmin: Boolean = false,
    isOwnerPreview: Boolean = false,
    onExitOwnerPreview: (() -> Unit)? = null
) {
    BackHandler {
        if (isOwnerPreview && onExitOwnerPreview != null) {
            onExitOwnerPreview()
        } else {
            onBackClick()
        }
    }

    val context = LocalContext.current
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val isFav = remember(favoriteIds, store.id) { favoriteIds.contains(store.id) }
    val allRatings by viewModel.ratings.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()

    val isMedical = store.sectionId.equals("medical", ignoreCase = true) ||
        store.medicalLicenseNo.isNotBlank()
    val isRestaurant = store.sectionId.equals("restaurants", ignoreCase = true)
    val isMall = !isMedical && !isRestaurant && (
        store.sectionId.contains("mall", ignoreCase = true) ||
            store.name.contains("مول") ||
            store.name.contains("مركز تجاري") ||
            store.name.contains("مجمع تجاري")
        )

    val ownerProducts by remember(store.id, context) {
        ProductsRepositoryImpl(context.applicationContext).getOwnerProducts(store.id)
    }.collectAsState(initial = emptyList<ProductItemEntity>())

    val specialOffers by remember(store.id) {
        DashboardExtensionsRepositoryImpl().getSpecialOffers(store.id)
    }.collectAsState(initial = emptyList<SpecialOfferEntity>())

    val doctors by remember(store.id, isMedical, context) {
        if (isMedical) {
            MedicalRepository(
                FirebaseFirestore.getInstance(),
                LocalAppCacheManager(context.applicationContext)
            ).getDoctors(store.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList<DoctorItem>())

    val storeProducts = remember(products, ownerProducts, store.id) {
        val fromGlobal = products.filter { it.storeId == store.id && !it.isDeleted }
        val existingIds = fromGlobal.map { it.id }.toSet()
        val extra = ownerProducts.filter { it.id !in existingIds }.map { p ->
            ProductEntity(
                id = p.id,
                storeId = p.ownerId,
                name = p.title,
                price = p.priceYer,
                oldPrice = p.oldPriceYer,
                description = p.description,
                imageUrl = p.imageUrl,
                category = p.category,
                isAvailable = p.isAvailable,
                isOffer = p.isOffer || p.discountPercent > 0,
                discountPercent = p.discountPercent
            )
        }
        fromGlobal + extra
    }

    val storeReviews = remember(allRatings, store.id) {
        allRatings.filter { it.targetId == store.id }
    }

    val productCategories = remember(storeProducts, isRestaurant, isMedical) {
        val list = mutableListOf("الكل")
        val availableCats = storeProducts.mapNotNull { it.category.takeIf { c -> c.isNotBlank() } }.distinct()
        list.addAll(availableCats)
        if (list.size == 1) {
            when {
                isRestaurant -> list.addAll(listOf("وجبات رئيسية", "مقبلات", "مشروبات", "حلويات"))
                isMedical -> list.addAll(listOf("استشارات", "فحوصات مخبرية", "أشعة", "خدمات طبية"))
                else -> list.addAll(listOf("الأكثر مبيعاً", "عروض وتخفيضات", "جديد المتجر"))
            }
        }
        list
    }

    var selectedSubCategory by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredProducts = remember(storeProducts, selectedSubCategory, searchQuery) {
        storeProducts.filter { prod ->
            val catMatch = selectedSubCategory == "الكل" ||
                prod.category.contains(selectedSubCategory, ignoreCase = true) ||
                prod.name.contains(selectedSubCategory, ignoreCase = true)
            val queryMatch = searchQuery.isBlank() ||
                prod.name.contains(searchQuery, ignoreCase = true) ||
                prod.description.contains(searchQuery, ignoreCase = true)
            catMatch && queryMatch
        }
    }

    val activeOffers = remember(specialOffers) { specialOffers.filter { it.isEnabled } }
    val hasOffers = remember(storeProducts, store.specialOffersJson, activeOffers) {
        activeOffers.isNotEmpty() || store.specialOffersJson.isNotBlank() || storeProducts.any { it.isOffer || it.discountPercent > 0 }
    }

    // Shopping cart (productId -> quantity)
    val cartQuantities = remember { mutableStateMapOf<String, Int>() }
    val cartCount by remember { derivedStateOf { cartQuantities.values.sum() } }
    val cartTotal by remember(storeProducts) {
        derivedStateOf {
            cartQuantities.entries.sumOf { (pid, qty) ->
                val p = storeProducts.find { it.id == pid }
                (p?.price ?: 0.0) * qty
            }
        }
    }

    var selectedProductForOrder by remember { mutableStateOf<ProductEntity?>(null) }
    var showCartCheckoutDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showGuestDialog by remember { mutableStateOf(false) }

    if (showGuestDialog) {
        com.example.ui.screens.register.GuestRegistrationDialog(
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { showGuestDialog = false },
            onRegisterCompleted = { _, _, _, _ -> showGuestDialog = false }
        )
    }

    Scaffold(
        containerColor = themeColors.background,
        bottomBar = {
            if (cartCount > 0) {
                Surface(
                    color = themeColors.surface,
                    shadowElevation = 10.dp,
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🛒 السلة ($cartCount ${if (cartCount == 1) "صنف" else "أصناف"})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.textPrimary
                            )
                            Text(
                                text = "الإجمالي: ${cartTotal.toInt()} ريال",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF10B981)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { cartQuantities.clear() }) {
                                Text("تفريغ", color = Color(0xFFEF4444), fontSize = 11.sp)
                            }
                            Button(
                                onClick = { showCartCheckoutDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("إتمام الطلب ✅", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(Color(0xFF0F172A))
                ) {
                    val bannerUrl = store.coverImage.ifBlank { store.logoImage }
                    if (bannerUrl.isNotBlank()) {
                        SmartAsyncImage(
                            model = bannerUrl,
                            contentDescription = store.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        when {
                                            isMedical -> listOf(Color(0xFF082F49), Color(0xFF0F172A), Color(0xFF065F46))
                                            isRestaurant -> listOf(Color(0xFF451A03), Color(0xFF1E293B), Color(0xFF9A3412))
                                            else -> listOf(Color(0xFF0F2B48), Color(0xFF1E3A5F), Color(0xFF2563EB))
                                        }
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = store.name,
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.55f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.45f)
                                    )
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onBackClick,
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                onClick = { viewModel.toggleFavorite(store.id) },
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "المفضلة",
                                        tint = if (isFav) Color(0xFFEF4444) else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    val shareText = "تفضل بزيارة ${store.name} على منصة دليل خدمات اليمن 🇾🇪\nالعنوان: ${store.localNeighborhood}\nالهاتف: ${store.phone}"
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "مشاركة"))
                                },
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "مشاركة",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Store Info + Quick Communication Bar
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(1.dp, themeColors.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = store.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = themeColors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (store.isVerified || store.isActive) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "موثق رسميًا",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    if (isMall) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(0.6.dp, Color(0xFF8B5CF6))
                                        ) {
                                            Text(
                                                text = "🏬 مول / مركز تجاري",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFA78BFA),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = store.description.ifBlank {
                                        when {
                                            isMedical -> "مركز طبي / عيادة معتمدة"
                                            isRestaurant -> "مطعم وكافيه معتمد"
                                            else -> "منشأة معتمدة لتقديم أرقى المنتجات والخدمات"
                                        }
                                    },
                                    fontSize = 11.5.sp,
                                    color = themeColors.textSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.6.dp, Color(0xFFF59E0B)),
                                modifier = Modifier.clickable { showRatingDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(13.dp))
                                    Text(String.format(Locale.US, "%.1f", store.rating), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF92400E))
                                    Text("(${maxOf(store.numReviews, storeReviews.size)})", fontSize = 9.5.sp, color = Color(0xFFB45309))
                                }
                            }
                        }

                        HorizontalDivider(color = themeColors.border)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val addressText = store.localNeighborhood.ifBlank { store.cityId.ifBlank { "اليمن" } }
                            Text(
                                text = "📍 $addressText",
                                fontSize = 11.5.sp,
                                color = themeColors.textSecondary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Surface(
                                onClick = {
                                    viewModel.selectedStore = store
                                    viewModel.navigateToScreen(AppScreens.MAP_VIEW)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1D4ED8).copy(alpha = 0.15f),
                                border = BorderStroke(0.6.dp, Color(0xFF3B82F6))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(13.dp))
                                    Text("الخريطة 🧭", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                                }
                            }
                        }

                        // Action Buttons Row (Chat + Call + Book Appointment for Medical)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (currentUserId.isBlank() || currentUserId == "guest") {
                                        showGuestDialog = true
                                    } else {
                                        viewModel.openDirectChat(
                                            targetUserId = store.id,
                                            targetUserName = store.name,
                                            targetUserPhoto = store.logoImage,
                                            relatedEntityId = store.id,
                                            relatedEntityType = when {
                                                isMedical -> "MEDICAL"
                                                isRestaurant -> "RESTAURANT"
                                                else -> "STORE"
                                            }
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Text("محادثة 💬", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            if (store.phone.isNotBlank()) {
                                Button(
                                    onClick = {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${store.phone}")))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Text("اتصال 📞", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            if (isMedical) {
                                Button(
                                    onClick = {
                                        viewModel.selectedStore = store
                                        viewModel.navigateToScreen(AppScreens.CREATE_BOOKING)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.2f).height(40.dp)
                                ) {
                                    Text("حجز موعد 🩺", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                        // Action Buttons Row (Chat + Call + Book Appointment for Medical)
                        // end of card content
                    }
                }
            }

            if (isOwnerPreview && onExitOwnerPreview != null) {
                item {
                    Surface(
                        color = themeColors.accent.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, themeColors.accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .clickable { onExitOwnerPreview() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "👁️ أنت الآن في وضع معاينة العميل",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.accent
                            )
                            Text(
                                text = "العودة لوضع الإدارة ↩",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            if (isAdmin && !isOwnerPreview) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 10.dp)) {
                        ProfileOwnerAdminControlBar(
                            entityId = store.id,
                            entityType = when {
                                isMedical -> ProfileEntityType.MEDICAL
                                isRestaurant -> ProfileEntityType.RESTAURANT
                                else -> ProfileEntityType.STORE
                            },
                            provider = null,
                            store = store,
                            property = null,
                            job = null,
                            isOwner = false,
                            isAdmin = true,
                            viewModel = viewModel,
                            themeColors = themeColors
                        )
                    }
                }
            }

            // 3. Promotions / Active Offers
            if (hasOffers) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (activeOffers.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                                border = BorderStroke(1.dp, Color(0xFFFCD34D))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                    Text("🔥 عروض وخصومات حصرية متوفرة الآن!", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                }
                            }
                        } else {
                            activeOffers.forEach { offer ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                                    border = BorderStroke(1.dp, Color(0xFFFCD34D))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "🏷️ ${offer.title.ifBlank { "عرض خاص" }}${if (offer.discountPercent > 0) " - خصم ${offer.discountPercent}%" else ""}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                        if (offer.description.isNotBlank()) {
                                            Text(offer.description, fontSize = 10.5.sp, color = Color(0xFFB45309))
                                        }
                                        if (offer.couponCode.isNotBlank()) {
                                            Text("الكود: ${offer.couponCode}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Doctors List for Medical Centers
            if (isMedical && doctors.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("👨‍⚕️ أطباء المركز والعيادات (${doctors.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                        doctors.forEach { doc ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, themeColors.border)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("د. ${doc.name}", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
                                        if (doc.specialty.isNotBlank()) {
                                            Text("التخصص: ${doc.specialty}", fontSize = 11.5.sp, color = Color(0xFF06B6D4))
                                        }
                                        if (doc.workingHours.isNotBlank()) {
                                            Text("الدوام: ${doc.workingHours}", fontSize = 11.sp, color = themeColors.textSecondary)
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.selectedStore = store
                                            viewModel.navigateToScreen(AppScreens.CREATE_BOOKING)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("احجز 📅", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Product Search & Category Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث داخل القائمة أو المنتجات...", fontSize = 12.sp, color = themeColors.textSecondary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = themeColors.textSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(productCategories) { catName ->
                            val isSelected = selectedSubCategory == catName
                            Surface(
                                onClick = { selectedSubCategory = catName },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) themeColors.accent else themeColors.surface,
                                border = BorderStroke(1.dp, if (isSelected) themeColors.accent else themeColors.border)
                            ) {
                                Text(
                                    text = catName,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else themeColors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 6. Products Two-Column Grid
            if (filteredProducts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = themeColors.textSecondary, modifier = Modifier.size(40.dp))
                            Text(
                                text = "لا توجد أصناف مدرجة في هذا القسم حالياً",
                                fontSize = 13.sp,
                                color = themeColors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                val productPairs = filteredProducts.chunked(2)
                items(productPairs) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { prod ->
                            val qtyInCart = cartQuantities[prod.id] ?: 0
                            Box(modifier = Modifier.weight(1f)) {
                                ProductItemCardTwoColumn(
                                    product = prod,
                                    themeColors = themeColors,
                                    quantityInCart = qtyInCart,
                                    onCardClick = { selectedProductForOrder = prod },
                                    onAddToCart = { cartQuantities[prod.id] = qtyInCart + 1 },
                                    onDecrementCart = {
                                        if (qtyInCart <= 1) cartQuantities.remove(prod.id)
                                        else cartQuantities[prod.id] = qtyInCart - 1
                                    }
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // 7. Customer Reviews & Add Review Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("التقييمات وآراء العملاء ⭐ (${storeReviews.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                        Button(
                            onClick = { showRatingDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("أضف تقييمك ⭐", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (storeReviews.isEmpty()) {
                        EmptyStateBox("لا توجد تقييمات مسجلة بعد. كن أول من يشارك رأيه!", themeColors)
                    } else {
                        storeReviews.forEach { review ->
                            ProfileReviewCard(review = review, themeColors = themeColors)
                        }
                    }
                }
            }
        }
    }

    // Single product modal
    val orderProduct = selectedProductForOrder
    if (orderProduct != null) {
        QuickProductOrderModal(
            product = orderProduct,
            store = store,
            viewModel = viewModel,
            onDismiss = { selectedProductForOrder = null }
        )
    }

    // Multi-item cart checkout modal
    if (showCartCheckoutDialog && cartCount > 0) {
        CartCheckoutModal(
            cartItems = cartQuantities.entries.mapNotNull { (pid, qty) ->
                storeProducts.find { it.id == pid }?.let { it to qty }
            },
            store = store,
            viewModel = viewModel,
            onDismiss = { showCartCheckoutDialog = false },
            onOrderPlaced = {
                cartQuantities.clear()
                showCartCheckoutDialog = false
            }
        )
    }

    // Add rating dialog
    if (showRatingDialog) {
        var ratingStars by remember { mutableIntStateOf(5) }
        var reviewComment by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRatingDialog = false },
            containerColor = themeColors.surface,
            title = { Text("تقييم ${store.name} ⭐", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
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
                        label = { Text("اكتب تجربتك (اختياري)", color = themeColors.textSecondary) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ratingObj = RatingEntity(
                            id = EntityIdGenerator.generate("REV"),
                            targetId = store.id,
                            targetType = if (isMedical) "MEDICAL" else if (isRestaurant) "RESTAURANT" else "STORE",
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
}

@Composable
fun ProductItemCardTwoColumn(
    product: ProductEntity,
    themeColors: VisualThemePalette,
    quantityInCart: Int = 0,
    onCardClick: () -> Unit,
    onAddToCart: () -> Unit,
    onDecrementCart: () -> Unit = {}
) {
    val hasDiscount = product.isOffer || product.discountPercent > 0 || (product.oldPrice > product.price && product.price > 0)
    val discountPercent = if (product.discountPercent > 0) {
        product.discountPercent
    } else if (product.oldPrice > product.price && product.price > 0) {
        (((product.oldPrice - product.price) / product.oldPrice) * 100).toInt()
    } else 0

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(0.8.dp, themeColors.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(themeColors.background.copy(alpha = 0.5f))
            ) {
                if (product.imageUrl.isNotBlank()) {
                    SmartAsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = themeColors.textSecondary, modifier = Modifier.size(36.dp))
                    }
                }

                if (hasDiscount && discountPercent > 0) {
                    Surface(
                        color = Color(0xFFEF4444),
                        shape = RoundedCornerShape(bottomStart = 8.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "-$discountPercent%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.name,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val specText = product.description.ifBlank { "سلعة متوفرة بجودة عالية" }
                Text(
                    text = specText,
                    fontSize = 10.sp,
                    color = themeColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (hasDiscount && product.oldPrice > product.price) {
                            Text(
                                text = "${product.oldPrice.toInt()} ر.ي",
                                fontSize = 9.sp,
                                color = themeColors.textSecondary,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                        Text(
                            text = "${product.price.toInt()} ريال",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10B981)
                        )
                    }

                    if (quantityInCart == 0) {
                        Surface(
                            onClick = onAddToCart,
                            shape = CircleShape,
                            color = Color(0xFF10B981),
                            shadowElevation = 3.dp,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = "إضافة للسلة", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                onClick = onDecrementCart,
                                shape = CircleShape,
                                color = Color(0xFFEF4444),
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                            Text("$quantityInCart", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = themeColors.textPrimary)
                            Surface(
                                onClick = onAddToCart,
                                shape = CircleShape,
                                color = Color(0xFF10B981),
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatYemenWhatsAppPhone(rawPhone: String): String {
    val clean = AppPreferenceHelper.normalizePhoneNumber(rawPhone)
    return if (clean.startsWith("967")) clean else "967$clean"
}

@Composable
fun CartCheckoutModal(
    cartItems: List<Pair<ProductEntity, Int>>,
    store: StoreEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onOrderPlaced: () -> Unit
) {
    val context = LocalContext.current
    val savedPhone by viewModel.currentUserPhone.collectAsState()
    val savedName by viewModel.currentUserName.collectAsState()
    var customerPhoneInput by remember(savedPhone) { mutableStateOf(savedPhone) }
    var customerNameInput by remember(savedName) { mutableStateOf(savedName) }
    var notesInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val total = cartItems.sumOf { (p, q) -> p.price * q }
    val summaryText = cartItems.joinToString(" + ") { (p, q) -> "${p.name} × $q" }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text("إتمام طلب السلة (${cartItems.sumOf { it.second }} أصناف) 🛒", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(summaryText, fontSize = 11.5.sp)
                Text("الإجمالي: ${total.toInt()} ريال يمني", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                OutlinedTextField(value = customerNameInput, onValueChange = { customerNameInput = it }, label = { Text("اسمك الكريم") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = customerPhoneInput,
                    onValueChange = { customerPhoneInput = it },
                    label = { Text("رقم هاتفك للتواصل") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = notesInput, onValueChange = { notesInput = it }, label = { Text("العنوان والملاحظات") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val waNum = formatYemenWhatsAppPhone(store.phone)
                        if (waNum.isNotBlank()) {
                            val itemsLines = cartItems.joinToString("\n") { (p, q) -> "• ${p.name} × $q (${(p.price * q).toInt()} ر.ي)" }
                            val msg = "مرحباً ${store.name}، أرغب في طلب:\n$itemsLines\nالإجمالي: ${total.toInt()} ريال يمني\n${if (customerNameInput.isNotBlank()) "العميل: $customerNameInput\n" else ""}${if (notesInput.isNotBlank()) "العنوان/ملاحظات: $notesInput" else ""}"
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waNum?text=${Uri.encode(msg)}")))
                            } catch (e: Exception) {
                                Toast.makeText(context, "تعذر فتح واتساب", Toast.LENGTH_SHORT).show()
                            }
                        }
                        onOrderPlaced()
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("واتساب 💬", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Button(
                    onClick = {
                        if (customerPhoneInput.isBlank()) {
                            Toast.makeText(context, "يرجى إدخال رقم هاتفك أولاً", Toast.LENGTH_SHORT).show()
                        } else if (!isSubmitting) {
                            isSubmitting = true
                            val booking = BookingEntity(
                                customerName = customerNameInput.trim().ifBlank { "عميل المتجر" },
                                customerPhone = customerPhoneInput.trim(),
                                customerArea = notesInput.trim().ifBlank { store.localNeighborhood.ifBlank { store.cityId } },
                                serviceType = summaryText,
                                serviceDetails = notesInput.trim(),
                                providerId = store.id,
                                providerName = store.name,
                                providerPhone = store.phone,
                                category = store.categoryId.ifBlank { "STORE" },
                                totalAmount = total,
                                price = total,
                                status = BookingStatus.PENDING.code
                            )
                            viewModel.bookingViewModel.createBooking(booking) { success ->
                                isSubmitting = false
                                if (success) {
                                    Toast.makeText(context, "تم تسجيل طلبك وإرساله للمتجر بنجاح ✅", Toast.LENGTH_SHORT).show()
                                    onOrderPlaced()
                                } else {
                                    Toast.makeText(context, "تعذر إرسال الطلب، يرجى التحقق من الاتصال", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2B48))
                ) {
                    Text(if (isSubmitting) "جاري الإرسال..." else "تأكيد الطلب ✅", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!isSubmitting) onDismiss() }, enabled = !isSubmitting) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun QuickProductOrderModal(
    product: ProductEntity,
    store: StoreEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val savedPhone by viewModel.currentUserPhone.collectAsState()
    val savedName by viewModel.currentUserName.collectAsState()
    var customerPhoneInput by remember(savedPhone) { mutableStateOf(savedPhone) }
    var customerNameInput by remember(savedName) { mutableStateOf(savedName) }
    var notesInput by remember { mutableStateOf("") }
    var quantity by remember { mutableIntStateOf(1) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF10B981))
                Text("طلب سلعة: ${product.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "السعر الإجمالي: ${(product.price * quantity).toInt()} ريال يمني",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("الكمية:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    IconButton(onClick = { if (quantity > 1) quantity-- }, modifier = Modifier.size(48.dp)) {
                        Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("$quantity", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { quantity++ }, modifier = Modifier.size(48.dp)) {
                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = customerNameInput,
                    onValueChange = { customerNameInput = it },
                    label = { Text("اسمك الكريم") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customerPhoneInput,
                    onValueChange = { customerPhoneInput = it },
                    label = { Text("رقم هاتفك للتواصل") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("ملاحظات إضافية أو العنوان") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val waNum = formatYemenWhatsAppPhone(store.phone)
                        val message = "مرحباً ${store.name}، أرغب في طلب:\n" +
                                "• السلعة: ${product.name}\n" +
                                "• الكمية: $quantity\n" +
                                "• الإجمالي: ${(product.price * quantity).toInt()} ريال يمني\n" +
                                (if (customerNameInput.isNotBlank()) "• العميل: $customerNameInput\n" else "") +
                                (if (notesInput.isNotBlank()) "• ملاحظات: $notesInput\n" else "") +
                                "\nتم الإرسال عبر دليل خدمات اليمن 🇾🇪"
                        val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waNum?text=${Uri.encode(message)}"))
                        try {
                            context.startActivity(waIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "تعذر فتح تطبيق الواتساب", Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("واتساب 💬", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        if (customerPhoneInput.isBlank()) {
                            Toast.makeText(context, "يرجى إدخال رقم هاتفك أولاً", Toast.LENGTH_SHORT).show()
                        } else if (!isSubmitting) {
                            isSubmitting = true
                            val orderTotal = product.price * quantity
                            val booking = BookingEntity(
                                customerName = customerNameInput.trim().ifBlank { "عميل المتجر" },
                                customerPhone = customerPhoneInput.trim(),
                                customerArea = notesInput.trim().ifBlank { store.localNeighborhood.ifBlank { store.cityId } },
                                serviceType = "${product.name} × $quantity",
                                serviceDetails = notesInput.trim(),
                                providerId = store.id,
                                providerName = store.name,
                                providerPhone = store.phone,
                                category = store.categoryId.ifBlank { "STORE" },
                                totalAmount = orderTotal,
                                price = orderTotal,
                                status = BookingStatus.PENDING.code
                            )
                            viewModel.bookingViewModel.createBooking(booking) { success ->
                                isSubmitting = false
                                if (success) {
                                    Toast.makeText(context, "تم تسجيل طلبك وإرساله للمتجر بنجاح ✅", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "تعذر إرسال الطلب، يرجى التحقق من الاتصال", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2B48))
                ) {
                    Text(if (isSubmitting) "جاري الإرسال..." else "تأكيد الطلب 🛒", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!isSubmitting) onDismiss() }, enabled = !isSubmitting) {
                Text("إلغاء")
            }
        }
    )
}
