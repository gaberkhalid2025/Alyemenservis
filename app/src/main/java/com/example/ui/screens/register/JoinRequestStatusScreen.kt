@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.register

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.ChatChannelEntity
import com.example.data.UnifiedBusinessAccount
import com.example.ui.MainViewModel
import com.example.ui.screens.dashboard.*
import com.example.ui.screens.register.status.*
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

/**
 * 📊 JoinRequestStatusScreen - الشاشة الموحدة لمتابعة حالة طلب الانضمام أو الدخول للوحات التحكم
 */
@Composable
fun JoinRequestStatusScreen(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val joinPhone by viewModel.joinRequestPhone.collectAsState()
    val pendingProviders by viewModel.pendingProviders.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val chatChannels by viewModel.chatChannels.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var activeChatChannel by remember { mutableStateOf<ChatChannelEntity?>(null) }

    val matchingPending = remember(pendingProviders, joinPhone) {
        pendingProviders.find { it.phone.trim() == joinPhone.trim() && joinPhone.isNotEmpty() }
    }
    val matchingApproved = remember(providers, joinPhone) {
        providers.find { it.phone.trim() == joinPhone.trim() && joinPhone.isNotEmpty() }
    }
    val matchingStore = remember(stores, joinPhone) {
        val cleanJoin = joinPhone.trim().replace(" ", "").replace("+", "")
        stores.find {
            (it.ownerId.trim().replace(" ", "").replace("+", "") == cleanJoin ||
                    it.phone.trim().replace(" ", "").replace("+", "") == cleanJoin) &&
                    cleanJoin.isNotEmpty() && !it.isDeleted
        }
    }
    val matchingProperty = remember(properties, joinPhone) {
        val cleanJoin = joinPhone.trim().replace(" ", "").replace("+", "")
        properties.find {
            (it.ownerId.trim().replace(" ", "").replace("+", "") == cleanJoin ||
                    it.phone.trim().replace(" ", "").replace("+", "") == cleanJoin) &&
                    cleanJoin.isNotEmpty() && !it.isDeleted
        }
    }

    // 🏬 1. If Store/Restaurant/Medical center is Active
    if (matchingStore != null && matchingStore.isActive) {
        val isRest = matchingStore.sectionId.contains("restaurant") || matchingStore.name.contains("مطعم")
        val isMed = matchingStore.sectionId.contains("medical") || matchingStore.name.contains("عيادة")
        val acc = UnifiedBusinessAccount.fromStore(matchingStore, if (isRest) "restaurants" else if (isMed) "medical" else "stores")

        if (isRest) {
            RestaurantDashboard(account = acc, viewModel = viewModel, themeColors = themeColors, onBackClick = { viewModel.cancelOrResetJoinRequest(context) })
        } else if (isMed) {
            MedicalDashboard(account = acc, viewModel = viewModel, themeColors = themeColors, onBackClick = { viewModel.cancelOrResetJoinRequest(context) })
        } else {
            StoreDashboard(account = acc, viewModel = viewModel, themeColors = themeColors, onBackClick = { viewModel.cancelOrResetJoinRequest(context) })
        }
        return
    }

    // 🏠 2. If Property is Active
    if (matchingProperty != null && matchingProperty.isActive) {
        val acc = UnifiedBusinessAccount.fromProperty(matchingProperty)
        PropertyDashboard(account = acc, viewModel = viewModel, themeColors = themeColors, onBackClick = { viewModel.cancelOrResetJoinRequest(context) })
        return
    }

    // 👷 3. If Provider/Technician is Approved
    if (matchingApproved != null) {
        val categoryName = remember(matchingApproved.categoryId) {
            categories.find { it.id == matchingApproved.categoryId }?.name ?: "صيانة فنية"
        }
        val myBookings = remember(bookings, matchingApproved.id) {
            bookings.filter { it.providerId == matchingApproved.id }
        }
        val myNotifications = remember(notifications, matchingApproved.phone) {
            notifications.filter { it.targetValue == matchingApproved.phone || it.targetType == "ALL" }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = themeColors.surface
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                ApprovedTechnicianView(
                    provider = matchingApproved,
                    categoryName = categoryName,
                    bookings = myBookings,
                    notifications = myNotifications,
                    onToggleAvailability = {
                        viewModel.updateProviderEntity(matchingApproved.copy(isAvailable = !matchingApproved.isAvailable))
                        scope.launch { snackbarHostState.showSnackbar("تم تحديث حالة التوافر") }
                    },
                    onAcceptBooking = { bookingId ->
                        viewModel.updateBookingStatus(bookingId, "IN_PROGRESS")
                        scope.launch { snackbarHostState.showSnackbar("✅ تم قبول طلب الحجز") }
                    },
                    onRejectBooking = { bookingId ->
                        viewModel.updateBookingStatus(bookingId, "REJECTED", "اعتذر الفني لإنشغاله")
                        scope.launch { snackbarHostState.showSnackbar("❌ تم الاعتذار عن الطلب") }
                    },
                    onOpenChatWithCustomer = { custPhone, custName ->
                        viewModel.getOrCreateChatChannel(matchingApproved.id, matchingApproved.name, custPhone, custName)
                        activeChatChannel = ChatChannelEntity(
                            id = "chat_p_${matchingApproved.id}_u_$custPhone",
                            userName = custName,
                            lastMessage = "",
                            messages = emptyList()
                        )
                    },
                    themeColors = themeColors
                )
            }
        }

        activeChatChannel?.let { channel ->
            StatusChatDialog(
                chatChannel = channel,
                currentUserId = matchingApproved.id,
                onSendMessage = { msg ->
                    viewModel.replyToChatChannel(channel.id, matchingApproved.id, msg, matchingApproved.name)
                },
                onDismiss = { activeChatChannel = null },
                themeColors = themeColors
            )
        }
        return
    }

    // ⏳ 4. Store/Property/Technician Pending Approvals
    val rejectionNotif = notifications.find {
        it.targetValue == joinPhone && (it.title.contains("رفض") || it.message.contains("رفض"))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = themeColors.surface
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                rejectionNotif != null -> {
                    RejectedView(
                        reason = rejectionNotif.message,
                        onReapply = { viewModel.cancelOrResetJoinRequest(context) },
                        themeColors = themeColors
                    )
                }
                matchingStore != null && !matchingStore.isActive -> {
                    PendingApprovalView(
                        title = "⏳ طلب انضمام النشاط التجاري قيد المراجعة",
                        message = "تم استلام طلب انضمام النشاط '${matchingStore.name}' وهو قيد التدقيق والاعتماد الإداري.",
                        detailsList = listOf(
                            "اسم النشاط" to matchingStore.name,
                            "اسم المالك" to matchingStore.ownerName,
                            "رقم الهاتف" to matchingStore.phone,
                            "المنطقة" to "${matchingStore.cityId} - ${matchingStore.localNeighborhood}"
                        ),
                        onCancelRequest = { viewModel.cancelOrResetJoinRequest(context) },
                        themeColors = themeColors
                    )
                }
                matchingProperty != null && !matchingProperty.isActive -> {
                    PendingApprovalView(
                        title = "⏳ إعلان العقار قيد المراجعة",
                        message = "تم استلام إعلان العقار '${matchingProperty.title}' وهو قيد المراجعة والاعتماد الظاهر.",
                        detailsList = listOf(
                            "عنوان العقار" to matchingProperty.title,
                            "السعر" to "${matchingProperty.price} ${matchingProperty.currency}",
                            "رقم التواصل" to matchingProperty.phone,
                            "المنطقة" to "${matchingProperty.cityId} - ${matchingProperty.localNeighborhood}"
                        ),
                        onCancelRequest = { viewModel.cancelOrResetJoinRequest(context) },
                        themeColors = themeColors
                    )
                }
                matchingPending != null -> {
                    PendingApprovalView(
                        title = "⏳ طلب الانضمام كفني قيد المراجعة",
                        message = "تم استلام بياناتك بنجاح وجاري مراجعة المؤهلات وتفعيل الملف الشخصي.",
                        detailsList = listOf(
                            "الاسم" to matchingPending.name,
                            "رقم التواصل" to matchingPending.phone,
                            "المنطقة" to "${matchingPending.area} - ${matchingPending.localNeighborhood}"
                        ),
                        onCancelRequest = { viewModel.cancelOrResetJoinRequest(context) },
                        themeColors = themeColors
                    )
                }
                else -> {
                    PendingApprovalView(
                        title = "⏳ طلب الانضمام قيد المراجعة والتدقيق",
                        message = "طلبك قيد المراجعة الإدارية. سيصلك إشعار فور الاعتماد وتفعيل حسابك.",
                        detailsList = listOf("رقم الهاتف المسجل" to joinPhone),
                        onCancelRequest = { viewModel.cancelOrResetJoinRequest(context) },
                        themeColors = themeColors
                    )
                }
            }
        }
    }
}
