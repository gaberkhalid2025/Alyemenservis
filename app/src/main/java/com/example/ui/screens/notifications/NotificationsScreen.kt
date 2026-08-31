package com.example.ui.screens.notifications

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NotificationEntity

import com.example.ui.screens.notifications.components.NotificationEmptyState
import com.example.ui.screens.notifications.components.NotificationsFilterBar
import com.example.ui.screens.notifications.components.NotificationsList
import com.example.utils.VisualThemePalette

/**
 * 🔔 NotificationsScreen
 * Full-screen or modal host for user notifications management with tabs, mark-all-read, and fast filtering.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: AuthViewModel,
    themeColors: VisualThemePalette,
    onNavigateBack: () -> Unit,
    onNotificationClick: (NotificationEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val notifViewModel = remember(viewModel) { NotificationViewModel(viewModel) }

    val allNotifications by notifViewModel.notifications.collectAsState()
    val readIds by notifViewModel.readNotificationIds.collectAsState()
    val activeTab by notifViewModel.activeTab.collectAsState()

    LaunchedEffect(Unit) {
        notifViewModel.loadReadNotifications(context)
    }

    // Filter list
    val filteredList = remember(allNotifications, readIds, activeTab) {
        when (activeTab) {
            "UNREAD" -> allNotifications.filter { !readIds.contains(it.id) }
            "BOOKING" -> allNotifications.filter { it.notificationType == "BOOKING" || it.title.contains("حجز") }
            "MESSAGE" -> allNotifications.filter { it.notificationType == "MESSAGE" || it.title.contains("رسالة") || it.title.contains("دردشة") }
            "SPECIAL_OFFER" -> allNotifications.filter { it.notificationType == "SPECIAL_OFFER" || it.title.contains("عرض") }
            "SYSTEM" -> allNotifications.filter { it.notificationType == "SYSTEM" || it.notificationType == "ADMIN" }
            else -> allNotifications
        }
    }

    // Counts Map
    val countsMap = remember(allNotifications, readIds) {
        mapOf(
            "ALL" to allNotifications.size,
            "UNREAD" to allNotifications.count { !readIds.contains(it.id) },
            "BOOKING" to allNotifications.count { it.notificationType == "BOOKING" || it.title.contains("حجز") },
            "MESSAGE" to allNotifications.count { it.notificationType == "MESSAGE" || it.title.contains("رسالة") },
            "SPECIAL_OFFER" to allNotifications.count { it.notificationType == "SPECIAL_OFFER" || it.title.contains("عرض") },
            "SYSTEM" to allNotifications.count { it.notificationType == "SYSTEM" || it.notificationType == "ADMIN" }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مركز الإشعارات",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (allNotifications.isNotEmpty()) {
                        IconButton(
                            onClick = { notifViewModel.markAllAsRead(context, allNotifications) }
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "تحديد الكل كمقروء",
                                tint = Color(0xFF00E5FF)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Category Filter Bar
            NotificationsFilterBar(
                selectedCategory = activeTab,
                onSelectCategory = { notifViewModel.setActiveTab(it) },
                countsMap = countsMap,
                themeColors = themeColors
            )

            // Content Area
            if (filteredList.isEmpty()) {
                NotificationEmptyState(
                    activeTab = activeTab,
                    themeColors = themeColors,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                NotificationsList(
                    notifications = filteredList,
                    readIds = readIds,
                    onNotificationClick = { notif ->
                        notifViewModel.markNotificationAsRead(context, notif.id)
                        onNotificationClick(notif)
                    },
                    onDeleteNotification = { notif ->
                        notifViewModel.deleteNotification(notif.id)
                    },
                    themeColors = themeColors
                )
            }
        }
    }
}
