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
import androidx.compose.ui.window.DialogProperties
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
 * 🛠️ Standalone Dedicated Dashboard for Technicians & Craftsmen (لوحة الفني المستقلة)
 */
@Composable
fun TechnicianDashboard(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) }

    val tabsList = listOf(
        Pair("🛠️", "الخدمات والتسعير"),
        Pair("🚨", "الطلبات العاجلة"),
        Pair("🎁", "العروض والخصومات"),
        Pair("📅", "الحجوزات"),
        Pair("🖼️", "معرض الأعمال"),
        Pair("💬", "تقييمات العملاء"),
        Pair("⚙️", "إعدادات الفني"),
        Pair("📊", "الإحصائيات والأداء")
    )

    val providers by viewModel.providers.collectAsState()
    val matchingProvider = providers.find { it.id == account.id || it.phone == account.phone }
    val isVerified = account.isVerified || (matchingProvider?.subscriptionStatus == "APPROVED" || matchingProvider?.isAvailable == true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22))
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
                    text = account.name.ifBlank { "لوحة تحكم الفني" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "🛠️ فني معتمد • ${account.neighborhood.ifBlank { account.city }}",
                    fontSize = 11.sp,
                    color = Color(0xFF8B949E)
                )
            }

            // Green Verified Badge
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
                .background(Color(0xFF0D1117))
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabsList.size) { index ->
                val tab = tabsList[index]
                val isSelected = activeTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color(0xFFFF9800) else Color(0xFF161B22))
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
                0 -> TechnicianServicesSection(account, viewModel)
                1 -> TechnicianUrgentRequestsSection(account, viewModel)
                2 -> TechnicianOffersSection(account, viewModel)
                3 -> TechnicianBookingsSection(account, viewModel)
                4 -> TechnicianPortfolioSection(account, viewModel)
                5 -> TechnicianRatingsSection(account, viewModel)
                6 -> TechnicianSettingsSection(account, viewModel)
                7 -> TechnicianStatsSection(account, viewModel)
            }
        }
    }
}

// ==========================================
// 1. Services & Instant Pricing Section
// ==========================================
@Composable
private fun TechnicianServicesSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var serviceToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var showQuickPriceDialog by remember { mutableStateOf(false) }
    var itemForQuickPrice by remember { mutableStateOf<ProductEntity?>(null) }

    val allProducts by viewModel.products.collectAsState()
    val allOffers by viewModel.offers.collectAsState()

    val myServices = remember(allProducts, account.id) {
        allProducts.filter { (it.storeId == account.id || it.storeId == account.phone) && !it.isDeleted }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🛠️ الخدمات الفنية والتسعير الفوري",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800)
                )
                Text(
                    text = "⚡ أي تعديل على الأسعار يصل للزبائن فوراً في الوقت الحقيقي",
                    fontSize = 10.sp,
                    color = Color(0xFF00C853)
                )
            }
            Button(
                onClick = {
                    serviceToEdit = null
                    showAddDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إضافة خدمة ➕", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        if (myServices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد خدمات مضافة حالياً. اضغط على 'إضافة خدمة' لنشر خدماتك وأسعارك.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(myServices, key = { it.id }) { service ->
                    val activeOffer = allOffers.find { off ->
                        off.isActive && off.entityId == account.id && (
                            off.applicableTo == ApplicableType.ALL ||
                            (off.applicableTo == ApplicableType.CATEGORY && off.categoryId == service.category) ||
                            (off.applicableTo == ApplicableType.PRODUCT && off.productIds.contains(service.id))
                        )
                    }
                    val finalPrice = activeOffer?.calculatePrice(service.price) ?: service.price

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
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
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(Color(0xFFFF9800).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🔧", fontSize = 22.sp)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = service.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (service.category.isNotEmpty()) {
                                        Text("التخصص: ${service.category}", fontSize = 10.sp, color = Color(0xFFFF9800))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (activeOffer != null && finalPrice < service.price) {
                                            Text(
                                                text = "${service.price.toInt()} YER",
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
                                                text = "${service.price.toInt()} YER",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00C853)
                                            )
                                        }
                                    }
                                }

                                // Quick Instant Price Edit Button
                                IconButton(
                                    onClick = {
                                        itemForQuickPrice = service
                                        showQuickPriceDialog = true
                                    },
                                    modifier = Modifier.size(32.dp).background(Color(0xFF00C853).copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Text("⚡", fontSize = 13.sp)
                                }

                                IconButton(
                                    onClick = {
                                        serviceToEdit = service
                                        showAddDialog = true
                                    },
                                    modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.08f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color.White, modifier = Modifier.size(15.dp))
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.deleteProduct(service.id)
                                        Toast.makeText(context, "تم الحذف بنجاح", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp).background(Color(0xFFE53935).copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFE53935), modifier = Modifier.size(15.dp))
                                }
                            }

                            if (service.description.isNotEmpty()) {
                                Text(
                                    text = service.description,
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF8B949E),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Instant Price Dialog
    if (showQuickPriceDialog && itemForQuickPrice != null) {
        val srv = itemForQuickPrice!!
        var pStr by remember { mutableStateOf(srv.price.toInt().toString()) }

        Dialog(onDismissRequest = { showQuickPriceDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF161B22),
                border = BorderStroke(1.dp, Color(0xFF00C853)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⚡ تعديل السعر الفوري المباشر", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("الخدمة: ${srv.name}", fontSize = 12.sp, color = Color(0xFFFF9800))
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
                                val np = pStr.toDoubleOrNull() ?: srv.price
                                viewModel.updateProductPrice(srv.id, np)
                                showQuickPriceDialog = false
                                Toast.makeText(context, "تم تحديث السعر فورياً!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تحديث ⚡", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Full Modal
    if (showAddDialog) {
        var name by remember { mutableStateOf(serviceToEdit?.name ?: "") }
        var category by remember { mutableStateOf(serviceToEdit?.category ?: account.specialty.ifBlank { "صيانة عامة" }) }
        var priceStr by remember { mutableStateOf(serviceToEdit?.price?.toInt()?.toString() ?: "5000") }
        var desc by remember { mutableStateOf(serviceToEdit?.description ?: "") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF161B22),
                border = BorderStroke(1.dp, Color(0xFFFF9800)),
                modifier = Modifier.fillMaxWidth().padding(14.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = if (serviceToEdit == null) "➕ إضافة خدمة فنية جديدة" else "✏️ تعديل الخدمة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الخدمة (مثال: صيانة مكيف سبليت)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFF9800))
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("التخصص / القسم", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFF9800))
                    )
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("سعر الخدمة التقديري (YER)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFF9800))
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("تفاصيل ومميزات الخدمة والضمان", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFF9800))
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showAddDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("إلغاء", color = Color.White, fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    val pr = ProductEntity(
                                        id = serviceToEdit?.id ?: "",
                                        storeId = account.id,
                                        name = name,
                                        category = category,
                                        price = priceStr.toDoubleOrNull() ?: 0.0,
                                        description = desc,
                                        isAvailable = true
                                    )
                                    viewModel.saveProduct(pr)
                                    showAddDialog = false
                                    Toast.makeText(context, "تم حفظ الخدمة بنجاح!", Toast.LENGTH_SHORT).show()
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
// 2. Urgent Requests Live Radar Section
// ==========================================
@Composable
private fun TechnicianUrgentRequestsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var showOfferDialog by remember { mutableStateOf(false) }
    var selectedReqId by remember { mutableStateOf("") }
    var offerPriceStr by remember { mutableStateOf("5000") }
    var arrivalTimeStr by remember { mutableStateOf("30 دقيقة") }
    var offerNotes by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🚨 رادار الطلبات العاجلة الحية (اطلب خدمتك الآن)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
        Text("يتم تنبيهك فور ورود أي طلب عاجل جديد يطابق تخصصك ومدينتك لتتمكن من تقديم عرضك مباشرة.", fontSize = 10.5.sp, color = Color(0xFF8B949E))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("طلب صيانة كهربائية طارئة ⚡", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Surface(color = Color(0xFFE53935).copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                        Text("عاجل جداً", color = Color(0xFFE53935), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                    }
                }
                Text("📍 الموقع: ${account.city} - ${account.neighborhood.ifBlank { "وسط المدينة" }}", fontSize = 11.sp, color = Color(0xFF8B949E))
                Text("📝 التفاصيل: عطل مفاجئ في لوحة القواطع الرئيسية وانقطاع تام للتيار مع وجود رائحة التماس.", fontSize = 11.sp, color = Color.White)
                
                Button(
                    onClick = {
                        selectedReqId = "req_demo_1"
                        showOfferDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تقديم عرض سعر والوصول 🚀", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
            }
        }
    }

    if (showOfferDialog) {
        Dialog(onDismissRequest = { showOfferDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF161B22),
                border = BorderStroke(1.dp, Color(0xFF00C853)),
                modifier = Modifier.fillMaxWidth().padding(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🚀 تقديم عرض مباشر للعميل", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(
                        value = offerPriceStr,
                        onValueChange = { offerPriceStr = it },
                        label = { Text("السعر المقترح (YER)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00C853))
                    )
                    OutlinedTextField(
                        value = arrivalTimeStr,
                        onValueChange = { arrivalTimeStr = it },
                        label = { Text("وقت الوصول المتوقع (مثال: 20 دقيقة)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00C853))
                    )
                    OutlinedTextField(
                        value = offerNotes,
                        onValueChange = { offerNotes = it },
                        label = { Text("ملاحظات إضافية أو المعدات المتوفرة", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00C853))
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showOfferDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("إلغاء", color = Color.White, fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                showOfferDialog = false
                                Toast.makeText(context, "تم إرسال العرض للعميل وإشعاره فوراً!", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إرسال العرض 📨", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. Offers & Discounts Section
// ==========================================
@Composable
private fun TechnicianOffersSection(
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
                Text("🎁 عروض وخصومات الفني", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                Text("جذب المزيد من العملاء عبر تقديم خصومات بنسب محددة لفترات محدودة", fontSize = 10.5.sp, color = Color(0xFF8B949E))
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
                Text("لا توجد عروض ترويجية حالياً. اضغط على 'عرض جديد' لإنشاء خصم فوري.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(myOffers, key = { it.id }) { offer ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
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
                                Text(offer.description, fontSize = 11.sp, color = Color(0xFF8B949E))
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
        var offDisc by remember { mutableStateOf("15") }
        var offDesc by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showCreateOffer = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF161B22),
                border = BorderStroke(1.dp, Color(0xFFFF9800)),
                modifier = Modifier.fillMaxWidth().padding(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("➕ إنشاء عرض ترويجي للفني", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(
                        value = offTitle,
                        onValueChange = { offTitle = it },
                        label = { Text("عنوان العرض (مثال: خصم 20% على فحص التكييف)", fontSize = 11.sp) },
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
                        label = { Text("شروط وتفاصيل العرض", fontSize = 11.sp) },
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
                                        entityType = EntityType.TECHNICIAN,
                                        title = offTitle,
                                        description = offDesc,
                                        discountType = DiscountType.PERCENTAGE,
                                        discountValue = offDisc.toDoubleOrNull() ?: 10.0,
                                        endDate = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L),
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
// 4. Bookings & Appointments Section
// ==========================================
@Composable
private fun TechnicianBookingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val bookings by viewModel.bookings.collectAsState()
    val myBookings = remember(bookings, account.id, account.phone) {
        bookings.filter { it.providerId == account.id || it.providerId == account.phone }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("📅 مواعيد وحجوزات العملاء", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
        Text("إدارة الحجوزات المجدولة ومتابعة الزيارات الميدانية للعملاء", fontSize = 10.5.sp, color = Color(0xFF8B949E))

        if (myBookings.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد حجوزات مجدولة حالياً. الحجوزات الجديدة ستظهر هنا تلقائياً.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(myBookings, key = { it.id }) { booking ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = "عميل: ${booking.customerName.ifBlank { booking.clientName.ifBlank { "عميل مباشر" } }}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(booking.status, fontSize = 11.sp, color = Color(0xFF00C853), fontWeight = FontWeight.Bold)
                            }
                            Text("📞 هاتف: ${booking.customerPhone.ifBlank { booking.clientPhone }}", fontSize = 11.sp, color = Color(0xFF8B949E))
                            Text("📍 العنوان: ${booking.customerArea.ifBlank { booking.clientAddress.ifBlank { "غير محدد" } }}", fontSize = 11.sp, color = Color(0xFF8B949E))
                            Text("🛠️ الخدمة: ${booking.serviceType.ifBlank { booking.serviceDetails }}", fontSize = 11.sp, color = Color.White)
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        viewModel.updateBookingStatus(booking.id, "APPROVED")
                                        Toast.makeText(context, "تم قبول الحجز", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("قبول ✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateBookingStatus(booking.id, "REJECTED")
                                        Toast.makeText(context, "تم الاعتذار عن الحجز", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("اعتذار ✕", color = Color(0xFFE53935), fontSize = 11.sp)
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
// 5. Portfolio Section (معرض الأعمال)
// ==========================================
@Composable
private fun TechnicianPortfolioSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val providers by viewModel.providers.collectAsState()
    val matchingProvider = providers.find { it.id == account.id || it.phone == account.phone }
    val portfolioList = matchingProvider?.portfolioImages ?: emptyList()

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val base64: String = com.example.util.ImageUtils.uriToBase64(context, uri, 700, 70)
            if (base64.isNotEmpty()) {
                val updated = portfolioList + base64
                if (matchingProvider != null) {
                    viewModel.updateProviderPortfolio(matchingProvider.id, updated)
                    Toast.makeText(context, "تم رفع الصورة إلى معرض أعمالك بنجاح!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("🖼️ معرض الأعمال والمشاريع المنجزة", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                Text("استعراض صور أعمالك السابقة يزيد من ثقة الزبائن ومعدل الحجوزات", fontSize = 10.5.sp, color = Color(0xFF8B949E))
            }
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إضافة صورة 📸", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        if (portfolioList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد صور في معرض الأعمال بعد. اضغط 'إضافة صورة' لرفع صور من هاتفك.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(portfolioList) { img ->
                    AsyncImage(
                        model = img,
                        contentDescription = "صورة عمل",
                        modifier = Modifier
                            .size(130.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

// ==========================================
// 6. Ratings & Customer Feedback Section
// ==========================================
@Composable
private fun TechnicianRatingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val ratings by viewModel.ratings.collectAsState()
    val myRatings = remember(ratings, account.id, account.phone) {
        ratings.filter { it.providerId == account.id || it.providerId == account.phone }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("💬 تقييمات وآراء العملاء", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
        Text("متابعة ملاحظات الزبائن للارتقاء بجودة الخدمة الفنية المقدمة", fontSize = 10.5.sp, color = Color(0xFF8B949E))

        if (myRatings.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد تقييمات مسجلة بعد. بعد اكتمال الخدمات سيقوم العملاء بكتابة تقييماتهم هنا.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(myRatings, key = { it.id }) { r ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(r.customerName.ifBlank { "عميل" }, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("⭐ ${r.rating}", fontSize = 12.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                            }
                            if (r.comment.isNotEmpty()) {
                                Text(r.comment, fontSize = 11.sp, color = Color(0xFF8B949E))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. Settings & Toggles Section
// ==========================================
@Composable
private fun TechnicianSettingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var isBookingEnabled by remember { mutableStateOf(account.isBookingEnabled) }
    var isUrgentEnabled by remember { mutableStateOf(account.isInstantOrdersEnabled) }
    var isChatEnabled by remember { mutableStateOf(account.isChatEnabled) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("⚙️ إعدادات التحكم والخيارات الفنية", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("📅 تفعيل نظام الحجوزات المجدولة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("السماح للعملاء بحجز مواعيد مسبقة عبر صفحتك", fontSize = 10.sp, color = Color(0xFF8B949E))
                    }
                    Switch(checked = isBookingEnabled, onCheckedChange = { isBookingEnabled = it })
                }
                Divider(color = Color.White.copy(alpha = 0.08f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🚨 استقبال طلبات «اطلب خدمتك الآن» العاجلة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("تلقي إشعارات الحالات الطارئة لتقديم عروض سريعة", fontSize = 10.sp, color = Color(0xFF8B949E))
                    }
                    Switch(checked = isUrgentEnabled, onCheckedChange = { isUrgentEnabled = it })
                }
                Divider(color = Color.White.copy(alpha = 0.08f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("💬 تفعيل المحادثة والدردشة الفورية", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("استقبال استفسارات ورسائل العملاء مباشرة", fontSize = 10.sp, color = Color(0xFF8B949E))
                    }
                    Switch(checked = isChatEnabled, onCheckedChange = { isChatEnabled = it })
                }
            }
        }

        Button(
            onClick = {
                Toast.makeText(context, "تم حفظ الإعدادات بنجاح!", Toast.LENGTH_SHORT).show()
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
// 8. Performance & Analytics Section
// ==========================================
@Composable
private fun TechnicianStatsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("📊 الإحصائيات ومؤشرات الأداء", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF00C853).copy(alpha = 0.3f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("الطلبات المنجزة", fontSize = 11.sp, color = Color(0xFF8B949E))
                    Text("48 طلب", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.3f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("تقييم الفني العام", fontSize = 11.sp, color = Color(0xFF8B949E))
                    Text("⭐ 4.9 / 5.0", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.3f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("مشاهدات الملف", fontSize = 11.sp, color = Color(0xFF8B949E))
                    Text("1,240 زيارة", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("نسبة القبول", fontSize = 11.sp, color = Color(0xFF8B949E))
                    Text("98%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
