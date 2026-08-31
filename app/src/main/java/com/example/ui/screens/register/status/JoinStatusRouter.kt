package com.example.ui.screens.register.status

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import com.example.viewmodels.RegistrationViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import com.example.data.ChatChannelEntity
import com.example.data.UnifiedBusinessAccount

import com.example.ui.screens.dashboard.MedicalDashboard
import com.example.ui.screens.dashboard.PropertyDashboard
import com.example.ui.screens.dashboard.RestaurantDashboard
import com.example.ui.screens.dashboard.StoreDashboard
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
        registrationViewModel: RegistrationViewModel = viewModel(),
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
                        
                        themeColors = themeColors,
                        onBackClick = { registrationViewModel.cancelOrResetJoinRequest(context) }
                    )
                    "medical" -> MedicalDashboard(
                        account = acc,
                        
                        themeColors = themeColors,
                        onBackClick = { registrationViewModel.cancelOrResetJoinRequest(context) }
                    )
                    else -> StoreDashboard(
                        account = acc,
                        
                        themeColors = themeColors,
                        onBackClick = { registrationViewModel.cancelOrResetJoinRequest(context) }
                    )
                }
            }
            is JoinStatus.ActiveProperty -> {
                val acc = UnifiedBusinessAccount.fromProperty(status.property)
                PropertyDashboard(
                    account = acc,
                    
                    themeColors = themeColors,
                    onBackClick = { registrationViewModel.cancelOrResetJoinRequest(context) }
                )
            }
            else -> {
                // Handled in Pending / Rejected views
            }
        }
    }
}
