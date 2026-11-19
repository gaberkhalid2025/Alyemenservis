package com.example.ui.screens.register.status

import android.content.Context
import com.example.ui.*
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
                        onBackClick = { viewModel.goBack() }
                    )
                    "medical" -> MedicalDashboard(
                        account = acc,
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onBackClick = { viewModel.goBack() }
                    )
                    else -> StoreDashboard(
                        account = acc,
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onBackClick = { viewModel.goBack() }
                    )
                }
            }
            is JoinStatus.ActiveProperty -> {
                val acc = UnifiedBusinessAccount.fromProperty(status.property)
                PropertyDashboard(
                    account = acc,
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onBackClick = { viewModel.goBack() }
                )
            }
            is JoinStatus.ApprovedTechnician -> {
                val prov = status.provider
                val acc = UnifiedBusinessAccount.fromProvider(prov)
                val isMed = prov.providerType.contains("medical", ignoreCase = true) ||
                    prov.categoryId.contains("medical", ignoreCase = true) ||
                    prov.customCategoryName.contains("طبي") ||
                    prov.customCategoryName.contains("عياد") ||
                    prov.profession.contains("طبي") ||
                    prov.profession.contains("عياد") ||
                    prov.profession.contains("مستشفى")
                if (isMed) {
                    MedicalDashboard(
                        account = acc,
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onBackClick = { viewModel.goBack() }
                    )
                } else {
                    TechnicianDashboard(
                        account = acc,
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onBackClick = { viewModel.goBack() }
                    )
                }
            }
            is JoinStatus.ActiveJobPoster -> {
                val acc = UnifiedBusinessAccount.fromJob(status.job)
                JobPosterDashboard(
                    account = acc,
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onBackClick = { viewModel.goBack() }
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
