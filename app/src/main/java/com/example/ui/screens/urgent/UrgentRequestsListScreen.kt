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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.urgent.components.UrgentCard
import com.example.utils.VisualThemePalette
import com.example.viewmodels.UrgentUiState
import com.example.viewmodels.UrgentViewModel

/**
 * ⚡ UrgentRequestsListScreen
 * عرض قائمة الطلبات العاجلة مع مؤقت الـ 30 دقيقة وفلترة الوقت الحرج وتلوين البطاقات.
 * تعتمد على UrgentViewModel وتدعم Snackbar للرسائل وحالات التحميل الكاملة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrgentRequestsListScreen(
    viewModel: MainViewModel,
    urgentViewModel: UrgentViewModel = viewModel(),
    themeColors: VisualThemePalette,
    onNavigateBack: () -> Unit = {},
    onNavigateToDetails: (requestId: String) -> Unit = {},
    onNavigateToNewUrgentRequest: () -> Unit = {},
    onNavigateToSubmitUrgentOffer: (requestId: String) -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUserId by viewModel.currentUserId.collectAsState()
    val isProvider = viewModel.isProviderUser

    val requestsList by urgentViewModel.urgentRequests.collectAsState()
    val uiState by urgentViewModel.uiState.collectAsState()

    var onlyUnder10MinFilter by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(currentUserId, isProvider) {
        urgentViewModel.observeUrgentRequests(currentUserId, isProvider)
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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

            when (uiState) {
                is UrgentUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFD32F2F))
                    }
                }
                is UrgentUiState.Error -> {
                    val errMsg = (uiState as UrgentUiState.Error).message
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFE57373))
                            Text(errMsg, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                            Button(
                                onClick = { urgentViewModel.observeUrgentRequests(currentUserId, isProvider) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                            ) {
                                Text("إعادة المحاولة")
                            }
                        }
                    }
                }
                else -> {
                    if (filteredList.isEmpty()) {
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
                            items(filteredList, key = { it.id }) { req ->
                                UrgentCard(
                                    request = req,
                                    isProvider = isProvider,
                                    themeColors = themeColors,
                                    onNavigateToDetails = onNavigateToDetails,
                                    onNavigateToSubmitOffer = onNavigateToSubmitUrgentOffer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
