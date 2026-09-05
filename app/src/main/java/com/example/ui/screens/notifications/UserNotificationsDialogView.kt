@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui.screens.notifications

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NotificationEntity
import com.example.ui.MainViewModel
import com.example.ui.components.ConfirmationDialog
import com.example.ui.screens.notifications.components.NotificationEmptyState
import com.example.ui.screens.notifications.components.NotificationFilterTabs
import com.example.ui.screens.notifications.components.NotificationItemCard
import com.example.ui.screens.notifications.components.NotificationLoadingState
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

/**
 * 🔔 UserNotificationsBottomSheet
 * Modern Material 3 Modal Bottom Sheet with glassy surface, derivedStateOf filtering,
 * batch read operations, snackbars, and category chips.
 */
@Composable
fun UserNotificationsBottomSheet(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val notifViewModel = remember(viewModel) { NotificationViewModel(viewModel) }

    // Flow Collections via NotificationViewModel
    val allNotifications by notifViewModel.notifications.collectAsState()
    val userPhone by notifViewModel.currentUserPhone.collectAsState()
    val userId by notifViewModel.currentUserId.collectAsState()
    val adminRole by notifViewModel.adminRole.collectAsState()
    val readIds by notifViewModel.readNotificationIds.collectAsState()

    val activeTab by notifViewModel.activeTab.collectAsState()
    val selectedTypeFilter by notifViewModel.selectedTypeFilter.collectAsState()
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        notifViewModel.loadReadNotifications(context)
    }

    // High performance filtering using derivedStateOf
    val validAndFilteredNotifs by remember {
        derivedStateOf {
            val cleanPhone = userPhone.trim().replace(" ", "").replace("+", "")
            val cleanUserId = userId.trim()
            val provPhone = viewModel.selectedProvider?.phone?.trim()?.replace(" ", "")?.replace("+", "") ?: ""
            val provId = viewModel.selectedProvider?.id ?: ""
            val isAdmin = adminRole == "OWNER" || adminRole == "SUPER_ADMIN" || adminRole == "ADMIN" || adminRole == "SUPERVISOR"
            val seenKeys = mutableSetOf<String>()

            allNotifications.filter { notif ->
                // 1. Strict Validation Check
                if (!notif.isValid()) return@filter false

                // 2. Deduplication check
                val dKey = if (notif.dedupKey.isNotBlank()) notif.dedupKey else "${notif.notificationType}_${notif.title}_${notif.timestamp / (30 * 1000L)}"
                if (!seenKeys.add(dKey) && notif.id.isBlank()) return@filter false

                // 3. Security & Sensitivity Check
                val isSensitive = notif.title.contains("كلمة مرور") || notif.message.contains("كلمة المرور") || 
                                  notif.title.contains("استعادة") || notif.title.contains("رمز التحقق")
                if (isSensitive) {
                    val isMyTarget = (cleanPhone.isNotEmpty() && notif.targetValue.contains(cleanPhone)) ||
                                     (cleanUserId.isNotEmpty() && notif.targetUserIds.contains(cleanUserId)) ||
                                     (provPhone.isNotEmpty() && notif.targetValue.contains(provPhone)) ||
                                     (provId.isNotEmpty() && notif.targetValue.contains(provId))
                    if (!isAdmin && !isMyTarget) return@filter false
                }

                // 4. Role & Audience Targeting Logic
                if (isAdmin) return@filter true

                val isRegistered = cleanPhone.isNotEmpty() || cleanUserId.isNotEmpty()
                // Rule: Guests do not receive targeted notifications
                if (!isRegistered && notif.targetAudience != "ALL") return@filter false

                when (notif.targetAudience.uppercase()) {
                    "ADMIN_ONLY" -> false
                    "ALL_REGISTERED_USERS" -> isRegistered
                    "SPECIFIC_ROLES", "ROLE" -> {
                        val isProvider = notifViewModel.isProviderUser
                        notif.targetRoles.any { r ->
                            when (r.uppercase()) {
                                "TECHNICIAN", "PROVIDER" -> isProvider
                                "STORE" -> isProvider && viewModel.selectedStore != null
                                "MEDICAL" -> isProvider && viewModel.selectedStore?.sectionId?.contains("medical") == true
                                "RESTAURANT" -> isProvider && viewModel.selectedStore?.sectionId?.contains("restaurant") == true
                                "REAL_ESTATE" -> isProvider && viewModel.selectedProperty != null
                                "USER" -> isRegistered
                                else -> false
                            }
                        }
                    }
                    "SPECIFIC_USERS", "SPECIFIC_USER" -> {
                        (cleanPhone.isNotEmpty() && (notif.targetValue.contains(cleanPhone) || notif.targetUserIds.contains(cleanPhone))) ||
                        (cleanUserId.isNotEmpty() && notif.targetUserIds.contains(cleanUserId)) ||
                        (provPhone.isNotEmpty() && notif.targetValue.contains(provPhone)) ||
                        (provId.isNotEmpty() && notif.targetValue.contains(provId))
                    }
                    "REGION" -> {
                        val currentRes = notifViewModel.currentUserResidence.value
                        notif.targetValue.isEmpty() || currentRes.contains(notif.targetValue)
                    }
                    "CATEGORY" -> true
                    "ALL" -> {
                        when (notif.targetType) {
                            "ALL" -> notif.targetAudience != "ADMIN_ONLY"
                            "USER" -> notif.targetValue.isEmpty() || (cleanPhone.isNotEmpty() && notif.targetValue.contains(cleanPhone))
                            "PROVIDER" -> (cleanPhone.isNotEmpty() && (notif.targetValue.contains(cleanPhone) || notif.targetUserIds.contains(cleanPhone))) ||
                                          (cleanUserId.isNotEmpty() && (notif.targetValue.contains(cleanUserId) || notif.targetUserIds.contains(cleanUserId))) ||
                                          (provPhone.isNotEmpty() && (notif.targetValue.contains(provPhone) || notif.targetUserIds.contains(provPhone))) ||
                                          (provId.isNotEmpty() && (notif.targetValue.contains(provId) || notif.targetUserIds.contains(provId)))
                            "SUPERVISOR", "ADMIN_ONLY" -> false
                            else -> true
                        }
                    }
                    else -> false
                }
            }.distinctBy { it.id.ifBlank { "${it.title}_${it.timestamp}" } }
        }
    }

    val unreadCount by remember {
        derivedStateOf {
            validAndFilteredNotifs.count { !readIds.contains(it.id) }
        }
    }

    val finalNotifs by remember {
        derivedStateOf {
            validAndFilteredNotifs.filter { notif ->
                val matchesTab = when (activeTab) {
                    "READ" -> readIds.contains(notif.id)
                    "UNREAD" -> !readIds.contains(notif.id)
                    "IMPORTANT" -> notif.notificationType == "BOOKING" || notif.notificationType == "ADMIN" || notif.title.contains("عاجل") || notif.title.contains("مهم")
                    else -> true
                }
                val matchesType = when (selectedTypeFilter) {
                    "BOOKING" -> notif.notificationType == "BOOKING" || notif.title.contains("حجز")
                    "MESSAGE" -> notif.notificationType == "MESSAGE" || notif.title.contains("دردشة") || notif.title.contains("رسالة")
                    "SPECIAL_OFFER" -> notif.notificationType == "SPECIAL_OFFER" || notif.title.contains("عرض")
                    "SYSTEM" -> notif.notificationType == "SYSTEM" || notif.notificationType == "ADMIN"
                    else -> true
                }
                matchesTab && matchesType
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A).copy(alpha = 0.95f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = themeColors.accent.copy(alpha = 0.6f))
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color.White,
                        actionColor = themeColors.accent,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع إلى الشاشة الرئيسية",
                                tint = Color.White
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🔔", fontSize = 18.sp)
                                Text(
                                    "مركز الإشعارات الذكية",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                            if (unreadCount > 0) {
                                Text(
                                    "$unreadCount إشعار غير مقروء",
                                    fontSize = 11.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Mark All as Read Button
                        if (unreadCount > 0) {
                            FilledTonalButton(
                                onClick = {
                                    notifViewModel.markAllAsRead(context, validAndFilteredNotifs)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("تم تحديد جميع الإشعارات كمقروءة بنجاح ✓")
                                    }
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = themeColors.accent.copy(alpha = 0.15f),
                                    contentColor = themeColors.accent
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("mark_all_read_btn")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تحديد الكل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Clear All Button
                        if (validAndFilteredNotifs.isNotEmpty()) {
                            IconButton(
                                onClick = { showClearAllConfirmDialog = true },
                                modifier = Modifier.testTag("clear_all_notifs_btn")
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "مسح الكل",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Tabs & Category Filter Chips
                val tabCounts = remember(validAndFilteredNotifs.size, unreadCount) {
                    listOf(
                        Triple("ALL", "الكل", validAndFilteredNotifs.size),
                        Triple("UNREAD", "غير مقروءة", unreadCount),
                        Triple("IMPORTANT", "الهامة ⭐", validAndFilteredNotifs.count { it.notificationType == "BOOKING" || it.title.contains("عاجل") }),
                        Triple("READ", "مقروءة", validAndFilteredNotifs.size - unreadCount)
                    )
                }

                NotificationFilterTabs(
                    activeTab = activeTab,
                    onTabSelected = { notifViewModel.setActiveTab(it) },
                    tabCounts = tabCounts,
                    selectedTypeFilter = selectedTypeFilter,
                    onTypeFilterSelected = { notifViewModel.setSelectedTypeFilter(it) },
                    themeColors = themeColors
                )

                // Main List or Empty State
                if (finalNotifs.isEmpty()) {
                    NotificationEmptyState(
                        activeTab = activeTab,
                        themeColors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(
                            items = finalNotifs,
                            key = { it.id.ifBlank { "${it.title}_${it.timestamp}" } }
                        ) { notif ->
                            NotificationItemCard(
                                notification = notif,
                                isUnread = !readIds.contains(notif.id),
                                onCardClick = {
                                    notifViewModel.markNotificationAsRead(context, notif.id)
                                },
                                onDeleteClick = {
                                    notifViewModel.deleteNotification(notif.id)
                                    coroutineScope.launch {
                                        val res = snackbarHostState.showSnackbar(
                                            message = "تم حذف الإشعار",
                                            actionLabel = "تراجع"
                                        )
                                        if (res == SnackbarResult.ActionPerformed) {
                                            notifViewModel.addNotification(
                                                title = notif.title,
                                                message = notif.message,
                                                targetType = notif.targetType,
                                                targetValue = notif.targetValue,
                                                targetAudience = notif.targetAudience,
                                                targetRoles = notif.targetRoles,
                                                targetUserIds = notif.targetUserIds,
                                                notificationType = notif.notificationType
                                            )
                                        }
                                    }
                                },
                                themeColors = themeColors
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Clearing All Notifications
    ConfirmationDialog(
        isOpen = showClearAllConfirmDialog,
        title = "مسح جميع الإشعارات",
        message = "هل أنت متأكد من رغبتك في مسح كافة الإشعارات الظاهرة؟ لا يمكن التراجع عن هذا الإجراء.",
        confirmLabel = "مسح الكل",
        cancelLabel = "إلغاء",
        isDestructive = true,
        onConfirm = {
            notifViewModel.deleteAllNotifications()
            coroutineScope.launch {
                snackbarHostState.showSnackbar("تم مسح جميع الإشعارات بنجاح 🗑️")
            }
        },
        onDismiss = { showClearAllConfirmDialog = false }
    )
}

/**
 * Compatibility wrapper for UserNotificationsDialogView
 */
@Composable
fun UserNotificationsDialogView(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    UserNotificationsBottomSheet(
        viewModel = viewModel,
        themeColors = themeColors,
        onDismiss = onDismiss
    )
}

/**
 * Fullscreen Route variant for notifications
 */
@Composable
fun UserNotificationsScreen(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBack: () -> Unit
) {
    UserNotificationsBottomSheet(
        viewModel = viewModel,
        themeColors = themeColors,
        onDismiss = onBack
    )
}
