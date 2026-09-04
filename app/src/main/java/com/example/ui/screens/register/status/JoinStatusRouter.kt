package com.example.ui.screens.register.status

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.ChatChannelEntity
import com.example.data.UnifiedBusinessAccount
import com.example.ui.MainViewModel
import com.example.ui.screens.dashboard.MedicalDashboard
import com.example.ui.screens.dashboard.PropertyDashboard
import com.example.ui.screens.dashboard.RestaurantDashboard
import com.example.ui.screens.dashboard.StoreDashboard
import com.example.ui.screens.dashboard.TechnicianDashboard
import com.example.ui.screens.dashboard.JobPosterDashboard
import com.example.ui.screens.dashboard.ClientPersonalAccountDashboard
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 🧭 JoinStatusRouter - موجه الشاشات واللوحات بناءً على حالة الطلب
 */
object JoinStatusRouter {

    @Composable
    fun RouteToDashboard(
        status: JoinStatus,
        viewModel: MainViewModel,
        themeColors: VisualThemePalette,
        context: Context,
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState,
        onOpenChat: (ChatChannelEntity) -> Unit
    ) {
        when (status) {
            is JoinStatus.ActiveStore -> {
                val acc = UnifiedBusinessAccount.fromStore(status.store, status.businessType)
                when (status.businessType) {
                    "restaurants" -> RestaurantDashboard(
                        account = acc,
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onBackClick = { viewModel.cancelOrResetJoinRequest(context) }
                    )
                    "medical" -> MedicalDashboard(
                        account = acc,
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onBackClick = { viewModel.cancelOrResetJoinRequest(context) }
                    )
                    else -> StoreDashboard(
                        account = acc,
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onBackClick = { viewModel.cancelOrResetJoinRequest(context) }
                    )
                }
            }
            is JoinStatus.ActiveProperty -> {
                val acc = UnifiedBusinessAccount.fromProperty(status.property)
                PropertyDashboard(
                    account = acc,
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onBackClick = { viewModel.cancelOrResetJoinRequest(context) }
                )
            }
            is JoinStatus.ApprovedTechnician -> {
                val acc = com.example.data.UnifiedBusinessAccount.fromProvider(status.provider)
                TechnicianDashboard(
                    account = acc,
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onBackClick = { viewModel.cancelOrResetJoinRequest(context) }
                )
            }
            is JoinStatus.ActiveJobPoster -> {
                val acc = com.example.data.UnifiedBusinessAccount.fromJob(status.job)
                JobPosterDashboard(
                    account = acc,
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onBackClick = { viewModel.cancelOrResetJoinRequest(context) }
                )
            }
            is JoinStatus.ActiveClient -> {
                val clientPhone = (status.userMap["phone"] as? String) ?: ""
                val clientName = (status.userMap["name"] as? String) ?: "العميل"
                val clientResidence = (status.userMap["residence"] as? String) ?: "اليمن"
                val clientId = (status.userMap["id"] as? String) ?: clientPhone
                val bookings by viewModel.bookings.collectAsState()
                ClientPersonalAccountDashboard(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    context = context,
                    currentUserName = clientName,
                    currentUserPhone = clientPhone,
                    currentUserResidence = clientResidence,
                    currentUserId = clientId,
                    bookings = bookings,
                    onShowRegistrationFormsAnyway = { viewModel.cancelOrResetJoinRequest(context) }
                )
            }
            else -> {
                // Handled in Pending / Rejected views
            }
        }
    }
}
