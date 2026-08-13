package com.example.data

import androidx.annotation.Keep

@Keep
data class ReportEntity(
    val id: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val reporterName: String = "",
    val content: String = "",
    val targetId: String = "",
    val targetType: String = "" // SERVICES, STORES, RESTAURANTS, MEDICAL, PROPERTIES, JOBS
)

@Keep
data class ActivityLogEntity(
    val id: String = "",
    val action: String = "",
    val timestamp: Long = 0L
)
