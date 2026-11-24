package com.example.ui.screens.dashboard

import android.widget.Toast
import androidx.activity.compose.BackHandler
import com.example.ui.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.data.MallEntity
import com.example.data.StoreEntity
import com.example.data.UnifiedBusinessAccount
import com.example.data.repositories.MallRepository
import com.example.data.repositories.MallTenantItem
import com.example.domain.entities.DashboardStatsEntity
import com.example.ui.MainViewModel
import com.example.ui.helpers.AppPreferenceHelper
import com.example.ui.screens.dashboard.components.UnifiedEmptyState
import com.example.ui.screens.dashboard.components.UnifiedLoadingIndicator
import com.example.ui.screens.dashboard.viewmodels.StoreDashboardViewModel
import com.example.utils.BookingStatus
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 🏬 MallDashboard
 * لوحة تحكم مخصصة للمراكز التجارية والمولات:
 * إدارة عدد الطوابق، دليل المحلات المستأجرة داخل كل دور، العروض، التقييمات، وإعدادات المول.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MallDashboard(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit,
    initialTab: Int = 0,
    mallViewModel: StoreDashboardViewModel = hiltViewModel()
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 5)) }

    val stores by viewModel.stores.collectAsState()
    val liveAccount = remember(stores, account) {
        stores.find { it.id == account.id }
            ?.let { UnifiedBusinessAccount.fromStore(it, "malls") }
            ?: account
    }

    LaunchedEffect(liveAccount.id) {
        mallViewModel.initialize(liveAccount.id)
    }

    val uiState by mallViewModel.uiState.collectAsState()
    val allBookings by viewModel.bookings.collectAsState()

    val cleanAccPhone = remember(liveAccount.phone) {
        AppPreferenceHelper.normalizePhoneNumber(liveAccount.phone)
    }
    val mallOrders = remember(allBookings, liveAccount.id, cleanAccPhone) {
        allBookings.filter { b ->
            b.providerId == liveAccount.id ||
                (cleanAccPhone.isNotBlank() &&
                    AppPreferenceHelper.normalizePhoneNumber(b.providerPhone) == cleanAccPhone)
        }
    }

    var mallEntity by remember(liveAccount.id) {
        mutableStateOf(
            MallRepository.defaultMallFromStore(
                liveAccount.rawStore ?: StoreEntity(
                    id = liveAccount.id,
                    sectionId = "malls",
                    name = liveAccount.name,
                    description = liveAccount.description,
                    phone = liveAccount.phone,
                    cityId = liveAccount.cityId,
                    localNeighborhood = liveAccount.neighborhood,
                    coverImage = liveAccount.coverImage,
                    logoImage = liveAccount.logoImage,
                    workingHours = liveAccount.workingHours,
                    latitude = liveAccount.latitude,
                    longitude = liveAccount.longitude
                )
            )
        )
    }
    var tenants by remember(liveAccount.id) { mutableStateOf<List<MallTenantItem>>(emptyList()) }
    var floorsCount by remember(liveAccount.id) { mutableIntStateOf(3) }
    var isMallLoading by remember(liveAccount.id) { mutableStateOf(true) }
    var isSavingMall by remember { mutableStateOf(false) }
    var selectedFloorFilter by remember { mutableIntStateOf(-1) }
    var showAddTenantDialog by remember { mutableStateOf(false) }

    LaunchedEffect(liveAccount.id) {
        isMallLoading = true
        val storeSource = liveAccount.rawStore ?: StoreEntity(
            id = liveAccount.id,
            sectionId = "malls",
            name = liveAccount.name,
            description = liveAccount.description,
            phone = liveAccount.phone,
            cityId = liveAccount.cityId,
            localNeighborhood = liveAccount.neighborhood,
            coverImage = liveAccount.coverImage,
            logoImage = liveAccount.logoImage,
            workingHours = liveAccount.workingHours,
            latitude = liveAccount.latitude,
            longitude = liveAccount.longitude
        )
        val resolved = MallRepository.getOrResolveMall(storeSource)
        mallEntity = resolved
        floorsCount = resolved.floorsCount.coerceIn(1, 20)
        tenants = MallRepository.parseTenantsJson(resolved.floorsDirectoryJson)
        isMallLoading = false
    }

    LaunchedEffect(mallViewModel) {
        mallViewModel.eventFlow.collect { event ->
            when (event) {
                is DashboardEvent.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                else -> {}
            }
        }
    }

    val computedStats = remember(uiState.stats, mallOrders, uiState.reviews, tenants, liveAccount) {
        val activeCount = mallOrders.count {
            it.status.uppercase() in listOf(
                BookingStatus.PENDING.code,
                BookingStatus.ACCEPTED.code,
                BookingStatus.IN_PROGRESS.code,
                "IN_PREPARATION",
                "READY"
            )
        }
        val completedCount = mallOrders.count {
            it.status.uppercase() in listOf(BookingStatus.COMPLETED.code, BookingStatus.PAID.code, BookingStatus.CLOSED.code, "DELIVERED")
        }
        val revenue = mallOrders
            .filter { it.status.uppercase() in listOf(BookingStatus.COMPLETED.code, BookingStatus.PAID.code, BookingStatus.CLOSED.code, "DELIVERED") }
            .sumOf { if (it.totalAmount > 0.0) it.totalAmount else it.price }
        val avgRating = if (uiState.reviews.isNotEmpty()) {
            uiState.reviews.map { it.rating }.average()
        } else {
            liveAccount.rating.toDouble().takeIf { it > 0.0 } ?: 5.0
        }
        DashboardStatsEntity(
            totalViews = uiState.stats.totalViews,
            activeBookingsCount = maxOf(uiState.stats.activeBookingsCount, activeCount),
            completedBookingsCount = maxOf(uiState.stats.completedBookingsCount, completedCount),
            averageRating = avgRating,
            totalReviewsCount = maxOf(uiState.stats.totalReviewsCount, maxOf(uiState.reviews.size, liveAccount.numReviews)),
            totalRevenueYer = maxOf(uiState.stats.totalRevenueYer, revenue),
            totalProductsCount = maxOf(uiState.stats.totalProductsCount, tenants.size),
            pendingRequestsCount = mallOrders.count { it.status.equals(BookingStatus.PENDING.code, ignoreCase = true) }
        )
    }

    val openClientChat: (String) -> Unit = { clientIdOrPhone ->
        val matched = mallOrders.find {
            it.clientId == clientIdOrPhone ||
                it.customerPhone == clientIdOrPhone ||
                it.clientPhone == clientIdOrPhone
        }
        val targetName = matched?.customerName?.ifBlank { matched.clientName }
            ?.ifBlank { "العميل" } ?: "العميل"
        val targetPhone = matched?.customerPhone?.ifBlank { matched.clientPhone }
            ?.ifBlank { clientIdOrPhone } ?: clientIdOrPhone
        viewModel.openOrCreateChatChannel(
            targetId = clientIdOrPhone.ifBlank { targetPhone },
            targetType = "CLIENT",
            targetName = targetName,
            targetPhone = targetPhone,
            relatedEntityId = liveAccount.id,
            relatedEntityType = "STORE",
            onCreated = { channel ->
                if (channel != null) {
                    viewModel.targetChatChannelId = channel.id
                    viewModel.targetChatUserId = clientIdOrPhone.ifBlank { targetPhone }
                    viewModel.targetChatUserName = targetName
                    viewModel.targetChatRelatedEntityId = liveAccount.id
                    viewModel.targetChatRelatedEntityType = "STORE"
                    viewModel.navigateToScreen(com.example.ui.AppScreens.CHAT_DIRECT)
                }
            }
        )
    }

    val tabs = listOf(
        "طوابق ومحلات المول 🏬",
        "العروض 🔥",
        "الطلبات 📦",
        "التقييمات ⭐",
        "الإحصائيات 📊",
        "الإعدادات ⚙️"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "لوحة إدارة المركز التجاري 🏬",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.textPrimary
                        )
                        Text(
                            text = "${liveAccount.name} • $floorsCount طوابق • ${tenants.size} محل",
                            fontSize = 11.sp,
                            color = themeColors.textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("mall_dashboard_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = themeColors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = themeColors.surface)
            )
        },
        containerColor = themeColors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = themeColors.surface,
                contentColor = themeColors.accent,
                edgePadding = 12.dp
            ) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == index) themeColors.accent else themeColors.textSecondary
                            )
                        }
                    )
                }
            }

            if (uiState.isLoading && isMallLoading) {
                UnifiedLoadingIndicator(themeColors = themeColors)
            } else {
                when (activeTab) {
                    0 -> MallDirectoryTab(
                        mall = mallEntity,
                        floorsCount = floorsCount,
                        tenants = tenants,
                        selectedFloorFilter = selectedFloorFilter,
                        isSaving = isSavingMall,
                        themeColors = themeColors,
                        onSelectFloor = { selectedFloorFilter = it },
                        onFloorsCountChange = { newCount ->
                            val clamped = newCount.coerceIn(1, 20)
                            floorsCount = clamped
                            isSavingMall = true
                            scope.launch {
                                val res = MallRepository.saveMallDirectory(mallEntity, tenants, clamped)
                                isSavingMall = false
                                res.onSuccess { mallEntity = it }
                            }
                        },
                        onOpenAddTenant = { showAddTenantDialog = true },
                        onDeleteTenant = { itemId ->
                            val updated = tenants.filterNot { it.id == itemId }
                            tenants = updated
                            isSavingMall = true
                            scope.launch {
                                val res = MallRepository.saveMallDirectory(mallEntity, updated, floorsCount)
                                isSavingMall = false
                                res.onSuccess {
                                    mallEntity = it
                                    Toast.makeText(context, "تم حذف المحل من الدليل 🗑️", Toast.LENGTH_SHORT).show()
                                }.onFailure {
                                    Toast.makeText(context, "تعذر الحفظ الآن، يرجى المحاولة", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                    1 -> TabOffersCoupons(storeId = liveAccount.id, themeColors = themeColors)
                    2 -> TabBookingsOrders(
                        bookings = mallOrders,
                        themeColors = themeColors,
                        onAcceptBooking = { bId -> viewModel.updateBookingStatus(bId, "IN_PREPARATION") },
                        onRejectBooking = { bId, reason -> viewModel.updateBookingStatus(bId, BookingStatus.REJECTED.code, reason) },
                        onStartProgress = { bId -> viewModel.updateBookingStatus(bId, "READY") },
                        onCompleteBooking = { bId -> viewModel.updateBookingStatus(bId, "DELIVERED") },
                        onChatWithClient = openClientChat,
                        onUpdateOrderStatus = { bId, status ->
                            viewModel.updateBookingStatus(bId, status)
                            Toast.makeText(context, "تم تحديث حالة الطلب ✅", Toast.LENGTH_SHORT).show()
                        }
                    )
                    3 -> TabReviewsFeedback(
                        reviews = uiState.reviews,
                        themeColors = themeColors,
                        onReplySubmit = { revId, reply -> mallViewModel.replyToReview(revId, reply) }
                    )
                    4 -> TabStatisticsGrowth(stats = computedStats, themeColors = themeColors)
                    5 -> TabProfileEdit(
                        name = liveAccount.name,
                        phone = liveAccount.phone,
                        cityArea = liveAccount.neighborhood.ifBlank { liveAccount.cityId },
                        description = liveAccount.description,
                        workingHours = liveAccount.workingHours,
                        photoUrl = liveAccount.logoImage,
                        coverUrl = liveAccount.coverImage,
                        isAvailable = liveAccount.rawStore?.isActive ?: liveAccount.isActive,
                        rating = liveAccount.rating.toDouble(),
                        reviewCount = liveAccount.numReviews,
                        latitude = liveAccount.latitude,
                        longitude = liveAccount.longitude,
                        themeColors = themeColors,
                        onChangePhoto = { url ->
                            mallViewModel.updateProfile(
                                name = liveAccount.name,
                                cityArea = liveAccount.neighborhood.ifBlank { liveAccount.cityId },
                                description = liveAccount.description,
                                workingHours = liveAccount.workingHours,
                                isAvailable = liveAccount.rawStore?.isActive ?: liveAccount.isActive,
                                photoUrl = url
                            )
                        },
                        onChangeCover = { url ->
                            mallViewModel.updateProfile(
                                name = liveAccount.name,
                                cityArea = liveAccount.neighborhood.ifBlank { liveAccount.cityId },
                                description = liveAccount.description,
                                workingHours = liveAccount.workingHours,
                                isAvailable = liveAccount.rawStore?.isActive ?: liveAccount.isActive,
                                coverUrl = url
                            )
                        },
                        onSaveLocation = { lat, lng ->
                            mallViewModel.updateProfile(
                                name = liveAccount.name,
                                cityArea = liveAccount.neighborhood.ifBlank { liveAccount.cityId },
                                description = liveAccount.description,
                                workingHours = liveAccount.workingHours,
                                isAvailable = liveAccount.rawStore?.isActive ?: liveAccount.isActive,
                                latitude = lat,
                                longitude = lng
                            )
                        },
                        onSaveProfileWithLocation = { name, editedPhone, city, desc, hours, available, lat, lng ->
                            mallViewModel.updateProfile(
                                name = name,
                                cityArea = city,
                                description = desc,
                                workingHours = hours,
                                isAvailable = available,
                                phone = editedPhone,
                                latitude = lat,
                                longitude = lng
                            )
                        },
                        onSaveProfile = { name, editedPhone, city, desc, hours, available ->
                            mallViewModel.updateProfile(name, city, desc, hours, available, phone = editedPhone)
                        },
                        onChangePassword = { oldP, newP -> mallViewModel.changePassword(oldP, newP) }
                    )
                }
            }
        }
    }

    if (showAddTenantDialog) {
        AddMallTenantDialog(
            maxFloors = floorsCount,
            themeColors = themeColors,
            onDismiss = { showAddTenantDialog = false },
            onAdd = { newItem ->
                val updated = tenants + newItem
                tenants = updated
                showAddTenantDialog = false
                isSavingMall = true
                scope.launch {
                    val res = MallRepository.saveMallDirectory(mallEntity, updated, floorsCount)
                    isSavingMall = false
                    res.onSuccess {
                        mallEntity = it
                        Toast.makeText(context, "تمت إضافة المحل إلى دليل المول ✅", Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(context, "فشل حفظ الدليل", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun MallDirectoryTab(
    mall: MallEntity,
    floorsCount: Int,
    tenants: List<MallTenantItem>,
    selectedFloorFilter: Int,
    isSaving: Boolean,
    themeColors: VisualThemePalette,
    onSelectFloor: (Int) -> Unit,
    onFloorsCountChange: (Int) -> Unit,
    onOpenAddTenant: () -> Unit,
    onDeleteTenant: (String) -> Unit
) {
    val visibleTenants = remember(tenants, selectedFloorFilter) {
        if (selectedFloorFilter < 0) tenants else tenants.filter { it.floorNumber == selectedFloorFilter }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, themeColors.border)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "هيكل الطوابق (${mall.floorsCount.coerceAtLeast(floorsCount)} طوابق)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.textPrimary
                            )
                            Text(
                                text = "حدّد عدد الطوابق لتنظيم المحلات للزوار",
                                fontSize = 11.sp,
                                color = themeColors.textSecondary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { if (floorsCount > 1) onFloorsCountChange(floorsCount - 1) },
                                enabled = !isSaving && floorsCount > 1,
                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("−", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
                            }
                            Text(
                                text = "$floorsCount",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.accent
                            )
                            OutlinedButton(
                                onClick = { if (floorsCount < 20) onFloorsCountChange(floorsCount + 1) },
                                enabled = !isSaving && floorsCount < 20,
                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
                            }
                        }
                    }

                    Button(
                        onClick = onOpenAddTenant,
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("mall_add_tenant_btn")
                    ) {
                        Text("إضافة محل مستأجر جديد 🏪", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedFloorFilter == -1,
                        onClick = { onSelectFloor(-1) },
                        label = { Text("الكل (${tenants.size})", fontSize = 11.5.sp) }
                    )
                }
                items((1..floorsCount).toList()) { floor ->
                    val count = tenants.count { it.floorNumber == floor }
                    FilterChip(
                        selected = selectedFloorFilter == floor,
                        onClick = { onSelectFloor(floor) },
                        label = { Text("${MallRepository.defaultFloorLabel(floor)} ($count)", fontSize = 11.5.sp) }
                    )
                }
            }
        }

        if (visibleTenants.isEmpty()) {
            item {
                UnifiedEmptyState(
                    title = "دليل المحلات فارغ حالياً",
                    description = "أضف المحلات والمطاعم والماركات الموجودة داخل المول مع رقم الدور والوحدة ليسهل على الزوار الوصول إليها.",
                    iconText = "🏬",
                    actionLabel = "إضافة محل الآن",
                    onActionClick = onOpenAddTenant,
                    themeColors = themeColors
                )
            }
        } else {
            items(visibleTenants, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, themeColors.border)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = item.shopName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.textPrimary
                            )
                            Text(
                                text = "${item.floorLabel.ifBlank { MallRepository.defaultFloorLabel(item.floorNumber) }}${if (item.unitCode.isNotBlank()) " • وحدة ${item.unitCode}" else ""}",
                                fontSize = 11.5.sp,
                                color = themeColors.accent,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (item.category.isNotBlank()) {
                                Text(text = "النشاط: ${item.category}", fontSize = 11.sp, color = themeColors.textSecondary)
                            }
                            if (item.phone.isNotBlank()) {
                                Text(text = "الهاتف: ${item.phone}", fontSize = 11.sp, color = themeColors.textSecondary)
                            }
                        }
                        IconButton(onClick = { onDeleteTenant(item.id) }) {
                            Text("🗑️", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddMallTenantDialog(
    maxFloors: Int,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit,
    onAdd: (MallTenantItem) -> Unit
) {
    var shopName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var unitCode by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var floorNumber by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة محل لدليل المول 🏪", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("اسم المحل / العلامة التجارية") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("التصنيف (ملابس، عطور، إلكترونيات...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الدور: ${MallRepository.defaultFloorLabel(floorNumber)}", fontSize = 12.sp, color = themeColors.textPrimary, modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = { if (floorNumber > 1) floorNumber-- },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("−") }
                    OutlinedButton(
                        onClick = { if (floorNumber < maxFloors) floorNumber++ },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("+") }
                }
                OutlinedTextField(
                    value = unitCode,
                    onValueChange = { unitCode = it },
                    label = { Text("رقم البوابة / المحل (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم تواصل المحل (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (shopName.isNotBlank()) {
                        onAdd(
                            MallTenantItem(
                                id = UUID.randomUUID().toString().take(8),
                                floorNumber = floorNumber,
                                floorLabel = MallRepository.defaultFloorLabel(floorNumber),
                                shopName = shopName.trim(),
                                category = category.trim(),
                                unitCode = unitCode.trim(),
                                phone = phone.trim()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
            ) {
                Text("حفظ المحل", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
