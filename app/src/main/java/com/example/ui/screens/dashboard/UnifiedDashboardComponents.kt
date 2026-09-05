package com.example.ui.screens.dashboard

import androidx.compose.runtime.Composable
import com.example.data.UnifiedBusinessAccount
import com.example.ui.MainViewModel
import com.example.ui.screens.dashboard.components.*
import com.example.utils.VisualThemePalette

@Composable
fun UnifiedProfileSectionWrapper(
    account: UnifiedBusinessAccount,
    themeColors: VisualThemePalette
) {
    com.example.ui.screens.dashboard.components.UnifiedProfileSection(
        title = account.name,
        subtitle = account.description,
        phone = account.phone,
        cityArea = account.neighborhood.ifBlank { account.cityId },
        photoUrl = account.logoImage,
        coverUrl = account.coverImage,
        rating = account.rating.toDouble(),
        reviewCount = account.numReviews,
        isAvailable = true,
        themeColors = themeColors
    )
}
