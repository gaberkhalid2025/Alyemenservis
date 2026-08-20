package com.example.ui.screens.urgent

import androidx.compose.animation.*
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
import com.example.data.models.InstantRequestEntity
import com.example.ui.MainViewModel
import com.example.ui.components.UrgentTimerComponent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/**
 * ⚡ UrgentRequestsListScreen
 * عرض قائمة الطلبات العاجلة مع مؤقت الـ 30 دقيقة وفلترة الوقت الحرج وتلوين البطاقات
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrgentRequestsListScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToDetails: (requestId: String) -> Unit = {},
    onNavigateToNewUrgentRequest: () -> Unit = {},
    onNavigateToSubmitUrgentOffer: (requestId: String) -> Unit = {}
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val currentUserId by viewModel.currentUserId.collectAsState()
    val isProvider = viewModel.isProviderUser

    var requestsList by remember { mutableStateOf<List<InstantRequestEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var onlyUnder10MinFilter by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(currentUserId, isProvider) {
        isLoading = true
        var query: Query = firestore.collection("instant_requests")

        if (!isProvider && currentUserId.isNotBlank() && currentUserId != "guest") {
            query = query.whereEqualTo("userId", currentUserId)
        }

        query.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(InstantRequestEntity::class.java) }
                    // تصفية الطلبات العاجلة (30 دقيقة أو كود URG)
                    requestsList = list.filter {
                        it.urgencyTime.contains("30") || it.requestCode.startsWith("URG") || it.serviceTitle.contains("عاجل")
                    }
                }
                isLoading = false
            }
    }

    val now = System.currentTimeMillis()
    val filteredList = requestsList.filter { req ->
        val remainingMinutes = ((req.expiresAt - now) / 1000) / 60
        val matchesTimeFilter = if (onlyUnder10MinFilter) remainingMinutes in 0..10 else true
        val matchesSearch = searchQuery.isBlank() ||
                req.requestCode.contains(searchQuery, ignoreCase = true) ||
                req.serviceTitle.contains(searchQuery, ignoreCase = true) ||
                req.userNeighborhood.contains(searchQuery, ignoreCase = true)

        matchesTimeFilter && matchesSearch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F))
                        Text("الطلبات المستعجلة (30 دقيقة)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFB71C1C))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (!isProvider) {
                        IconButton(onClick = onNavigateToNewUrgentRequest) {
                            Icon(Icons.Default.AddCircle, contentDescription = "طلب عاجل جديد", tint = Color(0xFFD32F2F))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFEBEE))
            )
        },
        floatingActionButton = {
            if (!isProvider) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToNewUrgentRequest,
                    icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                    text = { Text("طلب عاجل جديد (30 دقيقة)") },
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_urgent_new_request")
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // شريط البحث والفلترة السريعة
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث برقم الطلب / الحي...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f).testTag("urgent_search_field"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                FilterChip(
                    selected = onlyUnder10MinFilter,
                    onClick = { onlyUnder10MinFilter = !onlyUnder10MinFilter },
                    label = { Text("أقل من 10 دقائق ⏳", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFCDD2),
                        selectedLabelColor = Color(0xFFB71C1C)
                    )
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFD32F2F))
                }
            } else if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFE57373))
                        Text("لا توجد طلبات عاجلة نشطة حالياً", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        if (!isProvider) {
                            Button(
                                onClick = onNavigateToNewUrgentRequest,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                            ) {
                                Text("إنشاء طلب استجابة سريعة")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(filteredList) { req ->
                        val remainingMinutes = (((req.expiresAt - System.currentTimeMillis()) / 1000) / 60).coerceAtLeast(0)
                        val isCritical = remainingMinutes < 5
                        val isUrgent = remainingMinutes < 10

                        val cardBgColor = when {
                            req.status != "WAITING_FOR_OFFERS" -> MaterialTheme.colorScheme.surface
                            isCritical -> Color(0xFFFFEBEE)
                            isUrgent -> Color(0xFFFFF8E1)
                            else -> MaterialTheme.colorScheme.surface
                        }

                        val cardBorderColor = when {
                            req.status != "WAITING_FOR_OFFERS" -> MaterialTheme.colorScheme.outlineVariant
                            isCritical -> Color(0xFFE53935)
                            isUrgent -> Color(0xFFFFA000)
                            else -> Color(0xFFEF9A9A)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToDetails(req.id) }
                                .testTag("urgent_card_${req.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor),
                            border = BorderStroke(1.5.dp, cardBorderColor)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(req.requestCode, fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFFD32F2F))
                                    UrgentTimerComponent(
                                        expiresAt = req.expiresAt,
                                        totalDurationMillis = 30 * 60 * 1000L,
                                        isCompact = true
                                    )
                                }

                                Text(req.serviceTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(req.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = Color(0xFF424242))

                                HorizontalDivider()

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFD32F2F))
                                        Text("${req.userCity} - ${req.userNeighborhood}", fontSize = 12.sp)
                                    }
                                    if (!isProvider) {
                                        Text("${req.offersCount} عروض مستلمة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onNavigateToDetails(req.id) },
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) {
                                        Text("عرض التفاصيل", fontSize = 12.sp)
                                    }

                                    if (isProvider && req.status == "WAITING_FOR_OFFERS") {
                                        Button(
                                            onClick = { onNavigateToSubmitUrgentOffer(req.id) },
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                            contentPadding = PaddingValues(horizontal = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("عرض فوري", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
