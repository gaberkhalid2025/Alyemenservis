package com.example.ui.screens.dashboard

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

import com.example.data.repositories.*

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

    val techViewModel = remember(account.id) {
        TechnicianDashboardViewModel(
            ownerId = account.id,
            dashboardRepository = DashboardRepositoryImpl(context),
            productsRepository = ProductsRepositoryImpl(context),
            ratingsRepository = RatingsRepositoryImpl(context),
            galleryRepository = GalleryRepositoryImpl(context)
        )
    }

    val techUiState by techViewModel.uiState.collectAsState()

    LaunchedEffect(techViewModel) {
        techViewModel.eventFlow.collect { event ->
            when (event) {
                is DashboardEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is DashboardEvent.NavigateToDetail -> { /* Navigate */ }
            }
        }
    }

    val tabsList = listOf(
        Pair("🔧", "الخدمات والتسعير"),
        Pair("🚨", "الطلبات العاجلة"),
        Pair("📅", "حجوزات العملاء"),
        Pair("🖼️", "معرض الأعمال"),
        Pair("💬", "تقييمات وآراء"),
        Pair("📝", "الملف الشخصي الفني"),
        Pair("📊", "الإحصائيات والنمو")
    )

    val providers by viewModel.providers.collectAsState()
    val matchingProvider = providers.find { it.id == account.id || it.phone == account.phone }
    val isVerified = account.isVerified || (matchingProvider?.subscriptionStatus == "APPROVED" || matchingProvider?.isAvailable == true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
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
                    color = themeColors.accent
                )
            }

            Surface(
                color = if (isVerified) Color(0xFF10B981) else Color(0xFFF59E0B),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isVerified) "موثق ✓" else "قيد التوثيق ⏳",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Horizontal Tabs Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabsList.size) { index ->
                val tab = tabsList[index]
                val isSelected = activeTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) themeColors.accent else Color(0xFF1E293B))
                        .border(
                            1.dp,
                            if (isSelected) themeColors.accent else Color.White.copy(alpha = 0.08f),
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

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

        // Dynamic Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
        ) {
            when (activeTab) {
                0 -> TabProductsServices(account, viewModel, themeColors)
                1 -> TechnicianUrgentRequestsSection(account, viewModel, themeColors)
                2 -> TabBookingsOrders(account, viewModel, themeColors)
                3 -> TabGalleryAlbums(account, viewModel, themeColors)
                4 -> TabReviewsFeedback(account, viewModel, themeColors)
                5 -> TabProfileEdit(account, viewModel, themeColors)
                6 -> TabStatisticsGrowth(account, viewModel, themeColors)
            }
        }
    }
}

// ==========================================================
// 🚨 Urgent Requests Live Radar Section (رادار الطلبات الطارئة)
// ==========================================================
@Composable
private fun TechnicianUrgentRequestsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    val instantRequests by viewModel.instantRequests.collectAsState()
    val openRequests = remember(instantRequests, account.city) {
        instantRequests.filter { req ->
            (req.status == "WAITING_FOR_OFFERS" || req.status == "REVIEWING_OFFERS") &&
            (account.city.isBlank() || req.userCity.isBlank() || req.userCity.contains(account.city, ignoreCase = true) || account.city.contains(req.userCity, ignoreCase = true))
        }
    }

    var selectedReqForOffer by remember { mutableStateOf<com.example.data.models.InstantRequestEntity?>(null) }
    var offerPrice by remember { mutableStateOf("") }
    var offerArrival by remember { mutableStateOf("خلال 30 دقيقة") }
    var offerNotes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "🚨 رادار الطلبات العاجلة الحية (${openRequests.size} طلب مفتوح)",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = themeColors.accent
        )
        Text(
            text = "يتم تنبيهك فور ورود أي طلب عاجل جديد في مدينتك (${account.city.ifBlank { "كافة المناطق" }}) لتتمكن من تقديم عرض سعر فوري والتنفيذ مباشرة.",
            fontSize = 10.5.sp,
            color = Color.LightGray
        )

        if (openRequests.isEmpty()) {
            UnifiedEmptyState(
                icon = "🚨",
                title = "لا توجد طلبات عاجلة مفتوحة حالياً",
                description = "لا توجد طلبات فورية نشطة في مدينتك في هذه اللحظة. سيصلك إشعار فوري عند نشر أي طلب جديد.",
                themeColors = themeColors,
                modifier = Modifier.weight(1f)
            )
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(openRequests) { req ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = req.serviceTitle.ifBlank { "طلب خدمة عاجلة" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Surface(color = Color(0xFFEF5350).copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = "⚡ عاجل جداً",
                                        color = Color(0xFFEF5350),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text("📍 الموقع: ${req.userCity} - ${req.userNeighborhood.ifBlank { "وسط المدينة" }}", fontSize = 10.5.sp, color = themeColors.accent)
                            if (req.description.isNotBlank()) {
                                Text("📝 التفاصيل: ${req.description}", fontSize = 10.5.sp, color = Color.LightGray)
                            }
                            Text("👤 العميل: ${req.userName.ifBlank { "عميل معتمد" }} • ⏱️ وقت الإنجاز المطلوب: ${req.urgencyTime}", fontSize = 10.sp, color = Color.Gray)

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = {
                                    selectedReqForOffer = req
                                    offerPrice = ""
                                    offerArrival = "خلال 30 دقيقة"
                                    offerNotes = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تقديم عرض سعر مباشر 🚀", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedReqForOffer != null) {
        val req = selectedReqForOffer!!
        AlertDialog(
            onDismissRequest = { selectedReqForOffer = null },
            title = { Text("تقديم عرض سعر للطلب العاجل ⚡", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("الطلب: ${req.serviceTitle}", fontSize = 11.5.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = offerPrice,
                        onValueChange = { offerPrice = it },
                        label = { Text("عرض السعر المقترح (ريال يمني)", fontSize = 11.sp) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = offerArrival,
                        onValueChange = { offerArrival = it },
                        label = { Text("وقت الوصول المقدر للعميل", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = offerNotes,
                        onValueChange = { offerNotes = it },
                        label = { Text("ملاحظات إضافية وضمان الخدمة", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val priceVal = offerPrice.toDoubleOrNull()
                        if (priceVal != null && priceVal > 0) {
                            viewModel.submitOfferForRequest(
                                requestId = req.id,
                                requestCode = req.requestCode,
                                technicianId = account.id,
                                technicianName = account.name,
                                technicianPhone = account.phone,
                                technicianAvatar = account.rawProvider?.profileImage ?: "",
                                technicianRating = account.rating.toFloat(),
                                price = priceVal,
                                estimatedArrivalTime = offerArrival,
                                estimatedDuration = "ساعتان",
                                notes = offerNotes
                            )
                            selectedReqForOffer = null
                            Toast.makeText(context, "✅ تم إرسال عرضك بنجاح! سيتم إشعار العميل فوراً.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "⚠️ يرجى إدخال سعر صحيح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("إرسال العرض الآن 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedReqForOffer = null }) {
                    Text("إلغاء", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        )
    }
}
