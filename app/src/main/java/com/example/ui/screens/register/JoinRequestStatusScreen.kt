@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.register

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
import com.example.ui.MainViewModel
import com.example.ui.screens.register.status.*
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

/**
 * 📊 JoinRequestStatusScreen - الشاشة الموحدة لمتابعة حالة طلب الانضمام أو الدخول للوحات التحكم
 * تعتمد على UseCase و Router و StateFlow النظيف
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
    val useCase = remember { JoinStatusUseCase() }

    val joinPhone by viewModel.joinRequestPhone.collectAsState()
    val pendingProviders by viewModel.pendingProviders.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val jobs by viewModel.jobs.collectAsState()
    val registeredUsersList by viewModel.registeredUsersList.collectAsState()

    var activeChatChannel by remember { mutableStateOf<ChatChannelEntity?>(null) }

    val currentStatus = remember(joinPhone, pendingProviders, providers, stores, properties, categories, notifications, jobs, registeredUsersList) {
        useCase.determineStatus(
            joinPhone = joinPhone,
            pendingProviders = pendingProviders,
            providers = providers,
            stores = stores,
            properties = properties,
            categories = categories,
            notifications = notifications,
            jobs = jobs,
            registeredUsersList = registeredUsersList
        )
    }

    // Active Dashboards Routing
    if (currentStatus is JoinStatus.ActiveStore || 
        currentStatus is JoinStatus.ActiveProperty || 
        currentStatus is JoinStatus.ApprovedTechnician || 
        currentStatus is JoinStatus.ActiveJobPoster || 
        currentStatus is JoinStatus.ActiveClient
    ) {
        JoinStatusRouter.RouteToDashboard(
            status = currentStatus,
            viewModel = viewModel,
            themeColors = themeColors,
            context = context,
            scope = scope,
            snackbarHostState = snackbarHostState,
            onOpenChat = { activeChatChannel = it }
        )
        return
    }

    // Pending & Rejected Views
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
            when (currentStatus) {
                is JoinStatus.Rejected -> {
                    RejectedView(
                        reason = currentStatus.reason ?: "تم رفض الطلب لعدم استيفاء الشروط الخاصة بالخدمة.",
                        onReapply = { viewModel.cancelOrResetJoinRequest(context) },
                        themeColors = themeColors
                    )
                }
                is JoinStatus.PendingStore -> {
                    val store = currentStatus.store
                    val storeName = store?.name ?: "نشاط تجاري جديد"
                    val ownerName = store?.ownerName ?: "غير محدد"
                    val phone = store?.phone ?: "غير محدد"
                    val city = store?.cityId ?: "صنعاء"
                    val neighborhood = store?.localNeighborhood ?: ""
                    PendingApprovalView(
                        title = "⏳ طلب انضمام النشاط التجاري قيد المراجعة",
                        message = "تم استلام طلب انضمام النشاط '$storeName' وهو قيد التدقيق والاعتماد الإداري.",
                        detailsList = listOf(
                            "اسم النشاط" to storeName,
                            "اسم المالك" to ownerName,
                            "رقم الهاتف" to phone,
                            "المنطقة" to "$city - $neighborhood"
                        ),
                        onCancelRequest = { viewModel.cancelOrResetJoinRequest(context) },
                        themeColors = themeColors
                    )
                }
                is JoinStatus.PendingProperty -> {
                    val prop = currentStatus.property
                    val title = prop?.title ?: "عقار جديد"
                    val price = prop?.price ?: 0.0
                    val currency = prop?.currency ?: "ريال يمني"
                    val phone = prop?.phone ?: "غير محدد"
                    val city = prop?.cityId ?: "صنعاء"
                    val neighborhood = prop?.localNeighborhood ?: ""
                    PendingApprovalView(
                        title = "⏳ إعلان العقار قيد المراجعة",
                        message = "تم استلام إعلان العقار '$title' وهو قيد المراجعة والاعتماد الظاهر.",
                        detailsList = listOf(
                            "عنوان العقار" to title,
                            "السعر" to "$price $currency",
                            "رقم التواصل" to phone,
                            "المنطقة" to "$city - $neighborhood"
                        ),
                        onCancelRequest = { viewModel.cancelOrResetJoinRequest(context) },
                        themeColors = themeColors
                    )
                }
                is JoinStatus.PendingTechnician -> {
                    val pending = currentStatus.provider
                    val name = pending?.name ?: "فني جديد"
                    val phone = pending?.phone ?: "غير محدد"
                    val area = pending?.area ?: "صنعاء"
                    val neighborhood = pending?.localNeighborhood ?: ""
                    PendingApprovalView(
                        title = "⏳ طلب الانضمام كفني قيد المراجعة",
                        message = "تم استلام بياناتك بنجاح وجاري مراجعة المؤهلات وتفعيل الملف الشخصي.",
                        detailsList = listOf(
                            "الاسم" to name,
                            "رقم التواصل" to phone,
                            "المنطقة" to "$area - $neighborhood"
                        ),
                        onCancelRequest = { viewModel.cancelOrResetJoinRequest(context) },
                        themeColors = themeColors
                    )
                }
                is JoinStatus.PendingGeneric -> {
                    PendingApprovalView(
                        title = "⏳ طلب الانضمام قيد المراجعة والتدقيق",
                        message = "طلبك قيد المراجعة الإدارية. سيصلك إشعار فور الاعتماد وتفعيل حسابك.",
                        detailsList = listOf("رقم الهاتف المسجل" to (currentStatus.phone ?: "غير محدد")),
                        onCancelRequest = { viewModel.cancelOrResetJoinRequest(context) },
                        themeColors = themeColors
                    )
                }
                else -> {}
            }
        }
    }
}
