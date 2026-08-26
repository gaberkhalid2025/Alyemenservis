package com.example.ui.screens.status

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookingEntity
import com.example.data.NotificationEntity
import com.example.data.PendingProviderEntity
import com.example.data.InstantRequestEntity
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

enum class StatusTab(val title: String, val icon: String) {
    OVERVIEW("نظرة عامة", "📊"),
    JOIN_REQUESTS("طلبات الانضمام", "📝"),
    BOOKINGS("الحجوزات والطلبات", "📋"),
    NOTIFICATIONS("الإشعارات والتنبيهات", "🔔")
}

/**
 * 📊 StatusScreen
 * شاشة ملخص حالة المنصة الشاملة (الحجوزات، الإشعارات، طلبات الانضمام، إحصائيات النظام).
 */
@Composable
fun StatusScreen(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: (() -> Unit)? = null
) {
    val providers by viewModel.providers.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val instantRequests by viewModel.instantRequests.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val pendingProviders by viewModel.pendingProviders.collectAsState()

    var selectedTab by remember { mutableStateOf(StatusTab.OVERVIEW) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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

                        IconButton(
                            onClick = {
                                viewModel.refreshData()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("🔄 تم تحديث حالات وبيانات المنصة")
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "تحديث",
                                tint = themeColors.accent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tab Selector Row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(StatusTab.values()) { tab ->
                            val isSelected = tab == selectedTab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) themeColors.accent else themeColors.background)
                                    .border(
                                        1.dp,
                                        if (isSelected) themeColors.accent else themeColors.border,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedTab = tab }
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
            when (selectedTab) {
                StatusTab.OVERVIEW -> {
                    StatusOverviewContent(
                        providersCount = providers.size,
                        storesCount = stores.size,
                        propertiesCount = properties.size,
                        instantRequestsCount = instantRequests.size,
                        bookingsCount = bookings.size,
                        joinRequestsCount = pendingProviders.size,
                        themeColors = themeColors
                    )
                }
                StatusTab.JOIN_REQUESTS -> {
                    StatusJoinRequestsContent(
                        requests = pendingProviders,
                        themeColors = themeColors,
                        onApprove = { req ->
                            viewModel.approvePendingProvider(req)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("✅ تم قبول طلب انضمام: ${req.name}")
                            }
                        },
                        onReject = { req ->
                            viewModel.rejectPendingProvider(req)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("❌ تم رفض طلب انضمام: ${req.name}")
                            }
                        }
                    )
                }
                StatusTab.BOOKINGS -> {
                    StatusBookingsContent(
                        bookings = bookings,
                        instantRequests = instantRequests,
                        themeColors = themeColors
                    )
                }
                StatusTab.NOTIFICATIONS -> {
                    StatusNotificationsContent(
                        notifications = notifications,
                        themeColors = themeColors,
                        onClearAll = {
                            viewModel.clearAllNotifications()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("🧹 تم مسح كافة الإشعارات")
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * 📊 Overview Content
 */
@Composable
private fun StatusOverviewContent(
    providersCount: Int,
    storesCount: Int,
    propertiesCount: Int,
    instantRequestsCount: Int,
    bookingsCount: Int,
    joinRequestsCount: Int,
    themeColors: VisualThemePalette
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "📈 ملخص أداء النظام والبيانات الحية",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.textPrimary
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "الفنيين والمهنيين",
                    value = "$providersCount",
                    icon = "👨‍🔧",
                    color = Color(0xFF3B82F6),
                    themeColors = themeColors,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "المتاجر والمراكز",
                    value = "$storesCount",
                    icon = "🏬",
                    color = Color(0xFF10B981),
                    themeColors = themeColors,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "العقارات والمعارض",
                    value = "$propertiesCount",
                    icon = "🏠",
                    color = Color(0xFFF59E0B),
                    themeColors = themeColors,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "الطلبات العاجلة",
                    value = "$instantRequestsCount",
                    icon = "⚡",
                    color = Color(0xFFEF4444),
                    themeColors = themeColors,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "الحجوزات المؤكدة",
                    value = "$bookingsCount",
                    icon = "📋",
                    color = Color(0xFF8B5CF6),
                    themeColors = themeColors,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "طلبات الانضمام المعلقة",
                    value = "$joinRequestsCount",
                    icon = "📝",
                    color = Color(0xFFEC4899),
                    themeColors = themeColors,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: String,
    color: Color,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, themeColors.border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(icon, fontSize = 20.sp)
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themeColors.textPrimary
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = themeColors.textSecondary
            )
        }
    }
}

/**
 * 📝 Join Requests Content
 */
@Composable
private fun StatusJoinRequestsContent(
    requests: List<PendingProviderEntity>,
    themeColors: VisualThemePalette,
    onApprove: (PendingProviderEntity) -> Unit,
    onReject: (PendingProviderEntity) -> Unit
) {
    if (requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📝", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "لا توجد طلبات انضمام معلقة حالياً",
                    fontSize = 13.sp,
                    color = themeColors.textSecondary
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(requests, key = { it.id }) { req ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, themeColors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(req.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = themeColors.textPrimary)
                            Text(req.profession.ifEmpty { req.customCategoryName }, fontSize = 11.sp, color = themeColors.accent)
                        }
                        Text("📱 الهاتف: ${req.phone} | 📍 ${req.area} - ${req.localNeighborhood}", fontSize = 11.sp, color = themeColors.textSecondary)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onApprove(req) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Text("قبول الطلب ✅", color = Color.White, fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { onReject(req) },
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Text("رفض ❌", color = Color(0xFFEF4444), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 📋 Bookings Content
 */
@Composable
private fun StatusBookingsContent(
    bookings: List<BookingEntity>,
    instantRequests: List<InstantRequestEntity>,
    themeColors: VisualThemePalette
) {
    if (bookings.isEmpty() && instantRequests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📋", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "لا توجد حجوزات أو طلبات مسجلة",
                    fontSize = 13.sp,
                    color = themeColors.textSecondary
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (instantRequests.isNotEmpty()) {
                item {
                    Text("⚡ الطلبات العاجلة الحالية:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = themeColors.accent)
                }
                items(instantRequests, key = { "req_${it.id}" }) { req ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(req.serviceTitle.ifEmpty { req.requestCode }, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = themeColors.textPrimary)
                                Text(req.status, fontSize = 10.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                            }
                            Text(req.description, fontSize = 11.sp, color = themeColors.textSecondary)
                        }
                    }
                }
            }

            if (bookings.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("📋 الحجوزات والمواعيد:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = themeColors.accent)
                }
                items(bookings, key = { "b_${it.id}" }) { b ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, themeColors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(b.providerName.ifEmpty { "حجز جديد" }, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = themeColors.textPrimary)
                                Text(b.status, fontSize = 10.sp, color = Color(0xFF10B981))
                            }
                            Text("👤 العميل: ${b.customerName} (${b.customerPhone})", fontSize = 11.sp, color = themeColors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 🔔 Notifications Content
 */
@Composable
private fun StatusNotificationsContent(
    notifications: List<NotificationEntity>,
    themeColors: VisualThemePalette,
    onClearAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔔 الإشعارات الواردة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
            if (notifications.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("مسح الكل 🧹", fontSize = 11.sp, color = Color(0xFFEF4444))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔔", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("لا توجد إشعارات جديدة", fontSize = 13.sp, color = themeColors.textSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, themeColors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = themeColors.accent)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(notif.message, fontSize = 11.sp, color = themeColors.textPrimary)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Backwards compatibility alias
 */
@Composable
fun StatusScreenLayout() {
    val viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val settings by viewModel.settings.collectAsState()
    val themeColors = com.example.utils.resolveThemePalette(settings)
    StatusScreen(viewModel = viewModel, themeColors = themeColors)
}
