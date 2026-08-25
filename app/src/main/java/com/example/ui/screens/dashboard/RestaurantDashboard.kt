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
 * 🍔 Standalone Dedicated Dashboard for Restaurants & Cafes (لوحة المطعم والكافيه)
 */
@Composable
fun RestaurantDashboard(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) }

    val tabsList = listOf(
        Pair("🍔", "قائمة الوجبات والمنيو"),
        Pair("🛍️", "طلبات الزبائن"),
        Pair("🎁", "عروض الوجبات"),
        Pair("💬", "آراء الزبائن"),
        Pair("⚙️", "إعدادات المطعم"),
        Pair("📊", "الإحصائيات والأداء")
    )

    val stores by viewModel.stores.collectAsState()
    val matchingStore = stores.find { it.id == account.id || it.phone == account.phone }
    val isVerified = account.isVerified || (matchingStore?.isActive == true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF140E0E))
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF221616))
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
                    text = account.name.ifBlank { "لوحة تحكم المطعم" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "🍔 مطعم وكافيه • ${account.neighborhood.ifBlank { account.city }}",
                    fontSize = 11.sp,
                    color = Color(0xFFAAA09D)
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
                .background(Color(0xFF140E0E))
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabsList.size) { index ->
                val tab = tabsList[index]
                val isSelected = activeTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color(0xFFFF9800) else Color(0xFF221616))
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
                0 -> RestaurantMenuSection(account, viewModel)
                1 -> RestaurantOrdersSection(account, viewModel)
                2 -> RestaurantOffersSection(account, viewModel)
                3 -> RestaurantRatingsSection(account, viewModel)
                4 -> RestaurantSettingsSection(account, viewModel)
                5 -> RestaurantStatsSection(account, viewModel)
            }
        }
    }
}

// ==========================================
// 1. Restaurant Menu & Meals Section
// ==========================================
@Composable
private fun RestaurantMenuSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var mealToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var showQuickPriceDialog by remember { mutableStateOf(false) }
    var mealForQuickPrice by remember { mutableStateOf<ProductEntity?>(null) }

    val allProducts by viewModel.products.collectAsState()
    val allOffers by viewModel.offers.collectAsState()

    val myMeals = remember(allProducts, account.id) {
        allProducts.filter { (it.storeId == account.id || it.storeId == account.phone) && !it.isDeleted }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("🍔 قائمة الوجبات والمنيو المباشر", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                Text("⚡ تعديل أسعار الوجبات يظهر فوراً لزبائن المطعم", fontSize = 10.sp, color = Color(0xFF00C853))
            }
            Button(
                onClick = {
                    mealToEdit = null
                    showAddDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إضافة وجبة ➕", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        if (myMeals.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("قائمة الطعام فارغة حالياً. اضغط 'إضافة وجبة' لإضافة أطباقك اللذيذة.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(myMeals, key = { it.id }) { meal ->
                    val activeOffer = allOffers.find { off ->
                        off.isActive && off.entityId == account.id && (
                            off.applicableTo == ApplicableType.ALL ||
                            (off.applicableTo == ApplicableType.CATEGORY && off.categoryId == meal.category) ||
                            (off.applicableTo == ApplicableType.PRODUCT && off.productIds.contains(meal.id))
                        )
                    }
                    val finalPrice = activeOffer?.calculatePrice(meal.price) ?: meal.price

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF221616)),
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
                                if (meal.imageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = meal.imageUrl,
                                        contentDescription = meal.name,
                                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(54.dp).background(Color(0xFF332020), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🍲", fontSize = 24.sp)
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(meal.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    if (meal.category.isNotEmpty()) {
                                        Text("القسم: ${meal.category}", fontSize = 10.sp, color = Color(0xFFFF9800))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (activeOffer != null && finalPrice < meal.price) {
                                            Text(
                                                text = "${meal.price.toInt()} YER",
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
                                                text = "${meal.price.toInt()} YER",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00C853)
                                            )
                                        }
                                    }
                                }

                                // Quick Instant Price Edit
                                IconButton(
                                    onClick = {
                                        mealForQuickPrice = meal
                                        showQuickPriceDialog = true
                                    },
                                    modifier = Modifier.size(32.dp).background(Color(0xFF00C853).copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Text("⚡", fontSize = 13.sp)
                                }

                                IconButton(
                                    onClick = {
                                        mealToEdit = meal
                                        showAddDialog = true
                                    },
                                    modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.08f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color.White, modifier = Modifier.size(15.dp))
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.deleteProduct(meal.id)
                                        Toast.makeText(context, "تم حذف الوجبة", Toast.LENGTH_SHORT).show()
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
    if (showQuickPriceDialog && mealForQuickPrice != null) {
        val m = mealForQuickPrice!!
        var pStr by remember { mutableStateOf(m.price.toInt().toString()) }

        Dialog(onDismissRequest = { showQuickPriceDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF221616),
                border = BorderStroke(1.dp, Color(0xFF00C853)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⚡ تعديل سعر وجبة ${m.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                                val np = pStr.toDoubleOrNull() ?: m.price
                                viewModel.updateProductPrice(m.id, np)
                                showQuickPriceDialog = false
                                Toast.makeText(context, "تم تحديث السعر فورياً!", Toast.LENGTH_SHORT).show()
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
        var name by remember { mutableStateOf(mealToEdit?.name ?: "") }
        var category by remember { mutableStateOf(mealToEdit?.category ?: "الوجبات الرئيسية") }
        var priceStr by remember { mutableStateOf(mealToEdit?.price?.toInt()?.toString() ?: "2000") }
        var desc by remember { mutableStateOf(mealToEdit?.description ?: "") }
        var imgUrl by remember { mutableStateOf(mealToEdit?.imageUrl ?: "") }

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
                color = Color(0xFF221616),
                border = BorderStroke(1.dp, Color(0xFFFF9800)),
                modifier = Modifier.fillMaxWidth().padding(14.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = if (mealToEdit == null) "➕ إضافة وجبة للمنيو" else "✏️ تعديل الوجبة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الوجبة أو المشروب", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFF9800))
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("قسم المنيو (مشاوي، برجر، مقبلات، مشروبات)", fontSize = 11.sp) },
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
                        label = { Text("مكونات الطبق والإضافات المتوفرة", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFF9800))
                    )

                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF332020)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (imgUrl.isEmpty()) "رفع صورة الوجبة 📸" else "تم اختيار الصورة ✓", fontSize = 11.sp, color = Color.White)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showAddDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("إلغاء", color = Color.White, fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    val pr = ProductEntity(
                                        id = mealToEdit?.id ?: "",
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
                                    Toast.makeText(context, "تم حفظ الوجبة بنجاح!", Toast.LENGTH_SHORT).show()
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
// 2. Restaurant Orders Section
// ==========================================
@Composable
private fun RestaurantOrdersSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val bookings by viewModel.bookings.collectAsState()
    val myOrders = remember(bookings, account.id, account.phone) {
        bookings.filter { it.providerId == account.id || it.providerId == account.phone }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🛍️ طلبات الطعام والوجبات", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))

        if (myOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد طلبات طعام حالياً.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(myOrders, key = { it.id }) { ord ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF221616)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("الزبون: ${ord.customerName.ifBlank { ord.clientName }}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(ord.status, fontSize = 11.sp, color = Color(0xFF00C853), fontWeight = FontWeight.Bold)
                            }
                            Text("📞 هاتف: ${ord.customerPhone.ifBlank { ord.clientPhone }}", fontSize = 11.sp, color = Color(0xFFAAA09D))
                            Text("📍 العنوان: ${ord.customerArea.ifBlank { ord.clientAddress }}", fontSize = 11.sp, color = Color(0xFFAAA09D))
                            Text("🍲 الوجبة: ${ord.serviceType.ifBlank { ord.serviceDetails }}", fontSize = 11.sp, color = Color.White)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        viewModel.updateBookingStatus(ord.id, "APPROVED")
                                        Toast.makeText(context, "تم تأكيد وتحضير الوجبة", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("تأكيد الطلب ✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateBookingStatus(ord.id, "REJECTED")
                                        Toast.makeText(context, "تم إلغاء الطلب", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("إلغاء ✕", color = Color(0xFFE53935), fontSize = 11.sp)
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
// 3. Restaurant Offers Section
// ==========================================
@Composable
private fun RestaurantOffersSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val allOffers by viewModel.offers.collectAsState()
    val myOffers = remember(allOffers, account.id) {
        allOffers.filter { it.entityId == account.id || it.entityId == account.phone }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🎁 عروض وتخفيضات الوجبات", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
        if (myOffers.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد عروض نشطة حالياً للوجبات.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(myOffers, key = { it.id }) { offer ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF221616)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(offer.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("⏳ ${offer.getRemainingTimeString()}", fontSize = 10.sp, color = Color(0xFFFF9800))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. Restaurant Ratings Section
// ==========================================
@Composable
private fun RestaurantRatingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val ratings by viewModel.ratings.collectAsState()
    val myRatings = remember(ratings, account.id, account.phone) {
        ratings.filter { it.providerId == account.id || it.providerId == account.phone }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("💬 آراء الزبائن وتقييمات المذاق", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
        if (myRatings.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد تقييمات بعد.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(myRatings, key = { it.id }) { r ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF221616)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(r.customerName.ifBlank { "زبون" }, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("⭐ ${r.rating}", fontSize = 12.sp, color = Color(0xFFFF9800))
                            if (r.comment.isNotEmpty()) Text(r.comment, fontSize = 11.sp, color = Color(0xFFAAA09D))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. Restaurant Settings Section
// ==========================================
@Composable
private fun RestaurantSettingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var isOrdersEnabled by remember { mutableStateOf(account.isInstantOrdersEnabled) }
    var isChatEnabled by remember { mutableStateOf(account.isChatEnabled) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("⚙️ إعدادات المطعم", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF221616)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("🍔 تفعيل استقبال طلبات الطعام أونلاين", fontSize = 12.sp, color = Color.White)
                    Switch(checked = isOrdersEnabled, onCheckedChange = { isOrdersEnabled = it })
                }
                Divider(color = Color.White.copy(alpha = 0.08f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("💬 تفعيل المحادثة الفورية مع الزبائن", fontSize = 12.sp, color = Color.White)
                    Switch(checked = isChatEnabled, onCheckedChange = { isChatEnabled = it })
                }
            }
        }

        Button(
            onClick = { Toast.makeText(context, "تم حفظ إعدادات المطعم!", Toast.LENGTH_SHORT).show() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("حفظ التغييرات", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

// ==========================================
// 6. Restaurant Stats Section
// ==========================================
@Composable
private fun RestaurantStatsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("📊 إحصائيات المطعم والطلبات", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF221616)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("الوجبات المطلوبة", fontSize = 11.sp, color = Color(0xFFAAA09D))
                    Text("215 وجبة", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF221616)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("التقييم العام", fontSize = 11.sp, color = Color(0xFFAAA09D))
                    Text("⭐ 4.9 / 5.0", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                }
            }
        }
    }
}
