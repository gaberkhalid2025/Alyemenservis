package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.data.ActivityLogEntity
import com.example.data.CallEntity
import com.example.utils.DateFormatter
import com.example.utils.FullSyncManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

internal fun saveSettingsAtomicallyOp(
    vm: AdminViewModel,
    appName: String,
    welcomeMessage: String,
    footerMessage: String,
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    vm.viewModelScope.launch {
        try {
            val settingsRef = vm.db.collection("settings").document("main_settings")
            vm.db.runTransaction { transaction ->
                val snapshot = transaction.get(settingsRef)
                val currentVersion = snapshot.getLong("version") ?: 0L
                val updatedData = mapOf(
                    "appName" to appName,
                    "welcomeMessage" to welcomeMessage,
                    "footerMessage" to footerMessage,
                    "version" to (currentVersion + 1),
                    "updatedAt" to System.currentTimeMillis(),
                    "updatedBy" to FirebaseAuth.getInstance().currentUser?.uid
                )
                transaction.set(settingsRef, updatedData, SetOptions.merge())
                val auditRef = vm.db.collection("audit_logs").document()
                transaction.set(
                    auditRef,
                    mapOf(
                        "action" to "SETTINGS_UPDATED",
                        "userId" to (FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"),
                        "timestamp" to System.currentTimeMillis(),
                        "version" to (currentVersion + 1)
                    )
                )
                currentVersion + 1
            }.await()
            vm.appState._settings.value = vm.appState._settings.value.copy(
                appName = appName,
                welcomeMessage = welcomeMessage,
                footerMessage = footerMessage
            )
            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "فشل حفظ الإعدادات سحابياً")
        }
    }
}

internal fun createSettingsBackupOp(vm: AdminViewModel, onResult: (Boolean) -> Unit = {}) {
    vm.viewModelScope.launch {
        try {
            val settingsDoc = vm.db.collection("settings")
                .document("main_settings")
                .get()
                .await()
            if (!settingsDoc.exists()) {
                onResult(false)
                return@launch
            }
            val settingsData = settingsDoc.data ?: emptyMap<String, Any>()
            val backupData: Map<String, Any?> = mapOf(
                "timestamp" to System.currentTimeMillis(),
                "version" to (settingsData["version"] ?: 0L),
                "data" to settingsData,
                "createdBy" to FirebaseAuth.getInstance().currentUser?.uid
            )
            vm.db.collection("settings_backups").add(backupData).await()
            onResult(true)
        } catch (_: Exception) {
            onResult(false)
        }
    }
}

internal fun logAdminActivityOp(vm: AdminViewModel, action: String) {
    val id = vm.db.collection("activity_logs").document().id
    val log = ActivityLogEntity(id = id, action = action, timestamp = System.currentTimeMillis())
    vm.db.collection("activity_logs").document(id).set(log)
}

internal fun logCallOp(vm: AdminViewModel, providerId: String, providerName: String) {
    val callId = UUID.randomUUID().toString()
    val call = CallEntity(
        id = callId,
        providerId = providerId,
        providerName = providerName,
        callerName = "مواطن يمني 🇾🇪",
        timestamp = System.currentTimeMillis()
    )
    vm.db.collection("calls").document(callId).set(call)
}

internal fun loadSystemStatsOp(vm: AdminViewModel) {
    val totalProvs = vm.appState._providers.value.size
    val usersTotal = maxOf(vm._registeredUsersList.value.size, vm._registeredUsersCount.value)
    val allBookings = vm.appState._bookings.value
    val activeCount = allBookings.count { b ->
        val st = b.status.uppercase()
        st == "PENDING" || st == "ACCEPTED" || st == "APPROVED" || st == "IN_PROGRESS"
    }
    val completedCount = allBookings.count { it.status.equals("COMPLETED", ignoreCase = true) }
    val pendingCount = vm._pendingRequestsState.value.count { it.status.equals("PENDING", ignoreCase = true) } +
        vm._pendingProviders.value.count { it.status.equals("PENDING", ignoreCase = true) }
    vm._systemStatsState.value = SystemStats(
        totalProviders = totalProvs,
        totalUsers = usersTotal,
        activeBookings = activeCount,
        completedOrders = completedCount,
        pendingApprovals = pendingCount
    )
}

internal fun syncAllDataOp(vm: AdminViewModel, context: Context, onComplete: ((Boolean) -> Unit)? = null) {
    vm.viewModelScope.launch {
        try {
            val syncMgr = FullSyncManager(context)
            val res = syncMgr.syncAllSettings()
            onComplete?.invoke(res)
        } catch (_: Exception) {
            onComplete?.invoke(false)
        }
    }
}

internal fun clearCacheOp(vm: AdminViewModel, context: Context) {
    try {
        FullSyncManager(context).clearLocalCache()
        vm.mainViewModel.triggerNotification("🧹 تم مسح الذاكرة المؤقتة بنجاح")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun periodStartMillis(period: String): Long {
    val now = System.currentTimeMillis()
    val dayMs = 24L * 60L * 60L * 1000L
    return when (period.uppercase()) {
        "TODAY" -> now - dayMs
        "WEEK" -> now - 7L * dayMs
        "MONTH" -> now - 30L * dayMs
        "YEAR" -> now - 365L * dayMs
        else -> 0L
    }
}

internal fun getBookingStatsOp(vm: AdminViewModel, period: String = "ALL"): BookingStats {
    val now = System.currentTimeMillis()
    val dayMs = 24L * 60L * 60L * 1000L
    val cutoff = periodStartMillis(period)
    val allBookings = vm.appState._bookings.value
    val filtered = if (cutoff == 0L) allBookings else allBookings.filter { it.createdAt == 0L || it.createdAt >= cutoff }
    return BookingStats(
        total = filtered.size,
        pending = filtered.count { it.status.equals("PENDING", ignoreCase = true) },
        inProgress = filtered.count {
            val st = it.status.uppercase()
            st == "ACCEPTED" || st == "APPROVED" || st == "IN_PROGRESS"
        },
        completed = filtered.count { it.status.equals("COMPLETED", ignoreCase = true) },
        cancelled = filtered.count {
            val st = it.status.uppercase()
            st == "CANCELLED" || st == "REJECTED"
        },
        today = allBookings.count { it.createdAt >= now - dayMs },
        thisWeek = allBookings.count { it.createdAt >= now - 7L * dayMs },
        thisMonth = allBookings.count { it.createdAt >= now - 30L * dayMs }
    )
}

internal fun getRevenueStatsOp(vm: AdminViewModel, period: String = "ALL"): RevenueStats {
    val now = System.currentTimeMillis()
    val dayMs = 24L * 60L * 60L * 1000L
    val cutoff = periodStartMillis(period)
    val completedPayments = vm._payments.value.filter { it.status.equals("COMPLETED", ignoreCase = true) }
    val filteredPayments = if (cutoff == 0L) completedPayments else completedPayments.filter { it.createdAt == 0L || it.createdAt >= cutoff }

    val paidFromPayments = filteredPayments.sumOf { it.amount }
    val paidFromBookings = vm.appState._bookings.value
        .filter {
            val st = it.status.uppercase()
            (st == "COMPLETED" || st == "ACCEPTED" || st == "APPROVED") &&
                (cutoff == 0L || it.createdAt == 0L || it.createdAt >= cutoff)
        }
        .sumOf { if (it.totalAmount > 0.0) it.totalAmount else it.price }
    val paidFromOrders = vm._orders.value
        .filter {
            it.status.equals("COMPLETED", ignoreCase = true) &&
                (cutoff == 0L || it.timestamp == 0L || it.timestamp >= cutoff)
        }
        .sumOf { it.totalAmount }

    val totalRev = if (paidFromPayments > 0.0) paidFromPayments else (paidFromBookings + paidFromOrders)
    val commissionSum = filteredPayments.sumOf { it.commission }.takeIf { it > 0.0 } ?: (totalRev * 0.10)
    val payoutsSum = filteredPayments.sumOf { it.providerShare }.takeIf { it > 0.0 } ?: (totalRev - commissionSum).coerceAtLeast(0.0)
    val todayRev = completedPayments.filter { it.createdAt >= now - dayMs }.sumOf { it.amount }
    val monthRev = completedPayments.filter { it.createdAt >= now - 30L * dayMs }.sumOf { it.amount }

    return RevenueStats(
        totalRevenue = totalRev,
        platformCommission = commissionSum,
        providerPayouts = payoutsSum,
        currency = "YER",
        todayRevenue = todayRev,
        monthRevenue = monthRev
    )
}

internal fun getProviderStatsOp(vm: AdminViewModel): ProviderStats {
    val provs = vm.appState._providers.value
    val pendingCount = vm._pendingRequestsState.value.count { it.status.equals("PENDING", ignoreCase = true) } +
        vm._pendingProviders.value.count { it.status.equals("PENDING", ignoreCase = true) }
    return ProviderStats(
        totalVerified = provs.count { it.isVerified },
        pendingApproval = pendingCount,
        topRated = provs.count { it.rating >= 4.5f && it.numReviews > 0 },
        suspended = provs.count { !it.isAvailable }
    )
}

internal fun getCategoryStatsOp(vm: AdminViewModel): CategoryStats {
    val provs = vm.appState._providers.value
    val storesList = vm._stores.value
    val medicalProvs = provs.count {
        it.categoryId.contains("med", ignoreCase = true) ||
            it.categoryId.contains("health", ignoreCase = true) ||
            it.categoryId.contains("clinic", ignoreCase = true) ||
            it.categoryId.contains("طب")
    }
    val medicalStores = storesList.count { it.sectionId.equals("medical", ignoreCase = true) }
    val restaurantCount = storesList.count {
        it.sectionId.equals("restaurants", ignoreCase = true) ||
            it.categoryId.contains("rest", ignoreCase = true) ||
            it.categoryId.contains("مطعم")
    }
    val retailStoreCount = storesList.count {
        !it.sectionId.equals("restaurants", ignoreCase = true) &&
            !it.sectionId.equals("medical", ignoreCase = true)
    }
    val maintenanceCount = (provs.size - medicalProvs).coerceAtLeast(0)
    return CategoryStats(
        maintenanceCount = maintenanceCount,
        storeCount = retailStoreCount,
        restaurantCount = restaurantCount,
        medicalCount = medicalProvs + medicalStores
    )
}

internal fun getCityStatsOp(vm: AdminViewModel): CityStats {
    val cityStrings = buildList {
        vm.appState._providers.value.forEach { add("${it.cityId} ${it.area}") }
        vm._stores.value.forEach { add(it.cityId) }
        vm._properties.value.forEach { add(it.cityId) }
        vm.appState._bookings.value.forEach { add("${it.userCity} ${it.customerArea}") }
    }
    fun countCity(vararg keys: String): Int = cityStrings.count { text ->
        keys.any { k -> text.contains(k, ignoreCase = true) }
    }
    return CityStats(
        sanaaCount = countCity("ye_san", "صنعاء", "sanaa", "أمانة"),
        adenCount = countCity("ye_ade", "عدن", "aden"),
        taizCount = countCity("ye_tai", "تعز", "taiz"),
        ibbCount = countCity("ye_ibb", "إب", "اب", "ibb"),
        mukallaCount = countCity("ye_had", "ye_muk", "حضرموت", "المكلا", "سيئون", "mukalla", "hadramout")
    )
}

internal fun exportReportOp(delegate: AdminAuthAndSystemDelegate, type: String): String {
    delegate.recordAuditLog("EXPORT_REPORT", "تصدير تقرير من نوع $type")
    val dateStr = DateFormatter.formatDateDash(System.currentTimeMillis())
    return "تقرير شامل للـ $type - التاريخ: $dateStr"
}

internal fun exportAuditLogsOp(delegate: AdminAuthAndSystemDelegate, vm: AdminViewModel): String {
    delegate.recordAuditLog("EXPORT_AUDIT", "تصدير سجل التدقيق والأمان")
    return buildString {
        appendLine("ID,Admin,Action,Details,Timestamp")
        vm._auditLogsState.value.forEach {
            appendLine("${it.id},${it.adminEmail},${it.action},\"${it.details}\",${it.timestamp}")
        }
    }
}
