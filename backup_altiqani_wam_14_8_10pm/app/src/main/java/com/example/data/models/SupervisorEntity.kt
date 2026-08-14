package com.example.data

import androidx.annotation.Keep

@Keep
data class SupervisorEntity(
    val id: String = "",
    val name: String = "",
    val role: String = "", // "ADMIN", "AUDITOR", "SUPPORT", "OPERATIONS"
    val passcode: String = "",
    val permissions: List<String> = emptyList()
)
