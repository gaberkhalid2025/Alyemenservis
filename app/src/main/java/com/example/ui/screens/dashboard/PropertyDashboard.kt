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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.util.ImageUtils
import com.example.utils.VisualThemePalette

/**
 * 🏢 Standalone Dedicated Dashboard for Real Estate Offices & Agencies (لوحة العقارات المستقلة)
 */
@Composable
fun PropertyDashboard(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) }

    val tabsList = listOf(
        Pair("🏠", "العقارات والوحدات"),
        Pair("📅", "طلبات المعاينة والزيارات"),
        Pair("💬", "تقييمات العملاء"),
        Pair("⚙️", "إعدادات المكتب العقاري"),
        Pair("📊", "الإحصائيات والأداء")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF11141A))
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B202B))
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
                    text = account.name.ifBlank { "لوحة تحكم العقارات" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "🏢 مكتب عقاري معتمد • ${account.city}",
                    fontSize = 11.sp,
                    color = Color(0xFFFFB74D)
                )
            }

            Surface(
                color = Color(0xFF00C853),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "موثق ✓",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Horizontal Tabs Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF11141A))
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabsList.size) { index ->
                val tab = tabsList[index]
                val isSelected = activeTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color(0xFFFF9800) else Color(0xFF1B202B))
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
                0 -> RealEstateUnitsSection(account, viewModel)
                1 -> RealEstateInspectionsSection(account, viewModel)
                2 -> RealEstateRatingsSection(account, viewModel)
                3 -> RealEstateSettingsSection(account, viewModel)
                4 -> RealEstateStatsSection(account, viewModel)
            }
        }
    }
}

// ==========================================
// 1. Real Estate Units Section
// ==========================================
@Composable
private fun RealEstateUnitsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var propToEdit by remember { mutableStateOf<PropertyEntity?>(null) }

    val properties by viewModel.properties.collectAsState()
    val myProperties = remember(properties, account.phone, account.name) {
        properties.filter { (it.phone == account.phone || it.ownerName == account.name) && !it.isDeleted }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("🏠 العقارات والوحدات المعروضة", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                Text("⚡ تعديل أسعار البيع والإيجار فوري للمشترين والمستأجرين", fontSize = 10.sp, color = Color(0xFF00C853))
            }
            Button(
                onClick = {
                    propToEdit = null
                    showAddDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إضافة عقار ➕", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        if (myProperties.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد عقارات مدرجة حالياً. اضغط 'إضافة عقار' لعرض شقة أو عمارة أو أرض.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(myProperties, key = { it.id }) { prop ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B202B)),
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
                                if (prop.images.isNotEmpty()) {
                                    AsyncImage(
                                        model = prop.images.first(),
                                        contentDescription = prop.title,
                                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(60.dp).background(Color(0xFF2B3444), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🏢", fontSize = 26.sp)
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prop.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("📍 ${prop.cityId} - ${prop.address}", fontSize = 10.5.sp, color = Color(0xFFFFB74D))
                                    Text("💰 ${prop.price} • ${prop.propertyType}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.deleteProperty(prop.id)
                                        Toast.makeText(context, "تم حذف العقار", Toast.LENGTH_SHORT).show()
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

    if (showAddDialog) {
        var title by remember { mutableStateOf(propToEdit?.title ?: "") }
        var propType by remember { mutableStateOf(propToEdit?.propertyType ?: "شقة للبيع") }
        var priceStr by remember { mutableStateOf(propToEdit?.price?.toInt()?.toString() ?: "") }
        var address by remember { mutableStateOf(propToEdit?.localNeighborhood ?: "") }
        var desc by remember { mutableStateOf(propToEdit?.description ?: "") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF1B202B),
                border = BorderStroke(1.dp, Color(0xFFFF9800)),
                modifier = Modifier.fillMaxWidth().padding(14.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = if (propToEdit == null) "➕ إضافة عقار جديد" else "✏️ تعديل العقار", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان العقار (مثال: شقة فاخرة للإيجار)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFF9800))
                    )
                    OutlinedTextField(
                        value = propType,
                        onValueChange = { propType = it },
                        label = { Text("النوع (شقة، عمارة، أرض، فيلا)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFF9800))
                    )
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("السعر أو الإيجار الشهري", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFF9800))
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("الموقع والحي", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFF9800))
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("المواصفات والمميزات", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFF9800))
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showAddDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("إلغاء", color = Color.White, fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    val pr = PropertyEntity(
                                        id = propToEdit?.id ?: "",
                                        title = title,
                                        propertyType = propType,
                                        price = priceStr.toDoubleOrNull() ?: 0.0,
                                        localNeighborhood = address,
                                        description = desc,
                                        ownerName = account.name,
                                        phone = account.phone,
                                        cityId = account.city,
                                        isApproved = true,
                                        isActive = true
                                    )
                                    viewModel.saveProperty(pr)
                                    showAddDialog = false
                                    Toast.makeText(context, "تم حفظ العقار بنجاح!", Toast.LENGTH_SHORT).show()
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
// 2. Real Estate Inspection Appointments
// ==========================================
@Composable
private fun RealEstateInspectionsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val bookings by viewModel.bookings.collectAsState()
    val myInspections = remember(bookings, account.id, account.phone) {
        bookings.filter { it.providerId == account.id || it.providerId == account.phone }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("📅 مواعيد طلبات المعاينة الميدانية", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))

        if (myInspections.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد طلبات معاينة عقارية حالياً.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(myInspections, key = { it.id }) { insp ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B202B)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("العميل: ${insp.customerName.ifBlank { insp.clientName }}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(insp.status, fontSize = 11.sp, color = Color(0xFF00C853), fontWeight = FontWeight.Bold)
                            }
                            Text("📞 هاتف: ${insp.customerPhone.ifBlank { insp.clientPhone }}", fontSize = 11.sp, color = Color(0xFFFFB74D))
                            Text("🏠 العقار المطلوب معاينته: ${insp.serviceType.ifBlank { insp.serviceDetails }}", fontSize = 11.sp, color = Color.White)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        viewModel.updateBookingStatus(insp.id, "APPROVED")
                                        Toast.makeText(context, "تم تأكيد موعد المعاينة", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("تأكيد الموعد ✓", color = Color.White, fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateBookingStatus(insp.id, "REJECTED")
                                        Toast.makeText(context, "تم الاعتذار", Toast.LENGTH_SHORT).show()
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
// 3. Real Estate Ratings
// ==========================================
@Composable
private fun RealEstateRatingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val ratings by viewModel.ratings.collectAsState()
    val myRatings = remember(ratings, account.id, account.phone) {
        ratings.filter { it.providerId == account.id || it.providerId == account.phone }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("💬 تقييمات المشترين والمستأجرين", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
        if (myRatings.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد تقييمات بعد.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(myRatings, key = { it.id }) { r ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B202B)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(r.customerName.ifBlank { "عميل" }, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("⭐ ${r.rating}", fontSize = 12.sp, color = Color(0xFFFF9800))
                            if (r.comment.isNotEmpty()) Text(r.comment, fontSize = 11.sp, color = Color(0xFFFFB74D))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. Real Estate Settings
// ==========================================
@Composable
private fun RealEstateSettingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var isBookingEnabled by remember { mutableStateOf(account.isBookingEnabled) }
    var isChatEnabled by remember { mutableStateOf(account.isChatEnabled) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("⚙️ إعدادات المكتب العقاري", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B202B)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("📅 استقبال طلبات حجز المعاينة الميدانية", fontSize = 12.sp, color = Color.White)
                    Switch(checked = isBookingEnabled, onCheckedChange = { isBookingEnabled = it })
                }
                Divider(color = Color.White.copy(alpha = 0.08f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("💬 تفعيل المحادثة الفورية مع المشترين", fontSize = 12.sp, color = Color.White)
                    Switch(checked = isChatEnabled, onCheckedChange = { isChatEnabled = it })
                }
            }
        }

        Button(
            onClick = { Toast.makeText(context, "تم حفظ الإعدادات العقارية!", Toast.LENGTH_SHORT).show() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("حفظ التغييرات", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

// ==========================================
// 5. Real Estate Stats
// ==========================================
@Composable
private fun RealEstateStatsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("📊 إحصائيات العقارات والزيارات", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B202B)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("العقارات المنشورة", fontSize = 11.sp, color = Color(0xFFFFB74D))
                    Text("14 عقار", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B202B)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("طلبات المعاينة", fontSize = 11.sp, color = Color(0xFFFFB74D))
                    Text("29 طلب", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                }
            }
        }
    }
}
