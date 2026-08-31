package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
import com.example.viewmodels.ProviderViewModel
import com.example.viewmodels.StoreViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodels.RegistrationViewModel
import com.example.viewmodels.PropertyViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UnifiedBusinessAccount

import com.example.utils.VisualThemePalette

/**
 * 🏢 Dedicated Profile/Dashboard Router delegating to the 6 Specialized Dashboards
 */
@Composable
fun UnifiedBusinessProfileDashboard(
    accountType: String, // "TECHNICIAN", "STORE", "RESTAURANT", "MEDICAL", "REAL_ESTATE", "JOB_POSTER"
    providerId: String,
    propertyViewModel: PropertyViewModel = viewModel(),
    storeViewModel: StoreViewModel = viewModel(),
    providerViewModel: ProviderViewModel = viewModel(),
    registrationViewModel: RegistrationViewModel = viewModel(),
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    val providers by providerViewModel.providers.collectAsState()
    val stores by storeViewModel.stores.collectAsState()
    val properties by propertyViewModel.properties.collectAsState()

    val cleanId = providerId.trim().replace(" ", "").replace("+", "")
    val activeProvider = remember(providers, providerId) {
        providers.find { it.id == providerId || it.phone.trim().replace(" ", "").replace("+", "") == cleanId }
    }
    val activeStore = remember(stores, providerId) {
        stores.find { it.id == providerId || it.ownerId == providerId || it.phone.trim().replace(" ", "").replace("+", "") == cleanId || it.ownerId.trim().replace(" ", "").replace("+", "") == cleanId }
    }
    val activeProperty = remember(properties, providerId) {
        properties.find { it.id == providerId || it.phone.trim().replace(" ", "").replace("+", "") == cleanId || it.ownerId.trim().replace(" ", "").replace("+", "") == cleanId }
    }

    when (accountType) {
        "RESTAURANT" -> {
            if (activeStore != null) {
                val acc = UnifiedBusinessAccount.fromStore(activeStore, "restaurants")
                RestaurantDashboard(account = acc,  themeColors = themeColors, onBackClick = { registrationViewModel.cancelOrResetJoinRequest(context) })
            } else {
                LoadingOrEmptyDashboard("المطعم")
            }
        }
        "MEDICAL" -> {
            if (activeStore != null) {
                val acc = UnifiedBusinessAccount.fromStore(activeStore, "medical")
                MedicalDashboard(account = acc,  themeColors = themeColors, onBackClick = { registrationViewModel.cancelOrResetJoinRequest(context) })
            } else {
                LoadingOrEmptyDashboard("المركز الطبي")
            }
        }
        "STORE" -> {
            if (activeStore != null) {
                val acc = UnifiedBusinessAccount.fromStore(activeStore, "stores")
                StoreDashboard(account = acc,  themeColors = themeColors, onBackClick = { registrationViewModel.cancelOrResetJoinRequest(context) })
            } else {
                LoadingOrEmptyDashboard("المتجر")
            }
        }
        "REAL_ESTATE" -> {
            if (activeProperty != null) {
                val acc = UnifiedBusinessAccount.fromProperty(activeProperty)
                PropertyDashboard(account = acc,  themeColors = themeColors, onBackClick = { registrationViewModel.cancelOrResetJoinRequest(context) })
            } else {
                LoadingOrEmptyDashboard("العقارات")
            }
        }
        "JOB_POSTER" -> {
            val acc = UnifiedBusinessAccount(
                id = providerId,
                name = activeStore?.name ?: activeProvider?.name ?: "مسؤول التوظيف",
                phone = activeStore?.phone ?: activeProvider?.phone ?: providerId,
                cityId = activeStore?.cityId ?: activeProvider?.cityId ?: "",
                businessType = com.example.data.BusinessType.JOB_POSTER
            )
            JobPosterDashboard(account = acc,  themeColors = themeColors, onBackClick = { registrationViewModel.cancelOrResetJoinRequest(context) })
        }
        else -> { // TECHNICIAN or default
            if (activeProvider != null) {
                val acc = UnifiedBusinessAccount.fromProvider(activeProvider)
                TechnicianDashboard(account = acc,  themeColors = themeColors, onBackClick = { registrationViewModel.cancelOrResetJoinRequest(context) })
            } else if (activeStore != null) {
                val acc = UnifiedBusinessAccount.fromStore(activeStore, "stores")
                StoreDashboard(account = acc,  themeColors = themeColors, onBackClick = { registrationViewModel.cancelOrResetJoinRequest(context) })
            } else {
                LoadingOrEmptyDashboard("مقدم الخدمة")
            }
        }
    }
}

@Composable
private fun LoadingOrEmptyDashboard(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(color = Color(0xFF1E88E5))
            Text(
                text = "جاري تحميل بيانات $title...",
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}
