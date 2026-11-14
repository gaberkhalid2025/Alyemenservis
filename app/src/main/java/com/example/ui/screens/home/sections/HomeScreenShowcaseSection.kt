package com.example.ui.screens.home.sections

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductEntity
import com.example.data.PropertyEntity
import com.example.data.StoreEntity
import com.example.ui.*
import com.example.ui.categories.AppCategoryTaxonomy
import com.example.ui.components.SmartAsyncImage
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.TajawalFontFamily
import com.example.utils.VisualThemePalette
import java.util.Locale

/**
 * 🌟 SECTION 7 — CATEGORY CARDS SHOWCASE (بطاقات الأقسام المخصصة)
 * 1. Restaurant Cards: Hero dish image, price in Yemeni Rial (ر.ي), cuisine chips, delivery estimate (20-35 دقيقة), "اطلب وجبتك / حجز طاولة".
 * 2. Medical Clinic Cards: Specialist photo, consultation fee, appointment booking wizard (Day & Slot picker), location badge.
 * 3. Store Cards: Brand logo, cover banner, product gallery grid, discount coupon chips ("YEMEN20").
 * 4. Real Estate Cards: Property photo slider, price, area in m², rooms, rent/sale badge.
 */
@Composable
fun HomeScreenShowcaseSection(
    stores: List<StoreEntity>,
    properties: List<PropertyEntity>,
    products: List<ProductEntity>,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onStoreClick: (StoreEntity) -> Unit,
    onPropertyClick: (PropertyEntity) -> Unit,
    onChatOpen: (String) -> Unit
) {
    val context = LocalContext.current

    val couponsList by viewModel.coupons.collectAsState()

    // Categorize Stores without fake fallback to unrelated categories
    val restaurantStores = remember(stores) {
        stores.filter {
            it.sectionId.contains("restaurant", ignoreCase = true) ||
                it.categoryId.contains("restaurant", ignoreCase = true) ||
                it.name.contains("مطعم") || it.name.contains("كافيه") || it.name.contains("بوفية") || it.name.contains("شواية")
        }
    }

    val medicalStores = remember(stores) {
        stores.filter {
            it.sectionId.contains("medical", ignoreCase = true) ||
                it.categoryId.contains("medical", ignoreCase = true) ||
                it.name.contains("طبي") || it.name.contains("عيادة") || it.name.contains("مستشفى") || it.name.contains("صيدلية") || it.name.contains("دكتور")
        }
    }

    val retailStores = remember(stores) {
        stores.filter {
            !it.sectionId.contains("restaurant", ignoreCase = true) &&
                !it.sectionId.contains("medical", ignoreCase = true) &&
                !it.name.contains("مطعم") && !it.name.contains("طبي")
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // -------------------------------------------------------------
        // 1. المطاعم والكافيهات (Restaurant Cards Showcase)
        // -------------------------------------------------------------
        if (restaurantStores.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFE11D48), Color(0xFFF97316)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🍔", fontSize = 14.sp)
                    }
                    Text(
                        text = "أشهى المطاعم والكافيهات",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = CairoFontFamily,
                        color = Color.White
                    )
                }
                Text(
                    text = "عرض الكل ←",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF00E5FF),
                    modifier = Modifier.clickable { viewModel.setActiveBrowserTab("المطاعم والكافيهات") }
                )
            }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    restaurantStores.forEach { store ->
                        val restProducts = products.filter { it.storeId == store.id && !it.isDeleted }
                        RestaurantShowcaseCard(
                            store = store,
                            storeProducts = restProducts,
                            onCardClick = { onStoreClick(store) },
                            onOrderClick = {
                                viewModel.selectedStore = store
                                viewModel.navigateTo("STORE_DETAILS")
                            }
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 2. المراكز والعيادات الطبية (Medical Clinic Cards Showcase)
        // -------------------------------------------------------------
        if (medicalStores.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF0D9488), Color(0xFF2DD4BF)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏥", fontSize = 14.sp)
                        }
                        Text(
                            text = "المراكز والعيادات الطبية المعتمدة",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = CairoFontFamily,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "عرض الكل ←",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.clickable { viewModel.setActiveBrowserTab("المراكز الطبية") }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    medicalStores.forEach { clinic ->
                        val clinicServices = products.filter { it.storeId == clinic.id && !it.isDeleted }
                        MedicalClinicShowcaseCard(
                            clinic = clinic,
                            services = clinicServices,
                            onCardClick = { onStoreClick(clinic) },
                            onBookAppointment = {
                                viewModel.selectedStore = clinic
                                viewModel.navigateTo("STORE_DETAILS")
                            }
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 3. المتاجر والمحلات التجارية (Store Cards Showcase with Product Grid)
        // -------------------------------------------------------------
        if (retailStores.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF7C3AED)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏪", fontSize = 14.sp)
                        }
                        Text(
                            text = "المتاجر ومراكز التسوق",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = CairoFontFamily,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "عرض الكل ←",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.clickable { viewModel.setActiveBrowserTab("المحلات والمتاجر") }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    retailStores.forEach { store ->
                        val storeProducts = products.filter { it.storeId == store.id && !it.isDeleted }.take(3)
                        val activeCoupon = couponsList.firstOrNull {
                            it.isValidCoupon() && it.storeId == store.id
                        }
                        StoreShowcaseCard(
                            store = store,
                            products = storeProducts,
                            activeCouponCode = activeCoupon?.code,
                            onCardClick = { onStoreClick(store) }
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 4. العقارات والأملاك (Real Estate Cards Showcase)
        // -------------------------------------------------------------
        if (properties.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF059669), Color(0xFF10B981)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏠", fontSize = 14.sp)
                        }
                        Text(
                            text = "العقارات والفرص السكنية",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = CairoFontFamily,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "عرض الكل ←",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.clickable { viewModel.setActiveBrowserTab("العقارات") }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    properties.take(5).forEach { prop ->
                        RealEstateShowcaseCard(
                            property = prop,
                            onCardClick = { onPropertyClick(prop) },
                            onChatClick = { onChatOpen(prop.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 🍔 بطاقة المطعم المخصصة
 */
@Composable
private fun RestaurantShowcaseCard(
    store: StoreEntity,
    storeProducts: List<ProductEntity>,
    onCardClick: () -> Unit,
    onOrderClick: () -> Unit
) {
    val avgPrice = remember(storeProducts) {
        val priced = storeProducts.filter { it.price > 0.0 }
        if (priced.isNotEmpty()) priced.map { it.price }.average() else null
    }
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141A2E)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onCardClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // صورة الطبق / الغلاف مع شارات مميزة
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            ) {
                if (store.coverImage.isNotBlank()) {
                    SmartAsyncImage(
                        model = store.coverImage,
                        contentDescription = store.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(Color(0xFFE11D48), Color(0xFFF97316)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🍲", fontSize = 38.sp)
                    }
                }

                // تدرج داكن
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xFF141A2E).copy(alpha = 0.85f))
                            )
                        )
                )

                // شارة الموقع أو ساعات العمل الحقيقية
                val hoursOrArea = store.workingHours.ifBlank { store.localNeighborhood }
                if (hoursOrArea.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(11.dp))
                            Text(hoursOrArea, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                        }
                    }
                }

                // شارة التقييم الحقيقية
                if (store.numReviews > 0 && store.rating > 0f) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "⭐ ${String.format(Locale.US, "%.1f", store.rating)}",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFB45309),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // التفاصيل
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = store.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = CairoFontFamily,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (store.localNeighborhood.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.06f)
                        ) {
                            Text(
                                text = store.localNeighborhood,
                                fontSize = 9.sp,
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (store.isActive) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (store.isActive) "مفتوح للطلب" else "مغلق مؤقتاً",
                            fontSize = 9.sp,
                            color = if (store.isActive) Color(0xFF10B981) else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (avgPrice != null) "متوسط ${String.format(Locale.US, "%,.0f", avgPrice)} ر.ي" else "${storeProducts.size} صنف متاح",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )

                    Button(
                        onClick = onOrderClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("اطلب وجبتك", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * 🏥 بطاقة المركز الطبي المخصصة
 */
@Composable
private fun MedicalClinicShowcaseCard(
    clinic: StoreEntity,
    services: List<ProductEntity>,
    onCardClick: () -> Unit,
    onBookAppointment: () -> Unit
) {
    val minServicePrice = remember(services) {
        services.filter { it.price > 0.0 }.minOfOrNull { it.price }
    }
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141A2E)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onCardClick)
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
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F766E).copy(alpha = 0.3f))
                        .border(1.dp, Color(0xFF2DD4BF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (clinic.logoImage.isNotBlank()) {
                        SmartAsyncImage(
                            model = clinic.logoImage,
                            contentDescription = clinic.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("🩺", fontSize = 22.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (clinic.isActive) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.2f),
                    border = BorderStroke(0.6.dp, if (clinic.isActive) Color(0xFF10B981).copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = if (clinic.isActive) "متاح ✓" else "مغلق",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (clinic.isActive) Color(0xFF10B981) else Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Column {
                Text(
                    text = clinic.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = CairoFontFamily,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = clinic.localNeighborhood.ifBlank { "اليمن" },
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (minServicePrice != null) "تبدأ من" else "الخدمات", fontSize = 8.5.sp, color = Color(0xFF64748B))
                    Text(
                        text = if (minServicePrice != null) "${String.format(Locale.US, "%,.0f", minServicePrice)} ر.ي" else "${services.size} خدمة",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981)
                    )
                }

                Button(
                    onClick = onBookAppointment,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("حجز موعد", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * 🏪 بطاقة المتجر ومراكز التسوق المخصصة
 */
@Composable
private fun StoreShowcaseCard(
    store: StoreEntity,
    products: List<ProductEntity>,
    activeCouponCode: String? = null,
    onCardClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141A2E)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onCardClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            ) {
                if (store.coverImage.isNotBlank()) {
                    SmartAsyncImage(
                        model = store.coverImage,
                        contentDescription = store.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF7C3AED)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🛍️", fontSize = 32.sp)
                    }
                }

                if (!activeCouponCode.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFFEF4444),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "كوبون $activeCouponCode",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
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
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = store.name,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = CairoFontFamily,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "📍 ${store.localNeighborhood.ifBlank { "اليمن" }}",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1
                )

                // Mini product gallery
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(3) { index ->
                        val prod = products.getOrNull(index)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(0.6.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (prod != null && prod.imageUrl.isNotBlank()) {
                                SmartAsyncImage(
                                    model = prod.imageUrl,
                                    contentDescription = prod.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(if (index == 0) "📦" else if (index == 1) "✨" else "🏷️", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 🏠 بطاقة العقارات المخصصة
 */
@Composable
private fun RealEstateShowcaseCard(
    property: PropertyEntity,
    onCardClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141A2E)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onCardClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                if (property.images.isNotEmpty()) {
                    SmartAsyncImage(
                        model = property.images.first(),
                        contentDescription = property.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(Color(0xFF059669), Color(0xFF10B981)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏡", fontSize = 36.sp)
                    }
                }

                // شارة الإيجار أو البيع
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (property.type.contains("بيع")) Color(0xFF2563EB) else Color(0xFF059669),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (property.type.contains("بيع")) "للبيع 🏷️" else "للإيجار 🔑",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = property.title.ifBlank { "عقار مميز في اليمن" },
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = CairoFontFamily,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // مواصفات: المساحة والغرف
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📐 ${property.area} م²", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                    Text("🚪 ${property.rooms} غرف", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%,.0f", property.price)} ${property.currency.ifBlank { "ر.ي" }}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981)
                    )

                    IconButton(
                        onClick = onChatClick,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF0284C7).copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "محادثة", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
