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
 * 🏥 Standalone Dedicated Dashboard for Medical Centers & Clinics (لوحة المركز الطبي والعيادات)
 */
@Composable
fun MedicalDashboard(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) }

    val tabsList = listOf(
        Pair("🩺", "الخدمات الطبية والكشوفات"),
        Pair("📅", "حجوزات المرضى والمواعيد"),
        Pair("🎁", "عروض الفحوصات والتحاليل"),
        Pair("👨‍⚕️", "كادر الأطباء والاستشاريين"),
        Pair("💬", "تقييمات المراجعين"),
        Pair("⚙️", "إعدادات المركز الطبي"),
        Pair("📊", "الإحصائيات")
    )

    val stores by viewModel.stores.collectAsState()
    val matchingStore = stores.find { it.id == account.id || it.phone == account.phone }
    val isVerified = account.isVerified || (matchingStore?.isActive == true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A141A))
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF10222B))
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
                    text = account.name.ifBlank { "لوحة تحكم المركز الطبي" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "🏥 مركز طبي وعيادات • ${account.neighborhood.ifBlank { account.city }}",
                    fontSize = 11.sp,
                    color = Color(0xFF80CBC4)
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
                .background(Color(0xFF0A141A))
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabsList.size) { index ->
                val tab = tabsList[index]
                val isSelected = activeTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color(0xFF00B0FF) else Color(0xFF10222B))
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF00B0FF) else Color.White.copy(alpha = 0.08f),
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
                0 -> MedicalServicesSection(account, viewModel)
                1 -> MedicalBookingsSection(account, viewModel)
                2 -> MedicalOffersSection(account, viewModel)
                3 -> MedicalDoctorsSection(account, viewModel)
                4 -> MedicalRatingsSection(account, viewModel)
                5 -> MedicalSettingsSection(account, viewModel)
                6 -> MedicalStatsSection(account, viewModel)
            }
        }
    }
}

// ==========================================
// 1. Medical Services & Instant Pricing
// ==========================================
@Composable
private fun MedicalServicesSection(
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
                Text("🩺 الخدمات الطبية والتسعير الفوري", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00B0FF))
                Text("⚡ تعديل أسعار الكشوفات والتحاليل في الوقت الحقيقي", fontSize = 10.sp, color = Color(0xFF00C853))
            }
            Button(
                onClick = {
                    serviceToEdit = null
                    showAddDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إضافة كشف/خدمة ➕", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        if (myServices.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد خدمات طبية مسجلة حالياً. اضغط 'إضافة كشف/خدمة'.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
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
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10222B)),
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
                                    modifier = Modifier.size(46.dp).background(Color(0xFF00B0FF).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("💉", fontSize = 22.sp)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(service.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    if (service.category.isNotEmpty()) {
                                        Text("العيادة/القسم: ${service.category}", fontSize = 10.sp, color = Color(0xFF00B0FF))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (activeOffer != null && finalPrice < service.price) {
                                            Text("${service.price.toInt()} YER", fontSize = 11.sp, color = Color.Gray, textDecoration = TextDecoration.LineThrough)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("${finalPrice.toInt()} YER", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                                        } else {
                                            Text("${service.price.toInt()} YER", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                                        }
                                    }
                                }

                                // Quick Instant Price
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
                                        Toast.makeText(context, "تم الحذف", Toast.LENGTH_SHORT).show()
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
        val srv = itemForQuickPrice!!
        var pStr by remember { mutableStateOf(srv.price.toInt().toString()) }

        Dialog(onDismissRequest = { showQuickPriceDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF10222B),
                border = BorderStroke(1.dp, Color(0xFF00C853)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⚡ تعديل سعر الكشف الطبي المباشر", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("الخدمة: ${srv.name}", fontSize = 11.sp, color = Color(0xFF00B0FF))
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
                                Toast.makeText(context, "تم تحديث سعر الخدمة فورياً!", Toast.LENGTH_SHORT).show()
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
        var category by remember { mutableStateOf(serviceToEdit?.category ?: "عيادة الباطنية") }
        var priceStr by remember { mutableStateOf(serviceToEdit?.price?.toInt()?.toString() ?: "3000") }
        var desc by remember { mutableStateOf(serviceToEdit?.description ?: "") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF10222B),
                border = BorderStroke(1.dp, Color(0xFF00B0FF)),
                modifier = Modifier.fillMaxWidth().padding(14.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = if (serviceToEdit == null) "➕ إضافة كشف أو فحص طبي" else "✏️ تعديل الكشف", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الكشف / الفحص (مثال: كشف استشاري قلب)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00B0FF))
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("العيادة أو القسم الطبي", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00B0FF))
                    )
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("سعر الكشف (YER)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00B0FF))
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("المواعيد وملاحظات الاستشارة", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00B0FF))
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
                                    Toast.makeText(context, "تم حفظ الخدمة الطبية بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
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
// 2. Medical Bookings & Appointments
// ==========================================
@Composable
private fun MedicalBookingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val bookings by viewModel.bookings.collectAsState()
    val myBookings = remember(bookings, account.id, account.phone) {
        bookings.filter { it.providerId == account.id || it.providerId == account.phone }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("📅 مواعيد وحجوزات المرضى المسبقة", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00B0FF))

        if (myBookings.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد مواعيد كشوفات مجدولة حالياً.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(myBookings, key = { it.id }) { b ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10222B)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("المريض: ${b.customerName.ifBlank { b.clientName }}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(b.status, fontSize = 11.sp, color = Color(0xFF00C853), fontWeight = FontWeight.Bold)
                            }
                            Text("📞 هاتف: ${b.customerPhone.ifBlank { b.clientPhone }}", fontSize = 11.sp, color = Color(0xFF80CBC4))
                            Text("🩺 الكشف: ${b.serviceType.ifBlank { b.serviceDetails }}", fontSize = 11.sp, color = Color.White)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        viewModel.updateBookingStatus(b.id, "APPROVED")
                                        Toast.makeText(context, "تم تثبيت موعد الكشف", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("تثبيت الموعد ✓", color = Color.White, fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateBookingStatus(b.id, "REJECTED")
                                        Toast.makeText(context, "تم إلغاء الموعد", Toast.LENGTH_SHORT).show()
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
// 3. Medical Offers Section
// ==========================================
@Composable
private fun MedicalOffersSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val allOffers by viewModel.offers.collectAsState()
    val myOffers = remember(allOffers, account.id) {
        allOffers.filter { it.entityId == account.id || it.entityId == account.phone }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🎁 باقات وعروض الفحوصات والتحاليل", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00B0FF))
        if (myOffers.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد باقات عروض ترويجية منشورة.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(myOffers, key = { it.id }) { offer ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10222B)),
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
// 4. Doctors & Staff Section
// ==========================================
@Composable
private fun MedicalDoctorsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("👨‍⚕️ كادر الأطباء والاستشاريين بالمركز", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00B0FF))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10222B)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🩺 د. أحمد باحاج - استشاري أمراض القلب والأوعية الدموية", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("🕒 الدوام: السبت إلى الخميس (4:00 عصراً - 9:00 مساءً)", fontSize = 11.sp, color = Color(0xFF80CBC4))
            }
        }
    }
}

// ==========================================
// 5. Medical Ratings Section
// ==========================================
@Composable
private fun MedicalRatingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val ratings by viewModel.ratings.collectAsState()
    val myRatings = remember(ratings, account.id, account.phone) {
        ratings.filter { it.providerId == account.id || it.providerId == account.phone }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("💬 تقييمات المراجعين والمرضى", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00B0FF))
        if (myRatings.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد تقييمات مسجلة بعد.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(myRatings, key = { it.id }) { r ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10222B)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(r.customerName.ifBlank { "مراجع" }, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("⭐ ${r.rating}", fontSize = 12.sp, color = Color(0xFFFF9800))
                            if (r.comment.isNotEmpty()) Text(r.comment, fontSize = 11.sp, color = Color(0xFF80CBC4))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. Medical Settings Section
// ==========================================
@Composable
private fun MedicalSettingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var isBookingEnabled by remember { mutableStateOf(account.isBookingEnabled) }
    var isChatEnabled by remember { mutableStateOf(account.isChatEnabled) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("⚙️ إعدادات المركز الطبي", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00B0FF))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10222B)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("📅 تفعيل حجز الكشوفات الطبية أونلاين", fontSize = 12.sp, color = Color.White)
                    Switch(checked = isBookingEnabled, onCheckedChange = { isBookingEnabled = it })
                }
                Divider(color = Color.White.copy(alpha = 0.08f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("💬 تفعيل الاستشارات والدردشة الطبية", fontSize = 12.sp, color = Color.White)
                    Switch(checked = isChatEnabled, onCheckedChange = { isChatEnabled = it })
                }
            }
        }

        Button(
            onClick = { Toast.makeText(context, "تم حفظ إعدادات المركز الطبي!", Toast.LENGTH_SHORT).show() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("حفظ التغييرات", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

// ==========================================
// 7. Medical Stats Section
// ==========================================
@Composable
private fun MedicalStatsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("📊 إحصائيات العيادات والمراجعين", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00B0FF))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10222B)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("إجمالي المراجعين", fontSize = 11.sp, color = Color(0xFF80CBC4))
                    Text("340 مريض", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10222B)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("معدل الرضا", fontSize = 11.sp, color = Color(0xFF80CBC4))
                    Text("⭐ 4.95 / 5.0", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                }
            }
        }
    }
}
