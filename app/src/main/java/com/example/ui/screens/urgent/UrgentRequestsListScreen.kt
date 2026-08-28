package com.example.ui.screens.urgent

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.viewmodels.UrgentViewModel

/**
 * ⚡ UrgentRequestsListScreen
 * Main host screen for 30-minute Urgent Requests List with live filter and modular content.
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
                    text = { Text("طلب عاجل ⚡") },
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UrgentListFilterBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onlyUnder10MinFilter = onlyUnder10MinFilter,
                onToggleUnder10MinFilter = { onlyUnder10MinFilter = it },
                themeColors = themeColors
            )

            UrgentListContent(
                requests = filteredList,
                isProvider = isProvider,
                themeColors = themeColors,
                onNavigateToDetails = onNavigateToDetails,
                onNavigateToSubmitUrgentOffer = onNavigateToSubmitUrgentOffer
            )
        }
    }
}
