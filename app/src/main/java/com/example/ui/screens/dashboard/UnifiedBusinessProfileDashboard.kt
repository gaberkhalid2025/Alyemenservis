package com.example.ui.screens.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StoreEntity
import com.example.data.UnifiedBusinessAccount
import com.example.ui.MainViewModel
import com.example.ui.goBack
import com.example.ui.helpers.AppPreferenceHelper
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.delay

/**
 * 🏢 Dedicated Profile/Dashboard Router delegating to the 6 Specialized Dashboards
 */
@Composable
fun UnifiedBusinessProfileDashboard(
    accountType: String, // "TECHNICIAN", "STORE", "RESTAURANT", "MEDICAL", "REAL_ESTATE", "JOB_POSTER"
    providerId: String,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    BackHandler { viewModel.goBack() }

    val providers by viewModel.providers.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()

    val cleanId = remember(providerId) {
        AppPreferenceHelper.normalizePhoneNumber(providerId)
    }

    val activeProvider = remember(providers, providerId, cleanId) {
        providers.find { p ->
            p.id == providerId ||
                (cleanId.isNotBlank() && AppPreferenceHelper.normalizePhoneNumber(p.phone) == cleanId)
        }
    }

    val matchingStores = remember(stores, providerId, cleanId) {
        stores.filter { s ->
            s.id == providerId ||
                s.ownerId == providerId ||
                (cleanId.isNotBlank() && AppPreferenceHelper.normalizePhoneNumber(s.phone) == cleanId) ||
                (cleanId.isNotBlank() && AppPreferenceHelper.normalizePhoneNumber(s.ownerId) == cleanId)
        }
    }

    fun pickStoreForSection(sectionMatcher: (StoreEntity) -> Boolean): StoreEntity? {
        return matchingStores.firstOrNull(sectionMatcher) ?: matchingStores.firstOrNull()
    }

    val activeStore = remember(matchingStores, accountType) {
        when (accountType) {
            "RESTAURANT" -> pickStoreForSection {
                it.sectionId.contains("restaurant", ignoreCase = true) ||
                    it.categoryId.contains("rest", ignoreCase = true)
            }
            "MEDICAL" -> pickStoreForSection {
                it.sectionId.contains("medical", ignoreCase = true) ||
                    it.medicalLicenseNo.isNotBlank()
            }
            "STORE" -> pickStoreForSection {
                !it.sectionId.contains("restaurant", ignoreCase = true) &&
                    !it.sectionId.contains("medical", ignoreCase = true) &&
                    it.medicalLicenseNo.isBlank()
            }
            else -> matchingStores.firstOrNull()
        }
    }

    val activeProperty = remember(properties, providerId, cleanId) {
        properties.find { p ->
            p.id == providerId ||
                p.ownerId == providerId ||
                (cleanId.isNotBlank() && AppPreferenceHelper.normalizePhoneNumber(p.phone) == cleanId) ||
                (cleanId.isNotBlank() && AppPreferenceHelper.normalizePhoneNumber(p.ownerId) == cleanId)
        }
    }

    when (accountType) {
        "RESTAURANT" -> {
            if (activeStore != null) {
                val acc = UnifiedBusinessAccount.fromStore(activeStore, "restaurants")
                RestaurantDashboard(account = acc, viewModel = viewModel, themeColors = themeColors, onBackClick = { viewModel.goBack() })
            } else {
                LoadingOrEmptyDashboard("المطعم", themeColors = themeColors, onBackClick = { viewModel.goBack() })
            }
        }
        "MEDICAL" -> {
            if (activeStore != null) {
                val acc = UnifiedBusinessAccount.fromStore(activeStore, "medical")
                MedicalDashboard(account = acc, viewModel = viewModel, themeColors = themeColors, onBackClick = { viewModel.goBack() })
            } else {
                LoadingOrEmptyDashboard("المركز الطبي", themeColors = themeColors, onBackClick = { viewModel.goBack() })
            }
        }
        "STORE" -> {
            if (activeStore != null) {
                val acc = UnifiedBusinessAccount.fromStore(activeStore, "stores")
                StoreDashboard(account = acc, viewModel = viewModel, themeColors = themeColors, onBackClick = { viewModel.goBack() })
            } else {
                LoadingOrEmptyDashboard("المتجر", themeColors = themeColors, onBackClick = { viewModel.goBack() })
            }
        }
        "REAL_ESTATE" -> {
            if (activeProperty != null) {
                val acc = UnifiedBusinessAccount.fromProperty(activeProperty)
                PropertyDashboard(account = acc, viewModel = viewModel, themeColors = themeColors, onBackClick = { viewModel.goBack() })
            } else {
                LoadingOrEmptyDashboard("العقارات", themeColors = themeColors, onBackClick = { viewModel.goBack() })
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
            JobPosterDashboard(account = acc, viewModel = viewModel, themeColors = themeColors, onBackClick = { viewModel.goBack() })
        }
        else -> { // TECHNICIAN or default
            if (activeProvider != null) {
                val acc = UnifiedBusinessAccount.fromProvider(activeProvider)
                TechnicianDashboard(account = acc, viewModel = viewModel, themeColors = themeColors, onBackClick = { viewModel.goBack() })
            } else if (activeStore != null) {
                val acc = UnifiedBusinessAccount.fromStore(activeStore, activeStore.sectionId.ifBlank { "stores" })
                StoreDashboard(account = acc, viewModel = viewModel, themeColors = themeColors, onBackClick = { viewModel.goBack() })
            } else {
                LoadingOrEmptyDashboard("مقدم الخدمة", themeColors = themeColors, onBackClick = { viewModel.goBack() })
            }
        }
    }
}

@Composable
private fun LoadingOrEmptyDashboard(
    title: String,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit
) {
    var timedOut by remember(title) { mutableStateOf(false) }
    var retryTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(title, retryTick) {
        timedOut = false
        delay(6000)
        timedOut = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!timedOut) {
                CircularProgressIndicator(color = themeColors.accent)
                Text(
                    text = "جاري تحميل بيانات $title...",
                    color = themeColors.textPrimary,
                    fontSize = 14.sp
                )
            } else {
                Text(
                    text = "تعذر العثور على بيانات $title أو قد يكون بانتظار المزامنة.",
                    color = themeColors.textPrimary,
                    fontSize = 14.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { retryTick++ },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                    ) {
                        Text("إعادة المحاولة 🔄", color = Color.Black, fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = onBackClick) {
                        Text("رجوع", color = themeColors.textPrimary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
