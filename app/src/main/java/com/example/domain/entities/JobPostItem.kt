package com.example.domain.entities

/**
 * 💼 JobPostItem
 * تم نقلها من JobPosterDashboardViewModel لتكون متاحة للجميع
 */
data class JobPostItem(
    val id: String = "",
    val title: String = "",
    val companyName: String = "",
    val salary: String = "",
    val requirements: String = "",
    val applicantsCount: Int = 0
)
