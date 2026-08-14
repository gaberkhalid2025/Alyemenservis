package com.example.data

import androidx.annotation.Keep

@Keep
data class ReportEntity(
    val id: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val reporterName: String = "",
    val content: String = ""
)

@Keep
data class ActivityLogEntity(
    val id: String = "",
    val action: String = "",
    val timestamp: Long = 0L
)
