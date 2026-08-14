package com.example.data

import androidx.annotation.Keep

@Keep
data class CategoryEntity(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val order: Int = 0,
    val isPinned: Boolean = false,
    val parentId: String = "",
    val isMainCategory: Boolean = true
)
