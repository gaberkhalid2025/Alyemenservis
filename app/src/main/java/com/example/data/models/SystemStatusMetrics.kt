package com.example.data.models

data class SystemStatusMetrics(
    val providersCount: Int = 0,
    val storesCount: Int = 0,
    val propertiesCount: Int = 0,
    val instantRequestsCount: Int = 0,
    val bookingsCount: Int = 0,
    val pendingJoinRequestsCount: Int = 0,
    val unreadNotificationsCount: Int = 0,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)
