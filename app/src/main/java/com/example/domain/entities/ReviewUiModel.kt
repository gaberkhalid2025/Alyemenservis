package com.example.domain.entities

import java.util.UUID

/**
 * ⭐ ReviewUiModel
 * تم نقلها لتكون متاحة بشكل عام لنظام التقييمات
 */
data class ReviewUiModel(
    val id: String = UUID.randomUUID().toString(),
    val authorName: String,
    val rating: Int,
    val comment: String,
    var replyText: String = ""
)
