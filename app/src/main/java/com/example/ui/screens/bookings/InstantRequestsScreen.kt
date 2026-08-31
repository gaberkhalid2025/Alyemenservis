package com.example.ui.screens.bookings
import com.example.ui.MainViewModel

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.utils.VisualThemePalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstantRequestsScreen(
    viewModel: MainViewModel = viewModel(),
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit = {}
) {
    val instantReqViewModel: InstantRequestViewModel = viewModel()
    val uiState by instantReqViewModel.uiState.collectAsState()

    val instantRequests by viewModel.instantRequests.collectAsState()
    val requestOffers by viewModel.requestOffers.collectAsState()
    
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState(initial = "")
    val adminRole by viewModel.adminRole.collectAsState()

    val filteredList = remember(instantRequests, uiState.selectedTab, uiState.searchQuery, currentUserPhone, currentUserId) {
        instantRequests.filter { req ->
            val matchesTab = if (uiState.selectedTab == 0) {
                currentUserPhone.isBlank() || req.userPhone == currentUserPhone || req.userId == currentUserId || adminRole.isNotBlank()
            } else {
                req.status == "WAITING_FOR_OFFERS" || req.status == "REVIEWING_OFFERS"
            }
            val matchesQuery = uiState.searchQuery.isBlank() ||
                    req.requestCode.contains(uiState.searchQuery, ignoreCase = true) ||
                    req.serviceTitle.contains(uiState.searchQuery, ignoreCase = true) ||
                    req.userCity.contains(uiState.searchQuery, ignoreCase = true)
            matchesTab && matchesQuery
        }.sortedByDescending { it.createdAt }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🚨 اطلب خدمتك الآن (مزايدة فورية)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        Text("عروض أسعار تنافسية ومباشرة خلال ساعتين ⏱️", fontSize = 11.sp, color = Color(0xFFA7F3D0))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { instantReqViewModel.setShowCreateDialog(true) },
                containerColor = Color(0xFF10B981),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("اطلب خدمتك الآن ⚡", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = Color(0xFF1E293B),
                contentColor = themeColors.accent
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { instantReqViewModel.setTab(0) },
                    text = { Text("طلباتي السابقة 📑", fontSize = 12.sp) }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { instantReqViewModel.setTab(1) },
                    text = { Text("سوق الطلبات المفتوحة (للفنيين) 🛠️", fontSize = 12.sp) }
                )
            }
            
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { instantReqViewModel.setSearchQuery(it) },
                placeholder = { Text("بحث بالرقم أو الخدمة أو المدينة...", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColors.accent,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            InstantRequestList(
                requests = filteredList,
                offers = requestOffers,
                currentUserId = currentUserId,
                themeColors = themeColors,
                onViewOffers = { instantReqViewModel.setSelectedRequestForOffers(it) },
                onSubmitOffer = { instantReqViewModel.setSelectedRequestForSubmitOffer(it) }
            )
        }
    }

    if (uiState.showCreateDialog) {
        CreateInstantRequestDialog(
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { instantReqViewModel.setShowCreateDialog(false) }
        )
    }

    uiState.selectedRequestForOffers?.let { req ->
        val reqOffers = requestOffers.filter { it.requestId == req.id }
        ReviewOffersDialog(
            request = req,
            offers = reqOffers,
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { instantReqViewModel.setSelectedRequestForOffers(null) },
            onAcceptOffer = { selectedOffer ->
                viewModel.acceptRequestOffer(req, selectedOffer)
                instantReqViewModel.setSelectedRequestForOffers(null)
            }
        )
    }

    uiState.selectedRequestForSubmitOffer?.let { req ->
        SubmitOfferDialog(
            request = req,
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { instantReqViewModel.setSelectedRequestForSubmitOffer(null) }
        )
    }
}
