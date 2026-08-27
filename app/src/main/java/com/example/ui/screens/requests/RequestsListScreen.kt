package com.example.ui.screens.requests

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.InstantRequestEntity
import com.example.data.models.LoadingState
import com.example.ui.MainViewModel

/**
 * 📑 RequestsListScreen
 * شاشة عرض وتصفية طلبات الخدمات الفورية ومقارنة العروض المتاحة
 * متوافقة مع RequestsViewModel ومعمارية MVVM
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsListScreen(
    viewModel: MainViewModel,
    requestsViewModel: RequestsViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToDetails: (requestId: String) -> Unit = {},
    onNavigateToNewRequest: () -> Unit = {},
    onNavigateToOffersList: (requestId: String) -> Unit = {},
    onNavigateToSubmitOffer: (requestId: String) -> Unit = {}
) {
    val currentUserId by viewModel.currentUserId.collectAsState()
    val isProvider = viewModel.isProviderUser

    val requestsState by requestsViewModel.requestsState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: النشطة, 1: المكتملة, 2: الملغية
    var searchQuery by remember { mutableStateOf("") }

    val tabs = listOf("النشطة", "المكتملة", "الملغية")

    LaunchedEffect(Unit) {
        requestsViewModel.listenToAllRequests()
    }

    val allRequests = remember(requestsState) {
        when (val state = requestsState) {
            is LoadingState.Success -> state.data
            else -> emptyList()
        }
    }

    val userOrProviderRequests = remember(allRequests, currentUserId, isProvider) {
        if (!isProvider && currentUserId.isNotBlank() && currentUserId != "guest") {
            allRequests.filter { it.userId == currentUserId || it.userPhone == currentUserId }
        } else {
            allRequests
        }
    }

    val filteredList = userOrProviderRequests.filter { req ->
        val matchesTab = when (selectedTab) {
            0 -> req.status == "WAITING_FOR_OFFERS" || req.status == "REVIEWING_OFFERS"
            1 -> req.status == "COMPLETED" || req.status == "ACCEPTED"
            2 -> req.status == "CANCELLED"
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() ||
                req.requestCode.contains(searchQuery, ignoreCase = true) ||
                req.serviceTitle.contains(searchQuery, ignoreCase = true) ||
                req.userNeighborhood.contains(searchQuery, ignoreCase = true)

        matchesTab && matchesSearch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isProvider) "طلبات العملاء المتاحة" else "طلباتي الفورية", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (!isProvider) {
                        IconButton(onClick = onNavigateToNewRequest) {
                            Icon(Icons.Default.AddCircle, contentDescription = "طلب جديد", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        floatingActionButton = {
            if (!isProvider) {
                FloatingActionButton(
                    onClick = onNavigateToNewRequest,
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("fab_create_request")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة طلب", tint = Color.White)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // شريط البحث
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث برقم الطلب أو العنوان أو الحي...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_request_field"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // التبويبات
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            when (requestsState) {
                is LoadingState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is LoadingState.Error -> {
                    val errorMsg = (requestsState as LoadingState.Error).message
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                            Text(errorMsg, color = MaterialTheme.colorScheme.error)
                            Button(onClick = { requestsViewModel.listenToAllRequests() }) {
                                Text("إعادة المحاولة")
                            }
                        }
                    }
                }
                else -> {
                    if (filteredList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                                Text("لا توجد طلبات في هذا التبويب", color = Color.Gray)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
                        ) {
                            items(filteredList, key = { it.id }) { req ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToDetails(req.id) }
                                        .testTag("request_item_card_${req.id}"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(req.requestCode, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
                                            Badge(
                                                containerColor = when (req.status) {
                                                    "WAITING_FOR_OFFERS" -> Color(0xFF1976D2)
                                                    "COMPLETED", "ACCEPTED" -> Color(0xFF2E7D32)
                                                    "CANCELLED" -> Color(0xFFD32F2F)
                                                    else -> Color(0xFFFFA000)
                                                }
                                            ) {
                                                Text(
                                                    text = when (req.status) {
                                                        "WAITING_FOR_OFFERS" -> "بانتظار العروض"
                                                        "COMPLETED", "ACCEPTED" -> "مكتمل"
                                                        "CANCELLED" -> "ملغي"
                                                        else -> req.status
                                                    },
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        Text(req.serviceTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(req.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = Color.DarkGray)

                                        HorizontalDivider()

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                Text("${req.userCity} - ${req.userNeighborhood}", fontSize = 12.sp)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF2E7D32))
                                                Text("${req.offersCount} عروض", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                            }
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { onNavigateToDetails(req.id) },
                                                modifier = Modifier.weight(1f).height(36.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 4.dp)
                                            ) {
                                                Text("التفاصيل", fontSize = 12.sp)
                                            }

                                            if (!isProvider && req.offersCount > 0) {
                                                Button(
                                                    onClick = { onNavigateToOffersList(req.id) },
                                                    modifier = Modifier.weight(1f).height(36.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                                ) {
                                                    Text("مقارنة العروض (${req.offersCount})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else if (isProvider && req.status == "WAITING_FOR_OFFERS") {
                                                Button(
                                                    onClick = { onNavigateToSubmitOffer(req.id) },
                                                    modifier = Modifier.weight(1f).height(36.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                                ) {
                                                    Text("تقديم عرض", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
