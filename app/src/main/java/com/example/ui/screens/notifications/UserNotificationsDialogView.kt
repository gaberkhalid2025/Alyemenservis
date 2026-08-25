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

    // Direct Flow Collection without remember
    val allNotifications by viewModel.notifications.collectAsState()
    val userPhone by viewModel.currentUserPhone.collectAsState()
    val userId by viewModel.currentUserId.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val readIds by viewModel.readNotificationIds.collectAsState()

    var activeTab by remember { mutableStateOf("ALL") } // "ALL", "UNREAD", "IMPORTANT", "READ"
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "BOOKING", "MESSAGE", "SPECIAL_OFFER", "SYSTEM"
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadReadNotifications(context)
    }

    // High performance filtering using derivedStateOf
    val validAndFilteredNotifs by remember {
        derivedStateOf {
            val cleanPhone = userPhone.trim().replace(" ", "").replace("+", "")
            val cleanUserId = userId.trim()
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
                                     (cleanUserId.isNotEmpty() && notif.targetUserIds.contains(cleanUserId))
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
                        val isProvider = viewModel.isProviderUser
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
                        (cleanUserId.isNotEmpty() && notif.targetUserIds.contains(cleanUserId))
                    }
                    "REGION" -> {
                        val currentRes = viewModel.currentUserResidence.value
                        notif.targetValue.isEmpty() || currentRes.contains(notif.targetValue)
                    }
                    "CATEGORY" -> true
                    "ALL" -> {
                        when (notif.targetType) {
                            "ALL" -> true
                            "USER" -> notif.targetValue.isEmpty() || (cleanPhone.isNotEmpty() && notif.targetValue.contains(cleanPhone))
                            "PROVIDER" -> cleanPhone.isNotEmpty() && notif.targetValue.contains(cleanPhone)
                            "SUPERVISOR" -> false
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
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🔔", fontSize = 18.sp)
                            Text(
                                "مركز الإشعارات الذكية",
                                fontSize = 16.5.sp,
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

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Mark All as Read Button
                        if (unreadCount > 0) {
                            FilledTonalButton(
                                onClick = {
                                    validAndFilteredNotifs.forEach { notif ->
                                        viewModel.markNotificationAsRead(context, notif.id)
                                    }
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
                    onTabSelected = { activeTab = it },
                    tabCounts = tabCounts,
                    selectedTypeFilter = selectedTypeFilter,
                    onTypeFilterSelected = { selectedTypeFilter = it },
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
                                    viewModel.markNotificationAsRead(context, notif.id)
                                },
                                onDeleteClick = {
                                    viewModel.deleteNotification(notif.id)
                                    coroutineScope.launch {
                                        val res = snackbarHostState.showSnackbar(
                                            message = "تم حذف الإشعار",
                                            actionLabel = "تراجع"
                                        )
                                        if (res == SnackbarResult.ActionPerformed) {
                                            viewModel.addNotification(
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
            viewModel.deleteAllNotifications()
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
