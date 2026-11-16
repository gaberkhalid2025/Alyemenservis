package com.example.ui.viewmodels

import androidx.annotation.Keep
import java.util.UUID

@Keep
data class PendingRequest(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val section: String = "",
    val city: String = "",
    val details: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val createdAt: Long = System.currentTimeMillis()
)

@Keep
data class SystemStats(
    val totalProviders: Int = 0,
    val activeBookings: Int = 0,
    val completedOrders: Int = 0,
    val pendingApprovals: Int = 0,
    val totalUsers: Int = 0,
    val systemHealth: String = "EXCELLENT"
)

@Keep
data class SystemHealth(
    val status: String = "HEALTHY", // "HEALTHY", "WARNING", "CRITICAL"
    val uptime: Long = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 7,
    val memoryUsage: Double = 34.5,
    val cpuUsage: Double = 12.8,
    val activeConnections: Int = 142,
    val lastError: String? = null
)

@Keep
data class StorageUsage(
    val totalBytes: Long = 10L * 1024 * 1024 * 1024,
    val usedBytes: Long = 2L * 1024 * 1024 * 1024 + 450 * 1024 * 1024,
    val freeBytes: Long = 7L * 1024 * 1024 * 1024 + 574 * 1024 * 1024,
    val imageCount: Int = 854,
    val documentCount: Int = 128
)

@Keep
data class BookingStats(
    val total: Int = 0,
    val pending: Int = 0,
    val inProgress: Int = 0,
    val completed: Int = 0,
    val cancelled: Int = 0,
    val today: Int = 0,
    val thisWeek: Int = 0,
    val thisMonth: Int = 0
)

@Keep
data class RevenueStats(
    val totalRevenue: Double = 0.0,
    val platformCommission: Double = 0.0,
    val providerPayouts: Double = 0.0,
    val currency: String = "YER",
    val todayRevenue: Double = 0.0,
    val monthRevenue: Double = 0.0
)

@Keep
data class ProviderStats(
    val totalVerified: Int = 0,
    val pendingApproval: Int = 0,
    val topRated: Int = 0,
    val suspended: Int = 0
)

@Keep
data class CategoryStats(
    val maintenanceCount: Int = 0,
    val storeCount: Int = 0,
    val restaurantCount: Int = 0,
    val medicalCount: Int = 0
)

@Keep
data class CityStats(
    val sanaaCount: Int = 0,
    val adenCount: Int = 0,
    val taizCount: Int = 0,
    val ibbCount: Int = 0,
    val mukallaCount: Int = 0
)

@Keep
data class AuditLog(
    val id: String = UUID.randomUUID().toString(),
    val adminEmail: String = "admin",
    val action: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Keep
data class AdminNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val message: String = "",
    val type: String = "INFO",
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Keep
data class SystemLog(
    val id: String = UUID.randomUUID().toString(),
    val tag: String = "APP",
    val message: String = "",
    val level: String = "INFO", // INFO, WARN, ERROR
    val timestamp: Long = System.currentTimeMillis()
)
