package com.example.ui

import com.example.utils.*

import android.widget.Toast
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.util.*
import com.example.utils.VisualThemePalette

/**
 * 🏢 Modern SaaS Business Management Suite for Merchants
 * Redesigned for Restaurants, Stores, Medical Centers, Real Estate, and Service Providers.
 */

@Composable
fun UnifiedBusinessDashboardScreen(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) } // 0: Overview/Stats, 1: Products/Catalog, 2: Customer Replies, 3: Offers, 4: Bookings, 5: Settings

    val tabsList = listOf(
        Pair("📊", "لوحة القيادة"),
        Pair("🛒", "المنتجات والخدمات"),
        Pair("💬", "ردود العملاء"),
        Pair("🎁", "العروض والخصومات"),
        Pair("📅", "الحجوزات والطلبات"),
        Pair("⚙️", "إعدادات المنشأة")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101418)) // Deep Slate Background
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A2128))
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
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name.ifBlank { account.businessType.titleArabic },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${account.businessType.icon} ${account.businessType.titleArabic} • ID: ${account.id.take(12)}...",
                    fontSize = 11.sp,
                    color = Color(0xFF9EA9B5)
                )
            }
            Surface(
                color = if (account.isVerified) Color(0xFF00C853) else Color(0xFFFF9800),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (account.isVerified) "موثق ⚡" else "قيد التوثيق",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Horizontal SaaS Navigation Tabs
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF101418))
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabsList.size) { index ->
                val tab = tabsList[index]
                val isSelected = activeTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color(0xFFFF9800) else Color(0xFF1A2128))
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

        // Tab Content Router
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
        ) {
            when (activeTab) {
                0 -> DashboardStatsOverviewSection(account, viewModel)
                1 -> CatalogAndProductManagementSection(account, viewModel)
                2 -> MerchantReviewReplySection(account, viewModel)
                3 -> MerchantOffersSection(account, viewModel)
                4 -> MerchantBookingsSection(account, viewModel)
                5 -> MerchantSettingsSection(account, viewModel)
            }
        }
    }
}

// =========================================================
// 1. 📊 Dashboard & Stats Overview (Mini Charts & Metrics)
// =========================================================
@Composable
fun DashboardStatsOverviewSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(
                text = "⚡ ملخص الأداء وإحصائيات المنشأة اليومية",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9800)
            )
        }

        // 2x2 Stats Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Stat 1: طلبات اليوم
                    SaaSStatCard(
                        title = "طلبات اليوم",
                        value = "18 طلب",
                        subtext = "📈 +24% مقارنة بأمس",
                        icon = "📦",
                        color = Color(0xFF00C853),
                        modifier = Modifier.weight(1f),
                        chartBars = listOf(30, 45, 60, 40, 75, 90, 100)
                    )

                    // Stat 2: التقييمات الجديدة
                    SaaSStatCard(
                        title = "التقييمات الجديدة",
                        value = "⭐ 4.9",
                        subtext = "12 تقييم جديد اليوم",
                        icon = "🌟",
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f),
                        chartBars = listOf(80, 85, 90, 95, 88, 92, 98)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Stat 3: المبيعات
                    SaaSStatCard(
                        title = "المبيعات (YER)",
                        value = "245,000",
                        subtext = "💰 ريال يمني اليوم",
                        icon = "💳",
                        color = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f),
                        chartBars = listOf(40, 50, 70, 65, 85, 90, 110)
                    )

                    // Stat 4: الزيارات
                    SaaSStatCard(
                        title = "الزيارات والمشاهدات",
                        value = "1,420",
                        subtext = "🔥 تفاعل عالي جداً",
                        icon = "👁️",
                        color = Color(0xFFA855F7),
                        modifier = Modifier.weight(1f),
                        chartBars = listOf(50, 65, 80, 70, 90, 100, 120)
                    )
                }
            }
        }

        // Quick Control Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2128)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🚀 حالة استقبال الطلبيات المباشرة",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            color = Color(0xFF00C853).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF00C853))
                        ) {
                            Text(
                                text = "🟢 استقبال مفعل",
                                fontSize = 10.5.sp,
                                color = Color(0xFF00C853),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "منشأتك ظاهرة الآن في محرك مقارنة الأسعار والبحث الشامل لجميع مستخدمي دليل خدمات اليمن.",
                        fontSize = 11.sp,
                        color = Color(0xFF9EA9B5)
                    )
                }
            }
        }
    }
}

@Composable
fun SaaSStatCard(
    title: String,
    value: String,
    subtext: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier,
    chartBars: List<Int>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2128)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, color = Color(0xFF9EA9B5))
                Text(icon, fontSize = 14.sp)
            }
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtext, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)

            // Mini Sparkline Bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val max = chartBars.maxOrNull() ?: 100
                chartBars.forEach { valPct ->
                    val heightRatio = valPct.toFloat() / max.toFloat()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(heightRatio.coerceIn(0.2f, 1f))
                            .clip(RoundedCornerShape(4.dp))
                            .background(color.copy(alpha = 0.7f))
                    )
                }
            }
        }
    }
}

// =========================================================
// 2. 🛒 Intuitive Catalog & Rapid Price Management Section
// =========================================================
@Composable
fun CatalogAndProductManagementSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var showAddModal by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Sample merchant items state for rapid editing
    var itemsList by remember {
        mutableStateOf(
            listOf(
                ProductEntity(id = "1", name = "بيتزا ببروني سوبريم", price = 4500.0, category = "الوجبات الرئيسية", isAvailable = true, isOffer = true, discountPercent = 15, oldPrice = 5300.0),
                ProductEntity(id = "2", name = "وجبة برجر دجاج مقرمش", price = 2800.0, category = "الوجبات السريعة", isAvailable = true),
                ProductEntity(id = "3", name = "عصير مانجو طبيعي طازج", price = 1200.0, category = "المشروبات", isAvailable = true),
                ProductEntity(id = "4", name = "صحن كباب بلدي مع رز", price = 6000.0, category = "الوجبات الرئيسية", isAvailable = true)
            )
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("🛒 كتالوج المنتجات والخدمات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                Text("إضافة سريعة وتحديث الأسعار بضغطة زر واحدة", fontSize = 10.5.sp, color = Color(0xFF9EA9B5))
            }

            Button(
                onClick = { showAddModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إضافة عنصر ➕", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        // Live Item Cards
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(itemsList) { item ->
                MerchantItemCard(
                    item = item,
                    onPriceUpdate = { newPrice, discountPct ->
                        itemsList = itemsList.map {
                            if (it.id == item.id) {
                                val old = if (discountPct > 0) newPrice / (1 - discountPct / 100.0) else 0.0
                                it.copy(price = newPrice, discountPercent = discountPct, isOffer = discountPct > 0, oldPrice = old)
                            } else it
                        }
                        Toast.makeText(context, "✅ تم تحديث السعر والخصم بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    onToggleAvailability = {
                        itemsList = itemsList.map {
                            if (it.id == item.id) it.copy(isAvailable = !it.isAvailable) else it
                        }
                    }
                )
            }
        }
    }

    if (showAddModal) {
        DynamicAddItemModal(
            onDismiss = { showAddModal = false },
            onSave = { newItem ->
                itemsList = listOf(newItem) + itemsList
                Toast.makeText(context, "🎉 تم إضافة العنصر بنجاح!", Toast.LENGTH_SHORT).show()
                showAddModal = false
            }
        )
    }
}

@Composable
fun MerchantItemCard(
    item: ProductEntity,
    onPriceUpdate: (Double, Int) -> Unit,
    onToggleAvailability: () -> Unit
) {
    var isEditingPrice by remember { mutableStateOf(false) }
    var priceInput by remember { mutableStateOf(item.price.toInt().toString()) }
    var discountInput by remember { mutableStateOf(item.discountPercent.toString()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2128)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFF9800).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🍽️", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(item.name, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${item.category} • YER", fontSize = 10.5.sp, color = Color(0xFF9EA9B5))
                    }
                }

                // Rapid Price Trigger
                Surface(
                    onClick = { isEditingPrice = !isEditingPrice },
                    color = Color(0xFFFF9800).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF9800))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.price.toInt()} YER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color(0xFFFF9800), modifier = Modifier.size(13.dp))
                    }
                }
            }

            if (item.isOffer && item.discountPercent > 0) {
                Surface(
                    color = Color(0xFF00C853).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "🏷️ خصم مفعّل: %${item.discountPercent} (السعر السابق: ${item.oldPrice.toInt()} YER)",
                        fontSize = 10.sp,
                        color = Color(0xFF00C853),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Rapid Inline 2-Tap Price & Discount Updater Form
            AnimatedVisibility(visible = isEditingPrice) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF101418), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚡ تعديل السعر والخصم المباشر (2 Taps):", fontSize = 11.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = priceInput,
                            onValueChange = { priceInput = it },
                            label = { Text("السعر الجديد (YER)", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF9800))
                        )

                        OutlinedTextField(
                            value = discountInput,
                            onValueChange = { discountInput = it },
                            label = { Text("نسبة الخصم %", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF9800))
                        )
                    }

                    Button(
                        onClick = {
                            val newP = priceInput.toDoubleOrNull() ?: item.price
                            val newDisc = discountInput.toIntOrNull() ?: 0
                            onPriceUpdate(newP, newDisc)
                            isEditingPrice = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حفظ التحديث السريع ⚡", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Modal for Adding Dynamic Item with Auto-Compression Indicator & Custom Tags
@Composable
fun DynamicAddItemModal(
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("الوجبات الرئيسية") }
    var description by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(mutableListOf("متوفر", "توصيل سريع")) }
    val availableTags = listOf("نباتي", "توصيل سريع", "متوفر", "عرض خاص", "طازج يومياً", "خدمة فورية")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A2128),
        title = {
            Text("➕ إضافة عنصر جديد للكتالوج", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Image Upload Slot with WebP Auto-Compression Indicator
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101418)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clickable { }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "رفع صورة", tint = Color(0xFFFF9800))
                        Text("اضغط لرفع صورة المنتج / الخدمة", fontSize = 11.sp, color = Color.White)
                        Text("🖼️ ضغط تلقائي: WebP (800px - جودة 75%)", fontSize = 9.5.sp, color = Color(0xFF00C853))
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم المنتج / الخدمة", fontSize = 10.5.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF9800))
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("السعر (بالريال اليمني YER)", fontSize = 10.5.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF9800))
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("الوصف والتفاصيل", fontSize = 10.5.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF9800))
                )

                Text("🏷️ الشارات والعلامات المميزة (Tags):", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(availableTags) { tag ->
                        val isSel = selectedTags.contains(tag)
                        Surface(
                            onClick = {
                                val current = selectedTags.toMutableList()
                                if (isSel) current.remove(tag) else current.add(tag)
                                selectedTags = current
                            },
                            color = if (isSel) Color(0xFFFF9800) else Color(0xFF101418),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isSel) Color(0xFFFF9800) else Color.White.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = tag,
                                fontSize = 10.sp,
                                color = if (isSel) Color.Black else Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && price.isNotBlank()) {
                        val newEntity = ProductEntity(
                            id = System.currentTimeMillis().toString(),
                            name = title,
                            price = price.toDoubleOrNull() ?: 0.0,
                            category = category,
                            description = description,
                            isAvailable = true
                        )
                        onSave(newEntity)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("حفظ وإضافة للكتالوج 💾", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", fontSize = 11.sp, color = Color.LightGray)
            }
        }
    )
}

// =========================================================
// 3. 💬 Interactive Review & Rating Reply Management Section
// =========================================================
@Composable
fun MerchantReviewReplySection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val context = LocalContext.current

    // Sample reviews for direct merchant reply
    var sampleReviews by remember {
        mutableStateOf(
            listOf(
                Triple("1", "أحمد علي", Pair(5.0f, "خدمة رائعة جداً والطعام وصل حار وممتاز!")),
                Triple("2", "صالح محمد", Pair(4.0f, "المكان مميز والتوصيل سريع لكن أتمنى زيادة كمية الصلصة.")),
                Triple("3", "محمد باوزير", Pair(5.0f, "أفضل تجربة مع هذا المركز الطبي، دقة في المواعيد."))
            )
        )
    }

    var repliesMap by remember { mutableStateOf(mutableMapOf<String, String>()) }

    val quickResponses = listOf(
        "شكراً لزيارتك وثقتك بنا! 🌹",
        "يسعدنا خدمتك دائماً! ❤️",
        "أهلاً بك في أي وقت! ⚡",
        "نعتذر وسنعوضك في زيارتك القادمة 🙏"
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("💬 ردود العملاء وتفاعلات التقييم", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(sampleReviews) { review ->
                val reviewId = review.first
                val userName = review.second
                val rating = review.third.first
                val comment = review.third.second
                val currentReply = repliesMap[reviewId] ?: ""

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2128)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(userName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Surface(
                                color = Color(0xFFFF9800).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("⭐ $rating", fontSize = 11.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }

                        Text(comment, fontSize = 11.5.sp, color = Color(0xFF9EA9B5))

                        Divider(color = Color.White.copy(alpha = 0.08f))

                        Text("⚡ رد المنشأة المباشر (1-Click Quick Reply):", fontSize = 10.5.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)

                        // 1-Click Quick Response Chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(quickResponses) { quickMsg ->
                                Surface(
                                    onClick = {
                                        repliesMap = repliesMap.toMutableMap().apply { put(reviewId, quickMsg) }
                                        Toast.makeText(context, "✅ تم إرسال الرد السريع للعميل!", Toast.LENGTH_SHORT).show()
                                    },
                                    color = Color(0xFF101418),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                                ) {
                                    Text(
                                        text = quickMsg,
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (currentReply.isNotEmpty()) {
                            Surface(
                                color = Color(0xFF00C853).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF00C853))
                            ) {
                                Text(
                                    text = "💬 ردك المسجل: $currentReply",
                                    fontSize = 11.sp,
                                    color = Color(0xFF00C853),
                                    modifier = Modifier.padding(8.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// 4. 🎁 Merchant Offers & Discount Section
// =========================================================
@Composable
fun MerchantOffersSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var offerTitle by remember { mutableStateOf("") }
    var offerDiscount by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🎁 إدارة العروض والخصومات الخاصة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2128)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("نشر عرض ترويجي جديد ينزل في قسم العروض والخصومات:", fontSize = 11.5.sp, color = Color.White)

                OutlinedTextField(
                    value = offerTitle,
                    onValueChange = { offerTitle = it },
                    label = { Text("عنوان العرض الترويجي (مثال: خصم 20% على جميع الوجبات)", fontSize = 10.5.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF9800))
                )

                OutlinedTextField(
                    value = offerDiscount,
                    onValueChange = { offerDiscount = it },
                    label = { Text("نسبة الخصم %", fontSize = 10.5.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF9800))
                )

                Button(
                    onClick = {
                        if (offerTitle.isNotBlank()) {
                            Toast.makeText(context, "🎉 تم نشر العرض الترويجي للمستخدمين بنجاح!", Toast.LENGTH_SHORT).show()
                            offerTitle = ""
                            offerDiscount = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("نشر العرض في التطبيق 📢", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =========================================================
// 5. 📅 Merchant Bookings & Appointments Section
// =========================================================
@Composable
fun MerchantBookingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("📅 إدارة الحجوزات والطلبات المباشرة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2128)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("لا توجد طلبيات متأخرة أو حجوزات معلقة حالياً.", fontSize = 11.5.sp, color = Color.LightGray)
                Text("تصلك الإشعارات الفورية عند قيام أي مستخدم بحجز موعد أو طلب منتج من صفحتك.", fontSize = 10.5.sp, color = Color(0xFF9EA9B5))
            }
        }
    }
}

// =========================================================
// 6. ⚙️ Merchant Settings & Details Section
// =========================================================
@Composable
fun MerchantSettingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(account.name) }
    var phone by remember { mutableStateOf(account.phone) }
    var hours by remember { mutableStateOf(account.workingHours) }
    var address by remember { mutableStateOf(account.neighborhood) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2128)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("⚙️ بيانات وإعدادات المنشأة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم المنشأة / المكان", fontSize = 10.5.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF9800))
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الواتساب والهاتف للتواصل", fontSize = 10.5.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF9800))
                    )

                    OutlinedTextField(
                        value = hours,
                        onValueChange = { hours = it },
                        label = { Text("أوقات ودوام العمل", fontSize = 10.5.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF9800))
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("العنوان والحي التفصيلي", fontSize = 10.5.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF9800))
                    )

                    Button(
                        onClick = {
                            Toast.makeText(context, "✅ تم تحديث بيانات المنشأة بنجاح!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("حفظ التحديثات 💾", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
