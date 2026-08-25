package com.example.ui.screens.dashboard

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.*
import com.example.data.models.ApplicableType
import com.example.data.models.DiscountType
import com.example.data.models.EntityType
import com.example.data.models.Offer
import com.example.ui.MainViewModel
import com.example.util.ImageUtils
import com.example.utils.VisualThemePalette

/**
 * 🏬 Standalone Dedicated Dashboard for Stores & Commercial Centers (لوحة المتجر والمركز التجاري)
 */
@Composable
fun StoreDashboard(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) }

    val tabsList = listOf(
        Pair("📦", "المنتجات والمخزون"),
        Pair("🛍️", "طلبات الشراء"),
        Pair("🎁", "العروض والخصومات"),
        Pair("💬", "تقييمات المتجر"),
        Pair("⚙️", "إعدادات المتجر"),
        Pair("📊", "الإحصائيات والأداء")
    )

    val stores by viewModel.stores.collectAsState()
    val matchingStore = stores.find { it.id == account.id || it.phone == account.phone }
    val isVerified = account.isVerified || (matchingStore?.isActive == true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F141C))
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF18202C))
                .border(1.dp, Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name.ifBlank { "لوحة تحكم المتجر" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "🏬 متجر تجاري • ${account.neighborhood.ifBlank { account.city }}",
                    fontSize = 11.sp,
                    color = Color(0xFF90A4AE)
                )
            }

            Surface(
                color = if (isVerified) Color(0xFF00C853) else Color(0xFFFF9800),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isVerified) "موثق ✓" else "قيد التوثيق ⏳",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    color = if (isVerified) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Horizontal Tabs Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F141C))
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabsList.size) { index ->
                val tab = tabsList[index]
                val isSelected = activeTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color(0xFFFF9800) else Color(0xFF18202C))
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFFFF9800) else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { activeTab = index }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tab.first, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.second,
                            fontSize = 11.5.sp,
                            color = if (isSelected) Color.Black else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

        // Dynamic Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
        ) {
            when (activeTab) {
                0 -> StoreProductsSection(account, viewModel)
                1 -> StoreOrdersSection(account, viewModel)
                2 -> StoreOffersSection(account, viewModel)
                3 -> StoreRatingsSection(account, viewModel)
                4 -> StoreSettingsSection(account, viewModel)
                5 -> StoreStatsSection(account, viewModel)
            }
        }
    }
}

// ==========================================
// 1. Store Products & Inventory Section
// ==========================================
@Composable
private fun StoreProductsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var showQuickPriceDialog by remember { mutableStateOf(false) }
    var itemForQuickPrice by remember { mutableStateOf<ProductEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val allProducts by viewModel.products.collectAsState()
    val allOffers by viewModel.offers.collectAsState()

    val myProducts = remember(allProducts, account.id) {
        allProducts.filter { (it.storeId == account.id || it.storeId == account.phone) && !it.isDeleted }
    }

    val filteredList = remember(myProducts, searchQuery) {
        if (searchQuery.isBlank()) myProducts
        else myProducts.filter { it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("📦 كتالوج المنتجات والمخزون", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                Text("⚡ تعديل الأسعار فوري ومباشر لجميع العملاء", fontSize = 10.sp, color = Color(0xFF00C853))
            }
            Button(
                onClick = {
                    productToEdit = null
                    showAddDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إضافة منتج ➕", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث في المنتجات والتصنيفات...", fontSize = 11.sp, color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = Color(0xFFFF9800)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF9800),
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF18202C),
                unfocusedContainerColor = Color(0xFF18202C)
            )
        )

        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد منتجات مسجلة. اضغط 'إضافة منتج' لنشر بضائعك.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredList, key = { it.id }) { product ->
                    val activeOffer = allOffers.find { off ->
                        off.isActive && off.entityId == account.id && (
                            off.applicableTo == ApplicableType.ALL ||
                            (off.applicableTo == ApplicableType.CATEGORY && off.categoryId == product.category) ||
                            (off.applicableTo == ApplicableType.PRODUCT && off.productIds.contains(product.id))
                        )
                    }
                    val finalPrice = activeOffer?.calculatePrice(product.price) ?: product.price

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF18202C)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (product.imageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = product.imageUrl,
                                        contentDescription = product.name,
                                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(54.dp).background(Color(0xFF222B38), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("📦", fontSize = 24.sp)
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    if (product.category.isNotEmpty()) {
                                        Text("التصنيف: ${product.category}", fontSize = 10.sp, color = Color(0xFFFF9800))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (activeOffer != null && finalPrice < product.price) {
                                            Text(
                                                text = "${product.price.toInt()} YER",
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                textDecoration = TextDecoration.LineThrough
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${finalPrice.toInt()} YER",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00C853)
                                            )
                                        } else {
                                            Text(
                                                text = "${product.price.toInt()} YER",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00C853)
                                            )
                                        }
                                    }
                                }

                                // Quick Instant Price
                                IconButton(
                                    onClick = {
                                        itemForQuickPrice = product
                                        showQuickPriceDialog = true
                                    },
                                    modifier = Modifier.size(32.dp).background(Color(0xFF00C853).copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Text("⚡", fontSize = 13.sp)
                                }

                                IconButton(
                                    onClick = {
                                        productToEdit = product
                                        showAddDialog = true
                                    },
                                    modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.08f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color.White, modifier = Modifier.size(15.dp))
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.deleteProduct(product.id)
                                        Toast.makeText(context, "تم حذف المنتج", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp).background(Color(0xFFE53935).copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFE53935), modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Instant Price Dialog
    if (showQuickPriceDialog && itemForQuickPrice != null) {
        val prod = itemForQuickPrice!!
        var pStr by remember { mutableStateOf(prod.price.toInt().toString()) }

        Dialog(onDismissRequest = { showQuickPriceDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF18202C),
                border = BorderStroke(1.dp, Color(0xFF00C853)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⚡ تعديل السعر الفوري لـ ${prod.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(
                        value = pStr,
                        onValueChange = { pStr = it },
                        label = { Text("السعر الجديد (YER)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00C853))
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showQuickPriceDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("إلغاء", color = Color.White, fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                val np = pStr.toDoubleOrNull() ?: prod.price
                                viewModel.updateProductPrice(prod.id, np)
                                showQuickPriceDialog = false
                                Toast.makeText(context, "تم تحديث السعر فورياً لجميع العملاء!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تحديث فوراً ⚡", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Full Modal
    if (showAddDialog) {
        var name by remember { mutableStateOf(productToEdit?.name ?: "") }
        var category by remember { mutableStateOf(productToEdit?.category ?: "إلكترونيات") }
        var priceStr by remember { mutableStateOf(productToEdit?.price?.toInt()?.toString() ?: "1000") }
        var desc by remember { mutableStateOf(productToEdit?.description ?: "") }
        var imgUrl by remember { mutableStateOf(productToEdit?.imageUrl ?: "") }

        val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                val base64 = com.example.util.ImageUtils.uriToBase64(context, uri, 600, 70)
                if (base64.isNotEmpty()) {
                    imgUrl = base64
                }
            }
        }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF18202C),
                border = BorderStroke(1.dp, Color(0xFFFF9800)),
                modifier = Modifier.fillMaxWidth().padding(14.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = if (productToEdit == null) "➕ إضافة منتج جديد" else "✏️ تعديل المنتج", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم المنتج", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFF9800))
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("التصنيف / القسم", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFF9800))
                    )
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("السعر (YER)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFF9800))
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("وصف المنتج والمواصفات", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFF9800))
                    )

                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222B38)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (imgUrl.isEmpty()) "رفع صورة المنتج 📸" else "تم اختيار الصورة ✓", fontSize = 11.sp, color = Color.White)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showAddDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("إلغاء", color = Color.White, fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    val pr = ProductEntity(
                                        id = productToEdit?.id ?: "",
                                        storeId = account.id,
                                        name = name,
                                        category = category,
                                        price = priceStr.toDoubleOrNull() ?: 0.0,
                                        description = desc,
                                        imageUrl = imgUrl,
                                        isAvailable = true
                                    )
                                    viewModel.saveProduct(pr)
                                    showAddDialog = false
                                    Toast.makeText(context, "تم حفظ المنتج بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("حفظ", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. Store Orders Management Section
// ==========================================
@Composable
private fun StoreOrdersSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val bookings by viewModel.bookings.collectAsState()
    val myOrders = remember(bookings, account.id, account.phone) {
        bookings.filter { it.providerId == account.id || it.providerId == account.phone }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🛍️ طلبات الشراء الواردة", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
        Text("متابعة طلبات الزبائن وتجهيز البضائع للشحن والتسليم", fontSize = 10.5.sp, color = Color(0xFF90A4AE))

        if (myOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد طلبات شراء واردة حالياً.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(myOrders, key = { it.id }) { ord ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF18202C)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("الزبون: ${ord.customerName.ifBlank { ord.clientName }}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(ord.status, fontSize = 11.sp, color = Color(0xFF00C853), fontWeight = FontWeight.Bold)
                            }
                            Text("📞 هاتف: ${ord.customerPhone.ifBlank { ord.clientPhone }}", fontSize = 11.sp, color = Color(0xFF90A4AE))
                            Text("📍 التوصيل إلى: ${ord.customerArea.ifBlank { ord.clientAddress }}", fontSize = 11.sp, color = Color(0xFF90A4AE))
                            Text("📦 المطلوب: ${ord.serviceType.ifBlank { ord.serviceDetails }}", fontSize = 11.sp, color = Color.White)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        viewModel.updateBookingStatus(ord.id, "APPROVED")
                                        Toast.makeText(context, "تم قبول وتجهيز الطلب", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("قبول وتجهيز ✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateBookingStatus(ord.id, "REJECTED")
                                        Toast.makeText(context, "تم رفض الطلب", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("رفض ✕", color = Color(0xFFE53935), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. Store Offers & Discounts Section
// ==========================================
@Composable
private fun StoreOffersSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var showCreateOffer by remember { mutableStateOf(false) }

    val allOffers by viewModel.offers.collectAsState()
    val myOffers = remember(allOffers, account.id) {
        allOffers.filter { it.entityId == account.id || it.entityId == account.phone }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("🎁 عروض وتخفيضات المتجر", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                Text("تحديد عروض ترويجية على منتجات أو تصنيفات محددة بمدة معينة", fontSize = 10.5.sp, color = Color(0xFF90A4AE))
            }
            Button(
                onClick = { showCreateOffer = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("عرض جديد ➕", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        if (myOffers.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد عروض ترويجية نشطة حالياً.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(myOffers, key = { it.id }) { offer ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF18202C)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (offer.isActive) Color(0xFF00C853).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(offer.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Surface(color = Color(0xFF00C853).copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                    val disc = if (offer.discountType == DiscountType.PERCENTAGE) "خصم ${offer.discountValue.toInt()}%" else "خصم ${offer.discountValue.toInt()} YER"
                                    Text(disc, color = Color(0xFF00C853), fontSize = 10.5.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                            if (offer.description.isNotEmpty()) {
                                Text(offer.description, fontSize = 11.sp, color = Color(0xFF90A4AE))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("⏳ ${offer.getRemainingTimeString()}", fontSize = 10.sp, color = Color(0xFFFF9800))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Switch(
                                        checked = offer.isActive,
                                        onCheckedChange = { viewModel.toggleOfferStatus(offer.id, it) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00C853), checkedTrackColor = Color(0xFF00C853).copy(alpha = 0.3f))
                                    )
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteOffer(offer.id)
                                            Toast.makeText(context, "تم حذف العرض", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(30.dp).background(Color(0xFFE53935).copy(alpha = 0.15f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFE53935), modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateOffer) {
        var offTitle by remember { mutableStateOf("") }
        var offDisc by remember { mutableStateOf("10") }
        var offDesc by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showCreateOffer = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF18202C),
                border = BorderStroke(1.dp, Color(0xFFFF9800)),
                modifier = Modifier.fillMaxWidth().padding(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("➕ إنشاء عرض ترويجي للمتجر", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(
                        value = offTitle,
                        onValueChange = { offTitle = it },
                        label = { Text("عنوان العرض الترويجي", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFF9800))
                    )
                    OutlinedTextField(
                        value = offDisc,
                        onValueChange = { offDisc = it },
                        label = { Text("نسبة الخصم %", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFF9800))
                    )
                    OutlinedTextField(
                        value = offDesc,
                        onValueChange = { offDesc = it },
                        label = { Text("التفاصيل والشروط", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFF9800))
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showCreateOffer = false }, modifier = Modifier.weight(1f)) {
                            Text("إلغاء", color = Color.White, fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                if (offTitle.isNotBlank()) {
                                    val off = Offer(
                                        entityId = account.id,
                                        entityType = EntityType.STORE,
                                        title = offTitle,
                                        description = offDesc,
                                        discountType = DiscountType.PERCENTAGE,
                                        discountValue = offDisc.toDoubleOrNull() ?: 10.0,
                                        endDate = System.currentTimeMillis() + (5 * 24 * 60 * 60 * 1000L),
                                        isActive = true
                                    )
                                    viewModel.saveOffer(off)
                                    showCreateOffer = false
                                    Toast.makeText(context, "تم نشر العرض بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("نشر العرض", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. Store Ratings Section
// ==========================================
@Composable
private fun StoreRatingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val ratings by viewModel.ratings.collectAsState()
    val myRatings = remember(ratings, account.id, account.phone) {
        ratings.filter { it.providerId == account.id || it.providerId == account.phone }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("💬 تقييمات الزبائن", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))

        if (myRatings.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد تقييمات بعد.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(myRatings, key = { it.id }) { r ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF18202C)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(r.customerName.ifBlank { "زبون" }, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("⭐ ${r.rating}", fontSize = 12.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                            }
                            if (r.comment.isNotEmpty()) {
                                Text(r.comment, fontSize = 11.sp, color = Color(0xFF90A4AE))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. Store Settings Section
// ==========================================
@Composable
private fun StoreSettingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var isOrdersEnabled by remember { mutableStateOf(account.isInstantOrdersEnabled) }
    var isChatEnabled by remember { mutableStateOf(account.isChatEnabled) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("⚙️ إعدادات المتجر", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF18202C)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🛍️ تفعيل طلبات الشراء المباشرة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("السماح للمستخدمين بإضافة المنتجات للسلة والطلب", fontSize = 10.sp, color = Color(0xFF90A4AE))
                    }
                    Switch(checked = isOrdersEnabled, onCheckedChange = { isOrdersEnabled = it })
                }
                Divider(color = Color.White.copy(alpha = 0.08f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("💬 تفعيل المحادثة الفورية", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("التواصل المباشر مع الزبائن واستقبال استفساراتهم", fontSize = 10.sp, color = Color(0xFF90A4AE))
                    }
                    Switch(checked = isChatEnabled, onCheckedChange = { isChatEnabled = it })
                }
            }
        }

        Button(
            onClick = {
                Toast.makeText(context, "تم حفظ إعدادات المتجر بنجاح!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("حفظ التغييرات", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

// ==========================================
// 6. Store Analytics & Stats Section
// ==========================================
@Composable
private fun StoreStatsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("📊 إحصائيات المتجر والمبيعات", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF18202C)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF00C853).copy(alpha = 0.3f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("إجمالي المبيعات", fontSize = 11.sp, color = Color(0xFF90A4AE))
                    Text("132 طلب", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF18202C)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.3f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("تقييم المتجر", fontSize = 11.sp, color = Color(0xFF90A4AE))
                    Text("⭐ 4.8 / 5.0", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                }
            }
        }
    }
}
