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
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.data.UnifiedBusinessAccount
import com.example.ui.MainViewModel
import com.example.ui.goBack
import com.example.ui.helpers.AppPreferenceHelper
import com.example.utils.AppConstants
import com.example.utils.VisualThemePalette
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private enum class DashboardLookupState { LOADING, NOT_FOUND, NETWORK_ERROR }

/**
 * 🏢 Dedicated Profile/Dashboard Router delegating to Specialized Dashboards
 * يدعم التوجيه الدقيق لجميع القطاعات (بما في ذلك المراكز التجارية MallDashboard)
 * مع جلب أحادي مباشر من Firestore بدلاً من الانتظار الأعمى لتوفير الحصة وتسريع الفتح.
 */
@Composable
fun UnifiedBusinessProfileDashboard(
    accountType: String, // "TECHNICIAN", "STORE", "MALL", "RESTAURANT", "MEDICAL", "REAL_ESTATE", "JOB_POSTER"
    providerId: String,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    BackHandler { viewModel.goBack() }

    val providers by viewModel.providers.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()

    var directProvider by remember(providerId) { mutableStateOf<ProviderEntity?>(null) }
    var directStore by remember(providerId) { mutableStateOf<StoreEntity?>(null) }
    var directProperty by remember(providerId) { mutableStateOf<PropertyEntity?>(null) }

    val cleanId = remember(providerId) {
        AppPreferenceHelper.normalizePhoneNumber(providerId)
    }

    val activeProvider = remember(providers, directProvider, providerId, cleanId) {
        providers.find { p ->
            p.id == providerId ||
                (cleanId.isNotBlank() && AppPreferenceHelper.normalizePhoneNumber(p.phone) == cleanId)
        } ?: directProvider
    }

    val matchingStores = remember(stores, directStore, providerId, cleanId) {
        val fromList = stores.filter { s ->
            s.id == providerId ||
                s.ownerId == providerId ||
                (cleanId.isNotBlank() && AppPreferenceHelper.normalizePhoneNumber(s.phone) == cleanId) ||
                (cleanId.isNotBlank() && AppPreferenceHelper.normalizePhoneNumber(s.ownerId) == cleanId)
        }
        if (fromList.isNotEmpty()) fromList else listOfNotNull(directStore)
    }

    fun pickStoreForSection(sectionMatcher: (StoreEntity) -> Boolean): StoreEntity? {
        return matchingStores.firstOrNull(sectionMatcher)
    }

    val activeStore = remember(matchingStores, accountType) {
        when (accountType) {
            "RESTAURANT" -> pickStoreForSection {
                it.sectionId.contains("restaurant", ignoreCase = true) ||
                    it.categoryId.contains("rest", ignoreCase = true) ||
                    it.categoryId.contains("مطعم")
            }
            "MEDICAL" -> pickStoreForSection {
                it.sectionId.contains("medical", ignoreCase = true) ||
                    it.categoryId.contains("طبي") ||
                    it.medicalLicenseNo.isNotBlank()
            }
            "MALL" -> pickStoreForSection {
                it.sectionId.contains("mall", ignoreCase = true) ||
                    it.categoryId.contains("مول") ||
                    it.categoryId.contains("مركز تجاري") ||
                    it.name.contains("مول") ||
                    it.name.contains("مركز تجاري")
            }
            "STORE" -> pickStoreForSection {
                !it.sectionId.contains("restaurant", ignoreCase = true) &&
                    !it.sectionId.contains("medical", ignoreCase = true) &&
                    !it.sectionId.contains("mall", ignoreCase = true) &&
                    it.medicalLicenseNo.isBlank()
            }
            else -> matchingStores.firstOrNull()
        }
    }

    val activeProperty = remember(properties, directProperty, providerId, cleanId) {
        properties.find { p ->
            p.id == providerId ||
                p.ownerId == providerId ||
                (cleanId.isNotBlank() && AppPreferenceHelper.normalizePhoneNumber(p.phone) == cleanId) ||
                (cleanId.isNotBlank() && AppPreferenceHelper.normalizePhoneNumber(p.ownerId) == cleanId)
        } ?: directProperty
    }

    when (accountType) {
        "RESTAURANT" -> {
            if (activeStore != null) {
                val acc = UnifiedBusinessAccount.fromStore(activeStore, "restaurants")
                RestaurantDashboard(account = acc, viewModel = viewModel, themeColors = themeColors, onBackClick = { viewModel.goBack() })
            } else {
                LoadingOrEmptyDashboard(
                    title = "المطعم",
                    accountType = accountType,
                    providerId = providerId,
                    cleanId = cleanId,
                    themeColors = themeColors,
                    onStoreResolved = { directStore = it },
                    onRefreshFallback = { viewModel.refreshData() },
                    onBackClick = { viewModel.goBack() }
                )
            }
        }
        "MEDICAL" -> {
            if (activeStore != null) {
                val acc = UnifiedBusinessAccount.fromStore(activeStore, "medical")
                MedicalDashboard(account = acc, viewModel = viewModel, themeColors = themeColors, onBackClick = { viewModel.goBack() })
            } else {
                LoadingOrEmptyDashboard(
                    title = "المركز الطبي",
                    accountType = accountType,
                    providerId = providerId,
                    cleanId = cleanId,
                    themeColors = themeColors,
                    onStoreResolved = { directStore = it },
                    onRefreshFallback = { viewModel.refreshData() },
                    onBackClick = { viewModel.goBack() }
                )
            }
        }
        "MALL" -> {
            if (activeStore != null) {
                val acc = UnifiedBusinessAccount.fromStore(activeStore, "malls")
                MallDashboard(account = acc, viewModel = viewModel, themeColors = themeColors, onBackClick = { viewModel.goBack() })
            } else {
                LoadingOrEmptyDashboard(
                    title = "المركز التجاري",
                    accountType = accountType,
                    providerId = providerId,
                    cleanId = cleanId,
                    themeColors = themeColors,
                    onStoreResolved = { directStore = it },
                    onRefreshFallback = { viewModel.refreshData() },
                    onBackClick = { viewModel.goBack() }
                )
            }
        }
        "STORE" -> {
            if (activeStore != null) {
                val acc = UnifiedBusinessAccount.fromStore(activeStore, "stores")
                StoreDashboard(account = acc, viewModel = viewModel, themeColors = themeColors, onBackClick = { viewModel.goBack() })
            } else {
                LoadingOrEmptyDashboard(
                    title = "المتجر",
                    accountType = accountType,
                    providerId = providerId,
                    cleanId = cleanId,
                    themeColors = themeColors,
                    onStoreResolved = { directStore = it },
                    onRefreshFallback = { viewModel.refreshData() },
                    onBackClick = { viewModel.goBack() }
                )
            }
        }
        "REAL_ESTATE" -> {
            if (activeProperty != null) {
                val acc = UnifiedBusinessAccount.fromProperty(activeProperty)
                PropertyDashboard(account = acc, viewModel = viewModel, themeColors = themeColors, onBackClick = { viewModel.goBack() })
            } else {
                LoadingOrEmptyDashboard(
                    title = "العقارات",
                    accountType = accountType,
                    providerId = providerId,
                    cleanId = cleanId,
                    themeColors = themeColors,
                    onPropertyResolved = { directProperty = it },
                    onRefreshFallback = { viewModel.refreshData() },
                    onBackClick = { viewModel.goBack() }
                )
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
                val sec = activeStore.sectionId.ifBlank { "stores" }
                val acc = UnifiedBusinessAccount.fromStore(activeStore, sec)
                if (sec.contains("mall", ignoreCase = true)) {
                    MallDashboard(account = acc, viewModel = viewModel, themeColors = themeColors, onBackClick = { viewModel.goBack() })
                } else {
                    StoreDashboard(account = acc, viewModel = viewModel, themeColors = themeColors, onBackClick = { viewModel.goBack() })
                }
            } else {
                LoadingOrEmptyDashboard(
                    title = "مقدم الخدمة",
                    accountType = accountType,
                    providerId = providerId,
                    cleanId = cleanId,
                    themeColors = themeColors,
                    onProviderResolved = { directProvider = it },
                    onRefreshFallback = { viewModel.refreshData() },
                    onBackClick = { viewModel.goBack() }
                )
            }
        }
    }
}

@Composable
private fun LoadingOrEmptyDashboard(
    title: String,
    accountType: String,
    providerId: String,
    cleanId: String,
    themeColors: VisualThemePalette,
    onProviderResolved: (ProviderEntity) -> Unit = {},
    onStoreResolved: (StoreEntity) -> Unit = {},
    onPropertyResolved: (PropertyEntity) -> Unit = {},
    onRefreshFallback: () -> Unit = {},
    onBackClick: () -> Unit
) {
    var lookupState by remember(title, providerId) { mutableStateOf(DashboardLookupState.LOADING) }
    var retryTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(accountType, providerId, retryTick) {
        lookupState = DashboardLookupState.LOADING
        val outcome = withTimeoutOrNull(8000L) {
            try {
                val db = FirebaseFirestore.getInstance()
                val phoneVariants = if (cleanId.isNotBlank()) {
                    listOf(cleanId, "0$cleanId", "+967$cleanId", "00967$cleanId").distinct()
                } else {
                    emptyList()
                }
                when (accountType) {
                    "RESTAURANT", "MEDICAL", "MALL", "STORE" -> {
                        val candidateIds = listOf(providerId, "store_$cleanId", cleanId)
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .distinct()
                        var found: StoreEntity? = null
                        for (docId in candidateIds) {
                            val snap = db.collection(AppConstants.COL_STORES).document(docId).get().await()
                            val parsed = snap.toObject(StoreEntity::class.java)?.copy(id = snap.id)
                            if (parsed != null && !parsed.isDeleted) {
                                found = parsed
                                break
                            }
                        }
                        if (found == null && phoneVariants.isNotEmpty()) {
                            val q = db.collection(AppConstants.COL_STORES)
                                .whereIn("phone", phoneVariants)
                                .limit(10)
                                .get()
                                .await()
                            found = q.documents
                                .mapNotNull { doc -> doc.toObject(StoreEntity::class.java)?.copy(id = doc.id) }
                                .firstOrNull { !it.isDeleted }
                        }
                        if (found != null) {
                            onStoreResolved(found)
                            DashboardLookupState.LOADING
                        } else {
                            DashboardLookupState.NOT_FOUND
                        }
                    }
                    "REAL_ESTATE" -> {
                        val candidateIds = listOf(providerId, "prop_$cleanId", cleanId)
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .distinct()
                        var found: PropertyEntity? = null
                        for (docId in candidateIds) {
                            val snap = db.collection(AppConstants.COL_PROPERTIES).document(docId).get().await()
                            val parsed = snap.toObject(PropertyEntity::class.java)?.copy(id = snap.id)
                            if (parsed != null && !parsed.isDeleted) {
                                found = parsed
                                break
                            }
                        }
                        if (found == null && phoneVariants.isNotEmpty()) {
                            val q = db.collection(AppConstants.COL_PROPERTIES)
                                .whereIn("phone", phoneVariants)
                                .limit(10)
                                .get()
                                .await()
                            found = q.documents
                                .mapNotNull { doc -> doc.toObject(PropertyEntity::class.java)?.copy(id = doc.id) }
                                .firstOrNull { !it.isDeleted }
                        }
                        if (found != null) {
                            onPropertyResolved(found)
                            DashboardLookupState.LOADING
                        } else {
                            DashboardLookupState.NOT_FOUND
                        }
                    }
                    else -> {
                        val candidateIds = listOf(providerId, "prov_$cleanId", cleanId)
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .distinct()
                        var found: ProviderEntity? = null
                        for (docId in candidateIds) {
                            val snap = db.collection(AppConstants.COL_PROVIDERS).document(docId).get().await()
                            val parsed = snap.toObject(ProviderEntity::class.java)?.copy(id = snap.id)
                            if (parsed != null && !parsed.isDeleted) {
                                found = parsed
                                break
                            }
                        }
                        if (found == null && phoneVariants.isNotEmpty()) {
                            val q = db.collection(AppConstants.COL_PROVIDERS)
                                .whereIn("phone", phoneVariants)
                                .limit(10)
                                .get()
                                .await()
                            found = q.documents
                                .mapNotNull { doc -> doc.toObject(ProviderEntity::class.java)?.copy(id = doc.id) }
                                .firstOrNull { !it.isDeleted }
                        }
                        if (found != null) {
                            onProviderResolved(found)
                            DashboardLookupState.LOADING
                        } else {
                            DashboardLookupState.NOT_FOUND
                        }
                    }
                }
            } catch (_: Exception) {
                DashboardLookupState.NETWORK_ERROR
            }
        }
        lookupState = outcome ?: DashboardLookupState.NETWORK_ERROR
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
            when (lookupState) {
                DashboardLookupState.LOADING -> {
                    CircularProgressIndicator(color = themeColors.accent)
                    Text(
                        text = "جاري جلب بيانات $title...",
                        color = themeColors.textPrimary,
                        fontSize = 14.sp
                    )
                }
                DashboardLookupState.NETWORK_ERROR -> {
                    Text(
                        text = "تعذر الاتصال بالخادم لجلب بيانات $title. تحقق من الشبكة وأعد المحاولة.",
                        color = themeColors.textPrimary,
                        fontSize = 14.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                onRefreshFallback()
                                retryTick++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                        ) {
                            Text("إعادة المحاولة 🔄", color = Color.Black, fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = onBackClick) {
                            Text("رجوع", color = themeColors.textPrimary, fontSize = 12.sp)
                        }
                    }
                }
                DashboardLookupState.NOT_FOUND -> {
                    Text(
                        text = "لم يتم العثور على سجل نشط لـ $title لهذا الحساب أو قد يكون بانتظار التفعيل.",
                        color = themeColors.textPrimary,
                        fontSize = 14.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                onRefreshFallback()
                                retryTick++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                        ) {
                            Text("تحديث الآن 🔄", color = Color.Black, fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = onBackClick) {
                            Text("رجوع", color = themeColors.textPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
