package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UrgentRequestEntity

/**
 * ⚡ MyUrgentRequestsScreen (`my_urgent_requests_screen`):
 * Isolated destination strictly for Urgent Requests, Product inquiries, & Bidding offers.
 * Toolbar Title: "طلباتي والعروض"
 * Tabs: [قيد الانتظار] | [وصلت عروض] | [مقبولة] | [مكتملة]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyUrgentRequestsScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onOpenRequestDetails: (String) -> Unit = {}
) {
    val myRequestsList: List<UrgentRequestEntity> by viewModel.userUrgentRequests.collectAsState(initial = emptyList())
    var selectedTabIdx by remember { mutableIntStateOf(0) }
    val tabs = listOf("قيد الانتظار ⏳", "وصلت عروض 💬", "مقبولة ✅", "مكتملة 🏁")

    val filteredList = remember(myRequestsList, selectedTabIdx) {
        when (selectedTabIdx) {
            0 -> myRequestsList.filter { it.status == "OPEN" || it.status == "PENDING" }
            1 -> myRequestsList.filter { it.status == "HAS_OFFERS" }
            2 -> myRequestsList.filter { it.status == "ACCEPTED" || it.status == "IN_PROGRESS" }
            3 -> myRequestsList.filter { it.status == "COMPLETED" }
            else -> myRequestsList
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("طلباتي والعروض ⚡", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs Bar
            ScrollableTabRow(
                selectedTabIndex = selectedTabIdx,
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFF38BDF8),
                edgePadding = 12.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIdx == index,
                        onClick = { selectedTabIdx = index },
                        text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد طلبات عاجلة في هذه الفئة حالياً",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "يمكنك إنشاء طلب خدمة أو منتج عاجل ليصلك العروض فورياً من الفنيين والمتاجر",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { request ->
                        UrgentRequestCardItem(request = request, onClick = { onOpenRequestDetails(request.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun UrgentRequestCardItem(
    request: UrgentRequestEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request.category.ifEmpty { "طلب خدمة عاجل" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )

                Surface(
                    color = when (request.status) {
                        "ACCEPTED", "IN_PROGRESS" -> Color(0xFF10B981)
                        "COMPLETED" -> Color(0xFF3B82F6)
                        "HAS_OFFERS" -> Color(0xFFFFB300)
                        else -> Color(0xFFD97706)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = request.status,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = request.description.ifEmpty { "تفاصيل الطلب العاجل..." },
                fontSize = 12.sp,
                color = Color(0xFFCBD5E1),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (request.winningProviderName.isNotEmpty()) "الفني: ${request.winningProviderName}" else "الحالة: ${request.status}",
                    fontSize = 11.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "📍 ${request.cityId}",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}
