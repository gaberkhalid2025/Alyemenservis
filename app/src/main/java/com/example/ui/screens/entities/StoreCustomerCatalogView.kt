package com.example.ui.screens.entities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductEntity
import com.example.data.StoreEntity
import com.example.ui.AppScreens
import com.example.ui.MainViewModel
import com.example.ui.categories.AppCategoryTaxonomy
import com.example.ui.*
import com.example.ui.components.SmartAsyncImage
import com.example.utils.VisualThemePalette
import java.util.Locale

/**
 * 🛍️ شاشة كتالوج ومنيو وتفاصيل المتجر / المطعم للزبائن (Customer View)
 * صممت بأعلى معايير متاجر التجارة الإلكترونية العالمية
 */
@Composable
fun StoreCustomerCatalogView(
    store: StoreEntity,
    products: List<ProductEntity>,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val isFav = remember(favoriteIds, store.id) { favoriteIds.contains(store.id) }

    // المنتجات الخاصة بهذا المتجر
    val storeProducts = remember(products, store.id) {
        products.filter { it.storeId == store.id && !it.isDeleted }
    }

    // استخراج التصنيفات الفرعية المتوفرة في منتجات المتجر
    val productCategories = remember(storeProducts) {
        val list = mutableListOf("الكل")
        val availableCats = storeProducts.mapNotNull { it.category.takeIf { c -> c.isNotBlank() } }.distinct()
        list.addAll(availableCats)
        if (list.size == 1) {
            // إضافة أقسام فرعية افتراضية حسب نوع النشاط
            if (store.sectionId == "restaurants") {
                list.addAll(listOf("وجبات رئيسية", "مقبلات", "مشروبات", "حلويات"))
            } else {
                list.addAll(listOf("الأكثر مبيعاً", "عروض وتخفيضات", "جديد المتجر"))
            }
        }
        list
    }

    var selectedSubCategory by remember { mutableStateOf("الكل") }

    val filteredProducts = remember(storeProducts, selectedSubCategory) {
        if (selectedSubCategory == "الكل") {
            storeProducts
        } else {
            storeProducts.filter {
                it.category.contains(selectedSubCategory, ignoreCase = true) ||
                        it.name.contains(selectedSubCategory, ignoreCase = true)
            }
        }
    }

    // فحص توفر عروض ترويجية
    val hasOffers = remember(storeProducts, store.specialOffersJson) {
        store.specialOffersJson.isNotBlank() || storeProducts.any { it.isOffer || it.discountPercent > 0 }
    }

    // نافذة إتمام الطلب السريع
    var selectedProductForOrder by remember { mutableStateOf<ProductEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFBFBFB)) // خلفية كريمية فاتحة ومريحة
    ) {
        // 1. قسم الواجهة الاحترافي (Hero Banner)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
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
                                listOf(Color(0xFF0F2B48), Color(0xFF1E3A5F), Color(0xFF2563EB))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = store.name,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // تدرج داكن لحماية أزرار التحكم
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f)
                            )
                        )
                    )
            )

            // أزرار التحكم العلوية: الرجوع والمفضلة والمشاركة
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // زر العودة العائم
                Surface(
                    onClick = onBackClick,
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // زر المفضلة (Heart)
                    Surface(
                        onClick = { viewModel.toggleFavorite(store.id) },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.size(38.dp)
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

                    // زر مشاركة رابط المحل
                    Surface(
                        onClick = {
                            val shareText = "تفضل بزيارة متجر ${store.name} على منصة دليل خدمات اليمن 🇾🇪\nالعنوان: ${store.localNeighborhood}\nالهاتف: ${store.phone}"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "مشاركة المحل"))
                        },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.size(38.dp)
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

        // 2. صندوق البيانات الموثوقة (Info Card)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-16).dp)
                .padding(horizontal = 14.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                color = Color(0xFF0F172A),
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
                        }
                        Text(
                            text = store.description.ifBlank { "منشأة معتمدة لتقديم أرقى المنتجات والخدمات" },
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // التقييم الإجمالي
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.6.dp, Color(0xFFF59E0B))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f", store.rating),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "(${store.numReviews})",
                                fontSize = 9.5.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // العنوان الدقيق مع زر "افتح على خريطة Vela Map 🧭"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val addressText = store.localNeighborhood.ifBlank { store.cityId.ifBlank { "اليمن" } }
                    Text(
                        text = "📍 $addressText",
                        fontSize = 11.5.sp,
                        color = Color(0xFF475569),
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
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(0.6.dp, Color(0xFF3B82F6))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "افتح على الخريطة 🧭",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8)
                            )
                        }
                    }
                }
            }
        }

        // 3. شريط العروض والتخفيضات (Promotions Strip)
        if (hasOffers) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFFBEB)
                ),
                border = BorderStroke(1.dp, Color(0xFFFCD34D))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = "عرض ترويجي",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔥 عروض وخصومات حصرية متوفرة الآن!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "اطلب الآن واستمتع بأفضل الأسعار المعتمدة مع سرعة التوصيل",
                            fontSize = 10.sp,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 4. شريط الأقسام الفرعية (Horizontal Category Chips)
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
                    color = if (isSelected) Color(0xFF0F2B48) else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF0F2B48) else Color(0xFFCBD5E1)
                    ),
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Text(
                        text = catName,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. كتالوج المنتجات والمنيو (The Two-Column Grid)
        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(42.dp)
                    )
                    Text(
                        text = "لا توجد أصناف مدرجة في هذا القسم حالياً",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            // شبكة منتجات من عمودين باستخدام أزواج مكدسة
            val productPairs = filteredProducts.chunked(2)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                productPairs.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { prod ->
                            Box(modifier = Modifier.weight(1f)) {
                                ProductItemCardTwoColumn(
                                    product = prod,
                                    storePhone = store.phone,
                                    storeName = store.name,
                                    onAddToCart = { selectedProductForOrder = prod }
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // نافذة إتمام الطلب / المراسلة بالواتساب
    val orderProduct = selectedProductForOrder
    if (orderProduct != null) {
        QuickProductOrderModal(
            product = orderProduct,
            store = store,
            viewModel = viewModel,
            onDismiss = { selectedProductForOrder = null }
        )
    }
}

/**
 * بطاقة المنتج المقرمشة في شبكة العمودين بنسبة (1:1)
 */
@Composable
fun ProductItemCardTwoColumn(
    product: ProductEntity,
    storePhone: String,
    storeName: String,
    onAddToCart: () -> Unit
) {
    val context = LocalContext.current
    val hasDiscount = product.isOffer || product.discountPercent > 0 || (product.oldPrice > product.price && product.price > 0)
    val discountPercent = if (product.discountPercent > 0) {
        product.discountPercent
    } else if (product.oldPrice > product.price && product.price > 0) {
        (((product.oldPrice - product.price) / product.oldPrice) * 100).toInt()
    } else 0

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddToCart() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. صورة السلعة بنسبة 1:1 مربعة واضحة ومقرمشة
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFFF8FAFC))
            ) {
                if (product.imageUrl.isNotBlank()) {
                    SmartAsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // شارة التخفيض (إن وجدت) مثل: -20%
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

            // 2. تفاصيل السلعة والسعر وزر الإضافة (+)
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
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val specText = product.description.ifBlank { "سلعة متوفرة بجودة عالية" }
                Text(
                    text = specText,
                    fontSize = 10.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // السعر البارز بالريال اليمني
                    Column {
                        if (hasDiscount && product.oldPrice > product.price) {
                            Text(
                                text = "${product.oldPrice.toInt()} ر.ي",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                        Text(
                            text = "${product.price.toInt()} ريال",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF065F46) // أخضر زمردي مميز
                        )
                    }

                    // زر الإضافة السريع (+): دائري أخضر بارز
                    Surface(
                        onClick = onAddToCart,
                        shape = CircleShape,
                        color = Color(0xFF10B981),
                        shadowElevation = 3.dp,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "إضافة للطلب",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * نافذة حوارية سريعة لطلب السلعة أو مراسلة المحل بها عبر الواتساب
 */
@Composable
fun QuickProductOrderModal(
    product: ProductEntity,
    store: StoreEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var customerPhoneInput by remember { mutableStateOf("") }
    var customerNameInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    var quantity by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                    color = Color(0xFF065F46)
                )

                // اختيار الكمية
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("الكمية:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    IconButton(
                        onClick = { if (quantity > 1) quantity-- },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("$quantity", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = { quantity++ },
                        modifier = Modifier.size(32.dp)
                    ) {
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
                // إرسال عبر الواتساب مباشرة
                Button(
                    onClick = {
                        val cleanPhone = store.phone.replace("+", "").replace(" ", "")
                        val message = "مرحباً ${store.name}، أرغب في طلب:\n" +
                                "• السلعة: ${product.name}\n" +
                                "• الكمية: $quantity\n" +
                                "• الإجمالي: ${(product.price * quantity).toInt()} ريال يمني\n" +
                                (if (customerNameInput.isNotBlank()) "• العميل: $customerNameInput\n" else "") +
                                (if (notesInput.isNotBlank()) "• ملاحظات: $notesInput\n" else "") +
                                "\nتم الإرسال عبر دليل خدمات اليمن 🇾🇪"
                        val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanPhone?text=${Uri.encode(message)}"))
                        try {
                            context.startActivity(waIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "تعذر فتح تطبيق الواتساب", Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("واتساب 💬", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                // تسجيل الطلب بالمنصة
                Button(
                    onClick = {
                        if (customerPhoneInput.isBlank()) {
                            Toast.makeText(context, "يرجى إدخال رقم هاتفك أولاً", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.triggerNotification("🛒 تم إرسال طلب ${product.name} إلى ${store.name} بنجاح!")
                            Toast.makeText(context, "تم تسجيل طلبك بنجاح ✅", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2B48))
                ) {
                    Text("تأكيد الطلب 🛒", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
