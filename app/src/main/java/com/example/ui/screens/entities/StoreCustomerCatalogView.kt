package com.example.ui.screens.entities

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocalAppCacheManager
import com.example.data.ProductEntity
import com.example.data.SpecialOfferEntity
import com.example.data.StoreEntity
import com.example.data.repositories.DashboardExtensionsRepositoryImpl
import com.example.data.repositories.MedicalRepository
import com.example.data.repositories.ProductsRepositoryImpl
import com.example.domain.entities.DoctorItem
import com.example.domain.entities.ProductItemEntity
import com.example.ui.MainViewModel
import com.example.ui.screens.register.GuestRegistrationDialog
import com.example.utils.VisualThemePalette
import com.google.firebase.firestore.FirebaseFirestore

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
        GuestRegistrationDialog(
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
                StoreFloatingCartBar(
                    cartCount = cartCount,
                    cartTotal = cartTotal,
                    themeColors = themeColors,
                    onClearCart = { cartQuantities.clear() },
                    onCheckoutClick = { showCartCheckoutDialog = true }
                )
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
                StoreHeroBanner(
                    store = store,
                    isMedical = isMedical,
                    isRestaurant = isRestaurant,
                    isFav = isFav,
                    viewModel = viewModel,
                    onBackClick = onBackClick
                )
            }

            // 2. Store Info + Quick Communication Bar
            item {
                StoreInfoAndActionsCard(
                    store = store,
                    isMedical = isMedical,
                    isRestaurant = isRestaurant,
                    isMall = isMall,
                    storeReviews = storeReviews,
                    currentUserId = currentUserId,
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onOpenRatingDialog = { showRatingDialog = true },
                    onRequireGuestRegistration = { showGuestDialog = true }
                )
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
                            Text("👁️ أنت الآن في وضع معاينة العميل", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            Text("العودة لوضع الإدارة ↩", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
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

            if (isMall) {
                item {
                    MallCatalogDirectorySection(store = store, themeColors = themeColors)
                }
            }

            // 3. Promotions / Active Offers
            if (hasOffers) {
                item { StoreOffersSection(activeOffers = activeOffers) }
            }

            // 4. Doctors List for Medical Centers
            if (isMedical && doctors.isNotEmpty()) {
                item {
                    StoreDoctorsListSection(
                        store = store,
                        doctors = doctors,
                        viewModel = viewModel,
                        themeColors = themeColors
                    )
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
                StoreCustomerReviewsSection(
                    storeReviews = storeReviews,
                    themeColors = themeColors,
                    onAddReviewClick = { showRatingDialog = true }
                )
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
        StoreAddRatingDialog(
            store = store,
            isMedical = isMedical,
            isRestaurant = isRestaurant,
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { showRatingDialog = false }
        )
    }
}
