package com.example.ui.screens.status
import com.example.ui.MainViewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repositories.StatusRepositoryImpl

import com.example.utils.VisualThemePalette
import kotlinx.coroutines.flow.collectLatest

enum class StatusTab(val title: String, val icon: String) {
    OVERVIEW("نظرة عامة", "📊"),
    JOIN_REQUESTS("طلبات الانضمام", "📝"),
    BOOKINGS("الحجوزات والطلبات", "📋"),
    NOTIFICATIONS("الإشعارات والتنبيهات", "🔔")
}

/**
 * 📊 StatusScreen
 * Main platform status hub modularized cleanly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    statusViewModel: StatusViewModel,
    themeColors: VisualThemePalette,
    onBackClick: (() -> Unit)? = null
) {
    val uiState by statusViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        statusViewModel.eventFlow.collectLatest { event ->
            when (event) {
                is StatusEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is StatusEvent.ShowToast -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = themeColors.surface,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (onBackClick != null) {
                                IconButton(onClick = onBackClick) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "رجوع",
                                        tint = themeColors.textPrimary
                                    )
                                }
                            }
                            Text(
                                text = "مركز التنبيهات وحالة المنصة",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.textPrimary
                            )
                        }

                        IconButton(onClick = { statusViewModel.refreshData() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "تحديث",
                                tint = themeColors.accent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(StatusTab.values()) { tab ->
                            val isSelected = tab == uiState.selectedTab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) themeColors.accent else themeColors.background)
                                    .border(
                                        1.dp,
                                        if (isSelected) themeColors.accent else themeColors.border,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { statusViewModel.selectTab(tab) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(tab.icon, fontSize = 13.sp)
                                    Text(
                                        text = tab.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.Black else themeColors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = themeColors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(14.dp)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = themeColors.accent)
                }
            } else {
                when (uiState.selectedTab) {
                    StatusTab.OVERVIEW -> {
                        StatusOverviewContent(
                            metrics = uiState.metrics,
                            themeColors = themeColors
                        )
                    }
                    StatusTab.JOIN_REQUESTS -> {
                        StatusJoinRequestsContent(
                            requests = uiState.pendingJoinRequests,
                            themeColors = themeColors,
                            onApprove = { statusViewModel.approveJoinRequest(it) },
                            onReject = { statusViewModel.rejectJoinRequest(it) }
                        )
                    }
                    StatusTab.BOOKINGS -> {
                        StatusBookingsContent(
                            bookings = uiState.systemBookings,
                            instantRequests = uiState.instantRequests,
                            themeColors = themeColors
                        )
                    }
                    StatusTab.NOTIFICATIONS -> {
                        StatusNotificationsContent(
                            notifications = uiState.notifications,
                            themeColors = themeColors,
                            onClearAll = { statusViewModel.clearAllNotifications() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Backwards compatible overload for callers.
 */
@Composable
fun StatusScreen(
    viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    themeColors: VisualThemePalette,
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val statusViewModel = remember { StatusViewModel(StatusRepositoryImpl(context)) }
    StatusScreen(
        statusViewModel = statusViewModel,
        themeColors = themeColors,
        onBackClick = onBackClick
    )
}
